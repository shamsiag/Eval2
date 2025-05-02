<template>
    <div class="sales-dashboard">
      <NavBar />
      <h1 class="title">Tableau de bord des ventes</h1>
  
      <SalesSummary :total-sales="summary.total_sales" />
  
      <ProductSalesList :products="summary.product_sales" />
    </div>
  </template>
  
  <script>
  import NavBar from "@/components/NavBar.vue";
  import SalesSummary from "@/components/SalesSummary.vue";
  import ProductSalesList from "@/components/ProductSalesList.vue";
  
  export default {
    name: "SalesDashboard",
    components: {
      NavBar,
      SalesSummary,
      ProductSalesList,
    },
    data() {
      return {
        summary: {
          total_sales: 0,
          product_sales: [],
        },
      };
    },
    methods: {
      fetchSales() {
        fetch("http://localhost:8000/sales/summary")
          .then((res) => res.json())
          .then((data) => {
            this.summary = data;
          })
          .catch((err) => {
            console.error("Erreur de chargement des ventes :", err);
          });
      },
    },
    mounted() {
      this.fetchSales();
    },
  };
  </script>
  
  <style scoped>
  .sales-dashboard {
    max-width: 900px;
    margin: 0 auto;
    padding: 20px;
  }
  .title {
    font-size: 2rem;
    margin-bottom: 20px;
  }
  </style>
  