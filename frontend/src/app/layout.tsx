import type { Metadata, Viewport } from 'next';
import { Outfit, Bricolage_Grotesque, JetBrains_Mono } from 'next/font/google';
import { NextIntlClientProvider } from 'next-intl';
import './globals.css';
import { ToastViewport } from '@/components/ui/toast-viewport';
import { ThemeProvider } from '@/components/app/theme-provider';
import { AnalyticsProvider } from '@/components/app/analytics-provider';
import { SkipToContent } from '@/components/app/skip-to-content';
import { AuthBootstrap } from '@/components/app/auth-bootstrap';
import { defaultLocale, defaultMessages } from '@/i18n/config';

const bodyFont = Outfit({ subsets: ['latin'], variable: '--font-body', display: 'swap' });
const displayFont = Bricolage_Grotesque({ subsets: ['latin'], variable: '--font-display', display: 'swap' });
const monoFont = JetBrains_Mono({ subsets: ['latin'], variable: '--font-mono', display: 'swap' });

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
    { media: '(prefers-color-scheme: light)', color: '#fafafe' },
    { media: '(prefers-color-scheme: dark)', color: '#07070e' },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body className={`${bodyFont.variable} ${displayFont.variable} ${monoFont.variable}`}>
        <NextIntlClientProvider locale={defaultLocale} messages={defaultMessages}>
          <SkipToContent />
          <ThemeProvider>
            <AuthBootstrap />
            <AnalyticsProvider />
            {children}
          </ThemeProvider>
        </NextIntlClientProvider>
        <ToastViewport />
      </body>
    </html>
  );
}
