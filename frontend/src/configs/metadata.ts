import type { Metadata } from "next"

export const defaultMetadata: Metadata = {
  title: {
    template: "%s | Cursion",
    default: "Cursion",
  },
  description: "IPaaS Platform - Integration Catalog",
  applicationName: "Cursion",
  generator: "Next.js",
}

export function getPageMetadata(title: string, description?: string): Metadata {
  return {
    title,
    description,
  }
}
