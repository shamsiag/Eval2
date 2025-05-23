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

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
 
/**
*	Bank Statement Model
*
*  @author Eldir Tomassen/Jorg Janke
*  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
*   <li> BF [ 1933645 ] Wrong balance Bank Statement
*   @see https://sourceforge.net/p/adempiere/bugs/1145/
* 	<li> FR [ 2520591 ] Support multiples calendar for Org 
*	@see https://sourceforge.net/p/adempiere/feature-requests/631/
* 	<li> BF [ 2824951 ] The payments is not release when Bank Statement is void 
*	@see https://sourceforge.net/p/adempiere/bugs/1990/
*  @author Teo Sarca, http://www.arhipac.ro
* 	<li>FR [ 2616330 ] Use MPeriod.testPeriodOpen instead of isOpen
* 		https://sourceforge.net/p/adempiere/feature-requests/666/
*  
*   @version $Id: MBankStatement.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
*/
public class MBankStatement extends X_C_BankStatement implements DocAction
{
    /**
	 * generated serial id 
	 */
	private static final long serialVersionUID = 7420574960104461342L;

	/**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_BankStatement_UU  UUID key
     * @param trxName Transaction
     */
    public MBankStatement(Properties ctx, String C_BankStatement_UU, String trxName) {
        super(ctx, C_BankStatement_UU, trxName);
		if (Util.isEmpty(C_BankStatement_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_BankStatement_ID id
	 *	@param trxName transaction
	 */	
	public MBankStatement (Properties ctx, int C_BankStatement_ID, String trxName)
	{
		super (ctx, C_BankStatement_ID, trxName);
		if (C_BankStatement_ID == 0)
			setInitialDefaults();
	}	//	MBankStatement

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setStatementDate (new Timestamp(System.currentTimeMillis()));	// @Date@
		setDocAction (DOCACTION_Complete);	// CO
		setDocStatus (DOCSTATUS_Drafted);	// DR
		setBeginningBalance(Env.ZERO);
		setStatementDifference(Env.ZERO);
		setEndingBalance (Env.ZERO);
		setIsApproved (false);	// N
		setIsManual (true);	// Y
		setPosted (false);	// N
		super.setProcessed (false);
	}

	/**
	 * 	Load Constructor
	 * 	@param ctx Current context
	 * 	@param rs result set
	 *	@param trxName transaction
	 */
	public MBankStatement(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MBankStatement

 	/**
 	 * 	Parent Constructor
	 *	@param account Bank Account
 	 * 	@param isManual Manual statement
 	 **/
	public MBankStatement (MBankAccount account, boolean isManual)
	{
		this (account.getCtx(), 0, account.get_TrxName());
		setClientOrg(account);
		setC_BankAccount_ID(account.getC_BankAccount_ID());
		setStatementDate(new Timestamp(System.currentTimeMillis()));
		setDateAcct(new Timestamp(System.currentTimeMillis()));
		setBeginningBalance(account.getCurrentBalance());
		setName(getStatementDate().toString());
		setIsManual(isManual);
	}	//	MBankStatement
	
	/**
	 * 	Create a new Bank Statement
	 *	@param account Bank Account
	 */
	public MBankStatement(MBankAccount account)
	{
		this(account, false);
	}	//	MBankStatement
 
	/**	Lines							*/
	protected MBankStatementLine[] m_lines = null;
	
 	/**
 	 * 	Get Bank Statement Lines
 	 * 	@param requery true to always re-query from DB
 	 *	@return statement line array
 	 */
 	public MBankStatementLine[] getLines (boolean requery)
 	{
		if (m_lines != null && !requery) {
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		//
		final String whereClause = I_C_BankStatementLine.COLUMNNAME_C_BankStatement_ID+"=?";
		List<MBankStatementLine> list = new Query(getCtx(),I_C_BankStatementLine.Table_Name,whereClause,get_TrxName())
		.setParameters(getC_BankStatement_ID())
		.setOrderBy("Line,C_BankStatementLine_ID")
		.list();
		MBankStatementLine[] retValue = new MBankStatementLine[list.size()];
		list.toArray(retValue);
		return retValue;
 	}	//	getLines

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
			StringBuilder msgd = new StringBuilder(desc).append(" | ").append(description);
			setDescription(msgd.toString());
		}	
	}	//	addDescription

	/**
	 * 	Set Processed.
	 * 	Propagate to Lines.
	 *	@param processed processed
	 */
	@Override
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		StringBuilder sql = new StringBuilder("UPDATE C_BankStatementLine SET Processed='")
			.append((processed ? "Y" : "N"))
			.append("' WHERE C_BankStatement_ID=").append(getC_BankStatement_ID());
		int noLine = DB.executeUpdate(sql.toString(), get_TrxName());
		m_lines = null;
		if (log.isLoggable(Level.FINE)) log.fine("setProcessed - " + processed + " - Lines=" + noLine);
	}	//	setProcessed

	/**
	 * 	Get Bank Account
	 *	@return bank Account
	 */
	public MBankAccount getBankAccount()
	{
		return MBankAccount.getCopy(getCtx(), getC_BankAccount_ID(), (String)null);
	}	//	getBankAccount
	
	/**
	 * 	Get Document No 
	 *	@return name
	 */
	@Override
	public String getDocumentNo()
	{
		return getName();
	}	//	getDocumentNo
	
	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	@Override
	public String getDocumentInfo()
	{
		StringBuilder msgreturn = new StringBuilder().append(getBankAccount().getName()).append(" ").append(getDocumentNo());
		return msgreturn.toString();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return PDF File or null
	 */
	@Override
	public File createPDF ()
	{
		try
		{
			StringBuilder msgfile = new StringBuilder().append(get_TableName()).append(get_ID()).append("_");
			File temp = File.createTempFile(msgfile.toString(), ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return not implemented, always return null
	 */
	public File createPDF (File file)
	{
		return null;
	}	//	createPDF
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (getC_DocType_ID() <= 0) {
			setC_DocType_ID(MDocType.getDocType(MDocType.DOCBASETYPE_BankStatement));
		}
		// Set beginning balance
		if (! isProcessed() && getBeginningBalance().compareTo(Env.ZERO) == 0)
		{
			MBankAccount ba = getBankAccount();
			ba.load(get_TrxName());
			setBeginningBalance(ba.getCurrentBalance());
		}
		// Calculate ending balance
		setEndingBalance(getBeginningBalance().add(getStatementDifference()));
		return true;
	}	//	beforeSave
	
	/**
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if success
	 */
	@Override
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt
	
	/**	Process Message 			*/
	protected String m_processMsg = null;
	/**	Just Prepared Flag			*/
	protected boolean m_justPrepared = false;

	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	@Override
	public boolean unlockIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("unlockIt - " + toString());
		setProcessing(false);
		return true;
	}	//	unlockIt
	
	/**
	 * 	Invalidate Document
	 * 	@return true if success 
	 */
	@Override
	public boolean invalidateIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("invalidateIt - " + toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt
	
	/**
	 *	Prepare Document
	 * 	@return new status (In Progress or Invalid) 
	 */
	@Override
	public String prepareIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Std Period open?
		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
		MBankStatementLine[] lines = getLines(true);
		if (lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		//	Lines
		BigDecimal total = Env.ZERO;
		for (int i = 0; i < lines.length; i++)
		{
			MBankStatementLine line = lines[i];
			if (!line.isActive())
				continue;

			if (!line.isDateConsistentIfUsedForPosting()) {
				m_processMsg = Msg.getMsg(getCtx(), "BankStatementLinePeriodNotSameAsHeader", new Object[] {line.getLine()});
				return DocAction.STATUS_Invalid;
			}

			total = total.add(line.getStmtAmt());
		}
		setStatementDifference(total);
		setEndingBalance(getBeginningBalance().add(total));

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		
		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	/**
	 * 	Approve Document
	 * 	@return true if success 
	 */
	@Override
	public boolean  approveIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("approveIt - " + toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	/**
	 * 	Reject Approval
	 * 	@return true if success 
	 */
	@Override
	public boolean rejectIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("rejectIt - " + toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt
	
	/**
	 * 	Complete Document
	 * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
	 */
	@Override
	public String completeIt()
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			m_justPrepared = false;
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		// Set the definite document number after completed (if needed)
		setDefiniteDocumentNo();

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		if (log.isLoggable(Level.INFO)) log.info("completeIt - " + toString());
		
		//	Set Payment reconciled
		MBankStatementLine[] lines = getLines(false);
		for (int i = 0; i < lines.length; i++)
		{
			MBankStatementLine line = lines[i];
			if (line.getC_Payment_ID() != 0)
			{
				MPayment payment = new MPayment (getCtx(), line.getC_Payment_ID(), get_TrxName());
				if (payment.isReconciled()) {
					m_processMsg = Msg.getMsg(getCtx(), "PaymentIsAlreadyReconciled") + payment;
					return DocAction.STATUS_Invalid;
				}
				payment.setIsReconciled(true);
				payment.saveEx(get_TrxName());
			}
			else if (line.getC_DepositBatch_ID() != 0)
			{
				MDepositBatchLine[] depositBatchLines = ((MDepositBatch)line.getC_DepositBatch()).getLines();
				for (MDepositBatchLine mDepositBatchLine : depositBatchLines)
				{
					MPayment payment= new MPayment(getCtx(),mDepositBatchLine.getC_Payment_ID(),get_TrxName());
					if (payment.isReconciled()) {
						m_processMsg = Msg.getMsg(getCtx(), "PaymentIsAlreadyReconciled") + payment;
						return DocAction.STATUS_Invalid;
					}
					payment.setIsReconciled(true);
					payment.saveEx(get_TrxName());
				}
			}
		}
		//	Update Bank Account
		MBankAccount ba = getBankAccount();
		ba.load(get_TrxName());
		//BF 1933645
		ba.setCurrentBalance(ba.getCurrentBalance().add(getStatementDifference()));
		ba.saveEx(get_TrxName());
		
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}
		//
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	/**
	 * 	Set the definite document number after completed
	 */
	protected void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			if (this.getProcessedOn().signum() == 0) {
				setStatementDate(TimeUtil.getDay(0));
				if (getDateAcct().before(getStatementDate())) {
					setDateAcct(getStatementDate());
					MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
					if (isPostWithDateFromLine(getAD_Client_ID())) {
						// because the accounting date changed we need to validate again if each line still lands in the same period
						for (MBankStatementLine bl : getLines(false)) {
							if (!bl.isDateConsistentIfUsedForPosting(getDateAcct())) {
								throw new AdempiereException(
										Msg.getMsg(getCtx(), "ParentCannotChange", new Object[] {Msg.getElement(getCtx(), "DateAcct")}) + " - " +
										Msg.getMsg(getCtx(), "BankStatementLinePeriodNotSameAsHeader", new Object[] {bl.getLine()}));
							}
						}
					}
				}
			}
		}
		if (dt.isOverwriteSeqOnComplete()) {
			if (this.getProcessedOn().signum() == 0) {
				String value = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
				if (value != null)
					setDocumentNo(value);
			}
		}
	}

	/**
	 * 	Void Document.
	 * 	@return true if successfully voided 
	 */
	@Override
	public boolean voidIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;
		
		if (DOCSTATUS_Closed.equals(getDocStatus())
			|| DOCSTATUS_Reversed.equals(getDocStatus())
			|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			m_processMsg = "Document Closed: " + getDocStatus();
			setDocAction(DOCACTION_None);
			return false;
		}

		//	Not Processed
		if (DOCSTATUS_Drafted.equals(getDocStatus())
			|| DOCSTATUS_Invalid.equals(getDocStatus())
			|| DOCSTATUS_InProgress.equals(getDocStatus())
			|| DOCSTATUS_Approved.equals(getDocStatus())
			|| DOCSTATUS_NotApproved.equals(getDocStatus()) )
			;
		//	Std Period open?
		else
		{
			MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
			MFactAcct.deleteEx(Table_ID, getC_BankStatement_ID(), get_TrxName());
		}

		if (isProcessed()) {
			//Added Lines by AZ Goodwill
			//Restore Bank Account Balance
			MBankAccount ba = getBankAccount();
			ba.load(get_TrxName());
			ba.setCurrentBalance(ba.getCurrentBalance().subtract(getStatementDifference()));
			ba.saveEx();
			//End of Added Lines
		}

		//	Set lines to 0
		MBankStatementLine[] lines = getLines(true);
		for (int i = 0; i < lines.length; i++)
		{
			MBankStatementLine line = lines[i];
			if (line.getStmtAmt().compareTo(Env.ZERO) != 0)
			{
				StringBuilder description = new StringBuilder(Msg.getMsg(getCtx(), "Voided")).append(" (")
					.append(Msg.translate(getCtx(), "StmtAmt")).append("=").append(line.getStmtAmt());
				if (line.getTrxAmt().compareTo(Env.ZERO) != 0)
					description.append(", ").append(Msg.translate(getCtx(), "TrxAmt")).append("=").append(line.getTrxAmt());
				if (line.getChargeAmt().compareTo(Env.ZERO) != 0)
					description.append(", ").append(Msg.translate(getCtx(), "ChargeAmt")).append("=").append(line.getChargeAmt());
				if (line.getInterestAmt().compareTo(Env.ZERO) != 0)
					description.append(", ").append(Msg.translate(getCtx(), "InterestAmt")).append("=").append(line.getInterestAmt());
				description.append(")");
				line.addDescription(description.toString());
				//
				line.setStmtAmt(Env.ZERO);
				line.setTrxAmt(Env.ZERO);
				line.setChargeAmt(Env.ZERO);
				line.setInterestAmt(Env.ZERO);
				if (line.getC_Payment_ID() != 0)
				{
					MPayment payment = new MPayment (getCtx(), line.getC_Payment_ID(), get_TrxName());
					payment.setIsReconciled(false);
					payment.saveEx();
				}
				else if (line.getC_DepositBatch_ID() != 0)
				{
					MDepositBatchLine[] depositBatchLines = ((MDepositBatch)line.getC_DepositBatch()).getLines();
					for (MDepositBatchLine mDepositBatchLine : depositBatchLines)
					{
						MPayment payment=new MPayment(getCtx(), mDepositBatchLine.getC_Payment_ID(),get_TrxName());
						payment.setIsReconciled(false);
						payment.saveEx();
					}
				}
				line.setC_Payment_ID(0);
				line.saveEx();
			}
		}
		addDescription(Msg.getMsg(getCtx(), "Voided"));
		setStatementDifference(Env.ZERO);
		
		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;		
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}	//	voidIt
	
	/**
	 * 	Close Document.
	 * 	@return true if success 
	 */
	@Override
	public boolean closeIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("closeIt - " + toString());
		// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
		if (m_processMsg != null)
			return false;		

		setDocAction(DOCACTION_None);

		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;
		return true;
	}	//	closeIt
	
	/**
	 * 	Reverse Correction
	 * 	@return true if success 
	 */
	@Override
	public boolean reverseCorrectIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("reverseCorrectIt - " + toString());
		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		return false;
	}	//	reverseCorrectionIt
	
	/**
	 * 	Reverse Accrual
	 * 	@return false 
	 */
	@Override
	public boolean reverseAccrualIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("reverseAccrualIt - " + toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		return false;
	}	//	reverseAccrualIt
	
	/** 
	 * 	Re-activate
	 *  @return true if success 
	 */
	public boolean reActivateIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("reActivateIt - " + toString());
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;		

		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), MDocType.DOCBASETYPE_BankStatement, getAD_Org_ID());

		MFactAcct.deleteEx(Table_ID, getC_BankStatement_ID(), get_TrxName());
		setPosted(false);

		MBankStatementLine[] lines = getLines(true);
		for (int i = 0; i < lines.length; i++) {
			MBankStatementLine line = lines[i];
			line.setProcessed(false);

			if (line.getC_Payment_ID() != 0) {
				MPayment payment = new MPayment (getCtx(), line.getC_Payment_ID(), get_TrxName());
				payment.setIsReconciled(false);
				payment.saveEx();
			}
			else if (line.getC_DepositBatch_ID() != 0)
			{
				MDepositBatchLine[] depositBatchLines = ((MDepositBatch)line.getC_DepositBatch()).getLines();
				for (MDepositBatchLine mDepositBatchLine : depositBatchLines)
				{
					MPayment payment=new MPayment(getCtx(), mDepositBatchLine.getC_Payment_ID(),get_TrxName());
					payment.setIsReconciled(false);
					payment.saveEx(get_TrxName());
				}
			}
		}

		MBankAccount ba = getBankAccount();
		ba.load(get_TrxName());
		ba.setCurrentBalance(ba.getCurrentBalance().subtract(getStatementDifference()));
		ba.saveEx();

		setDocAction(X_C_BankStatement.DOCACTION_Complete);
		setProcessed(false);

		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;		
		return true;
	}	//	reActivateIt
		
	/**
	 * 	Get Summary
	 *	@return Summary of Document
	 */
	@Override
	public String getSummary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getName());
		//	: Total Lines = 123.00 (#1)
		sb.append(": ")
			.append(Msg.translate(getCtx(),"StatementDifference")).append("=").append(getStatementDifference())
			.append(" (#").append(getLines(false).length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary
	
	/**
	 * 	Get Process Message
	 *	@return clear text error message
	 */
	@Override
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	@Override
	public int getDoc_User_ID()
	{
		return getUpdatedBy();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Approval Amount.
	 *	@return Statement Difference
	 */
	@Override
	public BigDecimal getApprovalAmt()
	{
		return getStatementDifference();
	}	//	getApprovalAmt

	/**
	 * 	Get Document Currency
	 *	@return C_Currency_ID
	 */
	@Override
	public int getC_Currency_ID()
	{
		return 0;
	}	//	getC_Currency_ID

	/**
	 * 	Document Status is Complete, Closed or Reversed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	{
		String ds = getDocStatus();
		return DOCSTATUS_Completed.equals(ds) 
			|| DOCSTATUS_Closed.equals(ds)
			|| DOCSTATUS_Reversed.equals(ds);
	}	//	isComplete

	/**
	 * @param clientID
	 * @return true if bank statement posting should use accounting date from bank statement lines.
	 */
	public static boolean isPostWithDateFromLine(int clientID) {
		return MSysConfig.getBooleanValue(MSysConfig.BANK_STATEMENT_POST_WITH_DATE_FROM_LINE, false, Env.getAD_Client_ID(Env.getCtx()));
	}
	
}	//	MBankStatement
