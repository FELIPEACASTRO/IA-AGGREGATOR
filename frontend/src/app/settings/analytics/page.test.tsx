import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import AnalyticsDiagnosticsPage from './page';

const getTrackedEventsMock = jest.fn();
const clearTrackedEventsMock = jest.fn();
const flushTrackedEventsMock = jest.fn();
const apiGetMock = jest.fn();
const toastInfoMock = jest.fn();
const toastSuccessMock = jest.fn();
const toastErrorMock = jest.fn();

const authState = {
  user: {
    fullName: 'Docker Admin',
    email: 'dockeradmin@ia-aggregator.local',
  },
  logout: jest.fn(),
};

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
  usePathname: () => '/settings/analytics',
}));

jest.mock('@/stores/auth-store', () => ({
  useAuthStore: Object.assign((selector: (state: typeof authState) => unknown) => selector(authState), {
    getState: () => authState,
  }),
}));

jest.mock('@/lib/analytics', () => ({
  getTrackedEvents: () => getTrackedEventsMock(),
  clearTrackedEvents: () => clearTrackedEventsMock(),
  flushTrackedEvents: (...args: unknown[]) => flushTrackedEventsMock(...args),
}));

jest.mock('@/lib/api', () => ({
  __esModule: true,
  default: {
    get: (...args: unknown[]) => apiGetMock(...args),
  },
}));

jest.mock('@/stores/toast-store', () => ({
  toast: {
    info: (...args: unknown[]) => toastInfoMock(...args),
    success: (...args: unknown[]) => toastSuccessMock(...args),
    error: (...args: unknown[]) => toastErrorMock(...args),
  },
}));

describe('AnalyticsDiagnosticsPage', () => {
  beforeEach(() => {
    getTrackedEventsMock.mockReset();
    clearTrackedEventsMock.mockReset();
    flushTrackedEventsMock.mockReset();
    apiGetMock.mockReset();
    toastInfoMock.mockReset();
    toastSuccessMock.mockReset();
    toastErrorMock.mockReset();

    apiGetMock.mockResolvedValue({ data: { data: [] } });
    URL.createObjectURL = jest.fn(() => 'blob:analytics-history');
    URL.revokeObjectURL = jest.fn();
    localStorage.clear();
  });

  it('renders tracked events and summary counters', () => {
    getTrackedEventsMock.mockReturnValue([
      { event: 'chat_send_start', timestamp: new Date().toISOString(), metadata: { model: 'gpt-4o-mini' } },
      { event: 'chat_send_start', timestamp: new Date().toISOString() },
      { event: 'chat_send_success', timestamp: new Date().toISOString(), metadata: { latencyMs: 420 } },
    ]);

    render(<AnalyticsDiagnosticsPage />);

    expect(screen.getByText(/Total de eventos: 3/)).toBeInTheDocument();
    expect(screen.getAllByText('chat_send_start').length).toBeGreaterThan(0);
    expect(screen.getByText('2 ocorrência(s)')).toBeInTheDocument();
  });

  it('refreshes and clears events', () => {
    getTrackedEventsMock
      .mockReturnValueOnce([{ event: 'chat_retry', timestamp: new Date().toISOString() }])
      .mockReturnValueOnce([{ event: 'chat_retry', timestamp: new Date().toISOString() }]);

    render(<AnalyticsDiagnosticsPage />);

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }));
    expect(toastInfoMock).toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Limpar eventos' }));
    expect(clearTrackedEventsMock).toHaveBeenCalledTimes(1);
    expect(toastSuccessMock).toHaveBeenCalled();
  });

  it('sends analytics report successfully', async () => {
    getTrackedEventsMock.mockReturnValue([{ event: 'chat_retry', timestamp: new Date().toISOString() }]);
    flushTrackedEventsMock.mockResolvedValueOnce(true);

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Enviar relatório' }));

    await waitFor(() => {
      expect(flushTrackedEventsMock).toHaveBeenCalledTimes(1);
      expect(flushTrackedEventsMock).toHaveBeenCalledWith('frontend-manual-1');
      expect(toastSuccessMock).toHaveBeenCalledWith(
        'Relatório enviado',
        'Eventos enviados para o endpoint de analytics.'
      );
    });
  });

  it('shows error toast when report submission fails', async () => {
    getTrackedEventsMock.mockReturnValue([{ event: 'chat_retry', timestamp: new Date().toISOString() }]);
    flushTrackedEventsMock.mockResolvedValue(false);

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Enviar relatório' }));

    await waitFor(() => {
      expect(flushTrackedEventsMock).toHaveBeenCalledTimes(3);
      expect(toastErrorMock).toHaveBeenCalledWith(
        'Falha ao enviar relatório',
        'Configure o endpoint e tente novamente.'
      );
    }, { timeout: 3000 });
  });

  it('retries transient failures and succeeds on a later attempt', async () => {
    getTrackedEventsMock.mockReturnValue([{ event: 'chat_retry', timestamp: new Date().toISOString() }]);
    flushTrackedEventsMock
      .mockResolvedValueOnce(false)
      .mockResolvedValueOnce(false)
      .mockResolvedValueOnce(true);

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Enviar relatório' }));

    await waitFor(() => {
      expect(flushTrackedEventsMock).toHaveBeenCalledTimes(3);
      expect(toastSuccessMock).toHaveBeenCalledWith(
        'Relatório enviado',
        'Eventos enviados para o endpoint de analytics.'
      );
    });
  });

  it('does not attempt to send when there are no events', async () => {
    getTrackedEventsMock.mockReturnValue([]);

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Enviar relatório' }));

    await waitFor(() => {
      expect(flushTrackedEventsMock).not.toHaveBeenCalled();
    });
  });

  it('loads persisted reports from backend', async () => {
    getTrackedEventsMock.mockReturnValue([]);
    apiGetMock.mockResolvedValueOnce({
      data: {
        data: [
          {
            id: 'r-1',
            source: 'frontend',
            totalEvents: 4,
            receivedAt: new Date().toISOString(),
          },
        ],
      },
    });

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Carregar histórico' }));

    await waitFor(() => {
      expect(apiGetMock).toHaveBeenCalledTimes(1);
      expect(screen.getByText(/frontend · 4 evento\(s\)/i)).toBeInTheDocument();
      expect(toastSuccessMock).toHaveBeenCalledWith('Histórico carregado', '1 relatório(s) persistido(s).');
    });

    const requestUrl = String(apiGetMock.mock.calls[0][0]);
    expect(requestUrl).toContain('page=0');
  });

  it('applies date range and sort controls when loading persisted reports', async () => {
    getTrackedEventsMock.mockReturnValue([]);
    apiGetMock.mockResolvedValueOnce({ data: { data: [] } });

    render(<AnalyticsDiagnosticsPage />);

    fireEvent.change(screen.getByLabelText('Data inicial do histórico'), {
      target: { value: '2026-03-01' },
    });
    fireEvent.change(screen.getByLabelText('Data final do histórico'), {
      target: { value: '2026-03-02' },
    });
    fireEvent.change(screen.getByLabelText('Ordenar histórico por'), {
      target: { value: 'totalEvents' },
    });
    fireEvent.change(screen.getByLabelText('Direção da ordenação do histórico'), {
      target: { value: 'asc' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Carregar histórico' }));

    await waitFor(() => {
      expect(apiGetMock).toHaveBeenCalledTimes(1);
    });

    const requestUrl = String(apiGetMock.mock.calls[0][0]);
    expect(requestUrl).toContain('page=0');
    expect(requestUrl).toContain('from=2026-03-01T00%3A00%3A00Z');
    expect(requestUrl).toContain('to=2026-03-02T23%3A59%3A59Z');
    expect(requestUrl).toContain('sortBy=totalEvents');
    expect(requestUrl).toContain('sortDir=asc');
  });

  it('loads more persisted reports when requesting next page', async () => {
    getTrackedEventsMock.mockReturnValue([]);
    apiGetMock
      .mockResolvedValueOnce({
        data: {
          data: new Array(20).fill(null).map((_, index) => ({
            id: `r-${index}`,
            source: 'frontend',
            totalEvents: index + 1,
            receivedAt: new Date().toISOString(),
          })),
        },
      })
      .mockResolvedValueOnce({
        data: {
          data: [
            {
              id: 'r-next',
              source: 'frontend',
              totalEvents: 99,
              receivedAt: new Date().toISOString(),
            },
          ],
        },
      });

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Carregar histórico' }));

    await waitFor(() => {
      expect(apiGetMock).toHaveBeenCalledTimes(1);
    });

    fireEvent.click(screen.getByRole('button', { name: 'Carregar mais histórico' }));

    await waitFor(() => {
      expect(apiGetMock).toHaveBeenCalledTimes(2);
      expect(screen.getByText(/frontend · 99 evento\(s\)/i)).toBeInTheDocument();
      expect(toastSuccessMock).toHaveBeenCalledWith('Mais histórico carregado', '1 relatório(s) persistido(s).');
    });

    expect(String(apiGetMock.mock.calls[0][0])).toContain('page=0');
    expect(String(apiGetMock.mock.calls[1][0])).toContain('page=1');
  });

  it('shows error when persisted reports loading fails', async () => {
    getTrackedEventsMock.mockReturnValue([]);
    apiGetMock.mockRejectedValueOnce(new Error('Network error'));

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Carregar histórico' }));

    await waitFor(() => {
      expect(toastErrorMock).toHaveBeenCalledWith(
        'Falha ao carregar histórico',
        'Não foi possível buscar relatórios persistidos.'
      );
    });
  });

  it('loads persisted report events on drill-down', async () => {
    getTrackedEventsMock.mockReturnValue([]);
    apiGetMock
      .mockResolvedValueOnce({
        data: {
          data: [
            {
              id: 'r-1',
              source: 'frontend',
              totalEvents: 2,
              receivedAt: new Date().toISOString(),
            },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          data: [
            {
              id: 'e-1',
              reportId: 'r-1',
              eventName: 'chat_send_start',
              eventCategory: 'chat',
              eventTimestamp: new Date().toISOString(),
              metadata: { model: 'gpt-4o-mini' },
            },
          ],
        },
      });

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Carregar histórico' }));

    await waitFor(() => {
      expect(screen.getByText(/frontend · 2 evento\(s\)/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }));

    await waitFor(() => {
      expect(screen.getByText('chat_send_start')).toBeInTheDocument();
      expect(toastSuccessMock).toHaveBeenCalledWith('Detalhes carregados', '1 evento(s) do relatório.');
      expect(apiGetMock).toHaveBeenCalledTimes(2);
    });

    expect(String(apiGetMock.mock.calls[1][0])).toContain('limit=50');
    expect(String(apiGetMock.mock.calls[1][0])).toContain('offset=0');
  });

  it('shows error when report detail loading fails', async () => {
    getTrackedEventsMock.mockReturnValue([]);
    apiGetMock
      .mockResolvedValueOnce({
        data: {
          data: [
            {
              id: 'r-1',
              source: 'frontend',
              totalEvents: 2,
              receivedAt: new Date().toISOString(),
            },
          ],
        },
      })
      .mockRejectedValueOnce(new Error('Request failed'));

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Carregar histórico' }));

    await waitFor(() => {
      expect(screen.getByText(/frontend · 2 evento\(s\)/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }));

    await waitFor(() => {
      expect(toastErrorMock).toHaveBeenCalledWith(
        'Falha ao carregar detalhes',
        'Não foi possível buscar eventos do relatório.'
      );
    });
  });

  it('exports persisted reports as CSV', async () => {
    getTrackedEventsMock.mockReturnValue([]);
    const clickSpy = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    apiGetMock.mockResolvedValueOnce({
      data: {
        data: [
          {
            id: 'r-1',
            source: 'frontend',
            totalEvents: 4,
            receivedAt: '2026-03-04T12:00:00Z',
            counters: { chat_send_start: 2 },
          },
        ],
      },
    });

    render(<AnalyticsDiagnosticsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Carregar histórico' }));

    await waitFor(() => {
      expect(screen.getByText(/frontend · 4 evento\(s\)/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Exportar histórico CSV' }));

    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalledTimes(1);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:analytics-history');
    expect(toastSuccessMock).toHaveBeenCalledWith('CSV exportado', '1 relatório(s) exportado(s).');

    clickSpy.mockRestore();
  });
});
