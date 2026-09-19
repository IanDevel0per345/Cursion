// POST /api/execute - Execute an action on a connected integration

import { getConnector } from "@/connectors";
import type { ConnectionTokens } from "@/connectors/base";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { connectionId, actionId, params } = body;

    if (!connectionId || !actionId) {
      return Response.json(
        { success: false, error: "Missing connectionId or actionId" },
        { status: 400 }
      );
    }

    // TODO: Get connection from database
    // const connection = await db.connections.findUnique({ where: { id: connectionId } });
    // if (!connection) {
    //   return Response.json({ success: false, error: "Connection not found" }, { status: 404 });
    // }

    // Mock connection for now
    const connection = {
      id: connectionId,
      connectorId: "github",
      tokens: {
        accessToken: process.env.GITHUB_TOKEN || "mock_token",
      } as ConnectionTokens,
    };

    const connector = getConnector(connection.connectorId);
    if (!connector) {
      return Response.json(
        { success: false, error: "Connector not found" },
        { status: 404 }
      );
    }

    const action = connector.actions.find(a => a.id === actionId);
    if (!action) {
      return Response.json(
        { success: false, error: "Action not found" },
        { status: 404 }
      );
    }

    // Execute action via HTTP request to external API
    let url = action.endpoint || "";

    // Replace path parameters
    if (url.includes("{")) {
      for (const [key, value] of Object.entries(params)) {
        url = url.replace(`{${key}}`, String(value));
      }
    }

    // Build full URL for GitHub
    if (connection.connectorId === "github") {
      url = `https://api.github.com${url}`;
    } else if (connection.connectorId === "slack") {
      url = `https://slack.com/api${url}`;
    }

    const response = await fetch(url, {
      method: action.method || "GET",
      headers: {
        "Authorization": `Bearer ${connection.tokens.accessToken}`,
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: action.method !== "GET" ? JSON.stringify(params) : undefined,
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || `HTTP ${response.status}`);
    }

    // TODO: Log execution to database
    // await db.executions.create({ ... })

    return Response.json({
      success: true,
      data,
      meta: {
        connector: connection.connectorId,
        action: actionId,
        timestamp: new Date().toISOString(),
      },
    });
  } catch (error) {
    console.error("Execute action error:", error);
    return Response.json(
      { success: false, error: error instanceof Error ? error.message : "Execution failed" },
      { status: 500 }
    );
  }
}
