'use client';

import { ReactNode } from 'react';
import { cn } from '@/lib/cn';

type PrimitiveProps = {
  children?: ReactNode;
  className?: string;
};

function PrimitiveCard({ children, className }: PrimitiveProps) {
  return (
    <div className={cn('rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4', className)}>
      {children}
    </div>
  );
}

export const AppShell = PrimitiveCard;
export const SidebarNav = PrimitiveCard;
export const TopBar = PrimitiveCard;
export const SearchCommandPalette = PrimitiveCard;
export const TaskComposer = PrimitiveCard;
export const ModeToggle = PrimitiveCard;
export const PromptInput = PrimitiveCard;
export const VoiceDictationButton = PrimitiveCard;
export const ImageAttachmentTray = PrimitiveCard;
export const RepoSelector = PrimitiveCard;
export const EnvironmentSelector = PrimitiveCard;
export const BranchPicker = PrimitiveCard;
export const AttemptsSelectorBestOfN = PrimitiveCard;
export const InternetPolicyBadge = PrimitiveCard;
export const RiskWarningPanel = PrimitiveCard;
export const TaskCard = PrimitiveCard;
export const TaskList = PrimitiveCard;
export const StatusBadge = PrimitiveCard;
export const PRStatusBadge = PrimitiveCard;
export const TaskTimeline = PrimitiveCard;
export const LiveLogViewer = PrimitiveCard;
export const LogPhaseFilter = PrimitiveCard;
export const DiffViewer = PrimitiveCard;
export const ChangedFilesTree = PrimitiveCard;
export const InlineCommentPopover = PrimitiveCard;
export const EvidenceCitation = PrimitiveCard;
export const TestResultPanel = PrimitiveCard;
export const ArtifactGallery = PrimitiveCard;
export const PullRequestPanel = PrimitiveCard;
export const UsageCharts = PrimitiveCard;
export const CreditLedgerTable = PrimitiveCard;
export const EnvironmentForm = PrimitiveCard;
export const SetupScriptEditor = PrimitiveCard;
export const MaintenanceScriptEditor = PrimitiveCard;
export const RuntimePinEditor = PrimitiveCard;
export const SecretField = PrimitiveCard;
export const EnvironmentVariablesTable = PrimitiveCard;
export const CodeReviewPolicyForm = PrimitiveCard;
export const ConnectorStatusCard = PrimitiveCard;
export const EmptyState = PrimitiveCard;
export const ErrorState = PrimitiveCard;
export const LoadingState = PrimitiveCard;
export const KeyboardShortcutsModal = PrimitiveCard;
export const ConfirmDialog = PrimitiveCard;
export const RetryTaskDialog = PrimitiveCard;
export const ArchiveDialog = PrimitiveCard;

