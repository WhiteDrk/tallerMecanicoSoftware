"""Ejecuta lotes acotados contra la demostracion y prepara reportes portables."""

import argparse
import csv
import getpass
import html
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
from datetime import datetime
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from zoneinfo import ZoneInfo

ORIGIN = 'https://motordesk-pruebas.up.railway.app'
HERE = Path(__file__).resolve().parent
module_spec = importlib.util.spec_from_file_location('resumen_karate', HERE / 'resumir-concurrencia.py')
summary_module = importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(summary_module)
summarize = summary_module.summarize
STAGES = [('Referencia', 10, 1), ('Carga 1', 50, 5), ('Carga 2', 100, 10), ('Carga 3', 200, 20)]


def http(path, payload=None, token=None):
    headers = {'Origin': ORIGIN}
    if payload is not None:
        headers['Content-Type'] = 'application/json'
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = Request(ORIGIN + path, headers=headers,
                      data=json.dumps(payload).encode() if payload is not None else None)
    try:
        with urlopen(request, timeout=20) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        try:
            body = json.load(error)
        except (ValueError, UnicodeError):
            body = {}
        return error.code, body


def redact(folder, secrets):
    count = 0
    for path in folder.rglob('*'):
        if not path.is_file():
            continue
        try:
            original = path.read_text(encoding='utf-8')
        except (UnicodeError, OSError):
            continue
        cleaned = original
        for secret in secrets:
            if secret:
                cleaned = cleaned.replace(secret, '[OCULTO]')
        cleaned = re.sub(r'eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', '[TOKEN OCULTO]', cleaned)
        if cleaned != original:
            path.write_text(cleaned, encoding='utf-8')
            count += 1
    return count


def report(folder, data):
    rows = []
    for stage in data['etapas']:
        if 'escenarios' not in stage:
            rows.append('<tr><td>' + html.escape(stage['nombre']) + '</td><td colspan="7">Reporte incompleto: ' + html.escape(stage['error']) + '</td></tr>')
            continue
        rows.append('<tr>' + ''.join('<td>' + str(value) + '</td>' for value in [
            html.escape(stage['nombre']), stage['escenarios'], stage['paralelismo'],
            stage['aprobados'], stage['fallidos'], stage['http_p95_ms'], stage['http_maximo_ms'],
            '<a href="' + html.escape(stage['reporte'], quote=True) + '">Abrir detalle</a>',
        ]) + '</tr>')
    total = sum(stage.get('escenarios', 0) for stage in data['etapas'])
    failures = sum(stage.get('fallidos', 0) for stage in data['etapas'])
    interrupted = any('error' in stage for stage in data['etapas'])
    conclusion = ('La ejecucion quedo incompleta; revisar los archivos de diagnostico.' if interrupted else
                  'Hubo incumplimientos de alguna validacion; revisar el detalle antes de atribuirlos a una caida del servidor.' if failures else
                  'Todos los casos del alcance ejecutado aprobaron. No se encontro el limite de capacidad con estos lotes.')
    text = f'''<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>MotorDesk · Resultados de pruebas en nube</title><style>
*{{box-sizing:border-box}}body{{margin:0;background:#eef2f7;color:#18243b;font:16px/1.6 system-ui,sans-serif}}
main{{max-width:1120px;margin:auto;padding:40px 24px}}header{{background:#14233c;color:white;padding:34px;border-radius:18px}}
h1{{margin:8px 0;font-size:clamp(26px,4vw,40px)}}h2{{font-size:21px}}.label{{color:#83e5d0;letter-spacing:.14em;font-size:12px;font-weight:700}}
section,.card{{background:white;border:1px solid #dbe2ec;border-radius:14px;padding:24px;margin-top:20px}}
.cards{{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}}.card strong{{display:block;font-size:30px;color:#12665c}}
table{{border-collapse:collapse;width:100%;font-size:14px}}th,td{{padding:13px 10px;text-align:left;border-bottom:1px solid #e0e6ed}}
th{{background:#f1f5f9}}a{{color:#075e9b}}.table{{overflow:auto}}small{{color:#637086}}code{{background:#edf2f7;padding:3px 6px;border-radius:4px}}
@media(max-width:650px){{.cards{{grid-template-columns:1fr}}main{{padding:18px 12px}}header{{padding:24px}}}}
@media print{{body{{background:white}}main{{padding:0}}section,.card{{break-inside:avoid}}header{{background:#e9eef5;color:#18243b}}.label{{color:#12665c}}}}
</style><main><header><div class="label">EVIDENCIA DE EJECUCION · KARATE 2.1.4</div><h1>Pruebas de carga en MotorDesk</h1>
<p>Consulta de vehiculos con autenticacion y acceso a MySQL.</p><p>{html.escape(data['fecha_local'])} · America/Mexico_City</p></header>
<div class="cards"><div class="card"><strong>{total}</strong>consultas en los lotes terminados</div><div class="card"><strong>{failures}</strong>escenarios fallidos</div>
<div class="card"><strong>{data['umbral_ms']} ms</strong>umbral didactico por respuesta</div></div>
<section><h2>Objetivo y alcance</h2><p>Comprobar la consulta <code>GET /api/v1/vehicles</code> por VIN con una cuenta de recepcion autorizada. Se espera HTTP 200, el VIN de prueba y el taller correspondiente.</p>
<p>Destino: <a href="{ORIGIN}">{ORIGIN}</a>. Cliente Karate ejecutado desde la computadora local contra Railway. Un login y una comprobacion previa quedan fuera de las estadisticas.</p></section>
<section><h2>Resultados por etapa</h2><div class="table"><table><thead><tr><th>Etapa</th><th>Casos</th><th>Paralelismo</th><th>Aprobados</th><th>Fallidos</th><th>p95 (ms)</th><th>Max. (ms)</th><th>HTML Karate</th></tr></thead><tbody>{''.join(rows)}</tbody></table></div>
<p><strong>Interpretacion:</strong> {conclusion}</p></section>
<section><h2>Como interpretar los datos</h2><ul><li>Paralelismo es el maximo de escenarios del ejecutor; no equivale a usuarios virtuales ni garantiza solicitudes simultaneas exactas.</li>
<li>p95 usa rango mas cercano: aproximadamente el 95 % de las respuestas tardo ese tiempo o menos.</li>
<li>Los tiempos HTTP incluyen la red y la conexion con Railway. La duracion total de una suite tambien incluye preparacion, JVM y reportes.</li>
<li>El umbral de {data['umbral_ms']} ms es una decision de esta practica, no un SLA del producto. Una falla por tiempo no demuestra una caida.</li>
<li>Hay una ejecucion por etapa. No se midieron CPU, memoria o costos del servidor; agregar capturas de Metrics y Usage de Railway para complementar.</li>
<li>Una consulta de lectura y una cuenta no representan toda la aplicacion. Estos resultados no prueban capacidad maxima ni resistencia a todos los ataques.</li></ul></section>
<section><h2>Archivos para entregar</h2><p><a href="resultados.csv">Tabla CSV</a> · <a href="resultados.json">Resultados JSON</a> · <a href="LEEME.txt">Instrucciones</a></p>
<p>Burp Community se documenta por separado con capturas de Repeater y los codigos realmente observados. Este paquete no inventa resultados de Burp.</p>
<p>El reporte se conserva aunque se elimine el despliegue temporal. En el navegador se puede usar Imprimir → Guardar como PDF para entregar este resumen.</p></section>
<footer><p><small>Reportes revisados para retirar tokens y contraseñas. Datos del taller y vehiculo ficticios.</small></p></footer></main></html>'''
    (folder / 'index.html').write_text(text, encoding='utf-8')
    (folder / 'resultados.json').write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    fields = ['nombre', 'casos_configurados', 'paralelismo', 'escenarios', 'aprobados', 'fallidos',
              'estados_http', 'http_promedio_ms', 'http_p95_ms', 'http_maximo_ms', 'duracion_suite_ms']
    with (folder / 'resultados.csv').open('w', newline='', encoding='utf-8-sig') as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, extrasaction='ignore')
        writer.writeheader()
        writer.writerows(data['etapas'])
    (folder / 'LEEME.txt').write_text(
        'Extraer todo el ZIP y abrir index.html en el navegador. No abrir dentro del ZIP.\n'
        'El resumen enlaza los reportes originales de cada etapa. Conservar las subcarpetas y los archivos res.\n'
        'Para PDF: abrir index.html, Imprimir, Guardar como PDF.\n'
        'Puede adjuntarse el ZIP a Classroom, Moodle o un correo; este programa no envia mensajes ni publica archivos.\n'
        'Registrar por separado las pruebas manuales de Burp con capturas sin encabezados Authorization visibles.\n', encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--credentials-file', type=Path, help='JSON privado preparado para esta demostracion; no se copia al reporte')
    parser.add_argument('--max-ms', type=int, default=2000)
    parser.add_argument('--publish', action='store_true', help='Al terminar, subir el ZIP al portal con una cuenta de administración')
    args = parser.parse_args()
    if not 100 <= args.max_ms <= 10000:
        parser.error('--max-ms debe estar entre 100 y 10000')
    if args.credentials_file:
        accounts = json.loads(args.credentials_file.read_text())
        account = next(a for a in accounts if 'RECEPCIONISTA' in a['roles'])
        email, password = account['email'], account['password']
    else:
        email = input('Correo de recepcion: ').strip()
        password = getpass.getpass('Contrasena: ')
    status, body = http('/api/v1/auth/login', {'email': email, 'password': password})
    if status != 200 or not body.get('accessToken'):
        raise SystemExit('No se pudo iniciar sesion: HTTP ' + str(status))
    token = body['accessToken']
    status, body = http('/api/v1/vehicles?criterion=VIN&value=3KPF24AD3R2610131', token=token)
    if status != 200 or body.get('workshopName') != 'MotorDesk Pruebas Nube':
        raise SystemExit('La comprobacion previa no aprobo: HTTP ' + str(status) + '. No se ejecutaron lotes.')
    now = datetime.now(ZoneInfo('America/Mexico_City'))
    folder = HERE / 'target' / ('nube-' + now.strftime('%Y%m%d-%H%M%S'))
    folder.mkdir(parents=True, exist_ok=False)
    data = {'fecha_local': now.isoformat(timespec='seconds'), 'origen': ORIGIN,
            'herramienta': 'Karate 2.1.4', 'umbral_ms': args.max_ms, 'etapas': []}
    engine = Path.home() / '.karate/dist/karate-2.1.4.jar'
    if not engine.is_file():
        raise SystemExit('No se encontro el motor Karate 2.1.4 instalado.')
    for index, (name, cases, threads) in enumerate(STAGES, 1):
        print(f'{name}: {cases} consultas, paralelismo maximo {threads}', flush=True)
        stage_folder = folder / ('etapa-' + str(index))
        env = {**os.environ, 'MOTORDESK_ACCESS_TOKEN': token, 'NUBE_CASOS': str(cases), 'NUBE_MAX_MS': str(args.max_ms)}
        command = ['java', '-jar', str(engine), 'run', '-T', str(threads), '-o', str(stage_folder), str(HERE / '03-nube-consulta-autenticada.feature')]
        stage = {'nombre': name, 'casos_configurados': cases, 'paralelismo': threads}
        started = time.monotonic()
        try:
            with (folder / ('etapa-' + str(index) + '.log')).open('w') as log:
                process = subprocess.run(command, env=env, cwd=HERE, stdout=log, stderr=subprocess.STDOUT, timeout=75)
            stage.update(summarize(stage_folder))
            stage['reporte'] = str((stage_folder / 'karate-summary.html').relative_to(folder))
            stage['exit_code'] = process.returncode
        except subprocess.TimeoutExpired:
            stage['error'] = 'Se alcanzo el limite de 75 segundos del lote.'
        except (OSError, ValueError, KeyError) as error:
            stage['error'] = 'No se pudo cerrar o interpretar el reporte: ' + str(error)
        stage['duracion_proceso_s'] = round(time.monotonic() - started, 3)
        data['etapas'].append(stage)
        print(json.dumps(stage, ensure_ascii=False), flush=True)
        if stage.get('error') or stage.get('fallidos') or stage.get('exit_code'):
            print('Se detiene el aumento de carga para revisar esta etapa.', flush=True)
            break
        time.sleep(2)
    final_status, final_vehicle = http('/api/v1/vehicles?criterion=VIN&value=3KPF24AD3R2610131', token=token)
    data['comprobacion_final_http'] = final_status
    data['vehiculo_disponible_al_terminar'] = final_status == 200 and final_vehicle.get('vin') == '3KPF24AD3R2610131'
    data['archivos_redactados'] = redact(folder, [token, password])
    report(folder, data)
    redact(folder, [token, password])
    for path in folder.rglob('*'):
        if path.is_file():
            raw = path.read_bytes()
            assert token.encode() not in raw and password.encode() not in raw
    archive = shutil.make_archive(str(folder), 'zip', root_dir=folder.parent, base_dir=folder.name)
    print(json.dumps({'resumen_html': str(folder / 'index.html'), 'zip': archive}, ensure_ascii=False), flush=True)
    if args.publish:
        command = [sys.executable, str(HERE / 'subir-reporte.py'), archive]
        if args.credentials_file:
            command.extend(['--credentials-file', str(args.credentials_file)])
        subprocess.run(command, check=True)


if __name__ == '__main__':
    main()
