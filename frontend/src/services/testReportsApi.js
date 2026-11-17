async function request(path = '', options = {}, download = false) {
  const token = sessionStorage.getItem('accessToken')
  const response = await fetch(`/api/v1/test-reports${path}`, {
    ...options, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(options.headers || {}) }
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const messages = {
      401: 'Tu sesión terminó. Inicia sesión nuevamente.',
      409: 'Hay una ejecución en curso. Intenta de nuevo en unos segundos.'
    }
    const error = new Error(messages[response.status] || body.message || (options.method === 'POST'
      ? 'No fue posible guardar el resultado.'
      : 'No fue posible consultar los reportes.'))
    error.status = response.status
    throw error
  }
  return download ? response.blob() : response.json()
}

const jsonPost = body => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })

export const testReportsApi = {
  list: () => request(),
  checks: () => request('/checks'),
  run: spec => request('/runs', jsonPost(spec)),
  previewFeature: spec => request('/features/preview', jsonPost(spec)),
  runFeature: spec => request('/features', jsonPost(spec)),
  detail: id => request(`/${encodeURIComponent(id)}`),
  upload: ({ title, tool, notes, file }) => {
    const data = new FormData()
    data.append('title', title); data.append('tool', tool); data.append('notes', notes); data.append('file', file)
    return request('', { method: 'POST', body: data })
  },
  downloadPdf: id => request(`/${encodeURIComponent(id)}/pdf`, {}, true),
  download: id => request(`/${encodeURIComponent(id)}/archive`, {}, true)
}
