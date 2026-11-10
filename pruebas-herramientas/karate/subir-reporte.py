"""Publica un paquete ZIP en el portal privado de reportes de MotorDesk."""

import argparse
import getpass
import json
from pathlib import Path
import secrets
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ORIGIN = 'https://motordesk-pruebas.up.railway.app'


def send(path, body, headers):
    request = Request(ORIGIN + path, data=body, headers={**headers, 'Origin': ORIGIN}, method='POST')
    try:
        with urlopen(request, timeout=60) as response:
            return json.load(response)
    except HTTPError as error:
        try:
            message = json.load(error).get('message', '')
        except (ValueError, UnicodeError):
            message = ''
        raise SystemExit(f'No se pudo publicar: HTTP {error.code}. {message}') from None
    except (URLError, TimeoutError):
        raise SystemExit('No se pudo conectar con Railway. El ZIP sigue guardado localmente para reintentar la subida.') from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('zip', type=Path)
    parser.add_argument('--title', default='Consulta de vehículos · carga progresiva')
    parser.add_argument('--tool', choices=['KARATE', 'BURP'], default='KARATE')
    parser.add_argument('--notes', default='Ejecución desde la computadora local contra Railway. Paralelismo del ejecutor; una ejecución por etapa.')
    parser.add_argument('--credentials-file', type=Path, help='JSON privado local; nunca se sube al servidor')
    args = parser.parse_args()
    if not args.zip.is_file() or not 0 < args.zip.stat().st_size <= 8 * 1024 * 1024:
        parser.error('Selecciona un ZIP existente de hasta 8 MB')
    if args.credentials_file:
        accounts = json.loads(args.credentials_file.read_text())
        account = next(a for a in accounts if 'ADMINISTRADOR' in a['roles'])
        email, password = account['email'], account['password']
    else:
        email = input('Correo de administración: ').strip()
        password = getpass.getpass('Contraseña: ')
    login = send('/api/v1/auth/login', json.dumps({'email': email, 'password': password}).encode(), {'Content-Type': 'application/json'})
    token = login['accessToken']
    boundary = 'MotorDesk' + secrets.token_hex(16)
    parts = []
    for key, value in {'title': args.title, 'tool': args.tool, 'notes': args.notes}.items():
        parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n').encode())
    parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="reporte.zip"\r\nContent-Type: application/zip\r\n\r\n').encode())
    parts.extend([args.zip.read_bytes(), f'\r\n--{boundary}--\r\n'.encode()])
    result = send('/api/v1/test-reports', b''.join(parts), {
        'Content-Type': f'multipart/form-data; boundary={boundary}', 'Authorization': 'Bearer ' + token,
    })
    print(json.dumps({'id': result['id'], 'enlace': ORIGIN + '/#reportes/' + result['id'],
                      'resultado': result['outcome'], 'casos': result['total']}, ensure_ascii=False))


if __name__ == '__main__':
    main()
