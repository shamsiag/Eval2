<template>
    <div class="product-card">
      <div class="product-image-container">
        <img 
          :src="product.image || require('@/assets/item/default-product.png')" 
          :alt="product.name" 
          class="product-image" 
        />
      </div>
      
      <div class="product-details">
        <h3 class="product-name">{{ product.name }}</h3>
        <RatingBar :rating="product.note || 0" />
        <p class="product-price">{{ formatPrice((product.price_ht) * (1 + product.tva_tx/100)) }}</p>
        
        <RatingForm 
          :product-id="product.id" 
          :current-note="product.note" 
          @add-note="handleAddNote" 
        />
        
        <button class="product-button" @click="$emit('add-to-cart', product)">
          Ajouter au panier
        </button>
      </div>
    </div>
  </template>
  
  <script>
  import RatingBar from '@/components/RatingBar.vue';
  import RatingForm from '@/components/RatingForm.vue';
  
  export default {
    name: 'ProductCard',
    components: {
      RatingBar,
      RatingForm
    },
    props: {
      product: {
        type: Object,
        required: true
      }
    },
    emits: ['add-to-cart', 'add-note'],
    methods: {
      formatPrice(price) {
        if (typeof price !== 'number') {
          price = parseFloat(price) || 0;
        }
        return `${price.toFixed(2)} MGA`;
      },
      handleAddNote(data) {
        this.$emit('add-note', {
          productId: data.productId,
          noteInput: data.noteInput,
          currentNote: data.currentNote
        });
      }
    }
  };
  </script>
  
  <style scoped>
  .product-card {
    background-color: white;
    border-radius: 15px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    height: auto;
    max-width: 100%;
  }
  
  .product-card:active {
    transform: scale(0.98);
  }
  
  .product-image-container {
    width: 100%;
    display: flex;
    justify-content: center;
    padding: 15px;
    background-color: #f8f8f8;
  }
  
  .product-image {
    width: 100%;
    max-width: 200px;
    height: auto;
    border-radius: 8px;
    object-fit: contain;
  }
  
  .product-details {
    padding: 15px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  
  .product-name {
    font-size: 1.2rem;
    font-family: 'DAWBE', sans-serif;
    color: #1E1E1E;
    margin: 0;
    line-height: 1.3;
    word-break: break-word;
  }
  
  .product-price {
    font-size: 1.1rem;
    color: #1E1E1E;
    font-family: 'PP Formula', sans-serif;
    font-weight: bold;
    margin: 5px 0;
  }
  
  .product-button {
    width: 100%;
    padding: 12px 0;
    border: none;
    background-color: #0E100E;
    color: white;
    cursor: pointer;
    border-radius: 8px;
    margin-top: 10px;
    font-family: 'PP Formula', sans-serif;
    font-size: 1rem;
    transition: background-color 0.2s;
  }
  
  .product-button:hover {
    background-color: #5AAAD8;
  }
  
  .product-button:active {
    transform: scale(0.98);
  }
  
  @media (min-width: 768px) {
    .product-card {
      flex-direction: column;
    }
    
    .product-image {
      max-width: 180px;
    }
    
    .product-name {
      font-size: 1.3rem;
    }
  }
  </style>