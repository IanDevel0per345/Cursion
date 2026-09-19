// Cursion Connector SDK

export * from "./base";
export * from "./registry";

// Import all connectors to register them
import "./github";
import "./slack";
import "./gmail";
import "./notion";
import "./google_sheets";

// Import generated catalog (1500+ connectors)
import { registerAllConnectors } from "./generated/catalog";
registerAllConnectors();

// Re-export specific connectors
export { githubConnector } from "./github";
export { slackConnector } from "./slack";
export { gmailConnector } from "./gmail";
export { notionConnector } from "./notion";
export { googleSheetsConnector } from "./google_sheets";

// Export catalog for programmatic access
export { CONNECTOR_CATALOG } from "./generated/catalog";
