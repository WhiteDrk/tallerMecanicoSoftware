<script setup>
import { onMounted, reactive, ref } from 'vue'
import Swal from 'sweetalert2'
import { statusCatalogApi } from '../services/statusCatalogApi'

const statuses = ref([])
const loading = ref(false)
const loadError = ref('')
const editing = ref(null)
const form = reactive({ value: '', description: '', enabled: true })

async function load() {
  loading.value = true
  loadError.value = ''
  try { statuses.value = await statusCatalogApi.list() }
  catch (error) { loadError.value = error.message; await Swal.fire({ icon:'error', title:'No fue posible cargar los estatus', text:error.message, confirmButtonColor:'#0f766e' }) }
  finally { loading.value = false }
}
function create() { editing.value = null; form.value = ''; form.description = ''; form.enabled = true }
function edit(status) { editing.value = status; form.value = status.value; form.description = status.description; form.enabled = status.enabled }
async function save() {
  if (!form.description.trim() || (!editing.value && !form.value.trim())) return
  try {
    if (editing.value) await statusCatalogApi.update(editing.value.id, { description: form.description, enabled: form.enabled })
    else await statusCatalogApi.create({ value: form.value, description: form.description })
    await load(); create()
    await Swal.fire({ icon:'success', title:'Catálogo actualizado', confirmButtonColor:'#0f766e' })
  } catch (error) { await Swal.fire({ icon:'error', title:'No fue posible guardar', text:error.message, confirmButtonColor:'#0f766e' }) }
}
onMounted(load)
</script>

<template>
  <section class="max-w-5xl mx-auto space-y-6">
    <div class="bg-white border border-slate-100 rounded-2xl p-5 sm:p-7">
      <p class="text-teal-700 font-semibold text-sm">CONFIGURACIÓN</p><h2 class="text-2xl font-semibold mt-1">Catálogo de estatus</h2>
      <p class="text-slate-500 mt-1">Los estatus no se eliminan; se habilitan o inhabilitan. Los valores base permanecen protegidos.</p>
      <form @submit.prevent="save" class="mt-6 grid sm:grid-cols-2 gap-4">
        <label class="field">Valor<input v-model.trim="form.value" required maxlength="40" :readonly="!!editing" :class="editing ? 'bg-slate-100' : ''" placeholder="EJEMPLO_ESTATUS" /></label>
        <label class="field">Descripción<input v-model.trim="form.description" required maxlength="255" /></label>
        <label v-if="editing" class="field flex items-center gap-3 pt-5"><input v-model="form.enabled" :disabled="editing.systemBase" type="checkbox" /> Habilitado <span v-if="editing.systemBase" class="text-xs text-slate-400">Base protegido</span></label>
        <div class="sm:col-span-2 flex justify-end gap-2"><button type="button" @click="create" class="px-4 py-2 rounded-lg bg-slate-100">Limpiar</button><button class="px-4 py-2 rounded-lg bg-teal-700 text-white">{{ editing ? 'Guardar cambios' : 'Agregar estatus' }}</button></div>
      </form>
    </div>
    <section class="bg-white border border-slate-100 rounded-2xl overflow-hidden"><div class="overflow-x-auto"><table class="w-full text-sm min-w-[650px]"><thead class="bg-slate-50 text-left text-xs text-slate-500"><tr><th class="p-4">VALOR</th><th class="p-4">DESCRIPCIÓN</th><th class="p-4">DISPONIBILIDAD</th><th class="p-4"></th></tr></thead><tbody><tr v-for="status in statuses" :key="status.id" class="border-t border-slate-100"><td class="p-4 font-semibold">{{ status.value }}</td><td class="p-4 text-slate-600">{{ status.description }}</td><td class="p-4"><span :class="status.enabled ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'" class="rounded-full px-3 py-1 text-xs font-semibold">{{ status.enabled ? 'Habilitado' : 'Inhabilitado' }}</span></td><td class="p-4 text-right"><button @click="edit(status)" class="text-teal-700 font-semibold text-xs">Editar</button></td></tr><tr v-if="!statuses.length && !loading"><td colspan="4" class="p-8 text-center text-slate-500">{{ loadError ? 'No se pudo consultar el catálogo. Revisa los permisos de acceso.' : 'No hay estatus configurados.' }}</td></tr></tbody></table></div></section>
  </section>
</template>

<style scoped>
.field { display:block; color:#334155; font-size:.8rem; font-weight:600; }
.field input:not([type='checkbox']) { display:block; width:100%; margin-top:.45rem; border:1px solid #d9e5e3; border-radius:.55rem; padding:.7rem .75rem; font-size:.95rem; color:#172b2d; }
</style>
