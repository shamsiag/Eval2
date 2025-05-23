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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;

import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Product Category Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MProductCategory.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 */
public class MProductCategory extends X_M_Product_Category implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 6444388652482234582L;

	/**
	 * 	Get from Cache (immutable)
	 *	@param M_Product_Category_ID id
	 *	@return category
	 */
	public static MProductCategory get (int M_Product_Category_ID)
	{
		return get(Env.getCtx(), M_Product_Category_ID);
	}
	
	/**
	 * 	Get from Cache (immutable)
	 *	@param ctx context
	 *	@param M_Product_Category_ID id
	 *	@return category
	 */
	public static MProductCategory get (Properties ctx, int M_Product_Category_ID)
	{
		Integer ii = Integer.valueOf(M_Product_Category_ID);
		MProductCategory retValue = s_cache.get(ctx, ii, e -> new MProductCategory(ctx, e));
		if (retValue != null)
			return retValue;
		retValue = new MProductCategory (ctx, M_Product_Category_ID, (String)null);
		if (retValue.get_ID () == M_Product_Category_ID)
		{
			s_cache.put (M_Product_Category_ID, retValue, e -> new MProductCategory(Env.getCtx(), e));
			return retValue;
		}
		return null;
	}	//	get
	
	/**
	 * 	Is Product in Category
	 *	@param M_Product_Category_ID category
	 *	@param M_Product_ID product
	 *	@return true if product is with M_Product_Category_ID
	 */
	public static boolean isCategory (int M_Product_Category_ID, int M_Product_ID)
	{
		if (M_Product_ID == 0 || M_Product_Category_ID == 0)
			return false;
		//	Look up
		Integer product = Integer.valueOf(M_Product_ID);
		Integer category = (Integer)s_products.get(product);
		if (category != null)
			return category.intValue() == M_Product_Category_ID;
		
		String sql = "SELECT M_Product_Category_ID FROM M_Product WHERE M_Product_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, M_Product_ID);
			rs = pstmt.executeQuery ();
			if (rs.next ())
				category = Integer.valueOf(rs.getInt(1));
		}
		catch (Exception e)
		{
			s_log.log(Level.SEVERE, sql, e); 
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		if (category != null)
		{
			//	TODO: LRU logic  
			s_products.put(product, category);
			//
			if (s_log.isLoggable(Level.FINE)) s_log.fine("M_Product_ID=" + M_Product_ID + "(" + category
				+ ") in M_Product_Category_ID=" + M_Product_Category_ID
				+ " - " + (category.intValue() == M_Product_Category_ID));
			return category.intValue() == M_Product_Category_ID;
		}
		s_log.log(Level.SEVERE, "Not found M_Product_ID=" + M_Product_ID);
		return false;
	}	//	isCategory
	
	/**	Category Cache				*/
	private static ImmutableIntPOCache<Integer,MProductCategory>	s_cache = new ImmutableIntPOCache<Integer,MProductCategory>(Table_Name, 20);
	/**	Product Cache				*/
	private static CCache<Integer,Integer> s_products = new CCache<Integer,Integer>(I_M_Product.Table_Name, Table_Name + "|M_Product", 100);
	/**	Static Logger	*/
	private static CLogger	s_log	= CLogger.getCLogger (MProductCategory.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_Product_Category_UU  UUID key
     * @param trxName Transaction
     */
    public MProductCategory(Properties ctx, String M_Product_Category_UU, String trxName) {
        super(ctx, M_Product_Category_UU, trxName);
		if (Util.isEmpty(M_Product_Category_UU))
			setInitialDefaults();
    }

	/**
	 * 	Default Constructor
	 *	@param ctx context
	 *	@param M_Product_Category_ID id
	 *	@param trxName transaction
	 */
	public MProductCategory (Properties ctx, int M_Product_Category_ID, String trxName)
	{
		super(ctx, M_Product_Category_ID, trxName);
		if (M_Product_Category_ID == 0)
			setInitialDefaults();
	}	//	MProductCategory

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setMMPolicy (MMPOLICY_FiFo);	// F
		setPlannedMargin (Env.ZERO);
		setIsDefault (false);
		setIsSelfService (true);	// Y
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MProductCategory(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProductCategory

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MProductCategory(MProductCategory copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MProductCategory(Properties ctx, MProductCategory copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MProductCategory(Properties ctx, MProductCategory copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (hasLoopInTree()) {
			log.saveError("Error", Msg.getMsg(getCtx(), "ProductCategoryLoopDetected"));
			return false;
		}
		
		return true;
	}	//	beforeSave

	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		// Create new accounting record (M_Product_Category_Acct)
		if (newRecord && success)
			insert_Accounting("M_Product_Category_Acct", "C_AcctSchema_Default", null);

		return success;
	}	//	afterSave

	/**
	 * 	FiFo Material Movement Policy
	 *	@return true if FiFo
	 */
	public boolean isFiFo()
	{
		return MMPOLICY_FiFo.equals(getMMPolicy());
	}	//	isFiFo
		
	/**
	 *	Loop detection of product category tree.
	 *  @return true if loop detected
	 */
	public boolean hasLoopInTree ()
	{
		int productCategoryId = getM_Product_Category_ID();
		int newParentCategoryId = getM_Product_Category_Parent_ID();
		//	get values
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String sql = " SELECT M_Product_Category_ID, M_Product_Category_Parent_ID FROM M_Product_Category";
		final Vector<SimpleTreeNode> categories = new Vector<SimpleTreeNode>(100);
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				if (rs.getInt(1) == productCategoryId)
					categories.add(new SimpleTreeNode(rs.getInt(1), newParentCategoryId));
				categories.add(new SimpleTreeNode(rs.getInt(1), rs.getInt(2)));
			}
 			if (hasLoop(newParentCategoryId, categories, productCategoryId))
				return true;
		} catch (SQLException e) {
			s_log.log(Level.SEVERE, sql, e);
			return true;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return false;
	}	//	hasLoopInTree
	
	/**
	 * Recursive search for parent nodes - climbs to the root.<br/>
	 * If there is a circle, it will comes back to the start node.
	 * @param parentCategoryId
	 * @param categories
	 * @param loopIndicatorId
	 * @return true if loop detected
	 */
	private boolean hasLoop(int parentCategoryId, Vector<SimpleTreeNode> categories, int loopIndicatorId) {
		final Iterator<SimpleTreeNode> iter = categories.iterator();
		boolean ret = false;
		while (iter.hasNext()) {
			SimpleTreeNode node = (SimpleTreeNode) iter.next();
			if(node.getNodeId()==parentCategoryId){
				if (node.getParentId()==0) {
					//root node, all fine
					return false;
				}
				if(node.getNodeId()==loopIndicatorId){
					//loop found
					return true;
				}
				ret = hasLoop(node.getParentId(), categories, loopIndicatorId);
			}
		}
		return ret;
	}	//hasLoop

	@Override
	public MProductCategory markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

	/**
	 * Value object class for tree nodes.
	 * @author Karsten Thiemann, kthiemann@adempiere.org
	 *
	 */
	private static class SimpleTreeNode {
		/** id of the node */
		private int nodeId;
		/** id of the nodes parent */
		private int parentId;

		/**
		 * Constructor.
		 * @param nodeId
		 * @param parentId
		 */
		public SimpleTreeNode(int nodeId, int parentId) {
			this.nodeId = nodeId;
			this.parentId = parentId;
		}

		public int getNodeId() {
			return nodeId;
		}

		public int getParentId() {
			return parentId;
		}
	}
		
}	//	MProductCategory