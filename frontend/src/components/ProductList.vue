<template>
    <div class="product-list-container">
      <div class="cards-container">
        <ProductCard 
          v-for="product in products" 
          :key="product.id" 
          :product="product" 
          @add-to-cart="$emit('add-to-cart', product)" 
          @add-note="$emit('add-note', $event.productId, $event.noteInput, $event.currentNote)" 
        />
      </div>
    </div>
  </template>
  
  <script>
  import ProductCard from '@/components/ProductCard.vue';
  
  export default {
    name: 'ProductList',
    components: {
      ProductCard
    },
    props: {
      products: {
        type: Array,
        required: true
      }
    },
    emits: ['add-to-cart', 'add-note']
  };
  </script>
  
  <style scoped>
  .product-list-container {
    width: 100%;
    padding: 10px;
  }
  
  .cards-container {
    display: grid;
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 10px;
    max-width: 100%;
    margin: 0 auto;
  }
  
  /* Pour tablettes */
  @media (min-width: 768px) {
    .cards-container {
      grid-template-columns: repeat(2, 1fr);
      padding: 15px;
    }
  }
  
  /* Pour grands écrans */
  @media (min-width: 1024px) {
    .cards-container {
      grid-template-columns: repeat(3, 1fr);
      gap: 30px;
      padding: 20px;
      max-width: 90%;
      margin-left: 10%;
    }
  }
  </style>