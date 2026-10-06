<script setup>
import { onMounted, reactive, ref } from 'vue'
import Swal from 'sweetalert2'
import { registerClient } from '../services/clientApi'
import { addressCatalogApi } from '../services/addressCatalogApi'
import { availableWorkshops } from '../services/workshopApi'

const form = reactive({ fullName:'', alternateContactName:'', birthDate:'', phone:'', workPhone:'', email:'', workEmail:'', street:'', neighborhood:'', municipality:'', state:'', postalCode:'', workshopId:null })
const photo = ref(null)
const saving = ref(false)
const addressMode = ref('postal-code')
const states = ref([])
const municipalities = ref([])
const localities = ref([])
const selectedStateId = ref(null)
const selectedMunicipalityId = ref(null)
const selectedLocalityId = ref(null)
const addressMessage = ref('')
const workshops = ref([])

function resetDependentAddress() {
  form.state = ''; form.municipality = ''; form.neighborhood = ''
  selectedStateId.value = null; selectedMunicipalityId.value = null; selectedLocalityId.value = null
  municipalities.value = []; localities.value = []
}
function changeAddressMode(mode) {
  addressMode.value = mode; addressMessage.value = ''; form.postalCode = ''; resetDependentAddress()
}
async function loadStates() {
  try { [states.value, workshops.value] = await Promise.all([addressCatalogApi.states(), availableWorkshops()]) }
  catch (error) { addressMessage.value = error.message }
}
async function onPostalCodeInput() {
  addressMessage.value = ''; resetDependentAddress()
  if (!form.postalCode) return
  if (!/^\d{5}$/.test(form.postalCode)) { addressMessage.value = 'El código postal debe tener cinco dígitos.'; return }
  try {
    const result = await addressCatalogApi.postalCode(form.postalCode)
    form.state = result.state.name; form.municipality = result.municipality.name; localities.value = result.localities
    if (!localities.value.length) addressMessage.value = 'El código postal existe, pero no tiene colonias o comunidades disponibles.'
  } catch (error) { addressMessage.value = error.message }
}
async function onStateChange() {
  addressMessage.value = ''; form.municipality = ''; form.neighborhood = ''; form.postalCode = ''
  selectedMunicipalityId.value = null; selectedLocalityId.value = null; localities.value = []
  const state = states.value.find(item => item.id === selectedStateId.value); form.state = state?.name || ''
  if (!selectedStateId.value) { municipalities.value = []; return }
  try { municipalities.value = await addressCatalogApi.municipalities(selectedStateId.value) }
  catch (error) { addressMessage.value = error.message }
}
async function onMunicipalityChange() {
  addressMessage.value = ''; form.neighborhood = ''; form.postalCode = ''; selectedLocalityId.value = null
  const municipality = municipalities.value.find(item => item.id === selectedMunicipalityId.value); form.municipality = municipality?.name || ''
  if (!selectedMunicipalityId.value) { localities.value = []; return }
  try { localities.value = await addressCatalogApi.localities(selectedMunicipalityId.value) }
  catch (error) { addressMessage.value = error.message }
}
async function onLocalityChange() {
  addressMessage.value = ''; form.neighborhood = ''; form.postalCode = ''
  if (!selectedLocalityId.value) return
  try {
    const result = await addressCatalogApi.locality(selectedLocalityId.value)
    form.neighborhood = result.name; form.postalCode = result.postalCode
  } catch (error) { addressMessage.value = error.message }
}
onMounted(loadStates)
function choosePhoto(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 7 * 1024 * 1024) {
    event.target.value = ''; photo.value = null
    Swal.fire({ icon:'error', title:'Fotografía no válida', text:'Selecciona una imagen JPEG o PNG que no supere 7 MB.' })
    return
  }
  photo.value = file
}
async function submit() {
  saving.value = true
  try {
    const result = await registerClient(form, photo.value)
    await Swal.fire({ icon:'success', title:'Cliente registrado', text:`${result.fullName} fue registrado exitosamente.`, confirmButtonColor:'#0f766e' })
    Object.keys(form).forEach(key => form[key] = key === 'workshopId' ? null : ''); photo.value = null
    const input = document.getElementById('client-photo'); if (input) input.value = ''
  } catch (error) {
    await Swal.fire({ icon:'error', title:'No se registró el cliente', text:error.message, confirmButtonColor:'#0f766e' })
  } finally { saving.value = false }
}
</script>

<template>
  <section class="max-w-5xl mx-auto">
    <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-3"><div><p class="text-teal-700 font-semibold text-sm">FASE 02 · RECEPCIÓN</p><h2 class="text-2xl font-semibold mt-1">Registro de cliente</h2><p class="text-slate-500 mt-1">Los campos marcados son obligatorios. Se evita cualquier registro duplicado.</p></div><span class="text-xs bg-teal-100 text-teal-800 rounded-full px-3 py-2">Autorizado: Recepción, Gerencia y Administración</span></div>
    <form @submit.prevent="submit" class="mt-6 bg-white border border-slate-100 rounded-2xl p-5 sm:p-7 space-y-7">
      <fieldset><legend class="font-semibold mb-4">Datos principales</legend><div class="grid sm:grid-cols-2 gap-4"><label class="field">Taller asignado<select v-model="form.workshopId" required><option :value="null" disabled>Selecciona un taller</option><option v-for="workshop in workshops" :key="workshop.id" :value="workshop.id">{{ workshop.name }}</option></select></label><label class="field">Nombre completo<input v-model.trim="form.fullName" required maxlength="150" /></label><label class="field">Contacto alternativo<input v-model.trim="form.alternateContactName" required maxlength="150" /></label><label class="field">Fecha de nacimiento<input v-model="form.birthDate" required type="date" /></label><label class="field">Teléfono<input v-model.trim="form.phone" required inputmode="tel" pattern="[0-9+() .-]{7,20}" /></label><label class="field">Teléfono de trabajo<input v-model.trim="form.workPhone" required inputmode="tel" pattern="[0-9+() .-]{7,20}" /></label><label class="field">Correo electrónico<input v-model.trim="form.email" required type="email" maxlength="254" /></label><label class="field">Correo de trabajo <span class="text-slate-400 font-normal">opcional</span><input v-model.trim="form.workEmail" type="email" maxlength="254" /></label><label class="field">Fotografía <span class="text-slate-400 font-normal">JPEG o PNG, máx. 7 MB</span><input id="client-photo" @change="choosePhoto" accept="image/jpeg,image/png" type="file" /></label></div></fieldset>
      <fieldset class="border-t border-slate-100 pt-6"><legend class="font-semibold mb-4">Dirección</legend><div class="mb-4 flex flex-wrap gap-4 text-sm"><label><input name="addressMode" type="radio" :checked="addressMode === 'postal-code'" @change="changeAddressMode('postal-code')" /> Conozco mi código postal</label><label><input name="addressMode" type="radio" :checked="addressMode === 'location'" @change="changeAddressMode('location')" /> No conozco mi código postal</label></div><div class="grid sm:grid-cols-2 gap-4"><label class="field sm:col-span-2">Calle<input v-model.trim="form.street" required maxlength="160" /></label><template v-if="addressMode === 'postal-code'"><label class="field">Código postal<input v-model.trim="form.postalCode" required inputmode="numeric" pattern="[0-9]{5}" maxlength="5" @input="onPostalCodeInput" /></label><label class="field">Estado<input :value="form.state" required readonly /></label><label class="field">Municipio<input :value="form.municipality" required readonly /></label></template><template v-else><label class="field">Estado<select v-model.number="selectedStateId" required @change="onStateChange"><option :value="null" disabled>Selecciona un estado</option><option v-for="item in states" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label class="field">Municipio<select v-model.number="selectedMunicipalityId" required :disabled="!selectedStateId" @change="onMunicipalityChange"><option :value="null" disabled>Selecciona un municipio</option><option v-for="item in municipalities" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label class="field">Código postal<input :value="form.postalCode" required readonly /></label></template><label class="field">Colonia o comunidad<select v-model.number="selectedLocalityId" required :disabled="!localities.length" @change="onLocalityChange"><option :value="null" disabled>Selecciona una colonia o comunidad</option><option v-for="item in localities" :key="item.id" :value="item.id">{{ item.name }}</option></select></label></div><p v-if="addressMessage" class="mt-3 text-sm text-rose-700" role="alert">{{ addressMessage }}</p></fieldset>
      <div class="flex justify-end border-t border-slate-100 pt-5"><button :disabled="saving" class="bg-teal-700 disabled:bg-teal-300 text-white font-semibold rounded-lg px-5 py-3"><i class="pi pi-user-plus mr-2"></i>{{ saving ? 'Registrando…' : 'Registrar cliente' }}</button></div>
    </form>
  </section>
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }
.field input, .field select { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; background:#fff; }
.field input:focus, .field select:focus { outline:2px solid #99f6e4; border-color:#0f766e; }
</style>
