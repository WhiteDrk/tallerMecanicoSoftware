<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { testReportsApi } from '../services/testReportsApi'

defineProps({ canUpload: Boolean })
const emit = defineEmits(['session-expired'])
const reports = ref([]), selected = ref(null), loading = ref(false), uploading = ref(false), downloading = ref(false)
const error = ref(''), notice = ref(''), fileInput = ref(null), showUpload = ref(false)
const form = reactive({ title: '', tool: 'KARATE', notes: '', file: null })
const showRun = ref(false), checks = ref([]), loadingChecks = ref(false), running = ref(false)
const runForm = reactive({ checks: [], maxMs: 1000, vin: '', reportId: '' })
let selectionRequest = 0
const outcomes = { APROBADO: 'Aprobado', FALLIDO: 'Con fallos', INCOMPLETO: 'Incompleto', DOCUMENTADO: 'Documentado' }
const stages = computed(() => selected.value?.summary?.etapas || [])
const verifications = computed(() => selected.value?.summary?.verificaciones || [])
const needsVin = computed(() => checks.value.some(check => check.requiresVin && runForm.checks.includes(check.id)))
const needsReport = computed(() => checks.value.some(check => check.requiresReport && runForm.checks.includes(check.id)))
const reportOptions = computed(() => {
  const report = selected.value
  return report && !reports.value.some(item => item.id === report.id) ? [report, ...reports.value] : reports.value
})
const date = value => value ? new Date(value).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' }) : 'Sin fecha de ejecución'
const metric = value => value == null ? '—' : Number(value).toLocaleString('es-MX')
const httpCodes = value => value ? Object.entries(value).map(([code,count]) => `${code}: ${count}`).join(' · ') : '—'
function fail(cause) { error.value = cause.message; if (cause.status === 401) emit('session-expired') }
function openReport(report) {
  selected.value = report
  history.replaceState(null, '', `#reportes/${report.id}`)
}
async function select(id) {
  const current = ++selectionRequest
  error.value = ''; selected.value = null
  try {
    const report = await testReportsApi.detail(id)
    if (current !== selectionRequest) return
    openReport(report)
  } catch (cause) { if (current === selectionRequest) fail(cause) }
}
async function load() {
  loading.value = true; error.value = ''
  try { reports.value = await testReportsApi.list() }
  catch (cause) { fail(cause) }
  finally { loading.value = false }
}
async function toggleRun() {
  showRun.value = !showRun.value
  showUpload.value = false
  if (!showRun.value || checks.value.length) return
  loadingChecks.value = true; error.value = ''
  try {
    checks.value = await testReportsApi.checks()
    runForm.checks = checks.value.filter(check => !check.requiresVin && !check.requiresReport).map(check => check.id)
  } catch (cause) {
    showRun.value = false
    fail(cause)
  } finally { loadingChecks.value = false }
}
function toggleUpload() {
  showUpload.value = !showUpload.value
  showRun.value = false
}
async function runChecks() {
  if (running.value || !runForm.checks.length) return
  running.value = true; error.value = ''; notice.value = ''
  try {
    const result = await testReportsApi.run({
      checks: runForm.checks,
      maxMs: runForm.maxMs,
      vin: needsVin.value ? runForm.vin.trim() : null,
      reportId: needsReport.value ? runForm.reportId : null
    })
    await load()
    selectionRequest++
    openReport(result)
    showRun.value = false
    runForm.vin = ''
    notice.value = 'Ejecución guardada en el historial.'
  } catch (cause) { fail(cause) }
  finally { running.value = false }
}
function chooseFile(event) { form.file = event.target.files?.[0] || null }
async function upload() {
  if (!form.file || !form.title.trim()) return
  if (form.file.size > 8 * 1024 * 1024) { error.value = 'El paquete debe pesar como máximo 8 MB.'; return }
  uploading.value = true; error.value = ''; notice.value = ''
  try {
    const result = await testReportsApi.upload(form)
    await load(); await select(result.id)
    form.title = ''; form.notes = ''; form.file = null
    if (fileInput.value) fileInput.value.value = ''
    showUpload.value = false; notice.value = 'Reporte guardado y disponible para las cuentas autorizadas.'
  } catch (cause) { fail(cause) }
  finally { uploading.value = false }
}
async function download() {
  if (!selected.value || downloading.value) return
  const id = selected.value.id
  downloading.value = true; error.value = ''
  try {
    const blob = await testReportsApi.download(id)
    const url = URL.createObjectURL(blob), link = document.createElement('a')
    link.href = url; link.download = `motordesk-reporte-${id}.zip`
    document.body.append(link); link.click(); link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 10000)
  } catch (cause) { fail(cause) }
  finally { downloading.value = false }
}
async function copyLink() {
  try { await navigator.clipboard.writeText(location.href); notice.value = 'Enlace copiado. La persona necesitará iniciar sesión con una cuenta autorizada.' }
  catch { notice.value = 'Puedes copiar la dirección actual del navegador para compartir este reporte.' }
}
onMounted(async () => {
  await load()
  const id = location.hash.match(/^#reportes\/([0-9a-f-]{36})$/i)?.[1]
  if (id) await select(id)
})
</script>

<template>
  <section class="reports-page">
    <div class="reports-heading"><div><span class="reports-eyebrow">CONTROL DE CALIDAD</span><h1>Reportes de pruebas</h1><p>{{ canUpload ? 'Ejecuta verificaciones de MotorDesk y consulta sus resultados.' : 'Consulta los resultados de MotorDesk y descarga la evidencia.' }}</p></div>
      <div class="reports-actions">
        <button v-if="canUpload" class="primary" :disabled="running || uploading || loadingChecks" :aria-expanded="showRun" @click="toggleRun"><i class="pi pi-play"></i> Ejecutar pruebas</button>
        <button v-if="canUpload" class="secondary" :disabled="running || uploading || loadingChecks" :aria-expanded="showUpload" @click="toggleUpload"><i class="pi pi-upload"></i> Subir evidencia</button>
        <button class="secondary" :disabled="loading || running" @click="load"><i class="pi pi-refresh"></i> Actualizar</button>
      </div>
    </div>
    <p v-if="error" class="reports-alert error" role="alert">{{ error }}</p>
    <p v-if="notice" class="reports-alert success" role="status">{{ notice }}</p>
    <form v-if="canUpload && showRun" class="reports-upload run-panel" @submit.prevent="runChecks">
      <h2>Pruebas con Karate</h2>
      <p v-if="loadingChecks" class="reports-hint" role="status">Cargando pruebas…</p>
      <template v-else>
        <fieldset class="check-fieldset" :disabled="running">
          <legend>Selecciona los casos</legend>
          <div class="check-grid">
            <label v-for="check in checks" :key="check.id" class="check-option" :class="{ checked: runForm.checks.includes(check.id) }">
              <input v-model="runForm.checks" type="checkbox" :value="check.id">
              <span><strong>{{ check.name }}</strong><small>{{ check.description }}</small></span>
              <span class="check-http">HTTP {{ check.expectedStatus }}</span>
            </label>
          </div>
          <div class="upload-grid run-inputs">
            <label>Límite por respuesta (ms)<input v-model.number="runForm.maxMs" type="number" min="1" max="5000" required></label>
            <label v-if="needsVin">VIN de prueba<input v-model.trim="runForm.vin" required minlength="17" maxlength="17"
              pattern="[A-HJ-NPR-Za-hj-npr-z0-9]{17}" placeholder="VIN de un vehículo registrado"></label>
            <label v-if="needsReport" class="wide">Reporte que quieres verificar
              <select v-model="runForm.reportId" required>
                <option value="" disabled>Selecciona un reporte existente</option>
                <option v-for="report in reportOptions" :key="report.id" :value="report.id">
                  {{ report.title }} · {{ date(report.executedAt || report.createdAt) }}
                </option>
              </select>
              <small v-if="!reportOptions.length" class="reports-hint">Guarda una primera ejecución o sube evidencia para verificar su descarga.</small>
            </label>
          </div>
        </fieldset>
        <p class="reports-hint">Los tiempos se miden dentro del servidor de MotorDesk. El resultado se guarda al terminar.</p>
        <div class="reports-actions">
          <button class="secondary" type="button" :disabled="running" @click="showRun=false">Cerrar</button>
          <button class="primary" :disabled="running || !runForm.checks.length || (needsReport && !runForm.reportId)"><i class="pi" :class="running ? 'pi-spin pi-spinner' : 'pi-play'"></i>{{ running ? 'Ejecutando…' : 'Iniciar ejecución' }}</button>
          <span v-if="running" class="reports-hint" role="status">Karate está verificando los casos seleccionados.</span>
        </div>
      </template>
    </form>
    <form v-if="canUpload && showUpload" class="reports-upload" @submit.prevent="upload">
      <h2>Agregar evidencia</h2><div class="upload-grid"><label>Título<input v-model.trim="form.title" required maxlength="140" placeholder="Consulta de vehículos · carga progresiva" /></label>
      <label>Herramienta<select v-model="form.tool"><option value="KARATE">Karate</option><option value="BURP">Burp Community</option></select></label>
      <label class="wide">Paquete ZIP · máximo 8 MB<input ref="fileInput" required type="file" accept=".zip,application/zip" @change="chooseFile" /></label>
      <label class="wide">Observaciones<textarea v-model.trim="form.notes" maxlength="2000" rows="3" placeholder="Objetivo, condiciones y aspectos relevantes de esta ejecución." /></label></div>
      <p class="reports-hint">{{ form.tool === 'KARATE' ? 'Selecciona el ZIP de una ejecución de Karate con resultados.json.' : 'Incluye las capturas o los documentos de la prueba manual de Burp.' }}</p>
      <div class="reports-actions"><button class="secondary" type="button" :disabled="uploading" @click="showUpload=false">Cancelar</button><button class="primary" :disabled="uploading">{{ uploading ? 'Guardando…' : 'Guardar reporte' }}</button></div>
    </form>
    <div class="reports-layout"><aside class="reports-list"><div class="list-title"><h2>Últimas ejecuciones</h2><small>Hasta 50 reportes</small></div>
      <p v-if="loading" class="reports-empty">Cargando reportes…</p>
      <p v-else-if="!reports.length" class="reports-empty">{{ error ? 'No fue posible cargar el historial.' : 'Todavía no hay reportes de pruebas.' }}</p>
      <button v-for="report in reports" :key="report.id" class="report-item" :class="{selected:selected?.id===report.id}" @click="select(report.id)">
        <span class="report-item-meta"><b>{{ report.tool === 'KARATE' ? 'Karate' : 'Burp' }}</b><span class="report-badge" :class="report.outcome.toLowerCase()">{{ outcomes[report.outcome] }}</span></span>
        <strong>{{ report.title }}</strong><small>{{ date(report.executedAt || report.createdAt) }}</small>
        <span v-if="report.total != null" class="report-counts">{{ metric(report.total) }} casos · {{ metric(report.failed) }} fallidos</span>
      </button>
    </aside><article class="report-detail">
      <div v-if="!selected" class="reports-placeholder"><i class="pi pi-chart-bar"></i><h2>La evidencia, en un solo lugar</h2><p>Selecciona una ejecución para ver su resumen y descargar los archivos originales.</p></div>
      <template v-else>
        <div class="detail-heading"><div><span class="reports-eyebrow">{{ selected.tool === 'KARATE' ? 'KARATE · PRUEBAS DE API' : 'BURP · PRUEBAS MANUALES' }}</span><h2>{{ selected.title }}</h2><p>{{ date(selected.executedAt || selected.createdAt) }}</p></div><span class="report-badge" :class="selected.outcome.toLowerCase()">{{ outcomes[selected.outcome] }}</span></div>
        <div v-if="selected.total != null" class="report-metrics"><div><strong>{{ metric(selected.total) }}</strong><span>Casos ejecutados</span></div><div><strong>{{ metric(selected.passed) }}</strong><span>Aprobados</span></div><div :class="{hasFailures:selected.failed>0}"><strong>{{ metric(selected.failed) }}</strong><span>Fallidos</span></div></div>
        <p v-if="selected.notes" class="report-notes">{{ selected.notes }}</p>
        <dl class="report-context"><template v-if="selected.summary.origen"><dt>Aplicación evaluada</dt><dd>{{ selected.summary.origen }}</dd></template>
          <template v-if="selected.summary.umbral_ms != null"><dt>Límite por respuesta</dt><dd>{{ metric(selected.summary.umbral_ms) }} ms</dd></template>
          <template v-if="selected.summary.reporte_verificado"><dt>Reporte verificado</dt><dd><button class="report-reference" type="button" @click="select(selected.summary.reporte_verificado)">Ver reporte original</button></dd></template>
          <dt>Publicado en el portal</dt><dd>{{ date(selected.createdAt) }}</dd></dl>
        <div v-if="verifications.length" class="report-table verification-table">
          <table aria-label="Resultados por caso">
            <thead><tr><th>Prueba</th><th>HTTP esperado</th><th>HTTP obtenido</th><th>Respuesta</th><th>Resultado</th></tr></thead>
            <tbody>
              <tr v-for="row in verifications" :key="row.nombre">
                <td><strong>{{ row.nombre }}</strong><small>{{ row.esperado }}</small><small v-if="row.detalle" class="verification-note">{{ row.detalle }}</small></td>
                <td>{{ metric(row.http_esperado) }}</td>
                <td>{{ metric(row.http_obtenido) }}</td>
                <td>{{ row.tiempo_ms == null ? '—' : `${metric(row.tiempo_ms)} ms` }}</td>
                <td><span class="report-badge" :class="row.resultado.toLowerCase()">{{ outcomes[row.resultado] }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else-if="stages.length" class="report-table"><table><thead><tr><th>Etapa</th><th>Casos</th><th>Paralelismo</th><th>Fallidos</th><th>HTTP: cantidad</th><th>p95</th><th>Máximo</th></tr></thead><tbody>
          <tr v-for="(stage,index) in stages" :key="index"><td>{{ stage.nombre || `Etapa ${index+1}` }}</td><td v-if="stage.error" colspan="6">{{ stage.error }}</td><template v-else><td>{{ metric(stage.escenarios) }}</td><td>{{ metric(stage.paralelismo) }}</td><td>{{ metric(stage.fallidos) }}</td><td>{{ httpCodes(stage.estados_http) }}</td><td>{{ metric(stage.http_p95_ms) }} ms</td><td>{{ metric(stage.http_maximo_ms) }} ms</td></template></tr>
        </tbody></table></div>
        <p v-if="!verifications.length && stages.length" class="reports-hint">El paralelismo del ejecutor no equivale a usuarios simultáneos. Los tiempos incluyen la red. Un resultado aprobado no demuestra la capacidad máxima del sistema.</p>
        <div class="reports-actions detail-actions"><button class="primary" :disabled="downloading" @click="download"><i class="pi pi-download"></i> {{ downloading ? 'Preparando…' : 'Descargar reporte completo' }}</button><button class="secondary" @click="copyLink"><i class="pi pi-link"></i> Copiar enlace</button></div>
        <p class="reports-hint">{{ verifications.length ? 'El ZIP incluye los resultados y el archivo .feature de Karate.' : 'El ZIP conserva los archivos originales para consultarlos fuera del portal.' }} El enlace requiere una cuenta autorizada.</p>
      </template>
    </article></div>
  </section>
</template>

<style scoped>
.reports-page{max-width:1380px;margin:auto;color:#1e293b}.reports-heading,.detail-heading{display:flex;justify-content:space-between;gap:20px;align-items:flex-start}.reports-heading h1{font-size:30px;font-weight:750;margin:5px 0}.reports-heading p,.detail-heading p{color:#64748b;margin-top:5px}.reports-eyebrow{font-size:11px;letter-spacing:.14em;color:#0f766e;font-weight:750}.reports-actions{display:flex;gap:10px;align-items:center;flex-wrap:wrap}.reports-actions button{display:flex;align-items:center;gap:8px;border-radius:9px;padding:11px 15px;font-weight:650;font-size:13px;cursor:pointer}.primary{background:#0f766e;color:white;border:1px solid #0f766e}.secondary{background:white;color:#334155;border:1px solid #d7e0e8}button:disabled{opacity:.55;cursor:wait}.reports-layout{display:grid;grid-template-columns:300px minmax(0,1fr);gap:20px;margin-top:26px}.reports-list,.report-detail,.reports-upload{background:white;border:1px solid #dce4ec;border-radius:16px;overflow:hidden}.list-title{padding:20px;border-bottom:1px solid #e2e8f0}.list-title h2{font-size:15px;font-weight:750}.list-title small{font-size:11px;color:#64748b}.report-item{display:block;width:100%;text-align:left;padding:20px;border:0;border-bottom:1px solid #e2e8f0;background:white;cursor:pointer;border-left:3px solid transparent}.report-item:hover{background:#f8fafc}.report-item.selected{background:#f0fdfa;border-left-color:#0f766e}.report-item strong{display:block;margin:10px 0 7px;font-size:14px;line-height:1.5}.report-item small{display:block;font-size:11px;color:#64748b}.report-item-meta{display:flex;justify-content:space-between;gap:10px;align-items:center;font-size:11px}.report-counts{display:block;margin-top:12px;font-size:11px;color:#475569}.report-badge{display:inline-block;padding:4px 9px;border-radius:20px;font-size:10px;font-weight:750;white-space:nowrap;background:#eff6ff;color:#1e40af}.report-badge.aprobado{background:#d1fae5;color:#065f46}.report-badge.fallido{background:#ffe4e6;color:#9f1239}.report-badge.incompleto{background:#fef3c7;color:#92400e}.report-detail{padding:28px;min-width:0}.detail-heading h2{font-size:22px;font-weight:750;line-height:1.4;margin-top:7px}.detail-heading p{font-size:12px}.report-metrics{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:25px 0}.report-metrics>div{padding:18px;background:#f1f5f9;border-radius:11px}.report-metrics strong{display:block;font-size:28px;color:#0f766e;font-weight:750}.report-metrics span{font-size:11px;color:#64748b}.report-metrics .hasFailures strong{color:#be123c}.report-notes{white-space:pre-wrap;line-height:1.6;margin:20px 0;font-size:14px}.report-context{display:grid;grid-template-columns:160px minmax(0,1fr);gap:10px;font-size:12px;margin:20px 0}.report-context dt{color:#64748b}.report-context dd{overflow-wrap:anywhere}.report-table{overflow-x:auto;margin:22px 0}.report-table table{width:100%;border-collapse:collapse;font-size:12px;white-space:nowrap}.report-table th{background:#f8fafc;color:#64748b;font-size:10px}.report-table th,.report-table td{padding:13px 10px;text-align:left;border-bottom:1px solid #e2e8f0}.reports-hint{font-size:12px;line-height:1.65;color:#64748b;margin:15px 0}.detail-actions{margin-top:24px}.reports-placeholder,.reports-empty{color:#64748b;text-align:center;padding:55px 20px}.reports-placeholder>i{font-size:36px;color:#0f766e}.reports-placeholder h2{font-size:20px;color:#334155;margin:20px 0 12px;font-weight:700}.reports-placeholder p{font-size:14px;line-height:1.6;max-width:340px;margin:auto}.reports-empty{font-size:13px;line-height:1.65}.reports-upload{padding:25px;margin-top:20px}.reports-upload h2{font-size:18px;font-weight:750;margin-bottom:18px}.upload-grid{display:grid;grid-template-columns:2fr 1fr;gap:16px}.upload-grid label{font-size:12px;font-weight:650;color:#475569}.upload-grid input,.upload-grid select,.upload-grid textarea{display:block;width:100%;border:1px solid #cbd5e1;padding:10px;border-radius:8px;margin-top:7px;color:#1e293b;background:white;font:inherit}.upload-grid .wide{grid-column:1/-1}.reports-alert{padding:14px 18px;border-radius:10px;font-size:13px;line-height:1.6;margin-top:18px}.reports-alert.error{background:#fff1f2;color:#9f1239}.reports-alert.success{background:#ecfdf5;color:#065f46}:focus-visible{outline:3px solid #5eead4;outline-offset:2px}@media(max-width:1050px){.reports-layout{grid-template-columns:250px minmax(0,1fr)}.reports-heading{flex-direction:column}}@media(max-width:750px){.reports-layout{grid-template-columns:1fr}.report-detail{padding:20px}.upload-grid{grid-template-columns:1fr}.report-context{grid-template-columns:1fr;gap:4px}.report-context dd{margin-bottom:12px}.report-metrics>div{padding:12px}.report-metrics strong{font-size:24px}.detail-heading{flex-wrap:wrap}.report-item{padding:16px}}
</style>

<style scoped>
.check-fieldset {
  margin: 0;
  padding: 0;
  border: 0;
  min-width: 0;
}
.check-fieldset legend { margin-bottom: 9px; font-size: 13px; font-weight: 700; }
.check-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.check-option {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 10px;
  padding: 13px;
  border: 1px solid #dce4ec;
  border-radius: 9px;
  cursor: pointer;
}
.check-option.checked { border-color: #0f766e; background: #f0fdfa; }
.check-option input { margin-top: 3px; accent-color: #0f766e; }
.check-option strong { font-size: 13px; }
.check-option small, .verification-table small {
  display: block;
  margin-top: 4px;
  color: #475569;
  line-height: 1.4;
  white-space: normal;
}
.check-http { padding: 4px 7px; border-radius: 5px; background: #e2e8f0; white-space: nowrap; font-size: 10px; }
.run-inputs { grid-template-columns: minmax(0, 175px) minmax(0, 325px); margin-top: 18px; }
.verification-table td:first-child { min-width: 185px; white-space: normal; }
.verification-table .verification-note { color: #9f1239; }
.report-reference { color: #0f766e; font: inherit; text-decoration: underline; cursor: pointer; }
@media (max-width: 750px) {
  .check-grid, .run-inputs { grid-template-columns: 1fr; }
}
</style>
