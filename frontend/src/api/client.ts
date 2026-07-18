export class ApiError extends Error {
  status: number
  details: string[]

  constructor(status: number, message: string, details: string[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

const TOKEN_KEY = 'todolist.accessToken'

export function getStoredToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function storeToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
}

type RequestOptions = RequestInit & {
  token?: string | null
  authenticated?: boolean
}

async function parseError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as {
      message?: string
      details?: string[]
    }
    return new ApiError(
      response.status,
      body.message ?? 'Не удалось выполнить запрос',
      body.details ?? [],
    )
  } catch {
    return new ApiError(response.status, 'Не удалось выполнить запрос')
  }
}

export async function apiRequest<T>(
  path: string,
  { token, authenticated = true, headers, ...init }: RequestOptions = {},
): Promise<T> {
  const requestHeaders = new Headers(headers)
  if (init.body && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json')
  }

  const accessToken = token === undefined ? getStoredToken() : token
  if (authenticated && accessToken) {
    requestHeaders.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(`/api${path}`, {
    ...init,
    headers: requestHeaders,
  })
  if (!response.ok) {
    throw await parseError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}
