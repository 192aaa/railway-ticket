import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  User, Lock, UserFilled, HomeFilled, Tickets, SwitchButton,
  Switch, Search, Calendar, Clock, ShoppingCart, Plus,
  Warning, InfoFilled
} from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)

const icons = [
  User, Lock, UserFilled, HomeFilled, Tickets, SwitchButton,
  Switch, Search, Calendar, Clock, ShoppingCart, Plus,
  Warning, InfoFilled
]
icons.forEach(i => app.component(i.name, i))

app.use(createPinia())
app.use(router)

app.directive('ripple', {
  mounted(el) {
    el.addEventListener('click', (e) => {
      const rect = el.getBoundingClientRect()
      const size = Math.max(rect.width, rect.height)
      const span = document.createElement('span')
      span.className = 'ripple-ink'
      span.style.width = span.style.height = size + 'px'
      span.style.left = (e.clientX - rect.left - size / 2) + 'px'
      span.style.top = (e.clientY - rect.top - size / 2) + 'px'
      el.appendChild(span)
      setTimeout(() => span.remove(), 600)
    })
    el.style.position = el.style.position || 'relative'
    el.style.overflow = 'hidden'
    el.style.cursor = 'pointer'
  }
})

app.mount('#app')

router.beforeEach(() => {
  const bar = document.getElementById('top-progress')
  if (bar) {
    bar.style.width = '30%'
    bar.style.opacity = '1'
  }
})

router.afterEach(() => {
  const bar = document.getElementById('top-progress')
  if (bar) {
    bar.style.width = '100%'
    setTimeout(() => {
      bar.style.opacity = '0'
      bar.style.width = '0%'
    }, 200)
  }
})