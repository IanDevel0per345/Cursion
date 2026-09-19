// GET /api/connections/:connector/callback - OAuth callback handler

import { OAuthManager } from "@/lib/oauth/manager";

export async function GET(
  request: Request,
  { params }: { params: { connector: string } }
) {
  try {
    const { connector } = params;
    const url = new URL(request.url);
    const code = url.searchParams.get("code");
    const state = url.searchParams.get("state");
    const error = url.searchParams.get("error");

    if (error) {
      throw new Error(`OAuth error: ${error}`);
    }

    if (!code || !state) {
      throw new Error("Missing code or state parameter");
    }

    const redirectUri = `${process.env.NEXT_PUBLIC_APP_URL}/api/connections/${connector}/callback`;

    const tokens = await OAuthManager.exchangeCode(connector, code, redirectUri, state);

    // TODO: Save tokens to database (encrypted)

    const successUrl = new URL("/integrations/connected", process.env.NEXT_PUBLIC_APP_URL);
    successUrl.searchParams.set("connector", connector);
    successUrl.searchParams.set("status", "success");

    return Response.redirect(successUrl.toString());
  } catch (error) {
    console.error("OAuth callback error:", error);
    const errorUrl = new URL("/integrations/connect", process.env.NEXT_PUBLIC_APP_URL);
    errorUrl.searchParams.set("connector", params.connector);
    errorUrl.searchParams.set("error", error instanceof Error ? error.message : "Unknown error");
    return Response.redirect(errorUrl.toString());
  }
}
