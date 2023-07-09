import { createApp } from 'vue'
import App from './App.vue'
import vuetify from './plugins/vuetify'
import { loadFonts } from './plugins/webfontloader'
import VueResizeObserver from "vue-resize-observer";

loadFonts()

createApp(App)
  .use(vuetify)
  .use(VueResizeObserver)
  .mount('#app')
