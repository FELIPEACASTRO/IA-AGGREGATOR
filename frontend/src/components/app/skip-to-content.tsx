'use client';

import { MouseEvent, useCallback, useEffect } from 'react';
import { usePathname } from 'next/navigation';

function ensureMainContentTarget() {
  const existing = document.getElementById('main-content');
  if (existing) return existing as HTMLElement;

  const firstMain = document.querySelector<HTMLElement>('main');
  if (!firstMain) return null;

  firstMain.id = 'main-content';
  return firstMain;
}

export function SkipToContent() {
  const pathname = usePathname();

  useEffect(() => {
    ensureMainContentTarget();
  }, [pathname]);

  const handleClick = useCallback((event: MouseEvent<HTMLAnchorElement>) => {
    const target = ensureMainContentTarget();
    if (!target) return;

    event.preventDefault();

    if (!target.hasAttribute('tabindex')) {
      target.setAttribute('tabindex', '-1');
    }

    target.focus({ preventScroll: true });
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, []);

  return (
    <a href="#main-content" className="skip-to-content" onClick={handleClick}>
      Pular para o conteudo
    </a>
  );
}

