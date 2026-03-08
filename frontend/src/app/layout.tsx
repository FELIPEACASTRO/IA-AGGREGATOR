import type { Metadata, Viewport } from 'next';
import {
  Atkinson_Hyperlegible,
  Instrument_Sans,
  JetBrains_Mono,
  Newsreader,
} from 'next/font/google';
import { NextIntlClientProvider } from 'next-intl';
import './globals.css';
import { ToastViewport } from '@/components/ui/toast-viewport';
import { ThemeProvider } from '@/components/app/theme-provider';
import { AnalyticsProvider } from '@/components/app/analytics-provider';
import { AuthBootstrap } from '@/components/app/auth-bootstrap';
import { defaultLocale, defaultMessages } from '@/i18n/config';

const uiFont = Instrument_Sans({ subsets: ['latin'], variable: '--font-ui' });
const displayFont = Newsreader({
  subsets: ['latin'],
  variable: '--font-display',
  weight: ['400', '500', '600'],
});
const monoFont = JetBrains_Mono({ subsets: ['latin'], variable: '--font-mono' });
const dyslexiaFont = Atkinson_Hyperlegible({
  subsets: ['latin'],
  variable: '--font-dyslexia',
  weight: ['400', '700'],
});

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
    { media: '(prefers-color-scheme: light)', color: '#FAF9F5' },
    { media: '(prefers-color-scheme: dark)', color: '#262624' },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body className={`${uiFont.variable} ${displayFont.variable} ${monoFont.variable} ${dyslexiaFont.variable}`}>
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
