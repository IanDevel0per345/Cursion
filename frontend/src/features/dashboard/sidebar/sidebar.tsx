'use client'

import Link from 'next/link'

export function Sidebar() {
  return (
    <aside className="w-64 border-r bg-black/50 p-4">
      <div className="mb-6">
        <Link href="/" className="text-xl font-bold text-orange-500">
          Cursion
        </Link>
        <p className="text-xs text-muted-foreground mt-1">IPaaS Platform</p>
      </div>
      <nav className="space-y-1">
        <Link 
          href="/" 
          className="block px-3 py-2 rounded-md text-sm font-medium hover:bg-orange-500/10 hover:text-orange-500 transition-colors"
        >
          Dashboard
        </Link>
        <Link 
          href="/integrations" 
          className="block px-3 py-2 rounded-md text-sm font-medium hover:bg-orange-500/10 hover:text-orange-500 transition-colors"
        >
          Integrations
        </Link>
        <Link 
          href="/settings" 
          className="block px-3 py-2 rounded-md text-sm font-medium hover:bg-orange-500/10 hover:text-orange-500 transition-colors"
        >
          Settings
        </Link>
      </nav>
    </aside>
  )
}
