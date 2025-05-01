export function getStatusText(order) {
    switch (order.status) {
      case "0": return "Commande créée";
      case "1":
        if (order.billed === true || order.billed === "1") return "Validé-Facturé";
        if (order.invoice) return "Validé-Facture créée";
        return "Validé";
      case "2": return "Facturée";
      case "3": return "Livrée";
      case "4": return "Traitée";
      case "-1": return "Annulée";
      default: return "Statut inconnu";
    }
  }
  