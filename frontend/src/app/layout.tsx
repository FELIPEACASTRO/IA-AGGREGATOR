import type { Metadata, Viewport } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.css';
import { ToastViewport } from '@/components/ui/toast-viewport';
import { ThemeProvider } from '@/components/app/theme-provider';
import { AnalyticsProvider } from '@/components/app/analytics-provider';

const geist = Geist({ subsets: ['latin'], variable: '--font-geist' });
const geistMono = Geist_Mono({ subsets: ['latin'], variable: '--font-geist-mono' });

export const metadata: Metadata = {
  title: 'IA Aggregator',
  description: 'Plataforma de agregação de provedores de IA',
  manifest: '/manifest.json',
  appleWebApp: { capable: true, statusBarStyle: 'black-translucent', title: 'IA Aggregator' },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#7C3AED' },
    { media: '(prefers-color-scheme: dark)', color: '#0A0A0F' },
  ],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body className={`${geist.variable} ${geistMono.variable}`}>
        <a href="#main-content" className="skip-to-content">
          Pular para o conteúdo
        </a>
        <ThemeProvider>
          <AnalyticsProvider />
          {children}
        </ThemeProvider>
        <ToastViewport />
      </body>
    </html>
  );
}
