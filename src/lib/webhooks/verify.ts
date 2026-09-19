// Webhook Signature Verification

import { createHmac } from "crypto";

export async function verifyWebhookSignature(
  connector: string,
  body: string,
  signature: string
): Promise<boolean> {
  const secret = process.env[`${connector.toUpperCase()}_WEBHOOK_SECRET`];

  if (!secret) {
    return true; // Skip verification if no secret configured
  }

  switch (connector.toLowerCase()) {
    case "github":
      return verifyGitHubSignature(body, signature, secret);
    case "slack":
      return verifySlackSignature(body, signature, secret);
    default:
      return true;
  }
}

function verifyGitHubSignature(
  body: string,
  signature: string,
  secret: string
): boolean {
  if (!signature.startsWith("sha256=")) {
    return false;
  }

  const expected = createHmac("sha256", secret)
    .update(body, "utf8")
    .digest("hex");

  const provided = signature.slice(7);

  return crypto.timingSafeEqual(
    Buffer.from(expected, "hex"),
    Buffer.from(provided, "hex")
  );
}

async function verifySlackSignature(
  body: string,
  signature: string,
  secret: string
): Promise<boolean> {
  const timestamp = Date.now() / 1000;
  const baseString = `v0:${timestamp}:${body}`;

  const expected = createHmac("sha256", secret)
    .update(baseString)
    .digest("hex");

  const provided = signature.slice(3);

  return crypto.timingSafeEqual(
    Buffer.from(expected, "hex"),
    Buffer.from(provided, "hex")
  );
}

export function parseWebhookPayload(connector: string, body: any): any {
  // Normalize webhook payloads across providers
  switch (connector) {
    case "github":
      return {
        event: body.action,
        resource: body.issue?.type || body.pull_request ? "pull_request" : "issue",
        id: body.issue?.id || body.pull_request?.id,
        url: body.issue?.html_url || body.pull_request?.html_url,
      };
    case "slack":
      return {
        event: body.event?.type,
        channel: body.event?.channel,
        user: body.event?.user,
        text: body.event?.text,
      };
    default:
      return body;
  }
}
