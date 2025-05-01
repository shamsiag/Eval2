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
      apiKey: localStorage.getItem('apiKey'),
      apiLink: localStorage.getItem('apiLink'),
    };
  },
  async created() {
    await this.fetchProductsFromDolibarr();
    await this.initCart();
  },
  methods: {
    async fetchProductsFromDolibarr() {
      try {
        console.log('Tentative de récupération des produits depuis Dolibarr...');
        if (!this.apiKey || !this.apiLink) throw new Error("API Key ou API Link manquant");

        const url = `${this.apiLink}/products`;
        const response = await fetch(url, {
          headers: {
            "DOLAPIKEY": this.apiKey
          }
        });

        if (!response.ok) throw new Error(`Erreur réseau: ${response.statusText}`);
        const products = await response.json();

        this.products = products.map(product => {
          console.log("Produit récupéré:", product);
          return {
            id: product.id,
            name: product.label,
            description: product.description || 'Aucune description disponible.',
            image: product.url_photo ? `${this.apiLink}${product.url_photo}` : null,
            price_ht: parseFloat(product.price || 0),
            price_ttc: parseFloat((product.price) * (1 + product.tva_tx/100)),
            tva_tx: parseFloat(product.tva_tx || 0),
            label: product.label,
            ref: product.ref,
            weight: product.weight,
            width: product.width,
            length: product.length,
            height: product.height,
            note: product.array_options?.options_note || 'Aucune note disponible.',
          };
        });
        
        console.log('Produits récupérés et traités:', this.products);
      } catch (error) {
        console.error("Erreur lors de la récupération des produits:", error);
      }
    },

    async initCart() {
      try {
        const clientId = localStorage.getItem('clientId');
        if (!clientId || !this.apiKey || !this.apiLink) throw new Error("Données manquantes (ClientId, API Key ou API Link)");

        const searchUrl = `${this.apiLink}/orders?sqlfilters=(fk_statut:=:0)and(fk_soc:=:${clientId})`;
        console.log("Lien url commande:"+searchUrl);
        const response = await fetch(searchUrl, {
          headers: {
            "DOLAPIKEY": this.apiKey
          }
        });

        if (!response.ok) throw new Error(`Erreur réseau: ${response.statusText}`);
        const orders = await response.json();
        console.log("Commandes brouillon récupérées:", orders);

        if (Array.isArray(orders) && orders.length > 0) {
          const cartId = orders[0].id;
          this.cartId = cartId;
          await this.fetchCartItems(cartId);
        } else {
          console.log("Aucune commande brouillon trouvée. Création d'une nouvelle...");
          const newOrderId = await this.createDraftOrder(clientId);
          this.cartId = newOrderId;
          this.cartItems = [];
        }
      } catch (error) {
        console.error("Erreur lors de l'initialisation du panier:", error);
      }
    },

    async createDraftOrder(clientId) {
      try {
        const payload = {
          socid: clientId,
          date: new Date().toISOString().split('T')[0],
          status: 0,
          lines: []
        };

        const response = await fetch(`${this.apiLink}/orders`, {
          method: 'POST',
          headers: {
            "DOLAPIKEY": this.apiKey,
            "Content-Type": "application/json"
          },
          body: JSON.stringify(payload)
        });

        if (!response.ok) {
          const message = await response.text();
          throw new Error(`Erreur lors de la création de la commande brouillon: ${message}`);
        }

        const newOrder = await response.json();
        console.log("Commande brouillon créée:", newOrder);
        return newOrder.id;
      } catch (error) {
        console.error("Erreur lors de la création de la commande:", error);
        return null;
      }
    },

    async fetchCartItems(cartId) {
      try {
        console.log(`Récupération des articles pour le panier ID: ${cartId}...`);
        const url = `${this.apiLink}/orders/${cartId}/lines`;
        const response = await fetch(url, {
          headers: {
            "DOLAPIKEY": this.apiKey
          }
        });

        if (!response.ok) throw new Error(`Erreur réseau: ${response.statusText}`);
        const cartItems = await response.json();
        this.cartItems = cartItems;
        console.log("Articles du panier récupérés:", this.cartItems);
      } catch (error) {
        console.error("Erreur lors de la récupération des articles du panier:", error);
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

    async addToCart(product) {
      try {
        console.log('Tentative d\'ajout au panier pour le produit:', product);
        const isLoggedIn = localStorage.getItem('authToken');
        if (!isLoggedIn) {
          alert('Vous devez être connecté pour ajouter des produits au panier.');
          this.$router.push({ name: 'login' });
          return;
        }

        // S'assurer que le panier est initialisé
        if (!this.cartId) {
          await this.initCart();
          if (!this.cartId) {
            alert("Erreur : impossible d'initialiser le panier");
            return;
          }
        }

        // Calculer le prix HT à partir du prix TTC si nécessaire
        let priceHt = product.price_ht;
        if (!priceHt && product.price_ttc) {
          priceHt = product.price_ttc / (1 + (product.tva_tx / 100));
        }

        const itemData = {
          fk_product: product.id,
          qty: 1,
          subprice: priceHt,
          tva_tx: product.tva_tx,
          label: product.label,
          product_desc: product.description || "",
          product_ref: product.ref,
          price: priceHt,
          product_tobuy: 1,
          product_tosell: 1,
          weight: product.weight || 0,
          width: product.width || 0,
          length: product.length || 0,
          height: product.height || 0,
          localtax1_tx: "0.0000",
          localtax2_tx: "0.0000",
          total_ht: priceHt,
          total_ttc: product.price_ttc
        };

        const response = await fetch(`${this.apiLink}/orders/${this.cartId}/lines`, {
          method: 'POST',
          headers: {
            "DOLAPIKEY": this.apiKey,
            "Content-Type": "application/json"
          },
          body: JSON.stringify(itemData)
        });

        if (response.ok) {
          console.log('Ajout au panier réussi');
          await this.fetchCartItems(this.cartId);
          if (this.$toast) {
            this.$toast.success('Produit ajouté au panier !');
          } else {
            alert('Produit ajouté au panier !');
          }
        } else {
          console.error("Erreur lors de l'ajout au panier :", await response.text());
          alert("Erreur lors de l'ajout au panier");
        }
      } catch (error) {
        console.error("Erreur fetch (addToCart):", error);
        alert("Une erreur s'est produite lors de l'ajout au panier");
      }
    }
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