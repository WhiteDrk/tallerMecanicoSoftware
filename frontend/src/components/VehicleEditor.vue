<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import Swal from 'sweetalert2'
import { vehicleApi, vehicleCatalogApi } from '../services/vehicleApi'

const props = defineProps({ vehicle: { type: Object, required: true } })
const emit = defineEmits(['saved', 'cancel', 'busy', 'stale'])
const form = reactive({ vin: props.vehicle.vin, plate: props.vehicle.plate,
  brandId: props.vehicle.brandId, modelId: props.vehicle.modelId,
  manualBrand: '', manualModel: '', year: props.vehicle.year, version: props.vehicle.version,
  mileage: props.vehicle.mileage, serialNumber: props.vehicle.serialNumber, revision: props.vehicle.revision })
const brands = ref([]), models = ref([])
const manual = ref(false), saving = ref(false), catalogLoading = ref(false), catalogError = ref(''), error = ref('')
let mounted = true, modelRequest = 0
const token = sessionStorage.getItem('accessToken')
const current = () => mounted && token === sessionStorage.getItem('accessToken')
onBeforeUnmount(() => { mounted = false; modelRequest++ })

async function loadModels() {
  const request = ++modelRequest
  form.modelId = null; models.value = []; catalogError.value = ''
  if (!form.brandId) return
  catalogLoading.value = true
  try {
    const result = await vehicleCatalogApi.models(form.brandId)
    if (current() && request === modelRequest) models.value = result
  } catch (e) {
    if (current() && request === modelRequest) catalogError.value = 'No fue posible cargar modelos. Vuelve a seleccionar la marca.'
  } finally { if (mounted && request === modelRequest) catalogLoading.value = false }
}
function changeMode() {
  modelRequest++; catalogLoading.value = false
  form.brandId = null; form.modelId = null; models.value = []
  form.manualBrand = ''; form.manualModel = ''; catalogError.value = ''
}
onMounted(async () => {
  catalogLoading.value = true
  try {
    const [brandOptions, modelOptions] = await Promise.all([
      vehicleCatalogApi.brands(), vehicleCatalogApi.models(form.brandId)
    ])
    if (!current()) return
    // Keep the current association selectable even for a manually captured/pending catalog item.
    brands.value = brandOptions.some(b => b.id === form.brandId) ? brandOptions : [...brandOptions, { id: form.brandId, name: props.vehicle.brand }]
    models.value = modelOptions.some(m => m.id === form.modelId) ? modelOptions : [...modelOptions, { id: form.modelId, name: props.vehicle.model }]
  } catch (e) { if (current()) catalogError.value = 'No fue posible cargar el catálogo. Cancela y vuelve a abrir la edición.' }
  finally { if (mounted) catalogLoading.value = false }
})

async function save() {
  if (saving.value || !current()) return
  error.value = ''
  const plate = form.plate.toUpperCase().replace(/\s+/g, '')
  const serialNumber = form.serialNumber.toUpperCase().replace(/\s+/g, '')
  const mileage = form.mileage === '' || form.mileage == null ? null : Number(form.mileage)
  const manualName = /^[A-ZÁÉÍÓÚÜÑ0-9][A-ZÁÉÍÓÚÜÑ0-9 &().,'/+-]{1,99}$/
  if (!/^[A-Z0-9]+(?:-[A-Z0-9]+){0,2}$/.test(plate) || plate.length > 10 || !/^[A-Z0-9]{5,8}$/.test(plate.replace(/-/g, ''))) error.value = 'La placa debe tener de 5 a 8 letras o números y hasta dos guiones.'
  else if (!/^[A-Z0-9-]{4,40}$/.test(serialNumber)) error.value = 'Número de serie: de 4 a 40 letras, números o guiones.'
  else if (!Number.isInteger(Number(form.year)) || Number(form.year) < 1886 || Number(form.year) > new Date().getFullYear() + 1) error.value = 'El año no es válido.'
  else if (!form.version.trim() || form.version.trim().length > 100) error.value = 'La versión es obligatoria, máximo 100 caracteres.'
  else if (mileage != null && (!Number.isSafeInteger(mileage) || mileage < 0)) error.value = 'El kilometraje debe ser un entero no negativo.'
  else if (manual.value && (!manualName.test(form.manualBrand.trim().toUpperCase()) || !manualName.test(form.manualModel.trim().toUpperCase()))) error.value = 'Marca y modelo manuales: de 2 a 100 caracteres válidos.'
  else if (!manual.value && (!form.brandId || !form.modelId)) error.value = 'Selecciona marca y modelo.'
  if (error.value) return
  saving.value = true; emit('busy', true)
  try {
    const result = await vehicleApi.update(props.vehicle.id, { ...form, plate, serialNumber,
      year: Number(form.year), mileage,
      brandId: manual.value ? null : form.brandId, modelId: manual.value ? null : form.modelId,
      manualBrand: manual.value ? form.manualBrand : '', manualModel: manual.value ? form.manualModel : '' })
    if (!current()) return
    emit('saved', result)
    await Swal.fire({ icon: 'success', title: 'Ficha actualizada', text: 'Los cambios se guardaron y quedaron auditados.' })
  } catch (e) {
    if (!current()) return
    error.value = ({ 401: 'La sesión expiró. Inicia sesión nuevamente.', 403: 'No tienes permiso para editar este vehículo.', 404: 'El vehículo ya no está disponible en tus talleres.' })[e.status] || e.message
    if (e.status === 409 || e.status === 404) emit('stale', error.value)
  } finally { saving.value = false; if (mounted) emit('busy', false) }
}
</script>

<template>
  <form class="grid sm:grid-cols-2 gap-4 mt-5" @submit.prevent="save">
    <fieldset :disabled="saving" class="contents">
      <label class="field">VIN · no modificable<input :value="form.vin" readonly /></label>
      <label class="field">Placa<input v-model="form.plate" required maxlength="10" /></label>
      <template v-if="!manual">
        <label class="field">Marca<select v-model="form.brandId" required :disabled="catalogLoading" @change="loadModels"><option :value="null" disabled>Selecciona</option><option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option></select></label>
        <label class="field">Modelo<select v-model="form.modelId" required :disabled="catalogLoading || !form.brandId"><option :value="null" disabled>Selecciona</option><option v-for="m in models" :key="m.id" :value="m.id">{{ m.name }}</option></select></label>
      </template>
      <template v-else><label class="field">Marca manual<input v-model="form.manualBrand" required minlength="2" maxlength="100" /></label><label class="field">Modelo manual<input v-model="form.manualModel" required minlength="2" maxlength="100" /></label></template>
      <label class="sm:col-span-2 text-sm flex gap-2 items-center"><input v-model="manual" type="checkbox" :disabled="catalogLoading" @change="changeMode" /> No encuentro marca/modelo: capturar para revisión</label>
      <p v-if="catalogError" class="sm:col-span-2 text-rose-700 text-sm" role="alert">{{ catalogError }}</p>
      <label class="field">Año<input v-model="form.year" required type="number" min="1886" :max="new Date().getFullYear() + 1" step="1" /></label>
      <label class="field">Versión<input v-model="form.version" required maxlength="100" /></label>
      <label class="field">Kilometraje · opcional<input v-model="form.mileage" type="number" min="0" step="1" max="9007199254740991" /></label>
      <label class="field">Número de serie<input v-model="form.serialNumber" required minlength="4" maxlength="40" /></label>
      <p v-if="error" class="sm:col-span-2 text-rose-700 text-sm" role="alert">{{ error }}</p>
      <div class="sm:col-span-2 flex justify-end gap-3"><button type="button" class="px-4 py-3" @click="emit('cancel')">Cancelar edición</button><button type="submit" :disabled="catalogLoading || !!catalogError" class="rounded-lg px-5 py-3 bg-teal-700 text-white font-semibold disabled:opacity-60">{{ saving ? 'Guardando…' : 'Guardar ficha' }}</button></div>
    </fieldset>
  </form>
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }
.field input,.field select { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; background:#fff; }
.field input[readonly] { background:#e8eeed; color:#64748b; }
</style>
