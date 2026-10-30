import { vi } from 'vitest'

export const jsonResponse = (status, body) =>
  new Response(body === undefined ? '' : JSON.stringify(body), { status })

export function mockFetch(...responses) {
  const mock = vi.fn()
  responses.forEach((response) => mock.mockResolvedValueOnce(response))
  vi.stubGlobal('fetch', mock)
  return mock
}
