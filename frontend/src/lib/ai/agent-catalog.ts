export type AiAgentScope = 'consumer' | 'codex' | 'creative' | 'speech';

export interface AiAgentDescriptor {
  id: string;
  slug: string;
  label: string;
  version: string;
  scope: AiAgentScope;
  summary: string;
  preferredModelIds: string[];
  capabilities: string[];
}

export const AI_AGENT_CATALOG: AiAgentDescriptor[] = [
  {
    id: 'chat-general',
    slug: 'chat-general',
    label: 'General Chat',
    version: 'v1',
    scope: 'consumer',
    summary: 'Agente padrao para conversas gerais, follow-ups curtos e suporte amplo.',
    preferredModelIds: ['gpt-4o-mini', 'claude-3-5-haiku', 'gemini-1.5-flash'],
    capabilities: ['chat', 'summarization', 'qa'],
  },
  {
    id: 'chat-research',
    slug: 'chat-research',
    label: 'Research',
    version: 'v1',
    scope: 'consumer',
    summary: 'Agente orientado a sintese, pesquisa e comparacao de fontes.',
    preferredModelIds: ['sonar-pro', 'gemini-1.5-flash', 'command-r-plus'],
    capabilities: ['research', 'grounding', 'synthesis'],
  },
  {
    id: 'codex-ask',
    slug: 'codex-ask',
    label: 'Codex Ask',
    version: 'v1',
    scope: 'codex',
    summary: 'Agente de leitura e diagnostico para perguntas sobre repositorio e ambiente.',
    preferredModelIds: ['gpt-4.1-mini', 'claude-3-5-haiku', 'deepseek-chat'],
    capabilities: ['repository-analysis', 'environment-diagnostics', 'qa'],
  },
  {
    id: 'codex-code',
    slug: 'codex-code',
    label: 'Codex Code',
    version: 'v1',
    scope: 'codex',
    summary: 'Agente de alteracao de codigo, patching e follow-ups tecnicos.',
    preferredModelIds: ['gpt-4.1-mini', 'deepseek-reasoner', 'mistral-large-latest'],
    capabilities: ['code-generation', 'patch-planning', 'test-suggestions'],
  },
  {
    id: 'codex-review',
    slug: 'codex-review',
    label: 'Codex Review',
    version: 'v1',
    scope: 'codex',
    summary: 'Agente de revisao de diff, risco e criterios de aceite.',
    preferredModelIds: ['gpt-4.1-mini', 'claude-3-5-haiku', 'command-r-plus'],
    capabilities: ['diff-review', 'risk-analysis', 'acceptance-criteria'],
  },
  {
    id: 'creative-image',
    slug: 'creative-image',
    label: 'Creative Image',
    version: 'v1',
    scope: 'creative',
    summary: 'Agente conceitual para pipelines de imagem e variacoes criativas.',
    preferredModelIds: ['stable-image-ultra', 'fal-ai/flux-pro', 'black-forest-labs/flux-pro'],
    capabilities: ['image-generation', 'style-transfer'],
  },
  {
    id: 'creative-video',
    slug: 'creative-video',
    label: 'Creative Video',
    version: 'v1',
    scope: 'creative',
    summary: 'Agente conceitual para geracao e iteracao de video.',
    preferredModelIds: ['gen4_turbo'],
    capabilities: ['video-generation'],
  },
  {
    id: 'speech-transcribe',
    slug: 'speech-transcribe',
    label: 'Speech Transcribe',
    version: 'v1',
    scope: 'speech',
    summary: 'Agente para transcricao e analise de audio.',
    preferredModelIds: ['nova-3', 'universal-streaming'],
    capabilities: ['transcription', 'speaker-analysis'],
  },
  {
    id: 'speech-generate',
    slug: 'speech-generate',
    label: 'Speech Generate',
    version: 'v1',
    scope: 'speech',
    summary: 'Agente para TTS e geracao de voz.',
    preferredModelIds: ['eleven_multilingual_v2'],
    capabilities: ['text-to-speech'],
  },
];

export const AI_AGENT_BY_ID = Object.fromEntries(
  AI_AGENT_CATALOG.map((item) => [item.id, item])
) as Record<string, AiAgentDescriptor>;

export function getAiAgent(agentId: string | undefined) {
  if (!agentId) return undefined;
  return AI_AGENT_BY_ID[agentId];
}
