<template>
  <header class="navbar">
    <div class="navbar-content">
      <!-- Logo -->
      <div class="logo">
        <img src="/Property 1=Variant2.png" alt="Logo de la marque" />
      </div>
      
      <!-- Burger Menu pour Mobile -->
      <div class="burger-menu" @click="toggleMobileMenu">
        <div class="burger-bar" :class="{ 'active': mobileMenuOpen }"></div>
        <div class="burger-bar" :class="{ 'active': mobileMenuOpen }"></div>
        <div class="burger-bar" :class="{ 'active': mobileMenuOpen }"></div>
      </div>
      
      <!-- Navigation des liens (Desktop et Mobile avec classe conditionnelle) -->
      <div class="navbar-links-container" :class="{ 'show': mobileMenuOpen }">
        <nav class="nav-links">
          <!-- Ajoute d'autres liens de navigation ici si nécessaire -->
        </nav>
        
        <!-- Boutons -->
        <div class="navbar-buttons">
          <!-- Panier -->
          <button @click="goToCart" class="navbar-button cart-button">
            <i class="fa fa-shopping-cart"></i>
            <span v-if="cartItemCount > 0" class="cart-count">{{ cartItemCount }}</span>
          </button>
          
          <!-- Connexion -->
          <router-link to="/login" class="navbar-button login-button">
            Connexion
          </router-link>

          <router-link to="/orders" class="navbar-button orders-button">
            Commandes
          </router-link>

          <router-link to="/dashboard" class="navbar-button dashboard-button">
            Dashboard
          </router-link>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
export default {
  name: "NavBar",
  data() {
    return {
      mobileMenuOpen: false,
      cartItemCount: 0 // À connecter avec votre état réel du panier
    };
  },
  methods: {
    // Redirection vers la page du panier
    goToCart() {
      this.$router.push({ name: "Cart" });
      if (this.mobileMenuOpen) {
        this.closeMobileMenu();
      }
    },
    // Gestion du menu mobile
    toggleMobileMenu() {
      this.mobileMenuOpen = !this.mobileMenuOpen;
      
      // Empêcher le défilement du body quand le menu est ouvert
      if (this.mobileMenuOpen) {
        document.body.style.overflow = 'hidden';
      } else {
        document.body.style.overflow = '';
      }
    },
    closeMobileMenu() {
      this.mobileMenuOpen = false;
      document.body.style.overflow = '';
    }
  },
  // S'assurer que l'écouteur d'événement est supprimé lors de la destruction du composant
  beforeUnmount() {
    document.body.style.overflow = '';
  }
};
</script>

<style scoped>
/* Variables pour les couleurs */
:root {
  --primary-color: #0e100e;
  --accent-color: #5aaad8;
  --text-light: white;
}

/* Container de la navbar */
.navbar {
  position: sticky;
  top: 0;
  width: 100%;
  background-color: var(--primary-color);
  padding: 10px 20px;
  z-index: 1000;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

/* Contenu de la navbar */
.navbar-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
}

/* Logo */
.logo {
  z-index: 1001; /* Au-dessus du menu mobile */
}

.logo img {
  height: 40px;
  width: auto;
}

/* Burger Menu (visible uniquement sur mobile) */
.burger-menu {
  display: none;
  flex-direction: column;
  justify-content: space-between;
  width: 30px;
  height: 22px;
  cursor: pointer;
  z-index: 1001; /* Au-dessus du menu mobile */
}

.burger-bar {
  width: 100%;
  height: 3px;
  background-color: var(--text-light);
  border-radius: 3px;
  transition: all 0.3s ease;
}

/* Animation pour le burger menu */
.burger-bar.active:nth-child(1) {
  transform: translateY(9px) rotate(45deg);
}

.burger-bar.active:nth-child(2) {
  opacity: 0;
}

.burger-bar.active:nth-child(3) {
  transform: translateY(-9px) rotate(-45deg);
}

/* Container pour les liens et boutons */
.navbar-links-container {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-grow: 1;
}

/* Liens de navigation */
.nav-links {
  display: flex;
  gap: 20px;
  margin-right: 20px;
}

/* Boutons (panier, connexion, etc.) */
.navbar-buttons {
  display: flex;
  gap: 15px;
  align-items: center;
}

/* Style commun des boutons */
.navbar-button {
  padding: 8px 12px;
  border: none;
  background-color: var(--accent-color);
  color: var(--text-light);
  font-family: 'PP Formula', sans-serif;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Bouton Panier */
.cart-button {
  background-color: var(--primary-color);
  position: relative;
  padding: 8px;
}

.cart-button:hover {
  background-color: var(--accent-color);
}

.cart-count {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: red;
  color: white;
  padding: 1px 5px;
  border-radius: 50%;
  font-size: 12px;
  min-width: 18px;
  text-align: center;
}

/* Bouton Connexion et autres boutons */
.login-button, .orders-button, .dashboard-button {
  background-color: var(--accent-color);
}

.login-button:hover, .orders-button:hover, .dashboard-button:hover {
  background-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--accent-color);
}

/* Icone du panier */
.fa-shopping-cart {
  font-size: 1.2em;
}

/* Media queries pour la responsivité */
@media (max-width: 768px) {
  .navbar-content {
    justify-content: space-between;
  }
  
  .logo {
    position: static;
  }
  
  .burger-menu {
    display: flex;
  }
  
  .navbar-links-container {
    position: fixed;
    top: 0;
    right: -100%;
    width: 100%;
    height: 100vh;
    background-color: var(--primary-color);
    flex-direction: column;
    justify-content: center;
    transition: right 0.3s ease;
    padding: 60px 20px 20px;
  }
  
  .navbar-links-container.show {
    right: 0;
  }
  
  .nav-links {
    flex-direction: column;
    align-items: center;
    margin-right: 0;
    margin-bottom: 30px;
  }
  
  .navbar-buttons {
    flex-direction: column;
    width: 100%;
    gap: 15px;
  }
  
  .navbar-button {
    width: 100%;
    padding: 12px;
    font-size: 16px;
  }
  
  /* Exception pour le bouton du panier dans le menu mobile */
  .cart-button {
    width: auto;
    align-self: center;
    margin-bottom: 20px;
    padding: 10px;
  }
  
  .fa-shopping-cart {
    font-size: 1.5em;
  }
  
  .cart-count {
    padding: 2px 6px;
    font-size: 14px;
  }
}

/* Pour les très petits écrans */
@media (max-width: 350px) {
  .navbar {
    padding: 10px;
  }
  
  .logo img {
    height: 30px;
  }
}
</style>