<script setup>
import { onMounted, reactive, ref } from 'vue'
import Dialog from 'primevue/dialog'
import Swal from 'sweetalert2'
import { getClient, updateClient } from '../services/clientApi'
import { addressCatalogApi } from '../services/addressCatalogApi'
import { availableWorkshops } from '../services/workshopApi'

const props = defineProps({ clientId: { type: String, required: true } })
const emit = defineEmits(['saved', 'close'])
const form = reactive({ fullName:'', alternateContactName:'', birthDate:'', phone:'', workPhone:'', email:'', workEmail:'', street:'', neighborhood:'', municipality:'', state:'', postalCode:'', workshopId:null })
const workshops = ref([])
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

function resetDependentAddress() {
  form.state = ''; form.municipality = ''; form.neighborhood = ''
  selectedStateId.value = null; selectedMunicipalityId.value = null; selectedLocalityId.value = null
  municipalities.value = []; localities.value = []
}
function changeAddressMode(mode) { addressMode.value = mode; addressMessage.value = ''; form.postalCode = ''; resetDependentAddress() }
async function onPostalCodeInput() {
  addressMessage.value = ''; const currentNeighborhood = form.neighborhood; resetDependentAddress()
  if (!form.postalCode) return
  if (!/^\d{5}$/.test(form.postalCode)) { addressMessage.value = 'El código postal debe tener cinco dígitos.'; return }
  try {
    const result = await addressCatalogApi.postalCode(form.postalCode)
    form.state = result.state.name; form.municipality = result.municipality.name; localities.value = result.localities
    const current = localities.value.find(item => item.name === currentNeighborhood)
    if (current) { selectedLocalityId.value = current.id; form.neighborhood = current.name }
    else if (!localities.value.length) addressMessage.value = 'El código postal existe, pero no tiene colonias o comunidades disponibles.'
  } catch (error) { addressMessage.value = error.message }
}
async function onStateChange() {
  addressMessage.value = ''; form.municipality = ''; form.neighborhood = ''; form.postalCode = ''
  selectedMunicipalityId.value = null; selectedLocalityId.value = null; localities.value = []
  const state = states.value.find(item => item.id === selectedStateId.value); form.state = state?.name || ''
  if (!selectedStateId.value) { municipalities.value = []; return }
  try { municipalities.value = await addressCatalogApi.municipalities(selectedStateId.value) } catch (error) { addressMessage.value = error.message }
}
async function onMunicipalityChange() {
  addressMessage.value = ''; form.neighborhood = ''; form.postalCode = ''; selectedLocalityId.value = null
  const municipality = municipalities.value.find(item => item.id === selectedMunicipalityId.value); form.municipality = municipality?.name || ''
  if (!selectedMunicipalityId.value) { localities.value = []; return }
  try { localities.value = await addressCatalogApi.localities(selectedMunicipalityId.value) } catch (error) { addressMessage.value = error.message }
}
async function onLocalityChange() {
  addressMessage.value = ''; form.neighborhood = ''; form.postalCode = ''
  if (!selectedLocalityId.value) return
  try { const result = await addressCatalogApi.locality(selectedLocalityId.value); form.neighborhood = result.name; form.postalCode = result.postalCode } catch (error) { addressMessage.value = error.message }
}
function choosePhoto(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!['image/jpeg','image/png'].includes(file.type) || file.size > 7 * 1024 * 1024) {
    event.target.value=''; photo.value=null
    Swal.fire({icon:'error',title:'Fotografía no válida',text:'Selecciona JPEG o PNG de máximo 7 MB.'})
    return
  }
  photo.value=file
}
async function load() {
  try {
    const [client, available, catalogStates] = await Promise.all([getClient(props.clientId), availableWorkshops(), addressCatalogApi.states()])
    Object.assign(form, client); workshops.value = available; states.value = catalogStates
    await onPostalCodeInput()
  } catch (error) { await Swal.fire({icon:'error',title:'No fue posible abrir el cliente',text:error.message}); emit('close') }
}
async function save() {
  saving.value=true
  try { await updateClient(props.clientId, form, photo.value); await Swal.fire({icon:'success',title:'Cliente actualizado',confirmButtonColor:'#0f766e'}); emit('saved') }
  catch (error) { await Swal.fire({icon:'error',title:'No se actualizó el cliente',text:error.message,confirmButtonColor:'#0f766e'}) }
  finally { saving.value=false }
}
onMounted(load)
</script>

<template>
  <Dialog :visible="true" modal header="Editar cliente" :style="{width:'min(96vw, 900px)'}" @update:visible="value => { if (!value) $emit('close') }">
    <p class="text-slate-500 text-sm mb-5">Se conservan las validaciones de registro y el acceso se verifica en el servidor.</p>
    <form @submit.prevent="save" class="grid sm:grid-cols-2 gap-4">
      <label class="field">Nombre completo<input v-model.trim="form.fullName" required maxlength="150" /></label>
      <label class="field">Contacto alternativo<input v-model.trim="form.alternateContactName" required maxlength="150" /></label>
      <label class="field">Fecha de nacimiento<input v-model="form.birthDate" required type="date" /></label>
      <label class="field">Taller<select v-model="form.workshopId" required><option v-for="workshop in workshops" :key="workshop.id" :value="workshop.id">{{ workshop.name }}</option></select></label>
      <label class="field">Teléfono<input v-model.trim="form.phone" required inputmode="tel" pattern="[0-9+() .-]{7,20}" maxlength="20" /></label>
      <label class="field">Teléfono de trabajo<input v-model.trim="form.workPhone" required inputmode="tel" pattern="[0-9+() .-]{7,20}" maxlength="20" /></label>
      <label class="field">Correo<input v-model.trim="form.email" required type="email" maxlength="254" /></label>
      <label class="field">Correo trabajo<input v-model.trim="form.workEmail" type="email" maxlength="254" /></label>
      <label class="field">Fotografía nueva <span class="text-slate-400 font-normal">opcional · JPEG/PNG, 7 MB</span><input @change="choosePhoto" accept="image/jpeg,image/png" type="file" /></label>
      <fieldset class="sm:col-span-2 border-t border-slate-100 pt-5"><legend class="font-semibold mb-3">Dirección</legend><div class="mb-4 flex flex-wrap gap-4 text-sm"><label><input name="editorAddressMode" type="radio" :checked="addressMode === 'postal-code'" @change="changeAddressMode('postal-code')" /> Conozco mi código postal</label><label><input name="editorAddressMode" type="radio" :checked="addressMode === 'location'" @change="changeAddressMode('location')" /> No conozco mi código postal</label></div><div class="grid sm:grid-cols-2 gap-4"><label class="field sm:col-span-2">Calle<input v-model.trim="form.street" required maxlength="160" /></label><template v-if="addressMode === 'postal-code'"><label class="field">Código postal<input v-model.trim="form.postalCode" required inputmode="numeric" pattern="[0-9]{5}" maxlength="5" @input="onPostalCodeInput" /></label><label class="field">Estado<input :value="form.state" required readonly /></label><label class="field">Municipio<input :value="form.municipality" required readonly /></label></template><template v-else><label class="field">Estado<select v-model.number="selectedStateId" required @change="onStateChange"><option :value="null" disabled>Selecciona un estado</option><option v-for="item in states" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label class="field">Municipio<select v-model.number="selectedMunicipalityId" required :disabled="!selectedStateId" @change="onMunicipalityChange"><option :value="null" disabled>Selecciona un municipio</option><option v-for="item in municipalities" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label class="field">Código postal<input :value="form.postalCode" required readonly /></label></template><label class="field">Colonia o comunidad<select v-model.number="selectedLocalityId" required :disabled="!localities.length" @change="onLocalityChange"><option :value="null" disabled>Selecciona una colonia o comunidad</option><option v-for="item in localities" :key="item.id" :value="item.id">{{ item.name }}</option></select></label></div><p v-if="addressMessage" class="mt-3 text-sm text-rose-700" role="alert">{{ addressMessage }}</p></fieldset>
      <div class="sm:col-span-2 flex justify-end gap-3"><button type="button" @click="$emit('close')" class="px-4 py-3 font-semibold text-slate-600">Cancelar</button><button :disabled="saving" class="px-5 py-3 rounded-lg bg-teal-700 text-white font-semibold">{{ saving ? 'Guardando…' : 'Guardar cambios' }}</button></div>
    </form>
  </Dialog>
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }
.field input,.field select { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; background:#fff; }
</style>
