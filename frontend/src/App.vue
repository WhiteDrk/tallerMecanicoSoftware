<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import Swal from 'sweetalert2'
import ClientRegistrationForm from './components/ClientRegistrationForm.vue'
import ClientList from './components/ClientList.vue'
import WorkshopRegistrationForm from './components/WorkshopRegistrationForm.vue'
import StatusCatalogView from './components/StatusCatalogView.vue'
import TestReportsView from './components/TestReportsView.vue'
import VehicleBlueprint from './components/VehicleBlueprint.vue'
import { login, registerCustomerAccount } from './services/authApi'

// Claims are only used for presentation. The API verifies signature and permissions.
function sessionClaims(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(payload), char => char.charCodeAt(0))))
    return claims.exp * 1000 > Date.now() ? claims : null
  } catch { return null }
}
const identity = ref(sessionClaims(sessionStorage.getItem('accessToken') || ''))
if (!identity.value) sessionStorage.removeItem('accessToken')
const view = ref(identity.value ? 'app' : 'login')
const recovery = ref(false)
const registration = ref(false)
const signingIn = ref(false)
const mobileNav = ref(false)
const compactMedia = window.matchMedia('(max-width:1000px)')
const compactViewport = ref(compactMedia.matches)
function updateViewport(event) { compactViewport.value = event.matches; mobileNav.value = false }
onMounted(() => compactMedia.addEventListener('change', updateViewport))
onUnmounted(() => compactMedia.removeEventListener('change', updateViewport))
const email = ref('')
const password = ref('')
const account = ref({ fullName: '', email: '', password: '', confirmPassword: '' })
const registering = ref(false)
const activeNav = ref('Inicio')
const selectedOrder = ref(null)
const orderFilter = ref('Todas')
const clientList = ref(null)
const roles = computed(() => (identity.value?.roles || []).map(role => role.replace(/^ROLE_/, '')))
const roleLabels = { ADMINISTRADOR:'Administración', MANAGER:'Gerencia', RECEPCIONISTA:'Recepción', SECRETARIA:'Secretaría', CUSTOMER:'Cliente', TREASURY:'Tesorería', MECHANIC:'Mecánica', MECHANIC_CHIEF:'Jefatura de mecánicos', AUDITOR:'Auditoría' }
const roleLabel = computed(() => roles.value.map(role => roleLabels[role] || role).join(' · '))
const initials = computed(() => (identity.value?.email || 'MD').slice(0, 2).toUpperCase())
const hasRole = (...allowed) => allowed.some(role => roles.value.includes(role))
const navigation = computed(() => [
  { label:'Inicio', icon:'pi-th-large', enabled:true },
  ...(hasRole('ADMINISTRADOR', 'MANAGER') ? [{ label:'Talleres', icon:'pi-building', enabled:true }] : []),
  ...(hasRole('ADMINISTRADOR', 'MANAGER', 'RECEPCIONISTA', 'SECRETARIA') ? [{ label:'Clientes', icon:'pi-users', enabled:true }] : []),
  ...(hasRole('ADMINISTRADOR', 'MANAGER') ? [{ label:'Estatus', icon:'pi-tags', enabled:true }] : []),
  ...(hasRole('ADMINISTRADOR', 'MANAGER', 'AUDITOR') ? [{ label:'Reportes', icon:'pi-chart-bar', enabled:true }] : []),
  { label:'Órdenes', icon:'pi-file-edit', enabled:false },
  { label:'Inventario', icon:'pi-box', enabled:false },
  { label:'Presupuestos', icon:'pi-wallet', enabled:false },
  { label:'Auditoría', icon:'pi-shield', enabled:false },
])
const orders = [
  { id:'OT-2026-0841', vehicle:'Mazda CX-5 2022', plate:'RZN-482-C', customer:'Laura Ramírez', status:'Diagnóstico', mechanic:'Luis Herrera', time:'Hace 18 min', progress:35 },
  { id:'OT-2026-0840', vehicle:'Toyota Corolla 2020', plate:'PXV-207-B', customer:'Marco Ortega', status:'En reparación', mechanic:'Andrea Cruz', time:'Hace 44 min', progress:68 },
  { id:'OT-2026-0838', vehicle:'Nissan Versa 2021', plate:'UTM-991-A', customer:'Sofía Molina', status:'Esperando autorización', mechanic:'—', time:'Hace 1 h', progress:22 },
  { id:'OT-2026-0835', vehicle:'Kia Rio 2019', plate:'GHP-590-D', customer:'Pablo Santos', status:'Listo para entrega', mechanic:'David Reyes', time:'Hace 2 h', progress:100 },
]
const visibleOrders = computed(() => orderFilter.value === 'Todas' ? orders : orders.filter(order => order.status === orderFilter.value))
function acceptSession(session) {
  sessionStorage.setItem('accessToken', session.accessToken)
  identity.value = sessionClaims(session.accessToken)
  view.value = 'app'; activeNav.value = location.hash.startsWith('#reportes') && hasRole('ADMINISTRADOR','MANAGER','AUDITOR') ? 'Reportes' : 'Inicio'; password.value = ''
}
function signOut() {
  sessionStorage.removeItem('accessToken')
  identity.value = null; mobileNav.value = false; view.value = 'login'
}
async function signIn() {
  if (!email.value || !password.value || signingIn.value) return
  signingIn.value = true
  try { acceptSession(await login({ email: email.value, password: password.value })) }
  catch (error) { await Swal.fire({ icon:'error', title:'No fue posible iniciar sesión', text:error.message }) }
  finally { signingIn.value = false }
}
async function createAccount() {
  const data = account.value
  if (!data.fullName.trim() || !data.email.trim() || !data.password || !data.confirmPassword) return
  if (data.password.length < 12 || data.password.length > 72) return Swal.fire({ icon:'warning', title:'Contraseña no válida', text:'Debe tener entre 12 y 72 caracteres.' })
  if (data.password !== data.confirmPassword) return Swal.fire({ icon:'warning', title:'Las contraseñas no coinciden' })
  registering.value = true
  try {
    acceptSession(await registerCustomerAccount(data)); registration.value = false
    account.value = { fullName:'', email:'', password:'', confirmPassword:'' }
    await Swal.fire({ icon:'success', title:'Cuenta creada', text:'Tu cuenta de cliente ya está vinculada e iniciada.' })
  } catch (error) { await Swal.fire({ icon:'error', title:'No fue posible crear la cuenta', text:error.message }) }
  finally { registering.value = false }
}
function navigate(label) {
  activeNav.value = label; mobileNav.value = false
  history.replaceState(null, '', label === 'Reportes' ? '#reportes' : location.pathname + location.search)
}
function reportLink() {
  if (location.hash.startsWith('#reportes') && hasRole('ADMINISTRADOR','MANAGER','AUDITOR')) activeNav.value = 'Reportes'
}
onMounted(() => { reportLink(); window.addEventListener('hashchange', reportLink) })
onUnmounted(() => window.removeEventListener('hashchange', reportLink))
function severity(status) { return ({ 'Diagnóstico':'info', 'En reparación':'warn', 'Esperando autorización':'danger', 'Listo para entrega':'success' })[status] }
</script>

<template>
  <main v-if="view === 'login'" class="login-page">
    <section class="login-layout">
      <div class="login-story">
        <a class="brand" href="#"><span class="brand-mark"><i class="pi pi-wrench"></i></span><span>Motor<span class="brand-accent">Desk</span><small>WORKSHOP MANAGEMENT</small></span></a>
        <div class="login-pitch"><span class="eyebrow"><span class="signal-dot"></span> PRECISIÓN EN CADA PROCESO</span><h1>Tu taller.<br />Su siguiente<br /><span>velocidad.</span></h1><p>Conecta personas, vehículos y decisiones.<br />Todo el taller, en una sola dirección.</p></div>
        <div class="login-machine"><div class="machine-caption"><span>01 / SISTEMA OPERATIVO</span><span>GESTIÓN AUTOMOTRIZ</span></div><VehicleBlueprint /><div class="machine-features"><span><i class="pi pi-lock"></i> Acceso por rol</span><span><i class="pi pi-history"></i> Actividad trazable</span><span><i class="pi pi-mobile"></i> Siempre contigo</span></div></div>
        <div class="story-footer"><span>INGENIERÍA PARA TU OPERACIÓN</span><span>MD / 01</span></div>
      </div>
      <div class="login-form-panel">
        <div class="login-topline"><span>PORTAL DEL TALLER</span><i class="pi pi-shield"></i></div>
        <div class="login-form-content"><span class="login-step">ACCESO / 01</span><h2>Todo listo.<br /> Ponte al mando.</h2><p class="login-description">Inicia sesión para continuar con tu operación.</p>
          <form @submit.prevent="signIn" class="login-form"><label for="login-email">Correo electrónico<InputText id="login-email" v-model.trim="email" required type="email" maxlength="254" autocomplete="username" placeholder="nombre@taller.com" /></label><div><div class="password-label"><label for="login-password">Contraseña</label><button type="button" @click="recovery=true">¿La olvidaste?</button></div><Password v-model="password" inputId="login-password" required :feedback="false" toggleMask autocomplete="current-password" placeholder="Tu contraseña" /></div><Button type="submit" :loading="signingIn" :disabled="signingIn" label="Entrar al taller" icon="pi pi-arrow-right" iconPos="right" class="login-submit" /></form>
          <div class="login-divider"><span></span><small>¿ES TU PRIMERA VISITA?</small><span></span></div><button class="create-account-button" @click="registration=true"><i class="pi pi-user-plus"></i> Crear cuenta de cliente <i class="pi pi-arrow-up-right"></i></button><p class="access-note"><i class="pi pi-lock"></i> Tu acceso y actividad quedan registrados.</p>
        </div>
        <footer class="login-footer">MotorDesk <span>CONTROL QUE SE SIENTE.</span></footer>
      </div>
    </section>
    <Dialog v-model:visible="recovery" modal header="Recuperar contraseña" :style="{width:'min(92vw, 430px)'}"><div class="notice"><i class="pi pi-info-circle"></i><p>El envío de correos de recuperación aún está pendiente de configurar. Contacta al administrador del taller para recuperar tu acceso.</p></div><template #footer><Button label="Entendido" @click="recovery=false" /></template></Dialog>
    <Dialog v-model:visible="registration" modal header="Crear cuenta de cliente" :style="{width:'min(92vw, 460px)'}"><p class="text-slate-600 mb-5 text-sm">Usa el nombre y correo registrados por tu taller para vincular tu cuenta.</p><form @submit.prevent="createAccount" class="space-y-4"><label class="block text-sm font-medium" for="account-name">Nombre completo<InputText id="account-name" v-model.trim="account.fullName" required maxlength="150" class="w-full mt-2" autocomplete="name" /></label><label class="block text-sm font-medium" for="account-email">Correo electrónico<InputText id="account-email" v-model.trim="account.email" required type="email" maxlength="254" class="w-full mt-2" autocomplete="email" /></label><label class="block text-sm font-medium" for="account-password">Contraseña <span class="font-normal text-slate-500">12 a 72 caracteres</span><Password v-model="account.password" inputId="account-password" required :feedback="true" toggleMask class="w-full mt-2" inputClass="w-full" autocomplete="new-password" /></label><label class="block text-sm font-medium" for="account-confirm">Confirmar contraseña<Password v-model="account.confirmPassword" inputId="account-confirm" required :feedback="false" toggleMask class="w-full mt-2" inputClass="w-full" autocomplete="new-password" /></label><div class="flex justify-end gap-2 pt-2"><Button type="button" label="Cancelar" severity="secondary" @click="registration=false" /><Button type="submit" :loading="registering" :disabled="registering" label="Crear cuenta" icon="pi pi-user-plus" /></div></form></Dialog>
  </main>

  <div v-else class="app-shell" @keydown.esc="mobileNav=false">
    <button v-if="mobileNav" class="nav-overlay" aria-label="Cerrar menú" @click="mobileNav=false"></button>
    <aside :inert="compactViewport && !mobileNav" :aria-hidden="compactViewport && !mobileNav ? true : undefined" class="sidebar" :class="{ 'sidebar-open':mobileNav }"><a class="brand" href="#" @click.prevent="navigate('Inicio')"><span class="brand-mark"><i class="pi pi-wrench"></i></span><span>Motor<span class="brand-accent">Desk</span><small>WORKSHOP MANAGEMENT</small></span></a><div class="sidebar-workspace"><span class="workspace-icon"><i class="pi pi-building"></i></span><div><b>Mi espacio de trabajo</b><span>Gestión de talleres</span></div></div><span class="nav-heading">CENTRO DE OPERACIÓN</span><nav aria-label="Navegación principal"><button v-for="item in navigation" :key="item.label" :disabled="!item.enabled" :title="item.enabled ? item.label : 'Módulo pendiente de implementación'" :aria-current="activeNav === item.label ? 'page' : undefined" @click="navigate(item.label)" class="nav-item" :class="{ active:activeNav === item.label }"><i :class="['pi', item.icon]"></i><span>{{ item.label }}</span><small v-if="!item.enabled">PRÓX.</small><i v-else-if="activeNav === item.label" class="pi pi-angle-right"></i></button></nav><div class="sidebar-bottom"><div class="session-security"><i class="pi pi-shield"></i><span>Acceso por rol<small>{{ roleLabel }}</small></span><span class="signal-dot"></span></div><button class="sign-out" @click="signOut"><i class="pi pi-sign-out"></i> Cerrar sesión</button><div class="sidebar-version">MOTORDESK <span>OPERACIÓN CON PRECISIÓN</span></div></div></aside>
    <section class="app-body"><header class="app-header"><div class="header-context"><button class="mobile-menu-button" aria-label="Abrir menú" :aria-expanded="mobileNav" @click="mobileNav=!mobileNav"><i class="pi pi-bars"></i></button><span class="header-breadcrumb">Espacio de trabajo <i class="pi pi-angle-right"></i> <b>{{ activeNav }}</b></span></div><div class="header-user"><span class="session-indicator"><span class="signal-dot"></span> SESIÓN ACTIVA</span><span class="user-avatar">{{ initials }}</span><div class="user-info"><b>{{ identity?.email }}</b><small>{{ roleLabel }}</small></div></div></header>
      <main class="app-content">
        <WorkshopRegistrationForm v-if="activeNav === 'Talleres'" />
        <template v-else-if="activeNav === 'Clientes'"><ClientList ref="clientList" :roles="roles" /><ClientRegistrationForm @registered="clientList?.reload()" /></template>
        <StatusCatalogView v-else-if="activeNav === 'Estatus'" />
        <TestReportsView v-else-if="activeNav === 'Reportes' && hasRole('ADMINISTRADOR','MANAGER','AUDITOR')" :can-upload="hasRole('ADMINISTRADOR')" @session-expired="signOut" />
        <template v-else><div class="page-heading"><div><span class="eyebrow">TU CENTRO DE OPERACIÓN</span><h1>Una nueva marcha.<span class="heading-dot">●</span></h1><p>Personas, vehículos y procesos. Todo empieza aquí.</p></div><span class="date-chip"><i class="pi pi-calendar"></i>{{ new Date().toLocaleDateString('es-MX',{day:'numeric',month:'long',year:'numeric'}) }}</span></div>
          <section class="dashboard-hero"><div class="hero-copy"><span class="hero-kicker"><span class="signal-dot"></span> CONECTA TU OPERACIÓN</span><h2>Menos fricción.<br /><span>Más taller.</span></h2><p>Gestiona tus clientes y asocia sus vehículos desde un mismo espacio.</p><button v-if="hasRole('ADMINISTRADOR','MANAGER','RECEPCIONISTA','SECRETARIA')" @click="navigate('Clientes')" class="hero-action">Administrar clientes <i class="pi pi-arrow-up-right"></i></button><span v-else class="hero-customer-note">Has iniciado sesión en tu cuenta personal.</span></div><div class="hero-vehicle"><div class="hero-coordinate">MD — GARAGE CONTROL / 01</div><VehicleBlueprint /><div class="hero-vehicle-label"><span>PRECISIÓN / TRAZABILIDAD</span><i class="pi pi-compass"></i></div></div></section>
          <template v-if="!hasRole('CUSTOMER')">
          <div class="demo-notice"><i class="pi pi-info-circle"></i><span><b>Vista de demostración.</b> Las cifras y órdenes siguientes ilustran el diseño; aún no están conectadas a datos operativos.</span><span class="demo-badge">DEMO</span></div>
          <div class="metric-grid"><article class="metric-card"><div><span>Órdenes activas</span><i class="pi pi-file-edit"></i></div><strong>12<small>EN PROCESO</small></strong><footer><span class="metric-trend">↗ 2</span> respecto a ayer <span class="metric-spark">▂▃▂▄▃▆▅█</span></footer></article><article class="metric-card"><div><span>Por autorizar</span><i class="pi pi-clock"></i></div><strong>03<small>PENDIENTES</small></strong><footer><span class="metric-dot orange"></span> Requieren seguimiento</footer></article><article class="metric-card"><div><span>Listos para entrega</span><i class="pi pi-check-circle"></i></div><strong>05<small>VEHÍCULOS</small></strong><footer><span class="metric-dot"></span> Preparados para salir</footer></article><article class="metric-card revenue-card"><div><span>Facturación proyectada</span><i class="pi pi-chart-line"></i></div><strong>$48,250</strong><footer>MXN <span class="revenue-period">SEMANA ACTUAL</span></footer></article></div>
          <section class="orders-panel"><div class="orders-heading"><div><span class="eyebrow">VISTA DE DEMOSTRACIÓN</span><h2>El taller en movimiento<span class="count-chip">04</span></h2><p>Así se verá el seguimiento de las órdenes de trabajo.</p></div><span class="demo-badge">DATOS DE EJEMPLO</span></div><div class="order-filters" aria-label="Filtrar órdenes de demostración"><button v-for="filter in ['Todas','Diagnóstico','En reparación','Esperando autorización']" :key="filter" @click="orderFilter=filter" :aria-pressed="orderFilter===filter" :class="{ active:orderFilter===filter }">{{ filter }}</button></div><div class="table-scroll"><table class="orders-table"><thead><tr><th>ORDEN / VEHÍCULO</th><th>CLIENTE</th><th>ESTATUS</th><th>RESPONSABLE</th><th><span class="sr-only">Acciones</span></th></tr></thead><tbody><tr v-for="order in visibleOrders" :key="order.id"><td><span class="order-id">{{ order.id }}</span><b>{{ order.vehicle }}</b><small>{{ order.plate }}</small><div class="order-progress"><span :style="{width:order.progress+'%'}"></span></div></td><td>{{ order.customer }}</td><td><Tag :value="order.status" :severity="severity(order.status)" rounded /></td><td><span>{{ order.mechanic }}</span><small>{{ order.time }}</small></td><td><button @click="selectedOrder=order" class="order-detail" :aria-label="'Ver ejemplo '+order.id"><i class="pi pi-arrow-up-right"></i></button></td></tr></tbody></table></div><div class="orders-footer"><span><i class="pi pi-info-circle"></i> 4 órdenes de ejemplo</span><span>MOTORDESK / DEMO</span></div></section>
          </template>
        </template>
        <footer class="app-footer"><span>MotorDesk <span class="footer-separator">/</span> Gestión de talleres</span><span><i class="pi pi-shield"></i> Cada movimiento cuenta.</span></footer>
      </main>
    </section>
    <Dialog :visible="!!selectedOrder" @update:visible="value => { if (!value) selectedOrder=null }" modal :header="selectedOrder?.id" :style="{width:'min(92vw, 560px)'}"><template v-if="selectedOrder"><span class="demo-badge">ORDEN DE DEMOSTRACIÓN</span><p class="font-semibold text-lg mt-4">{{ selectedOrder.vehicle }}</p><p class="text-slate-500">{{ selectedOrder.customer }} · {{ selectedOrder.plate }}</p><div class="order-timeline"><div><b>09:42 · Diagnóstico iniciado</b><p>Ejemplo de una entrada en la bitácora de trabajo.</p></div><div><b>09:15 · Ingreso confirmado</b><p>Ejemplo de recepción y autorización inicial.</p></div></div></template></Dialog>
  </div>
</template>
