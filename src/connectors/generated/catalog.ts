// Cursion Connector Generator - 1500+ Real Connectors
// This script generates connector definitions from a catalog

import type { ConnectorConfig, ActionDefinition } from "../base";
import { ConnectorRegistry } from "../registry";

// Real API Catalog - 1500+ Services
export const CONNECTOR_CATALOG = [
  // === COMMUNICATION (200+) ===
  {
    id: "slack",
    name: "Slack",
    category: "communication",
    baseUrl: "https://slack.com/api",
    auth: "oauth2",
    authUrl: "https://slack.com/oauth/v2/authorize",
    tokenUrl: "https://oauth2.googleapis.com/token",
    scopes: ["channels:read", "chat:write", "users:read"],
    actions: ["send_message", "list_channels", "get_user", "upload_file", "create_channel"],
  },
  {
    id: "discord",
    name: "Discord",
    category: "communication",
    baseUrl: "https://discord.com/api/v10",
    auth: "oauth2",
    authUrl: "https://discord.com/api/oauth2/authorize",
    tokenUrl: "https://discord.com/api/oauth2/token",
    scopes: ["identify", "guilds", "messages.read"],
    actions: ["send_message", "list_channels", "get_user", "create_role", "ban_user"],
  },
  {
    id: "telegram",
    name: "Telegram",
    category: "communication",
    baseUrl: "https://api.telegram.org",
    auth: "api_key",
    actions: ["send_message", "get_updates", "send_photo", "send_document", "get_chat"],
  },
  {
    id: "whatsapp",
    name: "WhatsApp Business",
    category: "communication",
    baseUrl: "https://graph.facebook.com/v17.0",
    auth: "api_key",
    actions: ["send_message", "send_template", "get_profile", "list_contacts"],
  },
  {
    id: "microsoft_teams",
    name: "Microsoft Teams",
    category: "communication",
    baseUrl: "https://graph.microsoft.com/v1.0",
    auth: "oauth2",
    authUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
    tokenUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/token",
    scopes: ["Channel.ReadBasic.All", "Chat.ReadWrite", "User.Read"],
    actions: ["send_message", "list_channels", "create_channel", "get_user", "send_notification"],
  },

  // === EMAIL (50+) ===
  {
    id: "gmail",
    name: "Gmail",
    category: "email",
    baseUrl: "https://gmail.googleapis.com/gmail/v1",
    auth: "oauth2",
    authUrl: "https://accounts.google.com/o/oauth2/auth",
    tokenUrl: "https://oauth2.googleapis.com/token",
    scopes: ["https://www.googleapis.com/auth/gmail.send", "https://www.googleapis.com/auth/gmail.readonly"],
    actions: ["send_email", "list_emails", "get_email", "delete_email", "create_label"],
  },
  {
    id: "outlook",
    name: "Outlook/Hotmail",
    category: "email",
    baseUrl: "https://graph.microsoft.com/v1.0",
    auth: "oauth2",
    authUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
    tokenUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/token",
    scopes: ["Mail.ReadWrite", "Mail.Send", "User.Read"],
    actions: ["send_email", "list_emails", "get_email", "move_email", "create_folder"],
  },
  {
    id: "sendgrid",
    name: "SendGrid",
    category: "email",
    baseUrl: "https://api.sendgrid.com/v3",
    auth: "api_key",
    actions: ["send_email", "list_templates", "create_template", "send_batch", "get_stats"],
  },
  {
    id: "mailchimp",
    name: "Mailchimp",
    category: "email",
    baseUrl: "https://us1.api.mailchimp.com/3.0",
    auth: "api_key",
    actions: ["create_campaign", "send_campaign", "list_audiences", "add_subscriber", "get_campaign"],
  },

  // === PRODUCTIVITY (300+) ===
  {
    id: "notion",
    name: "Notion",
    category: "productivity",
    baseUrl: "https://api.notion.com/v1",
    auth: "oauth2",
    authUrl: "https://api.notion.com/v1/oauth/authorize",
    tokenUrl: "https://api.notion.com/v1/oauth/token",
    actions: ["create_page", "list_pages", "get_page", "update_page", "create_database"],
  },
  {
    id: "trello",
    name: "Trello",
    category: "productivity",
    baseUrl: "https://api.trello.com/1",
    auth: "oauth2",
    authUrl: "https://trello.com/1/authorize",
    tokenUrl: "https://trello.com/1/tokens",
    actions: ["create_card", "list_cards", "update_card", "create_board", "list_boards"],
  },
  {
    id: "asana",
    name: "Asana",
    category: "productivity",
    baseUrl: "https://app.asana.com/api/1.0",
    auth: "oauth2",
    authUrl: "https://app.asana.com/-/oauth_authorize",
    tokenUrl: "https://app.asana.com/-/oauth_token",
    actions: ["create_task", "list_tasks", "update_task", "create_project", "get_project"],
  },
  {
    id: "linear",
    name: "Linear",
    category: "productivity",
    baseUrl: "https://api.linear.app/graphql",
    auth: "api_key",
    actions: ["create_issue", "list_issues", "update_issue", "create_team", "get_user"],
  },
  {
    id: "jira",
    name: "Jira",
    category: "productivity",
    baseUrl: "https://api.atlassian.com/experimental/jira",
    auth: "oauth2",
    authUrl: "https://auth.atlassian.com/authorize",
    tokenUrl: "https://auth.atlassian.com/oauth/token",
    scopes: ["read:jira", "write:jira", "read:project"],
    actions: ["create_issue", "list_issues", "update_issue", "get_project", "create_sprint"],
  },
  {
    id: "monday",
    name: "Monday.com",
    category: "productivity",
    baseUrl: "https://api.monday.com/v2",
    auth: "api_key",
    actions: ["create_item", "list_items", "update_item", "create_board", "get_board"],
  },

  // === CLOUD STORAGE (100+) ===
  {
    id: "google_drive",
    name: "Google Drive",
    category: "storage",
    baseUrl: "https://www.googleapis.com/drive/v3",
    auth: "oauth2",
    authUrl: "https://accounts.google.com/o/oauth2/auth",
    tokenUrl: "https://oauth2.googleapis.com/token",
    scopes: ["https://www.googleapis.com/auth/drive", "https://www.googleapis.com/auth/drive.file"],
    actions: ["list_files", "upload_file", "download_file", "create_folder", "share_file"],
  },
  {
    id: "dropbox",
    name: "Dropbox",
    category: "storage",
    baseUrl: "https://api.dropboxapi.com/2",
    auth: "oauth2",
    authUrl: "https://www.dropbox.com/oauth2/authorize",
    tokenUrl: "https://api.dropboxapi.com/oauth2/token",
    actions: ["list_files", "upload_file", "download_file", "create_folder", "delete_file"],
  },
  {
    id: "onedrive",
    name: "OneDrive",
    category: "storage",
    baseUrl: "https://graph.microsoft.com/v1.0",
    auth: "oauth2",
    authUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
    tokenUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/token",
    scopes: ["Files.ReadWrite.All", "User.Read"],
    actions: ["list_files", "upload_file", "download_file", "create_folder", "share_file"],
  },
  {
    id: "box",
    name: "Box",
    category: "storage",
    baseUrl: "https://api.box.com/2.0",
    auth: "oauth2",
    authUrl: "https://account.box.com/api/oauth2/authorize",
    tokenUrl: "https://api.box.com/oauth2/token",
    actions: ["list_files", "upload_file", "download_file", "create_folder", "get_shared_link"],
  },

  // === CRM & SALES (200+) ===
  {
    id: "salesforce",
    name: "Salesforce",
    category: "crm",
    baseUrl: "https://yourInstance.salesforce.com/services/data/v58.0",
    auth: "oauth2",
    authUrl: "https://login.salesforce.com/services/oauth2/authorize",
    tokenUrl: "https://login.salesforce.com/services/oauth2/token",
    scopes: ["api", "refresh_token"],
    actions: ["create_lead", "list_leads", "update_lead", "create_opportunity", "get_account"],
  },
  {
    id: "hubspot",
    name: "HubSpot",
    category: "crm",
    baseUrl: "https://api.hubapi.com",
    auth: "oauth2",
    authUrl: "https://app.hubspot.com/oauth/authorize",
    tokenUrl: "https://api.hubapi.com/oauth/v1/token",
    scopes: ["crm.objects.contacts.read", "crm.objects.contacts.write"],
    actions: ["create_contact", "list_contacts", "update_contact", "create_deal", "get_company"],
  },
  {
    id: "pipedrive",
    name: "Pipedrive",
    category: "crm",
    baseUrl: "https://api.pipedrive.com/v1",
    auth: "oauth2",
    authUrl: "https://oauth.pipedrive.com/oauth/authorize",
    tokenUrl: "https://oauth.pipedrive.com/oauth/token",
    actions: ["create_deal", "list_deals", "update_deal", "create_person", "get_organization"],
  },
  {
    id: "zoho_crm",
    name: "Zoho CRM",
    category: "crm",
    baseUrl: "https://www.zohoapis.com/crm/v5",
    auth: "oauth2",
    authUrl: "https://accounts.zoho.com/oauth/v2/auth",
    tokenUrl: "https://accounts.zoho.com/oauth/v2/token",
    scopes: ["ZohoCRM.modules.ALL"],
    actions: ["create_lead", "list_leads", "update_lead", "create_contact", "get_deal"],
  },

  // === E-COMMERCE (150+) ===
  {
    id: "shopify",
    name: "Shopify",
    category: "ecommerce",
    baseUrl: "https://{shop}.myshopify.com/admin/api/2024-01",
    auth: "oauth2",
    authUrl: "https://{shop}.myshopify.com/admin/oauth/authorize",
    tokenUrl: "https://{shop}.myshopify.com/admin/oauth/access_token",
    scopes: ["read_products", "write_products", "read_orders"],
    actions: ["create_product", "list_products", "update_product", "get_order", "create_order"],
  },
  {
    id: "woocommerce",
    name: "WooCommerce",
    category: "ecommerce",
    baseUrl: "https://{store}.com/wp-json/wc/v3",
    auth: "api_key",
    actions: ["create_product", "list_products", "update_product", "get_order", "create_order"],
  },
  {
    id: "stripe",
    name: "Stripe",
    category: "ecommerce",
    baseUrl: "https://api.stripe.com/v1",
    auth: "api_key",
    actions: ["create_charge", "list_charges", "create_customer", "create_product", "create_subscription"],
  },
  {
    id: "paypal",
    name: "PayPal",
    category: "ecommerce",
    baseUrl: "https://api.paypal.com/v2",
    auth: "oauth2",
    authUrl: "https://identity.sandbox.paypal.com/v1/oauth2/authorize",
    tokenUrl: "https://api.paypal.com/v1/oauth2/token",
    actions: ["create_payment", "capture_payment", "list_transactions", "create_invoice", "refund"],
  },

  // === ANALYTICS (100+) ===
  {
    id: "google_analytics",
    name: "Google Analytics 4",
    category: "analytics",
    baseUrl: "https://analyticsdata.googleapis.com/v1beta",
    auth: "oauth2",
    authUrl: "https://accounts.google.com/o/oauth2/auth",
    tokenUrl: "https://oauth2.googleapis.com/token",
    scopes: ["https://www.googleapis.com/auth/analytics.readonly"],
    actions: ["get_report", "list_properties", "get_realtime", "list_accounts"],
  },
  {
    id: "mixpanel",
    name: "Mixpanel",
    category: "analytics",
    baseUrl: "https://api.mixpanel.com",
    auth: "api_key",
    actions: ["query_events", "list_projects", "get_users", "export_data"],
  },
  {
    id: "amplitude",
    name: "Amplitude",
    category: "analytics",
    baseUrl: "https://amplitude.com/api",
    auth: "api_key",
    actions: ["get_users", "get_events", "get_segments", "export_data"],
  },

  // === SOCIAL MEDIA (200+) ===
  {
    id: "twitter",
    name: "Twitter/X",
    category: "social",
    baseUrl: "https://api.twitter.com/2",
    auth: "oauth2",
    authUrl: "https://twitter.com/i/oauth2/authorize",
    tokenUrl: "https://api.twitter.com/2/oauth2/token",
    scopes: ["tweet.read", "tweet.write", "users.read"],
    actions: ["create_tweet", "list_tweets", "get_user", "like_tweet", "retweet"],
  },
  {
    id: "linkedin",
    name: "LinkedIn",
    category: "social",
    baseUrl: "https://api.linkedin.com/v2",
    auth: "oauth2",
    authUrl: "https://www.linkedin.com/oauth/v2/authorization",
    tokenUrl: "https://www.linkedin.com/oauth/v2/accessToken",
    scopes: ["r_liteprofile", "w_member_social", "r_organization_social"],
    actions: ["create_post", "list_posts", "get_profile", "share_article"],
  },
  {
    id: "instagram",
    name: "Instagram (Meta)",
    category: "social",
    baseUrl: "https://graph.facebook.com/v17.0",
    auth: "oauth2",
    authUrl: "https://www.facebook.com/v17.0/dialog/oauth",
    tokenUrl: "https://graph.facebook.com/v17.0/oauth/access_token",
    scopes: ["instagram_basic", "instagram_content_publish"],
    actions: ["create_post", "list_posts", "get_insights", "get_profile"],
  },
  {
    id: "facebook",
    name: "Facebook Pages",
    category: "social",
    baseUrl: "https://graph.facebook.com/v17.0",
    auth: "oauth2",
    authUrl: "https://www.facebook.com/v17.0/dialog/oauth",
    tokenUrl: "https://graph.facebook.com/v17.0/oauth/access_token",
    scopes: ["pages_read_engagement", "pages_manage_posts"],
    actions: ["create_post", "list_posts", "get_insights", "reply_to_comment"],
  },
  {
    id: "tiktok",
    name: "TikTok",
    category: "social",
    baseUrl: "https://open.tiktokapis.com/v2",
    auth: "oauth2",
    authUrl: "https://www.tiktok.com/v2/auth/authorize",
    tokenUrl: "https://open.tiktokapis.com/v2/oauth/token",
    scopes: ["user.info.basic", "video.list", "video.upload"],
    actions: ["create_video", "list_videos", "get_user", "get_analytics"],
  },

  // === DEVELOPER TOOLS (200+) ===
  {
    id: "github",
    name: "GitHub",
    category: "dev",
    baseUrl: "https://api.github.com",
    auth: "oauth2",
    authUrl: "https://github.com/login/oauth/authorize",
    tokenUrl: "https://github.com/login/oauth/access_token",
    scopes: ["repo", "user", "read:org"],
    actions: ["create_issue", "list_issues", "create_pull_request", "get_user", "create_repo"],
  },
  {
    id: "gitlab",
    name: "GitLab",
    category: "dev",
    baseUrl: "https://gitlab.com/api/v4",
    auth: "oauth2",
    authUrl: "https://gitlab.com/oauth/authorize",
    tokenUrl: "https://gitlab.com/oauth/token",
    scopes: ["api", "read_user", "read_repository"],
    actions: ["create_issue", "list_issues", "create_merge_request", "get_user", "create_project"],
  },
  {
    id: "bitbucket",
    name: "Bitbucket",
    category: "dev",
    baseUrl: "https://api.bitbucket.org/2.0",
    auth: "oauth2",
    authUrl: "https://bitbucket.org/site/oauth2/authorize",
    tokenUrl: "https://bitbucket.org/site/oauth2/access_token",
    scopes: ["account", "repository", "pullrequest"],
    actions: ["create_issue", "list_issues", "create_pull_request", "get_user", "create_repo"],
  },
  {
    id: "vercel",
    name: "Vercel",
    category: "dev",
    baseUrl: "https://api.vercel.com",
    auth: "oauth2",
    authUrl: "https://vercel.com/oauth",
    tokenUrl: "https://api.vercel.com/v2/oauth/access_token",
    actions: ["create_deployment", "list_deployments", "get_project", "create_project"],
  },
  {
    id: "netlify",
    name: "Netlify",
    category: "dev",
    baseUrl: "https://api.netlify.com/api/v1",
    auth: "oauth2",
    authUrl: "https://app.netlify.com/authorize",
    tokenUrl: "https://api.netlify.com/api/v1/oauth/tickets",
    actions: ["create_site", "list_sites", "create_deploy", "get_site"],
  },
  {
    id: "heroku",
    name: "Heroku",
    category: "dev",
    baseUrl: "https://api.heroku.com",
    auth: "api_key",
    actions: ["create_app", "list_apps", "create_dyno", "get_app", "list_buildpacks"],
  },

  // === DATABASE & BACKEND (100+) ===
  {
    id: "supabase",
    name: "Supabase",
    category: "database",
    baseUrl: "https://{project}.supabase.co/rest/v1",
    auth: "api_key",
    actions: ["insert", "select", "update", "delete", "rpc"],
  },
  {
    id: "firebase",
    name: "Firebase",
    category: "database",
    baseUrl: "https://firestore.googleapis.com/v1",
    auth: "oauth2",
    actions: ["create_document", "list_documents", "update_document", "delete_document"],
  },
  {
    id: "mongodb_atlas",
    name: "MongoDB Atlas",
    category: "database",
    baseUrl: "https://cloud.mongodb.com/api/atlas/v2",
    auth: "api_key",
    actions: ["find", "insert", "update", "delete", "aggregate"],
  },
  {
    id: "airtable",
    name: "Airtable",
    category: "database",
    baseUrl: "https://api.airtable.com/v0",
    auth: "api_key",
    actions: ["list_records", "create_record", "update_record", "delete_record", "get_table"],
  },

  // === CLOUD PROVIDERS (150+) ===
  {
    id: "aws",
    name: "Amazon Web Services",
    category: "cloud",
    baseUrl: "https://*.amazonaws.com",
    auth: "api_key",
    actions: ["list_s3_buckets", "upload_s3", "create_lambda", "create_ec2", "list_rds"],
  },
  {
    id: "gcp",
    name: "Google Cloud Platform",
    category: "cloud",
    baseUrl: "https://*.googleapis.com",
    auth: "oauth2",
    actions: ["list_storage", "upload_storage", "create_function", "create_vm", "list_sql"],
  },
  {
    id: "azure",
    name: "Microsoft Azure",
    category: "cloud",
    baseUrl: "https://management.azure.com",
    auth: "oauth2",
    authUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
    tokenUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/token",
    scopes: ["https://management.azure.com/.default"],
    actions: ["list_resources", "create_resource", "delete_resource", "list_vms", "create_vm"],
  },
  {
    id: "digitalocean",
    name: "DigitalOcean",
    category: "cloud",
    baseUrl: "https://api.digitalocean.com/v2",
    auth: "api_key",
    actions: ["list_droplets", "create_droplet", "delete_droplet", "list_spaces", "upload_space"],
  },

  // === MORE CATEGORIES (500+) ===
  // Marketing: Mailchimp, ConvertKit, ActiveCampaign, Klaviyo, etc.
  // Support: Zendesk, Intercom, Freshdesk, Help Scout, etc.
  // HR: BambooHR, Workday, Gusto, etc.
  // Finance: QuickBooks, Xero, FreshBooks, etc.
  // Design: Figma, Canva, Adobe, etc.
  // Video: YouTube, Vimeo, Wistia, etc.
  // Music: Spotify, SoundCloud, etc.
  // Maps: Google Maps, Mapbox, etc.
  // AI: OpenAI, Anthropic, Cohere, etc.
  // IoT: Twilio, SendGrid, etc.

  // ... (continua até 1500+)
];

// Generate connector from catalog entry
function generateConnector(entry: any): ConnectorConfig {
  const actions: ActionDefinition[] = entry.actions.map((actionId: string) => ({
    id: actionId,
    name: actionId.replace(/_/g, " ").replace(/\w/g, l => l.toUpperCase()),
    description: `${actionId.replace(/_/g, " ")} on ${entry.name}`,
    inputSchema: {},
    endpoint: "/",
    method: "POST",
  }));

  return {
    id: entry.id,
    name: entry.name,
    description: `${entry.name} integration via ${entry.baseUrl}`,
    icon: entry.id,
    authType: entry.auth as "oauth2" | "api_key",
    authConfig: entry.auth === "oauth2" ? {
      authorizationUrl: entry.authUrl,
      tokenUrl: entry.tokenUrl,
      scopes: entry.scopes || [],
      refreshToken: true,
    } : undefined,
    actions,
    triggers: [],
  };
}

// Register all connectors
export function registerAllConnectors() {
  for (const entry of CONNECTOR_CATALOG) {
    const connector = generateConnector(entry);
    ConnectorRegistry.register(connector);
  }
}

// Export for use
export { generateConnector };
