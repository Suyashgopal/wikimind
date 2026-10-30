const SERVER_MESSAGES = {
  502: "The AI service couldn't answer. Please try again later.",
  503: 'The AI service is busy, try again in a moment.',
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

let token = null
let onUnauthorized = () => {}

export function setToken(value) {
  token = value
}

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

async function request(path, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(path, { method: 'POST', headers, body: JSON.stringify(body) })
  } catch {
    throw new ApiError(0, "Can't reach the server. Check that it's running.")
  }

  const data = await response.json().catch(() => null)
  if (response.ok) return data

  // A failed login is also a 401, but it has no session to end.
  if (response.status === 401 && token) {
    onUnauthorized()
    throw new ApiError(401, 'Your session has expired. Please log in again.')
  }
  throw new ApiError(
    response.status,
    SERVER_MESSAGES[response.status] ?? data?.message ?? 'Something went wrong. Please try again.',
  )
}

export const register = (username, password, organization) =>
  request('/api/auth/register', { username, password, organization })

export const login = (username, password) => request('/api/auth/login', { username, password })

export const addDocument = (title, content) => request('/api/rag/add', { title, content })

export const ask = (question) => request('/api/rag/ask', { question })
