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
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Accounting Fact Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MFactAcct.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 *  
 *  @author Teo Sarca, http://www.arhipac.ro
 *  			<li>FR [ 2079083 ] Add MFactAcct.deleteEx method
 */
public class MFactAcct extends X_Fact_Acct
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = 5251847162314796574L;

	/**
	 * Delete Accounting
	 * @param AD_Table_ID table
	 * @param Record_ID record
	 * @param trxName transaction
	 * @return number of rows deleted or -1 for error
	 * @deprecated Since ADempiere 3.5.2a; please use {@link #deleteEx(int, int, String)} instead.
	 */
	@Deprecated
	public static int delete (int AD_Table_ID, int Record_ID, String trxName)
	{
		int no = -1;
		try {
			no = deleteEx(AD_Table_ID, Record_ID, trxName);
		}
		catch (DBException e) {
			s_log.log(Level.SEVERE, "failed: AD_Table_ID=" + AD_Table_ID + ", Record_ID" + Record_ID, e);
			no = -1;
		}
		return no;
	}	//	delete
	
	/**
	 * Delete Fact_Acct records via table and record id
	 * @param AD_Table_ID table
	 * @param Record_ID record
	 * @param trxName transaction
	 * @return number of rows deleted
	 * @throws DBException on database exception
	 */
	public static int deleteEx(int AD_Table_ID, int Record_ID, String trxName)
	throws DBException
	{
		// backup the posting records before delete them
		final String sqlInsert = "INSERT INTO T_Fact_Acct_History SELECT * FROM Fact_Acct WHERE AD_Table_ID=? AND Record_ID=?";
		int no = DB.executeUpdateEx(sqlInsert, new Object[]{AD_Table_ID, Record_ID}, trxName);
		if (no != 0)
			if (s_log.isLoggable(Level.INFO)) s_log.fine("insert - AD_Table_ID=" + AD_Table_ID + ", Record_ID=" + Record_ID + " - #" + no);
		
		// set the updated to current time - for house keeping purpose
		final String sqlUpdate = "UPDATE T_Fact_Acct_History SET Updated=? WHERE AD_Table_ID=? AND Record_ID=? AND Created=Updated";
		no = DB.executeUpdateEx(sqlUpdate.toString(), new Object[] {new Timestamp(System.currentTimeMillis()), AD_Table_ID, Record_ID}, trxName);
		if (no != 0)
			if (s_log.isLoggable(Level.INFO)) s_log.fine("update - AD_Table_ID=" + AD_Table_ID + ", Record_ID=" + Record_ID + " - #" + no);
				
		final String sql = "DELETE FROM Fact_Acct WHERE AD_Table_ID=? AND Record_ID=?";
		no = DB.executeUpdateEx(sql, new Object[]{AD_Table_ID, Record_ID}, trxName);
		if (s_log.isLoggable(Level.FINE)) s_log.fine("delete - AD_Table_ID=" + AD_Table_ID + ", Record_ID=" + Record_ID + " - #" + no);
		return no;
	}

	/**	Static Logger	*/
	private static CLogger	s_log	= CLogger.getCLogger (MFactAcct.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param Fact_Acct_UU  UUID key
     * @param trxName Transaction
     */
    public MFactAcct(Properties ctx, String Fact_Acct_UU, String trxName) {
        super(ctx, Fact_Acct_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param Fact_Acct_ID id
	 *	@param trxName transaction
	 */
	public MFactAcct (Properties ctx, int Fact_Acct_ID, String trxName)
	{
		super (ctx, Fact_Acct_ID, trxName);
	}	//	MFactAcct

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MFactAcct (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MFactAcct

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MFactAcct[");
		sb.append(get_ID()).append("-Acct=").append(getAccount_ID())
			.append(",Dr=").append(getAmtSourceDr()).append("|").append(getAmtAcctDr())
			.append(",Cr=").append(getAmtSourceCr()).append("|").append(getAmtAcctCr())
			.append(",C_Currency_ID=").append(getC_Currency_ID())
			.append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	Get MAccount from record
	 *	@return Valid Account Combination
	 */
	public MAccount getMAccount()
	{
		MAccount acct = MAccount.get (getCtx(), getAD_Client_ID(), getAD_Org_ID(),
			getC_AcctSchema_ID(), getAccount_ID(), getC_SubAcct_ID(),
			getM_Product_ID(), getC_BPartner_ID(), getAD_OrgTrx_ID(), 
			getC_LocFrom_ID(), getC_LocTo_ID(), getC_SalesRegion_ID(), 
			getC_Project_ID(), getC_Campaign_ID(), getC_Activity_ID(),
			getUser1_ID(), getUser2_ID(), getUserElement1_ID(), getUserElement2_ID(),
			get_TrxName());
		if (acct != null && acct.get_ID() == 0)
			acct.saveEx();
		return acct;
	}	//	getMAccount

	private final static String recordIdWhereClause = "AD_Table_ID=? AND Record_ID=? AND C_AcctSchema_ID=?";
	
	/**
	 * Create Fact_Acct query for table and record id
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param C_AcctSchema_ID
	 * @param trxName
	 * @return query
	 */
	public static final Query createRecordIdQuery(int AD_Table_ID, int Record_ID, int C_AcctSchema_ID, String trxName) {
		Query query = new Query(Env.getCtx(), Table_Name, recordIdWhereClause, trxName);
		return query.setParameters(AD_Table_ID, Record_ID, C_AcctSchema_ID);
	}
	
}	//	MFactAcct
