import { useState } from 'react'
import AddDocuments from './AddDocuments'
import AskQuestion from './AskQuestion'
import { useAuth } from './AuthContext'
import AuthForm from './AuthForm'

export default function App() {
  const { session, logout } = useAuth()
  const [page, setPage] = useState('ask')

  if (!session) return <AuthForm />

  const tab = (name, label) => (
    <button
      type="button"
      className={page === name ? 'tab active' : 'tab'}
      aria-current={page === name ? 'page' : undefined}
      onClick={() => setPage(name)}
    >
      {label}
    </button>
  )

  return (
    <>
      <header className="topbar">
        <strong>WikiMind</strong>
        <nav aria-label="Main">
          {tab('ask', 'Ask')}
          {tab('add', 'Add documents')}
        </nav>
        <div className="user">
          <span>{session.username} ({session.organization})</span>
          <button type="button" className="link" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="content">
        {/* Both stay mounted so switching tabs doesn't lose an upload in progress. */}
        <div hidden={page !== 'ask'}><AskQuestion onGoToAdd={() => setPage('add')} /></div>
        <div hidden={page !== 'add'}><AddDocuments /></div>
      </main>
    </>
  )
}
