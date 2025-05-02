export function formatCurrency(value) {
    return new Intl.NumberFormat("fr-FR", {
      style: "currency",
      currency: "EUR",
    }).format(value);
  }
  
export function formatDate(timestamp) {
    if (!timestamp) return "Inconnue";
    const date = new Date(timestamp * 1000);
    return date.toLocaleDateString("fr-FR");
  }
