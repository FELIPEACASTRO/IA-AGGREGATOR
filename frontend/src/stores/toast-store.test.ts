import { toast, useToastStore } from '@/stores/toast-store';

describe('toast-store', () => {
  beforeEach(() => {
    useToastStore.setState({ toasts: [] });
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('adds and auto-removes toast notifications', () => {
    toast.success('Salvo com sucesso', 'Preferências atualizadas');

    expect(useToastStore.getState().toasts).toHaveLength(1);
    expect(useToastStore.getState().toasts[0].title).toBe('Salvo com sucesso');

    jest.advanceTimersByTime(3500);

    expect(useToastStore.getState().toasts).toHaveLength(0);
  });
});
