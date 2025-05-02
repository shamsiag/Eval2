<template>
    <div class="cart-summary">
      <div class="total">
        <p>Total : <span>{{ formatCurrency(totalPrice) }}</span></p>
      </div>
      <button class="checkout-button" @click="$emit('validate')">
        Valider la commande
      </button>
    </div>
  </template>
  
  <script>
  import { formatCurrency } from '@/utils/currency';
  
  export default {
    name: "CartSummary",
    props: {
      totalPrice: Number,
    },
    methods: {
      formatCurrency,
    },
  };
  </script>
  