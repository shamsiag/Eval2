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
package org.compiere.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.tree.TreeNode;

import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MHierarchy;
import org.compiere.model.MRole;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Financial Report Tree Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MReportTree.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 */
public class MReportTree
{
	/**
	 * 	Get Report Tree (cached)
	 *	@param ctx context
	 *	@param PA_Hierarchy_ID optional hierarchy
	 *	@param ElementType Account Schema Element Type
	 *	@return tree
	 */
	public static MReportTree get (Properties ctx, int PA_Hierarchy_ID, String ElementType)
	{	
		MRole role = MRole.getDefault();
		String key = Env.getAD_Client_ID(ctx) + "_" + role.getAD_Role_ID() + "_" + PA_Hierarchy_ID + "_" + ElementType;
		if (!role.isAccessAllOrgs() && role.isUseUserOrgAccess() ) 
		{
			key = Env.getAD_Client_ID(ctx) + "_" + Env.getAD_User_ID(ctx) + "_" + role.getAD_Role_ID() + "_" + PA_Hierarchy_ID + "_" + ElementType;
		}
		
		MReportTree tree = (MReportTree)s_trees.get(key);
		if (tree == null)
		{
			tree = new MReportTree (ctx, PA_Hierarchy_ID, ElementType);
			s_trees.put(key, tree);
		}
		return tree;	
	}	//	get

	/**
	 * 	Get Where Clause
	 *	@param ctx context
	 *	@param PA_Hierarchy_ID optional hierarchy
	 *	@param ElementType Account Schema Element Type
	 *	@param ID leaf element id
	 *	@return where clause
	 */
	public static String getWhereClause (Properties ctx,
		int PA_Hierarchy_ID, String ElementType, int ID)
	{
		MReportTree tree = get (ctx, PA_Hierarchy_ID, ElementType);
		return tree.getWhereClause(ID);	
	}	//	getWhereClause

	/**
	 * 	Get Child IDs
	 *	@param ctx context
	 *	@param PA_Hierarchy_ID optional hierarchie
	 *	@param ElementType Account Schema Element Type
	 *	@param ID id
	 *	@return array of IDs
	 */
	public static Integer[] getChildIDs (Properties ctx,
		int PA_Hierarchy_ID, String ElementType, int ID)
	{
		MReportTree tree = get (ctx, PA_Hierarchy_ID, ElementType);
		return tree.getChildIDs(ID);	
	}	//	getChildIDs

	/**	Map with Tree				*/
	private static CCache<String,MReportTree> s_trees = new CCache<String,MReportTree>(null, "MReportTree", 20, false);

	/**
	 * @param ctx
	 * @param PA_Hierarchy_ID
	 * @param ElementType
	 */
	public MReportTree (Properties ctx, int PA_Hierarchy_ID, String ElementType)
	{
		this(ctx, PA_Hierarchy_ID, false, ElementType);
	}
	
	/**
	 * 	Report Tree
	 *	@param ctx context
	 *	@param PA_Hierarchy_ID optional hierarchy
	 *  @param allNodes true to always get full tree
	 *	@param ElementType Account Schema Element Type
	 */
	public MReportTree (Properties ctx, int PA_Hierarchy_ID, boolean allNodes, String ElementType)
	{
		m_ElementType = ElementType;
		m_TreeType = m_ElementType;
		if (MAcctSchemaElement.ELEMENTTYPE_Account.equals(m_ElementType)
			|| MAcctSchemaElement.ELEMENTTYPE_UserElementList1.equals(m_ElementType)
			|| MAcctSchemaElement.ELEMENTTYPE_UserElementList2.equals(m_ElementType) )
			m_TreeType = MTree.TREETYPE_ElementValue;
		if (MAcctSchemaElement.ELEMENTTYPE_OrgTrx.equals(m_ElementType))
				m_TreeType = MTree.TREETYPE_Organization;
		m_PA_Hierarchy_ID = PA_Hierarchy_ID;
		m_ctx = ctx;
		//
		int AD_Tree_ID = getAD_Tree_ID();
		//	Not found
		if (AD_Tree_ID == 0)
			throw new IllegalArgumentException("No AD_Tree_ID for TreeType=" + m_TreeType 
				+ ", PA_Hierarchy_ID=" + PA_Hierarchy_ID);
		//
		boolean clientTree = true;
		m_tree = new MTree (ctx, AD_Tree_ID, true, clientTree, allNodes, null);  // include inactive and empty summary nodes
		// remove summary nodes without children
		m_tree.trimTree();
	}	//	MReportTree

	/** Optional Hierarchy		*/
	private int			m_PA_Hierarchy_ID = 0;
	/**	Element Type			*/
	private String		m_ElementType = null;
	/** Context					*/
	private Properties	m_ctx = null;
	/** Tree Type				*/
	private String		m_TreeType = null;
	/**	The Tree				*/
	private MTree 		m_tree = null;
	/**	Logger					*/
	private CLogger		log = CLogger.getCLogger(getClass());
	
	/**
	 * 	Get AD_Tree_ID 
	 *	@return AD_Tree_ID
	 */
	protected int getAD_Tree_ID ()
	{
		if (m_PA_Hierarchy_ID == 0)
			return getDefaultAD_Tree_ID();

		MHierarchy hierarchy = MHierarchy.get(m_ctx, m_PA_Hierarchy_ID);
		int AD_Tree_ID = hierarchy.getAD_Tree_ID (m_TreeType);

		if (AD_Tree_ID == 0)
			return getDefaultAD_Tree_ID();
		
		return AD_Tree_ID;
	}	//	getAD_Tree_ID
	
	/**
	 * 	Get Default AD_Tree_ID 
	 * 	@see MTree#getDefaultAD_Tree_ID
	 *	@return default AD_Tree_ID
	 */
	protected int getDefaultAD_Tree_ID()
	{
		int AD_Tree_ID = 0;
		int AD_Client_ID = Env.getAD_Client_ID(m_ctx);
		
		String sql = "SELECT AD_Tree_ID, Name FROM AD_Tree "
			+ "WHERE AD_Client_ID=? AND TreeType=? AND IsActive='Y' AND IsAllNodes='Y' "
			+ "ORDER BY IsDefault DESC, AD_Tree_ID";	//	assumes first is primary tree
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, AD_Client_ID);
			pstmt.setString(2, m_TreeType);
			rs = pstmt.executeQuery();
			if (rs.next())
				AD_Tree_ID = rs.getInt(1);
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return AD_Tree_ID;
	}	//	getDefaultAD_Tree_ID

	/**
	 * 	Get Account Schema Element Type
	 *	@return element Type
	 */
	public String getElementType()
	{
		return m_ElementType;
	}	//	getElementType
	
	/**
	 * 	Get Tree Type
	 *	@return tree type
	 */
	public String getTreeType()
	{
		return m_TreeType;
	}	//	getTreeType
	
	/**
	 * 	Get Tree
	 *	@return tree
	 */
	public MTree getTree()
	{
		return m_tree;
	}	//	getTreeType

	/**
	 * 	Get Where Clause
	 *	@param ID start node id
	 *	@return ColumnName = 1 or ( ColumnName = 1 OR ColumnName = 2 OR ColumnName = 3)
	 */	
	public String getWhereClause (int ID)
	{
		if (log.isLoggable(Level.FINE)) log.fine("(" + m_ElementType + ") ID=" + ID);
		String ColumnName = MAcctSchemaElement.getColumnName(m_ElementType);
		//
		MTreeNode node = m_tree.getRoot().findNode(ID);
		if (log.isLoggable(Level.FINEST)) log.finest("Root=" + node);
		//
		StringBuilder result = null;
		if (node != null && node.isSummary ())
		{
			Enumeration<TreeNode> en = node.preorderEnumeration ();
			StringBuilder sb = new StringBuilder ();
			while (en.hasMoreElements ())
			{
				MTreeNode nn = (MTreeNode)en.nextElement ();
				if (!nn.isSummary ())
				{
					if (sb.length () > 0)
					{
						sb.append (" OR ");
					}
					sb.append (ColumnName);
					sb.append ('=');
					sb.append (nn.getNode_ID ());
					if (log.isLoggable(Level.FINEST)) log.finest ("- " + nn);
				}
				else
					if (log.isLoggable(Level.FINEST)) log.finest ("- skipped parent (" + nn + ")");
			}
			result = new StringBuilder (" ( ");
			result.append (sb);
			result.append (" ) ");
		}
		else	//	not found or not summary 
			result = new StringBuilder (ColumnName).append("=").append(ID);
		//
		if (log.isLoggable(Level.FINEST)) log.finest(result.toString());
		return result.toString();
	}	//	getWhereClause

	/**
	 * 	Get Child IDs
	 *	@param ID start node
	 *	@return array of child IDs
	 */	
	public Integer[] getChildIDs (int ID)
	{
		if (log.isLoggable(Level.FINE)) log.fine("(" + m_ElementType + ") ID=" + ID);
		ArrayList<Integer> list = new ArrayList<Integer>(); 
		//
		MTreeNode node = m_tree.getRoot().findNode(ID);
		if (log.isLoggable(Level.FINEST)) log.finest("Root=" + node);
		//
		if (node != null && node.isSummary())
		{
			Enumeration<?> en = node.preorderEnumeration();
			while (en.hasMoreElements())
			{
				MTreeNode nn = (MTreeNode)en.nextElement();
				if (!nn.isSummary())
				{
					list.add(Integer.valueOf(nn.getNode_ID()));
					if (log.isLoggable(Level.FINEST)) log.finest("- " + nn);
				}
				else
					if (log.isLoggable(Level.FINEST)) log.finest("- skipped parent (" + nn + ")");
			}
		}
		else	//	not found or not summary 
			list.add(Integer.valueOf(ID));
		//
		Integer[] retValue = new Integer[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getWhereClause
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("MReportTree[ElementType=");
		sb.append(m_ElementType).append(",TreeType=").append(m_TreeType)
			.append(",").append(m_tree)
			.append("]");
		return sb.toString();
	}	//	toString

}	//	MReportTree
