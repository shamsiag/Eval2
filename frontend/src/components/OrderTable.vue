<template>
    <div class="order-table">
      <table v-if="orders.length" class="table-desktop">
        <thead>
          <tr>
            <th>Référence</th>
            <th>Date</th>
            <th>Total TTC</th>
            <th>Statut</th>
            <th>Payée</th>
            <th>Facture</th>
          </tr>
        </thead>
        <tbody>
          <OrderRow
            v-for="order in orders"
            :key="order.id"
            :order="order"
            @show-invoice="$emit('show-invoice', $event)"
          />
        </tbody>
      </table>
  
      <!-- Affichage mobile -->
      <div v-if="orders.length" class="table-mobile">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <p><strong>Réf :</strong> {{ order.ref || order.id }}</p>
          <p><strong>Date :</strong> {{ formatDate(order.date_creation) }}</p>
          <p><strong>Total :</strong> {{ formatCurrency(order.total_ttc) }}</p>
          <p><strong>Statut :</strong> {{ getStatusText(order) }}</p>
          <p><strong>Payée :</strong> {{ order.isPaid ? 'Oui' : 'Non' }}</p>
          <p>
            <button v-if="order.invoice" @click="$emit('show-invoice', order.invoice.id)">Facture</button>
            <span v-else>Aucune</span>
          </p>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import OrderRow from "@/components/OrderRow.vue";
  import { formatCurrency, formatDate } from "@/utils/format";
  import { getStatusText } from "@/utils/orderStatus";
  
  export default {
    name: "OrderTable",
    components: { OrderRow },
    props: { orders: Array },
    methods: { formatCurrency, formatDate, getStatusText },
  };
  </script>
  
  <style scoped>
  .table-desktop {
    display: table;
  }
  .table-mobile {
    display: none;
  }
  .order-card {
    background: white;
    color: #1e1e1e;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 15px;
    box-shadow: 0 0 8px rgba(0,0,0,0.1);
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
  