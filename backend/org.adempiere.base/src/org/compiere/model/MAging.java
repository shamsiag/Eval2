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

import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 *	Aging Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MAging.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MAging extends X_T_Aging
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 3067400117623770188L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param T_Aging_UU  UUID key
     * @param trxName Transaction
     */
    public MAging(Properties ctx, String T_Aging_UU, String trxName) {
        super(ctx, T_Aging_UU, trxName);
		if (Util.isEmpty(T_Aging_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param T_Aging_ID id
	 *	@param trxName transaction
	 */
	public MAging (Properties ctx, int T_Aging_ID, String trxName)
	{
		super (ctx, T_Aging_ID, trxName);
		if (T_Aging_ID == 0)
			setInitialDefaults();
	}	//	T_Aging

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setDueAmt (Env.ZERO);
		setDue0 (Env.ZERO);
		setDue0_7 (Env.ZERO);
		setDue0_30 (Env.ZERO);
		setDue1_7 (Env.ZERO);
		setDue31_60 (Env.ZERO);
		setDue31_Plus (Env.ZERO);
		setDue61_90 (Env.ZERO);
		setDue61_Plus (Env.ZERO);
		setDue8_30 (Env.ZERO);
		setDue91_Plus (Env.ZERO);
		//
		setPastDueAmt (Env.ZERO);
		setPastDue1_7 (Env.ZERO);
		setPastDue1_30 (Env.ZERO);
		setPastDue31_60 (Env.ZERO);
		setPastDue31_Plus (Env.ZERO);
		setPastDue61_90 (Env.ZERO);
		setPastDue61_Plus (Env.ZERO);
		setPastDue8_30 (Env.ZERO);
		setPastDue91_Plus (Env.ZERO);
		//
		setOpenAmt(Env.ZERO);
		setInvoicedAmt(Env.ZERO);
		//
		setIsListInvoices (false);
		setIsSOTrx (false);
	}

	/**
	 *	@param ctx context
	 *	@param AD_PInstance_ID instance
	 *	@param StatementDate statement date
	 *	@param C_BPartner_ID bpartner
	 *	@param C_Currency_ID currency
	 *	@param C_Invoice_ID invoice
	 *	@param C_InvoicePaySchedule_ID invoice schedule
	 *	@param C_BP_Group_ID group
	 *	@param AD_Org_ID organization
	 *	@param DueDate due date
	 *	@param IsSOTrx SO Trx
	 *	@param trxName transaction
	 */
	public MAging (Properties ctx, int AD_PInstance_ID, Timestamp StatementDate, 
		int C_BPartner_ID, int C_Currency_ID, 
		int C_Invoice_ID, int C_InvoicePaySchedule_ID,
		int C_BP_Group_ID, int AD_Org_ID, Timestamp DueDate, boolean IsSOTrx, String trxName)
	{
		this (ctx, 0, trxName);
		setAD_PInstance_ID (AD_PInstance_ID);
		setStatementDate(StatementDate);
		//
		setC_BPartner_ID (C_BPartner_ID);
		setC_Currency_ID (C_Currency_ID);
		setC_BP_Group_ID (C_BP_Group_ID);
		setAD_Org_ID(AD_Org_ID);
		setIsSOTrx (IsSOTrx);

		set_ValueNoCheck ("C_Invoice_ID", Integer.valueOf(C_Invoice_ID));
		set_Value ("C_InvoicePaySchedule_ID", Integer.valueOf(C_InvoicePaySchedule_ID));
		setIsListInvoices(C_Invoice_ID != 0);
		//
		setDueDate(DueDate);		//	only sensible if List invoices
	}	//	MAging

	/**
	 * 	Partial Constructor - backward compatibility
	 *	@param ctx context
	 *	@param AD_PInstance_ID instance
	 *	@param StatementDate statement date
	 *	@param C_BPartner_ID bpartner
	 *	@param C_Currency_ID currency
	 *	@param C_Invoice_ID invoice
	 *	@param C_InvoicePaySchedule_ID invoice schedule
	 *	@param C_BP_Group_ID group
	 *	@param DueDate due date
	 *	@param IsSOTrx SO Trx
	 *	@param trxName transaction
	 *
	 * @deprecated - better use the new constructor with organization included
	 */
	@Deprecated(forRemoval = true, since = "11")
	public MAging (Properties ctx, int AD_PInstance_ID, Timestamp StatementDate, 
		int C_BPartner_ID, int C_Currency_ID, 
		int C_Invoice_ID, int C_InvoicePaySchedule_ID,
		int C_BP_Group_ID, Timestamp DueDate, boolean IsSOTrx, String trxName)
	{
		this (ctx, 0, trxName);
		setAD_PInstance_ID (AD_PInstance_ID);
		setStatementDate(StatementDate);
		//
		setC_BPartner_ID (C_BPartner_ID);
		setC_Currency_ID (C_Currency_ID);
		setC_BP_Group_ID (C_BP_Group_ID);
		setIsSOTrx (IsSOTrx);

		set_ValueNoCheck ("C_Invoice_ID", Integer.valueOf(C_Invoice_ID));
		set_Value ("C_InvoicePaySchedule_ID", Integer.valueOf(C_InvoicePaySchedule_ID));
		setIsListInvoices(C_Invoice_ID != 0);
		//
		setDueDate(DueDate);		//	only sensible if List invoices
	}	//	MAging
	
	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MAging (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MAging

	/** Number of items 		*/
	private int		m_noItems = 0;
	/** Sum of Due Days			*/
	private int		m_daysDueSum = 0;
	
	/**
	 * 	Add Amount to Buckets (by days due)
	 *	@param DueDate due date 
	 *	@param daysDue positive due - negative not due
	 *	@param invoicedAmt invoiced amount
	 *	@param openAmt open amount
	 */
	public void add (Timestamp DueDate, int daysDue, BigDecimal invoicedAmt, BigDecimal openAmt)
	{
		if (invoicedAmt == null)
			invoicedAmt = Env.ZERO;
		setInvoicedAmt(getInvoicedAmt().add(invoicedAmt));
		if (openAmt == null)
			openAmt = Env.ZERO;
		setOpenAmt(getOpenAmt().add(openAmt));
		//	Days Due
		m_noItems++;
		m_daysDueSum += daysDue;
		setDaysDue(m_daysDueSum/m_noItems);
		//	Due Date
		if (getDueDate().after(DueDate))
			setDueDate(DueDate);		//	earliest
		//
		BigDecimal amt = openAmt;
		//	Not due - negative
		if (daysDue <= 0)
		{
			setDueAmt (getDueAmt().add(amt));
			if (daysDue == 0)
				setDue0 (getDue0().add(amt));
				
			if (daysDue >= -7)
				setDue0_7 (getDue0_7().add(amt));
				
			if (daysDue >= -30)
				setDue0_30 (getDue0_30().add(amt));
				
			if (daysDue <= -1 && daysDue >= -7)
				setDue1_7 (getDue1_7().add(amt));
				
			if (daysDue <= -8 && daysDue >= -30)
				setDue8_30 (getDue8_30().add(amt));
				
			if (daysDue <= -31 && daysDue >= -60)
				setDue31_60 (getDue31_60().add(amt));
				
			if (daysDue <= -31)
				setDue31_Plus (getDue31_Plus().add(amt));
				
			if (daysDue <= -61 && daysDue >= -90)
				setDue61_90 (getDue61_90().add(amt));
				
			if (daysDue <= -61)
				setDue61_Plus (getDue61_Plus().add(amt));
				
			if (daysDue <= -91)
				setDue91_Plus (getDue91_Plus().add(amt));
		}
		else	//	Due = positive (> 1)
		{
			setPastDueAmt (getPastDueAmt().add(amt));
			if (daysDue <= 7)
				setPastDue1_7 (getPastDue1_7().add(amt));
				
			if (daysDue <= 30)
				setPastDue1_30 (getPastDue1_30().add(amt));
				
			if (daysDue >= 8 && daysDue <= 30)
				setPastDue8_30 (getPastDue8_30().add(amt));
			
			if (daysDue >= 31 && daysDue <= 60)
				setPastDue31_60 (getPastDue31_60().add(amt));
				
			if (daysDue >= 31)
				setPastDue31_Plus (getPastDue31_Plus().add(amt));
			
			if (daysDue >= 61 && daysDue <= 90)
				setPastDue61_90 (getPastDue61_90().add(amt));
				
			if (daysDue >= 61)
				setPastDue61_Plus (getPastDue61_Plus().add(amt));
				
			if (daysDue >= 91)
				setPastDue91_Plus (getPastDue91_Plus().add(amt));
		}
	}	//	add

	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder("MAging[");
		sb.append("AD_PInstance_ID=").append(getAD_PInstance_ID())
			.append(",C_BPartner_ID=").append(getC_BPartner_ID())
			.append(",C_Currency_ID=").append(getC_Currency_ID())
			.append(",C_Invoice_ID=").append(getC_Invoice_ID());
		sb.append("]");
		return sb.toString();
	} //	toString

}	//	MAging
