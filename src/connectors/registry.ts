// Connector Registry - Central catalog of all connectors

import type { ConnectorConfig } from "./base";

export class ConnectorRegistry {
  private static connectors: Map<string, ConnectorConfig> = new Map();

  static register(config: ConnectorConfig): void {
    this.connectors.set(config.id, config);
  }

  static get(id: string): ConnectorConfig | undefined {
    return this.connectors.get(id);
  }

  static getAll(): ConnectorConfig[] {
    return Array.from(this.connectors.values());
  }

  static getAvailable(): ConnectorConfig[] {
    // Return connectors that are configured (have credentials)
    return this.getAll().filter(c => {
      if (c.authType === "oauth2") {
        return !!(process.env[`${c.id.toUpperCase()}_CLIENT_ID`]);
      }
      return true; // api_key connectors are always available
    });
  }
}

// Export for use in API routes
export const getConnectors = () => ConnectorRegistry.getAvailable();
export const getConnector = (id: string) => ConnectorRegistry.get(id);
