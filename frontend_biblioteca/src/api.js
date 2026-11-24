let token = null
let refreshPromise = null

export function setToken(value) { token = value }
async function refresh() {
  if (!refreshPromise) refreshPromise = fetch('/api/v1/auth/refresh', { method: 'POST', credentials: 'include' })
    .then(async response => { if (!response.ok) throw new Error('Sessão expirada. Entre novamente.'); const data = await response.json(); token = data.accessToken; return data })
    .finally(() => { refreshPromise = null })
  return refreshPromise
}
export async function request(path, options = {}, retry = true) {
  const headers = new Headers(options.headers || {})
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`/api/v1${path}`, { ...options, headers, credentials: 'include' })
  if (response.status === 401 && token && retry) { await refresh(); return request(path, options, false) }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error([error.message || `Não foi possível concluir a operação (${response.status}).`, ...(error.fields || []).map(f => `${f.field}: ${f.message}`)].join(' '))
  }
  if (response.status === 204) return null
  if (options.raw) return response.blob()
  return response.json()
}
export async function restoreSession() { return refresh() }
export const send = (path, method, body) => request(path, { method, body: body === undefined ? undefined : JSON.stringify(body) })
