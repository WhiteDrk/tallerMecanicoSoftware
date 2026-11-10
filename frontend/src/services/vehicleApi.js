function tokenHeaders() {
  const token = sessionStorage.getItem('accessToken')
  return { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
}
async function request(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { ...tokenHeaders(), ...(options.headers || {}) } })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    const error = new Error(body.message || 'No fue posible completar la operación del vehículo')
    error.status = response.status
    error.code = body.code
    throw error
  }
  return body
}
export const vehicleCatalogApi = {
  brands: () => request('/api/v1/vehicle-catalog/brands'),
  models: (brandId) => request(`/api/v1/vehicle-catalog/brands/${brandId}/models`)
}
export const vehicleApi = {
  update: (id, payload) => request(`/api/v1/vehicles/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deactivate: (id, payload) => request(`/api/v1/vehicles/${id}/deactivation`, { method: 'POST', body: JSON.stringify(payload) }),
  lookup: (criterion, value, signal) => request(`/api/v1/vehicles?${new URLSearchParams({ criterion, value })}`, { signal, cache: 'no-store' }),
  register: (clientId, payload) => request(`/api/v1/clients/${clientId}/vehicles`, { method: 'POST', body: JSON.stringify(payload) })
}
