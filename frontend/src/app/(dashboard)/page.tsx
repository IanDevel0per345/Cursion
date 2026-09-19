export default function HomePage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-orange-500">Cursion Dashboard</h1>
        <p className="text-muted-foreground mt-1">
          IPaaS Platform - Integration Catalog
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <div className="rounded-lg border bg-card p-6">
          <h3 className="font-semibold mb-2">Connected Integrations</h3>
          <p className="text-2xl font-bold">0</p>
          <p className="text-sm text-muted-foreground">No integrations connected yet</p>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <h3 className="font-semibold mb-2">API Calls (24h)</h3>
          <p className="text-2xl font-bold">0</p>
          <p className="text-sm text-muted-foreground">Start connecting integrations</p>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <h3 className="font-semibold mb-2">Status</h3>
          <p className="text-2xl font-bold text-green-500">Active</p>
          <p className="text-sm text-muted-foreground">Phase 3 Complete</p>
        </div>
      </div>

      <div className="rounded-lg border bg-card p-6">
        <h3 className="font-semibold mb-2">Getting Started</h3>
        <ol className="list-decimal list-inside space-y-2 text-sm text-muted-foreground">
          <li>Connect your first integration from the catalog</li>
          <li>Configure OAuth authentication</li>
          <li>Execute actions and monitor usage</li>
        </ol>
      </div>
    </div>
  )
}
