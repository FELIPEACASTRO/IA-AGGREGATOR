import { TaskMode, TaskStatus } from '@prisma/client';

export type TaskCreatePayload = {
  prompt: string;
  mode: TaskMode;
  workspaceId: string;
  repositoryId?: string;
  environmentId?: string;
  baseBranch?: string;
  bestOfN?: number;
  sourceRef?: string;
  imageInputs?: string[];
  attachments?: string[];
  voiceTranscript?: string;
};

export type TaskEventPayload = {
  taskId: string;
  eventType: string;
  status?: TaskStatus;
  message?: string;
  metadata?: Record<string, unknown>;
};

