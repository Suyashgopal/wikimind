import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AddDocuments, { MAX_FILE_BYTES } from '../AddDocuments'
import App from '../App'
import AskQuestion from '../AskQuestion'
import { AuthProvider } from '../AuthContext'
import AuthForm from '../AuthForm'
import { jsonResponse, mockFetch } from './helpers'

afterEach(() => vi.unstubAllGlobals())

const renderWithAuth = (ui) => render(<AuthProvider>{ui}</AuthProvider>)

describe('AuthForm', () => {
  it('blocks a short password without calling the server', async () => {
    const fetchMock = mockFetch()
    const user = userEvent.setup()
    renderWithAuth(<AuthForm />)

    await user.click(screen.getByRole('button', { name: /create an account/i }))
    await user.type(screen.getByLabelText('Username'), 'alice')
    await user.type(screen.getByLabelText('Password'), 'short')
    await user.type(screen.getByLabelText('Organization'), 'Acme')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(screen.getByText('Password must be at least 8 characters.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})

describe('AddDocuments', () => {
  it('uploads files in order and keeps going after one fails', async () => {
    const fetchMock = mockFetch(
      jsonResponse(503, { message: 'busy' }),
      jsonResponse(200, { title: 'b.md', chunksStored: 3 }),
    )
    const user = userEvent.setup()
    render(<AddDocuments />)
    const big = new File(['x'.repeat(MAX_FILE_BYTES + 1)], 'big.md')

    await user.upload(screen.getByLabelText(/choose/i), [
      new File(['one'], 'a.md'),
      big,
      new File(['two'], 'b.md'),
    ])

    await waitFor(() => expect(screen.getByText('3 chunks stored')).toBeInTheDocument())
    expect(screen.getByText('The AI service is busy, try again in a moment.')).toBeInTheDocument()
    expect(screen.getByText(/Too large/)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(JSON.parse(fetchMock.mock.calls[0][1].body).title).toBe('a.md')
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({ title: 'b.md', content: 'two' })
  })
})

describe('AskQuestion', () => {
  const askIt = async () => {
    const user = userEvent.setup()
    render(<AskQuestion onGoToAdd={() => {}} />)
    expect(screen.getByRole('button', { name: 'Ask' })).toBeDisabled()
    await user.type(screen.getByLabelText('Your question'), 'What happens on Friday?')
    await user.click(screen.getByRole('button', { name: 'Ask' }))
  }

  it('shows the answer', async () => {
    mockFetch(jsonResponse(200, { answer: 'Pizza Day' }))
    await askIt()
    expect(await screen.findByText('Pizza Day')).toBeInTheDocument()
  })

  it('shows a friendly message on a 503', async () => {
    mockFetch(jsonResponse(503, { message: 'raw upstream text' }))
    await askIt()
    expect(await screen.findByRole('alert')).toHaveTextContent('The AI service is busy')
  })

  it('points to the add screen when there are no documents', async () => {
    mockFetch(jsonResponse(200, { answer: "I don't have any documents to answer that yet." }))
    await askIt()
    expect(await screen.findByRole('button', { name: 'Add documents' })).toBeInTheDocument()
  })
})

describe('App', () => {
  it('returns to the login screen when the token expires', async () => {
    sessionStorage.setItem('wikimind.session', JSON.stringify({ username: 'alice', token: 'old', organization: 'acme' }))
    mockFetch(jsonResponse(401))
    const user = userEvent.setup()
    renderWithAuth(<App />)

    await user.type(screen.getByLabelText('Your question'), 'hi')
    await user.click(screen.getAllByRole('button', { name: 'Ask' }).at(-1))

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })
})
