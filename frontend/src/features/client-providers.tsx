'use client'
import { ThemeProvider } from 'next-themes'
import { NuqsAdapter } from 'nuqs/adapters/next/app'
import { Toaster } from '@/ui/primitives/sonner'
import { ToastProvider } from '@/ui/primitives/toast'
import { TooltipProvider } from '@/ui/primitives/tooltip'
export default function ClientProviders({ children }: { children: React.ReactNode }) {
  return (<NuqsAdapter><ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange><TooltipProvider><ToastProvider>{children}</ToastProvider><Toaster /></TooltipProvider></ThemeProvider></NuqsAdapter>)
}