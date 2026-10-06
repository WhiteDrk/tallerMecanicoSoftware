export async function login(credentials) {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials)
  })
  if (!response.ok) throw new Error('Credenciales no válidas o cuenta sin autorización.')
  return response.json()
}
