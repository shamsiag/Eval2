import { createRouter, createWebHistory } from 'vue-router';
import ShopPage from '../views/ShopPage.vue';
import MesCommandes from '@/views/MesCommandes.vue';

const routes = [

  {
    path: '/shop',
    name: 'Shop',
    component: ShopPage,
  },
  {
    path: '/orders',
    name: 'Order',
    component: MesCommandes,
  }

];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
