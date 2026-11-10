import { createApp } from 'vue'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'
import { definePreset } from '@primevue/themes'
import 'primeicons/primeicons.css'
import './style.css'
import App from './App.vue'
const MotorDeskTheme = definePreset(Aura, {
  semantic: {
    primary: { 50:'#effaf6',100:'#d6f3e8',200:'#ace5d2',300:'#74d1b7',400:'#3bb496',500:'#169579',600:'#087c68',700:'#096653',800:'#0c5144',900:'#104337',950:'#06261f' }
  }
})
createApp(App).use(PrimeVue, { theme: { preset: MotorDeskTheme, options: { darkModeSelector: false } } }).mount('#app')
