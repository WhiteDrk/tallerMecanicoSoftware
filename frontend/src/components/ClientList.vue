<script setup>
import { onMounted, ref, watch } from 'vue'
import Swal from 'sweetalert2'
import { listClients, suspendClient } from '../services/clientApi'
import { availableWorkshops } from '../services/workshopApi'
import ClientEditor from './ClientEditor.vue'

const workshops = ref([])
const workshopId = ref(null)
const clients = ref([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const sort = ref('fullName')
const direction = ref('asc')
const loading = ref(false)
const editingId = ref(null)

async function loadWorkshops() {
  try { workshops.value = await availableWorkshops(); if (workshops.value.length) workshopId.value = workshops.value[0].id }
  catch (error) { await Swal.fire({ icon:'error', title:'No fue posible cargar talleres', text:error.message, confirmButtonColor:'#0f766e' }) }
}
async function loadClients() {
  if (!workshopId.value) { clients.value = []; return }
  loading.value = true
  try {
    const result = await listClients({ workshopId: workshopId.value, page: page.value, size: 10, sort: sort.value, direction: direction.value })
    clients.value = result.content; totalPages.value = result.totalPages; totalElements.value = result.totalElements
  } catch (error) { await Swal.fire({ icon:'error', title:'No fue posible cargar clientes', text:error.message, confirmButtonColor:'#0f766e' }) }
  finally { loading.value = false }
}
function changeWorkshop() { page.value = 0; loadClients() }
function changeSort() { page.value = 0; loadClients() }
function previous() { if (page.value > 0) { page.value--; loadClients() } }
function next() { if (page.value + 1 < totalPages.value) { page.value++; loadClients() } }
async function suspend(id, name) {
  const answer = await Swal.fire({ icon:'warning', title:'¿Suspender cliente?', text:`${name} conservará su registro pero quedará suspendido.`, showCancelButton:true, confirmButtonText:'Suspender', cancelButtonText:'Cancelar', confirmButtonColor:'#be123c' })
  if (!answer.isConfirmed) return
  try { await suspendClient(id); await loadClients(); await Swal.fire({icon:'success',title:'Cliente suspendido',confirmButtonColor:'#0f766e'}) }
  catch (error) { await Swal.fire({icon:'error',title:'No fue posible suspender',text:error.message,confirmButtonColor:'#0f766e'}) }
}
watch(workshopId, () => { if (workshopId.value) loadClients() })
onMounted(loadWorkshops)
</script>

<template>
  <section class="max-w-5xl mx-auto mb-8 bg-white border border-slate-100 rounded-2xl overflow-hidden">
    <div class="p-5 sm:p-7 flex flex-col gap-4"><div><p class="text-teal-700 font-semibold text-sm">UC-CV-02 · CLIENTES</p><h2 class="text-2xl font-semibold mt-1">Clientes por taller</h2><p class="text-slate-500 mt-1">Sólo se muestran clientes del taller autorizado.</p></div><div class="grid sm:grid-cols-3 gap-3"><label class="field">Taller<select v-model="workshopId" @change="changeWorkshop"><option v-for="workshop in workshops" :key="workshop.id" :value="workshop.id">{{ workshop.name }}</option></select></label><label class="field">Ordenar por<select v-model="sort" @change="changeSort"><option value="fullName">Nombre</option><option value="email">Correo</option><option value="status">Estatus</option></select></label><label class="field">Dirección<select v-model="direction" @change="changeSort"><option value="asc">Ascendente</option><option value="desc">Descendente</option></select></label></div></div>
    <div class="overflow-x-auto"><table class="w-full text-sm min-w-[680px]"><thead class="bg-slate-50 text-left text-xs text-slate-500"><tr><th class="p-4">CLIENTE</th><th class="p-4">CORREO</th><th class="p-4">ESTATUS</th><th class="p-4"></th></tr></thead><tbody><tr v-for="client in clients" :key="client.id" class="border-t border-slate-100"><td class="p-4 font-medium">{{ client.fullName }}</td><td class="p-4 text-slate-600">{{ client.email }}</td><td class="p-4"><span :class="client.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'" class="rounded-full px-3 py-1 text-xs font-semibold">{{ client.status === 'ACTIVE' ? 'Activo' : 'Suspendido' }}</span></td><td class="p-4 text-right whitespace-nowrap"><button @click="editingId=client.id" class="text-teal-700 font-semibold text-xs">Editar</button><button v-if="client.status === 'ACTIVE'" @click="suspend(client.id, client.fullName)" class="text-rose-700 font-semibold text-xs ml-4">Suspender</button></td></tr><tr v-if="!clients.length && !loading"><td colspan="4" class="p-8 text-center text-slate-500">No hay clientes para este taller.</td></tr></tbody></table></div>
    <div class="p-4 border-t border-slate-100 flex items-center justify-between text-sm"><span class="text-slate-500">{{ totalElements }} clientes · máximo 10 por página</span><div class="flex gap-2"><button @click="previous" :disabled="page === 0" class="px-3 py-2 rounded-lg bg-slate-100 disabled:text-slate-400">Anterior</button><span class="px-3 py-2">{{ totalPages ? page + 1 : 0 }} / {{ totalPages }}</span><button @click="next" :disabled="page + 1 >= totalPages" class="px-3 py-2 rounded-lg bg-slate-100 disabled:text-slate-400">Siguiente</button></div></div>
  </section>
  <ClientEditor v-if="editingId" :client-id="editingId" @close="editingId=null" @saved="editingId=null; loadClients()" />
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }
.field select { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; background:#fff; }
</style>
