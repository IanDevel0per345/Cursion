// Base Connector Interface for Cursion IPaaS

export interface ConnectorConfig {
  id: string;
  name: string;
  description: string;
  icon?: string;
  authType: "oauth2" | "api_key" | "basic";
  authConfig?: OAuth2Config | ApiKeyConfig;
  actions: ActionDefinition[];
  triggers?: TriggerDefinition[];
}

export interface OAuth2Config {
  authorizationUrl: string;
  tokenUrl: string;
  scopes: string[];
  refreshToken: boolean;
  clientId?: string;
  clientSecret?: string;
}

export interface ApiKeyConfig {
  headerName: string;
  description: string;
}

export interface ActionDefinition {
  id: string;
  name: string;
  description: string;
  inputSchema: Record<string, any>;
  endpoint?: string;
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
}

export interface TriggerDefinition {
  id: string;
  name: string;
  description: string;
  webhookPath?: string;
  eventTypes?: string[];
}

export interface ConnectionTokens {
  accessToken: string;
  refreshToken?: string;
  expiresAt?: Date;
}

export interface ExecutionResult {
  success: boolean;
  data?: any;
  error?: string;
}

export abstract class BaseConnector {
  abstract config: ConnectorConfig;

  abstract executeAction(
    actionId: string,
    params: Record<string, any>,
    tokens: ConnectionTokens
  ): Promise<ExecutionResult>;

  async validateConnection(tokens: ConnectionTokens): Promise<boolean> {
    try {
      const result = await this.executeAction("_test", {}, tokens);
      return result.success;
    } catch {
      return false;
    }
  }
}

export function defineConnector(config: ConnectorConfig): ConnectorConfig {
  return config;
}
