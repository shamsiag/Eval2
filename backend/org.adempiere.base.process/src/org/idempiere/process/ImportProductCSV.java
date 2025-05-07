/**************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2023 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 ***************************/
package org.idempiere.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MLocator;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductPrice;
import org.compiere.model.MWarehouse;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.model.MOrg;

/**
 * Process pour importer des produits à partir d'un CSV,
 * avec création de prix et d'enregistrements d'inventaire
 * 
 * @author Custom Development
 */
@org.adempiere.base.annotation.Process
public class ImportProductCSV extends SvrProcess {
    
    /** Client à importer */
    private int m_AD_Client_ID = 0;
    /** Organisation */  
    private int m_AD_Org_ID = 0;
    /** Chemin du fichier CSV */
    private String m_filePath = null;
    /** ID de la catégorie de produit */
    private int m_M_Product_Category_ID = 135;
    /** ID de la liste de prix */
    private int m_M_PriceList_ID = 0;
    /** ID de l'entrepôt */
    private int m_M_Warehouse_ID = 103;
    /** Charset utilisé */
    private Charset m_charset = StandardCharsets.UTF_8;
    /** Chemin du fichier produit CSV */
    private String m_productCSVPath = null;
    /** Chemin du fichier prix CSV */
    private String m_priceCSVPath = null;
    
    /**
     * Préparation du processus
     */
    protected void prepare() {
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            if (name.equals("AD_Client_ID"))
                m_AD_Client_ID = para[i].getParameterAsInt();
            else if (name.equals("AD_Org_ID"))
                m_AD_Org_ID = para[i].getParameterAsInt();
            else if (name.equals("FilePath"))
                m_filePath = (String) para[i].getParameter();
            else if (name.equals("M_Product_Category_ID"))
                m_M_Product_Category_ID = para[i].getParameterAsInt();
            else if (name.equals("M_PriceList_ID"))
                m_M_PriceList_ID = para[i].getParameterAsInt();
            else if (name.equals("M_Warehouse_ID"))
                m_M_Warehouse_ID = para[i].getParameterAsInt();
            else if (name.equals("ProductCSV"))
                m_productCSVPath = (String) para[i].getParameter();
            else if (name.equals("PriceCSV"))
                m_priceCSVPath = (String) para[i].getParameter();
            else
                log.log(Level.SEVERE, "Paramètre inconnu: " + name);
        }
        
        // Valeurs par défaut imposées pour ce process
        m_AD_Client_ID = 11;
        
        // Log de l'organisation actuelle
        log.info("AD_Org_ID initial: " + m_AD_Org_ID);
        
        // Si aucune organisation n'est spécifiée, essayer d'utiliser celle du contexte
        if (m_AD_Org_ID == 0) {
            int contextOrgId = Env.getAD_Org_ID(getCtx());
            log.info("Organisation du contexte: " + contextOrgId);
            
            if (contextOrgId > 0) {
                m_AD_Org_ID = contextOrgId;
                log.info("Utilisation de l'organisation du contexte: " + m_AD_Org_ID);
            } else {
                // Si l'organisation du contexte n'est pas valide, essayer de trouver l'organisation par défaut du client
                String sql = "SELECT AD_Org_ID FROM AD_Org WHERE AD_Client_ID = ? AND IsActive = 'Y' ORDER BY AD_Org_ID";
                m_AD_Org_ID = DB.getSQLValueEx(get_TrxName(), sql, m_AD_Client_ID);
                
                if (m_AD_Org_ID <= 0) {
                    // Si aucune organisation n'est trouvée, essayer de créer une organisation par défaut
                    try {
                        String orgName = "Organisation par défaut";
                        sql = "INSERT INTO AD_Org (AD_Client_ID, AD_Org_ID, Name, Value, IsActive) VALUES (?, ?, ?, ?, 'Y')";
                        DB.executeUpdateEx(sql, new Object[]{m_AD_Client_ID, 11, orgName, orgName}, get_TrxName());
                        m_AD_Org_ID = 11;
                        log.info("Création d'une nouvelle organisation par défaut: " + m_AD_Org_ID);
                    } catch (Exception e) {
                        log.severe("Erreur lors de la création de l'organisation par défaut: " + e.getMessage());
                        throw new AdempiereException("Impossible de créer une organisation par défaut: " + e.getMessage());
                    }
                } else {
                    log.info("Utilisation de l'organisation par défaut du client: " + m_AD_Org_ID);
                }
            }
        }
        
        // Vérifier que l'organisation est valide
        if (m_AD_Org_ID <= 0) {
            log.severe("AD_Org_ID invalide: " + m_AD_Org_ID);
            throw new AdempiereException("L'organisation (AD_Org_ID) est requise et doit être valide");
        }
        
        // Vérifier que l'organisation existe dans la base de données
        String sql = "SELECT COUNT(*) FROM AD_Org WHERE AD_Org_ID = ? AND AD_Client_ID = ? AND IsActive = 'Y'";
        int count = DB.getSQLValueEx(get_TrxName(), sql, m_AD_Org_ID, m_AD_Client_ID);
        if (count <= 0) {
            log.severe("L'organisation " + m_AD_Org_ID + " n'existe pas ou n'est pas active pour le client " + m_AD_Client_ID);
            throw new AdempiereException("L'organisation " + m_AD_Org_ID + " n'existe pas ou n'est pas active pour le client " + m_AD_Client_ID);
        }
        
        log.info("Organisation finale utilisée: " + m_AD_Org_ID);
    }
    
    /**
     * Parse a price value from CSV, handling various formats
     * @param value The price value to parse
     * @return BigDecimal price value, returns BigDecimal.ZERO for null/empty/0 values
     */
    private BigDecimal parsePrice(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null") || value.trim().equals("0")) {
            return BigDecimal.ZERO;
        }
        
        // Remove quotes if present
        value = value.trim().replace("\"", "");
        
        try {
            // Replace comma with dot for decimal separator
            value = value.replace(',', '.');
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warning("Invalid price value: " + value + " - Using 0 instead");
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Exécution du processus
     */
    protected String doIt() throws Exception {
        // --- Import Produit CSV (fichier1.csv) ---
        if (m_productCSVPath == null || m_productCSVPath.trim().isEmpty())
            throw new AdempiereException("Le chemin du fichier produit CSV est requis");
        if (m_M_Warehouse_ID == 0)
            throw new AdempiereException("L'entrepôt est requis");
        if (m_AD_Org_ID <= 0)
            throw new AdempiereException("L'organisation (AD_Org_ID) est requise et doit être valide");

        File productCSV = new File(m_productCSVPath);
        if (!productCSV.exists())
            throw new AdempiereException("Fichier produit non trouvé: " + m_productCSVPath);

        // Obtenir l'emplacement par défaut de l'entrepôt
        MWarehouse warehouse = new MWarehouse(getCtx(), 103, get_TrxName());
        MLocator locator = MLocator.getDefault(warehouse);
        if (locator == null)
            throw new AdempiereException("Aucun emplacement par défaut pour l'entrepôt: " + warehouse.getName());

        List<String[]> productRecords = readCSV(productCSV);
        if (productRecords.isEmpty())
            return "Aucune donnée trouvée dans le fichier produit CSV";

        // Validation des en-têtes
        String[] headers = productRecords.get(0);
        if (headers.length < 5 || !headers[0].equalsIgnoreCase("organization") ||
            !headers[1].equalsIgnoreCase("name") ||
            !headers[2].equalsIgnoreCase("product_type") ||
            !headers[3].equalsIgnoreCase("ref") ||
            !headers[4].equalsIgnoreCase("init_stock"))
            throw new AdempiereException("Format CSV produit invalide. Colonnes attendues: organization,name,product_type,ref,init_stock");

        // Création de l'inventaire
        MInventory inventory = new MInventory(getCtx(), 0, get_TrxName());
        inventory.setAD_Org_ID(m_AD_Org_ID);
        inventory.setM_Warehouse_ID(103);
        inventory.setDescription("Import Produit CSV " + new java.util.Date());
        inventory.setMovementDate(new java.sql.Timestamp(System.currentTimeMillis()));
        inventory.setC_DocType_ID(144);
        
        log.info("Création de l'inventaire avec AD_Org_ID=" + m_AD_Org_ID);
        if (!inventory.save())
            throw new AdempiereException("Erreur lors de la création de l'inventaire");

        int countImported = 0;
        Map<String, Integer> productRefToId = new HashMap<>(); // Pour stocker les références des produits créés

        // Parcours des données (ignorer la ligne d'en-tête)
        for (int i = 1; i < productRecords.size(); i++) {
            String[] record = productRecords.get(i);

            String orgName = record[0];
            String name = record[1];
            String productType;
            String typeValue = record[2] != null ? record[2].trim() : "";
            
            // Si le type est vide ou null, définir par défaut comme ITEM
            if (typeValue.isEmpty() || typeValue.equalsIgnoreCase("null")) {
                productType = MProduct.PRODUCTTYPE_Item;
                log.info("Type de produit non spécifié pour " + name + " - Défini par défaut comme ITEM");
            } else if (typeValue.equalsIgnoreCase("I") || 
                typeValue.equalsIgnoreCase("ITEM") || 
                typeValue.equalsIgnoreCase("ITEMS") ||
                typeValue.equalsIgnoreCase("PRODUIT") ||
                typeValue.equalsIgnoreCase("PRODUITS") ||
                typeValue.equalsIgnoreCase("ARTICLE") ||
                typeValue.equalsIgnoreCase("ARTICLES")) {
                productType = MProduct.PRODUCTTYPE_Item;
            } else if (typeValue.equalsIgnoreCase("S") || 
                      typeValue.equalsIgnoreCase("SERVICE") || 
                      typeValue.equalsIgnoreCase("SERVICES") ||
                      typeValue.equalsIgnoreCase("SERVIC") ||
                      typeValue.equalsIgnoreCase("SERVIS") ||
                      typeValue.equalsIgnoreCase("PRESTATION") ||
                      typeValue.equalsIgnoreCase("PRESTATIONS")) {
                productType = MProduct.PRODUCTTYPE_Service;
            } else {
                log.warning("Type de produit non reconnu: " + record[2] + " - Défini par défaut comme ITEM");
                productType = MProduct.PRODUCTTYPE_Item;
            }
            String ref = record[3];
            BigDecimal initStock = BigDecimal.ZERO; // Par défaut à 0
            try {
                if (record.length > 4 && record[4] != null && !record[4].trim().isEmpty() && !record[4].trim().equalsIgnoreCase("null")) {
                    initStock = new BigDecimal(record[4].replace(',', '.'));
                }
            } catch (NumberFormatException e) {
                log.warning("Valeur de stock invalide pour le produit " + ref + ": " + record[4] + " - Stock initialisé à 0");
            }

            // Récupérer l'ID de l'organisation par son nom (insensible à la casse)
            int orgId = DB.getSQLValueEx(get_TrxName(), "SELECT AD_Org_ID FROM AD_Org WHERE UPPER(Name) = UPPER(?)", orgName);
            if (orgId <= 0) {
                // Créer une nouvelle organisation si elle n'existe pas
                MOrg org = new MOrg(getCtx(), 0, get_TrxName());
                org.set_ValueOfColumn("AD_Client_ID", m_AD_Client_ID);
                org.set_ValueOfColumn("AD_Org_ID", 0);
                org.setName(orgName);
                org.setValue(orgName);
                org.setIsActive(true);
                if (!org.save())
                    throw new AdempiereException("Erreur lors de la création de l'organisation: " + orgName);
                orgId = org.get_ID();
            }

            // Recherche si le produit existe déjà par Value (ref)
            int productId = findProductByValue(ref);
            MProduct product;

            if (productId > 0) {
                product = new MProduct(getCtx(), productId, get_TrxName());
                // Optionnel: mettre à jour le nom/type si besoin
            } else {
                product = new MProduct(getCtx(), 0, get_TrxName());
                product.set_ValueOfColumn("AD_Client_ID", m_AD_Client_ID);
                product.set_ValueOfColumn("AD_Org_ID", orgId);
                product.setValue(ref);
                product.setName(name);
                product.setM_Product_Category_ID(105);
                product.setProductType(productType);
                product.setIsStocked(productType.equals(MProduct.PRODUCTTYPE_Item));
                product.setIsSold(true);
                product.setIsPurchased(true);
                product.setC_UOM_ID(getDefaultUOM());
                product.setC_TaxCategory_ID(getDefaultTaxCategory());
                if (!product.save())
                    throw new AdempiereException("Erreur lors de la création du produit: " + name);
                productId = product.get_ID();
            }

            // Stocker la référence du produit pour l'importation des prix
            productRefToId.put(ref, productId);

            // Ajout de la ligne d'inventaire
            MInventoryLine line = new MInventoryLine(inventory,
                    locator.get_ID(), product.get_ID(), 0,
                    BigDecimal.ZERO, // QtyBook
                    initStock); // QtyCount

            if (!line.save())
                throw new AdempiereException("Erreur lors de la création de la ligne d'inventaire pour: " + name);

            log.info("Produit " + ref + " importé avec stock initial: " + initStock);

            countImported++;
        }
        inventory.completeIt();

        // --- Import des prix (fichier2.csv) ---
        if (m_priceCSVPath != null && !m_priceCSVPath.trim().isEmpty()) {
            File priceCSV = new File(m_priceCSVPath);
            if (!priceCSV.exists())
                throw new AdempiereException("Fichier prix non trouvé: " + m_priceCSVPath);

            List<String[]> priceRecords = readCSV(priceCSV);
            if (!priceRecords.isEmpty()) {
                String[] priceHeaders = priceRecords.get(0);
                if (priceHeaders.length < 6 || !priceHeaders[0].equalsIgnoreCase("ref") ||
                    !priceHeaders[1].equalsIgnoreCase("price_list") ||
                    !priceHeaders[2].equalsIgnoreCase("active") ||
                    !priceHeaders[3].equalsIgnoreCase("list_price") ||
                    !priceHeaders[4].equalsIgnoreCase("standard_price") ||
                    !priceHeaders[5].equalsIgnoreCase("limit_price"))
                    throw new AdempiereException("Format CSV prix invalide. Colonnes attendues: ref,price_list,active,list_price,standard_price,limit_price");

                for (int i = 1; i < priceRecords.size(); i++) {
                    String[] record = priceRecords.get(i);
                    if (record.length < 6)
                        continue;

                    String ref = record[0];
                    String priceListName = record[1];
                    String isActive = "1".equals(record[2]) ? "Y" : "N";
                    
                    // Parse prices using the new utility method
                    BigDecimal priceList = parsePrice(record[3]);
                    BigDecimal priceStd = parsePrice(record[4]);
                    BigDecimal priceLimit = parsePrice(record[5]);

                    // Récupérer l'ID du produit depuis notre Map
                    Integer productId = productRefToId.get(ref);
                    if (productId == null) {
                        log.warning("Produit non trouvé pour ref: " + ref + " - Ignoré");
                        continue;
                    }

                    // 1. Récupérer ou créer la PriceList
                    int priceListId = DB.getSQLValueEx(get_TrxName(),
                        "SELECT M_PriceList_ID FROM M_PriceList WHERE AD_Client_ID=? AND Name=?",
                        m_AD_Client_ID, priceListName);
                    MPriceList priceListObj;
                    if (priceListId > 0) {
                        priceListObj = new MPriceList(getCtx(), priceListId, get_TrxName());
                    } else {
                        priceListObj = new MPriceList(getCtx(), 0, get_TrxName());
                        priceListObj.setAD_Org_ID(m_AD_Org_ID);
                        priceListObj.setName(priceListName);
                        priceListObj.setIsSOPriceList(true);
                        priceListObj.setIsDefault(false);
                        priceListObj.setC_Currency_ID(100); // EUR par défaut, à adapter si besoin
                        priceListObj.setIsActive(isActive.equals("Y"));
                        if (!priceListObj.save())
                            throw new AdempiereException("Erreur lors de la création de la PriceList: " + priceListName);
                    }

                    // 2. Récupérer ou créer la PriceListVersion
                    int priceListVersionId = DB.getSQLValueEx(get_TrxName(),
                        "SELECT M_PriceList_Version_ID FROM M_PriceList_Version WHERE AD_Client_ID=? AND Name=? AND M_PriceList_ID=?",
                        m_AD_Client_ID, priceListName, priceListObj.get_ID());
                    MPriceListVersion priceListVersion;
                    if (priceListVersionId > 0) {
                        priceListVersion = new MPriceListVersion(getCtx(), priceListVersionId, get_TrxName());
                    } else {
                        priceListVersion = new MPriceListVersion(getCtx(), 0, get_TrxName());
                        priceListVersion.setAD_Org_ID(m_AD_Org_ID);
                        priceListVersion.setName(priceListName);
                        priceListVersion.setM_PriceList_ID(priceListObj.get_ID());
                        priceListVersion.setValidFrom(new java.sql.Timestamp(System.currentTimeMillis()));
                        int discountSchemaId = DB.getSQLValueEx(get_TrxName(), "SELECT M_DiscountSchema_ID FROM M_DiscountSchema WHERE AD_Client_ID=? LIMIT 1", m_AD_Client_ID);
                        if (discountSchemaId > 0) {
                            priceListVersion.setM_DiscountSchema_ID(discountSchemaId);
                        } else {
                            throw new AdempiereException("Aucun schéma de remise trouvé pour le client: " + m_AD_Client_ID);
                        }
                        log.info("Création de la version de liste de prix: " + priceListName + " pour la liste de prix: " + priceListObj.getName());
                        if (!priceListVersion.save())
                            throw new AdempiereException("Erreur lors de la création de la PriceListVersion: " + priceListName);
                    }

                    // 4. Créer ou mettre à jour le ProductPrice
                    MProductPrice productPrice = MProductPrice.get(getCtx(), priceListVersion.get_ID(), productId, get_TrxName());
                    if (productPrice == null) {
                        productPrice = new MProductPrice(getCtx(), 0, get_TrxName());
                        productPrice.setM_PriceList_Version_ID(priceListVersion.get_ID());
                        productPrice.setM_Product_ID(productId);
                    }
                    productPrice.setPrices(priceList, priceStd, priceLimit);
                    productPrice.setIsActive(isActive.equals("Y"));
                    if (!productPrice.save())
                        throw new AdempiereException("Erreur lors de la création/mise à jour du prix pour le produit ref: " + ref + ", price list: " + priceListName);
                }
            }
        }

        return "Importation produits réussie. " + countImported + " produits importés.";
    }
    
    /**
     * Lecture du fichier CSV
     */
    private List<String[]> readCSV(File file) throws Exception {
        List<String[]> records = new ArrayList<>();
        
        try (InputStream inputStream = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, m_charset))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // Split CSV line by comma
                String[] values = line.split(",");
                records.add(values);
            }
        }
        
        return records;
    }
    
    /**
     * Recherche d'un produit par sa valeur (ref)
     */
    private int findProductByValue(String value) {
        String sql = "SELECT M_Product_ID FROM M_Product WHERE AD_Client_ID=? AND Value=?";
        return DB.getSQLValueEx(get_TrxName(), sql, m_AD_Client_ID, value);
    }
    
    /**
     * Récupère l'UOM par défaut
     */
    private int getDefaultUOM() {
        return DB.getSQLValueEx(get_TrxName(), 
                "SELECT C_UOM_ID FROM C_UOM WHERE AD_Client_ID IN (0,?) AND IsDefault='Y'", 
                m_AD_Client_ID);
    }
    
    /**
     * Récupère la catégorie de taxe par défaut
     */
    private int getDefaultTaxCategory() {
        return DB.getSQLValueEx(get_TrxName(), 
                "SELECT C_TaxCategory_ID FROM C_TaxCategory WHERE AD_Client_ID IN (0,?) AND IsDefault='Y'", 
                m_AD_Client_ID);
    }
}