// POST /api/connections/:connector/connect - Start OAuth flow

import { OAuthManager } from "@/lib/oauth/manager";

export async function POST(
  request: Request,
  { params }: { params: { connector: string } }
) {
  try {
    const { connector } = params;
    const { redirectUri } = await request.json();

    const userId = "user_123"; // TODO: Get from auth session

    const { url, state } = await OAuthManager.getAuthorizationUrl(
      connector,
      userId,
      redirectUri || `${process.env.NEXT_PUBLIC_APP_URL}/api/connections/${connector}/callback`
    );

    return Response.json({
      success: true,
      data: { authorizationUrl: url, state },
    });
  } catch (error) {
    console.error("Error starting OAuth:", error);
    return Response.json(
      { success: false, error: error instanceof Error ? error.message : "Failed to start OAuth" },
      { status: 500 }
    );
  }
}
