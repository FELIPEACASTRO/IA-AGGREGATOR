import type { Metadata } from 'next';
import './globals.css';
import { ToastViewport } from '@/components/ui/toast-viewport';

export const metadata: Metadata = {
  title: 'IA Aggregator',
  description: 'Plataforma de agregação de provedores de IA',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR">
      <body>
        {children}
        <ToastViewport />
      </body>
    </html>
  );
}
