import { Paperclip, Sparkles, ArrowRight, Mic } from 'lucide-react'
import type { FormEvent } from 'react'

type Hero1Props = {
  onPromptSubmit?: (prompt: string) => void
  loginUrl: string
  signupUrl: string
  chatUrl: string
}

const suggestions = [
  'Show all customer balances',
  'Record a sale for Sharma ji',
  'Who owes me the most?',
  'Settle an outstanding balance',
]

export function Hero1({ onPromptSubmit, loginUrl, signupUrl, chatUrl }: Hero1Props) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const prompt = String(form.get('prompt') ?? '').trim()
    if (prompt) onPromptSubmit?.(prompt)
  }

  return (
    <div className="relative flex min-h-screen flex-col overflow-x-hidden bg-[#0c0414] text-white">
      <div className="pointer-events-none absolute -right-80 -top-96 z-0 flex rotate-[-20deg] skew-[-40deg] gap-40 opacity-40 blur-[4rem]">
        {[1, 2, 3].map((item) => (
          <div key={item} className="h-80 w-40 bg-gradient-to-r from-white to-blue-300" />
        ))}
      </div>
      <div className="pointer-events-none absolute -right-[30rem] -top-[32rem] z-0 flex rotate-[-20deg] skew-[-40deg] gap-40 opacity-30 blur-[4rem]">
        {[1, 2, 3].map((item) => (
          <div key={item} className="h-[30rem] w-40 bg-gradient-to-r from-fuchsia-300 to-blue-300" />
        ))}
      </div>

      <header className="relative z-10 flex items-center justify-between px-6 py-6 sm:px-10 lg:px-16">
        <a href="/" className="flex items-center gap-3 no-underline">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-blue-300 to-fuchsia-400 text-lg font-black text-[#0c0414]">
            L
          </div>
          <span className="text-lg font-bold tracking-tight">Ledgerly</span>
        </a>
        <nav className="flex items-center gap-3">
          <a href={loginUrl} className="hidden text-sm font-semibold text-purple-100/75 transition hover:text-white sm:inline">
            Log in
          </a>
          <a
            href={signupUrl}
            className="rounded-full bg-white px-5 py-2.5 text-sm font-semibold text-black transition hover:bg-blue-100"
          >
            Get started <ArrowRight className="ml-1 inline h-4 w-4" />
          </a>
        </nav>
      </header>

      <main className="relative z-10 flex flex-1 flex-col items-center justify-center px-5 pb-16 pt-8 text-center sm:pt-0">
        <div className="w-full max-w-4xl space-y-7">
          <div className="mx-auto flex w-fit items-center gap-2 rounded-full bg-[#1c1528] px-4 py-2 text-xs text-purple-100">
            <span className="rounded-full bg-black px-2 py-1">🇮🇳</span>
            Your voice-first business ledger
          </div>

          <h1 className="text-4xl font-bold leading-[1.08] tracking-tight sm:text-6xl lg:text-7xl">
            Talk it. Track it.
            <span className="block bg-gradient-to-r from-blue-200 via-white to-fuchsia-300 bg-clip-text text-transparent">
              Grow your business.
            </span>
          </h1>
          <p className="mx-auto max-w-2xl text-base leading-7 text-purple-100/70 sm:text-lg">
            Ask about customers, record sales, and understand your khata in English,
            Hindi, or Hinglish. Ledgerly keeps every change safe and confirmed.
          </p>

          <form onSubmit={handleSubmit} className="mx-auto max-w-2xl rounded-full bg-[#1c1528] p-2 shadow-2xl shadow-purple-950/40">
            <div className="flex items-center gap-1">
              <button type="button" className="rounded-full p-3 text-gray-400 transition hover:bg-[#2a1f3d] hover:text-white" aria-label="Attach file">
                <Paperclip className="h-5 w-5" />
              </button>
              <button type="button" className="rounded-full p-3 text-purple-300 transition hover:bg-[#2a1f3d] hover:text-purple-200" aria-label="Use AI suggestions">
                <Sparkles className="h-5 w-5" />
              </button>
              <input
                name="prompt"
                type="text"
                placeholder="Ask Ledgerly about your business..."
                className="min-w-0 flex-1 bg-transparent px-3 text-sm text-white outline-none placeholder:text-gray-500 sm:text-base"
              />
              <button type="submit" className="rounded-full bg-white p-3 text-black transition hover:bg-blue-100" aria-label="Send prompt">
                <ArrowRight className="h-5 w-5" />
              </button>
            </div>
          </form>

          <div className="flex items-center justify-center gap-2 text-xs text-purple-100/50">
            <Mic className="h-4 w-4" />
            Voice mode with Indian-language speech support
          </div>

          <a
            href={chatUrl}
            className="inline-flex items-center gap-2 rounded-full border border-purple-300/30 px-5 py-2.5 text-sm font-semibold text-purple-100 transition hover:border-purple-200/70 hover:bg-white/10"
          >
            Open Ledgerly chat <ArrowRight className="h-4 w-4" />
          </a>

          <div className="mx-auto flex max-w-3xl flex-wrap justify-center gap-2 pt-5">
            {suggestions.map((suggestion) => (
              <button
                key={suggestion}
                type="button"
                onClick={() => onPromptSubmit?.(suggestion)}
                className="rounded-full bg-[#1c1528] px-4 py-2 text-sm text-purple-100/80 transition hover:bg-[#2a1f3d] hover:text-white"
              >
                {suggestion}
              </button>
            ))}
          </div>
        </div>
      </main>
    </div>
  )
}

export default Hero1
