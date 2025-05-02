<template>
    <div class="summary-box">
      <h2>Total des ventes</h2>
      <p class="total">{{ totalSales.toFixed(2) }} €</p>
    </div>
  </template>
  
  <script>
  export default {
    name: "SalesSummary",
    props: {
      totalSales: {
        type: Number,
        required: true,
      },
    },
  };
  </script>
  
  <style scoped>
  .summary-box {
    background: #f5f5f5;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
  }
  .total {
    font-size: 1.8rem;
    font-weight: bold;
    color: #333;
  }
  </style>
  