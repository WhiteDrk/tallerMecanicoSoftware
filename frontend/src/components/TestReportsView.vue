<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { testReportsApi } from '../services/testReportsApi'

defineProps({ canUpload: Boolean })
const emit = defineEmits(['session-expired'])
const reports = ref([]), selected = ref(null), loading = ref(false), uploading = ref(false), downloading = ref(false)
const error = ref(''), notice = ref(''), fileInput = ref(null), showUpload = ref(false)
const form = reactive({ title: '', tool: 'KARATE', notes: '', file: null })
let selectionRequest = 0
const outcomes = { APROBADO: 'Aprobado', FALLIDO: 'Con fallos', INCOMPLETO: 'Incompleto', DOCUMENTADO: 'Documentado' }
const stages = computed(() => selected.value?.summary?.etapas || [])
const date = value => value ? new Date(value).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' }) : 'Sin fecha de ejecución'
const metric = value => value == null ? '—' : Number(value).toLocaleString('es-MX')
const httpCodes = value => value ? Object.entries(value).map(([code,count]) => `${code}: ${count}`).join(' · ') : '—'
function fail(cause) { error.value = cause.message; if (cause.status === 401) emit('session-expired') }
async function select(id) {
  const current = ++selectionRequest
  error.value = ''; selected.value = null
  try {
    const report = await testReportsApi.detail(id)
    if (current !== selectionRequest) return
    selected.value = report
    history.replaceState(null, '', `#reportes/${id}`)
  } catch (cause) { if (current === selectionRequest) fail(cause) }
}
async function load() {
  loading.value = true; error.value = ''
  try { reports.value = await testReportsApi.list() }
  catch (cause) { fail(cause) }
  finally { loading.value = false }
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
    <div class="reports-heading"><div><span class="reports-eyebrow">CONTROL DE CALIDAD</span><h1>Reportes de pruebas</h1><p>Consulta las ejecuciones, revisa sus resultados y comparte la evidencia.</p></div>
      <div class="reports-actions"><button class="secondary" :disabled="loading" @click="load"><i class="pi pi-refresh"></i> Actualizar</button><button v-if="canUpload" class="primary" @click="showUpload=!showUpload"><i class="pi pi-upload"></i> Subir reporte</button></div></div>
    <p v-if="error" class="reports-alert error" role="alert">{{ error }}</p>
    <p v-if="notice" class="reports-alert success" role="status">{{ notice }}</p>
    <form v-if="canUpload && showUpload" class="reports-upload" @submit.prevent="upload">
      <h2>Agregar evidencia</h2><div class="upload-grid"><label>Título<input v-model.trim="form.title" required maxlength="140" placeholder="Consulta de vehículos · carga progresiva" /></label>
      <label>Herramienta<select v-model="form.tool"><option value="KARATE">Karate</option><option value="BURP">Burp Community</option></select></label>
      <label class="wide">Paquete ZIP · máximo 8 MB<input ref="fileInput" required type="file" accept=".zip,application/zip" @change="chooseFile" /></label>
      <label class="wide">Observaciones<textarea v-model.trim="form.notes" maxlength="2000" rows="3" placeholder="Objetivo, condiciones y aspectos relevantes de esta ejecución." /></label></div>
      <p class="reports-hint">{{ form.tool === 'KARATE' ? 'Selecciona el ZIP generado por ejecutar-nube.py. El resumen y las etapas se cargarán automáticamente.' : 'Incluye capturas o documentos de las pruebas manuales. Se mostrarán como evidencia documentada, sin inventar métricas de carga.' }}</p>
      <div class="reports-actions"><button class="secondary" type="button" :disabled="uploading" @click="showUpload=false">Cancelar</button><button class="primary" :disabled="uploading">{{ uploading ? 'Guardando…' : 'Guardar reporte' }}</button></div>
    </form>
    <div class="reports-layout"><aside class="reports-list"><div class="list-title"><h2>Últimas ejecuciones</h2><small>Hasta 50 reportes</small></div>
      <p v-if="loading" class="reports-empty">Cargando reportes…</p>
      <p v-else-if="!reports.length" class="reports-empty">{{ error ? 'No fue posible cargar el historial.' : 'Todavía no hay reportes. Administración puede subir la primera ejecución.' }}</p>
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
          <template v-if="selected.summary.umbral_ms != null"><dt>Umbral de práctica</dt><dd>{{ metric(selected.summary.umbral_ms) }} ms por respuesta</dd></template><dt>Publicado en el portal</dt><dd>{{ date(selected.createdAt) }}</dd></dl>
        <div v-if="stages.length" class="report-table"><table><thead><tr><th>Etapa</th><th>Casos</th><th>Paralelismo</th><th>Fallidos</th><th>HTTP: cantidad</th><th>p95</th><th>Máximo</th></tr></thead><tbody>
          <tr v-for="(stage,index) in stages" :key="index"><td>{{ stage.nombre || `Etapa ${index+1}` }}</td><td v-if="stage.error" colspan="6">{{ stage.error }}</td><template v-else><td>{{ metric(stage.escenarios) }}</td><td>{{ metric(stage.paralelismo) }}</td><td>{{ metric(stage.fallidos) }}</td><td>{{ httpCodes(stage.estados_http) }}</td><td>{{ metric(stage.http_p95_ms) }} ms</td><td>{{ metric(stage.http_maximo_ms) }} ms</td></template></tr>
        </tbody></table></div>
        <p v-if="stages.length" class="reports-hint">El paralelismo del ejecutor no equivale a usuarios simultáneos. Los tiempos incluyen la red. Un resultado aprobado no demuestra la capacidad máxima del sistema.</p>
        <div class="reports-actions detail-actions"><button class="primary" :disabled="downloading" @click="download"><i class="pi pi-download"></i> {{ downloading ? 'Preparando…' : 'Descargar reporte completo' }}</button><button class="secondary" @click="copyLink"><i class="pi pi-link"></i> Copiar enlace</button></div>
        <p class="reports-hint">El enlace requiere una cuenta autorizada. El ZIP conserva los reportes originales y sus recursos para consultarlos fuera del portal.</p>
      </template>
    </article></div>
  </section>
</template>

<style scoped>
.reports-page{max-width:1380px;margin:auto;color:#1e293b}.reports-heading,.detail-heading{display:flex;justify-content:space-between;gap:20px;align-items:flex-start}.reports-heading h1{font-size:30px;font-weight:750;margin:5px 0}.reports-heading p,.detail-heading p{color:#64748b;margin-top:5px}.reports-eyebrow{font-size:11px;letter-spacing:.14em;color:#0f766e;font-weight:750}.reports-actions{display:flex;gap:10px;align-items:center;flex-wrap:wrap}.reports-actions button{display:flex;align-items:center;gap:8px;border-radius:9px;padding:11px 15px;font-weight:650;font-size:13px;cursor:pointer}.primary{background:#0f766e;color:white;border:1px solid #0f766e}.secondary{background:white;color:#334155;border:1px solid #d7e0e8}button:disabled{opacity:.55;cursor:wait}.reports-layout{display:grid;grid-template-columns:300px minmax(0,1fr);gap:20px;margin-top:26px}.reports-list,.report-detail,.reports-upload{background:white;border:1px solid #dce4ec;border-radius:16px;overflow:hidden}.list-title{padding:20px;border-bottom:1px solid #e2e8f0}.list-title h2{font-size:15px;font-weight:750}.list-title small{font-size:11px;color:#64748b}.report-item{display:block;width:100%;text-align:left;padding:20px;border:0;border-bottom:1px solid #e2e8f0;background:white;cursor:pointer;border-left:3px solid transparent}.report-item:hover{background:#f8fafc}.report-item.selected{background:#f0fdfa;border-left-color:#0f766e}.report-item strong{display:block;margin:10px 0 7px;font-size:14px;line-height:1.5}.report-item small{display:block;font-size:11px;color:#64748b}.report-item-meta{display:flex;justify-content:space-between;gap:10px;align-items:center;font-size:11px}.report-counts{display:block;margin-top:12px;font-size:11px;color:#475569}.report-badge{display:inline-block;padding:4px 9px;border-radius:20px;font-size:10px;font-weight:750;white-space:nowrap;background:#eff6ff;color:#1e40af}.report-badge.aprobado{background:#d1fae5;color:#065f46}.report-badge.fallido{background:#ffe4e6;color:#9f1239}.report-badge.incompleto{background:#fef3c7;color:#92400e}.report-detail{padding:28px;min-width:0}.detail-heading h2{font-size:22px;font-weight:750;line-height:1.4;margin-top:7px}.detail-heading p{font-size:12px}.report-metrics{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:25px 0}.report-metrics>div{padding:18px;background:#f1f5f9;border-radius:11px}.report-metrics strong{display:block;font-size:28px;color:#0f766e;font-weight:750}.report-metrics span{font-size:11px;color:#64748b}.report-metrics .hasFailures strong{color:#be123c}.report-notes{white-space:pre-wrap;line-height:1.6;margin:20px 0;font-size:14px}.report-context{display:grid;grid-template-columns:160px minmax(0,1fr);gap:10px;font-size:12px;margin:20px 0}.report-context dt{color:#64748b}.report-context dd{overflow-wrap:anywhere}.report-table{overflow-x:auto;margin:22px 0}.report-table table{width:100%;border-collapse:collapse;font-size:12px;white-space:nowrap}.report-table th{background:#f8fafc;color:#64748b;font-size:10px}.report-table th,.report-table td{padding:13px 10px;text-align:left;border-bottom:1px solid #e2e8f0}.reports-hint{font-size:12px;line-height:1.65;color:#64748b;margin:15px 0}.detail-actions{margin-top:24px}.reports-placeholder,.reports-empty{color:#64748b;text-align:center;padding:55px 20px}.reports-placeholder>i{font-size:36px;color:#0f766e}.reports-placeholder h2{font-size:20px;color:#334155;margin:20px 0 12px;font-weight:700}.reports-placeholder p{font-size:14px;line-height:1.6;max-width:340px;margin:auto}.reports-empty{font-size:13px;line-height:1.65}.reports-upload{padding:25px;margin-top:20px}.reports-upload h2{font-size:18px;font-weight:750;margin-bottom:18px}.upload-grid{display:grid;grid-template-columns:2fr 1fr;gap:16px}.upload-grid label{font-size:12px;font-weight:650;color:#475569}.upload-grid input,.upload-grid select,.upload-grid textarea{display:block;width:100%;border:1px solid #cbd5e1;padding:10px;border-radius:8px;margin-top:7px;color:#1e293b;background:white;font:inherit}.upload-grid .wide{grid-column:1/-1}.reports-alert{padding:14px 18px;border-radius:10px;font-size:13px;line-height:1.6;margin-top:18px}.reports-alert.error{background:#fff1f2;color:#9f1239}.reports-alert.success{background:#ecfdf5;color:#065f46}:focus-visible{outline:3px solid #5eead4;outline-offset:2px}@media(max-width:1050px){.reports-layout{grid-template-columns:250px minmax(0,1fr)}.reports-heading{flex-direction:column}}@media(max-width:750px){.reports-layout{grid-template-columns:1fr}.report-detail{padding:20px}.upload-grid{grid-template-columns:1fr}.report-context{grid-template-columns:1fr;gap:4px}.report-context dd{margin-bottom:12px}.report-metrics>div{padding:12px}.report-metrics strong{font-size:24px}.detail-heading{flex-wrap:wrap}.report-item{padding:16px}}
</style>
