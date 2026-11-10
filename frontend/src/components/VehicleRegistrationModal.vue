<script setup>
import { onMounted, reactive, ref } from 'vue'
import Dialog from 'primevue/dialog'
import Swal from 'sweetalert2'
import { vehicleApi, vehicleCatalogApi } from '../services/vehicleApi'

const props = defineProps({ client: { type: Object, required: true } })
const emit = defineEmits(['close', 'saved'])
const form = reactive({ vin:'', plate:'', brandId:null, modelId:null, manualBrand:'', manualModel:'', year:null, version:'', mileage:null, serialNumber:'' })
const brands = ref([])
const models = ref([])
const saving = ref(false)
const manualCatalog = ref(false)

function normalizeUpper(field) { form[field] = form[field].toUpperCase().replace(/\s+/g, '') }
async function loadModels() {
  form.modelId = null; models.value = []
  if (!form.brandId) return
  try { models.value = await vehicleCatalogApi.models(form.brandId) }
  catch (error) { await Swal.fire({ icon:'error', title:'No fue posible cargar los modelos', text:error.message, confirmButtonColor:'#0f766e' }) }
}
function changeCatalogMode() {
  form.brandId = null; form.modelId = null; form.manualBrand = ''; form.manualModel = ''; models.value = []
}
async function save() {
  const catalogIsComplete = form.brandId && form.modelId
  const manualIsComplete = form.manualBrand.trim() && form.manualModel.trim()
  if (!form.vin || !form.plate || !form.year || !form.version || !form.serialNumber || (!catalogIsComplete && !manualIsComplete)) return Swal.fire({ icon:'warning', title:'Datos incompletos', text:'Selecciona marca y modelo o captura ambos datos manualmente.' })
  if (manualCatalog.value && !/^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9][A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 &().,'\/+-]{1,99}$/.test(form.manualBrand.trim())) return Swal.fire({ icon:'warning', title:'Marca manual no válida' })
  if (manualCatalog.value && !/^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9][A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 &().,'\/+-]{1,99}$/.test(form.manualModel.trim())) return Swal.fire({ icon:'warning', title:'Modelo manual no válido' })
  if (!/^[A-HJ-NPR-Z0-9]{17}$/.test(form.vin.toUpperCase())) return Swal.fire({ icon:'warning', title:'VIN no válido', text:'Debe tener 17 caracteres y no incluir I, O ni Q.' })
  if (!/^[A-Z0-9]+(?:-[A-Z0-9]+){0,2}$/.test(form.plate.toUpperCase().replace(/\s+/g, ''))) return Swal.fire({ icon:'warning', title:'Placa no válida' })
  if (!/^[A-Z0-9-]{4,40}$/.test(form.serialNumber.toUpperCase().replace(/\s+/g, ''))) return Swal.fire({ icon:'warning', title:'Número de serie no válido' })
  if (Number(form.year) < 1886 || Number(form.year) > new Date().getFullYear() + 1) return Swal.fire({ icon:'warning', title:'Año no válido' })
  if (form.mileage !== null && form.mileage !== '' && Number(form.mileage) < 0) return Swal.fire({ icon:'warning', title:'Kilometraje no válido' })
  saving.value = true
  try { await vehicleApi.register(props.client.id, { ...form, year:Number(form.year), mileage: form.mileage === '' || form.mileage === null ? null : Number(form.mileage) }); await Swal.fire({ icon:'success', title:'Vehículo asociado', text:`Se registró para ${props.client.fullName}.`, confirmButtonColor:'#0f766e' }); emit('saved') }
  catch (error) { await Swal.fire({ icon:'error', title:'No fue posible registrar', text:error.message, confirmButtonColor:'#0f766e' }) }
  finally { saving.value = false }
}
onMounted(async () => { try { brands.value = await vehicleCatalogApi.brands() } catch (error) { await Swal.fire({icon:'error', title:'No fue posible cargar marcas', text:error.message}); emit('close') } })
</script>

<template>
  <Dialog :visible="true" modal header="Asociar vehículo" :style="{width:'min(96vw, 720px)'}" @update:visible="value => { if (!value) $emit('close') }">
    <div class="rounded-xl bg-slate-50 p-4 mb-5 text-sm"><p class="font-semibold">{{ client.fullName }}</p><p class="text-slate-500">ID: {{ client.id }} · {{ client.email }}</p></div>
    <form @submit.prevent="save" class="grid sm:grid-cols-2 gap-4">
      <label class="field">VIN<input v-model.trim="form.vin" required maxlength="17" @input="normalizeUpper('vin')" placeholder="17 caracteres" /></label>
      <label class="field">Placa<input v-model.trim="form.plate" required maxlength="10" @input="normalizeUpper('plate')" placeholder="ABC-123-A" /></label>
      <template v-if="!manualCatalog"><label class="field">Marca<select v-model="form.brandId" required @change="loadModels"><option :value="null" disabled>Selecciona una marca</option><option v-for="brand in brands" :key="brand.id" :value="brand.id">{{ brand.name }}</option></select></label>
      <label class="field">Modelo<select v-model="form.modelId" required :disabled="!form.brandId"><option :value="null" disabled>Selecciona un modelo</option><option v-for="model in models" :key="model.id" :value="model.id">{{ model.name }}</option></select></label></template>
      <template v-else><label class="field">Marca no catalogada<input v-model.trim="form.manualBrand" required maxlength="100" /></label>
      <label class="field">Modelo no catalogado<input v-model.trim="form.manualModel" required maxlength="100" /></label></template>
      <label class="sm:col-span-2 flex gap-2 items-center text-sm text-slate-600"><input v-model="manualCatalog" type="checkbox" @change="changeCatalogMode" /> No encuentro la marca o el modelo: capturarlo manualmente para revisión.</label>
      <label class="field">Año<input v-model.number="form.year" required type="number" min="1886" :max="new Date().getFullYear() + 1" /></label>
      <label class="field">Versión<input v-model.trim="form.version" required maxlength="100" /></label>
      <label class="field">Kilometraje <span class="text-slate-400 font-normal">opcional</span><input v-model.number="form.mileage" type="number" min="0" /></label>
      <label class="field">Número de serie<input v-model.trim="form.serialNumber" required maxlength="40" @input="normalizeUpper('serialNumber')" /></label>
      <div class="sm:col-span-2 flex justify-end gap-3"><button type="button" @click="$emit('close')" class="px-4 py-3 font-semibold text-slate-600">Cancelar</button><button :disabled="saving" class="px-5 py-3 rounded-lg bg-teal-700 text-white font-semibold">{{ saving ? 'Registrando…' : 'Asociar vehículo' }}</button></div>
    </form>
  </Dialog>
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }
.field input,.field select { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; background:#fff; }
</style>
