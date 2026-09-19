// POST /api/api-keys - Create new API key
// GET /api/api-keys - List API keys
// DELETE /api/api-keys/:id - Revoke API key

import { hash, verify } from "bcryptjs";

const API_KEYS: Array<{ id: string; name: string; keyHash: string; createdAt: string }> = [];

export async function GET() {
  try {
    // TODO: Get from database
    return Response.json({
      success: true,
      data: API_KEYS.map(k => ({
        id: k.id,
        name: k.name,
        createdAt: k.createdAt,
        lastUsedAt: null,
      })),
    });
  } catch (error) {
    return Response.json(
      { success: false, error: "Failed to list API keys" },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const { name } = await request.json();

    if (!name) {
      return Response.json(
        { success: false, error: "Name is required" },
        { status: 400 }
      );
    }

    // Generate API key
    const keyId = crypto.randomUUID();
    const apiKey = `cursion_${keyId}`;
    const keyHash = await hash(apiKey, 12);

    // TODO: Save to database
    API_KEYS.push({
      id: keyId,
      name,
      keyHash,
      createdAt: new Date().toISOString(),
    });

    return Response.json({
      success: true,
      data: {
        id: keyId,
        name,
        key: apiKey, // Only shown once!
        createdAt: new Date().toISOString(),
      },
    });
  } catch (error) {
    console.error("Create API key error:", error);
    return Response.json(
      { success: false, error: "Failed to create API key" },
      { status: 500 }
    );
  }
}
