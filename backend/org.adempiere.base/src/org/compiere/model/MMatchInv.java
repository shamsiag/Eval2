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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 *	Match Invoice (Receipt&lt;&gt;Invoice) Model.
 *  <pre>
 *  Accounting:
 *  - Not Invoiced Receipts (relief)
 *  - IPV
 *	</pre>
 *  @author Jorg Janke
 *  @version $Id: MMatchInv.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 * 
 *  @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>BF [ 1926113 ] MMatchInv.getNewerDateAcct() should work in trx
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see https://sourceforge.net/p/adempiere/feature-requests/631/
 *  @author Bayu Cahya, Sistematika
 * 			<li>BF [ 2240484 ] Re MatchingPO, MMatchPO doesn't contains Invoice info
 */
public class MMatchInv extends X_M_MatchInv
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -6673764788466220541L;

	/**
	 * 	Get InOut-Invoice Matches
	 *	@param ctx context
	 *	@param M_InOutLine_ID shipment
	 *	@param C_InvoiceLine_ID invoice
	 *	@param trxName transaction
	 *	@return array of matches
	 */
	public static MMatchInv[] get (Properties ctx, int M_InOutLine_ID, int C_InvoiceLine_ID, String trxName)
	{
		if (M_InOutLine_ID <= 0 || C_InvoiceLine_ID <= 0)
			return new MMatchInv[]{};
		//
		final String whereClause = "M_InOutLine_ID=? AND C_InvoiceLine_ID=?";
		List<MMatchInv> list = new Query(ctx, I_M_MatchInv.Table_Name, whereClause, trxName)
		.setParameters(M_InOutLine_ID, C_InvoiceLine_ID)
		.list();
		return list.toArray (new MMatchInv[list.size()]);
	}	//	get

	/**
	 * 	Get InOut Matches for InvoiceLine
	 *	@param ctx context
	 *	@param C_InvoiceLine_ID invoice line
	 *	@param trxName transaction
	 *	@return array of matches
	 */
	public static MMatchInv[] getInvoiceLine (Properties ctx, int C_InvoiceLine_ID, String trxName)
	{
		if (C_InvoiceLine_ID <= 0)
			return new MMatchInv[]{};
		//
		String whereClause = "C_InvoiceLine_ID=?";
		List<MMatchInv> list = new Query(ctx, I_M_MatchInv.Table_Name, whereClause, trxName)
		.setParameters(C_InvoiceLine_ID)
		.list();
		return list.toArray (new MMatchInv[list.size()]);
	}	//	getInvoiceLine
	
	/**
	 * 	Get Invoice Matches for InOut
	 *	@param ctx context
	 *	@param M_InOut_ID material receipt
	 *	@param trxName transaction
	 *	@return array of matches
	 */
	public static MMatchInv[] getInOut (Properties ctx, int M_InOut_ID, String trxName)
	{
		if (M_InOut_ID <= 0)
			return new MMatchInv[]{};
		//
		final String whereClause = "EXISTS (SELECT 1 FROM M_InOutLine l"
			+" WHERE M_MatchInv.M_InOutLine_ID=l.M_InOutLine_ID AND l.M_InOut_ID=?)"; 
		List<MMatchInv> list = new Query(ctx, I_M_MatchInv.Table_Name, whereClause, trxName)
		.setParameters(M_InOut_ID)
		.list();
		return list.toArray (new MMatchInv[list.size()]);
	}	//	getInOut

	/**
	 * 	Get InOut Matches for Invoice
	 *	@param ctx context
	 *	@param C_Invoice_ID invoice
	 *	@param trxName transaction
	 *	@return array of matches
	 */
	public static MMatchInv[] getInvoice (Properties ctx, 
		int C_Invoice_ID, String trxName)
	{
		if (C_Invoice_ID == 0)
			return new MMatchInv[]{};
		//
		final String whereClause = " EXISTS (SELECT 1 FROM C_InvoiceLine il"
				+" WHERE M_MatchInv.C_InvoiceLine_ID=il.C_InvoiceLine_ID AND il.C_Invoice_ID=?)";
		List<MMatchInv> list = new Query(ctx, I_M_MatchInv.Table_Name, whereClause, trxName)
		.setParameters(C_Invoice_ID)
		.setOrderBy(COLUMNNAME_ProcessedOn)
		.list();
		return list.toArray (new MMatchInv[list.size()]);
	}	//	getInvoice
	
	/**	Static Logger	*/
	@SuppressWarnings("unused")
	private static CLogger	s_log	= CLogger.getCLogger (MMatchInv.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_MatchInv_UU  UUID key
     * @param trxName Transaction
     */
    public MMatchInv(Properties ctx, String M_MatchInv_UU, String trxName) {
        super(ctx, M_MatchInv_UU, trxName);
		if (Util.isEmpty(M_MatchInv_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_MatchInv_ID id
	 *	@param trxName transaction
	 */
	public MMatchInv (Properties ctx, int M_MatchInv_ID, String trxName)
	{
		super (ctx, M_MatchInv_ID, trxName);
		if (M_MatchInv_ID == 0)
			setInitialDefaults();
	}	//	MMatchInv

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setM_AttributeSetInstance_ID(0);
		setPosted (false);
		setProcessed (false);
		setProcessing (false);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MMatchInv (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MMatchInv
	
	/**
	 * 	Invoice Line Constructor
	 *	@param iLine invoice line
	 *	@param dateTrx optional date
	 *	@param qty matched quantity
	 */
	public MMatchInv (MInvoiceLine iLine, Timestamp dateTrx, BigDecimal qty)
	{
		this (iLine.getCtx(), 0, iLine.get_TrxName());
		setClientOrg(iLine);
		setC_InvoiceLine_ID(iLine.getC_InvoiceLine_ID());
		setM_InOutLine_ID(iLine.getM_InOutLine_ID());
		if (dateTrx != null)
			setDateTrx (dateTrx);
		setM_Product_ID (iLine.getM_Product_ID());
		setM_AttributeSetInstance_ID(iLine.getM_AttributeSetInstance_ID());
		setQty (qty);
		setProcessed(true);		//	auto
	}	//	MMatchInv
		
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		//	Set DateTrx to today date
		if (getDateTrx() == null)
			setDateTrx (new Timestamp(System.currentTimeMillis()));
		//	Set DateAcct
		if (getDateAcct() == null)
		{
			Timestamp ts = getNewerDateAcct();
			if (ts == null)
				ts = getDateTrx();
			setDateAcct (ts);
		}
		// Set M_AttributeSetInstance_ID to M_AttributeSetInstance_ID of material receipt line.
		if (getM_AttributeSetInstance_ID() == 0 && getM_InOutLine_ID() != 0)
		{
			MInOutLine iol = new MInOutLine (getCtx(), getM_InOutLine_ID(), get_TrxName());
			setM_AttributeSetInstance_ID(iol.getM_AttributeSetInstance_ID());
		}
		return true;
	}	//	beforeSave
	
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;
		
		// Validate total M_MatchInv.Qty for M_InOutLine_ID against M_InOutLine.MovementQty
		if (getM_InOutLine_ID() > 0)
		{
			MInOutLine line = new MInOutLine(getCtx(), getM_InOutLine_ID(), get_TrxName());
			BigDecimal matchedQty = DB.getSQLValueBD(get_TrxName(), "SELECT Coalesce(SUM(Qty),0) FROM M_MatchInv WHERE M_InOutLine_ID=?" , getM_InOutLine_ID());
			BigDecimal matchedQtyDB = matchedQty;
			BigDecimal movementQty = line.getMovementQty();
			if (movementQty.signum() < 0) {
				movementQty = movementQty.negate();
				matchedQty = matchedQty.negate();
			}
			if (matchedQty != null && matchedQty.compareTo(movementQty) > 0)
			{
				throw new IllegalStateException("Total matched qty > movement qty. MatchedQty="+matchedQtyDB+", MovementQty="+line.getMovementQty()+", Line="+line);
			}
		}
		
		// Validate total M_MatchInv.Qty for C_InvoiceLine_ID against M_InOutLine.MovementQty
		if (getC_InvoiceLine_ID() > 0)
		{
			MInvoiceLine line = new MInvoiceLine(getCtx(), getC_InvoiceLine_ID(), get_TrxName());
			BigDecimal matchedQty = DB.getSQLValueBD(get_TrxName(), "SELECT Coalesce(SUM(Qty),0) FROM M_MatchInv WHERE C_InvoiceLine_ID=?" , getC_InvoiceLine_ID());
			BigDecimal matchedQtyDB = matchedQty;
			BigDecimal qtyInvoiced = line.getQtyInvoiced();
			if (qtyInvoiced.signum() < 0) {
				qtyInvoiced = qtyInvoiced.negate();
				matchedQty = matchedQty.negate();
			}
			if (matchedQty != null && matchedQty.compareTo(qtyInvoiced) > 0)
			{
				throw new IllegalStateException("Total matched qty > invoiced qty. MatchedQty="+matchedQtyDB+", InvoicedQty="+line.getQtyInvoiced()+", Line="+line);
			}
		}
		return true;
	}
	
	/**
	 * 	Get the newer Date Acct between invoice and shipment
	 *	@return date or null
	 */
	public Timestamp getNewerDateAcct()
	{
		String sql = "SELECT i.DateAcct "
			+ "FROM C_InvoiceLine il"
			+ " INNER JOIN C_Invoice i ON (i.C_Invoice_ID=il.C_Invoice_ID) "
			+ "WHERE C_InvoiceLine_ID=?";
		Timestamp invoiceDate = DB.getSQLValueTS(get_TrxName(), sql, getC_InvoiceLine_ID());
		//
		sql = "SELECT io.DateAcct "
			+ "FROM M_InOutLine iol"
			+ " INNER JOIN M_InOut io ON (io.M_InOut_ID=iol.M_InOut_ID) "
			+ "WHERE iol.M_InOutLine_ID=?";
		Timestamp shipDate = DB.getSQLValueTS(get_TrxName(), sql, getM_InOutLine_ID());
		//
		if (invoiceDate == null)
			return shipDate;
		if (shipDate == null)
			return invoiceDate;
		if (invoiceDate.after(shipDate))
			return invoiceDate;
		return shipDate;
	}	//	getNewerDateAcct
		
	@Override
	protected boolean beforeDelete ()
	{
		// Check is period open and delete postings (Fact_Acct)
		if (isPosted())
		{
			MPeriod.testPeriodOpen(getCtx(), getDateTrx(), MDocType.DOCBASETYPE_MatchInvoice, getAD_Org_ID());
			setPosted(false);
			MFactAcct.deleteEx (Table_ID, get_ID(), get_TrxName());
		}
		return true;
	}	//	beforeDelete
	
	@Override
	protected boolean afterDelete (boolean success)
	{
		if (success)
		{
			deleteMatchInvCostDetail();
		}
		return success;
	}	//	afterDelete
	
	/**
	 * Delete cost detail records for M_MatchInv
	 * @return empty string
	 */
	protected String deleteMatchInvCostDetail()
	{
		// Get Account Schemas to delete MCostDetail
		MAcctSchema[] acctschemas = MAcctSchema.getClientAcctSchema(getCtx(), getAD_Client_ID());
		for(int asn = 0; asn < acctschemas.length; asn++)
		{
			MAcctSchema as = acctschemas[asn];
			
			if (as.isSkipOrg(getAD_Org_ID()))
			{
				continue;
			}
			
			MCostDetail cd = MCostDetail.get (getCtx(), "M_MatchInv_ID=?", 
					getM_MatchInv_ID(), getM_AttributeSetInstance_ID(), as.getC_AcctSchema_ID(), get_TrxName());
			if (cd != null)
			{
				cd.deleteEx(true);
			}
		}
		
		return "";
	}
	
	/**
	 * 	Get Invoice Matches for InOutLine
	 *	@param ctx context
	 *	@param M_InOutLine_ID shipment
	 *	@param trxName transaction
	 *	@return array of matches
	 */
	public static MMatchInv[] getInOutLine (Properties ctx, 
		int M_InOutLine_ID, String trxName)
	{
		if (M_InOutLine_ID <= 0)
		{
			return new MMatchInv[]{};
		}
		//
		final String whereClause = MMatchInv.COLUMNNAME_M_InOutLine_ID+"=?";
		List<MMatchInv> list = new Query(ctx, I_M_MatchInv.Table_Name, whereClause, trxName)
		.setParameters(M_InOutLine_ID)
		.list();
		return list.toArray (new MMatchInv[list.size()]);
	}	//	getInOutLine
	
	/**
	 * 	Reverse this MatchInv document.
	 *  @param reversalDate
	 *	@return true if reversed
	 */
	public boolean reverse(Timestamp reversalDate)  
	{
		if (this.isProcessed() && this.getReversal_ID() == 0)
		{		
			MMatchInv reversal = new MMatchInv (getCtx(), 0, get_TrxName());
			PO.copyValues(this, reversal);
			reversal.setAD_Org_ID(this.getAD_Org_ID());
			reversal.setDescription("(->" + this.getDocumentNo() + ")");
			reversal.setQty(this.getQty().negate());
			reversal.setDateAcct(reversalDate);
			reversal.setDateTrx(reversalDate);
			reversal.set_ValueNoCheck ("DocumentNo", null);
			reversal.setPosted (false);
			reversal.setReversal_ID(getM_MatchInv_ID());
			reversal.saveEx();
			this.setDescription("(" + reversal.getDocumentNo() + "<-)");
			this.setReversal_ID(reversal.getM_MatchInv_ID());
			this.saveEx();
			return true;
		}
		return false;
	}
	
	@Override
	public MInOutLine getM_InOutLine() throws RuntimeException {
		return new MInOutLine(Env.getCtx(), getM_InOutLine_ID(), get_TrxName());
	}

	/**
	 * @return true if this is created to reverse another match invoice document
	 */
	public boolean isReversal() {
		if (getReversal_ID() > 0) {
			MMatchInv reversal = new MMatchInv (getCtx(), getReversal_ID(), get_TrxName());
			if (reversal.getM_MatchInv_ID() < getM_MatchInv_ID())
				return true;
		}
		return false;
	}
}	//	MMatchInv
