<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Swal from 'sweetalert2'
import VehicleEditor from './VehicleEditor.vue'
import { vehicleApi } from '../services/vehicleApi'

const emit = defineEmits(['close'])
const criterion = ref('VIN')
const value = ref('')
const valueInput = ref(null)
const loading = ref(false)
const vehicle = ref(null)
const validationError = ref('')
const message = ref('')
const notFound = ref(false)
const editing = ref(false)
const mutating = ref(false)
let mounted = true
let controller = null
let requestId = 0

const field = computed(() => ({
  VIN: { label: 'VIN', help: '17 caracteres, sin I, O ni Q.', placeholder: 'VIN de 17 caracteres' },
  PLATE: { label: 'Placa', help: 'De 5 a 8 letras o números y hasta dos guiones.', placeholder: 'ABC-123-A' },
  ID: { label: 'ID del vehículo', help: 'UUID del vehículo, con formato de 8-4-4-4-12 caracteres.', placeholder: 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx' }
}[criterion.value]))
const details = computed(() => vehicle.value ? [
  ['ID del vehículo', vehicle.value.id], ['VIN', vehicle.value.vin],
  ['Placa', vehicle.value.plate], ['Marca', vehicle.value.brand],
  ['Modelo', vehicle.value.model], ['Año', vehicle.value.year],
  ['Versión', vehicle.value.version], ['Kilometraje', vehicle.value.mileage == null ? null : `${vehicle.value.mileage} km`],
  ['Número de serie', vehicle.value.serialNumber], ['Estatus', vehicle.value.status],
  ['Taller', vehicle.value.workshopName]
] : [])

function invalidate() {
  requestId++
  controller?.abort()
  controller = null
  loading.value = false
  vehicle.value = null
  validationError.value = ''
  message.value = ''
  notFound.value = false
  editing.value = false
}
watch([criterion, value], invalidate, { flush: 'sync' })
onBeforeUnmount(() => { mounted = false; invalidate() })

function close() {
  if (mutating.value) return
  invalidate()
  emit('close')
}
async function search() {
  if (loading.value || mutating.value || editing.value) return
  const normalized = criterion.value === 'PLATE'
    ? value.value.toUpperCase().replace(/\s+/g, '')
    : value.value.trim().toUpperCase()
  value.value = normalized
  invalidate()
  if (criterion.value === 'VIN' && !/^[A-HJ-NPR-Z0-9]{17}$/.test(normalized)) {
    validationError.value = 'El VIN debe tener 17 letras o números y no incluir I, O ni Q.'
  } else if (criterion.value === 'PLATE' && (
    !/^[A-Z0-9]+(?:-[A-Z0-9]+){0,2}$/.test(normalized) || normalized.length > 10 ||
    normalized.replace(/-/g, '').length < 5 || normalized.replace(/-/g, '').length > 8
  )) {
    validationError.value = 'La placa debe tener de 5 a 8 letras o números, hasta dos guiones y máximo 10 caracteres.'
  } else if (criterion.value === 'ID' && !/^[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}$/.test(normalized)) {
    validationError.value = 'Captura un UUID válido del vehículo con formato 8-4-4-4-12.'
  }
  if (validationError.value) {
    valueInput.value?.focus()
    return
  }
  controller = new AbortController()
  const currentRequest = requestId
  const currentToken = sessionStorage.getItem('accessToken')
  loading.value = true
  try {
    const result = await vehicleApi.lookup(criterion.value, normalized, controller.signal)
    if (currentRequest !== requestId) return
    if (currentToken !== sessionStorage.getItem('accessToken')) { close(); return }
    vehicle.value = result
  } catch (error) {
    if (currentRequest !== requestId || error.name === 'AbortError') return
    if (currentToken !== sessionStorage.getItem('accessToken')) { close(); return }
    if (error.status === 404) {
      notFound.value = true
      message.value = 'No se encontró un vehículo con ese dato en tus talleres autorizados.'
    } else {
      message.value = ({
        400: 'El criterio o valor no es válido. Revisa el dato e intenta nuevamente.',
        401: 'Tu sesión no es válida o ha expirado. Inicia sesión nuevamente.',
        403: 'No tienes permiso para consultar vehículos.'
      })[error.status] || 'No fue posible consultar el vehículo. Revisa tu conexión e intenta nuevamente.'
    }
  } finally {
    if (currentRequest === requestId) { loading.value = false; controller = null }
  }
}

function saved(result) {
  vehicle.value = result
  editing.value = false
  mutating.value = false
}
function stale(text) {
  invalidate()
  mutating.value = false
  message.value = text
}
async function deactivate() {
  if (mutating.value || editing.value || !vehicle.value || vehicle.value.status !== 'ACTIVO') return
  const selected = vehicle.value
  const token = sessionStorage.getItem('accessToken')
  const current = () => mounted && vehicle.value?.id === selected.id && token === sessionStorage.getItem('accessToken')
  mutating.value = true
  try {
    const confirmation = await Swal.fire({ icon: 'warning', title: '¿Desactivar este vehículo?',
      text: `Placa ${selected.plate}. Pasará a INACTIVO y conservará sus datos e historial. Escribe el VIN ${selected.vin} para confirmar.`,
      input: 'text', inputLabel: 'VIN de confirmación', inputAttributes: { maxlength: '17', autocomplete: 'off' },
      showCancelButton: true, confirmButtonText: 'Sí, desactivar', cancelButtonText: 'Cancelar',
      confirmButtonColor: '#b91c1c', focusCancel: true,
      inputValidator: value => value.trim().toUpperCase() === selected.vin ? undefined : 'El VIN no coincide. No se desactivará el vehículo.' })
    if (!confirmation.isConfirmed || !current()) return
    const result = await vehicleApi.deactivate(selected.id, { confirmationVin: confirmation.value.trim().toUpperCase(), confirmed: true, revision: selected.revision })
    if (!current()) return
    vehicle.value = result
    await Swal.fire({ icon: 'success', title: 'Vehículo desactivado', text: 'Estatus INACTIVO. El cambio quedó auditado y los datos se conservaron.' })
  } catch (e) {
    if (!current()) return
    if (e.status === 409 || e.status === 404) stale(e.message)
    await Swal.fire({ icon: 'error', title: 'No se pudo desactivar', text: e.message })
  } finally { mutating.value = false }
}
</script>

<template>
  <Dialog :visible="true" modal header="Consultar vehículo" :closable="!mutating" :close-on-escape="!mutating" :style="{ width: 'min(96vw, 720px)' }" @update:visible="visible => { if (!visible) close() }">
    <p class="text-sm text-slate-500 mb-5">Busca por un solo dato en tus talleres autorizados. No necesitas seleccionar un cliente.</p>
    <form class="grid sm:grid-cols-2 gap-4" novalidate @submit.prevent="search">
      <label class="field" for="vehicle-lookup-criterion">Buscar por
        <select id="vehicle-lookup-criterion" v-model="criterion" :disabled="mutating || editing"><option value="VIN">VIN</option><option value="PLATE">Placa</option><option value="ID">ID del vehículo</option></select>
      </label>
      <div>
        <label class="field" for="vehicle-lookup-value">{{ field.label }}
          <input id="vehicle-lookup-value" ref="valueInput" v-model="value" :disabled="mutating || editing" type="text" required autocomplete="off" spellcheck="false" :placeholder="field.placeholder" :aria-invalid="!!validationError" :aria-describedby="validationError ? 'vehicle-lookup-help vehicle-lookup-error' : 'vehicle-lookup-help'" />
        </label>
        <p id="vehicle-lookup-help" class="text-xs text-slate-500 mt-2">{{ field.help }}</p>
        <p v-if="validationError" id="vehicle-lookup-error" class="text-sm text-rose-700 mt-2" role="alert">{{ validationError }}</p>
      </div>
      <div class="sm:col-span-2 flex justify-end gap-3">
        <button type="button" :disabled="mutating" class="px-4 py-3 font-semibold text-slate-600" @click="close">Cerrar</button>
        <button type="submit" :disabled="loading || mutating || editing" class="px-5 py-3 rounded-lg bg-teal-700 text-white font-semibold disabled:opacity-60">{{ loading ? 'Consultando…' : 'Consultar' }}</button>
      </div>
    </form>
    <div class="mt-5" :aria-busy="loading" aria-live="polite" aria-atomic="true">
      <p v-if="loading" class="text-sm text-slate-500" role="status"><i class="pi pi-spin pi-spinner mr-2" aria-hidden="true"></i>Consultando vehículo…</p>
      <p v-else-if="message" class="rounded-xl p-4 text-sm" :class="notFound ? 'bg-slate-50 text-slate-600' : 'bg-rose-50 text-rose-700'" :role="notFound ? 'status' : 'alert'">{{ message }}</p>
      <section v-else-if="vehicle" aria-label="Ficha del vehículo" class="rounded-xl bg-slate-50 p-4">
        <h3 class="font-semibold mb-4">Ficha del vehículo</h3>
        <dl class="grid sm:grid-cols-2 gap-4 text-sm">
          <div v-for="[label, detail] in details" :key="label" class="min-w-0"><dt class="text-slate-500">{{ label }}</dt><dd class="font-medium break-words mt-1">{{ detail == null || detail === '' ? 'No registrado' : detail }}</dd></div>
        </dl>
        <VehicleEditor v-if="editing" :vehicle="vehicle" @saved="saved" @cancel="editing = false" @busy="mutating = $event" @stale="stale" />
        <div v-else-if="vehicle.status === 'ACTIVO'" class="flex flex-wrap justify-end gap-3 mt-5">
          <button type="button" :disabled="mutating" class="px-4 py-3 rounded-lg bg-teal-700 text-white font-semibold disabled:opacity-60" @click="editing = true">Editar ficha</button>
          <button type="button" :disabled="mutating" class="px-4 py-3 rounded-lg border border-rose-300 text-rose-700 font-semibold disabled:opacity-60" @click="deactivate">{{ mutating ? 'Procesando…' : 'Desactivar vehículo' }}</button>
        </div>
        <p v-else class="text-sm text-slate-500 mt-4">Ficha histórica de vehículo {{ vehicle.status }}. Sus datos se conservan para consulta.</p>
      </section>
    </div>
  </Dialog>
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }
.field input,.field select { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; background:#fff; }
</style>
