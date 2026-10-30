import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { setToken, setUnauthorizedHandler } from './api'

const STORAGE_KEY = 'wikimind.session'
const AuthContext = createContext(null)

function loadSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(STORAGE_KEY))
    setToken(session?.token ?? null)
    return session
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(loadSession)

  const login = useCallback((username, auth) => {
    const next = { username, token: auth.token, role: auth.role, organization: auth.organization }
    setToken(next.token)
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    setSession(next)
  }, [])

  const logout = useCallback(() => {
    setToken(null)
    sessionStorage.removeItem(STORAGE_KEY)
    setSession(null)
  }, [])

  useEffect(() => setUnauthorizedHandler(logout), [logout])

  const value = useMemo(() => ({ session, login, logout }), [session, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
