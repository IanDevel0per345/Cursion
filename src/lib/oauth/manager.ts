// OAuth2 Flow Manager for Cursion

import { getConnector } from "../connectors";
import type { OAuth2Config, ConnectionTokens } from "../connectors/base";

export interface OAuthState {
  connectorId: string;
  userId: string;
  redirectUri: string;
  createdAt: number;
}

export class OAuthManager {
  private static states: Map<string, OAuthState> = new Map();

  /**
   * Generate OAuth authorization URL
   */
  static async getAuthorizationUrl(
    connectorId: string,
    userId: string,
    redirectUri: string
  ): Promise<{ url: string; state: string }> {
    const connector = getConnector(connectorId);
    if (!connector || connector.authType !== "oauth2") {
      throw new Error(`Connector ${connectorId} not found or does not support OAuth2`);
    }

    const authConfig = connector.authConfig as OAuth2Config;
    const state = this.generateState(connectorId, userId, redirectUri);

    const params = new URLSearchParams({
      client_id: process.env[`${connectorId.toUpperCase()}_CLIENT_ID`] || "",
      redirect_uri: redirectUri,
      response_type: "code",
      scope: authConfig.scopes.join(" "),
      state,
    });

    const url = `${authConfig.authorizationUrl}?${params.toString()}`;
    return { url, state };
  }

  /**
   * Exchange authorization code for access token
   */
  static async exchangeCode(
    connectorId: string,
    code: string,
    redirectUri: string,
    state: string
  ): Promise<ConnectionTokens> {
    const connector = getConnector(connectorId);
    if (!connector || connector.authType !== "oauth2") {
      throw new Error(`Connector ${connectorId} not found or does not support OAuth2`);
    }

    // Verify state
    const savedState = this.states.get(state);
    if (!savedState || savedState.connectorId !== connectorId) {
      throw new Error("Invalid or expired state parameter");
    }
    this.states.delete(state);

    const authConfig = connector.authConfig as OAuth2Config;
    const clientId = process.env[`${connectorId.toUpperCase()}_CLIENT_ID`];
    const clientSecret = process.env[`${connectorId.toUpperCase()}_CLIENT_SECRET`];

    if (!clientId || !clientSecret) {
      throw new Error(`Missing credentials for ${connectorId}`);
    }

    // Exchange code for token
    const response = await fetch(authConfig.tokenUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
      },
      body: new URLSearchParams({
        client_id: clientId,
        client_secret: clientSecret,
        code,
        redirect_uri: redirectUri,
        grant_type: "authorization_code",
      }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Token exchange failed: ${error}`);
    }

    const data = await response.json();

    return {
      accessToken: data.access_token,
      refreshToken: data.refresh_token,
      expiresAt: data.expires_in ? new Date(Date.now() + data.expires_in * 1000) : undefined,
    };
  }

  /**
   * Refresh access token
   */
  static async refreshToken(
    connectorId: string,
    refreshToken: string
  ): Promise<ConnectionTokens> {
    const connector = getConnector(connectorId);
    if (!connector || connector.authType !== "oauth2") {
      throw new Error(`Connector ${connectorId} not found or does not support OAuth2`);
    }

    const authConfig = connector.authConfig as OAuth2Config;
    if (!authConfig.refreshToken) {
      throw new Error(`Connector ${connectorId} does not support token refresh`);
    }

    const clientId = process.env[`${connectorId.toUpperCase()}_CLIENT_ID`];
    const clientSecret = process.env[`${connectorId.toUpperCase()}_CLIENT_SECRET`];

    const response = await fetch(authConfig.tokenUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
      },
      body: new URLSearchParams({
        client_id: clientId!,
        client_secret: clientSecret!,
        refresh_token: refreshToken,
        grant_type: "refresh_token",
      }),
    });

    if (!response.ok) {
      throw new Error("Token refresh failed");
    }

    const data = await response.json();

    return {
      accessToken: data.access_token,
      refreshToken: data.refresh_token || refreshToken,
      expiresAt: data.expires_in ? new Date(Date.now() + data.expires_in * 1000) : undefined,
    };
  }

  private static generateState(
    connectorId: string,
    userId: string,
    redirectUri: string
  ): string {
    const state = crypto.randomUUID();
    this.states.set(state, {
      connectorId,
      userId,
      redirectUri,
      createdAt: Date.now(),
    });

    // Clean up old states after 10 minutes
    setTimeout(() => {
      this.states.delete(state);
    }, 10 * 60 * 1000);

    return state;
  }
}
