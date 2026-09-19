"use client";
import { useState } from "react";

export default function ApiKeysPage() {
  const [name, setName] = useState("");
  const [newKey, setNewKey] = useState<string | null>(null);

  const handleCreate = async () => {
    if (!name) return;
    const response = await fetch("/api/api-keys", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name }),
    });
    const data = await response.json();
    if (data.success) setNewKey(data.data.key);
  };

  return (
    <div>
      <h1 className="text-3xl font-bold text-orange-500">API Keys</h1>
      <p className="text-muted-foreground mt-1">Manage API keys</p>
      {newKey && (
        <div className="mt-4 p-4 rounded bg-green-500/10 border border-green-500/20">
          <p className="font-medium text-green-500 mb-2">Save this key - won't be shown again!</p>
          <code className="block p-3 rounded bg-black/50 text-green-400 font-mono">{newKey}</code>
        </div>
      )}
      <div className="mt-6 flex gap-2">
        <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="Key name" className="flex-1 px-4 py-2 rounded-md border bg-background" />
        <button onClick={handleCreate} className="px-6 py-2 rounded-md bg-orange-500 hover:bg-orange-600 text-white font-medium">Create</button>
      </div>
    </div>
  );
}
