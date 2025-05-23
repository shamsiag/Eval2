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
 * Contributor(s): Teo Sarca                                                  *
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
 *	Bank Statement Line Model
 *
 *	@author Eldir Tomassen/Jorg Janke
 *	@version $Id: MBankStatementLine.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 *  
 *  Carlos Ruiz - globalqss - integrate bug fixing from Teo Sarca
 *    [ 1619076 ] Bank statement's StatementDifference becames NULL
 *
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>BF [ 1896880 ] Unlink Payment if TrxAmt is zero
 * 			<li>BF [ 1896885 ] BS Line: don't update header if after save/delete fails
 */
 public class MBankStatementLine extends X_C_BankStatementLine
 {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2604381588523683439L;

	/**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_BankStatementLine_UU  UUID key
     * @param trxName Transaction
     */
    public MBankStatementLine(Properties ctx, String C_BankStatementLine_UU, String trxName) {
        super(ctx, C_BankStatementLine_UU, trxName);
		if (Util.isEmpty(C_BankStatementLine_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_BankStatementLine_ID id
	 *	@param trxName transaction
	 */
	public MBankStatementLine (Properties ctx, int C_BankStatementLine_ID, String trxName)
	{
		super (ctx, C_BankStatementLine_ID, trxName);
		if (C_BankStatementLine_ID == 0)
			setInitialDefaults();
	}	//	MBankStatementLine

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setStmtAmt(Env.ZERO);
		setTrxAmt(Env.ZERO);
		setInterestAmt(Env.ZERO);
		setChargeAmt(Env.ZERO);
		setIsReversal (false);
	}

	/**
	 *	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MBankStatementLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MBankStatementLine
	
	/**
	 * 	Parent Constructor
	 * 	@param statement Bank Statement that the line is part of
	 */
	public MBankStatementLine(MBankStatement statement)
	{
		this (statement.getCtx(), 0, statement.get_TrxName());
		setClientOrg(statement);
		setC_BankStatement_ID(statement.getC_BankStatement_ID());
		setStatementLineDate(statement.getStatementDate());
	}	//	MBankStatementLine

	/**
	 * 	Parent Constructor
	 * 	@param statement Bank Statement that the line is part of
	 * 	@param lineNo position of the line within the statement
	 */
	public MBankStatementLine(MBankStatement statement, int lineNo)
	{
		this (statement);
		setLine(lineNo);
	}	//	MBankStatementLine

	/**
	 * @param ctx
	 * @param C_BankStatementLine_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MBankStatementLine(Properties ctx, int C_BankStatementLine_ID, String trxName, String... virtualColumns) {
		super(ctx, C_BankStatementLine_ID, trxName, virtualColumns);
	}

	/**
	 * 	Set Statement Line Date and all other dates (Valuta, Acct)
	 *	@param StatementLineDate date
	 */
	@Override
	public void setStatementLineDate(Timestamp StatementLineDate)
	{
		super.setStatementLineDate(StatementLineDate);
		setValutaDate (StatementLineDate);
		setDateAcct (StatementLineDate);
	}	//	setStatementLineDate

	/**
	 * 	Set Payment
	 *	@param payment payment
	 */
	public void setPayment (MPayment payment)
	{
		setC_Payment_ID (payment.getC_Payment_ID());
		setC_Currency_ID (payment.getC_Currency_ID());
		//
		BigDecimal amt = payment.getPayAmt(true); 
		BigDecimal chargeAmt = getChargeAmt();
		if (chargeAmt == null)
			chargeAmt = Env.ZERO;
		BigDecimal interestAmt = getInterestAmt();
		if (interestAmt == null)
			interestAmt = Env.ZERO;
		setTrxAmt(amt);
		setStmtAmt(amt.add(chargeAmt).add(interestAmt));
		//
		setDescription(payment.getDescription());
	}	//	setPayment

	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else{
			StringBuilder msgsd = new StringBuilder(desc).append(" | ").append(description);
			setDescription(msgsd.toString());
		}
	}	//	addDescription
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (newRecord && getParent().isProcessed()) {
			log.saveError("ParentComplete", Msg.translate(getCtx(), "C_BankStatement_ID"));
			return false;
		}

		// Make sure date is on the same period as header if used for posting
		if (newRecord || is_ValueChanged(COLUMNNAME_DateAcct)) {
			if (!isDateConsistentIfUsedForPosting()) {
				log.saveError("SaveError", Msg.getMsg(getCtx(), "BankStatementLinePeriodNotSameAsHeader", new Object[] {getLine()}));
				return false;				
			}
		}
		
		if (getC_Payment_ID() != 0 && getC_DepositBatch_ID() != 0) {
			log.saveError("SaveError", Msg.translate(getCtx(), "EitherPaymentOrDepositBatch"));
			return false;
		}
		
		if (getC_DepositBatch_ID() != 0 && !getC_DepositBatch().isProcessed()) {
			log.saveError("SaveError", Msg.getMsg(getCtx(), "DepositBatchIsNotProcessed") + getC_DepositBatch());
			return false;
		}

		//	Calculate Charge = Statement - Trx - Interest  
		BigDecimal amt = getStmtAmt();
		amt = amt.subtract(getTrxAmt());
		amt = amt.subtract(getInterestAmt());
		if (amt.compareTo(getChargeAmt()) != 0)
			setChargeAmt (amt);
		// Charge is mandatory if charge amount is not zero
		if (getChargeAmt().signum() != 0 && getC_Charge_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_Charge_ID"));
			return false;
		}
		// Reset Payment and Invoice field to 0 if TrxAmt is zero 
		if (getTrxAmt().signum() == 0 && getC_Payment_ID() > 0)
		{
			setC_Payment_ID(I_ZERO);
			setC_Invoice_ID(I_ZERO);
		}
		//	Set Line No
		if (getLine() == 0)
		{
			String sql = "SELECT COALESCE(MAX(Line),0)+10 AS DefaultValue FROM C_BankStatementLine WHERE C_BankStatement_ID=?";
			int ii = DB.getSQLValue (get_TrxName(), sql, getC_BankStatement_ID());
			setLine (ii);
		}
		
		//	Set business partner and invoice from payment
		if (getC_Payment_ID() != 0 && getC_BPartner_ID() == 0)
		{
			MPayment payment = new MPayment (getCtx(), getC_Payment_ID(), get_TrxName());
			setC_BPartner_ID(payment.getC_BPartner_ID());
			if (payment.getC_Invoice_ID() != 0)
				setC_Invoice_ID(payment.getC_Invoice_ID());
		}
		// Set business partner from invoice
		if (getC_Invoice_ID() != 0 && getC_BPartner_ID() == 0)
		{
			MInvoice invoice = new MInvoice (getCtx(), getC_Invoice_ID(), get_TrxName());
			setC_BPartner_ID(invoice.getC_BPartner_ID());
		}
		
		return true;
	}	//	beforeSave
	
	/** Parent					*/
	protected MBankStatement m_parent = null;
	
	/**
	 * 	Get Parent
	 *	@return parent
	 */
	public MBankStatement getParent()
	{
		if (m_parent == null)
			m_parent = new MBankStatement (getCtx(), getC_BankStatement_ID(), get_TrxName());
		return m_parent;
	}	//	getParent
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		return updateHeader();
	}	//	afterSave
	
	@Override
	protected boolean afterDelete (boolean success)
	{
		if (!success)
			return success;
		return updateHeader();
	}	//	afterSave

	/**
	 * Update Header (Bank Statement)<br/>
	 * - Statement difference<br/>
	 * - Ending balance
	 */
	protected boolean updateHeader()
	{
		StringBuilder sql = new StringBuilder("UPDATE C_BankStatement bs")
			.append(" SET StatementDifference=(SELECT COALESCE(SUM(StmtAmt),0) FROM C_BankStatementLine bsl ")
				.append("WHERE bsl.C_BankStatement_ID=bs.C_BankStatement_ID AND bsl.IsActive='Y') ")
			.append("WHERE C_BankStatement_ID=").append(getC_BankStatement_ID());
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 1) {
			log.warning("StatementDifference #" + no);
			return false;
		}
		sql = new StringBuilder("UPDATE C_BankStatement bs")
			.append(" SET EndingBalance=BeginningBalance+StatementDifference ")
			.append("WHERE C_BankStatement_ID=").append(getC_BankStatement_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 1) {
			log.warning("Balance #" + no);
			return false;
		}
		return true;
	}	//	updateHeader

	/**
	 * If the posting is based on the date of the line (ie SysConfig BANK_STATEMENT_POST_WITH_DATE_FROM_LINE = Y), make sure line and header dates are in the same financial period
	 * @return true if not using date from statement line or header and line is in the same financial period
	 */
	public boolean isDateConsistentIfUsedForPosting() {
		return isDateConsistentIfUsedForPosting(getParent().getDateAcct());
	}

	/**
	 * If the posting is based on the date of the line (ie SysConfig BANK_STATEMENT_POST_WITH_DATE_FROM_LINE = Y), make sure line and header dates are in the same financial period
	 * @param headerDateAcct
	 * @return true if not using date from statement line or header and line is in the same financial period
	 */
	public boolean isDateConsistentIfUsedForPosting(Timestamp headerDateAcct) {
		if (MBankStatement.isPostWithDateFromLine(getAD_Client_ID())) {
			MPeriod headerPeriod = MPeriod.get(getCtx(), headerDateAcct, getParent().getAD_Org_ID(), get_TrxName());
			MPeriod linePeriod = MPeriod.get(getCtx(), getDateAcct(), getParent().getAD_Org_ID(), get_TrxName());

			return headerPeriod != null && linePeriod != null && headerPeriod.getC_Period_ID() == linePeriod.getC_Period_ID();	
		}
		return true;
	}
	
	@Override
	public MDepositBatch getC_DepositBatch() throws RuntimeException {
		return getC_DepositBatch_ID() > 0 ? new MDepositBatch(getCtx(), getC_DepositBatch_ID(), get_TrxName()) : null;
	}
	
 }	//	MBankStatementLine
