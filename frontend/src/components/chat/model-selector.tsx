'use client';

import { useMemo } from 'react';
import { useChatStore } from '@/stores/chat-store';
import { Dropdown, DropdownOption } from '@/components/ui/dropdown';
import { trackEvent } from '@/lib/analytics';

export function ModelSelector() {
  const { selectedModel, availableModels, setSelectedModel } = useChatStore();

  const options: DropdownOption[] = useMemo(
    () =>
      availableModels.map((m) => ({
        value: m.id,
        label: m.label,
        description: m.provider,
      })),
    [availableModels],
  );

  return (
    <Dropdown
      options={options}
      value={selectedModel}
      onChange={(val) => {
        setSelectedModel(val);
        trackEvent('chat_change_model', { model: val });
      }}
      triggerClassName="h-8 text-[13px]"
    />
  );
}
