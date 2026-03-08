import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import PromptsPage from './page';

const pushMock = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
  usePathname: () => '/prompts',
}));

describe('PromptsPage', () => {
  beforeEach(() => {
    pushMock.mockReset();
    global.fetch = jest.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);

      if (url.endsWith('/api/v1/chat/conversations')) {
        return {
          ok: true,
          json: async () => ({
            success: true,
            data: {
              conversations: [],
            },
          }),
        } as Response;
      }

      if (url.endsWith('/api/v1/prompts') && (!init?.method || init.method === 'GET')) {
        return {
          ok: true,
          json: async () => ({
            success: true,
            data: {
              templates: [
                {
                  id: 'template-analysis',
                  title: 'Resumo executivo',
                  description: 'Transforma contexto bruto em um resumo objetivo.',
                  prompt:
                    'Crie um resumo executivo estruturado em ate 7 bullets com foco em insights e decisoes estrategicas sobre o seguinte tema:',
                  scope: 'SYSTEM',
                  category: 'analysis',
                  tag: 'Core',
                  createdAt: '2026-03-08T00:00:00.000Z',
                  updatedAt: '2026-03-08T00:00:00.000Z',
                },
              ],
            },
          }),
        } as Response;
      }

      if (url.endsWith('/api/v1/prompts/template-analysis/use') && init?.method === 'POST') {
        return {
          ok: true,
          json: async () => ({
            success: true,
            data: {
              template: {
                id: 'template-analysis',
                title: 'Resumo executivo',
                description: 'Transforma contexto bruto em um resumo objetivo.',
                prompt:
                  'Crie um resumo executivo estruturado em ate 7 bullets com foco em insights e decisoes estrategicas sobre o seguinte tema:',
                scope: 'SYSTEM',
                category: 'analysis',
                tag: 'Core',
                createdAt: '2026-03-08T00:00:00.000Z',
                updatedAt: '2026-03-08T00:00:00.000Z',
              },
            },
          }),
        } as Response;
      }

      throw new Error(`Unexpected fetch: ${url}`);
    }) as jest.Mock;
  });

  it('loads templates from the backend and navigates to chat with encoded prompt', async () => {
    render(<PromptsPage />);

    expect(await screen.findByText('Resumo executivo')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /usar template/i }));

    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith(
        '/chat?prompt=Crie%20um%20resumo%20executivo%20estruturado%20em%20ate%207%20bullets%20com%20foco%20em%20insights%20e%20decisoes%20estrategicas%20sobre%20o%20seguinte%20tema%3A',
      );
    });
  });
});
