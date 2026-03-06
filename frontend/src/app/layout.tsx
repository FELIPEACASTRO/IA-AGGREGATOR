import type { Metadata, Viewport } from 'next';
import { Plus_Jakarta_Sans, Sora, Geist_Mono } from 'next/font/google';
import './globals.css';
import { ToastViewport } from '@/components/ui/toast-viewport';
import { ThemeProvider } from '@/components/app/theme-provider';
import { AnalyticsProvider } from '@/components/app/analytics-provider';
import { SkipToContent } from '@/components/app/skip-to-content';
import { AuthBootstrap } from '@/components/app/auth-bootstrap';

const bodyFont = Plus_Jakarta_Sans({ subsets: ['latin'], variable: '--font-body' });
const displayFont = Sora({ subsets: ['latin'], variable: '--font-display' });
const monoFont = Geist_Mono({ subsets: ['latin'], variable: '--font-mono' });

export const metadata: Metadata = {
  title: {
    default: 'Lume',
    template: '%s | Lume',
  },
  description: 'Workspace premium para conversar, organizar conhecimento e executar fluxos com IA em um unico lugar.',
  manifest: '/manifest.json',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'black-translucent',
    title: 'Lume',
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#08111f' },
    { media: '(prefers-color-scheme: dark)', color: '#08111f' },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body className={`${bodyFont.variable} ${displayFont.variable} ${monoFont.variable}`}>
        <SkipToContent />
        <ThemeProvider>
          <AuthBootstrap />
          <AnalyticsProvider />
          {children}
        </ThemeProvider>
        <ToastViewport />
      </body>
    </html>
  );
}
