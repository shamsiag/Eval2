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
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.model.X_M_Cost;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

/**
 * Form to Merge source/from record to target/to record
 */
public class Merge 
{
	/**	Window No			*/
	protected int         	m_WindowNo = 0;
	/**	Total Count			*/
	protected int				m_totalCount = 0;
	/** Error Log			*/
	protected StringBuffer	m_errorLog = new StringBuffer();
	private Trx 			m_trx = null;         
	/**	Logger			*/
	protected static final CLogger log = CLogger.getCLogger(Merge.class);

	public static String	AD_ORG_ID = "AD_Org_ID";
	public static String	C_BPARTNER_ID = "C_BPartner_ID";
	public static String	AD_USER_ID = "AD_User_ID";
	public static String	M_PRODUCT_ID = "M_Product_ID";

	/** Tables to delete (not update) for AD_Org	*/
	protected static String[]	s_delete_Org = new String[]
		{"AD_OrgInfo", "AD_Role_OrgAccess"};
	/** Tables to delete (not update) for AD_User	*/
	protected static String[]	s_delete_User = new String[]
		{"AD_User_Roles"};
	/** Tables to delete (not update) for C_BPartner	*/
	protected static String[]	s_delete_BPartner = new String[]
		{"C_BP_Employee_Acct", "C_BP_Vendor_Acct", "C_BP_Customer_Acct", 
		"T_Aging"};
	/** Tables to delete (not update) for M_Product		*/
	protected static String[]	s_delete_Product = new String[]
		{"M_Product_PO", "M_Replenish", "T_Replenish", 
		"M_ProductPrice", 
		"M_Cost", // teo_sarca [ 1704554 ]
		"M_Product_Trl", "M_Product_Acct"};

	protected String[]	m_columnName = null;
	protected String[]	m_deleteTables = null;
	
	/**
	 * Determine the list of tables to delete records by key columnName 
	 * @param columnName
	 */
	protected void updateDeleteTable(String columnName)
	{
		//	** Update **
		if (columnName.equals(AD_ORG_ID))
			m_deleteTables = s_delete_Org;
		else if (columnName.equals(AD_USER_ID))
			m_deleteTables = s_delete_User;
		else if (columnName.equals(C_BPARTNER_ID))
			m_deleteTables = s_delete_BPartner;
		else if (columnName.equals(M_PRODUCT_ID))
			m_deleteTables = s_delete_Product;
	} 


	/**
	 * 	Execute Merge.
	 *	@param ColumnName ID column (M_Product_ID, AD_Org_ID, C_BPartner_ID or AD_User_ID)
	 *	@param from_ID from id
	 *	@param to_ID to id
	 *	@return true if merged
	 */
	public boolean merge (String ColumnName, int from_ID, int to_ID)
	{
		String TableName = ColumnName.substring(0, ColumnName.length()-3);
		if (log.isLoggable(Level.CONFIG)) log.config(ColumnName
			+ " - From=" + from_ID + ",To=" + to_ID);

		updateDeleteTable(ColumnName);
		
		boolean success = true;
		m_totalCount = 0;
		m_errorLog = new StringBuffer();
		String sql = "SELECT t.TableName, c.ColumnName "
			+ "FROM AD_Table t"
			+ " INNER JOIN AD_Column c ON (t.AD_Table_ID=c.AD_Table_ID) "
			+ "WHERE t.IsView='N'"
				+ " AND t.TableName NOT IN ('C_TaxDeclarationAcct')"
				+ " AND ("
				+ "(c.ColumnName=? AND c.IsKey='N')"		//	#1 - direct
			+ " OR "
				+ "c.AD_Reference_Value_ID IN "				//	Table Reference
					+ "(SELECT rt.AD_Reference_ID FROM AD_Ref_Table rt"
					+ " INNER JOIN AD_Column cc ON (rt.AD_Table_ID=cc.AD_Table_ID AND rt.AD_Key=cc.AD_Column_ID) "
					+ "WHERE cc.IsKey='Y' AND cc.ColumnName=?)"	//	#2
			+ ") AND c.ColumnSQL IS NULL "
			+ "ORDER BY t.LoadSeq DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			m_trx = Trx.get(Trx.createTrxName("merge"), true);
			m_trx.setDisplayName(getClass().getName()+"_merge");
			//
			pstmt = DB.prepareStatement(sql, m_trx.getTrxName());
			pstmt.setString(1, ColumnName);
			pstmt.setString(2, ColumnName);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				String tName = rs.getString(1);
				String cName = rs.getString(2);
				if (!TableName.equals(tName))	//	to be sure - sql should prevent it
				{
					int count = mergeTable (tName, cName, from_ID, to_ID);
					if (count < 0)
						success = false;
					else
						m_totalCount += count;
				}
			}
			//
			if (log.isLoggable(Level.CONFIG)) log.config("Success=" + success
				+ " - " + ColumnName + " - From=" + from_ID + ",To=" + to_ID);
			if (success)
			{
				sql = "DELETE FROM " + TableName + " WHERE " + ColumnName + "=" + from_ID;
				
				if ( DB.executeUpdateEx(sql, m_trx.getTrxName()) < 0 )
				{
					m_errorLog.append(Env.NL).append("DELETE FROM ").append(TableName)
					.append(" - ");
				    success = false;
					if (log.isLoggable(Level.CONFIG)) log.config(m_errorLog.toString());
					m_trx.rollback();
					return false;
				}
				
			}
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, ColumnName, ex);
			success = false;
		}
		finally
		{
			//
			if (m_trx != null) {
				if (success)
					success = m_trx.commit();
				else
					m_trx.rollback();
				m_trx.close();
			}
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//	Cleanup
		return success;
	}	//	merge

	/**
	 * 	Execute Merge for record in a Table
	 * 	@param TableName table name
	 * 	@param ColumnName key column name
	 * 	@param from_ID from id
	 * 	@param to_ID to id
	 * 	@return -1 for error or number of records updated
	 */
	protected int mergeTable (String TableName, String ColumnName, int from_ID, int to_ID)
	{
		if (log.isLoggable(Level.FINE)) log.fine(TableName + "." + ColumnName + " - From=" + from_ID + ",To=" + to_ID);
		String sql = "UPDATE " + TableName
			+ " SET " + ColumnName + "=" + to_ID
			+ " WHERE " + ColumnName + "=" + from_ID;
		boolean delete = false;
		for (int i = 0; i < m_deleteTables.length; i++)
		{
			if (m_deleteTables[i].equals(TableName))
			{
				delete = true;
				sql = "DELETE FROM " + TableName + " WHERE " + ColumnName + "=" + from_ID;
			}
		}
		// Delete newly created MCost records - teo_sarca [ 1704554 ]
		if (delete && X_M_Cost.Table_Name.equals(TableName) && M_PRODUCT_ID.equals(ColumnName))
		{
			sql += " AND " + X_M_Cost.COLUMNNAME_CurrentCostPrice + "=0"
				+ " AND " + X_M_Cost.COLUMNNAME_CurrentQty + "=0"
				+ " AND " + X_M_Cost.COLUMNNAME_CumulatedAmt + "=0"
				+ " AND " + X_M_Cost.COLUMNNAME_CumulatedQty + "=0";
		}

		int count = DB.executeUpdateEx(sql, m_trx.getTrxName());
		if (count < 0)
		{
			count = -1;
			m_errorLog.append(Env.NL)
				.append(delete ? "DELETE FROM " : "UPDATE ")
				.append(TableName).append(" - ")
				.append(" - ").append(sql);
			if (log.isLoggable(Level.CONFIG)) log.config(m_errorLog.toString());
			m_trx.rollback();
		}
		if (log.isLoggable(Level.FINE)) log.fine(count
				+ (delete ? " -Delete- " : " -Update- ") + TableName);

		return count;
	}	//	mergeTable

	/**
	 * 	Post Merge Operations
	 *	@param ColumnName key column name
	 *	@param to_ID to id
	 */
	public void postMerge (String ColumnName, int to_ID)
	{
		if (ColumnName.equals(AD_ORG_ID))
		{
			
		}
		else if (ColumnName.equals(AD_USER_ID))
		{
			
		}
		else if (ColumnName.equals(C_BPARTNER_ID))
		{
			MBPartner bp = new MBPartner (Env.getCtx(), to_ID, null);
			if (bp.get_ID() != 0)
			{
				MPayment[] payments = MPayment.getOfBPartner(Env.getCtx(), bp.getC_BPartner_ID(), null);
				for (int i = 0; i < payments.length; i++) 
				{
					MPayment payment = payments[i];
					if (payment.testAllocation())
						payment.saveEx();
				}
				MInvoice[] invoices = MInvoice.getOfBPartner(Env.getCtx(), bp.getC_BPartner_ID(), null);
				for (int i = 0; i < invoices.length; i++) 
				{
					MInvoice invoice = invoices[i];
					if (invoice.testAllocation())
						invoice.saveEx();
				}
				bp.setTotalOpenBalance();
				bp.setActualLifeTimeValue();
				bp.saveEx();
			}
		}
		else if (ColumnName.equals(M_PRODUCT_ID))
		{
			
		}
	}	//	postMerge
}
