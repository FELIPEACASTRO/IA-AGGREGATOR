'use client';

import { useMemo } from 'react';
import { useChatStore } from '@/stores/chat-store';
import { Dropdown, DropdownOption } from '@/components/ui/dropdown';
import { trackEvent } from '@/lib/analytics';

export function ModelSelector() {
  const { selectedModel, availableModels, setSelectedModel } = useChatStore();

  const options: DropdownOption[] = useMemo(
    () =>
      availableModels.map((model) => ({
        value: model.id,
        label: model.label,
        description:
          model.maxContextTokens > 0
            ? `${model.provider} - ${Math.round((model.maxContextTokens ?? 0) / 1000)}k contexto`
            : model.provider,
      })),
    [availableModels]
  );

  return (
    <Dropdown
      options={options}
      value={selectedModel}
      onChange={(value) => {
        setSelectedModel(value);
        trackEvent('chat_change_model', { model: value });
      }}
      triggerClassName="h-8 max-w-[220px] text-[12px]"
    />
  );
}
