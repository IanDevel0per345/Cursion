// Notion Connector Implementation

import type { ConnectorConfig, ActionDefinition } from "./base";
import { ConnectorRegistry } from "./registry";

const notionActions: ActionDefinition[] = [
  {
    id: "create_page",
    name: "Create Page",
    description: "Create a new page in Notion",
    inputSchema: {
      parent: { type: "object", required: true, description: "Parent page/database ID" },
      title: { type: "string", required: true },
      content: { type: "array", required: false, description: "Page blocks" },
    },
    endpoint: "/pages",
    method: "POST",
  },
  {
    id: "list_pages",
    name: "List Pages",
    description: "List pages from a database",
    inputSchema: {
      database_id: { type: "string", required: true },
      filter: { type: "object", required: false },
    },
    endpoint: "/databases/{database_id}/query",
    method: "POST",
  },
  {
    id: "get_page",
    name: "Get Page",
    description: "Get page details",
    inputSchema: {
      page_id: { type: "string", required: true },
    },
    endpoint: "/pages/{page_id}",
    method: "GET",
  },
  {
    id: "update_page",
    name: "Update Page",
    description: "Update page properties",
    inputSchema: {
      page_id: { type: "string", required: true },
      properties: { type: "object", required: true },
    },
    endpoint: "/pages/{page_id}",
    method: "PATCH",
  },
];

export const notionConnector: ConnectorConfig = {
  id: "notion",
  name: "Notion",
  description: "Notion - Workspace and docs",
  icon: "notion",
  authType: "oauth2",
  authConfig: {
    authorizationUrl: "https://api.notion.com/v1/oauth/authorize",
    tokenUrl: "https://api.notion.com/v1/oauth/token",
    scopes: [],
    refreshToken: false,
  },
  actions: notionActions,
  triggers: [],
};

ConnectorRegistry.register(notionConnector);
export default notionConnector;
