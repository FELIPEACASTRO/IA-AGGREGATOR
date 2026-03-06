import { TaskMode } from '@prisma/client';
import { CodexDashboardClient } from '@/app/codex/codex-dashboard-client';

export const dynamic = 'force-dynamic';

export default async function CodexDashboardPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const read = (key: string) => {
    const value = params[key];
    return Array.isArray(value) ? value[0] : value;
  };

  const tab = read('tab') || 'active';
  const prompt = read('prompt') || undefined;
  const environment = read('environment') || undefined;
  const branch = read('branch') || undefined;
  const modeRaw = read('mode');
  const mode = modeRaw === 'ASK' || modeRaw === 'CODE' ? (modeRaw as TaskMode) : undefined;

  return (
    <CodexDashboardClient
      tab={tab}
      prompt={prompt}
      environment={environment}
      branch={branch}
      mode={mode}
    />
  );
}

