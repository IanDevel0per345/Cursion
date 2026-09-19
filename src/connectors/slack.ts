// Slack Connector Implementation

import type { ConnectorConfig, ActionDefinition } from "./base";
import { ConnectorRegistry } from "./registry";

const slackActions: ActionDefinition[] = [
  {
    id: "send_message",
    name: "Send Message",
    description: "Send a message to a Slack channel",
    inputSchema: {
      channel: { type: "string", required: true, description: "Channel ID or name" },
      text: { type: "string", required: true, description: "Message text" },
      blocks: { type: "array", required: false, description: "Block kit blocks" },
    },
    endpoint: "/chat.postMessage",
    method: "POST",
  },
  {
    id: "list_channels",
    name: "List Channels",
    description: "List public channels in workspace",
    inputSchema: {
      limit: { type: "number", required: false, default: 100 },
      cursor: { type: "string", required: false },
    },
    endpoint: "/conversations.list",
    method: "GET",
  },
  {
    id: "get_user",
    name: "Get User Info",
    description: "Get Slack user information",
    inputSchema: {
      user: { type: "string", required: true, description: "User ID" },
    },
    endpoint: "/users.info",
    method: "GET",
  },
];

export const slackConnector: ConnectorConfig = {
  id: "slack",
  name: "Slack",
  description: "Slack - Team communication and collaboration",
  icon: "slack",
  authType: "oauth2",
  authConfig: {
    authorizationUrl: "https://slack.com/oauth/v2/authorize",
    tokenUrl: "https://slack.com/api/oauth.v2.access",
    scopes: [
      "channels:read",
      "chat:write",
      "users:read",
      "incoming-webhook",
    ],
    refreshToken: false,
  },
  actions: slackActions,
  triggers: [
    {
      id: "message_received",
      name: "Message Received",
      description: "Triggered when a message is received",
      webhookPath: "/webhooks/slack/events",
      eventTypes: ["message", "app_mention"],
    },
  ],
};

ConnectorRegistry.register(slackConnector);

export default slackConnector;
