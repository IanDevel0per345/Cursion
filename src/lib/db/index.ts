// Prisma Database Client for Cursion

import { PrismaClient } from "@prisma/client";

const globalForPrisma = globalThis as unknown as {
  prisma: PrismaClient | undefined;
};

export const prisma =
  globalForPrisma.prisma ??
  new PrismaClient({
    log: process.env.NODE_ENV === "development" ? ["query", "error", "warn"] : ["error"],
  });

if (process.env.NODE_ENV !== "production") globalForPrisma.prisma = prisma;

// Helper functions
export async function getUserByEmail(email: string) {
  return prisma.user.findUnique({
    where: { email },
    include: { connections: true, apiKeys: true },
  });
}

export async function getUserConnections(userId: string, connectorId?: string) {
  return prisma.connection.findMany({
    where: connectorId ? { userId, connectorId, active: true } : { userId, active: true },
    include: { connector: true },
  });
}

export async function createExecution(data: {
  connectionId: string;
  userId: string;
  actionId: string;
  connectorId: string;
  input?: any;
  output?: any;
  status: "pending" | "success" | "failed";
  error?: string;
  durationMs?: number;
}) {
  return prisma.execution.create({ data });
}

export async function getExecutions(userId: string, limit = 50) {
  return prisma.execution.findMany({
    where: { userId },
    orderBy: { startedAt: "desc" },
    take: limit,
    include: { connection: { include: { connector: true } } },
  });
}

export async function createApiKey(data: {
  userId: string;
  name: string;
  keyHash: string;
  prefix: string;
  expiresAt?: Date;
}) {
  return prisma.apiKey.create({ data });
}

export async function getApiKeys(userId: string) {
  return prisma.apiKey.findMany({
    where: { userId },
    select: {
      id: true,
      name: true,
      prefix: true,
      createdAt: true,
      lastUsedAt: true,
      expiresAt: true,
    },
  });
}

export async function verifyApiKey(keyHash: string) {
  const apiKey = await prisma.apiKey.findUnique({
    where: { keyHash },
    include: { user: true },
  });

  if (apiKey && (!apiKey.expiresAt || apiKey.expiresAt > new Date())) {
    await prisma.apiKey.update({
      where: { id: apiKey.id },
      data: { lastUsedAt: new Date() },
    });
    return apiKey;
  }

  return null;
}

export async function incrementDailyMetric(
  userId: string,
  connectorId: string,
  date: Date,
  delta: { executions?: number; errors?: number; avgDurationMs?: number }
) {
  return prisma.dailyMetric.upsert({
    where: {
      userId_connectorId_date: {
        userId,
        connectorId,
        date,
      },
    },
    update: {
      executions: { increment: delta.executions || 0 },
      errors: { increment: delta.errors || 0 },
    },
    create: {
      userId,
      connectorId,
      date,
      executions: delta.executions || 0,
      errors: delta.errors || 0,
      avgDurationMs: delta.avgDurationMs || 0,
    },
  });
}
