<template>
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <NavBar />
    <div class="orders-page">
      <h1>Mes Commandes</h1>
      <OrderTable :orders="orders" @show-invoice="handleShowInvoice" />
      <p v-if="!orders.length">Aucune commande trouvée.</p>
    </div>
  </template>
  
  <script>
  import NavBar from "@/components/NavBar.vue";
  import OrderTable from "@/components/OrderTable.vue";
  import { fetchOrdersWithInvoices } from "@/utils/api";
  
  export default {
    name: "MesCommandes",
    components: { NavBar, OrderTable },
    data() {
      return {
        orders: [],
        apiKey: localStorage.getItem("apiKey"),
        apiLink: localStorage.getItem("apiLink"),
        clientId: localStorage.getItem("clientId"),
      };
    },
    async created() {
      try {
        this.orders = await fetchOrdersWithInvoices(this.apiLink, this.apiKey, this.clientId);
      } catch (e) {
        console.error("Erreur:", e);
      }
    },
    methods: {
      handleShowInvoice(invoiceId) {
        localStorage.setItem("invoiceId", invoiceId);
        this.$router.push("/invoice");
      },
    },
  };
  </script>
  
  <style scoped>
  .orders-page {
    padding: 20px;
    background-color: #0e100e;
    color: #f7f7f7;
    font-family: 'PP Formula', sans-serif;
  }
  h1 {
    font-family: 'DAWBE';
    font-size: 2rem;
    text-align: center;
    margin-bottom: 1rem;
  }

  @media screen and (max-width: 768px) {
  h1 {
    font-size: 28px; /* au lieu de 48px */
    text-align: center;
  }

  .orders-page {
    padding: 20px 10px;
    font-size: 14px; /* texte général plus petit */
  }

  .order-card p {
    font-size: 14px;
  }

  .order-card button {
    font-size: 14px;
    padding: 6px 12px;
  }
}

  </style>
  