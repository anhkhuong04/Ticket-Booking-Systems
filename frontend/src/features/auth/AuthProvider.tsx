import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { localizeApiError } from '../../shared/i18n/apiError'
import { initializeCsrf, login as loginRequest, logout as logoutRequest, refresh, register as registerRequest, type AuthUser } from './authApi'
import { setAccessToken } from '../../shared/api/apiClient'

type AuthContextValue = {
  user: AuthUser | null; ready: boolean; login: (email: string, password: string) => Promise<void>
  register: (fullName: string, email: string, password: string) => Promise<void>; logout: () => Promise<void>
  updateDisplayName: (fullName: string) => void
}
const AuthContext = createContext<AuthContextValue | null>(null)

function messageFor(error: unknown): string { return localizeApiError(error) }

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null); const [ready, setReady] = useState(false)
  const accept = useCallback((result: { accessToken: string; user: AuthUser }) => { setAccessToken(result.accessToken); setUser(result.user) }, [])
  useEffect(() => { void (async () => { try { await initializeCsrf(); accept(await refresh()) } catch { setAccessToken() } finally { setReady(true) } })() }, [accept])
  const login = useCallback(async (email: string, password: string) => { try { accept(await loginRequest(email, password)) } catch (error) { throw new Error(messageFor(error)) } }, [accept])
  const register = useCallback(async (fullName: string, email: string, password: string) => { try { accept(await registerRequest(fullName, email, password)) } catch (error) { throw new Error(messageFor(error)) } }, [accept])
  const logout = useCallback(async () => { try { await logoutRequest() } finally { setAccessToken(); setUser(null) } }, [])
  const updateDisplayName = useCallback((fullName: string) => setUser(current => current ? { ...current, fullName } : null), [])
  const value = useMemo(() => ({ user, ready, login, register, logout, updateDisplayName }), [user, ready, login, register, logout, updateDisplayName])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export function useAuth(): AuthContextValue { const value = useContext(AuthContext); if (!value) throw new Error('AuthProvider is required'); return value }
