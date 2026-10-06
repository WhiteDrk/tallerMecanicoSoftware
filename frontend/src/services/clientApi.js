export async function registerClient(payload, photo) {
  const form = new FormData()
  form.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  if (photo) form.append('photo', photo)
  const token = sessionStorage.getItem('accessToken')
  const response = await fetch('/api/v1/clients', { method: 'POST', headers: token ? { Authorization: `Bearer ${token}` } : {}, body: form })
  const body = response.status === 204 ? {} : await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible registrar al cliente')
  return body
}

export async function listClients({ workshopId, page = 0, size = 10, sort = 'fullName', direction = 'asc' }) {
  const token = sessionStorage.getItem('accessToken')
  const params = new URLSearchParams({ workshopId, page: String(page), size: String(size), sort, direction })
  const response = await fetch(`/api/v1/clients?${params}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible consultar los clientes')
  return body
}

export async function getClient(id) {
  const token = sessionStorage.getItem('accessToken')
  const response = await fetch(`/api/v1/clients/${id}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible consultar el cliente')
  return body
}

export async function updateClient(id, payload, photo) {
  const form = new FormData()
  form.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  if (photo) form.append('photo', photo)
  const token = sessionStorage.getItem('accessToken')
  const response = await fetch(`/api/v1/clients/${id}`, { method:'PUT', headers: token ? { Authorization: `Bearer ${token}` } : {}, body: form })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible actualizar el cliente')
  return body
}

export async function suspendClient(id) {
  const token = sessionStorage.getItem('accessToken')
  const response = await fetch(`/api/v1/clients/${id}/suspension`, { method:'POST', headers: token ? { Authorization: `Bearer ${token}` } : {} })
  if (!response.ok) { const body = await response.json().catch(() => ({})); throw new Error(body.message || 'No fue posible suspender el cliente') }
}
