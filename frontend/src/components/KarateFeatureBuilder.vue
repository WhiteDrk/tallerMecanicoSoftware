<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { testReportsApi } from '../services/testReportsApi'

const props = defineProps({ checks: { type: Array, required: true }, reports: { type: Array, required: true } })
const emit = defineEmits(['close', 'busy', 'completed', 'session-expired'])
const maxCases = 6
let nextId = 0
const checkById = computed(() => new Map(props.checks.map(check => [check.id, check])))
const makeCase = check => ({
  id: ++nextId, checkId: check.id, name: check.name,
  expectedStatus: check.expectedStatus, vin: '', reportId: '', minStates: 1
})
const draft = reactive({
  title: '', maxMs: 1000,
  cases: ['disponibilidad', 'catalogo'].map(id => checkById.value.get(id)).filter(Boolean).map(makeCase)
})
if (!draft.cases.length) draft.cases.push(makeCase(props.checks[0]))

const busy = ref('')
const source = ref('')
const error = ref('')
const incompleteReport = computed(() => draft.cases.some(row =>
  checkById.value.get(row.checkId)?.requiresReport && !props.reports.some(report => report.id === row.reportId)))

watch(draft, () => { source.value = ''; error.value = '' })

function changeCheck(row, id) {
  const check = checkById.value.get(id)
  Object.assign(row, { checkId: id, name: check.name, expectedStatus: check.expectedStatus, vin: '', reportId: '', minStates: 1 })
}

function addCase() {
  if (draft.cases.length >= maxCases) return
  const next = props.checks.find(check => !draft.cases.some(row => row.checkId === check.id)) || props.checks[0]
  draft.cases.push(makeCase(next))
}

function spec() {
  return {
    title: draft.title,
    maxMs: draft.maxMs,
    cases: draft.cases.map(row => {
      const check = checkById.value.get(row.checkId)
      return {
        checkId: row.checkId, name: row.name,
        vin: check.requiresVin ? row.vin.trim() : null,
        reportId: check.requiresReport ? row.reportId : null,
        expectedStatus: check.requiresVin ? row.expectedStatus : null,
        minStates: check.id === 'catalogo' ? row.minStates : null
      }
    })
  }
}

async function submit(event) {
  const action = event.submitter?.value === 'preview' ? 'preview' : 'run'
  if (busy.value || incompleteReport.value) return
  busy.value = action
  emit('busy', true)
  error.value = ''
  try {
    if (action === 'preview') {
      source.value = (await testReportsApi.previewFeature(spec())).source
    } else {
      emit('completed', await testReportsApi.runFeature(spec()))
    }
  } catch (cause) {
    error.value = cause.message
    if (cause.status === 401) emit('session-expired')
  } finally {
    busy.value = ''
    emit('busy', false)
  }
}

function downloadFeature() {
  const url = URL.createObjectURL(new Blob([source.value], { type: 'text/plain;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = 'verificaciones.feature'
  document.body.append(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function reportLabel(report) {
  const date = new Date(report.executedAt || report.createdAt).toLocaleString('es-MX')
  return `${report.title} · ${date}`
}
</script>

<template>
  <form class="feature-builder" aria-labelledby="builder-title" @submit.prevent="submit">
    <div class="builder-heading">
      <div><h2 id="builder-title">Crear un feature de Karate</h2><p>Combina consultas de MotorDesk en una sola ejecución.</p></div>
      <button type="button" class="builder-secondary" :disabled="!!busy" @click="emit('close')">Cerrar</button>
    </div>
    <fieldset :disabled="!!busy" class="builder-fields">
      <div class="feature-fields">
        <label>Nombre del feature<input v-model.trim="draft.title" required maxlength="120" placeholder="Registro y consulta en el taller"></label>
        <label>Límite por respuesta (ms)<input v-model.number="draft.maxMs" required type="number" min="1" max="5000"></label>
      </div>
      <div class="builder-heading case-heading">
        <h3>Escenarios <span>{{ draft.cases.length }} de {{ maxCases }}</span></h3>
        <button type="button" class="builder-secondary" :disabled="draft.cases.length >= maxCases" @click="addCase">
          <i class="pi pi-plus" aria-hidden="true"></i> Agregar escenario
        </button>
      </div>
      <fieldset v-for="(row, index) in draft.cases" :key="row.id" class="builder-case">
        <legend>Escenario {{ index + 1 }}</legend>
        <div class="case-fields">
          <label>Consulta
            <select :value="row.checkId" @change="changeCheck(row, $event.target.value)">
              <option v-for="check in checks" :key="check.id" :value="check.id">{{ check.name }}</option>
            </select>
          </label>
          <label>Nombre del escenario<input v-model.trim="row.name" required maxlength="90"></label>
          <label v-if="checkById.get(row.checkId).requiresVin">VIN de prueba
            <input v-model.trim="row.vin" required minlength="17" maxlength="17" pattern="[A-HJ-NPR-Za-hj-npr-z0-9]{17}">
          </label>
          <label v-if="checkById.get(row.checkId).requiresVin">Resultado esperado
            <select v-model.number="row.expectedStatus"><option :value="200">200 · Vehículo encontrado</option><option :value="404">404 · Vehículo inexistente</option></select>
          </label>
          <label v-if="row.checkId === 'catalogo'">Cantidad mínima de estados
            <input v-model.number="row.minStates" required type="number" min="1" max="32">
          </label>
          <label v-if="checkById.get(row.checkId).requiresReport">Reporte de prueba
            <select v-model="row.reportId" required>
              <option value="" disabled>Selecciona un reporte existente</option>
              <option v-for="report in reports" :key="report.id" :value="report.id">{{ reportLabel(report) }}</option>
            </select>
          </label>
        </div>
        <div class="case-footer">
          <p><code>{{ checkById.get(row.checkId).request }}</code><small>{{ checkById.get(row.checkId).authenticated ? 'Con sesión' : 'Sin sesión' }} · HTTP {{ row.expectedStatus }}</small></p>
          <button type="button" class="remove-case" :disabled="draft.cases.length === 1"
            :aria-label="`Quitar escenario ${index + 1}`" @click="draft.cases.splice(index, 1)">Quitar</button>
        </div>
      </fieldset>
    </fieldset>
    <p v-if="incompleteReport && !reports.length" class="builder-note">Guarda un reporte de prueba para poder verificar su descarga.</p>
    <p v-if="error" class="builder-error" role="alert">{{ error }}</p>
    <div class="builder-actions">
      <button class="builder-secondary" type="submit" value="preview" :disabled="!!busy || incompleteReport">
        {{ busy === 'preview' ? 'Generando…' : 'Ver .feature' }}
      </button>
      <button class="builder-primary" type="submit" value="run" :disabled="!!busy || incompleteReport">
        <i class="pi pi-play" aria-hidden="true"></i> {{ busy === 'run' ? 'Ejecutando…' : 'Ejecutar feature' }}
      </button>
      <span v-if="busy === 'run'" class="builder-note" role="status">Karate está ejecutando los escenarios.</span>
    </div>
    <section v-if="source" class="feature-preview" aria-label="Código generado">
      <div class="builder-heading"><h3>verificaciones.feature</h3><button type="button" class="builder-secondary" @click="downloadFeature">Descargar .feature</button></div>
      <pre><code>{{ source }}</code></pre>
    </section>
  </form>
</template>

<style scoped>
.feature-builder { background: #fff; border: 1px solid #dce4ec; border-radius: 16px; padding: 25px; margin-top: 20px; }
.builder-heading, .case-footer, .builder-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.builder-heading h2 { font-size: 18px; font-weight: 750; margin: 0 0 4px; }
.builder-heading h3 { font-size: 14px; font-weight: 750; margin: 0; }
.builder-heading h3 span { color: #64748b; font-size: 12px; font-weight: 500; margin-left: 6px; }
.builder-heading p, .builder-note { color: #475569; font-size: 13px; margin: 0; }
.builder-fields { border: 0; padding: 0; margin: 18px 0 0; min-width: 0; }
.feature-fields, .case-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 13px; }
.feature-fields { grid-template-columns: minmax(0, 1fr) minmax(0, 230px); }
.case-heading { margin: 20px 0 10px; }
.builder-case { border: 1px solid #dce4ec; border-radius: 10px; padding: 13px 16px; margin: 0 0 13px; min-width: 0; }
.builder-case legend { padding: 0 6px; font-size: 12px; font-weight: 700; color: #0f766e; }
label { display: block; font-size: 12px; font-weight: 650; color: #334155; }
input, select { display: block; width: 100%; margin-top: 6px; padding: 9px; border: 1px solid #cbd5e1; border-radius: 7px; background: #fff; color: #1e293b; font: inherit; }
.case-footer { margin-top: 12px; }
.case-footer p { margin: 0; min-width: 0; }
.case-footer code { color: #0f766e; font-size: 12px; overflow-wrap: anywhere; }
.case-footer small { display: block; color: #475569; font-size: 11px; margin-top: 4px; }
.builder-actions { justify-content: flex-start; margin-top: 14px; }
.builder-primary, .builder-secondary { font-size: 13px; font-weight: 650; border-radius: 8px; padding: 9px 12px; cursor: pointer; }
.builder-primary { color: #fff; background: #0f766e; border: 1px solid #0f766e; }
.builder-secondary { color: #334155; background: #fff; border: 1px solid #d7e0e8; }
.remove-case { font-size: 12px; color: #9f1239; cursor: pointer; }
button:disabled { opacity: .55; cursor: default; }
.builder-error { color: #9f1239; background: #fff1f2; font-size: 13px; padding: 10px; border-radius: 8px; }
.feature-preview { border-top: 1px solid #dce4ec; margin-top: 18px; padding-top: 15px; }
.feature-preview pre { max-height: 340px; overflow: auto; background: #0f172a; color: #d1fae5; padding: 13px; border-radius: 8px; margin: 12px 0 0; font-size: 12px; line-height: 1.6; }
@media (max-width: 720px) { .feature-fields, .case-fields { grid-template-columns: 1fr; } }
</style>
