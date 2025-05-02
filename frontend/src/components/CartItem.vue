<template>
    <div class="cart-item">
      <div class="cart-item-info">
        <img :src="image || defaultImage" alt="Product Image" class="cart-item-image" />
        <div class="cart-item-details">
          <h3 class="cart-item-name">{{ name }}</h3>
          <p class="cart-item-description">{{ description }}</p>
        </div>
      </div>
  
      <div class="cart-item-quantity">
        <span class="quantity-display">{{ quantity }}</span>
      </div>
  
      <div class="cart-item-price">
        <span class="product-unit-price">{{ formatCurrency(priceTtc) }} TTC</span>
        <span class="product-total-price">{{ formatCurrency(priceTtc * quantity) }}</span>
      </div>
  
      <button class="remove-button" @click="$emit('remove', lineId)">Supprimer</button>
    </div>
  </template>
  
  <script>
  import { formatCurrency } from '@/utils/currency';
  import defaultImage from '@/assets/item/default-product.png';
  
  export default {
    name: "CartItem",
    props: {
      lineId: Number,
      name: String,
      description: String,
      priceTtc: Number,
      quantity: Number,
      image: String,
    },
    methods: {
      formatCurrency,
    },
    data() {
      return {
        defaultImage,
      };
    },
  };
  </script>
  
  <style scoped>
  @media screen and (max-width: 768px) {
    .cart-item {
      flex-direction: column;
      align-items: flex-start;
    }
    .cart-item-price {
      align-items: flex-start;
    }
  }
  </style>
  