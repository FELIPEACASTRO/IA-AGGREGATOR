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
      '/chat?prompt=Resuma%20o%20texto%20abaixo%20em%20at%C3%A9%207%20bullets%20com%20foco%20executivo%3A'
    );
  });
});
