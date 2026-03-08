import type { ChatAttachment, ChatConversation, ChatMessage, ChatParticipant, ChatRun } from '@prisma/client';
import type { ChatAttachmentDto, ChatConversationDto, ConsumerChatMessageDto } from '@/lib/contracts/platform';
import { codexDb } from '@/server/codex/db';
import { invokeChatGateway } from '@/server/ai/gateway';

export const DEFAULT_CONVERSATION_TITLE = 'Nova Conversa';

export type ConversationWithMessagesDto = ChatConversationDto & {
  messages: ConsumerChatMessageDto[];
};

function mapConversation(conversation: ChatConversation): ChatConversationDto {
  return {
    id: conversation.id,
    workspaceId: conversation.workspaceId,
    createdById: conversation.createdById,
    title: conversation.title,
    model: conversation.model,
    pinned: conversation.pinned,
    archivedAt: conversation.archivedAt?.toISOString() ?? null,
    lastMessageAt: conversation.lastMessageAt?.toISOString() ?? null,
    createdAt: conversation.createdAt.toISOString(),
    updatedAt: conversation.updatedAt.toISOString(),
  };
}

function mapMessage(message: ChatMessage): ConsumerChatMessageDto {
  return {
    id: message.id,
    conversationId: message.conversationId,
    role: message.role as ConsumerChatMessageDto['role'],
    content: message.content,
    modelUsed: message.modelUsed ?? null,
    providerUsed: message.providerUsed ?? null,
    agentUsed: message.agentUsed ?? null,
    agentVersion: message.agentVersion ?? null,
    fallbackUsed: message.fallbackUsed,
    attempts: message.attempts ?? null,
    isComplete: message.isComplete,
    createdAt: message.createdAt.toISOString(),
    updatedAt: message.updatedAt.toISOString(),
  };
}

export function mapAttachment(attachment: ChatAttachment): ChatAttachmentDto {
  return {
    id: attachment.id,
    messageId: attachment.messageId,
    title: attachment.title,
    contentType: attachment.contentType,
    url: attachment.url,
    createdAt: attachment.createdAt.toISOString(),
  };
}

function mapConversationRecord(input: ChatConversation & {
  messages: ChatMessage[];
  participants?: ChatParticipant[];
  runs?: ChatRun[];
}) {
  return {
    ...mapConversation(input),
    messages: input.messages.map(mapMessage),
  };
}

async function getConversationOrThrow(input: {
  workspaceId: string;
  userId: string;
  conversationId: string;
}) {
  const conversation = await codexDb.chatConversation.findFirst({
    where: {
      id: input.conversationId,
      workspaceId: input.workspaceId,
      archivedAt: null,
      participants: {
        some: {
          userId: input.userId,
        },
      },
    },
    include: {
      messages: {
        orderBy: { createdAt: 'asc' },
      },
    },
  });

  if (!conversation) {
    throw new Error('Conversa nao encontrada');
  }

  return conversation;
}

export async function listConversations(input: {
  workspaceId: string;
  userId: string;
}): Promise<ConversationWithMessagesDto[]> {
  const conversations = await codexDb.chatConversation.findMany({
    where: {
      workspaceId: input.workspaceId,
      archivedAt: null,
      participants: {
        some: {
          userId: input.userId,
        },
      },
    },
    include: {
      messages: {
        orderBy: { createdAt: 'asc' },
      },
    },
    orderBy: [{ pinned: 'desc' }, { updatedAt: 'desc' }],
  });

  return conversations.map(mapConversationRecord);
}

export async function createConversation(input: {
  workspaceId: string;
  userId: string;
  title?: string;
  model: string;
}) {
  const conversation = await codexDb.chatConversation.create({
    data: {
      workspaceId: input.workspaceId,
      createdById: input.userId,
      title: input.title?.trim() || DEFAULT_CONVERSATION_TITLE,
      model: input.model,
      participants: {
        create: {
          userId: input.userId,
          role: 'OWNER',
        },
      },
    },
    include: {
      messages: {
        orderBy: { createdAt: 'asc' },
      },
    },
  });

  return mapConversationRecord(conversation);
}

export async function updateConversation(input: {
  workspaceId: string;
  userId: string;
  conversationId: string;
  title?: string;
  pinned?: boolean;
  model?: string;
  clearMessages?: boolean;
}) {
  await getConversationOrThrow(input);

  if (input.clearMessages) {
    await codexDb.chatMessage.deleteMany({
      where: {
        conversationId: input.conversationId,
      },
    });
  }

  const updated = await codexDb.chatConversation.update({
    where: { id: input.conversationId },
    data: {
      ...(typeof input.title === 'string'
        ? { title: input.title.trim() || DEFAULT_CONVERSATION_TITLE }
        : {}),
      ...(typeof input.pinned === 'boolean' ? { pinned: input.pinned } : {}),
      ...(typeof input.model === 'string' ? { model: input.model } : {}),
      ...(input.clearMessages
        ? {
            title: DEFAULT_CONVERSATION_TITLE,
            lastMessageAt: null,
          }
        : {}),
    },
    include: {
      messages: {
        orderBy: { createdAt: 'asc' },
      },
    },
  });

  return mapConversationRecord(updated);
}

export async function deleteConversation(input: {
  workspaceId: string;
  userId: string;
  conversationId: string;
}) {
  await getConversationOrThrow(input);
  await codexDb.chatConversation.delete({
    where: { id: input.conversationId },
  });
}

export async function sendConversationMessage(input: {
  workspaceId: string;
  userId: string;
  conversationId: string;
  prompt: string;
  preferredModel?: string;
  agentId?: string;
}) {
  const conversation = await getConversationOrThrow(input);
  const effectiveModel = input.preferredModel || conversation.model;
  const trimmedPrompt = input.prompt.trim();

  if (!trimmedPrompt) {
    throw new Error('Prompt vazio');
  }

  const userMessage = await codexDb.chatMessage.create({
    data: {
      conversationId: input.conversationId,
      role: 'user',
      content: trimmedPrompt,
      isComplete: true,
    },
  });

  const run = await codexDb.chatRun.create({
    data: {
      conversationId: input.conversationId,
      promptMessageId: userMessage.id,
      executionMode: 'live',
      status: 'running',
    },
  });

  await codexDb.chatConversation.update({
    where: { id: input.conversationId },
    data: {
      model: effectiveModel,
      title:
        conversation.messages.length === 0
          ? trimmedPrompt.slice(0, 60)
          : conversation.title,
      lastMessageAt: userMessage.createdAt,
    },
  });

  try {
    const result = await invokeChatGateway({
      prompt: trimmedPrompt,
      preferredModel: effectiveModel,
      agentId: input.agentId,
    });

    const assistantMessage = await codexDb.chatMessage.create({
      data: {
        conversationId: input.conversationId,
        role: 'assistant',
        content: result.content,
        modelUsed: result.modelUsed,
        providerUsed: result.providerUsed,
        agentUsed: result.agentUsed,
        agentVersion: result.agentVersion,
        fallbackUsed: result.fallbackUsed,
        attempts: result.attempts,
        isComplete: true,
      },
    });

    await codexDb.chatRun.update({
      where: { id: run.id },
      data: {
        responseMessageId: assistantMessage.id,
        providerUsed: result.providerUsed,
        modelUsed: result.modelUsed,
        agentUsed: result.agentUsed,
        agentVersion: result.agentVersion,
        status: 'completed',
        completedAt: new Date(),
      },
    });

    const updatedConversation = await codexDb.chatConversation.update({
      where: { id: input.conversationId },
      data: {
        lastMessageAt: assistantMessage.createdAt,
      },
      include: {
        messages: {
          orderBy: { createdAt: 'asc' },
        },
      },
    });

    return {
      conversation: mapConversationRecord(updatedConversation),
      runId: run.id,
    };
  } catch (error) {
    const message =
      error instanceof Error
        ? error.message
        : 'Falha ao executar o provider configurado';

    await codexDb.chatMessage.create({
      data: {
        conversationId: input.conversationId,
        role: 'error',
        content: message,
        isComplete: true,
      },
    });

    await codexDb.chatRun.update({
      where: { id: run.id },
      data: {
        status: 'failed',
        errorMessage: message,
        completedAt: new Date(),
      },
    });

    throw new Error(message);
  }
}

export async function ensureSystemPromptTemplates(workspaceId: string) {
  const totalTemplates = await codexDb.promptTemplate.count({
    where: {
      OR: [
        { scope: 'SYSTEM' },
        { scope: 'WORKSPACE', workspaceId },
      ],
    },
  });

  if (totalTemplates > 0) {
    return;
  }

  await codexDb.promptTemplate.createMany({
    data: [
      {
        scope: 'SYSTEM',
        category: 'analysis',
        title: 'Resumo Executivo',
        description: 'Condensa informacoes em topicos estrategicos para lideranca.',
        tag: 'Analise',
        prompt: 'Crie um resumo executivo estruturado em ate 7 bullets com foco em insights e decisoes estrategicas sobre o seguinte tema:',
      },
      {
        scope: 'SYSTEM',
        category: 'planning',
        title: 'Plano de Acao',
        description: 'Transforma objetivos em plano estruturado com etapas, donos e riscos.',
        tag: 'Planejamento',
        prompt: 'Crie um plano de acao detalhado com etapas claras, responsaveis, prazos estimados, dependencias e principais riscos para:',
      },
      {
        scope: 'SYSTEM',
        category: 'writing',
        title: 'E-mail Profissional',
        description: 'Rascunho claro, objetivo e com tom apropriado para comunicacoes formais.',
        tag: 'Escrita',
        prompt: 'Escreva um e-mail profissional com tom cordial, objetivo e estrutura clara sobre:',
      },
      {
        scope: 'SYSTEM',
        category: 'analysis',
        title: 'Analise Comparativa',
        description: 'Compara alternativas com criterios objetivos e recomendacao final.',
        tag: 'Analise',
        prompt: 'Compare as alternativas abaixo em uma tabela com criterios objetivos e conclua com uma recomendacao justificada:',
      },
      {
        scope: 'SYSTEM',
        category: 'writing',
        title: 'Documento Tecnico',
        description: 'Estrutura clara para documentacao tecnica, RFCs ou especificacoes.',
        tag: 'Escrita',
        prompt: 'Escreva um documento tecnico com secoes Objetivo, Contexto, Solucao proposta, Requisitos, Consideracoes e Plano de implementacao para:',
      },
      {
        scope: 'SYSTEM',
        category: 'planning',
        title: 'OKRs e Metas',
        description: 'Define Objectives e Key Results claros e mensuraveis para equipes.',
        tag: 'Planejamento',
        prompt: 'Defina 3 Objectives e 3 Key Results para o seguinte contexto de equipe ou area. Seja especifico, mensuravel e com prazo trimestral:',
      },
    ],
  });
}
