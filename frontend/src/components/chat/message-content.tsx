'use client';

import dynamic from 'next/dynamic';

const AssistantMarkdown = dynamic(
  () => import('@/components/chat/assistant-markdown').then((mod) => mod.AssistantMarkdown),
  {
    ssr: false,
    loading: () => <p className="text-[var(--text-sm)] whitespace-pre-wrap leading-relaxed">Carregando formatacao...</p>,
  }
);

export function MessageContent({ content, role }: { content: string; role: string }) {
  if (role !== 'assistant') {
    return <p className="text-[var(--text-sm)] whitespace-pre-wrap leading-relaxed">{content}</p>;
  }

  return <AssistantMarkdown content={content} />;
}

