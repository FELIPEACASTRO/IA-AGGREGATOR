import { fireEvent, render, screen } from '@testing-library/react';
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
  });

  it('navigates to chat with encoded prompt when using template', () => {
    render(<PromptsPage />);

    const buttons = screen.getAllByRole('button', { name: /usar template/i });
    fireEvent.click(buttons[0]);

    expect(pushMock).toHaveBeenCalledWith(
      '/chat?prompt=Crie%20um%20resumo%20executivo%20estruturado%20em%20at%C3%A9%207%20bullets%20com%20foco%20em%20insights%20e%20decis%C3%B5es%20estrat%C3%A9gicas%20sobre%20o%20seguinte%20tema%3A'
    );
  });
});
