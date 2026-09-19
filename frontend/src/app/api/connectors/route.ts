// GET /api/connectors - List all available connectors

import { getConnectors } from "@/connectors";

export async function GET() {
  try {
    const connectors = getConnectors();

    return Response.json({
      success: true,
      data: connectors.map(c => ({
        id: c.id,
        name: c.name,
        description: c.description,
        icon: c.icon,
        authType: c.authType,
        actions: c.actions.map(a => ({
          id: a.id,
          name: a.name,
          description: a.description,
        })),
        triggers: c.triggers?.map(t => ({
          id: t.id,
          name: t.name,
          description: t.description,
        })) || [],
      })),
    });
  } catch (error) {
    console.error("Error listing connectors:", error);
    return Response.json(
      { success: false, error: "Failed to list connectors" },
      { status: 500 }
    );
  }
}
