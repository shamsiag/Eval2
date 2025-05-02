<template>
    <div class="cart-page">
      <NavBar />
  
      <h1 class="cart-title">Votre panier</h1>
  
      <div v-if="cart.length > 0" class="cart-container">
        <CartItem
          v-for="item in cart"
          :key="item.rowid"
          :line-id="item.rowid"
          :name="item.label"
          :description="item.description"
          :price-ttc="parseFloat(item.total_ttc / item.qty)"
          :quantity="item.qty"
          :image="item.photo"
          @remove="removeItem"
        />
        <CartSummary :total-price="calculateTotal()" @validate="validateCart" />
      </div>
  
      <EmptyCartMessage v-else />
    </div>
  </template>
  
  <script>
  import NavBar from "@/components/NavBar.vue";
  import CartItem from "@/components/CartItem.vue";
  import CartSummary from "@/components/CartSummary.vue";
  import EmptyCartMessage from "@/components/EmptyCartMessage.vue";
  
  export default {
    name: "CartPage",
    components: {
      NavBar,
      CartItem,
      CartSummary,
      EmptyCartMessage,
    },
    data() {
      return {
        cart: [],
      };
    },
    methods: {
      fetchCart() {
        fetch("http://localhost:8000/cart")
          .then((res) => res.json())
          .then((data) => {
            this.cart = data;
          })
          .catch((err) => {
            console.error("Erreur de chargement du panier :", err);
          });
      },
      removeItem(id) {
        this.cart = this.cart.filter((item) => item.rowid !== id);
      },
      calculateTotal() {
        return this.cart.reduce((acc, item) => acc + parseFloat(item.total_ttc), 0);
      },
      validateCart() {
        // Implémentation future : redirection ou envoi de commande
        alert("Commande validée !");
      },
    },
    mounted() {
      this.fetchCart();
    },
  };
  </script>
  
  <style scoped>
  .cart-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 20px;
  }
  .cart-title {
    font-size: 2rem;
    margin-bottom: 20px;
    color: #F6F6FE;
  }
  .cart-container {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  </style>
  