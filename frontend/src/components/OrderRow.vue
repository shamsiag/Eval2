<template>
    <tr>
      <td>{{ order.ref || order.id }}</td>
      <td>{{ formatDate(order.date_creation) }}</td>
      <td>{{ formatCurrency(order.total_ttc) }}</td>
      <td>{{ getStatusText(order) }}</td>
      <td>{{ order.isPaid ? 'Oui' : 'Non' }}</td>
      <td>
        <button v-if="order.invoice" @click="$emit('show-invoice', order.invoice.id)">Facture</button>
        <span v-else>Aucune</span>
      </td>
    </tr>
  </template>
  
  <script>
  import { formatCurrency, formatDate } from "@/utils/format";
  import { getStatusText } from "@/utils/orderStatus";
  
  export default {
    name: "OrderRow",
    props: { order: Object },
    methods: { formatCurrency, formatDate, getStatusText },
  };
  </script>
  