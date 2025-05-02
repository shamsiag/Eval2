<template>
    <div class="product-item">
      <p><strong>{{ label }}</strong></p>
      <p>{{ qty }} vendu(s)</p>
      <p>{{ total.toFixed(2) }} €</p>
    </div>
  </template>
  
  <script>
  export default {
    name: "ProductSalesItem",
    props: {
      label: String,
      qty: Number,
      total: Number,
    },
  };
  </script>
  
  <style scoped>
  .product-item {
    display: flex;
    justify-content: space-between;
    padding: 10px;
    border-bottom: 1px solid #ccc;
  }
  </style>
  