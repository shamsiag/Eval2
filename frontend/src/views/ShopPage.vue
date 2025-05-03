<template>
  <div class="shop-container">
    <NavBar />
    <ShopHeader title="NOS" subtitle="PRODUITS" />
    <ProductList 
      :products="products" 
      @add-to-cart="addToCart" 
      @add-note="addNote" 
    />
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue';
import ShopHeader from '@/components/ShopHeader.vue';
import ProductList from '@/components/ProductList.vue';

export default {
  name: 'ShopPage',
  components: {
    NavBar,
    ShopHeader,
    ProductList
  },
  data() {
    return {
      products: [],
      cartItems: [],
      cartId: null,
      authToken: localStorage.getItem('authToken'),
      apiLink: localStorage.getItem('apiLink') || 'http://localhost:8080/api/v1',
      priceListVersionId: 104
    };
  },
  async created() {
    await this.fetchProducts();
    this.loadCartFromStorage();
  },
  methods: {
    async fetchProducts() {
      try {
        console.log('Récupération des produits iDempiere...');
        if (!this.authToken) throw new Error('Token d\'authentification manquant');

        const filter = `M_PriceList_Version_ID eq ${this.priceListVersionId}`;
        const url = `${this.apiLink}/models/M_Product?$select=M_Product_ID,Name&$expand=M_ProductPrice($select=M_PriceList_Version_ID,PriceList,PriceStd,PriceLimit;$filter=${filter})`;

        const response = await fetch(url, {
          headers: { 'Authorization': `Bearer ${this.authToken}` }
        });

        if (!response.ok) throw new Error(`Erreur réseau: ${response.statusText}`);
        const { records } = await response.json();

        this.products = records.map(item => {
          const priceInfo = (item.M_ProductPrice && item.M_ProductPrice.length)
            ? item.M_ProductPrice[0]
            : { PriceStd: 0 };

          return {
            id: item.id || item.M_Product_ID,
            name: item.Name,
            description: 'Aucune description disponible.',
            image: null,
            price_ht: parseFloat(priceInfo.PriceStd),
            price_ttc: parseFloat(priceInfo.PriceStd),
            tva_tx: 0,
            label: item.Name,
            ref: null,
            weight: null,
            width: null,
            length: null,
            height: null,
            note: 'Aucune note disponible.'
          };
        });
        console.log('Produits chargés:', this.products);
      } catch (error) {
        console.error('Erreur lors de la récupération des produits :', error);
      }
    },

    loadCartFromStorage() {
      const storedCart = localStorage.getItem('cartItems');
      if (storedCart) {
        try {
          this.cartItems = JSON.parse(storedCart);
        } catch (e) {
          console.error('Erreur de parsing du panier localStorage:', e);
          this.cartItems = [];
        }
      }
    },

    saveCartToStorage() {
      localStorage.setItem('cartItems', JSON.stringify(this.cartItems));
    },

    addToCart(product) {
      const existingItem = this.cartItems.find(item => item.id === product.id);
      if (existingItem) {
        existingItem.qty += 1;
      } else {
        this.cartItems.push({
          ...product,
          qty: 1
        });
      }

      this.saveCartToStorage();

      if (this.$toast) {
        this.$toast.success('Produit ajouté au panier !');
      } else {
        alert('Produit ajouté au panier !');
      }
    },


    async addNote(productId, noteInput, currentNote) {
      try {
        if(noteInput > 10 || noteInput < 0) {
          alert("La note doit être comprise entre 0 et 10");
          return;
        }
        
        let newNote;
        
        if (currentNote !== 'Aucune note disponible.') {
          newNote = (parseFloat(currentNote) + parseFloat(noteInput)) / 2;
        } else {
          newNote = parseFloat(noteInput);
        }
        
        const noteData = {
          array_options: {
            options_note: newNote.toString()
          }
        };
        
        const response = await fetch(`${this.apiLink}/products/${productId}`, {
          method: 'PUT',
          headers: {
            'DOLAPIKEY': this.apiKey,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(noteData)
        });
        
        if (!response.ok) {
          throw new Error(`Erreur lors de la mise à jour de la note: ${response.status}`);
        }
        
        const productToUpdate = this.products.find(p => p.id === productId);
        if (productToUpdate) {
          productToUpdate.note = newNote;
          productToUpdate.noteInput = ''; 
        }
        
        if (this.$toast) {
          this.$toast.success("Note mise à jour avec succès!");
        } else {
          alert("Note mise à jour avec succès!");
        }
        
      } catch (error) {
        console.error("Erreur lors de la mise à jour de la note:", error);
        if (this.$toast) {
          this.$toast.error("Échec de la mise à jour de la note");
        } else {
          alert("Échec de la mise à jour de la note");
        }
      }
    },
  }
};
</script>

<style scoped>
.shop-container {
  width: 100%;
  min-height: 100vh;
  background-color: #111;
  overflow-x: hidden;
}

@font-face {
  font-family: 'PP Formula';
  src: url('@/assets/fonts/PPFormula-NarrowRegular.otf') format('opentype');
  font-weight: medium;
}

@font-face {
  font-family: 'DAWBE';
  src: url('@/assets/fonts/dawbe.otf') format('opentype');
  font-weight: bold;
}
</style>