export async function fetchInvoiceForOrder(apiLink, apiKey, orderId) {
    const url = `${apiLink}/orders/${orderId}/invoices`;
    const response = await fetch(url, {
      headers: { DOLAPIKEY: apiKey },
    });
  
    if (!response.ok) return null;
    const invoices = await response.json();
    return invoices.length > 0 ? invoices[0] : null;
  }
  
  export async function fetchOrdersWithInvoices(apiLink, apiKey, clientId) {
    const url = `${apiLink}/orders?sqlfilters=(fk_soc:=:${clientId})`;
    const response = await fetch(url, {
      headers: { DOLAPIKEY: apiKey, "Content-Type": "application/json" },
    });
  
    if (!response.ok) throw new Error(`Erreur HTTP: ${response.status}`);
  
    const rawOrders = await response.json();
  
    const ordersWithInvoices = await Promise.all(
      rawOrders.map(async (order) => {
        const invoice = await fetchInvoiceForOrder(apiLink, apiKey, order.id);
        return {
          ...order,
          invoice,
          isPaid: invoice ? invoice.paye === "1" || invoice.paye === 1 : false,
        };
      })
    );
  
    return ordersWithInvoices;
  }
  