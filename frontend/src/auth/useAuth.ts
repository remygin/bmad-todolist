import { createContext, useContext } from 'react'
import { ApiError, type MeResponse } from '../api/auth'

export type AuthContextValue = {
  user: MeResponse | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}
