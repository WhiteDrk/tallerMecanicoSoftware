<script setup>
import { onMounted, reactive, ref } from 'vue'
import Swal from 'sweetalert2'
import { addressCatalogApi } from '../services/addressCatalogApi'
import { deactivateWorkshop, getWorkshop, listWorkshops, managers, registerWorkshop, updateWorkshop } from '../services/workshopApi'

const form = reactive({ name:'', legalName:'', phone:'', managerId:null, rfc:'', email:'', street:'', neighborhood:'', municipality:'', state:'', postalCode:'' })
const photo = ref(null)
const saving = ref(false)
const mode = ref('list')
const editingId = ref(null)
const workshops = ref([])
const addressMode = ref('postal-code')
const managerOptions = ref([])
const states = ref([])
const municipalities = ref([])
const localities = ref([])
const selectedStateId = ref(null)
const selectedMunicipalityId = ref(null)
const selectedLocalityId = ref(null)
const message = ref('')

function resetDependentAddress() {
  form.state = ''; form.municipality = ''; form.neighborhood = ''
  selectedStateId.value = null; selectedMunicipalityId.value = null; selectedLocalityId.value = null
  municipalities.value = []; localities.value = []
}
function changeAddressMode(mode) { addressMode.value = mode; message.value = ''; form.postalCode = ''; resetDependentAddress() }
async function load() {
  try { [states.value, managerOptions.value, workshops.value] = await Promise.all([addressCatalogApi.states(), managers(), listWorkshops()]) }
  catch (error) { message.value = error.message }
}
async function loadWorkshops() { try { workshops.value = await listWorkshops() } catch (error) { message.value = error.message } }
function resetForm() {
  Object.keys(form).forEach(key => form[key] = key === 'managerId' ? null : '')
  photo.value = null; editingId.value = null; resetDependentAddress()
  const input = document.getElementById('workshop-photo'); if (input) input.value = ''
}
function startCreate() { resetForm(); mode.value = 'create'; message.value = '' }
async function startEdit(id) {
  try {
    const workshop = await getWorkshop(id)
    Object.assign(form, { name:workshop.name, legalName:workshop.legalName, phone:workshop.phone, managerId:workshop.managerId, rfc:workshop.rfc, email:workshop.email, street:workshop.street, neighborhood:workshop.neighborhood, municipality:workshop.municipality, state:workshop.state, postalCode:workshop.postalCode })
    editingId.value = id; addressMode.value = 'postal-code'; mode.value = 'edit'; await onPostalCodeInput()
  } catch (error) { await Swal.fire({ icon:'error', title:'No fue posible abrir el taller', text:error.message, confirmButtonColor:'#0f766e' }) }
}
async function deactivate(id, name) {
  const answer = await Swal.fire({ icon:'warning', title:'¿Desactivar taller?', text:`${name} dejará de estar disponible, pero su historial se conservará.`, showCancelButton:true, confirmButtonText:'Desactivar', cancelButtonText:'Cancelar', confirmButtonColor:'#be123c' })
  if (!answer.isConfirmed) return
  try { await deactivateWorkshop(id); await loadWorkshops(); await Swal.fire({ icon:'success', title:'Taller desactivado', confirmButtonColor:'#0f766e' }) }
  catch (error) { await Swal.fire({ icon:'error', title:'No fue posible desactivar', text:error.message, confirmButtonColor:'#0f766e' }) }
}
async function onPostalCodeInput() {
  message.value = ''; resetDependentAddress()
  if (!form.postalCode) return
  if (!/^\d{5}$/.test(form.postalCode)) { message.value = 'El código postal debe tener cinco dígitos.'; return }
  try {
    const result = await addressCatalogApi.postalCode(form.postalCode)
    form.state = result.state.name; form.municipality = result.municipality.name; localities.value = result.localities
    if (!localities.value.length) message.value = 'El código postal existe, pero no tiene colonias o comunidades disponibles.'
  } catch (error) { message.value = error.message }
}
async function onStateChange() {
  message.value = ''; form.municipality = ''; form.neighborhood = ''; form.postalCode = ''
  selectedMunicipalityId.value = null; selectedLocalityId.value = null; localities.value = []
  form.state = states.value.find(item => item.id === selectedStateId.value)?.name || ''
  if (!selectedStateId.value) { municipalities.value = []; return }
  try { municipalities.value = await addressCatalogApi.municipalities(selectedStateId.value) } catch (error) { message.value = error.message }
}
async function onMunicipalityChange() {
  message.value = ''; form.neighborhood = ''; form.postalCode = ''; selectedLocalityId.value = null
  form.municipality = municipalities.value.find(item => item.id === selectedMunicipalityId.value)?.name || ''
  if (!selectedMunicipalityId.value) { localities.value = []; return }
  try { localities.value = await addressCatalogApi.localities(selectedMunicipalityId.value) } catch (error) { message.value = error.message }
}
async function onLocalityChange() {
  message.value = ''; form.neighborhood = ''; form.postalCode = ''
  if (!selectedLocalityId.value) return
  try { const result = await addressCatalogApi.locality(selectedLocalityId.value); form.neighborhood = result.name; form.postalCode = result.postalCode } catch (error) { message.value = error.message }
}
function choosePhoto(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 15 * 1024 * 1024) {
    event.target.value = ''; photo.value = null
    Swal.fire({ icon:'error', title:'Fotografía no válida', text:'Selecciona una imagen JPEG o PNG que no supere 15 MB.' })
    return
  }
  photo.value = file
}
async function submit() {
  saving.value = true
  try {
    const payload = { ...form, rfc: form.rfc.toUpperCase() }
    const result = editingId.value ? await updateWorkshop(editingId.value, payload, photo.value) : await registerWorkshop(payload, photo.value)
    await Swal.fire({ icon:'success', title: editingId.value ? 'Taller actualizado' : 'Taller registrado', text:`${result.name} fue guardado exitosamente.`, confirmButtonColor:'#0f766e' })
    resetForm(); mode.value = 'list'; await loadWorkshops()
  } catch (error) { await Swal.fire({ icon:'error', title:'No se guardó el taller', text:error.message, confirmButtonColor:'#0f766e' }) }
  finally { saving.value = false }
}
onMounted(load)
</script>

<template>
  <section class="max-w-5xl mx-auto">
    <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-3"><div><p class="text-teal-700 font-semibold text-sm">UC-CV-02 · ADMINISTRACIÓN</p><h2 class="text-2xl font-semibold mt-1">Administración de talleres</h2><p class="text-slate-500 mt-1">Consulta, actualiza o desactiva los talleres autorizados.</p></div><button v-if="mode === 'list'" @click="startCreate" class="bg-teal-700 text-white font-semibold rounded-lg px-4 py-3"><i class="pi pi-plus mr-2"></i>Nuevo taller</button></div>
    <section v-if="mode === 'list'" class="mt-6 bg-white border border-slate-100 rounded-2xl overflow-hidden"><div class="overflow-x-auto"><table class="w-full text-sm min-w-[760px]"><thead class="bg-slate-50 text-left text-xs text-slate-500"><tr><th class="p-4">TALLER</th><th class="p-4">RFC</th><th class="p-4">GERENTE</th><th class="p-4">ESTATUS</th><th class="p-4"></th></tr></thead><tbody><tr v-for="workshop in workshops" :key="workshop.id" class="border-t border-slate-100"><td class="p-4"><b>{{ workshop.name }}</b><p class="text-slate-500 mt-1">{{ workshop.email }}</p></td><td class="p-4">{{ workshop.rfc }}</td><td class="p-4">{{ workshop.managerName }}</td><td class="p-4"><span :class="workshop.active ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-700'" class="rounded-full px-3 py-1 text-xs font-semibold">{{ workshop.active ? 'Activo' : 'Inactivo' }}</span></td><td class="p-4 text-right whitespace-nowrap"><button @click="startEdit(workshop.id)" :disabled="!workshop.active" class="text-teal-700 font-semibold text-xs disabled:text-slate-400">Editar</button><button v-if="workshop.active && workshop.canDeactivate" @click="deactivate(workshop.id, workshop.name)" class="text-rose-700 font-semibold text-xs ml-4">Desactivar</button></td></tr><tr v-if="!workshops.length"><td colspan="5" class="p-8 text-center text-slate-500">No hay talleres autorizados para esta cuenta.</td></tr></tbody></table></div></section>
    <form v-else @submit.prevent="submit" class="mt-6 bg-white border border-slate-100 rounded-2xl p-5 sm:p-7 space-y-7">
      <fieldset><legend class="font-semibold mb-4">Datos del taller</legend><div class="grid sm:grid-cols-2 gap-4"><label class="field">Nombre comercial<input v-model.trim="form.name" required maxlength="150" /></label><label class="field">Razón social<input v-model.trim="form.legalName" required maxlength="200" /></label><label class="field">RFC<input :value="form.rfc" required maxlength="13" pattern="[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}" class="uppercase" @input="form.rfc = $event.target.value.toUpperCase()" /></label><label class="field">Teléfono<input v-model.trim="form.phone" required inputmode="tel" pattern="[0-9+() .-]{7,20}" /></label><label class="field">Correo electrónico<input v-model.trim="form.email" required type="email" maxlength="254" /></label><label class="field">Gerente<select v-model="form.managerId" required><option :value="null" disabled>Selecciona un gerente</option><option v-for="manager in managerOptions" :key="manager.id" :value="manager.id">{{ manager.fullName }} · {{ manager.email }}</option></select></label><label class="field">Fotografía <span class="text-slate-400 font-normal">opcional, JPEG/PNG, máx. 15 MB</span><input id="workshop-photo" @change="choosePhoto" accept="image/jpeg,image/png" type="file" /></label></div></fieldset>
      <fieldset class="border-t border-slate-100 pt-6"><legend class="font-semibold mb-4">Dirección</legend><div class="mb-4 flex flex-wrap gap-4 text-sm"><label><input name="workshopAddressMode" type="radio" :checked="addressMode === 'postal-code'" @change="changeAddressMode('postal-code')" /> Conozco mi código postal</label><label><input name="workshopAddressMode" type="radio" :checked="addressMode === 'location'" @change="changeAddressMode('location')" /> No conozco mi código postal</label></div><div class="grid sm:grid-cols-2 gap-4"><label class="field sm:col-span-2">Calle<input v-model.trim="form.street" required maxlength="160" /></label><template v-if="addressMode === 'postal-code'"><label class="field">Código postal<input v-model.trim="form.postalCode" required inputmode="numeric" pattern="[0-9]{5}" maxlength="5" @input="onPostalCodeInput" /></label><label class="field">Estado<input :value="form.state" required readonly /></label><label class="field">Municipio<input :value="form.municipality" required readonly /></label></template><template v-else><label class="field">Estado<select v-model.number="selectedStateId" required @change="onStateChange"><option :value="null" disabled>Selecciona un estado</option><option v-for="item in states" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label class="field">Municipio<select v-model.number="selectedMunicipalityId" required :disabled="!selectedStateId" @change="onMunicipalityChange"><option :value="null" disabled>Selecciona un municipio</option><option v-for="item in municipalities" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label class="field">Código postal<input :value="form.postalCode" required readonly /></label></template><label class="field">Colonia o comunidad<select v-model.number="selectedLocalityId" required :disabled="!localities.length" @change="onLocalityChange"><option :value="null" disabled>Selecciona una colonia o comunidad</option><option v-for="item in localities" :key="item.id" :value="item.id">{{ item.name }}</option></select></label></div><p v-if="message" class="mt-3 text-sm text-rose-700" role="alert">{{ message }}</p></fieldset>
      <div class="flex justify-end gap-3 border-t border-slate-100 pt-5"><button type="button" @click="resetForm(); mode='list'" class="text-slate-600 font-semibold px-5 py-3">Cancelar</button><button :disabled="saving" class="bg-teal-700 disabled:bg-teal-300 text-white font-semibold rounded-lg px-5 py-3"><i class="pi pi-building mr-2"></i>{{ saving ? 'Guardando…' : (editingId ? 'Guardar cambios' : 'Registrar taller') }}</button></div>
    </form>
  </section>
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }
.field input, .field select { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; background:#fff; }
.field input:focus, .field select:focus { outline:2px solid #99f6e4; border-color:#0f766e; }
</style>
