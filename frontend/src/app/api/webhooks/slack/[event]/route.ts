// POST /api/webhooks/slack/:event - Receive Slack webhooks
export async function POST(request: Request, { params }: { params: { event: string } }) {
  const body = await request.text();
  const data = JSON.parse(body);
  if (data.type === "url_verification") {
    return Response.json({ challenge: data.challenge });
  }
  console.log("Slack webhook:", data.type, data.event?.type);
  return Response.json({ success: true });
}
