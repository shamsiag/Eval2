package org.idempiere.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MLocator;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Process pour importer deux fichiers CSV dont:
 * CSV1 : organization,name,product_type,ref,init_stock
 * CSV2 : ref,price_list,active,list_price,standard_price,limit_price
 */
@org.adempiere.base.annotation.Process(name = "ImportTwoCSV")
public class ImportTwoCSV extends SvrProcess {

    private String m_fileCSV1;
    private String m_fileCSV2;
    private Charset m_charset = StandardCharsets.UTF_8;
    private int m_M_Warehouse_ID=103;
    private MLocator m_locator;
    private MInventory m_inventory;

    @Override
    protected void prepare() {
        m_M_Warehouse_ID = 103;
        for (ProcessInfoParameter p : getParameter()) {
            String name = p.getParameterName();
            if ("FileCSV1".equals(name)) {
                m_fileCSV1 = (String) p.getParameter();
            } else if ("FileCSV2".equals(name)) {
                m_fileCSV2 = (String) p.getParameter();
            } else if ("M_Warehouse_ID".equals(name)) {
                m_M_Warehouse_ID = p.getParameterAsInt();
            } else {
                log.log(Level.WARNING, "Paramètre inconnu: " + name);
            }
        }
        if (m_fileCSV1 == null || m_fileCSV1.trim().isEmpty())
            throw new AdempiereException("Le premier fichier CSV est requis");
        if (m_fileCSV2 == null || m_fileCSV2.trim().isEmpty())
            throw new AdempiereException("Le deuxième fichier CSV est requis");
        if (m_M_Warehouse_ID == 0)
            throw new AdempiereException("L'entrepôt est requis");
    }

    @Override
    protected String doIt() throws Exception {
        // Préparation de l'entrepôt et de l'inventaire
        prepareWarehouseAndInventory();

        List<String[]> records1 = readCSV(new File(m_fileCSV1));
        List<String[]> records2 = readCSV(new File(m_fileCSV2));
        if (records1.size() < 2)
            throw new AdempiereException("Aucune donnée trouvée dans le premier CSV");
        if (records2.size() < 2)
            throw new AdempiereException("Aucune donnée trouvée dans le deuxième CSV");

        // 1) Traitement CSV1 : produits et stock initial
        int prodCount = 0;
        for (int i = 1; i < records1.size(); i++) {
            String[] r1 = records1.get(i);
            String orgValue    = r1[0];
            String name        = r1[1];
            String productType = r1[2];
            int prodId         = Integer.parseInt(r1[3]); // ref correspond à M_Product_ID
            BigDecimal initStock = new BigDecimal(r1[4]);

            // Vérifier si le produit existe
            int exists = DB.getSQLValueEx(get_TrxName(),
                "SELECT COUNT(1) FROM M_Product WHERE M_Product_ID=?", prodId);
            if (exists <= 0) {
                throw new AdempiereException("Produit introuvable pour M_Product_ID: " + prodId);
            }
            MProduct product = new MProduct(getCtx(), prodId, get_TrxName());

            // Ligne d'inventaire explicitement
            MInventoryLine line = new MInventoryLine(m_inventory,m_locator.get_ID(),prodId,0,
                Env.ZERO,initStock);
            // Organisation de l'inventaire déjà définie sur m_inventory
            line.setAD_Org_ID(m_inventory.getAD_Org_ID());
            line.setM_Inventory_ID(m_inventory.get_ID());
            line.setM_Locator_ID(m_locator.get_ID());
            line.setM_Product_ID(prodId);
            line.setM_AttributeSetInstance_ID(0);
            line.setQtyBook(Env.ZERO);
            line.setQtyCount(initStock);
            line.saveEx();

            prodCount++;
        }

        // 2) Traitement CSV2 : multiple PriceList par ref
        int priceCount = 0;
        for (int j = 1; j < records2.size(); j++) {
            String[] r2 = records2.get(j);
            int prodId            = Integer.parseInt(r2[0]);
            String priceListName  = r2[1];
            boolean active        = "1".equals(r2[2]);
            BigDecimal listPrice      = new BigDecimal(r2[3]);
            BigDecimal standardPrice  = new BigDecimal(r2[4]);
            BigDecimal limitPrice     = new BigDecimal(r2[5]);

            if (!active) continue;

            // Vérifier produit
            int exists2 = DB.getSQLValueEx(get_TrxName(),
                "SELECT COUNT(1) FROM M_Product WHERE M_Product_ID=?", prodId);
            if (exists2 <= 0) 
                throw new AdempiereException("Produit introuvable pour M_Product_ID: " + prodId);

            // PriceList et version
            int plId = DB.getSQLValueEx(get_TrxName(),
                "SELECT M_PriceList_ID FROM M_PriceList WHERE Name=?", priceListName);
            if (plId <= 0)
                throw new AdempiereException("Price List introuvable: " + priceListName);
            MPriceList pl = new MPriceList(getCtx(), plId, get_TrxName());
            MPriceListVersion plv = pl.getPriceListVersion(null);
            if (plv == null)
                throw new AdempiereException(
                    "Pas de version active pour Price List: " + priceListName);

            // Insert/update prix
            MProductPrice pp = MProductPrice.get(
                getCtx(), plv.get_ID(), prodId, get_TrxName());
            if (pp == null) {
                pp = new MProductPrice(getCtx(), 0, get_TrxName());
                pp.setM_PriceList_Version_ID(plv.get_ID());
                pp.setM_Product_ID(prodId);
            }
            pp.setPrices(listPrice, standardPrice, limitPrice);
            pp.saveEx();
            priceCount++;
        }

        // Finalisation 
        m_inventory.completeIt();
        return "Import terminé: " + prodCount + " lignes d'inventaire, "
            + priceCount + " enregistrements prix insérés.";
    }

    /**
     * Initialise l'entrepôt, le locator et crée l'inventaire.
     */
    private void prepareWarehouseAndInventory() {
        org.compiere.model.MWarehouse wh = new org.compiere.model.MWarehouse(
            getCtx(), m_M_Warehouse_ID, get_TrxName());
        m_locator = MLocator.getDefault(wh);
        if (m_locator == null)
            throw new AdempiereException(
                "Pas d'emplacement par défaut pour entrepôt: " + wh.getName());
        m_inventory = new MInventory(getCtx(), 0, get_TrxName());
        m_inventory.setAD_Org_ID(11);
        m_inventory.setM_Warehouse_ID(m_M_Warehouse_ID);
        m_inventory.setDescription("Import CSV double " +
            new Timestamp(System.currentTimeMillis()));
        m_inventory.setMovementDate(
            new Timestamp(System.currentTimeMillis()));
        m_inventory.setC_DocType_ID(144);
        m_inventory.saveEx();
    }

    private List<String[]> readCSV(File file) throws Exception {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(
                     new FileInputStream(file), m_charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                records.add(line.split(",", -1));
            }
        }
        return records;
    }

    private int getDefaultCategory() {
        return DB.getSQLValueEx(get_TrxName(),
            "SELECT M_Product_Category_ID FROM M_Product_Category " +
            "WHERE AD_Client_ID IN (0,?) AND IsDefault='Y'",
            Env.getAD_Client_ID(getCtx()));
    }

    private int getDefaultUOM() {
        return DB.getSQLValueEx(get_TrxName(),
            "SELECT C_UOM_ID FROM C_UOM " +
            "WHERE AD_Client_ID IN (0,?) AND IsDefault='Y'",
            Env.getAD_Client_ID(getCtx()));
    }

    private int getDefaultTaxCategory() {
        return DB.getSQLValueEx(get_TrxName(),
            "SELECT C_TaxCategory_ID FROM C_TaxCategory " +
            "WHERE AD_Client_ID IN (0,?) AND IsDefault='Y'",
            Env.getAD_Client_ID(getCtx()));
    }
}
