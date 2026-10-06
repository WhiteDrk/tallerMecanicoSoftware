function headers() {
  const token = sessionStorage.getItem('accessToken')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function get(path) {
  const response = await fetch(`/api/v1/address-catalog${path}`, { headers: headers() })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'No fue posible consultar el catálogo de direcciones')
  return body
}

export const addressCatalogApi = {
  states: () => get('/states'),
  municipalities: (stateId) => get(`/states/${stateId}/municipalities`),
  localities: (municipalityId) => get(`/municipalities/${municipalityId}/localities`),
  postalCode: (postalCode) => get(`/postal-codes/${postalCode}`),
  locality: (localityId) => get(`/localities/${localityId}`)
}
