// Gmail Connector Implementation

import type { ConnectorConfig, ActionDefinition } from "./base";
import { ConnectorRegistry } from "./registry";

const gmailActions: ActionDefinition[] = [
  {
    id: "send_email",
    name: "Send Email",
    description: "Send an email via Gmail",
    inputSchema: {
      to: { type: "string", required: true, description: "Recipient email" },
      subject: { type: "string", required: true },
      body: { type: "string", required: true },
      cc: { type: "string", required: false },
      bcc: { type: "string", required: false },
    },
    endpoint: "/gmail/v1/users/me/messages/send",
    method: "POST",
  },
  {
    id: "list_emails",
    name: "List Emails",
    description: "List emails in inbox",
    inputSchema: {
      q: { type: "string", required: false, description: "Search query" },
      maxResults: { type: "number", required: false, default: 10 },
    },
    endpoint: "/gmail/v1/users/me/messages",
    method: "GET",
  },
  {
    id: "get_email",
    name: "Get Email",
    description: "Get email details by ID",
    inputSchema: {
      id: { type: "string", required: true, description: "Message ID" },
    },
    endpoint: "/gmail/v1/users/me/messages/{id}",
    method: "GET",
  },
];

export const gmailConnector: ConnectorConfig = {
  id: "gmail",
  name: "Gmail",
  description: "Google Gmail - Email service",
  icon: "gmail",
  authType: "oauth2",
  authConfig: {
    authorizationUrl: "https://accounts.google.com/o/oauth2/auth",
    tokenUrl: "https://oauth2.googleapis.com/token",
    scopes: [
      "https://www.googleapis.com/auth/gmail.send",
      "https://www.googleapis.com/auth/gmail.readonly",
    ],
    refreshToken: true,
  },
  actions: gmailActions,
  triggers: [
    {
      id: "new_email",
      name: "New Email Received",
      description: "Triggered when a new email arrives",
      webhookPath: "/webhooks/gmail/new",
      eventTypes: ["message"],
    },
  ],
};

ConnectorRegistry.register(gmailConnector);
export default gmailConnector;
