// Real Backend Connector Executor - 3000+ Connectors
import type { ConnectorConfig, ActionDefinition, ConnectionTokens, ExecutionResult } from "./base";

export class ConnectorExecutor {
  private baseUrl: string;
  private tokens: ConnectionTokens;
  private connector: ConnectorConfig;

  constructor(connector: ConnectorConfig, tokens: ConnectionTokens) {
    this.connector = connector;
    this.tokens = tokens;
    this.baseUrl = this.getBaseUrl(connector);
  }

  private getBaseUrl(connector: ConnectorConfig): string {
    // Map connector IDs to real API base URLs
    const urlMap: Record<string, string> = {
      // Communication
      "slack": "https://slack.com/api",
      "discord": "https://discord.com/api/v10",
      "telegram": "https://api.telegram.org",
      "whatsapp": "https://graph.facebook.com/v17.0",
      "microsoft_teams": "https://graph.microsoft.com/v1.0",
      "zoom": "https://api.zoom.us/v2",
      "google_meet": "https://meet.googleapis.com/v3",

      // Email
      "gmail": "https://gmail.googleapis.com/gmail/v1",
      "outlook": "https://graph.microsoft.com/v1.0",
      "sendgrid": "https://api.sendgrid.com/v3",
      "mailchimp": "https://us1.api.mailchimp.com/3.0",

      // Productivity
      "notion": "https://api.notion.com/v1",
      "trello": "https://api.trello.com/1",
      "asana": "https://app.asana.com/api/1.0",
      "linear": "https://api.linear.app/graphql",
      "jira": "https://api.atlassian.com/experimental/jira",
      "monday": "https://api.monday.com/v2",
      "clickup": "https://api.clickup.com/api/v2",
      "airtable": "https://api.airtable.com/v0",

      // E-commerce
      "shopify": "https://{shop}.myshopify.com/admin/api/2024-01",
      "woocommerce": "https://{store}.com/wp-json/wc/v3",
      "stripe": "https://api.stripe.com/v1",
      "paypal": "https://api.paypal.com/v2",
      "square": "https://connect.squareup.com/v2",

      // Social Media
      "twitter": "https://api.twitter.com/2",
      "linkedin": "https://api.linkedin.com/v2",
      "instagram": "https://graph.facebook.com/v17.0",
      "facebook": "https://graph.facebook.com/v17.0",
      "tiktok": "https://open.tiktokapis.com/v2",

      // Developer Tools
      "github": "https://api.github.com",
      "gitlab": "https://gitlab.com/api/v4",
      "bitbucket": "https://api.bitbucket.org/2.0",
      "vercel": "https://api.vercel.com",
      "netlify": "https://api.netlify.com/api/v1",

      // Cloud
      "aws": "https://*.amazonaws.com",
      "gcp": "https://*.googleapis.com",
      "azure": "https://management.azure.com",
      "digitalocean": "https://api.digitalocean.com/v2",

      // Database
      "supabase": "https://{project}.supabase.co/rest/v1",
      "firebase": "https://firestore.googleapis.com/v1",
      "mongodb_atlas": "https://cloud.mongodb.com/api/atlas/v2",

      // Analytics
      "google_analytics": "https://analyticsdata.googleapis.com/v1beta",
      "mixpanel": "https://api.mixpanel.com",
      "amplitude": "https://amplitude.com/api",
    };

    return urlMap[connector.id] || `https://api.${connector.id}.com`;
  }

  async executeAction(actionId: string, params: Record<string, any>): Promise<ExecutionResult> {
    const action = this.connector.actions.find(a => a.id === actionId);
    if (!action) {
      return { success: false, error: `Action ${actionId} not found` };
    }

    try {
      let url = action.endpoint || "/";

      // Replace path parameters
      for (const [key, value] of Object.entries(params)) {
        url = url.replace(`{${key}}`, String(value));
      }

      const fullUrl = `${this.baseUrl}${url}`;

      const headers: Record<string, string> = {
        "Content-Type": "application/json",
        "Accept": "application/json",
      };

      // Add auth headers based on connector type
      if (this.connector.authType === "oauth2") {
        headers["Authorization"] = `Bearer ${this.tokens.accessToken}`;
      } else if (this.connector.authType === "api_key") {
        // API key connectors use different header names
        if (this.connector.id === "stripe") {
          headers["Authorization"] = `Bearer ${this.tokens.accessToken}`;
        } else if (this.connector.id === "airtable") {
          headers["Authorization"] = `Bearer ${this.tokens.accessToken}`;
        } else {
          headers["X-API-Key"] = this.tokens.accessToken;
        }
      }

      const response = await fetch(fullUrl, {
        method: action.method || "GET",
        headers,
        body: action.method !== "GET" ? JSON.stringify(params) : undefined,
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}`);
      }

      return { success: true, data };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : "Execution failed",
      };
    }
  }
}

// Factory function
export function createConnectorExecutor(
  connector: ConnectorConfig,
  tokens: ConnectionTokens
): ConnectorExecutor {
  return new ConnectorExecutor(connector, tokens);
}
