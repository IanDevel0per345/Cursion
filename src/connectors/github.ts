// GitHub Connector Implementation

import type { ConnectorConfig, ActionDefinition } from "./base";
import { ConnectorRegistry } from "./registry";

const githubActions: ActionDefinition[] = [
  {
    id: "create_issue",
    name: "Create Issue",
    description: "Create a new issue in a GitHub repository",
    inputSchema: {
      owner: { type: "string", required: true, description: "Repository owner" },
      repo: { type: "string", required: true, description: "Repository name" },
      title: { type: "string", required: true, description: "Issue title" },
      body: { type: "string", required: false, description: "Issue description" },
      labels: { type: "array", required: false, description: "Labels to add" },
    },
    endpoint: "/repos/{owner}/{repo}/issues",
    method: "POST",
  },
  {
    id: "list_issues",
    name: "List Issues",
    description: "List issues in a GitHub repository",
    inputSchema: {
      owner: { type: "string", required: true },
      repo: { type: "string", required: true },
      state: { type: "string", required: false, enum: ["open", "closed", "all"] },
      per_page: { type: "number", required: false, default: 30 },
    },
    endpoint: "/repos/{owner}/{repo}/issues",
    method: "GET",
  },
  {
    id: "create_pull_request",
    name: "Create Pull Request",
    description: "Create a new pull request",
    inputSchema: {
      owner: { type: "string", required: true },
      repo: { type: "string", required: true },
      title: { type: "string", required: true },
      body: { type: "string", required: false },
      head: { type: "string", required: true, description: "Source branch" },
      base: { type: "string", required: true, description: "Target branch" },
    },
    endpoint: "/repos/{owner}/{repo}/pulls",
    method: "POST",
  },
  {
    id: "get_user",
    name: "Get User",
    description: "Get authenticated user info",
    inputSchema: {},
    endpoint: "/user",
    method: "GET",
  },
];

export const githubConnector: ConnectorConfig = {
  id: "github",
  name: "GitHub",
  description: "GitHub - Code hosting and collaboration",
  icon: "github",
  authType: "oauth2",
  authConfig: {
    authorizationUrl: "https://github.com/login/oauth/authorize",
    tokenUrl: "https://github.com/login/oauth/access_token",
    scopes: ["repo", "user", "read:org"],
    refreshToken: false,
  },
  actions: githubActions,
  triggers: [
    {
      id: "new_issue",
      name: "New Issue",
      description: "Triggered when a new issue is created",
      webhookPath: "/webhooks/github/issues",
      eventTypes: ["issues.opened"],
    },
    {
      id: "new_pr",
      name: "New Pull Request",
      description: "Triggered when a new PR is created",
      webhookPath: "/webhooks/github/pulls",
      eventTypes: ["pull_request.opened"],
    },
  ],
};

// Register connector
ConnectorRegistry.register(githubConnector);

export default githubConnector;
