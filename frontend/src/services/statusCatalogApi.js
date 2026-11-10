function headers() {
  const token = sessionStorage.getItem('accessToken')
  return { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
}

async function request(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { ...headers(), ...(options.headers || {}) } })
  const body = response.status === 204 ? null : await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body?.message || 'No fue posible gestionar el catálogo de estatus')
  return body
}

export const statusCatalogApi = {
  list: () => request('/api/v1/status-catalog'),
  create: (payload) => request('/api/v1/status-catalog', { method: 'POST', body: JSON.stringify(payload) }),
  update: (id, payload) => request(`/api/v1/status-catalog/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
}
