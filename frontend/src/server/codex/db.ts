import { PrismaClient } from '@prisma/client';

declare global {
  var __codexPrisma: PrismaClient | undefined;
}

export const codexDb =
  global.__codexPrisma ??
  new PrismaClient({
    log: process.env.NODE_ENV === 'development' ? ['warn', 'error'] : ['error'],
  });

if (process.env.NODE_ENV !== 'production') {
  global.__codexPrisma = codexDb;
}
