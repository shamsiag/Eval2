<template>
    <section class="shop-header">
      <h2 class="header-title">{{ title }}</h2>
      <h2 class="header-subtitle">{{ subtitle }}</h2>
    </section>
  </template>
  
  <script>
  export default {
    name: 'ShopHeader',
    props: {
      title: {
        type: String,
        required: true
      },
      subtitle: {
        type: String,
        required: true
      }
    }
  };
  </script>
  
  <style scoped>
  .shop-header {
    text-align: left;
    padding: 20px 15px;
  }
  
  .header-title, .header-subtitle {
    font-size: 40px;
    font-family: 'DAWBE', sans-serif;
    color: #F7F7F7;
    margin: 0;
    line-height: 1;
    padding-left: 15px;
  }
  
  @media (min-width: 768px) {
    .header-title, .header-subtitle {
      font-size: 56px;
      padding-left: 25px;
    }
  }
  </style>