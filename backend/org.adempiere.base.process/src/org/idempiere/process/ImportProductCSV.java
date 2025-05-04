/******************************************************************************
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
 *****************************************************************************/
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
    private int m_M_Product_Category_ID = 0;
    /** ID de la liste de prix */
    private int m_M_PriceList_ID = 0;
    /** ID de l'entrepôt */
    private int m_M_Warehouse_ID = 0;
    /** Charset utilisé */
    private Charset m_charset = StandardCharsets.UTF_8;
    
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
            else
                log.log(Level.SEVERE, "Paramètre inconnu: " + name);
        }
        
        if (m_AD_Client_ID == 0)
            m_AD_Client_ID = Env.getAD_Client_ID(getCtx());
        
        if (m_AD_Org_ID == 0)
            m_AD_Org_ID = Env.getAD_Org_ID(getCtx());
    }
    
    /**
     * Exécution du processus
     */
    protected String doIt() throws Exception {
        // Vérification des paramètres requis
        if (m_filePath == null || m_filePath.trim().isEmpty())
            throw new AdempiereException("Le chemin du fichier CSV est requis");
        
        if (m_M_Product_Category_ID == 0)
            throw new AdempiereException("La catégorie de produit est requise");
        
        if (m_M_PriceList_ID == 0)
            throw new AdempiereException("La liste de prix est requise");
        
        if (m_M_Warehouse_ID == 0)
            throw new AdempiereException("L'entrepôt est requis");
        
        // Vérification que le fichier existe
        File csvFile = new File(m_filePath);
        if (!csvFile.exists())
            throw new AdempiereException("Fichier non trouvé: " + m_filePath);
        
        // Obtenir la version de liste de prix Standard 2003
        MPriceList priceList = new MPriceList(getCtx(), m_M_PriceList_ID, get_TrxName());
        MPriceListVersion priceListVersion = priceList.getPriceListVersion(null);
        if (priceListVersion == null)
            throw new AdempiereException("Aucune version active pour la liste de prix: " + priceList.getName());
        
        // Obtenir l'emplacement par défaut de l'entrepôt
        MWarehouse warehouse = new MWarehouse(getCtx(), m_M_Warehouse_ID, get_TrxName());
        MLocator locator = MLocator.getDefault(warehouse);
        if (locator == null)
            throw new AdempiereException("Aucun emplacement par défaut pour l'entrepôt: " + warehouse.getName());
        
        // Lecture du fichier CSV
        List<String[]> records = readCSV(csvFile);
        if (records.isEmpty())
            return "Aucune donnée trouvée dans le fichier CSV";
        
        // Validation des en-têtes
        String[] headers = records.get(0);
        if (headers.length < 3 || !headers[0].equalsIgnoreCase("Name") || 
                !headers[1].equalsIgnoreCase("Price") || !headers[2].equalsIgnoreCase("Stock"))
            throw new AdempiereException("Format CSV invalide. Les colonnes attendues sont: Name, Price, Stock");
        
        // Création de l'inventaire
        MInventory inventory = new MInventory(getCtx(), 0, get_TrxName());
        inventory.setAD_Org_ID(m_AD_Org_ID);
        inventory.setM_Warehouse_ID(m_M_Warehouse_ID);
        inventory.setDescription("Import CSV " + new java.util.Date());
        inventory.setMovementDate(new java.sql.Timestamp(System.currentTimeMillis()));
        inventory.setC_DocType_ID(144);
        if (!inventory.save())
            throw new AdempiereException("Erreur lors de la création de l'inventaire");
        
        int countImported = 0;
        
        // Parcours des données (ignorer la ligne d'en-tête)
        for (int i = 1; i < records.size(); i++) {
            String[] record = records.get(i);
            if (record.length < 3)
                continue;
            
            String productName = record[0];
            BigDecimal price = new BigDecimal(record[1]);
            BigDecimal stock = new BigDecimal(record[2]);
            
            // Recherche si le produit existe déjà
            int productId = findProduct(productName);
            MProduct product;
            
            if (productId > 0) {
                // Le produit existe déjà - mise à jour
                product = new MProduct(getCtx(), productId, get_TrxName());
            } else {
                // Création d'un nouveau produit
                product = new MProduct(getCtx(), 0, get_TrxName());
                product.setAD_Org_ID(m_AD_Org_ID);
                product.setValue(generateProductCode(productName));
                product.setName(productName);
                product.setM_Product_Category_ID(m_M_Product_Category_ID);
                product.setProductType(MProduct.PRODUCTTYPE_Item);
                product.setIsStocked(true);
                product.setIsSold(true);
                product.setIsPurchased(true);
                
                // Obtenir l'UOM par défaut de la catégorie
                MProductCategory category = new MProductCategory(getCtx(), m_M_Product_Category_ID, get_TrxName());
                product.setC_UOM_ID(getDefaultUOM());
                product.setC_TaxCategory_ID(getDefaultTaxCategory());
                
                if (!product.save())
                    throw new AdempiereException("Erreur lors de la création du produit: " + productName);
            }
            
            // Création/Mise à jour du prix
            MProductPrice productPrice = MProductPrice.get(getCtx(), 
                    priceListVersion.get_ID(), product.get_ID(), get_TrxName());
            
            if (productPrice == null) {
                productPrice = new MProductPrice(getCtx(), 0, get_TrxName());
                productPrice.setM_PriceList_Version_ID(priceListVersion.get_ID());
                productPrice.setM_Product_ID(product.get_ID());
            }
            
            productPrice.setPrices(price, price, price);
            
            if (!productPrice.save())
                throw new AdempiereException("Erreur lors de la création/mise à jour du prix pour: " + productName);
            
            // Création de l'enregistrement d'inventaire
            MInventoryLine line = new MInventoryLine(inventory, 
                    locator.get_ID(), product.get_ID(), 0, // ASI_ID = 0
                    BigDecimal.ZERO, // QtyBook
                    stock); // QtyCount
            
            if (!line.save())
                throw new AdempiereException("Erreur lors de la création de la ligne d'inventaire pour: " + productName);
            
            countImported++;
        }
        inventory.completeIt();

        return "Importation réussie. " + countImported + " produits importés.";
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
     * Recherche d'un produit par son nom
     */
    private int findProduct(String name) {
        String sql = "SELECT M_Product_ID FROM M_Product "
                + "WHERE AD_Client_ID=? AND Name=?";
        return DB.getSQLValueEx(get_TrxName(), sql, m_AD_Client_ID, name);
    }
    
    /**
     * Génère un code produit basé sur le nom
     */
    private String generateProductCode(String name) {
        // Prendre les 3 premières lettres du nom et ajouter un timestamp
        String prefix = name.length() > 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
        return prefix + System.currentTimeMillis();
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