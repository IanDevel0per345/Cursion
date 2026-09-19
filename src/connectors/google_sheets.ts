// Google Sheets Connector Implementation

import type { ConnectorConfig, ActionDefinition } from "./base";
import { ConnectorRegistry } from "./registry";

const gsheetsActions: ActionDefinition[] = [
  {
    id: "get_values",
    name: "Get Values",
    description: "Get values from a spreadsheet range",
    inputSchema: {
      spreadsheetId: { type: "string", required: true },
      range: { type: "string", required: true, description: "e.g., Sheet1!A1:B2" },
    },
    endpoint: "/sheets/v4/spreadsheets/{spreadsheetId}/values/{range}",
    method: "GET",
  },
  {
    id: "update_values",
    name: "Update Values",
    description: "Update values in a spreadsheet range",
    inputSchema: {
      spreadsheetId: { type: "string", required: true },
      range: { type: "string", required: true },
      values: { type: "array", required: true, description: "2D array of values" },
    },
    endpoint: "/sheets/v4/spreadsheets/{spreadsheetId}/values/{range}",
    method: "PUT",
  },
  {
    id: "append_values",
    name: "Append Values",
    description: "Append values to a spreadsheet",
    inputSchema: {
      spreadsheetId: { type: "string", required: true },
      range: { type: "string", required: true },
      values: { type: "array", required: true },
    },
    endpoint: "/sheets/v4/spreadsheets/{spreadsheetId}/values/{range}:append",
    method: "POST",
  },
  {
    id: "list_spreadsheets",
    name: "List Spreadsheets",
    description: "List user's spreadsheets",
    inputSchema: {
      pageSize: { type: "number", required: false, default: 10 },
    },
    endpoint: "/drive/v3/files?mimeType=application/vnd.google-apps.spreadsheet",
    method: "GET",
  },
];

export const googleSheetsConnector: ConnectorConfig = {
  id: "google_sheets",
  name: "Google Sheets",
  description: "Google Sheets - Spreadsheets",
  icon: "sheets",
  authType: "oauth2",
  authConfig: {
    authorizationUrl: "https://accounts.google.com/o/oauth2/auth",
    tokenUrl: "https://oauth2.googleapis.com/token",
    scopes: [
      "https://www.googleapis.com/auth/spreadsheets",
      "https://www.googleapis.com/auth/drive.readonly",
    ],
    refreshToken: true,
  },
  actions: gsheetsActions,
  triggers: [],
};

ConnectorRegistry.register(googleSheetsConnector);
export default googleSheetsConnector;
