'use client';

import { CSS } from '@dnd-kit/utilities';
import { DndContext, DragEndEvent, PointerSensor, closestCenter, useSensor, useSensors } from '@dnd-kit/core';
import { SortableContext, arrayMove, rectSortingStrategy, useSortable } from '@dnd-kit/sortable';
import { Check, Copy, GripVertical } from 'lucide-react';
import { ChatMessage } from '@/stores/chat-store';
import { cn } from '@/lib/cn';
import { MessageContent } from '@/components/chat/message-content';

function CanvasCard({
  message,
  isCopied,
  onCopy,
}: {
  message: ChatMessage;
  isCopied: boolean;
  onCopy: () => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: message.id });

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition, opacity: isDragging ? 0.5 : 1 }}
      className={cn(
        'relative rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-1)] p-4 shadow-[var(--shadow-md)] transition-shadow hover:shadow-[var(--shadow-lg)]',
        message.role === 'user' && 'border-[var(--brand-primary)]/30',
        isDragging && 'shadow-[var(--shadow-xl)] z-10'
      )}
    >
      <div className="mb-3 flex items-center gap-2">
        <div {...attributes} {...listeners} className="cursor-grab touch-none text-[var(--muted-foreground)] active:cursor-grabbing">
          <GripVertical className="h-4 w-4" />
        </div>
        <span
          className={cn(
            'inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-bold',
            message.role === 'user' ? 'bg-[var(--brand-primary)] text-white' : 'bg-[var(--surface-2)] text-[var(--muted-foreground)]'
          )}
        >
          {message.role === 'user' ? 'U' : 'IA'}
        </span>
        <span className="text-[10px] text-[var(--subtle-foreground)]">
          {new Date(message.timestamp).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
        </span>
      </div>

      <div className="line-clamp-6 pl-6 text-[var(--text-sm)] leading-relaxed">
        <MessageContent content={message.content} role={message.role} />
      </div>

      <button
        onClick={onCopy}
        className="absolute right-2 top-2 rounded-md p-1.5 text-[var(--muted-foreground)] transition-colors hover:bg-[var(--surface-2)]"
      >
        {isCopied ? <Check className="h-3 w-3 text-[var(--success)]" /> : <Copy className="h-3 w-3" />}
      </button>
    </div>
  );
}

export function ChatCanvasBoard({
  messages,
  order,
  setOrder,
  copiedId,
  onCopy,
}: {
  messages: ChatMessage[];
  order: string[];
  setOrder: (next: string[]) => void;
  copiedId: string | null;
  onCopy: (id: string, content: string) => void;
}) {
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const canvasMessages = order
    .map((id) => messages.find((message) => message.id === id))
    .filter((message): message is ChatMessage => Boolean(message));

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;

    const from = order.indexOf(String(active.id));
    const to = order.indexOf(String(over.id));
    setOrder(arrayMove(order, from, to));
  };

  return (
    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={order} strategy={rectSortingStrategy}>
        <div className="canvas-viewport min-h-full p-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {canvasMessages.map((message) => (
              <CanvasCard
                key={message.id}
                message={message}
                isCopied={copiedId === message.id}
                onCopy={() => onCopy(message.id, message.content)}
              />
            ))}
          </div>
        </div>
      </SortableContext>
    </DndContext>
  );
}

