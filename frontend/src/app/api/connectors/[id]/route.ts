// GET /api/connectors/:id - Get connector details

import { getConnector } from "@/connectors";

export async function GET(
  request: Request,
  { params }: { params: { id: string } }
) {
  try {
    const { id } = params;
    const connector = getConnector(id);

    if (!connector) {
      return Response.json(
        { success: false, error: "Connector not found" },
        { status: 404 }
      );
    }

    return Response.json({
      success: true,
      data: connector,
    });
  } catch (error) {
    console.error("Error getting connector:", error);
    return Response.json(
      { success: false, error: "Failed to get connector" },
      { status: 500 }
    );
  }
}
