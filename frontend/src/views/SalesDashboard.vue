<template>
  <div class="sales-dashboard">
    <NavBar />
    <h1 class="title">Tableau de bord des ventes</h1>

    <SalesSummary :total-sales="summary.total_sales" />

    <ProductSalesList :products="summary.product_sales" />
  </div>
</template>

<script>
import NavBar from "@/components/NavBar.vue";
import SalesSummary from "@/components/SalesSummary.vue";
import ProductSalesList from "@/components/ProductSalesList.vue";

export default {
  name: "SalesDashboard",
  components: {
    NavBar,
    SalesSummary,
    ProductSalesList,
  },
  data() {
    return {
      summary: {
        total_sales: 0,
        product_sales: [],
      },
      authToken: localStorage.getItem('authToken'),
      apiLink: localStorage.getItem('apiLink') || 'http://localhost:8080/api/v1'
    };
  },
  async mounted() {
    await this.fetchSales();
  },
  methods: {
    async fetchSales() {
      try {
        const filter = encodeURIComponent("DocStatus eq 'CO' AND IsSOTrx eq true");
        const select = "C_Order_ID,DocumentNo,DateOrdered";
        const expand = encodeURIComponent("C_OrderLine($select=M_Product_ID,QtyOrdered,LineNetAmt;$expand=M_Product_ID($select=Name))");
        const url = `${this.apiLink}/models/C_Order?$filter=${filter}&$select=${select}&$expand=${expand}`;

        const headers = this.authToken ? { 'Authorization': `Bearer ${this.authToken}` } : {};
        const response = await fetch(url, { headers });
        if (!response.ok) throw new Error(`Erreur réseau: ${response.statusText}`);

        const data = await response.json();
        const orders = Array.isArray(data.records) ? data.records : [];

        // 1. Aplatir toutes les lignes de commande
        const lines = orders.flatMap(order => order.C_OrderLine || []);

        // 2. Calcul du chiffre d'affaires total
        const totalSales = lines.reduce((sum, line) => sum + (parseFloat(line.LineNetAmt) || 0), 0);

        // 3. Regroupement par produit
        const salesMap = {};
        lines.forEach(line => {
          const prod = line.M_Product_ID || {};
          const pid = prod.id;
          if (!salesMap[pid]) {
            salesMap[pid] = { name: prod.Name || '', sales: 0, quantity: 0 };
          }
          salesMap[pid].sales += parseFloat(line.LineNetAmt) || 0;
          salesMap[pid].quantity += parseFloat(line.QtyOrdered) || 0;
        });

        // 4. Format pour ProductSalesList
        const productSales = Object.values(salesMap).map(p => ({
          label: p.name,
          qty_sold: p.quantity,
          total_ttc: p.sales
        }));

        // Mise à jour du state
        this.summary.total_sales = totalSales;
        this.summary.product_sales = productSales;
      } catch (error) {
        console.error("Erreur de chargement des ventes :", error);
      }
    }
  }
};
</script>

<style scoped>
.sales-dashboard {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}
.title {
  font-size: 2rem;
  margin-bottom: 20px;
}
</style>
