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
import java.util.Calendar;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Recurring Model
 *
 *	@author Jorg Janke
 *	@version $Id: MRecurring.java,v 1.2 2006/07/30 00:51:03 jjanke Exp $
 */
public class MRecurring extends X_C_Recurring
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 6053691462050896981L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_Recurring_UU  UUID key
     * @param trxName Transaction
     */
    public MRecurring(Properties ctx, String C_Recurring_UU, String trxName) {
        super(ctx, C_Recurring_UU, trxName);
		if (Util.isEmpty(C_Recurring_UU))
			setInitialDefaults();
    }

    /**
     * @param ctx
     * @param C_Recurring_ID
     * @param trxName
     */
	public MRecurring (Properties ctx, int C_Recurring_ID, String trxName)
	{
		super (ctx, C_Recurring_ID, trxName);
		if (C_Recurring_ID == 0)
			setInitialDefaults();
	}	//	MRecurring

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setDateNextRun (new Timestamp(System.currentTimeMillis()));
		setFrequencyType (FREQUENCYTYPE_Monthly);
		setFrequency(1);
		setRunsMax (1);
		setRunsRemaining (0);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MRecurring (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MRecurring

	/**
	 *	String representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder ("MRecurring[")
			.append(get_ID()).append("-").append(getName());
		if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Order))
			sb.append(",C_Order_ID=").append(getC_Order_ID());
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Invoice))
			sb.append(",C_Invoice_ID=").append(getC_Invoice_ID());
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Project))
			sb.append(",C_Project_ID=").append(getC_Project_ID());
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_GLJournal))
			sb.append(",GL_JournalBatch_ID=").append(getGL_JournalBatch_ID());
		sb.append(",Frequency=").append(getFrequencyType()).append("*").append(getFrequency());
		sb.append("]");
		return sb.toString();
	}	//	toString
	
	/**
	 * 	Execute Recurring Run.
	 *	@return clear text execution message
	 */
	public String executeRun()
	{
		Timestamp dateDoc = getDateNextRun();
		if (!calculateRuns())
			throw new IllegalStateException ("No Runs Left");

		//	log
		MRecurringRun run = new MRecurringRun (getCtx(), this);
		String msg = "@Created@ ";

		//	Copy
		if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Order))
		{
			MOrder from = new MOrder (getCtx(), getC_Order_ID(), get_TrxName());
			MOrder order = MOrder.copyFrom (from, dateDoc, 
				from.getC_DocType_ID(), from.isSOTrx(), false, false, get_TrxName());
			run.setC_Order_ID(order.getC_Order_ID());
			msg += order.getDocumentNo();
			setLastPO(order);
		}
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Invoice))
		{
			MInvoice from = new MInvoice (getCtx(), getC_Invoice_ID(), get_TrxName());
			MInvoice invoice = MInvoice.copyFrom (from, dateDoc, dateDoc,
				from.getC_DocType_ID(), from.isSOTrx(), false, get_TrxName(), false);
			run.setC_Invoice_ID(invoice.getC_Invoice_ID());
			msg += invoice.getDocumentNo();
			setLastPO(invoice);
		}
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Project))
		{
			MProject project = MProject.copyFrom (getCtx(), getC_Project_ID(), dateDoc, get_TrxName());
			run.setC_Project_ID(project.getC_Project_ID());
			msg += project.getValue();
			setLastPO(project);
		}
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_GLJournal))
		{
			MJournalBatch journal = MJournalBatch.copyFrom (getCtx(), getGL_JournalBatch_ID(), dateDoc, get_TrxName());
			run.setGL_JournalBatch_ID(journal.getGL_JournalBatch_ID());
			msg += journal.getDocumentNo();
			setLastPO(journal);
		}
		else if (getRecurringType().equals(MRecurring.RECURRINGTYPE_Payment))
		{
			MPayment from = new MPayment(getCtx(), getC_Payment_ID(), get_TrxName());
			MPayment to = new MPayment(getCtx(), 0, get_TrxName());
			copyValues(from, to);
			to.setAD_Org_ID(from.getAD_Org_ID());
			to.setIsReconciled(false); // can't be already on a bank statement
			to.setDateAcct(dateDoc);
			to.setDateTrx(dateDoc);
			to.setDocumentNo("");
			to.setProcessed(false);
			to.setPosted(false);
			to.setDocStatus(MPayment.DOCSTATUS_Drafted);
			to.setDocAction(MPayment.DOCACTION_Complete);
			to.saveEx();

			run.setC_Payment_ID(to.getC_Payment_ID());
			msg += to.getDocumentNo();	
			setLastPO(to);
		}
		else
			return "Invalid @RecurringType@ = " + getRecurringType();
		run.saveEx(get_TrxName());

		//
		setDateLastRun (run.getUpdated());
		setRunsRemaining (getRunsRemaining()-1);
		setDateNextRun();
		saveEx(get_TrxName());
		return msg;
	}	//	executeRun

	/**
	 *	Calculate & set remaining Runs
	 *	@return true if there are remaining runs left
	 */
	private boolean calculateRuns()
	{
		String sql = "SELECT COUNT(*) FROM C_Recurring_Run WHERE C_Recurring_ID=?";
		int current = DB.getSQLValue(get_TrxName(), sql, getC_Recurring_ID());
		int remaining = getRunsMax() - current;
		setRunsRemaining(remaining);
		saveEx();
		return remaining > 0;
	}	//	calculateRuns

	/**
	 *	Calculate and set next run date
	 */
	private void setDateNextRun()
	{
		if (getFrequency() < 1)
			setFrequency(1);
		int frequency = getFrequency();
		Calendar cal = Calendar.getInstance();
		cal.setTime(getDateNextRun());
		//
		if (getFrequencyType().equals(FREQUENCYTYPE_Daily))
			cal.add(Calendar.DAY_OF_YEAR, frequency);
		else if (getFrequencyType().equals(FREQUENCYTYPE_Weekly))
			cal.add(Calendar.WEEK_OF_YEAR, frequency);
		else if (getFrequencyType().equals(FREQUENCYTYPE_Monthly))
			cal.add(Calendar.MONTH, frequency);
		else if (getFrequencyType().equals(FREQUENCYTYPE_Quarterly))
			cal.add(Calendar.MONTH, 3*frequency);
		Timestamp next = new Timestamp (cal.getTimeInMillis());
		setDateNextRun(next);
	}	//	setDateNextRun

	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Validate mandatory for RecurringType and corresponding field
		String rt = getRecurringType();
		if (rt == null)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "RecurringType"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_Order)
			&& getC_Order_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_Order_ID"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_Invoice)
			&& getC_Invoice_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_Invoice_ID"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_GLJournal)
			&& getGL_JournalBatch_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "GL_JournalBatch_ID"));
			return false;
		}
		if (rt.equals(MRecurring.RECURRINGTYPE_Project)
			&& getC_Project_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_Project_ID"));
			return false;
		}
		return true;
	}	//	beforeSave

	/* The last PO generated by a recent execute Run */
	private volatile PO lastPO;

	/**
	 * @return last PO generated by recent {@link #executeRun()}
	 */
	public PO getLastPO() {
		return lastPO;
	}

	/**
	 * @param lastPO
	 */
	public void setLastPO(PO lastPO) {
		this.lastPO = lastPO;
	}

}	//	MRecurring
