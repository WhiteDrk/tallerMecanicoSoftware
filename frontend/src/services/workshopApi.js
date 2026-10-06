function headers() {
  const token = sessionStorage.getItem('accessToken')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function managers() {
  const response = await fetch('/api/v1/workshops/managers', { headers: headers() })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible consultar los gerentes')
  return body
}

export async function availableWorkshops() {
  const response = await fetch('/api/v1/workshops/available', { headers: headers() })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible consultar los talleres disponibles')
  return body
}

export async function registerWorkshop(payload, photo) {
  return saveWorkshop('/api/v1/workshops', 'POST', payload, photo)
}

export async function listWorkshops() {
  const response = await fetch('/api/v1/workshops', { headers: headers() })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible consultar los talleres')
  return body
}

export async function getWorkshop(id) {
  const response = await fetch(`/api/v1/workshops/${id}`, { headers: headers() })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible consultar el taller')
  return body
}

export async function updateWorkshop(id, payload, photo) {
  return saveWorkshop(`/api/v1/workshops/${id}`, 'PUT', payload, photo)
}

export async function deactivateWorkshop(id) {
  const response = await fetch(`/api/v1/workshops/${id}`, { method: 'DELETE', headers: headers() })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || 'No fue posible desactivar el taller')
  }
}

async function saveWorkshop(url, method, payload, photo) {
  const form = new FormData()
  form.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  if (photo) form.append('photo', photo)
  const response = await fetch(url, { method, headers: headers(), body: form })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible registrar el taller')
  return body
}
