

-- Création du processus d'importation CSV de produits
INSERT INTO AD_Process
(AD_Process_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
 Value, Name, Description, Help, IsReport, IsServerProcess,
 ShowHelp, Classname, AccessLevel, EntityType,
 Statistic_Count, Statistic_Seconds, IsBetaFunctionality, IsDirectPrint)
VALUES
    (191009, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
     'ImportProductCSV', 'Import Product CSV', 'Import products from CSV file with price and inventory',
     'This process imports products from a CSV file. It creates or updates products, adds a price entry in the specified price list, and creates an inventory record.',
     'N', 'Y', 'Y', 'org.idempiere.process.ImportProductCSV', '3', 'U', 0, 0, 'N', 'N');

-- Récupérer l'ID du processus créé

DO $$
    DECLARE
        p_AD_Process_ID INTEGER;
    BEGIN
        SELECT 191009 INTO p_AD_Process_ID FROM AD_Process WHERE Value = 'ImportProductCSV';

        -- Ajouter les paramètres du processus
        INSERT INTO AD_Process_Para
        (AD_Process_Para_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
         Name, Description, Help, AD_Process_ID, SeqNo, AD_Reference_ID,
         FieldLength, IsCentrallyMaintained, IsRange, ColumnName, IsMandatory,
         EntityType, AD_Element_ID, AD_Reference_Value_ID, DefaultValue, IsAutocomplete)
        VALUES
            (191009, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
             'Organization', 'Organizational entity within client', NULL, p_AD_Process_ID, 10, 19,
             10, 'Y', 'N', 'AD_Org_ID', 'Y', 'U', 113, NULL, NULL, 'N');

        INSERT INTO AD_Process_Para
        (AD_Process_Para_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
         Name, Description, Help, AD_Process_ID, SeqNo, AD_Reference_ID,
         FieldLength, IsCentrallyMaintained, IsRange, ColumnName, IsMandatory,
         EntityType, AD_Element_ID, AD_Reference_Value_ID, DefaultValue, IsAutocomplete)
        VALUES
            (191010, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
             'File Path', 'Path to the CSV file to import', 'Full path to the CSV file on the server',
             p_AD_Process_ID, 20, 39, 255, 'Y', 'N', 'FilePath', 'Y', 'U', NULL, NULL, NULL, 'N');

        INSERT INTO AD_Process_Para
        (AD_Process_Para_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
         Name, Description, Help, AD_Process_ID, SeqNo, AD_Reference_ID,
         FieldLength, IsCentrallyMaintained, IsRange, ColumnName, IsMandatory,
         EntityType, AD_Element_ID, AD_Reference_Value_ID, DefaultValue, IsAutocomplete)
        VALUES
            (191011, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
             'Product Category', 'Category of a Product', NULL, p_AD_Process_ID, 30, 19,
             10, 'Y', 'N', 'M_Product_Category_ID', 'Y', 'U', 453, NULL, NULL, 'N');

        INSERT INTO AD_Process_Para
        (AD_Process_Para_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
         Name, Description, Help, AD_Process_ID, SeqNo, AD_Reference_ID,
         FieldLength, IsCentrallyMaintained, IsRange, ColumnName, IsMandatory,
         EntityType, AD_Element_ID, AD_Reference_Value_ID, DefaultValue, IsAutocomplete)
        VALUES
            (191012, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
             'Price List', 'Unique identifier of a Price List', NULL, p_AD_Process_ID, 40, 19,
             10, 'Y', 'N', 'M_PriceList_ID', 'Y', 'U', 449, NULL, NULL, 'N');

        INSERT INTO AD_Process_Para
        (AD_Process_Para_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
         Name, Description, Help, AD_Process_ID, SeqNo, AD_Reference_ID,
         FieldLength, IsCentrallyMaintained, IsRange, ColumnName, IsMandatory,
         EntityType, AD_Element_ID, AD_Reference_Value_ID, DefaultValue, IsAutocomplete)
        VALUES
            (191013, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
             'Warehouse', 'Storage Warehouse and Service Point', NULL, p_AD_Process_ID, 50, 19,
             10, 'Y', 'N', 'M_Warehouse_ID', 'Y', 'U', 459, NULL, NULL, 'N');

        -- Ajouter le processus au menu
        INSERT INTO AD_Menu
        (AD_Menu_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
         Name, Description, IsSOTrx, IsSummary, Action, AD_Process_ID,
         EntityType, IsCentrallyMaintained, AD_Window_ID, AD_Workflow_ID, AD_Task_ID, AD_Form_ID, AD_Workbench_ID)
        VALUES
            (191009, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
             'Import Product CSV', 'Import products from CSV file with price and inventory', 'Y', 'N', 'P', p_AD_Process_ID,
             'U', 'Y', NULL, NULL, NULL, NULL, NULL);
    END $$;

DO $$
    DECLARE
        p_Menu_ID INTEGER;
        v_Tree_ID INTEGER;
        v_Parent_ID INTEGER;
    BEGIN
        -- Get the menu ID
        SELECT AD_Menu_ID INTO p_Menu_ID FROM AD_Menu WHERE Name = 'Import Product CSV';

        -- Get the tree ID for the menu
        SELECT AD_Tree_ID INTO v_Tree_ID FROM AD_Tree WHERE TreeType = 'MM' AND Name = 'Menu';

        -- Get parent ID (try to use System Import as parent)
        SELECT Node_ID INTO v_Parent_ID FROM AD_TreeNodeMM
        WHERE AD_Tree_ID = v_Tree_ID AND Node_ID IN
                                         (SELECT AD_Menu_ID FROM AD_Menu WHERE Name = 'System Import' OR Name = 'Import' LIMIT 1);

        -- If parent not found, use Material Management
        IF v_Parent_ID IS NULL THEN
            SELECT Node_ID INTO v_Parent_ID FROM AD_TreeNodeMM
            WHERE AD_Tree_ID = v_Tree_ID AND Node_ID IN
                                             (SELECT AD_Menu_ID FROM AD_Menu WHERE Name = 'Material Management' LIMIT 1);
        END IF;

        -- Add menu to tree
        INSERT INTO AD_TreeNodeMM
        (AD_Tree_ID, Node_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
         Parent_ID, SeqNo)
        SELECT v_Tree_ID, p_Menu_ID, 0, 0, 'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
               v_Parent_ID, COALESCE(MAX(SeqNo), 0) + 10
        FROM AD_TreeNodeMM
        WHERE Parent_ID = v_Parent_ID;
    END $$;