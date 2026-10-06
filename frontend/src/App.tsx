import { Hero1 } from './components/ui/hero-1'

function App() {
  const appBaseUrl = import.meta.env.VITE_APP_URL ?? 'http://localhost:8081'
  const loginUrl = `${appBaseUrl}/login.html`
  const signupUrl = `${appBaseUrl}/signup.html`
  const chatUrl = `${appBaseUrl}/chat.html`

  const openChat = (prompt?: string) => {
    window.location.href = prompt
      ? `${chatUrl}?prompt=${encodeURIComponent(prompt)}`
      : chatUrl
  }

  return (
    <Hero1
      onPromptSubmit={openChat}
      loginUrl={loginUrl}
      signupUrl={signupUrl}
      chatUrl={chatUrl}
    />
  )
}

export default App
