<template>
    <div class="cart-badge">
      <button class="cart-button" @click="$emit('open-cart')">
        <i class="cart-icon">🛒</i>
        <span v-if="itemCount > 0" class="badge">{{ itemCount }}</span>
      </button>
    </div>
  </template>
  
  <script>
  export default {
    name: 'CartBadge',
    props: {
      itemCount: {
        type: Number,
        default: 0
      }
    },
    emits: ['open-cart']
  };
  </script>
  
  <style scoped>
  .cart-badge {
    position: relative;
    display: inline-block;
  }
  
  .cart-button {
    background: none;
    border: none;
    cursor: pointer;
    position: relative;
    padding: 8px;
  }
  
  .cart-icon {
    font-size: 1.5rem;
    color: #fff;
  }
  
  .badge {
    position: absolute;
    top: 0;
    right: 0;
    background-color: #5AAAD8;
    color: white;
    border-radius: 50%;
    padding: 2px 6px;
    font-size: 0.7rem;
    font-weight: bold;
    min-width: 18px;
    text-align: center;
  }
  </style>