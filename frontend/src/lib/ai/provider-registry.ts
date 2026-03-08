export type AiProviderCategory =
  | 'llm'
  | 'image'
  | 'audio'
  | 'speech'
  | 'search'
  | 'embedding'
  | 'multimodal';

export type AiProviderProtocol =
  | 'openai-compatible'
  | 'openai-responses'
  | 'anthropic-messages'
  | 'google-gemini'
  | 'cohere-chat-v2'
  | 'mistral-chat'
  | 'azure-openai'
  | 'aws-bedrock'
  | 'cloudflare-workers-ai'
  | 'huggingface-router'
  | 'replicate-predictions'
  | 'fal-queue'
  | 'stability-rest'
  | 'deepgram-rest'
  | 'assemblyai-rest'
  | 'elevenlabs-rest'
  | 'runway-rest'
  | 'exa-search'
  | 'newscatcher-search'
  | 'generic-http';

export type AiModelKind =
  | 'chat'
  | 'reasoning'
  | 'code'
  | 'search'
  | 'image'
  | 'speech'
  | 'transcription'
  | 'embedding'
  | 'multimodal';

export interface ProviderEnvRequirement {
  key: string;
  required: boolean;
  description: string;
}

export interface AiModelDescriptor {
  id: string;
  slug: string;
  label: string;
  providerId: string;
  provider: string;
  kind: AiModelKind;
  description: string;
  availability: 'free' | 'trial' | 'paid' | 'mixed';
  maxContextTokens?: number;
  enabledForChat: boolean;
  tags: string[];
}

export interface AiProviderDescriptor {
  id: string;
  slug: string;
  name: string;
  category: AiProviderCategory;
  protocol: AiProviderProtocol;
  summary: string;
  docsUrl: string;
  defaultBaseUrl?: string;
  env: ProviderEnvRequirement[];
  supportsLiveChat: boolean;
  supportedAgentIds: string[];
  models: AiModelDescriptor[];
}

function model(input: Omit<AiModelDescriptor, 'slug'>): AiModelDescriptor {
  return {
    ...input,
    slug: input.id,
  };
}

function provider(input: AiProviderDescriptor): AiProviderDescriptor {
  return input;
}

export const AI_PROVIDER_CATALOG: AiProviderDescriptor[] = [
  provider({
    id: 'openai',
    slug: 'openai',
    name: 'OpenAI',
    category: 'llm',
    protocol: 'openai-responses',
    summary: 'Responses API para chat, reasoning, multimodal, embeddings, audio e imagem.',
    docsUrl: 'https://platform.openai.com/docs',
    defaultBaseUrl: 'https://api.openai.com/v1',
    env: [
      { key: 'OPENAI_API_KEY', required: true, description: 'API key principal da OpenAI.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-ask', 'codex-code', 'codex-review'],
    models: [
      model({
        id: 'gpt-4o-mini',
        label: 'GPT-4o Mini',
        providerId: 'openai',
        provider: 'OpenAI',
        kind: 'chat',
        description: 'Modelo rapido para chat geral e automacoes.',
        availability: 'paid',
        maxContextTokens: 128000,
        enabledForChat: true,
        tags: ['fast', 'general'],
      }),
      model({
        id: 'gpt-4.1-mini',
        label: 'GPT-4.1 Mini',
        providerId: 'openai',
        provider: 'OpenAI',
        kind: 'code',
        description: 'Modelo equilibrado para codigo, ferramentas e follow-ups.',
        availability: 'paid',
        maxContextTokens: 128000,
        enabledForChat: true,
        tags: ['balanced', 'code'],
      }),
    ],
  }),
  provider({
    id: 'anthropic',
    slug: 'anthropic',
    name: 'Anthropic',
    category: 'llm',
    protocol: 'anthropic-messages',
    summary: 'Messages API para Claude com foco em chat, reasoning e code workflows.',
    docsUrl: 'https://docs.anthropic.com/en/api/messages',
    defaultBaseUrl: 'https://api.anthropic.com/v1',
    env: [
      { key: 'ANTHROPIC_API_KEY', required: true, description: 'API key da Anthropic.' },
      { key: 'ANTHROPIC_VERSION', required: false, description: 'Versao do header anthropic-version.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-ask', 'codex-code', 'codex-review'],
    models: [
      model({
        id: 'claude-3-5-haiku',
        label: 'Claude 3.5 Haiku',
        providerId: 'anthropic',
        provider: 'Anthropic',
        kind: 'chat',
        description: 'Modelo leve da familia Claude para respostas rapidas.',
        availability: 'paid',
        maxContextTokens: 200000,
        enabledForChat: true,
        tags: ['fast', 'chat'],
      }),
    ],
  }),
  provider({
    id: 'google',
    slug: 'google',
    name: 'Google Gemini',
    category: 'multimodal',
    protocol: 'google-gemini',
    summary: 'Gemini API via generateContent para texto, imagem, audio e long context.',
    docsUrl: 'https://ai.google.dev/gemini-api/docs/text-generation',
    defaultBaseUrl: 'https://generativelanguage.googleapis.com/v1beta',
    env: [
      { key: 'GEMINI_API_KEY', required: true, description: 'API key do Google AI Studio / Gemini API.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-ask'],
    models: [
      model({
        id: 'gemini-1.5-flash',
        label: 'Gemini 1.5 Flash',
        providerId: 'google',
        provider: 'Google Gemini',
        kind: 'multimodal',
        description: 'Modelo rapido com contexto longo e entrada multimodal.',
        availability: 'mixed',
        maxContextTokens: 1048576,
        enabledForChat: true,
        tags: ['fast', 'multimodal', 'long-context'],
      }),
      model({
        id: 'gemini-2.5-flash',
        label: 'Gemini 2.5 Flash',
        providerId: 'google',
        provider: 'Google Gemini',
        kind: 'multimodal',
        description: 'Geracao multimodal atual da Gemini API.',
        availability: 'mixed',
        maxContextTokens: 1048576,
        enabledForChat: true,
        tags: ['latest', 'multimodal'],
      }),
    ],
  }),
  provider({
    id: 'deepseek',
    slug: 'deepseek',
    name: 'DeepSeek',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Stack compatível com OpenAI para chat e reasoning de baixo custo.',
    docsUrl: 'https://api-docs.deepseek.com/',
    defaultBaseUrl: 'https://api.deepseek.com/v1',
    env: [
      { key: 'DEEPSEEK_API_KEY', required: true, description: 'API key da DeepSeek.' },
      { key: 'DEEPSEEK_BASE_URL', required: false, description: 'Override opcional do endpoint base.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-ask', 'codex-code'],
    models: [
      model({
        id: 'deepseek-chat',
        label: 'DeepSeek Chat',
        providerId: 'deepseek',
        provider: 'DeepSeek',
        kind: 'chat',
        description: 'Modelo geral para chat e assistencia ampla.',
        availability: 'paid',
        maxContextTokens: 64000,
        enabledForChat: true,
        tags: ['chat', 'economical'],
      }),
      model({
        id: 'deepseek-reasoner',
        label: 'DeepSeek Reasoner',
        providerId: 'deepseek',
        provider: 'DeepSeek',
        kind: 'reasoning',
        description: 'Modelo voltado a raciocinio e cadeias mais profundas.',
        availability: 'paid',
        maxContextTokens: 64000,
        enabledForChat: true,
        tags: ['reasoning'],
      }),
    ],
  }),
  provider({
    id: 'groq',
    slug: 'groq',
    name: 'Groq',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Inferencia de baixa latencia via API compatível com OpenAI.',
    docsUrl: 'https://console.groq.com/docs',
    defaultBaseUrl: 'https://api.groq.com/openai/v1',
    env: [
      { key: 'GROQ_API_KEY', required: true, description: 'API key da Groq.' },
      { key: 'GROQ_BASE_URL', required: false, description: 'Override opcional do endpoint base.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-ask', 'codex-code'],
    models: [
      model({
        id: 'llama-3.1-8b-instant',
        label: 'Llama 3.1 8B',
        providerId: 'groq',
        provider: 'Groq',
        kind: 'chat',
        description: 'Opcao de altissima velocidade para respostas curtas.',
        availability: 'mixed',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['fast', 'llama'],
      }),
      model({
        id: 'llama-3.1-70b-versatile',
        label: 'Llama 3.1 70B',
        providerId: 'groq',
        provider: 'Groq',
        kind: 'chat',
        description: 'Versao mais forte da familia Llama hospedada pela Groq.',
        availability: 'mixed',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['general', 'llama'],
      }),
    ],
  }),
  provider({
    id: 'mistral',
    slug: 'mistral',
    name: 'Mistral',
    category: 'multimodal',
    protocol: 'mistral-chat',
    summary: 'Chat Completions, embeddings, OCR e audio via La Plateforme.',
    docsUrl: 'https://docs.mistral.ai/api/',
    defaultBaseUrl: 'https://api.mistral.ai/v1',
    env: [
      { key: 'MISTRAL_API_KEY', required: true, description: 'API key da Mistral.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-ask', 'codex-code'],
    models: [
      model({
        id: 'mistral-small-latest',
        label: 'Mistral Small',
        providerId: 'mistral',
        provider: 'Mistral',
        kind: 'chat',
        description: 'Modelo geral de custo menor para chat.',
        availability: 'paid',
        maxContextTokens: 32000,
        enabledForChat: true,
        tags: ['economical'],
      }),
      model({
        id: 'mistral-large-latest',
        label: 'Mistral Large',
        providerId: 'mistral',
        provider: 'Mistral',
        kind: 'multimodal',
        description: 'Modelo topo de linha com chat completions e recursos multimodais.',
        availability: 'paid',
        maxContextTokens: 128000,
        enabledForChat: true,
        tags: ['premium', 'multimodal'],
      }),
    ],
  }),
  provider({
    id: 'cohere',
    slug: 'cohere',
    name: 'Cohere',
    category: 'llm',
    protocol: 'cohere-chat-v2',
    summary: 'Chat, embed e rerank com v2 Chat API.',
    docsUrl: 'https://docs.cohere.com/v2/reference/chat',
    defaultBaseUrl: 'https://api.cohere.ai/v2',
    env: [
      { key: 'COHERE_API_KEY', required: true, description: 'API key da Cohere.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-ask'],
    models: [
      model({
        id: 'command-r',
        label: 'Command R',
        providerId: 'cohere',
        provider: 'Cohere',
        kind: 'chat',
        description: 'Modelo geral da Cohere com foco em business chat.',
        availability: 'mixed',
        maxContextTokens: 128000,
        enabledForChat: true,
        tags: ['chat', 'rag'],
      }),
      model({
        id: 'command-r-plus',
        label: 'Command R+',
        providerId: 'cohere',
        provider: 'Cohere',
        kind: 'reasoning',
        description: 'Versao mais forte para tasks maiores e grounded answers.',
        availability: 'mixed',
        maxContextTokens: 128000,
        enabledForChat: true,
        tags: ['premium', 'rag'],
      }),
    ],
  }),
  provider({
    id: 'perplexity',
    slug: 'perplexity',
    name: 'Perplexity',
    category: 'search',
    protocol: 'openai-compatible',
    summary: 'Sonar e busca nativa com API compatível com OpenAI para respostas grounded.',
    docsUrl: 'https://docs.perplexity.ai/',
    defaultBaseUrl: 'https://api.perplexity.ai',
    env: [
      { key: 'PERPLEXITY_API_KEY', required: true, description: 'API key da Perplexity.' },
      { key: 'PERPLEXITY_BASE_URL', required: false, description: 'Override opcional do endpoint base.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-research', 'codex-ask'],
    models: [
      model({
        id: 'sonar',
        label: 'Sonar',
        providerId: 'perplexity',
        provider: 'Perplexity',
        kind: 'search',
        description: 'Pesquisa com grounding e resumo rapido.',
        availability: 'paid',
        maxContextTokens: 128000,
        enabledForChat: true,
        tags: ['search', 'grounded'],
      }),
      model({
        id: 'sonar-pro',
        label: 'Sonar Pro',
        providerId: 'perplexity',
        provider: 'Perplexity',
        kind: 'search',
        description: 'Versao mais forte para pesquisa e synthesis.',
        availability: 'paid',
        maxContextTokens: 200000,
        enabledForChat: true,
        tags: ['search', 'premium'],
      }),
    ],
  }),
  provider({
    id: 'openrouter',
    slug: 'openrouter',
    name: 'OpenRouter',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Gateway multi-provider com API compatível para varios modelos.',
    docsUrl: 'https://openrouter.ai/docs/api-reference/overview',
    defaultBaseUrl: 'https://openrouter.ai/api/v1',
    env: [
      { key: 'OPENROUTER_API_KEY', required: true, description: 'API key do OpenRouter.' },
      { key: 'OPENROUTER_HTTP_REFERER', required: false, description: 'Header opcional para ranking/referer.' },
      { key: 'OPENROUTER_APP_TITLE', required: false, description: 'Header opcional para nome da aplicacao.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-ask', 'codex-code'],
    models: [
      model({
        id: 'openrouter/auto',
        label: 'OpenRouter Auto',
        providerId: 'openrouter',
        provider: 'OpenRouter',
        kind: 'chat',
        description: 'Roteamento automatico sobre o catalogo do OpenRouter.',
        availability: 'mixed',
        maxContextTokens: 128000,
        enabledForChat: true,
        tags: ['router', 'multi-provider'],
      }),
    ],
  }),
  provider({
    id: 'together',
    slug: 'together',
    name: 'Together AI',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Modelos abertos e multimodais via endpoint compatível com OpenAI.',
    docsUrl: 'https://docs.together.ai/docs',
    defaultBaseUrl: 'https://api.together.xyz/v1',
    env: [
      { key: 'TOGETHER_API_KEY', required: true, description: 'API key da Together AI.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-code'],
    models: [
      model({
        id: 'meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo',
        label: 'Together Llama 3.1 70B',
        providerId: 'together',
        provider: 'Together AI',
        kind: 'chat',
        description: 'Exemplo de modelo aberto servido pela Together.',
        availability: 'paid',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['open-models'],
      }),
    ],
  }),
  provider({
    id: 'fireworks',
    slug: 'fireworks',
    name: 'Fireworks AI',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Inferencia multimodelo com compatibilidade OpenAI.',
    docsUrl: 'https://docs.fireworks.ai/',
    defaultBaseUrl: 'https://api.fireworks.ai/inference/v1',
    env: [
      { key: 'FIREWORKS_API_KEY', required: true, description: 'API key da Fireworks AI.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research', 'codex-code'],
    models: [
      model({
        id: 'accounts/fireworks/models/llama-v3p1-70b-instruct',
        label: 'Fireworks Llama 70B',
        providerId: 'fireworks',
        provider: 'Fireworks AI',
        kind: 'chat',
        description: 'Modelo de exemplo para inferencia geral na Fireworks.',
        availability: 'paid',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['open-models'],
      }),
    ],
  }),
  provider({
    id: 'xai',
    slug: 'xai',
    name: 'xAI',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'API para Grok e modelos relacionados com superficie compatível.',
    docsUrl: 'https://docs.x.ai/',
    defaultBaseUrl: 'https://api.x.ai/v1',
    env: [
      { key: 'XAI_API_KEY', required: true, description: 'API key da xAI.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'chat-research'],
    models: [
      model({
        id: 'grok-beta',
        label: 'Grok Beta',
        providerId: 'xai',
        provider: 'xAI',
        kind: 'chat',
        description: 'Entrada generica para o catalogo Grok.',
        availability: 'paid',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['general'],
      }),
    ],
  }),
  provider({
    id: 'cerebras',
    slug: 'cerebras',
    name: 'Cerebras',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Serving de modelos focado em throughput, usualmente com compatibilidade OpenAI.',
    docsUrl: 'https://inference-docs.cerebras.ai/',
    defaultBaseUrl: 'https://api.cerebras.ai/v1',
    env: [
      { key: 'CEREBRAS_API_KEY', required: true, description: 'API key da Cerebras.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'codex-code'],
    models: [
      model({
        id: 'llama3.1-70b',
        label: 'Cerebras Llama 70B',
        providerId: 'cerebras',
        provider: 'Cerebras',
        kind: 'chat',
        description: 'Modelo de exemplo para high-throughput text generation.',
        availability: 'paid',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['throughput'],
      }),
    ],
  }),
  provider({
    id: 'sambanova',
    slug: 'sambanova',
    name: 'SambaNova',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Inference cloud com endpoint compatível para modelos abertos.',
    docsUrl: 'https://docs.sambanova.ai/',
    defaultBaseUrl: 'https://api.sambanova.ai/v1',
    env: [
      { key: 'SAMBANOVA_API_KEY', required: true, description: 'API key da SambaNova.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'codex-code'],
    models: [
      model({
        id: 'Meta-Llama-3.1-70B-Instruct',
        label: 'SambaNova Llama 70B',
        providerId: 'sambanova',
        provider: 'SambaNova',
        kind: 'chat',
        description: 'Modelo aberto via plataforma SambaNova.',
        availability: 'paid',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['open-models'],
      }),
    ],
  }),
  provider({
    id: 'nvidia',
    slug: 'nvidia',
    name: 'NVIDIA NIM',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'NVIDIA NIM e endpoints para inferencia compatível com OpenAI.',
    docsUrl: 'https://docs.api.nvidia.com/',
    defaultBaseUrl: 'https://integrate.api.nvidia.com/v1',
    env: [
      { key: 'NVIDIA_API_KEY', required: true, description: 'API key da NVIDIA API catalog.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'codex-code'],
    models: [
      model({
        id: 'meta/llama-3.1-70b-instruct',
        label: 'NVIDIA Llama 70B',
        providerId: 'nvidia',
        provider: 'NVIDIA NIM',
        kind: 'chat',
        description: 'Modelo exemplo na plataforma NVIDIA NIM.',
        availability: 'mixed',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['open-models'],
      }),
    ],
  }),
  provider({
    id: 'huggingface',
    slug: 'huggingface',
    name: 'Hugging Face',
    category: 'llm',
    protocol: 'huggingface-router',
    summary: 'Inference Providers e routers para modelos abertos.',
    docsUrl: 'https://huggingface.co/docs/api-inference/index',
    defaultBaseUrl: 'https://router.huggingface.co/v1',
    env: [
      { key: 'HUGGINGFACE_API_KEY', required: true, description: 'User access token da Hugging Face.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['chat-research', 'codex-code'],
    models: [
      model({
        id: 'meta-llama/Llama-3.1-8B-Instruct',
        label: 'HF Llama 3.1 8B',
        providerId: 'huggingface',
        provider: 'Hugging Face',
        kind: 'chat',
        description: 'Modelo aberto servido por providers da Hugging Face.',
        availability: 'mixed',
        maxContextTokens: 131072,
        enabledForChat: false,
        tags: ['open-models'],
      }),
    ],
  }),
  provider({
    id: 'cloudflare',
    slug: 'cloudflare',
    name: 'Cloudflare Workers AI',
    category: 'llm',
    protocol: 'cloudflare-workers-ai',
    summary: 'Workers AI e AI Gateway para inferencia no edge.',
    docsUrl: 'https://developers.cloudflare.com/workers-ai/',
    defaultBaseUrl: 'https://api.cloudflare.com/client/v4',
    env: [
      { key: 'CLOUDFLARE_API_TOKEN', required: true, description: 'Token da Cloudflare com escopo Workers AI.' },
      { key: 'CLOUDFLARE_ACCOUNT_ID', required: true, description: 'Account ID da Cloudflare.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['chat-general', 'codex-code'],
    models: [
      model({
        id: '@cf/meta/llama-3.1-8b-instruct',
        label: 'CF Llama 3.1 8B',
        providerId: 'cloudflare',
        provider: 'Cloudflare Workers AI',
        kind: 'chat',
        description: 'Exemplo de modelo servido pela Cloudflare.',
        availability: 'paid',
        maxContextTokens: 131072,
        enabledForChat: false,
        tags: ['edge'],
      }),
    ],
  }),
  provider({
    id: 'azure-openai',
    slug: 'azure-openai',
    name: 'Azure OpenAI',
    category: 'llm',
    protocol: 'azure-openai',
    summary: 'Deployments Azure OpenAI com endpoint proprio e api-version.',
    docsUrl: 'https://learn.microsoft.com/azure/ai-services/openai/',
    defaultBaseUrl: 'https://YOUR-RESOURCE.openai.azure.com',
    env: [
      { key: 'AZURE_OPENAI_API_KEY', required: true, description: 'API key do recurso Azure OpenAI.' },
      { key: 'AZURE_OPENAI_ENDPOINT', required: true, description: 'Endpoint do recurso Azure OpenAI.' },
      { key: 'AZURE_OPENAI_API_VERSION', required: true, description: 'Api-version do Azure OpenAI.' },
      { key: 'AZURE_OPENAI_DEPLOYMENT', required: true, description: 'Deployment padrao para chat.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'codex-code'],
    models: [
      model({
        id: 'azure-deployment-default',
        label: 'Azure Deployment',
        providerId: 'azure-openai',
        provider: 'Azure OpenAI',
        kind: 'chat',
        description: 'Deployment default resolvido via configuracao do Azure.',
        availability: 'paid',
        maxContextTokens: 128000,
        enabledForChat: true,
        tags: ['enterprise'],
      }),
    ],
  }),
  provider({
    id: 'aws-bedrock',
    slug: 'aws-bedrock',
    name: 'AWS Bedrock',
    category: 'llm',
    protocol: 'aws-bedrock',
    summary: 'Bedrock com provedores multi-modelo via AWS runtime.',
    docsUrl: 'https://docs.aws.amazon.com/bedrock/',
    env: [
      { key: 'AWS_ACCESS_KEY_ID', required: true, description: 'Access key AWS para Bedrock.' },
      { key: 'AWS_SECRET_ACCESS_KEY', required: true, description: 'Secret key AWS para Bedrock.' },
      { key: 'AWS_REGION', required: true, description: 'Regiao Bedrock.' },
      { key: 'AWS_BEDROCK_MODEL_ID', required: false, description: 'Model ID default para invocacao.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['chat-general', 'chat-research'],
    models: [
      model({
        id: 'bedrock-default',
        label: 'Bedrock Default Model',
        providerId: 'aws-bedrock',
        provider: 'AWS Bedrock',
        kind: 'chat',
        description: 'Model ID default resolvido por configuracao AWS.',
        availability: 'paid',
        maxContextTokens: 200000,
        enabledForChat: false,
        tags: ['enterprise', 'aws'],
      }),
    ],
  }),
  provider({
    id: 'ai21',
    slug: 'ai21',
    name: 'AI21',
    category: 'llm',
    protocol: 'generic-http',
    summary: 'APIs de texto e modelos Jamba/Studio.',
    docsUrl: 'https://docs.ai21.com/',
    defaultBaseUrl: 'https://api.ai21.com/studio/v1',
    env: [
      { key: 'AI21_API_KEY', required: true, description: 'API key da AI21.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['chat-general'],
    models: [
      model({
        id: 'jamba-1.5-large',
        label: 'Jamba 1.5 Large',
        providerId: 'ai21',
        provider: 'AI21',
        kind: 'chat',
        description: 'Exemplo de modelo da AI21 para chat.',
        availability: 'paid',
        maxContextTokens: 256000,
        enabledForChat: false,
        tags: ['general'],
      }),
    ],
  }),
  provider({
    id: 'deepinfra',
    slug: 'deepinfra',
    name: 'DeepInfra',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Gateway para modelos abertos com compatibilidade ampla.',
    docsUrl: 'https://deepinfra.com/docs',
    defaultBaseUrl: 'https://api.deepinfra.com/v1/openai',
    env: [
      { key: 'DEEPINFRA_API_KEY', required: true, description: 'API key da DeepInfra.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'codex-code'],
    models: [
      model({
        id: 'meta-llama/Meta-Llama-3.1-70B-Instruct',
        label: 'DeepInfra Llama 70B',
        providerId: 'deepinfra',
        provider: 'DeepInfra',
        kind: 'chat',
        description: 'Exemplo de modelo aberto via DeepInfra.',
        availability: 'paid',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['open-models'],
      }),
    ],
  }),
  provider({
    id: 'siliconflow',
    slug: 'siliconflow',
    name: 'SiliconFlow',
    category: 'llm',
    protocol: 'openai-compatible',
    summary: 'Router para modelos abertos e multimodais.',
    docsUrl: 'https://docs.siliconflow.com/',
    defaultBaseUrl: 'https://api.siliconflow.cn/v1',
    env: [
      { key: 'SILICONFLOW_API_KEY', required: true, description: 'API key da SiliconFlow.' },
    ],
    supportsLiveChat: true,
    supportedAgentIds: ['chat-general', 'codex-code'],
    models: [
      model({
        id: 'Qwen/Qwen2.5-72B-Instruct',
        label: 'SiliconFlow Qwen 72B',
        providerId: 'siliconflow',
        provider: 'SiliconFlow',
        kind: 'chat',
        description: 'Exemplo de modelo roteado pela SiliconFlow.',
        availability: 'paid',
        maxContextTokens: 131072,
        enabledForChat: true,
        tags: ['open-models'],
      }),
    ],
  }),
  provider({
    id: 'github-models',
    slug: 'github-models',
    name: 'GitHub Models',
    category: 'llm',
    protocol: 'generic-http',
    summary: 'Catalogo de modelos acessados pela camada GitHub/Azure.',
    docsUrl: 'https://docs.github.com/en/github-models',
    env: [
      { key: 'GITHUB_TOKEN', required: true, description: 'Token com acesso ao GitHub Models.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['codex-code'],
    models: [
      model({
        id: 'github-models-default',
        label: 'GitHub Models Default',
        providerId: 'github-models',
        provider: 'GitHub Models',
        kind: 'chat',
        description: 'Entrada de catalogo para o portfolio GitHub Models.',
        availability: 'mixed',
        maxContextTokens: 128000,
        enabledForChat: false,
        tags: ['github'],
      }),
    ],
  }),
  provider({
    id: 'exa',
    slug: 'exa',
    name: 'Exa',
    category: 'search',
    protocol: 'exa-search',
    summary: 'Busca semantica para agentes de IA e pesquisa profunda.',
    docsUrl: 'https://docs.exa.ai/',
    defaultBaseUrl: 'https://api.exa.ai',
    env: [
      { key: 'EXA_API_KEY', required: true, description: 'API key da Exa.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['chat-research', 'codex-ask'],
    models: [
      model({
        id: 'exa-search',
        label: 'Exa Search',
        providerId: 'exa',
        provider: 'Exa',
        kind: 'search',
        description: 'Busca semantica para grounding e retrieval.',
        availability: 'mixed',
        enabledForChat: false,
        tags: ['search', 'research'],
      }),
    ],
  }),
  provider({
    id: 'newscatcher',
    slug: 'newscatcher',
    name: 'NewsCatcher',
    category: 'search',
    protocol: 'newscatcher-search',
    summary: 'Busca de noticias com foco em cobertura e recall alto.',
    docsUrl: 'https://www.newscatcherapi.com/docs',
    defaultBaseUrl: 'https://v3-api.newscatcherapi.com',
    env: [
      { key: 'NEWSCATCHER_API_KEY', required: true, description: 'API key da NewsCatcher.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['chat-research'],
    models: [
      model({
        id: 'newscatcher-search',
        label: 'NewsCatcher Search',
        providerId: 'newscatcher',
        provider: 'NewsCatcher',
        kind: 'search',
        description: 'Busca estruturada de noticias e eventos.',
        availability: 'paid',
        enabledForChat: false,
        tags: ['news', 'search'],
      }),
    ],
  }),
  provider({
    id: 'stability',
    slug: 'stability',
    name: 'Stability AI',
    category: 'image',
    protocol: 'stability-rest',
    summary: 'Image generation e image editing pela API da Stability.',
    docsUrl: 'https://platform.stability.ai/docs/api-reference',
    defaultBaseUrl: 'https://api.stability.ai',
    env: [
      { key: 'STABILITY_API_KEY', required: true, description: 'API key da Stability AI.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['creative-image'],
    models: [
      model({
        id: 'stable-image-ultra',
        label: 'Stable Image Ultra',
        providerId: 'stability',
        provider: 'Stability AI',
        kind: 'image',
        description: 'Modelo de imagem premium da Stability.',
        availability: 'paid',
        enabledForChat: false,
        tags: ['image'],
      }),
    ],
  }),
  provider({
    id: 'fal',
    slug: 'fal',
    name: 'fal.ai',
    category: 'image',
    protocol: 'fal-queue',
    summary: 'Inference serverless para imagem, video e multimidia.',
    docsUrl: 'https://fal.ai/docs',
    defaultBaseUrl: 'https://queue.fal.run',
    env: [
      { key: 'FAL_KEY', required: true, description: 'Token da fal.ai.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['creative-image'],
    models: [
      model({
        id: 'fal-ai/flux-pro',
        label: 'FLUX Pro',
        providerId: 'fal',
        provider: 'fal.ai',
        kind: 'image',
        description: 'Modelo de imagem servido pela fal.ai.',
        availability: 'paid',
        enabledForChat: false,
        tags: ['image'],
      }),
    ],
  }),
  provider({
    id: 'replicate',
    slug: 'replicate',
    name: 'Replicate',
    category: 'multimodal',
    protocol: 'replicate-predictions',
    summary: 'Predictions API para modelos de imagem, video, audio e open-source AI.',
    docsUrl: 'https://replicate.com/docs',
    defaultBaseUrl: 'https://api.replicate.com/v1',
    env: [
      { key: 'REPLICATE_API_TOKEN', required: true, description: 'Token da Replicate.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['creative-image'],
    models: [
      model({
        id: 'black-forest-labs/flux-pro',
        label: 'Black Forest Labs FLUX Pro',
        providerId: 'replicate',
        provider: 'Replicate',
        kind: 'image',
        description: 'Exemplo de modelo FLUX servido via Replicate.',
        availability: 'paid',
        enabledForChat: false,
        tags: ['image'],
      }),
    ],
  }),
  provider({
    id: 'deepgram',
    slug: 'deepgram',
    name: 'Deepgram',
    category: 'audio',
    protocol: 'deepgram-rest',
    summary: 'Speech-to-text, TTS e audio intelligence.',
    docsUrl: 'https://developers.deepgram.com/',
    defaultBaseUrl: 'https://api.deepgram.com',
    env: [
      { key: 'DEEPGRAM_API_KEY', required: true, description: 'API key da Deepgram.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['speech-transcribe'],
    models: [
      model({
        id: 'nova-3',
        label: 'Nova-3',
        providerId: 'deepgram',
        provider: 'Deepgram',
        kind: 'transcription',
        description: 'Modelo de transcricao e speech intelligence.',
        availability: 'mixed',
        enabledForChat: false,
        tags: ['audio', 'transcription'],
      }),
    ],
  }),
  provider({
    id: 'assemblyai',
    slug: 'assemblyai',
    name: 'AssemblyAI',
    category: 'audio',
    protocol: 'assemblyai-rest',
    summary: 'Speech-to-text e analise de audio.',
    docsUrl: 'https://www.assemblyai.com/docs',
    defaultBaseUrl: 'https://api.assemblyai.com/v2',
    env: [
      { key: 'ASSEMBLYAI_API_KEY', required: true, description: 'API key da AssemblyAI.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['speech-transcribe'],
    models: [
      model({
        id: 'universal-streaming',
        label: 'Universal Streaming',
        providerId: 'assemblyai',
        provider: 'AssemblyAI',
        kind: 'transcription',
        description: 'Entrada de catalogo para speech-to-text.',
        availability: 'paid',
        enabledForChat: false,
        tags: ['audio', 'transcription'],
      }),
    ],
  }),
  provider({
    id: 'elevenlabs',
    slug: 'elevenlabs',
    name: 'ElevenLabs',
    category: 'speech',
    protocol: 'elevenlabs-rest',
    summary: 'Text-to-speech, voices e audio generation.',
    docsUrl: 'https://elevenlabs.io/docs',
    defaultBaseUrl: 'https://api.elevenlabs.io',
    env: [
      { key: 'ELEVENLABS_API_KEY', required: true, description: 'API key da ElevenLabs.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['speech-generate'],
    models: [
      model({
        id: 'eleven_multilingual_v2',
        label: 'Eleven Multilingual v2',
        providerId: 'elevenlabs',
        provider: 'ElevenLabs',
        kind: 'speech',
        description: 'Modelo TTS multilingual.',
        availability: 'paid',
        enabledForChat: false,
        tags: ['speech', 'tts'],
      }),
    ],
  }),
  provider({
    id: 'runway',
    slug: 'runway',
    name: 'Runway',
    category: 'multimodal',
    protocol: 'runway-rest',
    summary: 'Geracao e edicao de video para pipelines criativos.',
    docsUrl: 'https://docs.dev.runwayml.com/',
    defaultBaseUrl: 'https://api.dev.runwayml.com',
    env: [
      { key: 'RUNWAY_API_KEY', required: true, description: 'API key da Runway.' },
    ],
    supportsLiveChat: false,
    supportedAgentIds: ['creative-video'],
    models: [
      model({
        id: 'gen4_turbo',
        label: 'Gen-4 Turbo',
        providerId: 'runway',
        provider: 'Runway',
        kind: 'multimodal',
        description: 'Modelo de video generation da Runway.',
        availability: 'paid',
        enabledForChat: false,
        tags: ['video'],
      }),
    ],
  }),
];

export const CHAT_MODEL_CATALOG = AI_PROVIDER_CATALOG.flatMap((providerItem) =>
  providerItem.models.filter((item) => item.enabledForChat)
);

export const AI_MODEL_CATALOG = AI_PROVIDER_CATALOG.flatMap((providerItem) => providerItem.models);

export const AI_PROVIDER_BY_ID = Object.fromEntries(
  AI_PROVIDER_CATALOG.map((item) => [item.id, item])
) as Record<string, AiProviderDescriptor>;

export const AI_MODEL_BY_ID = Object.fromEntries(
  AI_MODEL_CATALOG.map((item) => [item.id, item])
) as Record<string, AiModelDescriptor>;

export const DEFAULT_CHAT_MODEL_IDS = [
  'gpt-4o-mini',
  'claude-3-5-haiku',
  'gemini-1.5-flash',
  'llama-3.1-8b-instant',
  'mistral-small-latest',
];

export const AI_PROVIDER_KEY_CHECKLIST = AI_PROVIDER_CATALOG.map((providerItem) => ({
  providerId: providerItem.id,
  providerName: providerItem.name,
  category: providerItem.category,
  docsUrl: providerItem.docsUrl,
  env: providerItem.env,
}));

export function getAiProvider(providerId: string | undefined) {
  if (!providerId) return undefined;
  return AI_PROVIDER_BY_ID[providerId];
}

export function getAiModel(modelId: string | undefined) {
  if (!modelId) return undefined;
  return AI_MODEL_BY_ID[modelId];
}

export function listModelsForProvider(providerId: string) {
  return AI_MODEL_CATALOG.filter((item) => item.providerId === providerId);
}
