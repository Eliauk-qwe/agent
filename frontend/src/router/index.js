import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoveMasterView from '../views/LoveMasterView.vue'
import ManusAgentView from '../views/ManusAgentView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/love-master',
      name: 'love-master',
      component: LoveMasterView
    },
    {
      path: '/manus-agent',
      name: 'manus-agent',
      component: ManusAgentView
    }
  ]
})

export default router
