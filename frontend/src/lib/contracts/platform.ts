export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export type SearchEntityKind = 'task' | 'repository' | 'environment' | 'setting' | 'workspace' | 'conversation' | 'prompt';

export interface SessionUser {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string | null;
  role: string;
  status: string;
}

export interface WorkspaceDto {
  id: string;
  slug: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceMembershipDto {
  membershipId: string;
  role: WorkspaceRole;
  workspace: WorkspaceDto;
}

export interface SessionContext {
  user: SessionUser;
  workspaces: WorkspaceMembershipDto[];
  currentWorkspace: WorkspaceDto | null;
  currentRole: WorkspaceRole | null;
  hasWorkspace: boolean;
  organizationId?: string | null;
  organizationSlug?: string | null;
}

export interface ChatConversationDto {
  id: string;
  workspaceId: string;
  createdById: string;
  title: string;
  model: string;
  pinned: boolean;
  archivedAt?: string | null;
  lastMessageAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ConsumerChatMessageDto {
  id: string;
  conversationId: string;
  role: 'user' | 'assistant' | 'error';
  content: string;
  modelUsed?: string | null;
  providerUsed?: string | null;
  agentUsed?: string | null;
  agentVersion?: string | null;
  fallbackUsed?: boolean;
  attempts?: number | null;
  isComplete: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ChatAttachmentDto {
  id: string;
  messageId: string;
  title: string;
  contentType: string;
  url: string;
  createdAt: string;
}

export interface PromptTemplateDto {
  id: string;
  workspaceId?: string | null;
  scope: 'SYSTEM' | 'WORKSPACE';
  category: string;
  title: string;
  description: string;
  prompt: string;
  tag?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserPreferencesDto {
  fullName: string;
  locale: string;
  theme: 'light' | 'dark' | 'system';
  chatFontMode: 'default' | 'sans' | 'system' | 'dyslexia';
}

export interface NotificationPreferencesDto {
  emailEnabled: boolean;
  pushEnabled: boolean;
  billingAlerts: boolean;
  usageAlerts: boolean;
  securityAlerts: boolean;
  productUpdates: boolean;
}

export interface BillingSummaryDto {
  currentPlanId?: string | null;
  currentPlanName: string;
  tokensUsed: number;
  monthlyLimit: number;
  includedUsageLeft: number;
  balance: number;
  usageAvailable?: boolean;
}

export interface ProjectDto {
  id: string;
  workspaceId: string;
  name: string;
  description?: string | null;
  status?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskMessageDto {
  id: string;
  taskId: string;
  role: 'user' | 'assistant' | 'system' | 'tool';
  content: string;
  createdAt: string;
}

export interface TaskRunDto {
  id: string;
  taskId: string;
  runNumber: number;
  status: string;
  startedAt?: string | null;
  completedAt?: string | null;
  failureReason?: string | null;
}

export interface TaskStepDto {
  id: string;
  taskId: string;
  title: string;
  status: string;
  startedAt?: string | null;
  completedAt?: string | null;
  detail?: string | null;
}

export interface TaskArtifactDto {
  id: string;
  taskId: string;
  artifactType: string;
  title: string;
  contentType: string;
  url: string;
  createdAt: string;
}

export interface ScheduleDto {
  id: string;
  taskId: string;
  cron: string;
  timezone?: string | null;
  enabled: boolean;
  nextRunAt?: string | null;
}

export interface TaskDto {
  id: string;
  workspaceId: string;
  projectId?: string | null;
  title: string;
  prompt: string;
  mode: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
  failedAt?: string | null;
  archivedAt?: string | null;
}

export interface ProviderDto {
  id: string;
  slug: string;
  name: string;
  status?: string;
  category?: string;
  protocol?: string;
  summary?: string;
  docsUrl?: string;
  configured?: boolean;
  missingKeys?: string[];
  supportsLiveChat?: boolean;
}

export interface ModelDto {
  id: string;
  providerId: string;
  slug: string;
  name: string;
  modality?: string;
  status?: string;
  description?: string;
  availability?: string;
  maxContextTokens?: number;
  enabledForChat?: boolean;
  tags?: string[];
}

export interface AgentDto {
  id: string;
  slug: string;
  label: string;
  version: string;
  scope: string;
  summary: string;
  preferredModelIds: string[];
  capabilities: string[];
}

export interface ProviderCredentialRequirementDto {
  providerId: string;
  providerName: string;
  category: string;
  docsUrl: string;
  env: Array<{
    key: string;
    required: boolean;
    description: string;
  }>;
}

export interface UsageSummaryDto {
  workspaceId: string;
  totalUsage: number;
  totalCreditsRemaining: number;
  period: string;
}

export interface BudgetDto {
  id: string;
  workspaceId: string;
  name: string;
  limitAmount: number;
  spentAmount: number;
  currency: string;
  period: string;
}

export interface WorkspaceSummary {
  workspace: WorkspaceDto;
  members: number;
  repositories: number;
  environments: number;
  tasks: {
    total: number;
    queued: number;
    running: number;
    completed: number;
    failed: number;
  };
  connectors: {
    github: number;
    slack: number;
    linear: number;
  };
  credits: {
    balance: number;
    includedUsageLeft: number;
  };
  recentTasks: TaskDto[];
}

export interface SearchResultItem {
  id: string;
  kind: SearchEntityKind;
  title: string;
  description?: string;
  href: string;
  metadata?: Record<string, string | number | boolean | null>;
}

export interface SearchResponse {
  query: string;
  items: SearchResultItem[];
}
