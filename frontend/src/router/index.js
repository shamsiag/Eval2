import { createRouter, createWebHistory } from 'vue-router';
import ShopPage from '../views/ShopPage.vue';
import MesCommandes from '@/views/MesCommandes.vue';
import Cart from '@/views/CartPage.vue';
import LoginPage from '@/views/LoginPage.vue';

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
  },
  {
    path: '/cart',
    name: 'Cart',
    component: Cart,
  },
  {
    path: '/',
    name: 'Login',
    component: LoginPage,
  },

];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
