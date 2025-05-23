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
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Time + Expense Line Model
 *
 *	@author Jorg Janke
 *	@version $Id: MTimeExpenseLine.java,v 1.4 2006/09/25 00:59:41 jjanke Exp $
 */
public class MTimeExpenseLine extends X_S_TimeExpenseLine
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 3580618153284679385L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param S_TimeExpenseLine_UU  UUID key
     * @param trxName Transaction
     */
    public MTimeExpenseLine(Properties ctx, String S_TimeExpenseLine_UU, String trxName) {
        super(ctx, S_TimeExpenseLine_UU, trxName);
		if (Util.isEmpty(S_TimeExpenseLine_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param S_TimeExpenseLine_ID id
	 *	@param trxName transaction
	 */
	public MTimeExpenseLine (Properties ctx, int S_TimeExpenseLine_ID, String trxName)
	{
		super (ctx, S_TimeExpenseLine_ID, trxName);
		if (S_TimeExpenseLine_ID == 0)
			setInitialDefaults();
	}	//	MTimeExpenseLine

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setQty(Env.ONE);
		setQtyInvoiced(Env.ZERO);
		setQtyReimbursed(Env.ZERO);
		//
		setExpenseAmt(Env.ZERO);
		setConvertedAmt(Env.ZERO);
		setPriceReimbursed(Env.ZERO);
		setInvoicePrice(Env.ZERO);
		setPriceInvoiced(Env.ZERO);
		//
		setDateExpense (new Timestamp(System.currentTimeMillis()));
		setIsInvoiced (false);
		setIsTimeReport (false);
		setLine (10);
		setProcessed(false);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MTimeExpenseLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MTimeExpenseLine

	/** Parent					*/
	private MTimeExpense			m_parent = null;
	
	/**
	 * 	Get Parent
	 *	@return parent
	 */
	public MTimeExpense getParent()
	{
		if (m_parent == null)
			m_parent = new MTimeExpense (getCtx(), getS_TimeExpense_ID(), get_TrxName());
		return m_parent;
	}	//	getParent

	/**	Currency of Report			*/
	private int m_C_Currency_Report_ID = 0;
	
	/**
	 * 	Get Qty Invoiced
	 *	@return QtyInvoiced or Qty if QtyInvoiced is zero
	 */
	public BigDecimal getQtyInvoiced ()
	{
		BigDecimal bd = super.getQtyInvoiced ();
		if (Env.ZERO.compareTo(bd) == 0)
			return getQty();
		return bd;
	}	//	getQtyInvoiced

	/**
	 * 	Get Qty Reimbursed
	 *	@return QtyReimbursed or Qty if QtyReimbursed is zero
	 */
	public BigDecimal getQtyReimbursed ()
	{
		BigDecimal bd = super.getQtyReimbursed ();
		if (Env.ZERO.compareTo(bd) == 0)
			return getQty();
		return bd;
	}	//	getQtyReimbursed
		
	/**
	 * 	Get Price Invoiced
	 *	@return PriceInvoiced or InvoicePrice if PriceInvoiced is zero 
	 */
	public BigDecimal getPriceInvoiced ()
	{
		BigDecimal bd = super.getPriceInvoiced ();
		if (Env.ZERO.compareTo(bd) == 0)
			return getInvoicePrice();
		return bd;
	}	//	getPriceInvoiced
	
	/**
	 * 	Get Price Reimbursed
	 *	@return PriceReimbursed or converted amt if PriceReimbursed is zero
	 */
	public BigDecimal getPriceReimbursed ()
	{
		BigDecimal bd = super.getPriceReimbursed ();
		if (Env.ZERO.compareTo(bd) == 0)
			return getConvertedAmt();
		return bd;
	}	//	getPriceReimbursed
		
	/**
	 * 	Get Approval Amt
	 *	@return qty * converted amt
	 */
	public BigDecimal getApprovalAmt()
	{
		return getQty().multiply(getConvertedAmt());
	}	//	getApprovalAmt
		
	/**
	 * 	Get C_Currency_ID of Report (Price List)
	 *	@return C_Currency_ID of Report (if set) or C_Currency_ID of header
	 */
	public int getC_Currency_Report_ID()
	{
		if (m_C_Currency_Report_ID != 0)
			return m_C_Currency_Report_ID;
		//	Get it from header
		MTimeExpense te = new MTimeExpense (getCtx(), getS_TimeExpense_ID(), get_TrxName());
		m_C_Currency_Report_ID = te.getC_Currency_ID();
		return m_C_Currency_Report_ID;
	}	//	getC_Currency_Report_ID

	/**
	 * 	Set C_Currency_ID of Report (Price List)
	 *	@param C_Currency_ID currency
	 */
	protected void setC_Currency_Report_ID (int C_Currency_ID)
	{
		m_C_Currency_Report_ID = C_Currency_ID;
	}	//	getC_Currency_Report_ID
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (newRecord && getParent().isProcessed()) {
			log.saveError("ParentComplete", Msg.translate(getCtx(), "S_TimeExpense_ID"));
			return false;
		}
		
		//	Calculate Converted Amount from ExpenseAmt
		if (newRecord || is_ValueChanged("ExpenseAmt") || is_ValueChanged("C_Currency_ID"))
		{
			if (getC_Currency_ID() == getC_Currency_Report_ID())
				setConvertedAmt(getExpenseAmt());
			else
			{
				setConvertedAmt(MConversionRate.convert (getCtx(),
					getExpenseAmt(), getC_Currency_ID(), getC_Currency_Report_ID(), 
					getDateExpense(), 0, getAD_Client_ID(), getAD_Org_ID()) );
			}
		}
		
		// Calculate Line Net Amount
		if (newRecord || is_ValueChanged(COLUMNNAME_Qty) || is_ValueChanged(COLUMNNAME_ExpenseAmt))
		{
			BigDecimal lineNetAmt = getExpenseAmt().multiply(getQty());
			setLineNetAmt(lineNetAmt);
		}
		
		// Set expense, converted and line net amount to zero if IsTimeReport=Y
		if (isTimeReport())
		{
			setExpenseAmt(Env.ZERO);
			setConvertedAmt(Env.ZERO);
			setLineNetAmt(Env.ZERO);
		}
		return true;
	}	//	beforeSave
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (success)
		{
			updateHeader();
			if (newRecord || is_ValueChanged("S_ResourceAssignment_ID"))
			{
				int S_ResourceAssignment_ID = getS_ResourceAssignment_ID();
				int old_S_ResourceAssignment_ID = 0;
				if (!newRecord)
				{
					// Delete previous resource assignment record
					Object ii = get_ValueOld("S_ResourceAssignment_ID");
					if (ii instanceof Integer)
					{
						old_S_ResourceAssignment_ID = ((Integer)ii).intValue();						
						if (old_S_ResourceAssignment_ID != S_ResourceAssignment_ID
							&& old_S_ResourceAssignment_ID != 0)
						{
							MResourceAssignment ra = new MResourceAssignment (getCtx(), 
								old_S_ResourceAssignment_ID, get_TrxName());
							ra.delete(false);
						}
					}
				}
				// Sync Resource Assignment (Qty and Description) 
				if (S_ResourceAssignment_ID != 0)
				{
					MResourceAssignment ra = new MResourceAssignment (getCtx(), 
						S_ResourceAssignment_ID, get_TrxName());
					if (getQty().compareTo(ra.getQty()) != 0)
					{
						ra.setQty(getQty());
						if (getDescription() != null && getDescription().length() > 0)
							ra.setDescription(getDescription());
						ra.saveEx();
					}
				}
			}
		}
		return success;
	}	//	afterSave
		
	@Override
	protected boolean afterDelete (boolean success)
	{
		if (success)
		{
			updateHeader();
			//
			Object ii = get_ValueOld("S_ResourceAssignment_ID");
			if (ii instanceof Integer)
			{
				int old_S_ResourceAssignment_ID = ((Integer)ii).intValue();
				//	Delete Previous Resource Assignment record
				if (old_S_ResourceAssignment_ID != 0)
				{
					MResourceAssignment ra = new MResourceAssignment (getCtx(), 
						old_S_ResourceAssignment_ID, get_TrxName());
					ra.delete(false);
				}
			}
		}
		return success;
	}	//	afterDelete
	
	/**
	 * 	Update Header.
	 * 	Set Approved Amount.
	 */
	private void updateHeader()
	{
		String sql = "UPDATE S_TimeExpense te"
			+ " SET ApprovalAmt = "
				+ "(SELECT SUM(Qty*ConvertedAmt) FROM S_TimeExpenseLine tel "
				+ "WHERE te.S_TimeExpense_ID=tel.S_TimeExpense_ID) "
			+ "WHERE S_TimeExpense_ID=" + getS_TimeExpense_ID();
		@SuppressWarnings("unused")
		int no = DB.executeUpdate(sql, get_TrxName());
	}	//	updateHeader
	
}	//	MTimeExpenseLine
