async function request(path = '', options = {}, download = false) {
  const token = sessionStorage.getItem('accessToken')
  const response = await fetch(`/api/v1/test-reports${path}`, {
    ...options, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(options.headers || {}) }
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const error = new Error(response.status === 401 ? 'Tu sesión terminó. Inicia sesión nuevamente.' : body.message || 'No fue posible consultar los reportes.')
    error.status = response.status
    throw error
  }
  return download ? response.blob() : response.json()
}

export const testReportsApi = {
  list: () => request(),
  detail: id => request(`/${encodeURIComponent(id)}`),
  upload: ({ title, tool, notes, file }) => {
    const data = new FormData()
    data.append('title', title); data.append('tool', tool); data.append('notes', notes); data.append('file', file)
    return request('', { method: 'POST', body: data })
  },
  download: id => request(`/${encodeURIComponent(id)}/archive`, {}, true)
}
