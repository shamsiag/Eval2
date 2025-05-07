-- 1) Création du process
INSERT INTO AD_Process
  (AD_Process_ID, AD_Client_ID, AD_Org_ID, IsActive,
   Created, CreatedBy, Updated, UpdatedBy,
   Value, Name, Description, Help,
   IsReport, IsServerProcess, ShowHelp,
   Classname, AccessLevel, EntityType,
   Statistic_Count, Statistic_Seconds,
   IsBetaFunctionality, IsDirectPrint)
VALUES
  (191100, 0, 0, 'Y',
   CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
   'ImportTwoCSV', 'Import de deux CSV',
   'Importe deux fichiers CSV dans un même process',
   'Sélectionnez deux fichiers CSV pour traitement conjoint.',
   'N', 'Y', 'Y',
   'org.idempiere.process.ImportTwoCSV',
   '3', 'U',
   0, 0, 'N', 'N');

-- 2) Ajout des paramètres “FileName” pour deux uploads

DO $$
DECLARE
    p_AD_Process_ID INTEGER;
    v_Ref_ID        INTEGER;
BEGIN
    -- Récupérer l’ID du process créé
    SELECT AD_Process_ID INTO p_AD_Process_ID
      FROM AD_Process
     WHERE Value = 'ImportTwoCSV';

    -- Récupérer l’AD_Reference_ID pour le type FileName
    SELECT AD_Reference_ID INTO v_Ref_ID
      FROM AD_Reference
     WHERE Name = 'FileName';

    -- Paramètre 1 : premier CSV
    INSERT INTO AD_Process_Para
      (AD_Process_Para_ID, AD_Client_ID, AD_Org_ID, IsActive,
       Created, CreatedBy, Updated, UpdatedBy,
       Name, Description, Help,
       AD_Process_ID, SeqNo, AD_Reference_ID,
       FieldLength, IsCentrallyMaintained, IsRange,
       ColumnName, IsMandatory,
       EntityType, AD_Element_ID,
       AD_Reference_Value_ID, DefaultValue, IsAutocomplete)
    VALUES
      (191101, 0, 0, 'Y',
       CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
       'CSV File 1', 'Premier fichier CSV à importer',
       'Sélectionnez le premier fichier CSV',
       p_AD_Process_ID, 10, v_Ref_ID,
       255, 'Y', 'N',
       'FileCSV1', 'Y',
       'U', NULL, NULL, NULL, 'N');

    -- Paramètre 2 : deuxième CSV
    INSERT INTO AD_Process_Para
      (AD_Process_Para_ID, AD_Client_ID, AD_Org_ID, IsActive,
       Created, CreatedBy, Updated, UpdatedBy,
       Name, Description, Help,
       AD_Process_ID, SeqNo, AD_Reference_ID,
       FieldLength, IsCentrallyMaintained, IsRange,
       ColumnName, IsMandatory,
       EntityType, AD_Element_ID,
       AD_Reference_Value_ID, DefaultValue, IsAutocomplete)
    VALUES
      (191102, 0, 0, 'Y',
       CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
       'CSV File 2', 'Deuxième fichier CSV à importer',
       'Sélectionnez le deuxième fichier CSV',
       p_AD_Process_ID, 20, v_Ref_ID,
       255, 'Y', 'N',
       'FileCSV2', 'Y',
       'U', NULL, NULL, NULL, 'N');
END $$;

-- 3) Ajout du process au menu
INSERT INTO AD_Menu
  (AD_Menu_ID, AD_Client_ID, AD_Org_ID, IsActive,
   Created, CreatedBy, Updated, UpdatedBy,
   Name, Description, IsSOTrx, IsSummary,
   Action, AD_Process_ID, EntityType,
   IsCentrallyMaintained, AD_Window_ID,
   AD_Workflow_ID, AD_Task_ID,
   AD_Form_ID, AD_Workbench_ID)
VALUES
  (191100, 0, 0, 'Y',
   CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
   'Import Deux CSV', 'Import de deux fichiers CSV',
   'Y', 'N', 'P', 191100,
   'U', 'Y', NULL, NULL, NULL, NULL, NULL);

-- 4) Ajout au tree “Menu” (System Import ou Material Management)
DO $$
DECLARE
    p_Menu_ID   INTEGER;
    v_Tree_ID   INTEGER;
    v_Parent_ID INTEGER;
BEGIN
    -- Récupérer l’ID du menu créé
    SELECT AD_Menu_ID INTO p_Menu_ID
      FROM AD_Menu
     WHERE Name = 'Import Deux CSV';

    -- ID de l’arbre “Menu” (type MM)
    SELECT AD_Tree_ID INTO v_Tree_ID
      FROM AD_Tree
     WHERE TreeType = 'MM'
       AND Name = 'Menu';

    -- Chercher un parent “System Import” ou “Import”
    SELECT Node_ID INTO v_Parent_ID
      FROM AD_TreeNodeMM
     WHERE AD_Tree_ID = v_Tree_ID
       AND Node_ID IN (
           SELECT AD_Menu_ID
             FROM AD_Menu
            WHERE Name IN ('System Import','Import')
           )
     LIMIT 1;

    -- Si pas trouvé, prendre “Material Management”
    IF v_Parent_ID IS NULL THEN
        SELECT Node_ID INTO v_Parent_ID
          FROM AD_TreeNodeMM
         WHERE AD_Tree_ID = v_Tree_ID
           AND Node_ID = (
               SELECT AD_Menu_ID
                 FROM AD_Menu
                WHERE Name = 'Material Management'
               LIMIT 1
           );
    END IF;

    -- Insertion dans l’arbre
    INSERT INTO AD_TreeNodeMM
      (AD_Tree_ID, Node_ID, AD_Client_ID, AD_Org_ID,
       IsActive, Created, CreatedBy, Updated, UpdatedBy,
       Parent_ID, SeqNo)
    SELECT v_Tree_ID, p_Menu_ID, 0, 0,
           'Y', CURRENT_TIMESTAMP, 100, CURRENT_TIMESTAMP, 100,
           v_Parent_ID, COALESCE(MAX(SeqNo),0)+10
      FROM AD_TreeNodeMM
     WHERE Parent_ID = v_Parent_ID;
END $$;
