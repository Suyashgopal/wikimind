import { useState } from 'react'
import * as api from './api'

const NO_DOCUMENTS_PREFIX = "I don't have any documents"

export default function AskQuestion({ onGoToAdd }) {
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setAnswer(null)
    try {
      const result = await api.ask(question.trim())
      setAnswer(result.answer)
    } catch (failure) {
      setError(failure.message)
    } finally {
      setBusy(false)
    }
  }

  const noDocuments = answer?.startsWith(NO_DOCUMENTS_PREFIX)

  return (
    <section>
      <h2>Ask a question</h2>
      <form className="card" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="question">Your question</label>
          <input id="question" value={question} onChange={(event) => setQuestion(event.target.value)} />
        </div>
        <button type="submit" disabled={busy || !question.trim()}>
          {busy ? 'Thinking...' : 'Ask'}
        </button>
      </form>

      {error && <p role="alert" className="alert">{error}</p>}
      {noDocuments && (
        <div className="card" role="status">
          <p>Your organization hasn't added any documents yet.</p>
          <button type="button" className="link" onClick={onGoToAdd}>Add documents</button>
        </div>
      )}
      {answer && !noDocuments && (
        <div className="card answer" role="status">
          <h3>Answer</h3>
          <p>{answer}</p>
        </div>
      )}
    </section>
  )
}
