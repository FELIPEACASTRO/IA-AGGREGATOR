'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function HomePage() {
  const router = useRouter();

  useEffect(() => {
    router.replace('/chat');
  }, [router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--background)]">
      <div className="flex gap-1.5">
        <span className="pulse-dot" />
        <span className="pulse-dot" />
        <span className="pulse-dot" />
      </div>
    </div>
  );
}
