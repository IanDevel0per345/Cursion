"use client";

import { useParams } from "next/navigation";
import { useState } from "react";

export default function ConnectPage() {
  const params = useParams();
  const connectorId = params.connector as string;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleConnect = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/connections/${connectorId}/connect`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          redirectUri: `${window.location.origin}/api/connections/${connectorId}/callback`,
        }),
      });

      const data = await response.json();

      if (!data.success) {
        throw new Error(data.error);
      }

      // Redirect to OAuth provider
      window.location.href = data.data.authorizationUrl;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to connect");
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto text-center py-12">
      <h1 className="text-2xl font-bold text-orange-500 mb-2">
        Connect {connectorId?.charAt(0).toUpperCase()}{connectorId?.slice(1)}
      </h1>
      <p className="text-muted-foreground mb-6">
        You will be redirected to authenticate with {connectorId}
      </p>

      {error && (
        <div className="mb-4 p-3 rounded bg-red-500/10 text-red-500 text-sm">
          {error}
        </div>
      )}

      <button
        onClick={handleConnect}
        disabled={loading}
        className="w-full py-3 px-4 rounded-md bg-orange-500 hover:bg-orange-600 disabled:bg-orange-500/50 text-white font-medium transition-colors"
      >
        {loading ? "Connecting..." : "Connect with " + connectorId?.charAt(0).toUpperCase() + connectorId?.slice(1)}
      </button>

      <p className="mt-4 text-xs text-muted-foreground">
        By connecting, you agree to share your {connectorId} data with Cursion
      </p>
    </div>
  );
}
