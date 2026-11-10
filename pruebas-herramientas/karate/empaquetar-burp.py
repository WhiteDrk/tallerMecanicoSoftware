"""Empaqueta evidencia manual de Burp y permite publicarla en el portal privado."""

import argparse
from datetime import datetime
from pathlib import Path
import subprocess
import sys
from zoneinfo import ZoneInfo
import zipfile

HERE = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('carpeta', type=Path, help='Carpeta con capturas o documentos de las pruebas que realizaste')
    parser.add_argument('--publish', action='store_true')
    parser.add_argument('--credentials-file', type=Path)
    args = parser.parse_args()
    folder = args.carpeta.resolve()
    if not folder.is_dir():
        parser.error('La carpeta de evidencia no existe')
    files = sorted(p for p in folder.rglob('*') if p.is_file() and not p.is_symlink()
                   and p.resolve().is_relative_to(folder) and p.suffix.lower() in {'.png', '.jpg', '.jpeg', '.pdf', '.txt'})
    if not 1 <= len(files) <= 30:
        parser.error('Incluye entre 1 y 30 capturas o documentos PNG, JPG, PDF o TXT')
    if sum(p.stat().st_size for p in files) > 8 * 1024 * 1024:
        parser.error('La evidencia supera 8 MB; selecciona menos capturas o reduce su tamaño')
    now = datetime.now(ZoneInfo('America/Mexico_City'))
    target = HERE / 'target' / ('burp-clase-' + now.strftime('%Y%m%d-%H%M%S-%f') + '.zip')
    target.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in files:
            archive.write(path, 'evidencia/' + path.relative_to(folder).as_posix())
        archive.writestr('LEEME.txt',
            'Evidencia manual aportada por el expositor para la demostración con Burp Community.\n'
            'Este paquete no ejecuta pruebas ni certifica resultados: revisar las capturas y documentos adjuntos.\n'
            'El portal lo registra como Documentado, sin inventar métricas de carga.\n')
    if target.stat().st_size > 8 * 1024 * 1024:
        target.unlink()
        parser.error('El ZIP supera el máximo de 8 MB')
    print('Paquete de evidencia: ' + str(target), flush=True)
    if args.publish:
        command = [sys.executable, str(HERE / 'subir-reporte.py'), str(target), '--tool', 'BURP',
                   '--title', 'Burp en clase · autenticación, roles y validación', '--notes',
                   'Evidencia de solicitudes manipuladas manualmente con Repeater. Consultar las capturas para ver los resultados observados.']
        if args.credentials_file:
            command.extend(['--credentials-file', str(args.credentials_file)])
        subprocess.run(command, check=True)


if __name__ == '__main__':
    main()
