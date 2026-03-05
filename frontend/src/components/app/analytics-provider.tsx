'use client';

import { useEffect } from 'react';
import { usePathname } from 'next/navigation';
import { initializeAnalyticsLifecycle, trackPageView } from '@/lib/analytics';

export function AnalyticsProvider() {
  const pathname = usePathname();

  useEffect(() => {
    initializeAnalyticsLifecycle();
  }, []);

  useEffect(() => {
    if (!pathname) return;
    trackPageView(pathname);
  }, [pathname]);

  return null;
}
