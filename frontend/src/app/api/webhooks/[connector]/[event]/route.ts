// POST /api/webhooks/:connector/:event - Receive external webhooks

import { prisma } from "@/lib/db";

export async function POST(
  request: Request,
  { params }: { params: { connector: string; event: string } }
) {
  try {
    const { connector, event } = params;
    const body = await request.text();
    const signature = request.headers.get("x-hub-signature-256") || "";

    // Parse payload
    const payload = JSON.parse(body);

    // Find matching webhook subscriptions
    const subscriptions = await prisma.webhookSubscription.findMany({
      where: {
        connectorId: connector,
        event: event,
        active: true,
      },
    });

    if (subscriptions.length === 0) {
      return Response.json({ success: true, message: "No subscribers" });
    }

    // Forward to all subscribers
    const deliveries = await Promise.all(
      subscriptions.map(async (sub) => {
        try {
          const response = await fetch(sub.callbackUrl, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "X-Cursion-Connector": connector,
              "X-Cursion-Event": event,
            },
            body,
          });

          return {
            subscriptionId: sub.id,
            success: response.ok,
            status: response.status,
          };
        } catch (error) {
          return {
            subscriptionId: sub.id,
            success: false,
            error: error instanceof Error ? error.message : "Delivery failed",
          };
        }
      })
    );

    const successful = deliveries.filter(d => d.success).length;
    const failed = deliveries.length - successful;

    return Response.json({
      success: true,
      data: {
        connector,
        event,
        subscribers: subscriptions.length,
        delivered: successful,
        failed,
      },
    });
  } catch (error) {
    console.error("Webhook receiver error:", error);
    return Response.json(
      { success: false, error: error instanceof Error ? error.message : "Webhook processing failed" },
      { status: 500 }
    );
  }
}
