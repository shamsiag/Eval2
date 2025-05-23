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
package org.compiere.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.MTree;
import org.compiere.model.MTree_Base;
import org.compiere.model.MTree_Node;
import org.compiere.model.MTree_NodeBP;
import org.compiere.model.MTree_NodeMM;
import org.compiere.model.MTree_NodePR;
import org.compiere.model.PO;
import org.compiere.util.DB;

/**
 *	Tree Maintenance	
 *	
 *  @author Jorg Janke
 *  @version $Id: TreeMaintenance.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
@org.adempiere.base.annotation.Process
public class TreeMaintenance extends SvrProcess
{
	/**	Tree				*/
	private int		m_AD_Tree_ID;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			if (para[i].getParameter() == null)
				;
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
		m_AD_Tree_ID = getRecord_ID();		//	from Window
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("AD_Tree_ID=" + m_AD_Tree_ID);
		if (m_AD_Tree_ID == 0)
			throw new IllegalArgumentException("Tree_ID = 0");
		MTree tree = new MTree (getCtx(), m_AD_Tree_ID, get_TrxName());	
		if (tree == null || tree.getAD_Tree_ID() == 0)
			throw new IllegalArgumentException("No Tree -" + tree);
		//
		if (MTree.TREETYPE_BoM.equals(tree.getTreeType()))
			return "BOM Trees not implemented";
		return verifyTree(tree);
	}	//	doIt

	/**
	 *  Verify Tree
	 * 	@param tree tree
	 */
	private String verifyTree (MTree_Base tree)
	{
		String nodeTableName = tree.getNodeTableName();
		String sourceTableName = tree.getSourceTableName(true);
		String sourceTableKey = sourceTableName + "_ID";
		int AD_Client_ID = tree.getAD_Client_ID();
		int C_Element_ID = 0;
		if (MTree.TREETYPE_ElementValue.equals(tree.getTreeType()))
		{
			StringBuilder sql = new StringBuilder("SELECT C_Element_ID FROM C_Element ")
				.append("WHERE AD_Tree_ID=").append(tree.getAD_Tree_ID());
			C_Element_ID = DB.getSQLValue(null, sql.toString());
			if (C_Element_ID <= 0)
				throw new IllegalStateException("No Account Element found");
		}
		
		//	Delete unused
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM ").append(nodeTableName)
			.append(" WHERE AD_Tree_ID=").append(tree.getAD_Tree_ID())
			.append(" AND Node_ID NOT IN (SELECT ").append(sourceTableKey)
			.append(" FROM ").append(sourceTableName)
			.append(" WHERE AD_Client_ID=").append(AD_Client_ID);
		if (C_Element_ID > 0)
			sql.append(" AND C_Element_ID=").append(C_Element_ID);
		sql.append(")");
		if (log.isLoggable(Level.FINER)) log.finer(sql.toString());
		//
		int deletes = DB.executeUpdate(sql.toString(), get_TrxName());
		addLog(0,null, new BigDecimal(deletes), tree.getName()+ " Deleted");
		if (!tree.isAllNodes()){
			StringBuilder msgreturn = new StringBuilder().append(tree.getName()).append(" OK");
			return msgreturn.toString();
		}
		//	Insert new
		int inserts = 0;
		sql = new StringBuilder();
		sql.append("SELECT ").append(sourceTableKey)
			.append(" FROM ").append(sourceTableName)
			.append(" WHERE AD_Client_ID=").append(AD_Client_ID);
		if (C_Element_ID > 0)
			sql.append(" AND C_Element_ID=").append(C_Element_ID);
		sql.append(" AND ").append(sourceTableKey)
			.append("  NOT IN (SELECT Node_ID FROM ").append(nodeTableName)
			.append(" WHERE AD_Tree_ID=").append(tree.getAD_Tree_ID()).append(")");
		if (log.isLoggable(Level.FINER)) log.finer(sql.toString());
		//
		boolean ok = true;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				int Node_ID = rs.getInt(1);
				PO node = null;
				if (nodeTableName.equals("AD_TreeNode"))
					node = new MTree_Node(tree, Node_ID);
				else if (nodeTableName.equals("AD_TreeNodeBP"))
					node = new MTree_NodeBP(tree, Node_ID);
				else if (nodeTableName.equals("AD_TreeNodePR"))
					node = new MTree_NodePR(tree, Node_ID);
				else if (nodeTableName.equals("AD_TreeNodeMM"))
					node = new MTree_NodeMM(tree, Node_ID);
				//				
				if (node == null)
					log.log(Level.SEVERE, "No Model for " + nodeTableName);
				else
				{
					if (node.save())
						inserts++;
					else
						log.log(Level.SEVERE, "Could not add to " + tree + " Node_ID=" + Node_ID);
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "verifyTree", e);
			ok = false;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	Driven by Value
		if (tree.isTreeDrivenByValue()) {
			sql = new StringBuilder();
			sql.append("SELECT ").append(sourceTableKey)
				.append(" FROM ").append(sourceTableName)
				.append(" WHERE AD_Client_ID=").append(AD_Client_ID);
			if (C_Element_ID > 0)
				sql.append(" AND C_Element_ID=").append(C_Element_ID);
			if (log.isLoggable(Level.FINER)) log.finer(sql.toString());
			//
			MTable table = MTable.get(getCtx(), sourceTableName);
			try
			{
				pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					int Node_ID = rs.getInt(1);
					PO rec = table.getPO(Node_ID, get_TrxName());
					rec.update_Tree(tree.getTreeType());
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "verifyTree", e);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}
		}

		StringBuilder msglog = new StringBuilder().append(tree.getName()).append(" Inserted");
		addLog(0,null, new BigDecimal(inserts), msglog.toString());
		StringBuilder msgreturn = new StringBuilder().append(tree.getName()).append((ok ? " OK" : " Error"));
		return msgreturn.toString();
	}	//	verifyTree

}	//	TreeMaintenence
