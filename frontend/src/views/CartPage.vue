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
        <CartSummary :total-price="calculateTotal()" @validate="openCheckoutModal"/>
      </div>
  
      <EmptyCartMessage v-else />

      <div v-if="showModal" class="modal-backdrop">
      <div class="modal-content">
        <h2>Finaliser votre commande</h2>
        <label for="clientName">Entrez votre nom :</label>
        <input id="clientName" v-model="clientName" placeholder="Votre nom complet" />
        <div class="modal-actions">
          <button @click="processOrder">Valider</button>
          <button @click="closeCheckoutModal">Annuler</button>
        </div>
      </div>
    </div>

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
        showModal: false,
        clientName: '',
        authToken: localStorage.getItem('authToken'),
        apiLink: localStorage.getItem('apiLink') || 'http://localhost:8080/api/v1'
      };
    },
    created() {
    this.loadCartFromStorage();
  },
  methods: {
    loadCartFromStorage() {
      const stored = localStorage.getItem("cartItems");
      try {
        this.cart = stored ? JSON.parse(stored) : [];
      } catch (e) {
        console.error("Impossible de parser le panier depuis localStorage", e);
        this.cart = [];
      }
    },

    saveCartToStorage() {
      try {
        localStorage.setItem("cartItems", JSON.stringify(this.cart));
      } catch (e) {
        console.error("Erreur lors de la sauvegarde du panier", e);
      }
    },

    removeItem(id) {
      this.cart = this.cart.filter(item => item.id !== id);
      this.saveCartToStorage();
    },

    calculateTotal() {
      return this.cart
        .reduce((sum, item) => sum + item.qty * parseFloat(item.price_ttc || 0), 0)
        .toFixed(2);
    },

    validateCart() {
      // TODO : implémenter envoi de commande ou redirection
      alert("Commande validée !");
    },
    openCheckoutModal() {
      this.showModal = true;
    },
    closeCheckoutModal() {
      this.showModal = false;
      this.clientName = '';
    },
    async processOrder() {
      if (!this.clientName) {
        alert('Veuillez entrer votre nom.');
        return;
      }
      try {
        const headers = {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.authToken}`
        };
        // 1. Vérifier existence du client
        const filter = encodeURIComponent(`name eq '${this.clientName}'`);
        let res = await fetch(`${this.apiLink}/models/C_BPartner?$filter=${filter}`, { headers });
        console.log('Response:', res);
        let bpData = await res.json();
        let partnerId;
        if (Array.isArray(bpData.records) && bpData.records.length > 0) {
          partnerId = bpData.records[0].id;
        } else {
          // 2. Créer le client
          res = await fetch(`${this.apiLink}/models/C_BPartner`, {
            method: 'POST', headers,
            body: JSON.stringify({
              AD_Client_ID: { propertyLabel: "Tenant", id: 11, identifier: "GardenWorld", "model-name": "ad_client" },
              AD_Org_ID: { propertyLabel: "Organization", id: 0, identifier: "*", "model-name": "ad_org" },
              IsActive: true,
              CreatedBy: { propertyLabel: "Created By", id: 100, identifier: "SuperUser", "model-name": "ad_user" },
              Value: this.clientName.replace(/\s+/g, ''),
              Name: this.clientName,
              IsCustomer: true,
              IsDiscountPrinted: true,
              C_BP_Group_ID: { propertyLabel: "Business Partner Group", id: 105, identifier: "Staff", "model-name": "c_bp_group" },
              TotalOpenBalance: 0
            })
          });
          const newBP = await res.json();
          console.log('Nouveau client créé:', newBP);
          partnerId = newBP.id;
        }
        // 3. Créer une adresse
        res = await fetch(`${this.apiLink}/models/C_Location`, {
          method: 'POST', headers,
          body: JSON.stringify({
            AD_Client_ID: 11,
            AD_Org_ID: 11,
            IsActive: true,
            CreatedBy: 100,
            Address1: "Address",
            City: "City",
            C_Country_ID: 100,
            C_Region_ID: 102,
            Postal: "06488",
            IsValid: false
          })
        });
        const loc = await res.json();
        // 4. Lier client-adresse
        await fetch(`${this.apiLink}/models/C_BPartner_Location`, {
          method: 'POST', headers,
          body: JSON.stringify({
            AD_Client_ID: 11,
            AD_Org_ID: 11,
            C_Location_ID: loc.id,
            C_BPartner_ID: partnerId
          })
        });
        // 5. Créer la commande //target id 135 mampiena stock
        res = await fetch(`${this.apiLink}/models/C_Order`, {
          method: 'POST', headers,
          body: JSON.stringify({
            C_BPartner_ID: partnerId,
            C_DocTypeTarget_ID: 132,
            AD_Org_ID: 11,
            M_Warehouse_ID: 103,
            "IsSOTrx": true
          })
        });
        const order = await res.json();
        // 6. Ajouter les lignes de commande
        const orderLines = this.cart.map(item => ({
          M_Product_ID: item.id,
          QtyEntered: item.qty,
          QtyOrdered: item.qty
        }));
        await fetch(`${this.apiLink}/models/C_Order/${order.id}`, {
          method: 'PUT', headers,
          body: JSON.stringify({
            C_OrderLine: orderLines,
            'doc-action': 'PR'
          })
        });
        alert('Commande créée avec succès !');
        this.cart = [];
        this.saveCartToStorage();
        this.closeCheckoutModal();
        this.$router.push('/shop');
      } catch (error) {
        console.error('Erreur lors du checkout :', error);
        alert('Une erreur est survenue lors de la validation de votre commande.');
      }
    }
  }
};
</script>

  
  <style scoped>
  .cart-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 20px;
    color: #F6F6FE;
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
    color: #F6F6FE;
  }
  .modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1rem;
}
.modal-content {
  background: #fff;
  color: #000;
  width: 100%;
  max-width: 400px;
  border-radius: 1rem;
  padding: 1.5rem;
}
.modal-actions {
  margin-top: 1rem;
  display: flex;
  justify-content: space-between;
}
.modal-actions button {
  flex: 1;
  margin: 0 0.25rem;
  padding: 0.5rem;
  border: none;
  border-radius: 0.5rem;
}
  </style>
  