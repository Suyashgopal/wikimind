import { afterEach, describe, expect, it, vi } from 'vitest'
import { ask, login, setToken, setUnauthorizedHandler } from '../api'
import { jsonResponse, mockFetch } from './helpers'

afterEach(() => {
  vi.unstubAllGlobals()
  setUnauthorizedHandler(() => {})
})

describe('api', () => {
  it('sends the token as a bearer header', async () => {
    const fetchMock = mockFetch(jsonResponse(200, { answer: 'ok' }))
    setToken('abc')

    await expect(ask('hi')).resolves.toEqual({ answer: 'ok' })

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer abc')
  })

  it('ends the session when a logged-in request gets a 401', async () => {
    mockFetch(jsonResponse(401))
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    setToken('expired')

    await expect(ask('hi')).rejects.toMatchObject({ status: 401 })

    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('shows the server message for a failed login without ending a session', async () => {
    mockFetch(jsonResponse(401, { message: 'invalid username or password' }))
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)

    await expect(login('a', 'b')).rejects.toThrow('invalid username or password')

    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('reports an unreachable server clearly', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('failed')))

    await expect(ask('hi')).rejects.toThrow("Can't reach the server")
  })
})
