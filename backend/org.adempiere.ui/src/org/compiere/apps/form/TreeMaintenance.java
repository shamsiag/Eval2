/******************************************************************************
 * Copyright (C) 2009 Low Heng Sin                                            *
 * Copyright (C) 2009 Idalica Corporation                                     *
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
package org.compiere.apps.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;

import org.compiere.model.MRole;
import org.compiere.model.MTree;
import org.compiere.model.MTree_Node;
import org.compiere.model.MTree_NodeBP;
import org.compiere.model.MTree_NodeMM;
import org.compiere.model.MTree_NodePR;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/**
 * Tree maintenance form.
 */
public class TreeMaintenance {

	/**	Window No				*/
	public int         	m_WindowNo = 0;
	/**	Active Tree				*/
	public MTree		 	m_tree;
	/**	Logger			*/
	public static final CLogger log = CLogger.getCLogger(TreeMaintenance.class);
	
	/**
	 * Get tree (AD_Tree) records.
	 * @return tree (AD_Tree) records.
	 */
	public KeyNamePair[] getTreeData()
	{
		return DB.getKeyNamePairsEx(MRole.getDefault().addAccessSQL(
				"SELECT AD_Tree_ID, Name FROM AD_Tree WHERE IsActive='Y' AND TreeType NOT IN ('BB','PC') ORDER BY 2", 
				"AD_Tree", MRole.SQL_NOTQUALIFIED, MRole.SQL_RW), false);
	}
	
	/**
	 * Get tree nodes
	 * @return tree nodes
	 */
	public ArrayList<ListItem> getTreeItemData()
	{
		ArrayList<ListItem> data = new ArrayList<ListItem>();
		
		String fromClause = m_tree.getSourceTableName(false);	//	fully qualified
		String columnNameX = m_tree.getSourceTableName(true);
		String actionColor = m_tree.getActionColorName();
		String fieldName = null;
		String fieldDescription = null;
		String join = null;
		if (m_tree.getTreeType().equals(MTree.TREETYPE_Menu) // IDEMPIERE-1581 (see MTree.getNodeDetails)
			&& ! Env.isBaseLanguage(Env.getCtx(), "AD_Menu")) {
			fieldName = "trl.Name";
			fieldDescription ="trl.Description";
			join = " LEFT JOIN AD_Menu_Trl trl ON (t.AD_Menu_ID = trl.AD_Menu_ID AND trl.AD_Language='"
				+ Env.getAD_Language(Env.getCtx()) + "')";
		} else {
			fieldName ="t.Name";
			fieldDescription ="t.Description";
			join = "";
		}

		StringBuilder sqlb = new StringBuilder("SELECT t.")
			.append(columnNameX) 
			.append("_ID,").append(fieldName).append(",").append(fieldDescription).append(",t.IsSummary,")
			.append(actionColor)
			.append(" FROM ").append(fromClause).append(join)
			.append(" ORDER BY 2");
		String sql = MRole.getDefault().addAccessSQL(sqlb.toString(),
			"t", MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);
		if (log.isLoggable(Level.CONFIG)) log.config(sql);
		//	
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				ListItem item = new ListItem(rs.getInt(1), rs.getString(2),
					rs.getString(3), "Y".equals(rs.getString(4)), rs.getString(5));
				data.add(item);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		
		return data;
	}
	
	/**
	 * 	Action: Add Node to Tree
	 * 	@param item item
	 */
	public void addNode(ListItem item)
	{
		if (item != null)
		{
			//	May cause Error if in tree
			if (m_tree.isProduct())
			{
				MTree_NodePR node = new MTree_NodePR (m_tree, item.id);
				node.saveEx();
			}
			else if (m_tree.isBPartner())
			{
				MTree_NodeBP node = new MTree_NodeBP (m_tree, item.id);
				node.saveEx();
			}
			else if (m_tree.isMenu())
			{
				MTree_NodeMM node = new MTree_NodeMM (m_tree, item.id);
				node.saveEx();
			}
			else
			{
				MTree_Node node = new MTree_Node (m_tree, item.id);
				node.saveEx();
			}
		}
	}	//	action_treeAdd
	
	/**
	 * 	Action: Delete Node from Tree
	 * 	@param item item
	 */
	public void deleteNode(ListItem item)
	{
		if (item != null)
		{
			if (m_tree.isProduct())
			{
				MTree_NodePR node = MTree_NodePR.get (m_tree, item.id);
				if (node != null)
					node.delete(true);
			}
			else if (m_tree.isBPartner())
			{
				MTree_NodeBP node = MTree_NodeBP.get (m_tree, item.id);
				if (node != null)
					node.delete(true);
			}
			else if (m_tree.isMenu())
			{
				MTree_NodeMM node = MTree_NodeMM.get (m_tree, item.id);
				if (node != null)
					node.delete(true);
			}
			else
			{
				MTree_Node node = MTree_Node.get (m_tree, item.id);
				if (node != null)
					node.delete(true);
			}
		}
	}	//	action_treeDelete
	
	/**
	 * 	Tree List Item
	 */
	public static class ListItem
	{
		/**
		 * 	ListItem
		 *	@param ID
		 *	@param Name
		 *	@param Description
		 *	@param summary
		 *	@param ImageIndicator
		 */
		public ListItem (int ID, String Name, String Description, 
			boolean summary, String ImageIndicator)
		{
			id = ID;
			name = Name;
			description = Description;
			isSummary = summary;
			imageIndicator = ImageIndicator;
		}	//	ListItem
		
		/**	ID			*/
		public int id;
		/** Name		*/
		public String name;
		/** Description	*/
		public String description;
		/** Summary		*/
		public boolean isSummary;
		/** Indicator	*/
		public String imageIndicator;  //  Menu - Action
		
		/**
		 * 	To String
		 *	@return	String Representation
		 */
		public String toString ()
		{
			String retValue = name;
			if (description != null && description.length() > 0)
				retValue += " (" + description + ")";
			return retValue;
		}	//	toString
		
	}	//	ListItem
}
