import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import PromptsPage from './page';

const pushMock = jest.fn();
const fetchMock = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
  usePathname: () => '/prompts',
}));

describe('PromptsPage', () => {
  beforeEach(() => {
    pushMock.mockReset();
    fetchMock.mockReset();
    fetchMock.mockResolvedValue({
      ok: false,
      status: 503,
      json: async () => [],
    });
    global.fetch = fetchMock as unknown as typeof fetch;
  });

  it('navigates to chat with encoded prompt when using template', async () => {
    render(<PromptsPage />);

    const buttons = await screen.findAllByRole('button', { name: /usar template/i });
    fireEvent.click(buttons[0]);

    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith(
        '/chat?prompt=Crie%20um%20resumo%20executivo%20estruturado%20em%20at%C3%A9%207%20bullets%20com%20foco%20em%20insights%20e%20decis%C3%B5es%20estrat%C3%A9gicas%20sobre%20o%20seguinte%20tema%3A'
      );
    });
  });
});
