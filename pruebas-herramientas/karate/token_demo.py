"""Cambia el rol de un JWT conservando su firma: produce un token inválido para la demostración."""

import base64
import getpass
import json


def change_role(token):
    parts = token.strip().removeprefix('Bearer ').split('.')
    if len(parts) != 3 or len(token) > 8192:
        raise ValueError('Se esperaba un JWT de tres partes')
    payload = parts[1]
    claims = json.loads(base64.urlsafe_b64decode(payload + '=' * (-len(payload) % 4)))
    claims['roles'] = ['ROLE_ADMINISTRADOR']
    parts[1] = base64.urlsafe_b64encode(json.dumps(claims, separators=(',', ':')).encode()).decode().rstrip('=')
    return '.'.join(parts)


if __name__ == '__main__':
    token = getpass.getpass('Pega aquí el token de Lucía (la entrada no se muestra): ')
    print('\nToken modificado para comprobar el rechazo de la firma; no es una credencial válida:\n')
    print(change_role(token))
