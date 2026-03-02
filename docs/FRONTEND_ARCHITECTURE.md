# Frontend Architecture - IA Aggregator Platform

> Multi-Model AI Aggregator for the Brazilian Market
> Next.js 15 + React 19 + TypeScript 5.x
> Version: 1.0.0 | Last Updated: 2026-03-01

---

## Table of Contents

1. [Technology Stack](#1-technology-stack)
2. [Project Structure](#2-project-structure)
3. [Design System](#3-design-system)
4. [UX/UI Best Practices](#4-uxui-best-practices)
5. [Key Features Architecture](#5-key-features-architecture)
6. [Reusability Patterns](#6-reusability-patterns)
7. [Navigation and Routing](#7-navigation-and-routing)
8. [Performance](#8-performance)

---

## 1. Technology Stack

### 1.1 Core Framework

| Technology | Version | Purpose |
|---|---|---|
| Next.js | 15.x (App Router) | Full-stack React framework with RSC, streaming, and edge runtime |
| React | 19.x | UI library with Server Components, Actions, `use()`, and `useOptimistic` |
| TypeScript | 5.x (strict mode) | Type safety across the entire codebase |
| Node.js | 22 LTS | Runtime for server-side rendering and API routes |

**Justification:** Next.js 15 App Router provides React Server Components for zero-bundle server rendering, streaming SSR for chat interfaces, built-in route handlers for BFF (Backend for Frontend) patterns, and middleware for auth/i18n. React 19 delivers `use()` for promise-based data loading, `useOptimistic` for instant UI updates on credit consumption, Server Actions for form mutations, and `useFormStatus`/`useActionState` for form UX.

### 1.2 State Management

| Layer | Tool | Scope |
|---|---|---|
| Server state | TanStack Query v5 | API data, caching, background refetching |
| Client global state | Zustand v5 | Auth session, UI preferences, theme, sidebar |
| Form state | React Hook Form v7 + Zod | Validation, multi-step forms, field-level errors |
| URL state | `nuqs` (type-safe search params) | Filters, pagination, model selection |
| Real-time state | Custom WebSocket store (Zustand middleware) | Chat streaming, credit balance, notifications |

**Why Zustand over Redux/Jotai:**
- Minimal boilerplate (no providers, reducers, or action creators)
- Built-in devtools middleware and persist middleware for theme/locale
- TypeScript-first with full inference
- Smaller bundle (~1KB vs Redux Toolkit ~11KB)
- Perfect for the 4-5 global slices this platform requires
- Zustand slices pattern allows code-splitting stores by feature

**Why TanStack Query for server state:**
- Automatic background refetching keeps credit balances current
- Optimistic updates for instant UX when sending chat messages
- Built-in infinite scroll support for chat history
- Request deduplication prevents duplicate API calls
- Stale-while-revalidate caching for model listings and pricing

### 1.3 Styling

| Tool | Purpose |
|---|---|
| Tailwind CSS v4 | Utility-first CSS with CSS-first configuration |
| Shadcn/UI | Accessible, composable component primitives (copied into project) |
| tailwind-merge | Conflict-free className merging |
| class-variance-authority (CVA) | Type-safe component variant definitions |
| tailwindcss-animate | Animation utilities for transitions |
| @tailwindcss/typography | Prose styling for AI-generated markdown content |

**Tailwind v4 Configuration Strategy:**
Tailwind v4 uses CSS-first configuration. All theme tokens are defined in `app/globals.css` using `@theme` blocks, eliminating `tailwind.config.ts`. CSS custom properties power the dark/light theme switching.

### 1.4 Data Fetching

```
Layer Architecture:

Browser <-> Next.js Route Handlers (BFF) <-> Backend API Gateway
              |
              +-- TanStack Query (client components)
              +-- Server Components (direct fetch with cache)
              +-- Server Actions (mutations)
```

| Pattern | Use Case |
|---|---|
| React Server Components + `fetch` | Initial page data (model catalog, user profile, plan details) |
| TanStack Query `useQuery` | Client-side data that needs revalidation (credit balance, chat list) |
| TanStack Query `useMutation` | Create/update operations (send message, update profile) |
| TanStack Query `useInfiniteQuery` | Paginated data (chat history, conversation list) |
| Server Actions | Form submissions (login, settings, checkout) |
| WebSocket + Zustand | Real-time streaming (chat responses, live credit updates) |

### 1.5 Real-Time Communication

| Feature | Transport | Protocol |
|---|---|---|
| Chat streaming (AI responses) | Server-Sent Events (SSE) | HTTP/2 streaming via Route Handlers |
| Credit balance updates | WebSocket | JSON frames with heartbeat |
| Notification feed | WebSocket | Multiplexed channel |
| Typing indicators | WebSocket | Lightweight ping frames |

**SSE for Chat Streaming (preferred over WebSocket):**
- Simpler to implement with Next.js Route Handlers and `ReadableStream`
- Automatic reconnection built into `EventSource` API
- Works through proxies and CDNs without special configuration
- Unidirectional (server-to-client) matches the AI response pattern
- User messages sent via standard POST requests (TanStack Mutation)

**WebSocket for bidirectional needs:**
- Reconnection logic via custom Zustand middleware with exponential backoff
- Message queue for offline resilience
- Heartbeat every 30 seconds to maintain connection

### 1.6 Internationalization

| Tool | Purpose |
|---|---|
| `next-intl` v4 | Route-based i18n with RSC support |
| ICU MessageFormat | Pluralization and number formatting for BRL currency |
| `date-fns` with `pt-BR` locale | Date formatting in Brazilian Portuguese |

**Strategy:**
- PT-BR is the default and primary locale; `defaultLocale: 'pt-BR'`
- Route prefix strategy: `/` for PT-BR (no prefix), `/en` for English
- All UI strings in `/messages/pt-BR.json` and `/messages/en.json`
- Currency formatting: `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`
- Number formatting: Brazilian convention (1.000,00)
- Date formatting: DD/MM/YYYY

### 1.7 Testing

| Tool | Layer | Purpose |
|---|---|---|
| Vitest | Unit/Integration | Component logic, hooks, utilities, stores |
| Testing Library | Component | User-centric component testing |
| MSW v2 | Mocking | API mocking for integration tests |
| Playwright | E2E | Full user flow testing across browsers |
| Storybook v8 | Visual | Component documentation and visual regression |
| axe-core + jest-axe | Accessibility | Automated a11y validation |

**Testing Strategy:**
- Unit tests for all custom hooks, utilities, and store slices
- Integration tests for feature modules (chat flow, checkout, onboarding)
- E2E tests for critical paths: signup -> onboarding -> first chat -> checkout
- Visual regression via Storybook + Chromatic
- Coverage target: 80% for business logic, 60% for UI components

### 1.8 Additional Libraries

| Library | Purpose |
|---|---|
| `next-auth` v5 (Auth.js) | Authentication (OAuth, credentials, magic link) |
| `react-markdown` + `remark-gfm` + `rehype-highlight` | Render AI markdown responses with syntax highlighting |
| `framer-motion` v11 | Complex animations (page transitions, chat bubbles) |
| `react-dropzone` | Document upload drag-and-drop |
| `recharts` | Dashboard analytics charts |
| `cmdk` | Command palette (Cmd+K model search) |
| `sonner` | Toast notifications |
| `vaul` | Mobile drawer component |
| `lucide-react` | Icon system |
| `sharp` | Image optimization (server-side) |
| `zod` | Schema validation (shared with backend DTOs) |

---

## 2. Project Structure

### 2.1 Root Layout

```
ia-aggregator/
├── .github/                    # CI/CD workflows
│   ├── workflows/
│   │   ├── ci.yml              # Lint, test, build
│   │   ├── e2e.yml             # Playwright tests
│   │   └── deploy.yml          # Vercel/Docker deployment
├── .husky/                     # Git hooks
│   ├── pre-commit              # lint-staged
│   └── commit-msg              # commitlint
├── .storybook/                 # Storybook configuration
├── e2e/                        # Playwright E2E tests
│   ├── fixtures/
│   ├── pages/                  # Page Object Models
│   ├── chat.spec.ts
│   ├── onboarding.spec.ts
│   ├── checkout.spec.ts
│   └── auth.spec.ts
├── messages/                   # i18n translation files
│   ├── pt-BR.json
│   └── en.json
├── public/
│   ├── fonts/                  # Self-hosted Inter/JetBrains Mono
│   ├── images/
│   │   ├── providers/          # AI provider logos
│   │   ├── onboarding/         # Onboarding illustrations
│   │   └── marketing/          # Landing page assets
│   ├── manifest.json           # PWA manifest
│   └── sw.js                   # Service worker
├── src/
│   ├── app/                    # Next.js App Router
│   ├── components/             # Shared UI components
│   ├── features/               # Feature modules
│   ├── hooks/                  # Shared custom hooks
│   ├── lib/                    # Utilities and configuration
│   ├── stores/                 # Zustand stores
│   ├── types/                  # Global TypeScript types
│   └── services/               # API client layer
├── .env.example
├── .eslintrc.json
├── .prettierrc
├── components.json             # Shadcn/UI config
├── next.config.ts
├── package.json
├── tailwind.config.ts          # Minimal (Tailwind v4 uses CSS config)
├── tsconfig.json
└── vitest.config.ts
```

### 2.2 App Router Structure (`src/app/`)

```
src/app/
├── (auth)/                         # Auth group (no sidebar layout)
│   ├── login/
│   │   └── page.tsx
│   ├── register/
│   │   └── page.tsx
│   ├── forgot-password/
│   │   └── page.tsx
│   ├── reset-password/
│   │   └── page.tsx
│   ├── verify-email/
│   │   └── page.tsx
│   └── layout.tsx                  # Centered card layout
├── (marketing)/                    # Public marketing pages
│   ├── page.tsx                    # Landing page (/)
│   ├── pricing/
│   │   └── page.tsx
│   ├── about/
│   │   └── page.tsx
│   ├── blog/
│   │   ├── page.tsx
│   │   └── [slug]/
│   │       └── page.tsx
│   ├── terms/
│   │   └── page.tsx
│   ├── privacy/
│   │   └── page.tsx
│   └── layout.tsx                  # Marketing navbar + footer
├── (platform)/                     # Authenticated platform
│   ├── onboarding/
│   │   ├── page.tsx                # Step 1: persona selection
│   │   ├── models/
│   │   │   └── page.tsx            # Step 2: model preferences
│   │   ├── plan/
│   │   │   └── page.tsx            # Step 3: plan selection
│   │   └── complete/
│   │       └── page.tsx            # Step 4: success
│   ├── chat/
│   │   ├── page.tsx                # Chat home (new conversation)
│   │   ├── [conversationId]/
│   │   │   └── page.tsx            # Active conversation
│   │   └── layout.tsx              # Chat sidebar + main area
│   ├── explore/
│   │   └── page.tsx                # Model explorer/catalog
│   ├── documents/
│   │   ├── page.tsx                # Document list
│   │   └── [documentId]/
│   │       └── page.tsx            # Document Q&A view
│   ├── history/
│   │   └── page.tsx                # Conversation history
│   ├── settings/
│   │   ├── page.tsx                # redirect to /settings/profile
│   │   ├── profile/
│   │   │   └── page.tsx
│   │   ├── billing/
│   │   │   └── page.tsx
│   │   ├── api-keys/
│   │   │   └── page.tsx
│   │   ├── preferences/
│   │   │   └── page.tsx
│   │   └── layout.tsx              # Settings sidebar nav
│   ├── checkout/
│   │   ├── page.tsx                # Plan selection + payment
│   │   ├── success/
│   │   │   └── page.tsx
│   │   └── cancel/
│   │       └── page.tsx
│   └── layout.tsx                  # Platform shell (sidebar, header, credits)
├── (partner)/                      # Partner dashboard
│   ├── partner/
│   │   ├── page.tsx                # Partner overview
│   │   ├── coupons/
│   │   │   ├── page.tsx            # Coupon management
│   │   │   └── [couponId]/
│   │   │       └── page.tsx
│   │   ├── analytics/
│   │   │   └── page.tsx
│   │   ├── commissions/
│   │   │   └── page.tsx
│   │   ├── settings/
│   │   │   └── page.tsx
│   │   └── layout.tsx              # Partner sidebar
│   └── layout.tsx
├── (admin)/                        # Admin panel
│   ├── admin/
│   │   ├── page.tsx                # Admin dashboard
│   │   ├── users/
│   │   │   ├── page.tsx
│   │   │   └── [userId]/
│   │   │       └── page.tsx
│   │   ├── models/
│   │   │   └── page.tsx            # Model configuration
│   │   ├── billing/
│   │   │   └── page.tsx
│   │   ├── partners/
│   │   │   └── page.tsx
│   │   ├── analytics/
│   │   │   └── page.tsx
│   │   ├── system/
│   │   │   └── page.tsx            # System health
│   │   └── layout.tsx
│   └── layout.tsx
├── api/                            # Route Handlers (BFF)
│   ├── auth/
│   │   └── [...nextauth]/
│   │       └── route.ts
│   ├── chat/
│   │   ├── route.ts                # Create conversation
│   │   └── [conversationId]/
│   │       ├── route.ts            # Get/update conversation
│   │       ├── messages/
│   │       │   └── route.ts        # Send message
│   │       └── stream/
│   │           └── route.ts        # SSE streaming endpoint
│   ├── models/
│   │   └── route.ts
│   ├── credits/
│   │   └── route.ts
│   ├── documents/
│   │   └── route.ts
│   ├── checkout/
│   │   └── route.ts
│   └── webhooks/
│       ├── stripe/
│       │   └── route.ts
│       └── partner/
│           └── route.ts
├── globals.css                     # Tailwind v4 theme + global styles
├── layout.tsx                      # Root layout (providers, fonts)
├── loading.tsx                     # Global loading UI
├── not-found.tsx                   # 404 page
└── error.tsx                       # Global error boundary
```

### 2.3 Components Directory (Atomic Design)

```
src/components/
├── ui/                             # Shadcn/UI primitives (auto-generated)
│   ├── button.tsx
│   ├── input.tsx
│   ├── textarea.tsx
│   ├── select.tsx
│   ├── dialog.tsx
│   ├── dropdown-menu.tsx
│   ├── popover.tsx
│   ├── tooltip.tsx
│   ├── badge.tsx
│   ├── card.tsx
│   ├── avatar.tsx
│   ├── skeleton.tsx
│   ├── separator.tsx
│   ├── slider.tsx
│   ├── switch.tsx
│   ├── tabs.tsx
│   ├── toast.tsx                   # sonner integration
│   ├── command.tsx                 # cmdk integration
│   ├── sheet.tsx                   # Side panel / mobile drawer
│   ├── scroll-area.tsx
│   ├── progress.tsx
│   ├── alert.tsx
│   ├── table.tsx
│   ├── pagination.tsx
│   ├── breadcrumb.tsx
│   ├── collapsible.tsx
│   ├── accordion.tsx
│   ├── toggle.tsx
│   ├── toggle-group.tsx
│   ├── radio-group.tsx
│   ├── checkbox.tsx
│   ├── label.tsx
│   ├── form.tsx                    # React Hook Form integration
│   ├── chart.tsx                   # Recharts wrapper
│   └── sidebar.tsx                 # App sidebar
├── atoms/                          # Smallest building blocks
│   ├── Logo.tsx
│   ├── ThemeToggle.tsx
│   ├── CreditBadge.tsx             # Displays current credit count
│   ├── ModelIcon.tsx               # Provider-specific model icon
│   ├── StatusDot.tsx               # Online/offline/loading indicator
│   ├── CopyButton.tsx              # Copy to clipboard
│   ├── LoadingDots.tsx             # Animated typing indicator
│   ├── MarkdownRenderer.tsx        # AI response markdown
│   ├── CodeBlock.tsx               # Syntax-highlighted code
│   ├── CurrencyDisplay.tsx         # BRL formatting
│   └── RelativeTime.tsx            # "há 5 minutos"
├── molecules/                      # Composed atom groups
│   ├── ModelSelector.tsx           # Model dropdown with cost/provider
│   ├── ModelCard.tsx               # Model info card for explorer
│   ├── CreditMeter.tsx             # Visual credit usage gauge
│   ├── ConversationItem.tsx        # Sidebar conversation entry
│   ├── MessageBubble.tsx           # Single chat message
│   ├── UserMessage.tsx             # User message variant
│   ├── AssistantMessage.tsx        # AI response variant
│   ├── SystemMessage.tsx           # System notification in chat
│   ├── FileUploadCard.tsx          # Upload preview with progress
│   ├── PlanCard.tsx                # Pricing plan display
│   ├── CouponInput.tsx             # Coupon code entry with validation
│   ├── SearchInput.tsx             # Search with debounce
│   ├── EmptyState.tsx              # Configurable empty state
│   ├── ErrorState.tsx              # Error display with retry
│   ├── StatCard.tsx                # Dashboard metric card
│   ├── NavItem.tsx                 # Sidebar navigation item
│   └── UserMenu.tsx                # Avatar + dropdown menu
├── organisms/                      # Complex UI sections
│   ├── ChatPanel.tsx               # Full chat message area
│   ├── ChatInput.tsx               # Message input + model selector + send
│   ├── ChatSidebar.tsx             # Conversation list sidebar
│   ├── AppSidebar.tsx              # Main application sidebar
│   ├── AppHeader.tsx               # Top header bar
│   ├── ModelExplorer.tsx           # Model catalog grid/list
│   ├── SavingsCalculator.tsx       # Landing page calculator
│   ├── PricingTable.tsx            # Plan comparison table
│   ├── OnboardingWizard.tsx        # Multi-step onboarding
│   ├── DocumentUploader.tsx        # Drag-drop document upload
│   ├── CreditDashboard.tsx         # Credit usage overview
│   ├── PartnerDashboard.tsx        # Partner stats overview
│   ├── AdminUserTable.tsx          # Admin user management
│   ├── CommandPalette.tsx          # Cmd+K search
│   ├── NotificationCenter.tsx      # Notification dropdown
│   └── Footer.tsx                  # Marketing footer
├── templates/                      # Page-level layouts
│   ├── AuthTemplate.tsx            # Centered auth card
│   ├── MarketingTemplate.tsx       # Marketing page wrapper
│   ├── PlatformTemplate.tsx        # App shell with sidebar
│   ├── SettingsTemplate.tsx        # Settings with sub-nav
│   ├── DashboardTemplate.tsx       # Dashboard grid layout
│   └── ChatTemplate.tsx            # Chat-specific layout
└── providers/                      # Context providers
    ├── ThemeProvider.tsx            # Dark/light mode
    ├── QueryProvider.tsx           # TanStack Query
    ├── AuthProvider.tsx             # Session context
    ├── WebSocketProvider.tsx        # WS connection
    ├── ToastProvider.tsx            # Sonner toast
    └── IntlProvider.tsx             # next-intl
```

### 2.4 Features Directory

```
src/features/
├── auth/
│   ├── components/
│   │   ├── LoginForm.tsx
│   │   ├── RegisterForm.tsx
│   │   ├── ForgotPasswordForm.tsx
│   │   ├── SocialLoginButtons.tsx
│   │   └── VerifyEmailBanner.tsx
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   └── useSession.ts
│   ├── schemas/
│   │   ├── loginSchema.ts
│   │   └── registerSchema.ts
│   ├── actions/
│   │   ├── login.ts                # Server Action
│   │   └── register.ts
│   └── index.ts                    # Public API barrel
├── chat/
│   ├── components/
│   │   ├── ChatContainer.tsx
│   │   ├── MessageList.tsx
│   │   ├── MessageInput.tsx
│   │   ├── StreamingMessage.tsx
│   │   ├── ModelSelectorPopover.tsx
│   │   ├── SplitView.tsx           # Duel mode: two models side by side
│   │   ├── RegenerateButton.tsx
│   │   ├── MessageActions.tsx      # Copy, regenerate, rate
│   │   ├── ConversationList.tsx
│   │   ├── ConversationSearch.tsx
│   │   └── ChatWelcome.tsx         # New chat suggestions
│   ├── hooks/
│   │   ├── useChat.ts              # Core chat logic
│   │   ├── useStreaming.ts         # SSE connection management
│   │   ├── useConversations.ts
│   │   ├── useModelSelector.ts
│   │   └── useChatScroll.ts        # Auto-scroll behavior
│   ├── stores/
│   │   └── chatStore.ts            # Active chat state
│   ├── utils/
│   │   ├── messageParser.ts
│   │   └── streamProcessor.ts
│   └── index.ts
├── models/
│   ├── components/
│   │   ├── ModelCatalog.tsx
│   │   ├── ModelDetailSheet.tsx
│   │   ├── ModelComparisonTable.tsx
│   │   ├── ModelFilterBar.tsx
│   │   └── ProviderBadge.tsx
│   ├── hooks/
│   │   ├── useModels.ts
│   │   └── useModelPricing.ts
│   ├── types/
│   │   └── model.types.ts
│   └── index.ts
├── credits/
│   ├── components/
│   │   ├── CreditBalance.tsx
│   │   ├── CreditHistory.tsx
│   │   ├── CreditAlert.tsx         # Low credit warning
│   │   ├── UsageChart.tsx
│   │   └── CostEstimator.tsx       # Per-message cost preview
│   ├── hooks/
│   │   ├── useCredits.ts
│   │   └── useCreditStream.ts      # Real-time balance via WS
│   └── index.ts
├── onboarding/
│   ├── components/
│   │   ├── PersonaSelector.tsx     # Developer, Student, Professional, Creator
│   │   ├── ModelPreferences.tsx
│   │   ├── PlanSelector.tsx
│   │   ├── OnboardingComplete.tsx
│   │   ├── OnboardingProgress.tsx
│   │   └── PersonaCard.tsx
│   ├── hooks/
│   │   └── useOnboarding.ts
│   ├── constants/
│   │   └── personas.ts
│   └── index.ts
├── documents/
│   ├── components/
│   │   ├── DocumentList.tsx
│   │   ├── DocumentUpload.tsx
│   │   ├── DocumentChat.tsx        # Q&A over document
│   │   └── DocumentPreview.tsx
│   ├── hooks/
│   │   ├── useDocuments.ts
│   │   └── useDocumentUpload.ts
│   └── index.ts
├── checkout/
│   ├── components/
│   │   ├── CheckoutForm.tsx
│   │   ├── PlanComparison.tsx
│   │   ├── PaymentMethodSelector.tsx
│   │   ├── OrderSummary.tsx
│   │   ├── CouponField.tsx
│   │   └── CheckoutSuccess.tsx
│   ├── hooks/
│   │   ├── useCheckout.ts
│   │   └── useCoupon.ts
│   └── index.ts
├── partner/
│   ├── components/
│   │   ├── PartnerOverview.tsx
│   │   ├── CouponManager.tsx
│   │   ├── CommissionTable.tsx
│   │   ├── PartnerAnalytics.tsx
│   │   ├── CreateCouponDialog.tsx
│   │   └── ReferralLink.tsx
│   ├── hooks/
│   │   ├── usePartnerStats.ts
│   │   └── useCoupons.ts
│   └── index.ts
├── admin/
│   ├── components/
│   │   ├── AdminDashboard.tsx
│   │   ├── UserManagement.tsx
│   │   ├── ModelConfiguration.tsx
│   │   ├── SystemHealth.tsx
│   │   ├── RevenueChart.tsx
│   │   └── AuditLog.tsx
│   ├── hooks/
│   │   ├── useAdminStats.ts
│   │   └── useUserManagement.ts
│   └── index.ts
├── settings/
│   ├── components/
│   │   ├── ProfileForm.tsx
│   │   ├── BillingOverview.tsx
│   │   ├── ApiKeyManager.tsx
│   │   ├── PreferencesForm.tsx
│   │   ├── DangerZone.tsx          # Account deletion
│   │   └── NotificationSettings.tsx
│   ├── hooks/
│   │   └── useSettings.ts
│   └── index.ts
└── landing/
    ├── components/
    │   ├── Hero.tsx
    │   ├── SavingsCalculator.tsx
    │   ├── FeatureShowcase.tsx
    │   ├── ModelProviderLogos.tsx
    │   ├── TestimonialCarousel.tsx
    │   ├── PricingSection.tsx
    │   ├── FAQAccordion.tsx
    │   └── CTASection.tsx
    └── index.ts
```

### 2.5 Remaining Source Directories

```
src/hooks/                          # Shared custom hooks
├── useDebounce.ts
├── useMediaQuery.ts
├── useLocalStorage.ts
├── useClipboard.ts
├── useKeyboardShortcut.ts
├── useIntersectionObserver.ts
├── useOnClickOutside.ts
├── useScrollPosition.ts
├── useNetworkStatus.ts
└── usePrevious.ts

src/lib/                            # Utilities and config
├── api-client.ts                   # Axios/fetch wrapper with interceptors
├── auth.ts                         # NextAuth config
├── cn.ts                           # clsx + tailwind-merge utility
├── constants.ts                    # App-wide constants
├── env.ts                          # Type-safe env variables (t3-env)
├── fonts.ts                        # Next.js font loading
├── formatters.ts                   # Currency, date, number formatting (BRL)
├── metadata.ts                     # SEO metadata helpers
├── queryClient.ts                  # TanStack Query client config
├── validators.ts                   # Shared Zod schemas
├── websocket.ts                    # WebSocket connection manager
└── errors.ts                       # Error classes and handlers

src/stores/                         # Zustand global stores
├── authStore.ts                    # Session, user profile
├── uiStore.ts                      # Sidebar, theme, locale, modals
├── chatStore.ts                    # Active conversation, streaming state
├── creditStore.ts                  # Real-time credit balance
└── notificationStore.ts            # Notification queue

src/types/                          # Global TypeScript types
├── api.types.ts                    # API response/request shapes
├── auth.types.ts                   # User, session, role types
├── chat.types.ts                   # Message, conversation, stream
├── model.types.ts                  # AI model, provider, pricing
├── credit.types.ts                 # Credit, usage, plan
├── partner.types.ts                # Partner, coupon, commission
├── common.types.ts                 # Shared utility types
└── next-auth.d.ts                  # NextAuth type augmentation

src/services/                       # API service layer
├── authService.ts
├── chatService.ts
├── modelService.ts
├── creditService.ts
├── documentService.ts
├── checkoutService.ts
├── partnerService.ts
├── adminService.ts
├── userService.ts
└── analyticsService.ts
```

---

## 3. Design System

### 3.1 Color Palette

The color system uses HSL values defined as CSS custom properties for runtime theme switching. The palette is designed for accessibility (WCAG 2.1 AA contrast ratios) and a premium, modern feel suitable for an AI platform.

#### Light Theme

```css
/* globals.css - Light Theme (default) */
:root {
  /* Background layers */
  --background: 0 0% 100%;           /* #FFFFFF - page background */
  --background-secondary: 210 20% 98%; /* #F8FAFC - subtle sections */
  --background-tertiary: 210 16% 96%; /* #F1F5F9 - cards, panels */

  /* Foreground / text */
  --foreground: 222 47% 11%;          /* #0F172A - primary text */
  --foreground-secondary: 215 16% 47%; /* #64748B - secondary text */
  --foreground-muted: 215 20% 65%;    /* #94A3B8 - muted/placeholder */

  /* Brand - Vibrant Blue (primary action color) */
  --primary: 221 83% 53%;             /* #3B82F6 */
  --primary-hover: 221 83% 46%;       /* #2563EB */
  --primary-foreground: 0 0% 100%;    /* white text on primary */
  --primary-soft: 221 83% 96%;        /* #EFF6FF - primary tint */

  /* Secondary - Cool Gray */
  --secondary: 215 20% 93%;           /* #E2E8F0 */
  --secondary-hover: 215 16% 85%;
  --secondary-foreground: 222 47% 11%;

  /* Accent - Teal (for AI/smart features) */
  --accent: 173 80% 40%;              /* #0D9488 */
  --accent-hover: 173 80% 35%;
  --accent-foreground: 0 0% 100%;
  --accent-soft: 173 80% 96%;

  /* Semantic colors */
  --success: 142 71% 45%;             /* #22C55E */
  --success-soft: 142 71% 95%;
  --warning: 38 92% 50%;              /* #F59E0B */
  --warning-soft: 38 92% 95%;
  --error: 0 84% 60%;                 /* #EF4444 */
  --error-soft: 0 84% 96%;
  --info: 199 89% 48%;                /* #0EA5E9 */
  --info-soft: 199 89% 96%;

  /* Surface/Card */
  --card: 0 0% 100%;
  --card-foreground: 222 47% 11%;
  --card-hover: 210 20% 98%;

  /* Border */
  --border: 214 32% 91%;              /* #E2E8F0 */
  --border-hover: 215 20% 80%;
  --ring: 221 83% 53%;                /* Focus ring = primary */

  /* Sidebar */
  --sidebar: 222 47% 11%;             /* Dark sidebar */
  --sidebar-foreground: 210 20% 90%;
  --sidebar-accent: 221 83% 53%;
  --sidebar-border: 222 30% 18%;

  /* Chat-specific */
  --user-bubble: 221 83% 53%;         /* Primary blue for user messages */
  --user-bubble-foreground: 0 0% 100%;
  --assistant-bubble: 210 20% 98%;    /* Light gray for AI responses */
  --assistant-bubble-foreground: 222 47% 11%;

  /* Model provider colors */
  --openai: 160 84% 39%;              /* OpenAI green */
  --anthropic: 25 95% 53%;            /* Anthropic orange */
  --google: 217 89% 61%;              /* Google blue */
  --meta: 214 89% 52%;                /* Meta blue */
  --mistral: 14 89% 55%;              /* Mistral orange */
}
```

#### Dark Theme

```css
.dark {
  --background: 222 47% 5%;           /* #030711 */
  --background-secondary: 222 47% 8%; /* #0B1120 */
  --background-tertiary: 222 40% 12%; /* #131B2E */

  --foreground: 210 20% 95%;          /* #F1F5F9 */
  --foreground-secondary: 215 20% 65%;
  --foreground-muted: 215 16% 47%;

  --primary: 221 83% 58%;             /* Slightly brighter in dark */
  --primary-hover: 221 83% 65%;
  --primary-foreground: 0 0% 100%;
  --primary-soft: 221 83% 15%;

  --accent: 173 80% 48%;
  --accent-soft: 173 80% 12%;

  --card: 222 40% 10%;
  --card-foreground: 210 20% 95%;
  --card-hover: 222 40% 14%;

  --border: 222 30% 18%;
  --border-hover: 222 30% 25%;
  --ring: 221 83% 58%;

  --sidebar: 222 47% 3%;
  --sidebar-foreground: 210 20% 85%;
  --sidebar-border: 222 30% 12%;

  --user-bubble: 221 83% 45%;
  --assistant-bubble: 222 40% 12%;
  --assistant-bubble-foreground: 210 20% 95%;

  --success-soft: 142 71% 10%;
  --warning-soft: 38 92% 10%;
  --error-soft: 0 84% 10%;
  --info-soft: 199 89% 10%;
}
```

### 3.2 Typography Scale

```css
/* Font loading via next/font (src/lib/fonts.ts) */
/* Primary: Inter - clean, modern, excellent readability */
/* Mono: JetBrains Mono - code blocks in AI responses */

:root {
  --font-sans: 'Inter', system-ui, -apple-system, sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;
}
```

| Token | Size (rem) | Size (px) | Line Height | Weight | Use Case |
|---|---|---|---|---|---|
| `text-xs` | 0.75 | 12 | 1.5 | 400 | Labels, badges, timestamps |
| `text-sm` | 0.875 | 14 | 1.5 | 400 | Secondary text, captions, sidebar items |
| `text-base` | 1 | 16 | 1.6 | 400 | Body text, chat messages, form inputs |
| `text-lg` | 1.125 | 18 | 1.6 | 500 | Subheadings, card titles |
| `text-xl` | 1.25 | 20 | 1.5 | 600 | Section headings |
| `text-2xl` | 1.5 | 24 | 1.4 | 700 | Page titles |
| `text-3xl` | 1.875 | 30 | 1.3 | 700 | Marketing headings |
| `text-4xl` | 2.25 | 36 | 1.2 | 800 | Hero title |
| `text-5xl` | 3 | 48 | 1.1 | 800 | Landing hero display |

**Font Weight Usage:**
- 400 (Regular): Body text, messages, descriptions
- 500 (Medium): Navigation items, labels, subtle emphasis
- 600 (Semibold): Card headings, button text, section titles
- 700 (Bold): Page headings, important values (credit count)
- 800 (Extrabold): Marketing display, hero, pricing amounts

### 3.3 Spacing System

Based on a 4px base unit. Follows Tailwind's default scale:

| Token | Value | Use Case |
|---|---|---|
| `0.5` | 2px | Micro gaps (icon-text) |
| `1` | 4px | Tight padding (badges, tags) |
| `1.5` | 6px | Compact elements |
| `2` | 8px | Button padding (y-axis), input padding |
| `3` | 12px | Small card padding, gaps between related items |
| `4` | 16px | Standard card padding, section gaps |
| `5` | 20px | Medium spacing |
| `6` | 24px | Large card padding, group spacing |
| `8` | 32px | Section spacing within page |
| `10` | 40px | Large section gaps |
| `12` | 48px | Page section dividers |
| `16` | 64px | Major page sections |
| `20` | 80px | Hero padding |
| `24` | 96px | Marketing section spacing |

**Layout-specific spacing:**
- Sidebar width: 280px (expanded), 64px (collapsed)
- Chat panel max-width: 768px (centered)
- Marketing content max-width: 1280px
- Header height: 64px
- Chat input area height: min 64px, max 200px

### 3.4 Component Library Catalog

Complete list of components required for the AI aggregator platform:

#### Primitives (from Shadcn/UI)
1. Button (variants: default, secondary, destructive, outline, ghost, link; sizes: sm, md, lg, icon)
2. Input
3. Textarea (auto-grow for chat input)
4. Select / Combobox
5. Checkbox
6. Radio Group
7. Switch / Toggle
8. Slider
9. Label
10. Form (React Hook Form integration)
11. Dialog / Modal
12. Sheet (side panel)
13. Popover
14. Tooltip
15. Dropdown Menu
16. Context Menu
17. Command (cmdk for palette)
18. Tabs
19. Accordion
20. Collapsible
21. Alert / Alert Dialog
22. Badge
23. Card
24. Avatar
25. Skeleton
26. Progress
27. Scroll Area
28. Separator
29. Table
30. Pagination
31. Breadcrumb
32. Toast (Sonner)
33. Sidebar

#### Custom Platform Components
34. Logo
35. ThemeToggle (sun/moon with animation)
36. CreditBadge (animated count with color states)
37. CreditMeter (radial or bar gauge)
38. ModelIcon (per-provider branded icon)
39. ModelSelector (dropdown with provider, model name, cost/message)
40. ModelCard (explorer view with capabilities, pricing, benchmarks)
41. ModelComparisonTable
42. MessageBubble (user variant)
43. MessageBubble (assistant variant with markdown)
44. StreamingMessage (with animated cursor)
45. CodeBlock (syntax-highlighted with copy button)
46. MarkdownRenderer
47. ChatInput (auto-grow textarea + model selector + attachments + send)
48. ChatSidebar (conversation list with search, create new)
49. ConversationItem (title, model icon, timestamp, preview)
50. SplitView (dual-pane for model comparison)
51. DocumentUploader (drag-and-drop zone)
52. FileUploadCard (preview with progress bar)
53. CouponInput (code entry with inline validation)
54. PlanCard (pricing tier with feature list)
55. PricingTable (3-column comparison)
56. SavingsCalculator (interactive sliders/inputs)
57. PersonaCard (onboarding persona selection)
58. OnboardingProgress (step indicator)
59. StatCard (metric + trend + sparkline)
60. EmptyState (icon + message + action)
61. ErrorState (retry button, error description)
62. LoadingDots (animated typing indicator)
63. CopyButton
64. CurrencyDisplay (BRL formatting)
65. RelativeTime (PT-BR relative dates)
66. SearchInput (with debounce and clear)
67. CommandPalette (Cmd+K overlay)
68. NotificationCenter (bell icon + dropdown)
69. UserMenu (avatar + dropdown with profile, settings, logout)
70. NavItem (sidebar link with icon, label, active state)
71. AppSidebar (collapsible navigation)
72. AppHeader (credits, notifications, user, mobile hamburger)
73. Footer (marketing)
74. Hero (landing page)
75. FeatureShowcase (landing page feature cards)
76. TestimonialCarousel
77. FAQAccordion
78. CTASection

### 3.5 Icon System

| Provider | Usage |
|---|---|
| `lucide-react` | Primary icon set (400+ icons, tree-shakeable) |
| Custom SVGs | AI provider logos (OpenAI, Anthropic, Google, Meta, Mistral) |
| Emoji flags | Locale selector |

**Icon Sizing:**
| Context | Size | Tailwind Class |
|---|---|---|
| Inline text | 16px | `size-4` |
| Button icons | 18px | `size-[18px]` |
| Navigation | 20px | `size-5` |
| Feature cards | 24px | `size-6` |
| Empty states | 48px | `size-12` |
| Hero | 64px | `size-16` |

### 3.6 Animation and Motion Guidelines

**Philosophy:** Motion should feel purposeful and responsive, not decorative. Prioritize performance by using CSS transforms and opacity -- avoid animating layout properties.

| Type | Duration | Easing | Use Case |
|---|---|---|---|
| Micro-interaction | 150ms | `ease-out` | Button hover, toggle, icon change |
| Transition | 200ms | `ease-in-out` | Panel open/close, dropdown, tooltip |
| Enter/Exit | 250ms | `cubic-bezier(0.16, 1, 0.3, 1)` | Modal, sheet, notification |
| Page transition | 300ms | `cubic-bezier(0.33, 1, 0.68, 1)` | Route changes |
| Emphasis | 400ms | `spring(1, 80, 10)` | Success animations, confetti |
| Streaming cursor | 500ms | `steps(2)` | Blinking cursor on AI response |

**Key Animations:**
- **Chat messages:** Slide up + fade in (150ms stagger per message)
- **Streaming text:** Characters appear progressively, blinking cursor at end
- **Credit deduction:** Number counter animation (counting down)
- **Theme toggle:** Sun/moon rotation with scale
- **Sidebar collapse:** Width transition with icon rotation
- **Model selector:** Dropdown with scale + fade origin from trigger
- **Skeleton loading:** Shimmer pulse effect
- **Toast notifications:** Slide in from top-right with progress bar

**Reduced Motion:**
```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

### 3.7 Responsive Breakpoints

| Breakpoint | Width | Target |
|---|---|---|
| `sm` | 640px | Large phones (landscape) |
| `md` | 768px | Tablets |
| `lg` | 1024px | Small laptops |
| `xl` | 1280px | Desktops |
| `2xl` | 1536px | Large monitors |

**Layout Behavior by Breakpoint:**

| Element | Mobile (<768px) | Tablet (768-1024px) | Desktop (>1024px) |
|---|---|---|---|
| Sidebar | Hidden (hamburger) | Collapsed (icons only) | Expanded (full) |
| Chat input | Full width, bottom fixed | Full width, inline | Max 768px, centered |
| Model selector | Full-screen sheet | Popover | Popover |
| Split view (duel) | Stacked vertical | Side by side (50/50) | Side by side (50/50) |
| Settings | Single column | Two columns | Two columns + sidebar |
| Pricing table | Stacked cards | 2-column grid | 3-column grid |
| Admin table | Horizontal scroll | Responsive table | Full table |

---

## 4. UX/UI Best Practices

### 4.1 Landing Page UX Flow

**Goal:** Convert visitors into registered users by demonstrating value immediately.

```
[Hero Section]
  "Acesse todos os modelos de IA por um unico preco"
  -> CTA: "Comece Gratuitamente" | "Calcule sua Economia"
      |
[Savings Calculator] (interactive, above the fold)
  -> User inputs: messages/month, current provider
  -> Shows: monthly cost vs. IA Aggregator cost
  -> Animated number transition showing savings in BRL
      |
[Feature Showcase] (3-4 cards)
  -> Multi-model access | Unified billing | Credit system | Documents
      |
[Model Provider Logos] (trust signals)
  -> OpenAI, Anthropic, Google, Meta, Mistral logos in carousel
      |
[Pricing Section] (3 tiers)
  -> Free | Pro | Enterprise with plan comparison
      |
[Testimonials] (social proof)
      |
[FAQ Accordion]
      |
[Final CTA] -> "Comece agora"
      |
[Footer] -> Links, social, legal
```

**Key UX Decisions:**
- Savings calculator is the primary conversion tool -- positioned high on page
- All pricing displayed in BRL with Brazilian formatting (R$ 1.234,56)
- Social proof from Brazilian companies/developers
- Mobile: calculator collapses to simplified version with expandable details
- Page loads in < 2.5s (LCP); calculator interactive in < 3.5s (INP)

### 4.2 Onboarding Flow (4 Persona-Based Paths)

```
Step 1: Persona Selection
  +-----------+  +-----------+  +-----------+  +-----------+
  |Developer  |  |Estudante  |  |Profissional|  |Criador    |
  |           |  |           |  |            |  |de Conteudo|
  |  </>      |  |  Grad cap |  |  Briefcase |  |  Pen      |
  +-----------+  +-----------+  +-----------+  +-----------+
      |                |              |               |
      v                v              v               v
Step 2: Model Preferences (personalized per persona)
  Developer -> GPT-4, Claude, CodeLlama preselected
  Student   -> GPT-4o-mini, Gemini preselected (budget-friendly)
  Professional -> GPT-4, Claude, Gemini preselected
  Creator   -> GPT-4, Claude, DALL-E suggested
      |
Step 3: Plan Selection
  Show recommended plan based on persona:
  Developer -> Pro (higher token needs)
  Student   -> Free (with upgrade nudge)
  Professional -> Pro
  Creator   -> Pro
      |
Step 4: Welcome / Success
  -> Personalized welcome message
  -> Quick-start suggestions based on persona
  -> "Enviar sua primeira mensagem" CTA
  -> Skip to chat
```

**UX Principles:**
- Maximum 4 steps, progress bar visible throughout
- "Pular" (Skip) button available on every step
- Persona selection influences default model, welcome prompts, and suggested use cases
- Step transitions use horizontal slide animation
- Each step validates before proceeding (client-side)
- Back button preserves selections
- Mobile: full-screen steps with bottom-fixed navigation

### 4.3 Chat Interface UX

```
+------------------------------------------------------------------+
|  [=] AppSidebar           [Header: Credits | Notif | User]       |
|----+------------------------------------------------------------+|
|    |                                                            ||
| C  |              Chat Messages Area                            ||
| o  |              (scrollable, auto-scroll on new)              ||
| n  |                                                            ||
| v  |  +------------------------------------------+              ||
| e  |  | User message (right-aligned, blue bg)    |              ||
| r  |  +------------------------------------------+              ||
| s  |                                                            ||
| a  |  +------------------------------------------+              ||
| t  |  | AI response (left-aligned, gray bg)      |              ||
| i  |  | - Markdown rendered                      |              ||
| o  |  | - Code blocks with copy                  |              ||
| n  |  | - Loading dots while streaming           |              ||
| s  |  +------------------------------------------+              ||
|    |                                                            ||
| L  |  [Cost: ~0.3 creditos] (per-message estimate)              ||
| i  |                                                            ||
| s  |  +------------------------------------------+              ||
| t  |  | [Model: GPT-4 v] [Attach] [____input____] [Send ->]   ||
|    |  +------------------------------------------+              ||
+----+------------------------------------------------------------+
```

**Streaming UX Details:**
1. User sends message -> message appears immediately (optimistic)
2. Credit cost estimate shown before sending
3. "Thinking..." indicator with animated dots (150ms)
4. Text streams in token-by-token, auto-scrolling
5. Blinking cursor at the end of streaming text
6. On complete: action bar appears (Copy | Regenerate | Rate)
7. Credit balance updates in real-time (header badge animates)
8. If error: retry button with error explanation in PT-BR

**Model Selector (within chat input):**
```
+-----------------------------------+
| Selecionar Modelo           [x]  |
|-----------------------------------|
| > Populares                       |
|   GPT-4o         ~2.5 cr/msg  [*]|
|   Claude 3.5     ~2.0 cr/msg     |
|   Gemini Pro     ~1.5 cr/msg     |
|                                   |
| > Econômicos                     |
|   GPT-4o-mini    ~0.3 cr/msg     |
|   Gemini Flash   ~0.2 cr/msg     |
|   Llama 3.1      ~0.1 cr/msg     |
|                                   |
| > Especializados                 |
|   Claude 3 Opus  ~5.0 cr/msg     |
|   GPT-4 Vision   ~3.0 cr/msg     |
+-----------------------------------+
```

**Credit Display (header):**
```
[coin-icon] 1.234 creditos  (normal state - text-foreground)
[coin-icon]   156 creditos  (warning state - text-warning, < 20%)
[coin-icon]    23 creditos  (critical state - text-error, pulsing)
```

### 4.4 Partner Dashboard UX

```
+------------------------------------------------------------------+
| Partner Dashboard                                                 |
|------------------------------------------------------------------|
|  +----------------+  +----------------+  +-------------------+   |
|  | Comissoes       |  | Cupons Ativos  |  | Usuarios Referidos|  |
|  | R$ 3.456,78    |  |       12       |  |       234         |  |
|  | +15.3% este mes|  | 3 expirando    |  | +42 este mes      |  |
|  +----------------+  +----------------+  +-------------------+   |
|                                                                   |
|  [Criar Novo Cupom]                                              |
|                                                                   |
|  +-----------------------------------------------------------+  |
|  | Cupom       | Desconto | Usos | Receita  | Status         |  |
|  |-------------|----------|------|----------|----------------|  |
|  | TECH2026    | 20%      | 89   | R$1.2k   | Ativo          |  |
|  | DEVSTART    | 15%      | 156  | R$2.1k   | Ativo          |  |
|  | SUMMER      | 25%      | 45   | R$890    | Expirando      |  |
|  +-----------------------------------------------------------+  |
|                                                                   |
|  [Analytics Chart - Commission over time]                        |
+------------------------------------------------------------------+
```

### 4.5 Admin Panel UX

```
+------------------------------------------------------------------+
| Admin Panel                                                       |
|------------------------------------------------------------------|
| [Overview] [Usuarios] [Modelos] [Financeiro] [Parceiros] [Sistema]|
|                                                                   |
|  +----------+ +----------+ +----------+ +----------+            |
|  | Usuarios  | | MRR      | | Msgs/dia | | Uptime   |           |
|  | 12.456    | | R$89.2k  | | 456.7k   | | 99.97%   |           |
|  +----------+ +----------+ +----------+ +----------+            |
|                                                                   |
|  [Revenue Chart]     [User Growth Chart]                         |
|                                                                   |
|  [Recent Activity Feed]                                          |
|  [System Health Indicators]                                      |
+------------------------------------------------------------------+
```

### 4.6 Settings/Profile UX

```
+------------------------------------------------------------------+
|  Settings                                                         |
|  +------------+-------------------------------------------------+|
|  | Perfil     |  Profile Form                                   ||
|  | Cobranca   |  - Avatar upload                                ||
|  | Chaves API |  - Nome, Email                                  ||
|  | Preferencias|  - Telefone (BR format: (11) 99999-9999)       ||
|  |            |  - Idioma preferido                              ||
|  |            |  - Fuso horario (America/Sao_Paulo default)     ||
|  |            |  [Salvar Alteracoes]                             ||
|  +------------+-------------------------------------------------+|
+------------------------------------------------------------------+
```

### 4.7 Checkout Flow UX

```
Step 1: Plan Selection (if not preselected)
  -> 3 plan cards with feature comparison
  -> Current plan highlighted with "Plano Atual" badge
  -> Recommended plan based on usage with "Recomendado" badge

Step 2: Coupon (optional)
  -> Coupon input field with instant validation
  -> Discount shown in real-time on order summary
  -> "TECH2026" -> "20% de desconto aplicado!"

Step 3: Payment
  -> Order summary sidebar (sticky on desktop)
  -> Payment methods: Credit card (Stripe), Boleto, PIX
  -> PIX: QR code displayed with copy-paste code
  -> Boleto: PDF generated with barcode
  -> BRL amounts with Brazilian formatting

Step 4: Success
  -> Confirmation with plan details
  -> Credits added immediately
  -> CTA: "Ir para o Chat" or "Explorar Modelos"
```

### 4.8 Accessibility (WCAG 2.1 AA)

| Requirement | Implementation |
|---|---|
| Color contrast | Minimum 4.5:1 for text, 3:1 for large text; verified in both themes |
| Keyboard navigation | Full tab navigation, visible focus rings (ring-2 ring-primary), skip links |
| Screen readers | Semantic HTML, ARIA labels on all interactive elements, live regions for chat |
| Focus management | Auto-focus on chat input, trap focus in modals, return focus on close |
| Motion | `prefers-reduced-motion` respected; disable all non-essential animations |
| Text scaling | UI tested at 200% zoom; no horizontal scroll at 320px width |
| Form errors | Inline errors associated via `aria-describedby`, error summary on submit |
| Status updates | `aria-live="polite"` for credit updates, `aria-live="assertive"` for errors |
| Images | All images have descriptive alt text; decorative images use `alt=""` |
| Language | `<html lang="pt-BR">` set; language attribute on any English content |
| Chat messages | Messages use `role="log"` container with `aria-live="polite"` for streaming |
| Model selector | Combobox pattern with `aria-expanded`, `aria-activedescendant` |

### 4.9 Performance Budgets (Core Web Vitals)

| Metric | Target | Measurement |
|---|---|---|
| LCP (Largest Contentful Paint) | < 2.5s | Landing hero / chat area |
| INP (Interaction to Next Paint) | < 200ms | Chat send button, model selector |
| CLS (Cumulative Layout Shift) | < 0.1 | No layout shifts from fonts, images, or async data |
| FCP (First Contentful Paint) | < 1.8s | Shell renders immediately via streaming SSR |
| TTFB (Time to First Byte) | < 800ms | Edge-cached pages, streaming SSR |
| TTI (Time to Interactive) | < 3.5s | Chat input ready to receive messages |

**JavaScript Bundle Budgets:**

| Bundle | Max Size (gzipped) |
|---|---|
| Framework (React + Next.js) | 85 KB |
| Core app (shell, routing) | 50 KB |
| Chat feature | 40 KB |
| Landing page | 30 KB |
| Total initial JS | < 170 KB |
| Per-route chunk | < 50 KB |

### 4.10 Progressive Loading Strategies

| Page/Feature | Strategy |
|---|---|
| Landing page | Static generation (ISR 1h); hero image priority loaded |
| Chat list | Skeleton loader for sidebar; instant navigation |
| Chat messages | Infinite scroll (load older on scroll up); latest 50 messages first |
| Model catalog | Skeleton grid -> progressive load with `Suspense` boundaries |
| AI response | Streaming text with placeholder; code blocks lazy-rendered |
| Dashboard charts | Skeleton chart -> `React.lazy` for `recharts`; data via `useQuery` |
| Partner analytics | Skeleton -> deferred loading with `React.lazy` |
| Images | Next.js `<Image>` with `placeholder="blur"`, responsive `srcSet` |
| Fonts | `next/font` with `display: swap`; Inter subset for Latin characters |

**Suspense Boundary Architecture:**
```tsx
<Suspense fallback={<AppShellSkeleton />}>        // App shell
  <Suspense fallback={<SidebarSkeleton />}>        // Sidebar
    <ChatSidebar />
  </Suspense>
  <Suspense fallback={<ChatAreaSkeleton />}>        // Main content
    <Suspense fallback={<MessagesSkeleton />}>      // Messages
      <MessageList />
    </Suspense>
    <ChatInput />                                   // Always visible
  </Suspense>
</Suspense>
```

### 4.11 Error Handling UX

| Error Type | UX Pattern | Example |
|---|---|---|
| Network error | Toast + retry button | "Sem conexao. Tentando reconectar..." |
| API error (4xx) | Inline error message | "Creditos insuficientes. Adquira mais creditos." |
| API error (5xx) | Error state component | "Erro no servidor. Tente novamente em instantes." |
| Streaming error | Message-level retry | "Erro ao gerar resposta. [Tentar novamente]" |
| Auth expired | Redirect to login | Auto-redirect with return URL preserved |
| Rate limit | Toast with timer | "Limite atingido. Tente novamente em 30s." |
| Form validation | Inline field errors | "E-mail invalido" below input |
| Upload failure | File card error state | "Falha no upload. [Tentar novamente]" |
| Payment failure | Alert with guidance | "Pagamento recusado. Verifique os dados do cartao." |
| WebSocket disconnect | Status bar | "Reconectando..." with progress indicator |

**Global Error Boundary:**
```tsx
// src/app/error.tsx
'use client'
export default function GlobalError({ error, reset }) {
  return (
    <ErrorState
      title="Algo deu errado"
      description="Ocorreu um erro inesperado. Nossa equipe foi notificada."
      action={{ label: "Tentar novamente", onClick: reset }}
    />
  )
}
```

### 4.12 Empty States Design

| Context | Illustration | Message | Action |
|---|---|---|---|
| No conversations | Chat bubble illustration | "Nenhuma conversa ainda" | "Iniciar nova conversa" |
| Empty chat | Sparkle/wand icon | "Como posso ajudar voce hoje?" + 4 suggestion cards | Click suggestion |
| No documents | File folder illustration | "Nenhum documento enviado" | "Enviar documento" |
| No search results | Magnifying glass | "Nenhum resultado para '{query}'" | "Limpar filtros" |
| No credits | Empty wallet | "Seus creditos acabaram" | "Adquirir creditos" |
| No coupons (partner) | Ticket illustration | "Crie seu primeiro cupom" | "Criar cupom" |
| No notifications | Bell illustration | "Voce esta em dia!" | None |
| Empty history | Clock illustration | "Nenhuma atividade recente" | "Explorar modelos" |

---

## 5. Key Features Architecture

### 5.1 Chat with Streaming (SSE)

**Component Tree:**
```
ChatPage
├── ChatSidebar (client component)
│   ├── SearchInput
│   ├── NewChatButton
│   └── ConversationList (virtualized)
│       └── ConversationItem (x N)
└── ChatContainer (client component)
    ├── ChatHeader
    │   ├── ConversationTitle (editable)
    │   ├── ModelBadge
    │   └── ChatActions (export, delete, share)
    ├── MessageList (ref for scroll management)
    │   ├── ChatWelcome (if empty)
    │   │   └── SuggestionCard (x 4)
    │   ├── MessageBubble (x N)
    │   │   ├── UserMessage
    │   │   │   └── MessageContent (plain text)
    │   │   └── AssistantMessage
    │   │       ├── MarkdownRenderer
    │   │       │   ├── CodeBlock (with copy)
    │   │       │   ├── Table
    │   │       │   └── InlineCode
    │   │       └── MessageActions (copy, regenerate, rate)
    │   └── StreamingMessage (during active stream)
    │       ├── LoadingDots (before first token)
    │       ├── MarkdownRenderer (progressive)
    │       └── BlinkingCursor
    ├── CostEstimator (shows estimated cost before send)
    └── ChatInputArea
        ├── ModelSelectorPopover
        │   ├── ModelSearchInput
        │   ├── ModelGroup ("Populares")
        │   │   └── ModelOption (x N)
        │   └── ModelGroup ("Economicos")
        │       └── ModelOption (x N)
        ├── AttachmentButton
        ├── TextareaAutoGrow
        └── SendButton (disabled during stream)
```

**Streaming Data Flow:**
```
1. User clicks Send
   -> useMutation: POST /api/chat/{id}/messages { content, modelId }
   -> Optimistic update: user message appears immediately
   -> Credit estimate deducted optimistically

2. Server responds with messageId
   -> useStreaming hook opens SSE: GET /api/chat/{id}/stream?messageId=X

3. SSE events received:
   event: token    -> append to streaming message buffer
   event: metadata -> update model info, token count
   event: done     -> finalize message, update credit balance
   event: error    -> show error state with retry

4. Stream complete:
   -> Streaming message converts to permanent AssistantMessage
   -> Credit balance refreshed via TanStack Query invalidation
   -> Conversation list updated (latest message preview)
```

**Key Hook: `useStreaming`**
```typescript
interface UseStreamingOptions {
  conversationId: string;
  messageId: string;
  onToken: (token: string) => void;
  onComplete: (fullMessage: AssistantMessage) => void;
  onError: (error: StreamError) => void;
}

function useStreaming(options: UseStreamingOptions) {
  // Returns: { isStreaming, abort, retry, tokensReceived }
  // Manages EventSource lifecycle
  // Auto-reconnects on network failure (3 attempts)
  // Cleans up on unmount
}
```

**Key Hook: `useChat`**
```typescript
interface UseChatReturn {
  messages: Message[];
  sendMessage: (content: string, modelId: string) => Promise<void>;
  isStreaming: boolean;
  streamingContent: string;
  activeModel: Model;
  setActiveModel: (model: Model) => void;
  regenerate: (messageId: string) => Promise<void>;
  estimatedCost: number;
  abort: () => void;
}
```

### 5.2 Model Selector with Credit Costs

**Component Architecture:**
```
ModelSelectorPopover
├── Trigger: ModelBadge (shows current model icon + name)
└── Content: Popover
    ├── ModelSearchInput (cmdk-based filtering)
    ├── ModelGroup (label: "Populares")
    │   └── ModelOption
    │       ├── ProviderIcon (OpenAI/Anthropic/Google logo)
    │       ├── ModelName ("GPT-4o")
    │       ├── ProviderLabel ("OpenAI")
    │       ├── CreditCost ("~2.5 cr/msg")
    │       ├── CapabilityBadges (vision, code, long-context)
    │       └── SelectedIndicator (checkmark)
    ├── ModelGroup (label: "Economicos")
    │   └── ModelOption (x N)
    ├── ModelGroup (label: "Especializados")
    │   └── ModelOption (x N)
    └── Footer
        ├── Link: "Explorar todos os modelos"
        └── CreditBalance mini-display
```

**State Management:**
```typescript
// URL state for model selection (persisted in URL)
const [selectedModel, setSelectedModel] = useQueryState('model', {
  defaultValue: userPreferences.defaultModel,
  parse: (v) => modelSchema.parse(v),
})

// Model data via TanStack Query
const { data: models } = useQuery({
  queryKey: ['models'],
  queryFn: modelService.getAvailableModels,
  staleTime: 5 * 60 * 1000, // 5 minutes
  select: (data) => groupByCategory(data),
})
```

### 5.3 Split Screen / Duel Mode

**Component Tree:**
```
DuelModePage
├── DuelHeader
│   ├── Title: "Comparar Modelos"
│   └── LayoutToggle (side-by-side | stacked)
├── DuelContainer (CSS Grid: 1fr 1fr)
│   ├── DuelPane (left)
│   │   ├── ModelSelectorPopover (model A)
│   │   ├── MessageList (model A responses)
│   │   └── StreamingMessage (if active)
│   ├── DragHandle (resize divider)
│   └── DuelPane (right)
│       ├── ModelSelectorPopover (model B)
│       ├── MessageList (model B responses)
│       └── StreamingMessage (if active)
├── DuelCostDisplay
│   ├── CostA ("GPT-4o: ~2.5 cr")
│   ├── CostB ("Claude 3.5: ~2.0 cr")
│   └── TotalCost ("Total: ~4.5 cr")
└── SharedChatInput (sends to BOTH models simultaneously)
    ├── TextareaAutoGrow
    └── SendButton
```

**Behavior:**
- Single input sends the same message to both selected models
- Both responses stream simultaneously in their respective panes
- Each pane shows independent streaming state
- Total credit cost = sum of both models
- Mobile: switches to tabbed view with swipe between panes
- Response timing comparison shown (which model responded faster)
- "Vote" buttons after both complete (thumbs up on preferred response)

### 5.4 Document Upload and Q&A

**Component Tree:**
```
DocumentsPage
├── DocumentHeader
│   ├── Title: "Meus Documentos"
│   └── UploadButton
├── DocumentUploader (drag-and-drop zone)
│   ├── DropZone (react-dropzone)
│   │   ├── UploadIcon
│   │   ├── "Arraste arquivos aqui ou clique para selecionar"
│   │   └── SupportedFormats ("PDF, DOCX, TXT, MD - max 10MB")
│   └── UploadProgressList
│       └── FileUploadCard (x N)
│           ├── FileIcon (by type)
│           ├── FileName
│           ├── ProgressBar
│           ├── FileSize
│           └── CancelButton
├── DocumentList
│   └── DocumentCard (x N)
│       ├── FileIcon
│       ├── DocumentName
│       ├── UploadDate (relative, PT-BR)
│       ├── PageCount
│       ├── StatusBadge (processing | ready | error)
│       └── Actions (chat, delete, download)
└── DocumentChatView (when document selected)
    ├── DocumentPreview (left pane)
    │   ├── PDFViewer / TextViewer
    │   └── PageNavigation
    └── ChatPanel (right pane, same as main chat)
        ├── MessageList (scoped to document context)
        └── ChatInput (with document context indicator)
```

**Upload Flow:**
```
1. User drops file(s)
   -> Client-side validation (type, size)
   -> FileUploadCard appears with progress bar

2. Upload via chunked upload (for large files)
   -> POST /api/documents (multipart/form-data)
   -> Progress tracked via XMLHttpRequest onprogress

3. Processing:
   -> Status badge: "Processando..."
   -> Backend extracts text, creates embeddings
   -> WebSocket notification when ready

4. Ready:
   -> Status badge: "Pronto"
   -> "Conversar sobre este documento" button enabled
```

### 5.5 Real-Time Credit Consumption Display

**Component Tree:**
```
CreditSystem
├── CreditBadge (header - always visible)
│   ├── CoinIcon (animated on change)
│   ├── CreditCount (animated number transition)
│   └── StatusColor (normal | warning | critical)
├── CreditMeter (settings/billing page)
│   ├── RadialGauge (visual representation)
│   ├── UsedCredits / TotalCredits
│   ├── ResetDate ("Renova em 15 dias")
│   └── UpgradeButton (if near limit)
├── CreditHistory (settings/billing)
│   ├── DateFilter
│   └── TransactionList (virtualized)
│       └── TransactionItem
│           ├── Timestamp
│           ├── Model used
│           ├── TokenCount
│           ├── CreditCost
│           └── ConversationLink
└── CreditAlert (overlay notification)
    ├── LowCreditWarning (< 20% remaining)
    ├── CriticalCreditWarning (< 5% remaining)
    └── ZeroCreditBlock (blocks chat, shows upgrade)
```

**Real-Time Update Architecture:**
```typescript
// Zustand store with WebSocket middleware
const useCreditStore = create<CreditStore>()(
  subscribeWithSelector(
    persist(
      (set, get) => ({
        balance: 0,
        plan: null,
        lastUpdated: null,

        // Called by WebSocket handler
        updateBalance: (newBalance: number) => {
          const prev = get().balance;
          set({
            balance: newBalance,
            lastUpdated: Date.now(),
          });
          // Trigger alert if crossing threshold
          if (prev > newBalance * 0.2 && newBalance <= newBalance * 0.2) {
            triggerLowCreditAlert();
          }
        },

        // Optimistic deduction before message send
        deductOptimistic: (estimatedCost: number) => {
          set((s) => ({ balance: s.balance - estimatedCost }));
        },

        // Reconcile with server after message complete
        reconcile: (actualBalance: number) => {
          set({ balance: actualBalance });
        },
      }),
      { name: 'credit-store' }
    )
  )
);
```

### 5.6 Dark/Light Mode Toggle

**Implementation:**
```typescript
// ThemeProvider wraps the app at root layout level
// Uses next-themes for SSR-safe theme switching

// src/components/providers/ThemeProvider.tsx
import { ThemeProvider as NextThemesProvider } from 'next-themes';

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  return (
    <NextThemesProvider
      attribute="class"         // Adds .dark class to <html>
      defaultTheme="system"     // Respects OS preference
      enableSystem={true}
      disableTransitionOnChange // Prevents FOUC
      storageKey="ia-theme"
    >
      {children}
    </NextThemesProvider>
  );
}

// ThemeToggle atom component
// Animated sun/moon icon with framer-motion rotation
// Accessible: aria-label="Alternar tema escuro"
```

**Theme Variables Flow:**
```
OS preference / user choice
  -> next-themes adds .dark class to <html>
  -> CSS custom properties switch values (Section 3.1)
  -> All components use hsl(var(--primary)) syntax
  -> Zero JavaScript runtime cost for theme application
```

### 5.7 Responsive Mobile Experience

**Mobile-First Design Patterns:**

| Feature | Mobile Implementation |
|---|---|
| Navigation | Bottom tab bar (Chat, Explorar, Documentos, Perfil) + hamburger for full menu |
| Chat sidebar | Full-screen sheet (swipe from left edge or hamburger) |
| Chat input | Fixed to bottom; model selector opens as full-screen sheet |
| Model selector | Full-screen sheet with search and categories |
| Split/Duel mode | Tabbed view with swipe gesture between models |
| Settings | Single column, accordion sections |
| Pricing | Stacked cards with horizontal scroll for comparison |
| Dashboard (partner) | Stacked stat cards, scrollable charts |
| Document upload | Native file picker (no drag-and-drop on mobile) |
| Notifications | Full-screen sheet from top |

**Touch Interactions:**
- Swipe left on conversation: delete/archive actions
- Swipe right from edge: open sidebar
- Long press on message: context menu (copy, regenerate, rate)
- Pull to refresh on conversation list
- Pinch-to-zoom on document preview

**Mobile-Specific Components:**
```
src/components/mobile/
├── BottomTabBar.tsx          # Fixed bottom navigation
├── MobileDrawer.tsx          # Vaul-based drawer
├── SwipeActions.tsx          # Swipe-to-reveal actions
├── PullToRefresh.tsx         # Pull-down refresh
└── MobileModelSheet.tsx      # Full-screen model picker
```

---

## 6. Reusability Patterns

### 6.1 Custom Hooks Catalog

#### Data Fetching Hooks

| Hook | Purpose | Returns |
|---|---|---|
| `useModels()` | Fetch and cache available AI models | `{ models, isLoading, error, refetch }` |
| `useModelPricing(modelId)` | Get pricing for specific model | `{ pricing, estimateCost(tokens) }` |
| `useConversations()` | Paginated conversation list | `{ conversations, fetchNext, hasMore }` |
| `useConversation(id)` | Single conversation with messages | `{ conversation, messages, isLoading }` |
| `useCredits()` | Current credit balance and plan | `{ balance, plan, usage, isLow }` |
| `useCreditStream()` | Real-time credit updates via WS | `{ balance, lastUpdate }` |
| `useDocuments()` | User document list | `{ documents, isLoading, refetch }` |
| `usePartnerStats()` | Partner dashboard metrics | `{ stats, commissions, coupons }` |
| `useAdminStats()` | Admin dashboard metrics | `{ users, revenue, health }` |
| `useNotifications()` | User notification feed | `{ notifications, unread, markRead }` |

```typescript
// Example: useModels hook
export function useModels(options?: { category?: string }) {
  return useQuery({
    queryKey: ['models', options?.category],
    queryFn: () => modelService.getModels(options),
    staleTime: 5 * 60 * 1000,        // Models rarely change
    gcTime: 30 * 60 * 1000,           // Keep in garbage collection 30min
    select: (data) => ({
      all: data,
      grouped: groupByProvider(data),
      popular: data.filter(m => m.isPopular),
      economic: data.filter(m => m.creditCost < 1),
    }),
  });
}
```

#### Chat Hooks

| Hook | Purpose | Returns |
|---|---|---|
| `useChat(conversationId)` | Core chat logic | `{ messages, send, isStreaming, abort }` |
| `useStreaming(options)` | SSE stream management | `{ isStreaming, content, abort, retry }` |
| `useChatScroll(ref)` | Auto-scroll to bottom | `{ scrollToBottom, isAtBottom, newMessageCount }` |
| `useModelSelector()` | Model selection state | `{ selected, setSelected, grouped, search }` |
| `useChatInput()` | Input state management | `{ value, setValue, submit, isDisabled }` |

```typescript
// Example: useChatScroll hook
export function useChatScroll(containerRef: RefObject<HTMLDivElement>) {
  const [isAtBottom, setIsAtBottom] = useState(true);
  const [newMessageCount, setNewMessageCount] = useState(0);

  // Scroll to bottom on new messages (only if already at bottom)
  // Show "N new messages" button if scrolled up
  // Intersection observer on last message for auto-scroll detection

  return {
    scrollToBottom: () => { /* smooth scroll */ },
    isAtBottom,
    newMessageCount,
  };
}
```

#### UI Hooks

| Hook | Purpose | Returns |
|---|---|---|
| `useDebounce(value, ms)` | Debounce rapidly changing values | `debouncedValue` |
| `useMediaQuery(query)` | Responsive breakpoint detection | `boolean` |
| `useLocalStorage(key, init)` | Typed localStorage wrapper | `[value, setValue]` |
| `useClipboard()` | Copy to clipboard with feedback | `{ copy, copied, error }` |
| `useKeyboardShortcut(key, fn)` | Register keyboard shortcuts | `void` |
| `useIntersectionObserver(ref)` | Element visibility detection | `{ isIntersecting, entry }` |
| `useOnClickOutside(ref, fn)` | Click outside detection | `void` |
| `useScrollPosition()` | Window scroll position | `{ x, y, direction }` |
| `useNetworkStatus()` | Online/offline detection | `{ isOnline, wasOffline }` |
| `usePrevious(value)` | Previous render value | `previousValue` |
| `useCountAnimation(target)` | Animated number counting | `{ displayValue, isAnimating }` |
| `useFormPersist(formId)` | Persist form state across sessions | `{ onSave, onRestore, clear }` |

```typescript
// Example: useClipboard hook
export function useClipboard(timeout = 2000) {
  const [copied, setCopied] = useState(false);

  const copy = useCallback(async (text: string) => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), timeout);
  }, [timeout]);

  return { copy, copied };
}
```

#### Auth Hooks

| Hook | Purpose | Returns |
|---|---|---|
| `useAuth()` | Auth state and methods | `{ user, isAuthenticated, login, logout }` |
| `useSession()` | Session data | `{ session, status, update }` |
| `useRequireAuth(role?)` | Redirect if unauthenticated | `{ user }` (redirects if null) |
| `usePermissions()` | Role-based access checks | `{ can(action), role }` |

### 6.2 Compound Component Patterns

**ModelSelector Compound Component:**
```typescript
// Usage:
<ModelSelector value={model} onChange={setModel}>
  <ModelSelector.Trigger>
    <ModelSelector.SelectedIcon />
    <ModelSelector.SelectedName />
    <ModelSelector.CreditCost />
  </ModelSelector.Trigger>
  <ModelSelector.Content>
    <ModelSelector.Search placeholder="Buscar modelo..." />
    <ModelSelector.Group label="Populares">
      <ModelSelector.Option value="gpt-4o" />
      <ModelSelector.Option value="claude-3.5-sonnet" />
    </ModelSelector.Group>
    <ModelSelector.Group label="Economicos">
      <ModelSelector.Option value="gpt-4o-mini" />
    </ModelSelector.Group>
  </ModelSelector.Content>
</ModelSelector>
```

**MessageBubble Compound Component:**
```typescript
<MessageBubble variant="assistant" timestamp={msg.createdAt}>
  <MessageBubble.Header>
    <MessageBubble.ModelIcon model={msg.model} />
    <MessageBubble.ModelName />
    <MessageBubble.Timestamp />
  </MessageBubble.Header>
  <MessageBubble.Content>
    <MarkdownRenderer content={msg.content} />
  </MessageBubble.Content>
  <MessageBubble.Actions>
    <MessageBubble.CopyAction />
    <MessageBubble.RegenerateAction />
    <MessageBubble.RateAction />
  </MessageBubble.Actions>
</MessageBubble>
```

**CreditDisplay Compound Component:**
```typescript
<CreditDisplay>
  <CreditDisplay.Icon />
  <CreditDisplay.Balance />     {/* Animated count */}
  <CreditDisplay.Label />       {/* "creditos" */}
  <CreditDisplay.Progress />    {/* Optional bar/gauge */}
  <CreditDisplay.Alert />       {/* Low credit warning */}
</CreditDisplay>
```

### 6.3 Higher-Order Component Patterns

HOCs are used sparingly, only where cross-cutting concerns require wrapping:

```typescript
// withAuth: Protects routes requiring authentication
export function withAuth<P extends object>(
  Component: ComponentType<P>,
  options?: { requiredRole?: UserRole; redirectTo?: string }
) {
  return function AuthenticatedComponent(props: P) {
    const { user, isLoading } = useAuth();
    const router = useRouter();

    if (isLoading) return <PageSkeleton />;
    if (!user) {
      router.replace(options?.redirectTo ?? '/login');
      return null;
    }
    if (options?.requiredRole && user.role !== options.requiredRole) {
      return <ForbiddenPage />;
    }
    return <Component {...props} />;
  };
}

// withErrorBoundary: Wraps feature components with error boundary
export function withErrorBoundary<P extends object>(
  Component: ComponentType<P>,
  fallback?: ReactNode
) {
  return function BoundedComponent(props: P) {
    return (
      <ErrorBoundary fallback={fallback ?? <ErrorState />}>
        <Component {...props} />
      </ErrorBoundary>
    );
  };
}
```

**Note:** Prefer hooks over HOCs. HOCs are used only for `withAuth` (route protection) and `withErrorBoundary` (error isolation). All other shared logic uses custom hooks.

### 6.4 Context Providers Architecture

```
RootLayout
└── Providers (src/components/providers/)
    ├── ThemeProvider           # Dark/light mode (next-themes)
    │   ├── IntlProvider        # next-intl with PT-BR messages
    │   │   ├── QueryProvider   # TanStack Query client
    │   │   │   ├── AuthProvider  # NextAuth session
    │   │   │   │   ├── WebSocketProvider  # WS connection
    │   │   │   │   │   ├── ToastProvider    # Sonner toasts
    │   │   │   │   │   │   └── {children}   # App content
```

```typescript
// src/components/providers/Providers.tsx
export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider>
      <IntlProvider locale="pt-BR" messages={messages}>
        <QueryProvider>
          <AuthProvider>
            <WebSocketProvider>
              <ToastProvider />
              {children}
            </WebSocketProvider>
          </AuthProvider>
        </QueryProvider>
      </IntlProvider>
    </ThemeProvider>
  );
}
```

**Provider Responsibilities:**

| Provider | State Managed | Re-render Scope |
|---|---|---|
| ThemeProvider | theme: 'light' / 'dark' / 'system' | Entire app (CSS-only, no JS re-render) |
| IntlProvider | locale, messages | Entire app (rare change) |
| QueryProvider | Query cache, defaults | None (cache is external) |
| AuthProvider | Session, user, tokens | Auth-dependent components |
| WebSocketProvider | WS connection state, message handlers | Components subscribed to WS events |

**Zustand stores are NOT wrapped in providers.** They are imported directly, avoiding unnecessary context re-renders. Zustand's `useStore` with selectors ensures minimal re-renders:

```typescript
// Only re-renders when balance changes, not on other store updates
const balance = useCreditStore((state) => state.balance);
```

### 6.5 Render Props (Limited Use)

Render props are used only where children need access to internal state for maximum flexibility:

```typescript
// Virtualized list with render prop for custom item rendering
<VirtualList
  items={conversations}
  itemHeight={72}
  renderItem={(conversation, index, style) => (
    <ConversationItem
      key={conversation.id}
      conversation={conversation}
      style={style}
      isActive={conversation.id === activeId}
    />
  )}
/>

// File drop zone with render prop for custom UI
<FileDropZone
  accept={['.pdf', '.docx', '.txt']}
  maxSize={10 * 1024 * 1024} // 10MB
  onDrop={handleUpload}
>
  {({ isDragActive, isDragReject, openFileDialog }) => (
    <div className={cn(
      'border-2 border-dashed rounded-lg p-8 text-center',
      isDragActive && 'border-primary bg-primary-soft',
      isDragReject && 'border-error bg-error-soft'
    )}>
      {isDragActive ? 'Solte o arquivo aqui' : 'Arraste ou clique'}
    </div>
  )}
</FileDropZone>
```

---

## 7. Navigation and Routing

### 7.1 Complete Route Map

| Route | Page | Auth | Roles | Layout Group |
|---|---|---|---|---|
| `/` | Landing page | Public | All | (marketing) |
| `/pricing` | Pricing plans | Public | All | (marketing) |
| `/about` | About page | Public | All | (marketing) |
| `/blog` | Blog listing | Public | All | (marketing) |
| `/blog/[slug]` | Blog post | Public | All | (marketing) |
| `/terms` | Terms of service | Public | All | (marketing) |
| `/privacy` | Privacy policy | Public | All | (marketing) |
| `/login` | Login | Guest only | All | (auth) |
| `/register` | Registration | Guest only | All | (auth) |
| `/forgot-password` | Password reset request | Guest only | All | (auth) |
| `/reset-password` | Password reset form | Guest only | All | (auth) |
| `/verify-email` | Email verification | Guest only | All | (auth) |
| `/onboarding` | Step 1: Persona | Authenticated | user | (platform) |
| `/onboarding/models` | Step 2: Model prefs | Authenticated | user | (platform) |
| `/onboarding/plan` | Step 3: Plan | Authenticated | user | (platform) |
| `/onboarding/complete` | Step 4: Success | Authenticated | user | (platform) |
| `/chat` | New conversation | Authenticated | user | (platform) |
| `/chat/[conversationId]` | Active conversation | Authenticated | user | (platform) |
| `/explore` | Model explorer | Authenticated | user | (platform) |
| `/documents` | Document list | Authenticated | user | (platform) |
| `/documents/[documentId]` | Document Q&A | Authenticated | user | (platform) |
| `/history` | Conversation history | Authenticated | user | (platform) |
| `/settings` | Redirect -> profile | Authenticated | user | (platform) |
| `/settings/profile` | User profile | Authenticated | user | (platform) |
| `/settings/billing` | Billing & credits | Authenticated | user | (platform) |
| `/settings/api-keys` | API key management | Authenticated | user | (platform) |
| `/settings/preferences` | App preferences | Authenticated | user | (platform) |
| `/checkout` | Payment flow | Authenticated | user | (platform) |
| `/checkout/success` | Payment success | Authenticated | user | (platform) |
| `/checkout/cancel` | Payment cancelled | Authenticated | user | (platform) |
| `/partner` | Partner overview | Authenticated | partner | (partner) |
| `/partner/coupons` | Coupon management | Authenticated | partner | (partner) |
| `/partner/coupons/[id]` | Coupon details | Authenticated | partner | (partner) |
| `/partner/analytics` | Partner analytics | Authenticated | partner | (partner) |
| `/partner/commissions` | Commission history | Authenticated | partner | (partner) |
| `/partner/settings` | Partner settings | Authenticated | partner | (partner) |
| `/admin` | Admin dashboard | Authenticated | admin | (admin) |
| `/admin/users` | User management | Authenticated | admin | (admin) |
| `/admin/users/[userId]` | User detail | Authenticated | admin | (admin) |
| `/admin/models` | Model configuration | Authenticated | admin | (admin) |
| `/admin/billing` | Billing overview | Authenticated | admin | (admin) |
| `/admin/partners` | Partner management | Authenticated | admin | (admin) |
| `/admin/analytics` | Platform analytics | Authenticated | admin | (admin) |
| `/admin/system` | System health | Authenticated | admin | (admin) |

### 7.2 Protected Routes Implementation

```typescript
// src/middleware.ts
import { auth } from '@/lib/auth';
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const publicRoutes = ['/', '/pricing', '/about', '/blog', '/terms', '/privacy'];
const authRoutes = ['/login', '/register', '/forgot-password', '/reset-password'];
const partnerRoutes = ['/partner'];
const adminRoutes = ['/admin'];

export default auth((req) => {
  const { pathname } = req.nextUrl;
  const isAuthenticated = !!req.auth;
  const userRole = req.auth?.user?.role;

  // Public routes: accessible by everyone
  if (publicRoutes.some(r => pathname === r || pathname.startsWith(r + '/'))) {
    return NextResponse.next();
  }

  // Auth routes: redirect to /chat if already authenticated
  if (authRoutes.some(r => pathname.startsWith(r))) {
    if (isAuthenticated) {
      return NextResponse.redirect(new URL('/chat', req.url));
    }
    return NextResponse.next();
  }

  // All other routes require authentication
  if (!isAuthenticated) {
    const loginUrl = new URL('/login', req.url);
    loginUrl.searchParams.set('callbackUrl', pathname);
    return NextResponse.redirect(loginUrl);
  }

  // Onboarding check: redirect if not completed
  if (!req.auth?.user?.onboardingComplete && !pathname.startsWith('/onboarding')) {
    return NextResponse.redirect(new URL('/onboarding', req.url));
  }

  // Role-based access
  if (partnerRoutes.some(r => pathname.startsWith(r)) && userRole !== 'partner' && userRole !== 'admin') {
    return NextResponse.redirect(new URL('/chat', req.url));
  }

  if (adminRoutes.some(r => pathname.startsWith(r)) && userRole !== 'admin') {
    return NextResponse.redirect(new URL('/chat', req.url));
  }

  return NextResponse.next();
});

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico|fonts|images).*)'],
};
```

### 7.3 Role-Based Access Control

| Role | Access |
|---|---|
| `guest` | Public pages, auth pages |
| `user` | Platform pages (chat, documents, settings, checkout) |
| `partner` | All user pages + partner dashboard |
| `admin` | All pages including admin panel |

```typescript
// src/hooks/usePermissions.ts
type Permission =
  | 'chat:send'
  | 'chat:duel'
  | 'documents:upload'
  | 'models:all'
  | 'partner:view'
  | 'partner:coupons'
  | 'admin:users'
  | 'admin:models'
  | 'admin:billing'
  | 'admin:system';

const rolePermissions: Record<UserRole, Permission[]> = {
  user: ['chat:send', 'chat:duel', 'documents:upload', 'models:all'],
  partner: ['chat:send', 'chat:duel', 'documents:upload', 'models:all',
            'partner:view', 'partner:coupons'],
  admin: ['chat:send', 'chat:duel', 'documents:upload', 'models:all',
          'partner:view', 'partner:coupons',
          'admin:users', 'admin:models', 'admin:billing', 'admin:system'],
};

export function usePermissions() {
  const { user } = useAuth();
  return {
    can: (permission: Permission) =>
      rolePermissions[user?.role ?? 'user']?.includes(permission) ?? false,
    role: user?.role ?? 'user',
  };
}
```

### 7.4 Deep Linking

| Feature | Deep Link Format | Example |
|---|---|---|
| Specific conversation | `/chat/[conversationId]` | `/chat/abc123` |
| Conversation with model | `/chat?model=gpt-4o` | Pre-selects model |
| Document Q&A | `/documents/[documentId]` | `/documents/xyz789` |
| Specific plan checkout | `/checkout?plan=pro` | Pre-selects plan |
| Coupon checkout | `/checkout?plan=pro&coupon=TECH2026` | Auto-applies coupon |
| Partner referral | `/register?ref=PARTNER_CODE` | Tracks partner referral |
| Model explorer | `/explore?provider=openai` | Filtered by provider |
| Settings section | `/settings/billing` | Direct to billing |
| Admin user | `/admin/users/[userId]` | Specific user view |

**URL State Management with `nuqs`:**
```typescript
// Type-safe search params
const [model, setModel] = useQueryState('model');
const [provider, setProvider] = useQueryState('provider');
const [page, setPage] = useQueryState('page', parseAsInteger.withDefault(1));
const [sort, setSort] = useQueryState('sort', parseAsStringLiteral(['recent', 'popular', 'cost']));
```

### 7.5 Breadcrumb System

**Auto-generated breadcrumbs from route segments:**

```typescript
// src/components/molecules/Breadcrumbs.tsx
const routeLabels: Record<string, string> = {
  chat: 'Chat',
  explore: 'Explorar Modelos',
  documents: 'Documentos',
  history: 'Historico',
  settings: 'Configuracoes',
  profile: 'Perfil',
  billing: 'Cobranca',
  'api-keys': 'Chaves API',
  preferences: 'Preferencias',
  partner: 'Painel do Parceiro',
  coupons: 'Cupons',
  analytics: 'Analiticos',
  commissions: 'Comissoes',
  admin: 'Administracao',
  users: 'Usuarios',
  models: 'Modelos',
  system: 'Sistema',
  checkout: 'Pagamento',
  onboarding: 'Primeiros Passos',
};
```

**Breadcrumb Examples:**
- Chat: `Inicio > Chat > Conversa sobre React Hooks`
- Settings: `Inicio > Configuracoes > Cobranca`
- Partner: `Painel do Parceiro > Cupons > TECH2026`
- Admin: `Administracao > Usuarios > joao@email.com`

**Implementation:**
```
AppHeader
└── Breadcrumbs
    ├── BreadcrumbItem (Home icon, link to /chat)
    ├── BreadcrumbSeparator (/)
    ├── BreadcrumbItem (segment label, link)
    ├── BreadcrumbSeparator (/)
    └── BreadcrumbItem (current page, no link, text-foreground)
```

---

## 8. Performance

### 8.1 Code Splitting Strategy

**Route-Level Splitting (automatic via Next.js App Router):**
Each route segment in the `app/` directory is automatically code-split. Users only download JavaScript for the current page.

**Feature-Level Splitting:**
```typescript
// Heavy components loaded on demand
const SavingsCalculator = dynamic(() =>
  import('@/features/landing/components/SavingsCalculator'), {
  loading: () => <SavingsCalculatorSkeleton />,
  ssr: false, // Client-only interactive calculator
});

const SplitView = dynamic(() =>
  import('@/features/chat/components/SplitView'), {
  loading: () => <SplitViewSkeleton />,
});

const DocumentPreview = dynamic(() =>
  import('@/features/documents/components/DocumentPreview'), {
  loading: () => <DocumentPreviewSkeleton />,
  ssr: false,
});

const AdminDashboard = dynamic(() =>
  import('@/features/admin/components/AdminDashboard'), {
  loading: () => <DashboardSkeleton />,
});

// Chart libraries loaded only when visible
const UsageChart = dynamic(() =>
  import('@/features/credits/components/UsageChart'), {
  ssr: false,
});
```

**Library Splitting:**
```javascript
// next.config.ts
const nextConfig = {
  experimental: {
    optimizePackageImports: [
      'lucide-react',       // Tree-shake unused icons
      'recharts',           // Only import used chart types
      'date-fns',           // Only import used functions
      'framer-motion',      // Tree-shake animation utilities
    ],
  },
};
```

### 8.2 Image Optimization

| Strategy | Implementation |
|---|---|
| Next.js Image | `<Image>` component for all images with automatic WebP/AVIF |
| Responsive sizes | `sizes` prop set per usage context |
| Blur placeholder | `placeholder="blur"` with `blurDataURL` for LCP images |
| Priority loading | `priority` flag on hero image and above-fold content |
| SVG for icons | Inline SVGs for AI provider logos (no image requests) |
| Lazy loading | Default `loading="lazy"` for below-fold images |
| Remote patterns | Configured for user avatars (Gravatar, OAuth providers) |

```typescript
// next.config.ts
const nextConfig = {
  images: {
    formats: ['image/avif', 'image/webp'],
    deviceSizes: [640, 768, 1024, 1280, 1536],
    imageSizes: [16, 32, 48, 64, 96, 128, 256],
    remotePatterns: [
      { protocol: 'https', hostname: '*.googleusercontent.com' },
      { protocol: 'https', hostname: '*.githubusercontent.com' },
      { protocol: 'https', hostname: 'avatars.githubusercontent.com' },
    ],
  },
};
```

### 8.3 Bundle Size Budgets

| Bundle | Budget (gzipped) | Contents |
|---|---|---|
| Framework | 85 KB | React 19, Next.js runtime, React DOM |
| App Shell | 45 KB | Layout, sidebar, header, navigation, theme |
| Chat Page | 40 KB | Message list, input, streaming, markdown |
| Landing Page | 30 KB | Hero, calculator, features, pricing |
| Auth Pages | 15 KB | Forms, validation, social buttons |
| Settings Pages | 25 KB | Forms, profile, billing |
| Checkout | 20 KB | Payment form, plan comparison |
| Partner Dashboard | 35 KB | Tables, charts, coupon management |
| Admin Panel | 40 KB | Data tables, charts, system health |
| Shared Libraries | 50 KB | Zustand, TanStack Query, zod, date-fns |
| **Total first load** | **< 180 KB** | Framework + Shell + current route |

**Monitoring:**
```json
// package.json
{
  "scripts": {
    "analyze": "ANALYZE=true next build",
    "size": "npx size-limit"
  }
}
```

```json
// .size-limit.json
[
  { "path": ".next/static/chunks/main-*.js", "limit": "85 KB" },
  { "path": ".next/static/chunks/app/layout-*.js", "limit": "45 KB" },
  { "path": ".next/static/chunks/app/(platform)/chat/page-*.js", "limit": "40 KB" }
]
```

### 8.4 Caching Strategy

**Multi-Layer Cache Architecture:**

| Layer | Cache Type | TTL | Purpose |
|---|---|---|---|
| CDN (Vercel Edge) | Static assets | 1 year (immutable) | JS, CSS, images, fonts |
| CDN (Vercel Edge) | ISR pages | 1 hour | Landing, pricing, blog |
| Next.js Data Cache | `fetch` with `revalidate` | Varies | Server-side API responses |
| Next.js Full Route Cache | Static routes | Build time | Marketing pages |
| TanStack Query | Client memory | Per-query config | API responses |
| Service Worker | Cache-first | Varies | Offline shell, assets |
| LocalStorage | Persisted | Indefinite | Theme, locale, draft messages |

**TanStack Query Cache Configuration:**

```typescript
// src/lib/queryClient.ts
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000,           // 1 minute default
      gcTime: 5 * 60 * 1000,          // 5 minute garbage collection
      retry: 2,
      refetchOnWindowFocus: true,
      refetchOnReconnect: true,
    },
  },
});

// Per-query overrides:
const cacheConfig = {
  models:        { staleTime: 5 * 60 * 1000 },   // 5 min (rarely changes)
  credits:       { staleTime: 10 * 1000 },         // 10 sec (near real-time)
  conversations: { staleTime: 30 * 1000 },         // 30 sec
  user:          { staleTime: 5 * 60 * 1000 },     // 5 min
  partnerStats:  { staleTime: 60 * 1000 },          // 1 min
};
```

**Server-Side Caching (RSC):**
```typescript
// Cached fetch in Server Components
const models = await fetch(`${API_URL}/models`, {
  next: {
    revalidate: 300,        // Revalidate every 5 minutes
    tags: ['models'],       // Tag for on-demand revalidation
  },
});

// On-demand revalidation (via webhook/admin action)
import { revalidateTag } from 'next/cache';
revalidateTag('models');    // Purge model cache when admin updates
```

### 8.5 Prefetching

| Strategy | Implementation | Trigger |
|---|---|---|
| Route prefetch | `<Link prefetch>` (default in Next.js) | Link enters viewport |
| Query prefetch | `queryClient.prefetchQuery` | Hover intent (200ms delay) |
| Conversation prefetch | Prefetch messages on hover | Mouse enters conversation item |
| Model data prefetch | Prefetch on chat page load | Chat page mount |
| Checkout prefetch | Prefetch plan data on "Upgrade" hover | Hover on upgrade CTA |

```typescript
// Prefetch conversation messages on hover
function ConversationItem({ conversation }: Props) {
  const queryClient = useQueryClient();

  const handleMouseEnter = () => {
    queryClient.prefetchQuery({
      queryKey: ['conversation', conversation.id, 'messages'],
      queryFn: () => chatService.getMessages(conversation.id),
      staleTime: 30 * 1000,
    });
  };

  return (
    <Link
      href={`/chat/${conversation.id}`}
      onMouseEnter={handleMouseEnter}
    >
      {/* ... */}
    </Link>
  );
}
```

**Prefetch Strategy for Navigation:**
```typescript
// Prefetch common destinations on app shell mount
export function PlatformLayout({ children }) {
  const queryClient = useQueryClient();

  useEffect(() => {
    // Prefetch data likely needed soon
    queryClient.prefetchQuery({
      queryKey: ['models'],
      queryFn: modelService.getModels,
    });
    queryClient.prefetchQuery({
      queryKey: ['credits'],
      queryFn: creditService.getBalance,
    });
    queryClient.prefetchQuery({
      queryKey: ['conversations'],
      queryFn: chatService.getConversations,
    });
  }, []);

  return <>{children}</>;
}
```

### 8.6 Virtualization for Long Lists

| List | Library | Trigger |
|---|---|---|
| Conversation sidebar | `@tanstack/react-virtual` | Always (can have hundreds) |
| Chat message history | `@tanstack/react-virtual` | > 100 messages |
| Credit transaction history | `@tanstack/react-virtual` | Always (unbounded) |
| Admin user table | `@tanstack/react-virtual` | > 50 rows |
| Model catalog | Not virtualized | < 50 items (paginated) |
| Document list | Not virtualized | < 100 items (paginated) |

```typescript
// Virtualized conversation list
import { useVirtualizer } from '@tanstack/react-virtual';

function ConversationList({ conversations }: Props) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: conversations.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 72,              // Estimated item height
    overscan: 5,                         // Extra items above/below viewport
  });

  return (
    <ScrollArea ref={parentRef} className="h-full">
      <div style={{ height: virtualizer.getTotalSize() }}>
        {virtualizer.getVirtualItems().map((virtualItem) => (
          <ConversationItem
            key={conversations[virtualItem.index].id}
            conversation={conversations[virtualItem.index]}
            style={{
              position: 'absolute',
              top: virtualItem.start,
              height: virtualItem.size,
              width: '100%',
            }}
          />
        ))}
      </div>
    </ScrollArea>
  );
}
```

**Chat Message Virtualization with Bidirectional Scroll:**
```typescript
// Messages load newest-first; scrolling up loads older messages
function MessageList({ conversationId }: Props) {
  const parentRef = useRef<HTMLDivElement>(null);

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ['messages', conversationId],
    queryFn: ({ pageParam }) =>
      chatService.getMessages(conversationId, { cursor: pageParam }),
    getNextPageParam: (lastPage) => lastPage.nextCursor,
    initialPageParam: undefined,
  });

  const allMessages = data?.pages.flatMap(p => p.messages) ?? [];

  const virtualizer = useVirtualizer({
    count: allMessages.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 120,
    overscan: 10,
  });

  // Load more when scrolling near top
  useEffect(() => {
    const [firstItem] = virtualizer.getVirtualItems();
    if (firstItem?.index === 0 && hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [virtualizer.getVirtualItems()]);

  return (
    <ScrollArea ref={parentRef} className="flex-1">
      <div style={{ height: virtualizer.getTotalSize() }}>
        {virtualizer.getVirtualItems().map((virtualItem) => (
          <MessageBubble
            key={allMessages[virtualItem.index].id}
            message={allMessages[virtualItem.index]}
            style={{
              position: 'absolute',
              top: virtualItem.start,
              width: '100%',
            }}
          />
        ))}
      </div>
    </ScrollArea>
  );
}
```

### 8.7 Additional Performance Optimizations

**React 19 Optimizations:**
```typescript
// use() for suspense-based data loading in Server Components
async function ChatPage({ params }: Props) {
  const messagesPromise = chatService.getMessages(params.conversationId);
  return (
    <Suspense fallback={<MessagesSkeleton />}>
      <MessageList messagesPromise={messagesPromise} />
    </Suspense>
  );
}

// useOptimistic for instant credit updates
function ChatInput() {
  const { balance } = useCredits();
  const [optimisticBalance, updateOptimistic] = useOptimistic(
    balance,
    (current, cost: number) => current - cost
  );

  const handleSend = async (message: string) => {
    updateOptimistic(estimatedCost);
    await sendMessage(message);
  };
}

// useTransition for non-urgent updates
function ModelSelector() {
  const [isPending, startTransition] = useTransition();

  const handleModelChange = (model: Model) => {
    startTransition(() => {
      setSelectedModel(model);
      // Model change triggers re-fetch of pricing, suggestions
    });
  };
}
```

**Memo Strategies:**
```typescript
// Memoize expensive markdown rendering
const MemoizedMarkdown = memo(MarkdownRenderer, (prev, next) =>
  prev.content === next.content
);

// Memoize message list items (messages are immutable once received)
const MemoizedMessageBubble = memo(MessageBubble);

// Memoize model options (changes rarely)
const MemoizedModelOption = memo(ModelOption);
```

**Web Worker for Heavy Processing:**
```typescript
// Offload markdown parsing for long AI responses
// src/workers/markdownWorker.ts
const worker = new Worker(
  new URL('../workers/markdownWorker.ts', import.meta.url)
);

// Used for responses > 5000 characters
function useMarkdownWorker(content: string) {
  const [html, setHtml] = useState('');
  useEffect(() => {
    if (content.length > 5000) {
      worker.postMessage(content);
      worker.onmessage = (e) => setHtml(e.data);
    }
  }, [content]);
  return html;
}
```

---

## Appendix A: Development Standards

### A.1 File Naming Conventions

| Type | Convention | Example |
|---|---|---|
| Components | PascalCase | `ChatInput.tsx`, `ModelCard.tsx` |
| Hooks | camelCase with `use` prefix | `useChat.ts`, `useCredits.ts` |
| Stores | camelCase with `Store` suffix | `chatStore.ts`, `uiStore.ts` |
| Services | camelCase with `Service` suffix | `chatService.ts` |
| Types | camelCase with `.types.ts` suffix | `chat.types.ts` |
| Schemas | camelCase with `Schema` suffix | `loginSchema.ts` |
| Utils | camelCase | `formatters.ts`, `cn.ts` |
| Constants | camelCase or UPPER_SNAKE | `constants.ts` |
| Tests | Same as source + `.test.ts` | `ChatInput.test.tsx` |
| Stories | Same as source + `.stories.tsx` | `ChatInput.stories.tsx` |

### A.2 Import Order

```typescript
// 1. React / Next.js
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

// 2. Third-party libraries
import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';

// 3. Internal aliases (@/)
import { cn } from '@/lib/cn';
import { Button } from '@/components/ui/button';

// 4. Feature-relative imports
import { useChat } from '../hooks/useChat';
import { MessageBubble } from './MessageBubble';

// 5. Types (type-only imports)
import type { Message, Conversation } from '@/types/chat.types';

// 6. Styles (if any non-Tailwind CSS)
import './ChatInput.css';
```

### A.3 Component Structure Template

```typescript
// 1. Imports
// 2. Types/Interfaces
// 3. Component (named export)
// 4. Sub-components (if compound)
// 5. Default export (if page)

'use client'; // Only if needed

import { useState } from 'react';
import { cn } from '@/lib/cn';
import type { ComponentProps } from './types';

interface ChatInputProps {
  conversationId: string;
  onSend: (message: string) => Promise<void>;
  disabled?: boolean;
  className?: string;
}

export function ChatInput({
  conversationId,
  onSend,
  disabled = false,
  className,
}: ChatInputProps) {
  const [value, setValue] = useState('');

  // hooks, handlers, effects...

  return (
    <div className={cn('flex items-center gap-2', className)}>
      {/* JSX */}
    </div>
  );
}
```

### A.4 Environment Variables

```bash
# .env.example
# API
NEXT_PUBLIC_API_URL=http://localhost:8000/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8000/ws

# Auth
AUTH_SECRET=
AUTH_GOOGLE_ID=
AUTH_GOOGLE_SECRET=
AUTH_GITHUB_ID=
AUTH_GITHUB_SECRET=

# Payments
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=

# Analytics
NEXT_PUBLIC_POSTHOG_KEY=
NEXT_PUBLIC_POSTHOG_HOST=

# Feature flags
NEXT_PUBLIC_ENABLE_DUEL_MODE=true
NEXT_PUBLIC_ENABLE_DOCUMENT_QA=true
NEXT_PUBLIC_ENABLE_PARTNER_DASHBOARD=true
```

---

*End of Frontend Architecture Document*
