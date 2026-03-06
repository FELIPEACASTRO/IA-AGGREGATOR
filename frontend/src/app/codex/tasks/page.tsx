import { redirect } from 'next/navigation';

export default async function CodexTasksPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const tabParam = params.tab;
  const tab = Array.isArray(tabParam) ? tabParam[0] : tabParam;
  if (tab) {
    redirect(`/codex?tab=${encodeURIComponent(tab)}`);
  }
  redirect('/codex');
}

