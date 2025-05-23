/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Base Tree Model.
 *	(see also MTree in project base)
 *	
 *  @author Jorg Janke
 *  @version $Id: MTree_Base.java,v 1.2 2006/07/30 00:58:37 jjanke Exp $
 */
public class MTree_Base extends X_AD_Tree implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -6785430530028279055L;

	/**
	 * 	Add Node to correct tree
	 *	@param ctx session context
	 *	@param treeType tree type
	 *	@param Record_ID id
	 *	@param trxName transaction
	 *	@return true if node added
	 */
	public static boolean addNode (Properties ctx, String treeType, int Record_ID, String trxName)
	{
		//	Get Tree
		int AD_Tree_ID = 0;
		MClient client = MClient.get(ctx);
		MClientInfo ci = client.getInfo();
		
		if (TREETYPE_Activity.equals(treeType))
			AD_Tree_ID = ci.getAD_Tree_Activity_ID();
		else if (TREETYPE_BoM.equals(treeType))
			throw new IllegalArgumentException("BoM Trees not supported");
		else if (TREETYPE_BPartner.equals(treeType))
			AD_Tree_ID = ci.getAD_Tree_BPartner_ID();
		else if (TREETYPE_Campaign.equals(treeType))
			AD_Tree_ID = ci.getAD_Tree_Campaign_ID();
		else if (TREETYPE_ElementValue.equals(treeType))
			throw new IllegalArgumentException("ElementValue cannot use this API");
		else if (TREETYPE_Menu.equals(treeType))
			AD_Tree_ID = ci.getAD_Tree_Menu_ID();
		else if (TREETYPE_Organization.equals(treeType))
			AD_Tree_ID = ci.getAD_Tree_Org_ID();
		else if (TREETYPE_Product.equals(treeType))
			AD_Tree_ID = ci.getAD_Tree_Product_ID();
		else if (TREETYPE_ProductCategory.equals(treeType))
			throw new IllegalArgumentException("Product Category Trees not supported");
		else if (TREETYPE_Project.equals(treeType))
			AD_Tree_ID = ci.getAD_Tree_Project_ID();
		else if (TREETYPE_SalesRegion.equals(treeType))
			AD_Tree_ID = ci.getAD_Tree_SalesRegion_ID();

		if (AD_Tree_ID == 0)
			throw new IllegalArgumentException("No Tree found");
		MTree_Base tree = MTree_Base.get(ctx, AD_Tree_ID, trxName);
		if (tree.get_ID() != AD_Tree_ID)
			throw new IllegalArgumentException("Tree found AD_Tree_ID=" + AD_Tree_ID);

		//	Insert Tree in correct tree
		boolean saved = false;
		if (TREETYPE_Menu.equals(treeType))
		{
			MTree_NodeMM node = new MTree_NodeMM (tree, Record_ID);
			saved = node.save();
		}
		else if  (TREETYPE_BPartner.equals(treeType))
		{
			MTree_NodeBP node = new MTree_NodeBP (tree, Record_ID);
			saved = node.save();
		}
		else if  (TREETYPE_Product.equals(treeType))
		{
			MTree_NodePR node = new MTree_NodePR (tree, Record_ID);
			saved = node.save();
		}
		else
		{
			MTree_Node node = new MTree_Node (tree, Record_ID);
			saved = node.save();
		}
		return saved;	
	}	//	addNode
		
	/**
	 * 	Get Node TableName
	 *	@param treeType tree type
	 *	@return node table name, e.g. AD_TreeNode
	 */
	public static String getNodeTableName(String treeType)
	{
		String	nodeTableName = "AD_TreeNode";
		if (TREETYPE_Menu.equals(treeType))
			nodeTableName += "MM";
		else if  (TREETYPE_BPartner.equals(treeType))
			nodeTableName += "BP";
		else if  (TREETYPE_Product.equals(treeType))
			nodeTableName += "PR";
		//
		else if  (TREETYPE_CMContainer.equals(treeType))
			nodeTableName += "CMC";
		else if  (TREETYPE_CMContainerStage.equals(treeType))
			nodeTableName += "CMS";
		else if  (TREETYPE_CMMedia.equals(treeType))
			nodeTableName += "CMM";
		else if  (TREETYPE_CMTemplate.equals(treeType))
			nodeTableName += "CMT";
		//
		else if  (TREETYPE_User1.equals(treeType))
			nodeTableName += "U1";
		else if  (TREETYPE_User2.equals(treeType))
			nodeTableName += "U2";
		else if  (TREETYPE_User3.equals(treeType))
			nodeTableName += "U3";
		else if  (TREETYPE_User4.equals(treeType))
			nodeTableName += "U4";
		return nodeTableName;
	}	//	getNodeTableName

	/**
	 * 	Get Source TableName
	 *	@param treeType tree type
	 *	@return source table name, e.g. AD_Org or null 
	 */
	public static String getSourceTableName(String treeType)
	{
		if (treeType == null)
			return null;
		String sourceTable = null;
		if (treeType.equals(TREETYPE_Menu))
			sourceTable = "AD_Menu";
		else if (treeType.equals(TREETYPE_Organization))
			sourceTable = "AD_Org";
		else if (treeType.equals(TREETYPE_Product))
			sourceTable = "M_Product";
		else if (treeType.equals(TREETYPE_ProductCategory))
			sourceTable = "M_Product_Category";
		else if (treeType.equals(TREETYPE_BoM))
			sourceTable = "M_BOM";
		else if (treeType.equals(TREETYPE_ElementValue))
			sourceTable = "C_ElementValue";
		else if (treeType.equals(TREETYPE_BPartner))
			sourceTable = "C_BPartner";
		else if (treeType.equals(TREETYPE_Campaign))
			sourceTable = "C_Campaign";
		else if (treeType.equals(TREETYPE_Project))
			sourceTable = "C_Project";
		else if (treeType.equals(TREETYPE_Activity))
			sourceTable = "C_Activity";
		else if (treeType.equals(TREETYPE_SalesRegion))
			sourceTable = "C_SalesRegion";
		//
		else if (treeType.equals(TREETYPE_CMContainer))
			sourceTable = "CM_Container";
		else if (treeType.equals(TREETYPE_CMContainerStage))
			sourceTable = "CM_CStage";
		else if (treeType.equals(TREETYPE_CMMedia))
			sourceTable = "CM_Media";
		else if (treeType.equals(TREETYPE_CMTemplate))
			sourceTable = "CM_Template";
		//	User Trees
		else if (treeType.equals(TREETYPE_User1) || 
				 treeType.equals(TREETYPE_User2) || 
				 treeType.equals(TREETYPE_User3) || 
				 treeType.equals(TREETYPE_User4))
			sourceTable = "C_ElementValue";
		
		return sourceTable;		
	}	//	getSourceTableName

	/**
	 * 	Get MTree_Base from Cache (immutable)
	 *	@param AD_Tree_ID id
	 *	@return MTree_Base
	 */
	public static MTree_Base get (int AD_Tree_ID)
	{
		return get(AD_Tree_ID, (String)null);
	}
	
	/**
	 * 	Get MTree_Base from Cache (immutable)
	 *	@param AD_Tree_ID id
	 *	@param trxName transaction
	 *	@return MTree_Base
	 */
	public static MTree_Base get (int AD_Tree_ID, String trxName)
	{
		return get(Env.getCtx(), AD_Tree_ID, trxName);
	}
	
	/**
	 * 	Get MTree_Base from Cache (immutable)
	 *	@param ctx context
	 *	@param AD_Tree_ID id
	 *	@param trxName transaction
	 *	@return MTree_Base
	 */
	public static MTree_Base get (Properties ctx, int AD_Tree_ID, String trxName)
	{
		Integer key = Integer.valueOf(AD_Tree_ID);
		MTree_Base retValue = s_cache.get (ctx, key, e -> new MTree_Base(ctx, e));
		if (retValue != null)
			return retValue;
		retValue = new MTree_Base (ctx, AD_Tree_ID, trxName);
		if (retValue.get_ID () == AD_Tree_ID)
		{
			s_cache.put (key, retValue, e -> new MTree_Base(Env.getCtx(), e));
			return retValue;
		}
		return null;
	}	//	get
	
	/**	Cache						*/
	private static ImmutableIntPOCache<Integer,MTree_Base> s_cache = new ImmutableIntPOCache<Integer,MTree_Base>(Table_Name, 10);
		
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_Tree_UU  UUID key
     * @param trxName Transaction
     */
    public MTree_Base(Properties ctx, String AD_Tree_UU, String trxName) {
        super(ctx, AD_Tree_UU, trxName);
		if (Util.isEmpty(AD_Tree_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param AD_Tree_ID id
	 *	@param trxName transaction
	 */
	public MTree_Base (Properties ctx, int AD_Tree_ID, String trxName)
	{
		super(ctx, AD_Tree_ID, trxName);
		if (AD_Tree_ID == 0)
			setInitialDefaults();
	}	//	MTree_Base

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setIsAllNodes (true);	//	complete tree
		setIsDefault(false);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MTree_Base (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MTree_Base

	/**
	 * 	Parent Constructor
	 *	@param client client
	 *	@param name name
	 *	@param treeType
	 */
	public MTree_Base (MClient client, String name, String treeType)
	{
		this (client.getCtx(), 0, client.get_TrxName());
		setClientOrg (client);
		setName (name);
		setTreeType (treeType);
	}	//	MTree_Base

	/**
	 * 	Full Constructor
	 *	@param ctx context
	 *	@param Name name
	 *	@param TreeType tree type
	 *	@param trxName transaction
	 */
	public MTree_Base (Properties ctx, String Name, String TreeType,  
		String trxName)
	{
		super(ctx, 0, trxName);
		setName (Name);
		setTreeType (TreeType);
		setIsAllNodes (true);	//	complete tree
		setIsDefault(false);
	}	//	MTree_Base

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MTree_Base(MTree_Base copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MTree_Base(Properties ctx, MTree_Base copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MTree_Base(Properties ctx, MTree_Base copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 *	Get Node TableName
	 *	@return node table name, e.g. AD_TreeNode
	 */
	public String getNodeTableName()
	{
		return getNodeTableName(getTreeType());
	}	//	getNodeTableName
	
	/**
	 * 	Get Source TableName (i.e. where to get the name and color)
	 * 	@param tableNameOnly if false return From clause (alias = t)
	 *	@return source table name, e.g. AD_Org or null
	 */
	public String getSourceTableName (boolean tableNameOnly)
	{
		String tableName = getSourceTableName(getTreeType());
		if (tableName == null)
		{
			if (getAD_Table_ID() > 0)
				tableName = MTable.getTableName(getCtx(), getAD_Table_ID());
		}
		if (tableNameOnly)
			return tableName;
		if ("M_Product".equals(tableName))
			return "M_Product t INNER JOIN M_Product_Category x ON (t.M_Product_Category_ID=x.M_Product_Category_ID)";
		if ("C_BPartner".equals(tableName))
			return "C_BPartner t INNER JOIN C_BP_Group x ON (t.C_BP_Group_ID=x.C_BP_Group_ID)";
		if ("AD_Org".equals(tableName))
			return "AD_Org t INNER JOIN AD_OrgInfo i ON (t.AD_Org_ID=i.AD_Org_ID) "
				+ "LEFT OUTER JOIN AD_OrgType x ON (i.AD_OrgType_ID=x.AD_OrgType_ID)";
		if ("C_Campaign".equals(tableName))
			return "C_Campaign t LEFT OUTER JOIN C_Channel x ON (t.C_Channel_ID=x.C_Channel_ID)";
		if (tableName != null)
			tableName += " t";
		return tableName;
	}	//	getSourceTableName
	
	/**
	 * 	Get fully qualified Name of Action/Color Column
	 *	@return NULL or Action or Color
	 */
	public String getActionColorName()
	{
		String tableName = getSourceTableName(getTreeType());
		if ("AD_Menu".equals(tableName))
			return "t.Action";
		if ("M_Product".equals(tableName) || "C_BPartner".equals(tableName) 
			|| "AD_Org".equals(tableName) || "C_Campaign".equals(tableName))
			return "x.AD_PrintColor_ID";
		return "NULL";
	}	//	getSourceTableName
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (!isActive() || !isAllNodes())
			setIsDefault(false);

		// AD_Table_ID and Parent_Column_ID only use for CustomTable tree type
		if (! TREETYPE_CustomTable.equals(getTreeType())) {
			setAD_Table_ID(-1);
			setParent_Column_ID(-1);
		}
		
		// Validate that source table must have IsSummary column
		String tableName = getSourceTableName(true);
		MTable table = MTable.get(getCtx(), tableName);
		if (! table.columnExistsInDB("IsSummary")) {
			log.saveError("Error", "IsSummary column required for tree tables"); 
			return false;
		}
		
		// Set IsTreeDrivenByValue and IsValueDisplayed to false if table doesn't have Value column 
		if (! table.columnExistsInDB("Value")) {
			if (isTreeDrivenByValue()) {
				// Value is mandatory column to have a tree driven by Value
				setIsTreeDrivenByValue(false);
			}
			if (isValueDisplayed()) {
				// Value is mandatory column to be displayed
				setIsValueDisplayed(false);
			}
		}

		return true;
	}	//	beforeSave
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		if (newRecord)	
		{
			// Create tree node record
			if (TREETYPE_BPartner.equals(getTreeType()))
			{
				MTree_NodeBP ndBP = new MTree_NodeBP(this, 0);
				ndBP.saveEx();
			}
			else if (TREETYPE_Menu.equals(getTreeType()))
			{
				MTree_NodeMM ndMM = new MTree_NodeMM(this, 0);
				ndMM.saveEx();
			}
			else if (TREETYPE_Product.equals(getTreeType()))
			{
				MTree_NodePR ndPR = new MTree_NodePR(this, 0);
				ndPR.saveEx();
			}
			else
			{
				MTree_Node nd = new MTree_Node(this, 0);
				nd.saveEx();
			}
		}
		
		return success;
	}	//	afterSave

	@Override
	public MTree_Base markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

	/** 
	 * Is load all tree nodes immediately
	 * @return true if tree will load all nodes immediately
	 */
	public static boolean isLoadAllNodesImmediately(int treeID, String trxName) {
		return DB.getSQLValueStringEx(trxName, "SELECT IsLoadAllNodesImmediately FROM AD_Tree WHERE AD_Tree_ID = ?", treeID).equals("Y");
	}

}	//	MTree_Base
