import type { Metadata, Viewport } from 'next';
import { Geist_Mono } from 'next/font/google';
import { NextIntlClientProvider } from 'next-intl';
import './globals.css';
import { ToastViewport } from '@/components/ui/toast-viewport';
import { ThemeProvider } from '@/components/app/theme-provider';
import { AnalyticsProvider } from '@/components/app/analytics-provider';
import { AuthBootstrap } from '@/components/app/auth-bootstrap';
import { defaultLocale, defaultMessages } from '@/i18n/config';

const monoFont = Geist_Mono({ subsets: ['latin'], variable: '--font-mono' });

export const metadata: Metadata = {
  title: {
    default: 'Lume',
    template: '%s | Lume',
  },
  description: 'Converse com multiplos modelos de IA em um unico lugar.',
  manifest: '/manifest.json',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'default',
    title: 'Lume',
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#f9f9f7' },
    { media: '(prefers-color-scheme: dark)', color: '#191919' },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body className={monoFont.variable}>
        <NextIntlClientProvider locale={defaultLocale} messages={defaultMessages}>
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
