<script setup>
import { onMounted, reactive, ref } from 'vue'
import Swal from 'sweetalert2'
import { getClient, updateClient } from '../services/clientApi'
import { availableWorkshops } from '../services/workshopApi'

const props = defineProps({ clientId: { type: String, required: true } })
const emit = defineEmits(['saved', 'close'])
const form = reactive({ fullName:'', alternateContactName:'', birthDate:'', phone:'', workPhone:'', email:'', workEmail:'', street:'', neighborhood:'', municipality:'', state:'', postalCode:'', workshopId:null })
const workshops = ref([])
const photo = ref(null)
const saving = ref(false)
async function load() { try { const [client, available] = await Promise.all([getClient(props.clientId), availableWorkshops()]); Object.assign(form, client); workshops.value = available } catch (error) { await Swal.fire({icon:'error',title:'No fue posible abrir el cliente',text:error.message}); emit('close') } }
function choosePhoto(event) { const file = event.target.files?.[0]; if (!file) return; if (!['image/jpeg','image/png'].includes(file.type) || file.size > 7 * 1024 * 1024) { event.target.value=''; photo.value=null; Swal.fire({icon:'error',title:'Fotografía no válida',text:'Selecciona JPEG o PNG de máximo 7 MB.'}); return } photo.value=file }
async function save() { saving.value=true; try { await updateClient(props.clientId, form, photo.value); await Swal.fire({icon:'success',title:'Cliente actualizado',confirmButtonColor:'#0f766e'}); emit('saved') } catch (error) { await Swal.fire({icon:'error',title:'No se actualizó el cliente',text:error.message,confirmButtonColor:'#0f766e'}) } finally { saving.value=false } }
onMounted(load)
</script>

<template>
  <section class="mt-6 bg-white border border-slate-100 rounded-2xl p-5 sm:p-7"><div class="flex justify-between gap-3"><div><h2 class="text-xl font-semibold">Editar cliente</h2><p class="text-slate-500 text-sm">El cambio de taller se valida con el alcance del usuario.</p></div><button @click="$emit('close')" class="text-slate-500">Cerrar</button></div><form @submit.prevent="save" class="mt-5 grid sm:grid-cols-2 gap-4"><label class="field">Nombre<input v-model.trim="form.fullName" required maxlength="150" /></label><label class="field">Contacto alternativo<input v-model.trim="form.alternateContactName" required maxlength="150" /></label><label class="field">Fecha de nacimiento<input v-model="form.birthDate" required type="date" /></label><label class="field">Taller<select v-model="form.workshopId" required><option v-for="workshop in workshops" :key="workshop.id" :value="workshop.id">{{ workshop.name }}</option></select></label><label class="field">Teléfono<input v-model.trim="form.phone" required pattern="[0-9+() .-]{7,20}" /></label><label class="field">Teléfono trabajo<input v-model.trim="form.workPhone" required pattern="[0-9+() .-]{7,20}" /></label><label class="field">Correo<input v-model.trim="form.email" required type="email" /></label><label class="field">Correo trabajo<input v-model.trim="form.workEmail" type="email" /></label><label class="field">Calle<input v-model.trim="form.street" required maxlength="160" /></label><label class="field">Colonia<input v-model.trim="form.neighborhood" required maxlength="120" /></label><label class="field">Municipio<input v-model.trim="form.municipality" required maxlength="120" /></label><label class="field">Estado<input v-model.trim="form.state" required maxlength="120" /></label><label class="field">Código postal<input v-model.trim="form.postalCode" required pattern="[0-9]{5}" maxlength="5" /></label><label class="field">Fotografía nueva <span class="text-slate-400 font-normal">opcional</span><input @change="choosePhoto" accept="image/jpeg,image/png" type="file" /></label><div class="sm:col-span-2 flex justify-end gap-3"><button type="button" @click="$emit('close')" class="px-4 py-3 font-semibold text-slate-600">Cancelar</button><button :disabled="saving" class="px-5 py-3 rounded-lg bg-teal-700 text-white font-semibold">{{ saving ? 'Guardando…' : 'Guardar cambios' }}</button></div></form></section>
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }.field input,.field select { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; background:#fff; }
</style>
