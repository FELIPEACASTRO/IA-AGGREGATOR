import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type Theme = 'light' | 'dark' | 'system';
export type ChatFontMode = 'default' | 'sans' | 'system' | 'dyslexia';

interface ThemeState {
  theme: Theme;
  resolvedTheme: 'light' | 'dark';
  chatFontMode: ChatFontMode;
  setTheme: (theme: Theme) => void;
  setChatFontMode: (chatFontMode: ChatFontMode) => void;
}

const getSystemTheme = (): 'light' | 'dark' => {
  if (typeof window === 'undefined') return 'dark';
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

const applyAppearance = (
  resolved: 'light' | 'dark',
  chatFontMode: ChatFontMode,
) => {
  if (typeof document === 'undefined') return;
  document.documentElement.setAttribute('data-theme', resolved);
  document.documentElement.setAttribute('data-chat-font', chatFontMode);
};

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      theme: 'dark',
      resolvedTheme: 'dark',
      chatFontMode: 'default',

      setTheme: (theme: Theme) => {
        const resolved = theme === 'system' ? getSystemTheme() : theme;
        applyAppearance(resolved, get().chatFontMode);
        set({ theme, resolvedTheme: resolved });
      },
      setChatFontMode: (chatFontMode: ChatFontMode) => {
        const currentTheme = get().theme;
        const resolved: 'light' | 'dark' =
          currentTheme === 'system' ? getSystemTheme() : currentTheme;
        applyAppearance(resolved, chatFontMode);
        set({ chatFontMode });
      },
    }),
    {
      name: 'lume-theme',
      onRehydrateStorage: () => (state) => {
        if (!state) return;
        const resolved =
          state.theme === 'system' ? getSystemTheme() : state.theme;
        applyAppearance(resolved, state.chatFontMode ?? 'default');
        state.resolvedTheme = resolved;
        state.chatFontMode = state.chatFontMode ?? 'default';
      },
    }
  )
);
