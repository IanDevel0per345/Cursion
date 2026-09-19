import Link from "next/link";
import { getConnectors } from "@/connectors";

export default async function IntegrationsPage() {
  const connectors = getConnectors();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-orange-500">Integrations</h1>
        <p className="text-muted-foreground mt-1">
          Connect your favorite apps and services
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {connectors.map(connector => (
          <div
            key={connector.id}
            className="rounded-lg border bg-card p-6 hover:border-orange-500/50 transition-colors"
          >
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-lg bg-orange-500/10 flex items-center justify-center">
                <span className="text-orange-500 font-bold text-lg">
                  {connector.icon?.charAt(0).toUpperCase() || connector.id.charAt(0).toUpperCase()}
                </span>
              </div>
              <div>
                <h3 className="font-semibold">{connector.name}</h3>
                <p className="text-xs text-muted-foreground">{connector.authType.toUpperCase()}</p>
              </div>
            </div>

            <p className="text-sm text-muted-foreground mb-4">
              {connector.description}
            </p>

            <div className="space-y-2">
              <p className="text-xs font-medium">Available actions:</p>
              <ul className="text-xs text-muted-foreground space-y-1">
                {connector.actions.slice(0, 3).map(action => (
                  <li key={action.id}>• {action.name}</li>
                ))}
              </ul>
            </div>

            <Link
              href={`/integrations/connect/${connector.id}`}
              className="mt-4 block w-full text-center py-2 px-4 rounded-md bg-orange-500 hover:bg-orange-600 text-white text-sm font-medium transition-colors"
            >
              Connect
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}
