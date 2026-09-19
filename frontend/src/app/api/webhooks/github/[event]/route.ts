// POST /api/webhooks/github/:event - Receive GitHub webhooks
export async function POST(request: Request, { params }: { params: { event: string } }) {
  const body = await request.json();
  const eventType = request.headers.get("x-github-event");
  console.log("GitHub webhook:", eventType, body.action);
  // TODO: Process and trigger webhooks
  return Response.json({ success: true });
}
