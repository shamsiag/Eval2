<template>
    <div class="product-sales-list">
      <h2>Détails par produit</h2>
      <ProductSalesItem
        v-for="product in products"
        :key="product.label"
        :label="product.label"
        :qty="product.qty_sold"
        :total="product.total_ttc"
      />
    </div>
  </template>
  
  <script>
  import ProductSalesItem from "@/components/ProductSalesItem.vue";
  
  export default {
    name: "ProductSalesList",
    components: { ProductSalesItem },
    props: {
      products: {
        type: Array,
        required: true,
      },
    },
  };
  </script>
  