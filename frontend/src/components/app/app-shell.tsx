'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode, useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuthStore } from '@/stores/auth-store';
import { useThemeStore } from '@/stores/theme-store';
import { cn } from '@/lib/cn';
import { Avatar } from '@/components/ui/avatar';
import { CommandPalette, useCommandPalette } from '@/components/ui/command-palette';
import { useTranslations } from 'next-intl';
import {
  Bot,
  House,
  MessageSquare,
  BookOpen,
  Zap,
  CreditCard,
  Settings,
  LogOut,
  Sun,
  Moon,
  Monitor,
  ChevronsLeft,
  Search,
  ChevronRight,
  Command,
} from 'lucide-react';

/* ═══════════════════════════════════════════════════════
   TYPES
   ═══════════════════════════════════════════════════════ */

type AppShellProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
  headerActions?: ReactNode;
  noPadding?: boolean;
};

/* ═══════════════════════════════════════════════════════
   NAV ITEMS — 5 primary + 2 utility (settings, billing)
   Reduces Hick's Law cognitive load
   ═══════════════════════════════════════════════════════ */

const primaryNav = [
  { href: '/home', labelKey: 'shell.nav.home', icon: House },
  { href: '/chat', labelKey: 'shell.nav.chat', icon: MessageSquare },
  { href: '/library', labelKey: 'shell.nav.library', icon: BookOpen },
  { href: '/prompts', labelKey: 'shell.nav.prompts', icon: Zap },
];

const utilityNav = [
  { href: '/billing', labelKey: 'shell.nav.billing', icon: CreditCard },
  { href: '/settings', labelKey: 'shell.nav.settings', icon: Settings },
];

/* ═══════════════════════════════════════════════════════
   SIDEBAR STATE HOOK
   ═══════════════════════════════════════════════════════ */

const SIDEBAR_KEY = 'lume-sidebar-collapsed';

function useSidebarState() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem(SIDEBAR_KEY);
    if (saved === 'true') setCollapsed(true);
  }, []);

  const toggle = useCallback(() => {
    setCollapsed((current) => {
      const next = !current;
      localStorage.setItem(SIDEBAR_KEY, String(next));
      return next;
    });
  }, []);

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'b') {
        event.preventDefault();
        toggle();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [toggle]);

  return { collapsed, toggle, mobileOpen, setMobileOpen };
}

/* ═══════════════════════════════════════════════════════
   HEALTH STATUS HOOK
   ═══════════════════════════════════════════════════════ */

function useHealthStatus() {
  const [status, setStatus] = useState<'online' | 'degraded' | 'offline'>('offline');

  useEffect(() => {
    let canceled = false;
    const fetchHealth = async () => {
      try {
        const response = await fetch('/api/health/status', { cache: 'no-store' });
        const payload = (await response.json()) as { data?: { status?: 'online' | 'degraded' | 'offline' } };
        if (!canceled && payload.data?.status) setStatus(payload.data.status);
      } catch {
        if (!canceled) setStatus('offline');
      }
    };
    fetchHealth();
    const timer = setInterval(fetchHealth, 15_000);
    return () => { canceled = true; clearInterval(timer); };
  }, []);

  return status;
}

/* ═══════════════════════════════════════════════════════
   THEME CYCLER
   ═══════════════════════════════════════════════════════ */

function ThemeCycler({ collapsed = false }: { collapsed?: boolean }) {
  const { theme, setTheme } = useThemeStore();
  const next = () => setTheme(theme === 'system' ? 'light' : theme === 'light' ? 'dark' : 'system');
  const Icon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor;

  return (
    <button
      onClick={next}
      className={cn(
        'flex items-center justify-center rounded-[var(--radius-md)] text-[var(--foreground-muted)] transition-colors hover:bg-[rgba(255,255,255,0.04)] hover:text-[var(--foreground)]',
        collapsed ? 'h-10 w-10' : 'h-9 w-9'
      )}
      aria-label={`Tema: ${theme}`}
    >
      <Icon className="h-4 w-4" />
    </button>
  );
}

/* ═══════════════════════════════════════════════════════
   NAV LINK — with active accent bar
   ═══════════════════════════════════════════════════════ */

function NavLink({
  href,
  icon: Icon,
  label,
  active,
  collapsed,
  onClick,
}: {
  href: string;
  icon: React.ElementType;
  label: string;
  active: boolean;
  collapsed: boolean;
  onClick?: () => void;
}) {
  return (
    <Link
      href={href}
      onClick={onClick}
      title={collapsed ? label : undefined}
      className={cn(
        'group relative flex items-center gap-3 rounded-[var(--radius-md)] px-3 py-2.5 transition-all',
        active
          ? 'bg-[rgba(124,106,255,0.08)] text-[var(--foreground)]'
          : 'text-[var(--foreground-muted)] hover:bg-[rgba(255,255,255,0.03)] hover:text-[var(--foreground)]',
        collapsed && 'justify-center px-0'
      )}
      aria-current={active ? 'page' : undefined}
    >
      {active && (
        <motion.span
          layoutId="nav-active-bar"
          className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r-full bg-[var(--brand-primary)]"
          transition={{ type: 'spring', stiffness: 380, damping: 30 }}
        />
      )}
      <Icon className={cn('h-[18px] w-[18px] shrink-0 transition-colors', active && 'text-[var(--brand-primary)]')} />
      {!collapsed && (
        <span className="truncate text-[0.84rem] font-medium">{label}</span>
      )}
    </Link>
  );
}

/* ═══════════════════════════════════════════════════════
   SIDEBAR CONTENT
   ═══════════════════════════════════════════════════════ */

function SidebarContent({
  collapsed,
  pathname,
  onNavigate,
  onLogout,
}: {
  collapsed: boolean;
  pathname: string;
  onNavigate?: () => void;
  onLogout: () => void;
}) {
  const t = useTranslations();
  const user = useAuthStore((state) => state.user);
  const { setOpen: setCmdOpen } = useCommandPalette();
  const healthStatus = useHealthStatus();
  const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

  return (
    <div className="flex h-full flex-col">
      {/* ── Logo + Status ── */}
      <div className={cn('flex items-center gap-3 px-4 py-5', collapsed && 'justify-center px-2')}>
        <Link href="/chat" onClick={onNavigate} className="group relative inline-flex shrink-0">
          <span className="inline-flex h-10 w-10 items-center justify-center rounded-[14px] bg-[var(--brand-gradient)] text-white shadow-[var(--shadow-brand)] transition-shadow group-hover:shadow-[var(--brand-glow)]">
            <Bot className="h-[18px] w-[18px]" />
          </span>
          <span
            className={cn(
              'absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full border-2 border-[var(--card)]',
              healthStatus === 'online' ? 'bg-[var(--success)]' : healthStatus === 'degraded' ? 'bg-[var(--warning)]' : 'bg-[var(--foreground-muted)]'
            )}
            title={`Status: ${healthStatus}`}
          />
        </Link>
        {!collapsed && (
          <div className="min-w-0">
            <span className="block text-[0.92rem] font-bold tracking-[-0.04em] text-[var(--foreground)]">Lume</span>
            <span className="block text-[0.64rem] font-semibold uppercase tracking-[0.16em] text-[var(--foreground-muted)]">AI Workspace</span>
          </div>
        )}
      </div>

      {/* ── Search trigger ── */}
      <div className={cn('px-3', collapsed && 'px-2')}>
        <button
          onClick={() => setCmdOpen(true)}
          className={cn(
            'flex w-full items-center gap-2.5 rounded-[var(--radius-md)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] text-[var(--foreground-muted)] transition-all hover:border-[var(--border-hover)] hover:bg-[rgba(255,255,255,0.04)]',
            collapsed ? 'justify-center p-2.5' : 'px-3 py-2.5'
          )}
          aria-label={t('shell.openSearch')}
        >
          <Search className="h-3.5 w-3.5 shrink-0" />
          {!collapsed && (
            <>
              <span className="flex-1 text-left text-[0.78rem]">{t('shell.search')}</span>
              <kbd className="hidden items-center gap-0.5 rounded-md border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-1.5 py-0.5 text-[0.6rem] font-medium text-[var(--foreground-muted)] lg:inline-flex">
                <Command className="h-2.5 w-2.5" />K
              </kbd>
            </>
          )}
        </button>
      </div>

      {/* ── Primary Navigation ── */}
      <nav className="mt-4 flex-1 space-y-0.5 overflow-y-auto px-3" aria-label={t('shell.mainNavigation')}>
        <div className={cn('mb-3', !collapsed && 'px-3')}>
          {!collapsed && (
            <span className="text-[0.62rem] font-semibold uppercase tracking-[0.18em] text-[var(--foreground-muted)]">Menu</span>
          )}
        </div>
        {primaryNav.map(({ href, labelKey, icon }) => (
          <NavLink
            key={href}
            href={href}
            icon={icon}
            label={t(labelKey)}
            active={isActive(href)}
            collapsed={collapsed}
            onClick={onNavigate}
          />
        ))}

        <div className={cn('my-3 h-px bg-[var(--border)]', collapsed && 'mx-2')} />

        {utilityNav.map(({ href, labelKey, icon }) => (
          <NavLink
            key={href}
            href={href}
            icon={icon}
            label={t(labelKey)}
            active={isActive(href)}
            collapsed={collapsed}
            onClick={onNavigate}
          />
        ))}
      </nav>

      {/* ── Footer: Theme + User ── */}
      <div className={cn('border-t border-[var(--border)] p-3 space-y-2', collapsed && 'px-2')}>
        <div className={cn('flex items-center', collapsed ? 'justify-center' : 'justify-between px-1')}>
          <ThemeCycler collapsed={collapsed} />
          {!collapsed && (
            <button
              onClick={onLogout}
              className="flex h-9 w-9 items-center justify-center rounded-[var(--radius-md)] text-[var(--foreground-muted)] transition-colors hover:bg-[rgba(255,92,111,0.06)] hover:text-[var(--destructive)]"
              aria-label={t('shell.logout')}
              title={t('shell.logout')}
            >
              <LogOut className="h-4 w-4" />
            </button>
          )}
        </div>

        <div className={cn(
          'flex items-center gap-3 rounded-[var(--radius-md)] p-2 transition-colors hover:bg-[rgba(255,255,255,0.03)]',
          collapsed && 'justify-center p-1.5'
        )}>
          <Avatar name={user?.fullName || 'Lume'} size="sm" />
          {!collapsed && (
            <div className="min-w-0 flex-1">
              <p className="truncate text-[0.8rem] font-semibold text-[var(--foreground)]">{user?.fullName || 'Conta Lume'}</p>
              <p className="truncate text-[0.66rem] text-[var(--foreground-muted)]">{user?.email}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════
   MOBILE BOTTOM BAR
   ═══════════════════════════════════════════════════════ */

function MobileBottomBar({ pathname }: { pathname: string }) {
  const t = useTranslations();
  const mobileItems = [...primaryNav, ...utilityNav].slice(0, 5);
  const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

  return (
    <nav
      className="fixed bottom-0 left-0 right-0 z-[var(--z-sticky)] border-t border-[var(--border)] bg-[var(--card)] pb-[env(safe-area-inset-bottom)] backdrop-blur-xl md:hidden"
      aria-label={t('shell.quickNavigation')}
    >
      <div className="flex items-center justify-around px-2 py-1.5">
        {mobileItems.map(({ href, labelKey, icon: Icon }) => {
          const label = t(labelKey);
          const active = isActive(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                'relative flex flex-col items-center gap-1 px-3 py-2 transition-colors',
                active ? 'text-[var(--brand-primary)]' : 'text-[var(--foreground-muted)]'
              )}
              aria-current={active ? 'page' : undefined}
            >
              {active && (
                <motion.span
                  layoutId="mobile-active-dot"
                  className="absolute -top-1.5 left-1/2 h-[3px] w-5 -translate-x-1/2 rounded-full bg-[var(--brand-primary)]"
                  transition={{ type: 'spring', stiffness: 400, damping: 28 }}
                />
              )}
              <Icon className="h-[18px] w-[18px]" />
              <span className="text-[0.58rem] font-semibold">{label.split(' ')[0]}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}

/* ═══════════════════════════════════════════════════════
   MOBILE DRAWER
   ═══════════════════════════════════════════════════════ */

function MobileDrawer({
  open,
  onClose,
  children,
}: {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
}) {
  const t = useTranslations();
  const drawerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      document.body.style.overflow = '';
      return;
    }
    document.body.style.overflow = 'hidden';

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleEscape);
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleEscape);
    };
  }, [open, onClose]);

  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            className="fixed inset-0 z-[var(--z-modal)] bg-[rgba(7,7,14,0.7)] backdrop-blur-sm md:hidden"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onClose}
            aria-label={t('shell.closeMenu')}
          />
          <motion.aside
            ref={drawerRef}
            role="dialog"
            aria-modal="true"
            aria-label={t('shell.menu')}
            className="fixed inset-y-0 left-0 z-[calc(var(--z-modal)+1)] flex w-[280px] flex-col border-r border-[var(--border)] bg-[var(--card)] shadow-[var(--shadow-xl)] md:hidden"
            initial={{ x: '-100%' }}
            animate={{ x: 0 }}
            exit={{ x: '-100%' }}
            transition={{ type: 'spring', stiffness: 400, damping: 32 }}
          >
            {children}
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
}

/* ═══════════════════════════════════════════════════════
   BREADCRUMB
   ═══════════════════════════════════════════════════════ */

function Breadcrumb({ pathname }: { pathname: string }) {
  const t = useTranslations();
  const crumbs = useMemo(() => {
    const allNav = [...primaryNav, ...utilityNav];
    const segments = pathname.split('/').filter(Boolean);
    if (segments.length <= 1) return null;

    const parts: { label: string; href: string }[] = [];
    const baseNav = allNav.find((nav) => `/${segments[0]}` === nav.href);
    if (baseNav) {
      parts.push({ label: t(baseNav.labelKey), href: baseNav.href });
    }

    if (segments.length > 1) {
      if (pathname.startsWith('/settings/analytics')) {
        parts.push({ label: t('shell.breadcrumb.analytics'), href: '/settings/analytics' });
      }
    }

    return parts.length > 0 ? parts : null;
  }, [pathname, t]);

  if (!crumbs) return null;

  return (
    <div className="flex items-center gap-1.5 text-[0.72rem] text-[var(--foreground-muted)]">
      {crumbs.map((crumb, index) => (
        <span key={crumb.href} className="flex items-center gap-1.5">
          {index > 0 && <ChevronRight className="h-3 w-3" />}
          <Link href={crumb.href} className="hover:text-[var(--foreground)] transition-colors">
            {crumb.label}
          </Link>
        </span>
      ))}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════
   APP SHELL — Main Export
   ═══════════════════════════════════════════════════════ */

export function AppShell({ title, subtitle, children, headerActions, noPadding }: AppShellProps) {
  const t = useTranslations();
  const pathname = usePathname();
  const router = useRouter();
  const { collapsed, toggle, mobileOpen, setMobileOpen } = useSidebarState();
  const { open: cmdOpen, setOpen: setCmdOpen } = useCommandPalette();

  useEffect(() => { setMobileOpen(false); }, [pathname, setMobileOpen]);

  const handleLogout = () => {
    useAuthStore.getState().logout();
    router.push('/login');
  };

  return (
    <div className="flex h-screen overflow-hidden bg-[var(--background)]">
      {/* ── Desktop Sidebar ── */}
      <motion.aside
        className="relative hidden flex-col border-r border-[var(--border)] bg-[var(--card)] md:flex"
        animate={{ width: collapsed ? 72 : 260 }}
        transition={{ type: 'spring', stiffness: 400, damping: 32 }}
      >
        <SidebarContent collapsed={collapsed} pathname={pathname} onLogout={handleLogout} />

        {/* Collapse toggle */}
        <button
          onClick={toggle}
          className="absolute -right-3 top-7 z-10 hidden h-6 w-6 items-center justify-center rounded-full border border-[var(--border)] bg-[var(--card)] text-[var(--foreground-muted)] shadow-[var(--shadow-sm)] transition-colors hover:bg-[var(--card-hover)] hover:text-[var(--foreground)] md:inline-flex"
          aria-label={collapsed ? t('shell.expandMenu') : t('shell.collapseMenu')}
        >
          <ChevronsLeft className={cn('h-3 w-3 transition-transform', collapsed && 'rotate-180')} />
        </button>
      </motion.aside>

      {/* ── Mobile Drawer ── */}
      <MobileDrawer open={mobileOpen} onClose={() => setMobileOpen(false)}>
        <SidebarContent collapsed={false} pathname={pathname} onNavigate={() => setMobileOpen(false)} onLogout={handleLogout} />
      </MobileDrawer>

      {/* ── Main Content Area ── */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* ── Header ── */}
        <header className="flex items-center justify-between border-b border-[var(--border)] bg-[var(--card)] px-5 py-4 md:px-8">
          <div className="flex items-center gap-4">
            {/* Mobile hamburger */}
            <button
              onClick={() => setMobileOpen(true)}
              className="inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-md)] border border-[var(--border)] text-[var(--foreground)] md:hidden"
              aria-label={t('shell.openMenu')}
            >
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
                <line x1="3" y1="5" x2="15" y2="5" />
                <line x1="3" y1="9" x2="12" y2="9" />
                <line x1="3" y1="13" x2="15" y2="13" />
              </svg>
            </button>

            <div>
              <Breadcrumb pathname={pathname} />
              <h1 className="text-[var(--text-xl)] font-bold tracking-[-0.03em] text-[var(--foreground)]">{title}</h1>
              {subtitle && <p className="mt-0.5 text-[var(--text-sm)] text-[var(--foreground-secondary)]">{subtitle}</p>}
            </div>
          </div>

          <div className="flex items-center gap-2">
            {headerActions}
          </div>
        </header>

        {/* ── Main Content ── */}
        <main
          id="workspace-main-content"
          data-main-content
          className={cn(
            'flex-1 overflow-y-auto',
            noPadding ? '' : 'px-5 py-6 md:px-8'
          )}
        >
          {children}
        </main>
      </div>

      {/* ── Mobile Bottom Bar ── */}
      <MobileBottomBar pathname={pathname} />

      {/* ── Command Palette (global) ── */}
      <CommandPalette open={cmdOpen} onClose={() => setCmdOpen(false)} />
    </div>
  );
}
