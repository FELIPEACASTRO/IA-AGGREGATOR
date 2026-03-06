import IORedis from 'ioredis';

const redisUrl = process.env.CODEX_REDIS_URL || 'redis://localhost:6379';

declare global {
  var __codexRedis: IORedis | undefined;
}

export const codexRedis =
  global.__codexRedis ??
  new IORedis(redisUrl, {
    maxRetriesPerRequest: null,
    lazyConnect: true,
  });

if (process.env.NODE_ENV !== 'production') {
  global.__codexRedis = codexRedis;
}

export async function ensureRedisConnected() {
  if (codexRedis.status === 'ready' || codexRedis.status === 'connecting') {
    return;
  }
  await codexRedis.connect();
}
