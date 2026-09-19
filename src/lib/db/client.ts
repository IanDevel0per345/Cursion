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
  return prisma.user.findUnique({ where: { email } });
}

export async function getUserConnections(userId: string) {
  return prisma.connection.findMany({
    where: { userId },
    include: { connector: true },
  });
}

export async function getConnection(id: string) {
  return prisma.connection.findUnique({
    where: { id },
    include: { connector: true },
  });
}

export async function createExecution(data: {
  connectionId: string;
  actionId: string;
  input?: any;
  output?: any;
  status: string;
  error?: string;
  duration?: number;
  userId: string;
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

export async function updateApiKeyLastUsed(id: string) {
  return prisma.apiKey.update({
    where: { id },
    data: { lastUsedAt: new Date() },
  });
}

export async function incrementUsage(
  userId: string,
  connectorId: string,
  actionId: string
) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return prisma.usageMetric.upsert({
    where: {
      userId_connectorId_actionId_date: {
        userId,
        connectorId,
        actionId,
        date: today,
      },
    },
    update: { count: { increment: 1 } },
    create: {
      userId,
      connectorId,
      actionId,
      count: 1,
      date: today,
    },
  });
}
