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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;

/**
 *  Journal Batch Model
 *
 *	@author Jorg Janke
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li>FR [ 1948157  ]  Is necessary the reference for document reverse
 *  		@see https://sourceforge.net/p/adempiere/feature-requests/412/
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see https://sourceforge.net/p/adempiere/feature-requests/631/
 *  @author Teo Sarca, www.arhipac.ro
 * 			<li>FR [ 1776045 ] Add ReActivate action to GL Journal
 *	@version $Id: MJournalBatch.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MJournalBatch extends X_GL_JournalBatch implements DocAction
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 4447134860127309777L;

	/**
	 * 	Create new Journal Batch by copying
	 * 	@param ctx context
	 *	@param GL_JournalBatch_ID journal batch
	 * 	@param dateDoc date of the document date
	 *	@param trxName transaction
	 *	@return Journal Batch
	 */
	public static MJournalBatch copyFrom (Properties ctx, int GL_JournalBatch_ID, 
		Timestamp dateDoc, String trxName)
	{
		MJournalBatch from = new MJournalBatch (ctx, GL_JournalBatch_ID, trxName);
		if (from.getGL_JournalBatch_ID() == 0)
			throw new IllegalArgumentException ("From Journal Batch not found GL_JournalBatch_ID=" + GL_JournalBatch_ID);
		//
		MJournalBatch to = new MJournalBatch (ctx, 0, trxName);
		PO.copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.set_ValueNoCheck ("DocumentNo", null);
		to.set_ValueNoCheck ("C_Period_ID", null);
		to.setDateAcct(dateDoc);
		to.setDateDoc(dateDoc);
		to.setDocStatus(DOCSTATUS_Drafted);
		to.setDocAction(DOCACTION_Complete);
		to.setIsApproved(false);
		to.setProcessed (false);
		//
		if (!to.save())
			throw new IllegalStateException("Could not create Journal Batch");

		if (to.copyDetailsFrom(from) == 0)
			throw new IllegalStateException("Could not create Journal Batch Details");

		return to;
	}	//	copyFrom
		
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param GL_JournalBatch_UU  UUID key
     * @param trxName Transaction
     */
    public MJournalBatch(Properties ctx, String GL_JournalBatch_UU, String trxName) {
        super(ctx, GL_JournalBatch_UU, trxName);
		if (Util.isEmpty(GL_JournalBatch_UU))
			setInitialDefaults();
    }

	/**
	 *	@param ctx context
	 *	@param GL_JournalBatch_ID id if 0 - create actual batch
	 *	@param trxName transaction
	 */
	public MJournalBatch (Properties ctx, int GL_JournalBatch_ID, String trxName)
	{
		super (ctx, GL_JournalBatch_ID, trxName);
		if (GL_JournalBatch_ID == 0)
			setInitialDefaults();
	}	//	MJournalBatch

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setPostingType (POSTINGTYPE_Actual);
		setDocAction (DOCACTION_Complete);
		setDocStatus (DOCSTATUS_Drafted);
		setTotalCr (Env.ZERO);
		setTotalDr (Env.ZERO);
		setProcessed (false);
		setProcessing (false);
		setIsApproved(false);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MJournalBatch (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MJournalBatch

	/**
	 * 	Copy Constructor.
	 * 	Does not copy: Dates/Period
	 *	@param original original
	 */
	public MJournalBatch (MJournalBatch original)
	{
		this (original.getCtx(), 0, original.get_TrxName());
		setClientOrg(original);
		setGL_Category_ID(original.getGL_Category_ID());
		setPostingType(original.getPostingType());
		setDescription(original.getDescription());
		setC_DocType_ID(original.getC_DocType_ID());
		setControlAmt(original.getControlAmt());
		//
		setC_Currency_ID(original.getC_Currency_ID());
	}	//	MJournal
	
	/**
	 * 	Overwrite Client/Org if required
	 * 	@param AD_Client_ID client
	 * 	@param AD_Org_ID org
	 */
	@Override
	public void setClientOrg (int AD_Client_ID, int AD_Org_ID)
	{
		super.setClientOrg(AD_Client_ID, AD_Org_ID);
	}	//	setClientOrg

	/**
	 * 	Set Accounting Date.
	 * 	Set also Period if not set earlier.
	 *	@param DateAcct date
	 */
	@Override
	public void setDateAcct (Timestamp DateAcct)
	{
		super.setDateAcct(DateAcct);
		if (DateAcct == null)
			return;
		int C_Period_ID = MPeriod.getC_Period_ID(getCtx(), DateAcct, getAD_Org_ID());
		if (C_Period_ID == 0)
			log.saveError("PeriodNotFound", " : " + DisplayType.getDateFormat().format(getDateAcct()));
		else if (C_Period_ID != getC_Period_ID())
			setC_Period_ID(C_Period_ID);
	}	//	setDateAcct

	/**
	 * 	Get Journal Lines
	 * 	@param requery ignore
	 *	@return Array of lines
	 */
	public MJournal[] getJournals (boolean requery)
	{
		ArrayList<MJournal> list = new ArrayList<MJournal>();
		String sql = "SELECT * FROM GL_Journal WHERE GL_JournalBatch_ID=? ORDER BY DocumentNo";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getGL_JournalBatch_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add(new MJournal (getCtx(), rs, get_TrxName()));
		}
		catch (SQLException ex)
		{
			log.log(Level.SEVERE, sql, ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//
		MJournal[] retValue = new MJournal[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getJournals

	/**
	 * 	Copy Journal/Lines from other Journal Batch
	 *	@param jb Journal Batch
	 *	@return number of journals + lines copied
	 */
	public int copyDetailsFrom (MJournalBatch jb)
	{
		if (isProcessed() || jb == null)
			return 0;
		int count = 0;
		int lineCount = 0;
		MJournal[] fromJournals = jb.getJournals(false);
		for (int i = 0; i < fromJournals.length; i++)
		{
			MJournal toJournal = new MJournal (getCtx(), 0, jb.get_TrxName());
			PO.copyValues(fromJournals[i], toJournal, getAD_Client_ID(), getAD_Org_ID());
			toJournal.setGL_JournalBatch_ID(getGL_JournalBatch_ID());
			toJournal.set_ValueNoCheck ("DocumentNo", null);	//	create new
			toJournal.set_ValueNoCheck ("C_Period_ID", null);
			toJournal.setDateDoc(getDateDoc());		//	dates from this Batch
			toJournal.setDateAcct(getDateAcct());
			toJournal.setDocStatus(MJournal.DOCSTATUS_Drafted);
			toJournal.setDocAction(MJournal.DOCACTION_Complete);
			toJournal.setTotalCr(Env.ZERO);
			toJournal.setTotalDr(Env.ZERO);
			toJournal.setIsApproved(false);
			toJournal.setIsPrinted(false);
			toJournal.setPosted(false);
			toJournal.setProcessed(false);
			if (toJournal.save())
			{
				count++;
				lineCount += toJournal.copyLinesFrom(fromJournals[i], getDateAcct(), 'x');
			}
		}
		if (fromJournals.length != count)
			log.log(Level.SEVERE, "Line difference - Journals=" + fromJournals.length + " <> Saved=" + count);

		return count + lineCount;
	}	//	copyLinesFrom
	
	/**
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if performed
	 */
	@Override
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	process
	
	/**	Process Message 			*/
	protected String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	protected boolean		m_justPrepared = false;

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
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());

		//	Std Period open?
		if (!MPeriod.isOpen(getCtx(), getDateAcct(), dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		
		//	Add up Amounts & prepare them
		MJournal[] journals = getJournals(false);
		if (journals.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		
		BigDecimal TotalDr = Env.ZERO;
		BigDecimal TotalCr = Env.ZERO;		
		for (int i = 0; i < journals.length; i++)
		{
			MJournal journal = journals[i];
			if (!journal.isActive())
				continue;
			//	Prepare if not closed
			if (DOCSTATUS_Closed.equals(journal.getDocStatus())
				|| DOCSTATUS_Voided.equals(journal.getDocStatus())
				|| DOCSTATUS_Reversed.equals(journal.getDocStatus())
				|| DOCSTATUS_Completed.equals(journal.getDocStatus()))
				;
			else
			{
				String status = journal.prepareIt();
				if (!DocAction.STATUS_InProgress.equals(status))
				{
					journal.setDocStatus(status);
					journal.saveEx();
					m_processMsg = journal.getProcessMsg();
					return status;
				}
				journal.setDocStatus(DOCSTATUS_InProgress);
				journal.saveEx();
			}
			//
			TotalDr = TotalDr.add(journal.getTotalDr());
			TotalCr = TotalCr.add(journal.getTotalCr());
		}
		setTotalDr(TotalDr);
		setTotalCr(TotalCr);
		
		//	Control Amount
		if (Env.ZERO.compareTo(getControlAmt()) != 0
			&& getControlAmt().compareTo(getTotalDr()) != 0)
		{
			m_processMsg = "@ControlAmtError@";
			return DocAction.STATUS_Invalid;
		}
		
//		 Bug 1353695 Currency Rate and COnbversion Type should get copied from journal to lines
		for (int i = 0; i < journals.length; i++) 
		{
			MJournal journal = journals[i];
			MJournalLine[] lines = journal.getLines(true);
			if (journal.getCurrencyRate() != null && journal.getCurrencyRate().compareTo(Env.ZERO) != 0)
			{
				for (int j = 0; j < lines.length; j++) 
				{
					MJournalLine line = lines[j];
					line.setCurrencyRate(journal.getCurrencyRate());
					line.saveEx();
				}
			}
			if (journal.getC_ConversionType_ID() > 0)
			{
				for (int j = 0; j < lines.length; j++) 
				{
					MJournalLine line = lines[j];
					line.setC_ConversionType_ID(journal.getC_ConversionType_ID());
					line.saveEx();
				}
			}
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//	Add up Amounts
		m_justPrepared = true;
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
		if (log.isLoggable(Level.INFO)) log.info("completeIt - " + toString());
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
		approveIt();

		//	Add up Amounts & complete them
		MJournal[] journals = getJournals(true);
		BigDecimal TotalDr = Env.ZERO;
		BigDecimal TotalCr = Env.ZERO;		
		for (int i = 0; i < journals.length; i++)
		{
			MJournal journal = journals[i];
			if (!journal.isActive())
			{
				journal.setProcessed(true);
				journal.setDocStatus(DOCSTATUS_Voided);
				journal.setDocAction(DOCACTION_None);
				journal.saveEx();
				continue;
			}
			//	Complete if not closed
			if (DOCSTATUS_Closed.equals(journal.getDocStatus())
				|| DOCSTATUS_Voided.equals(journal.getDocStatus())
				|| DOCSTATUS_Reversed.equals(journal.getDocStatus())
				|| DOCSTATUS_Completed.equals(journal.getDocStatus()))
				;
			else
			{
				// added AdempiereException by zuhri
				if (!journal.processIt(DocAction.ACTION_Complete))
					throw new AdempiereException(Msg.getMsg(getCtx(), "FailedProcessingDocument") + " - " + journal.getProcessMsg());
				// end added
				journal.saveEx();
				if (!DocAction.STATUS_Completed.equals(journal.getDocStatus()))
				{
					m_processMsg = journal.getProcessMsg();
					return journal.getDocStatus();
				}
			}
			//
			TotalDr = TotalDr.add(journal.getTotalDr());
			TotalCr = TotalCr.add(journal.getTotalCr());
		}
		setTotalDr(TotalDr);
		setTotalCr(TotalCr);
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
			setDateDoc(TimeUtil.getDay(0));
			if (getDateAcct().before(getDateDoc())) {
				setDateAcct(getDateDoc());
				MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
			}
		}
		if (dt.isOverwriteSeqOnComplete()) {
			String value = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
			if (value != null)
				setDocumentNo(value);
		}
	}

	/**
	 * 	Void Document.
	 * 	@return false 
	 */
	@Override
	public boolean voidIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("voidIt - " + toString());
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;
		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;
		
		return false;
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
		
		MJournal[] journals = getJournals(true);
		for (int i = 0; i < journals.length; i++)
		{
			MJournal journal = journals[i];
			if (!journal.isActive() && !journal.isProcessed())
			{
				journal.setProcessed(true);
				journal.setDocStatus(DOCSTATUS_Voided);
				journal.setDocAction(DOCACTION_None);
				journal.saveEx();
				continue;
			}
			if (DOCSTATUS_Drafted.equals(journal.getDocStatus())
				|| DOCSTATUS_InProgress.equals(journal.getDocStatus())
				|| DOCSTATUS_Invalid.equals(journal.getDocStatus()))
			{
				m_processMsg = "Journal not Completed: " + journal.getSummary();
				return false;
			}
			
			//	Close if not closed
			if (DOCSTATUS_Closed.equals(journal.getDocStatus())
				|| DOCSTATUS_Voided.equals(journal.getDocStatus())
				|| DOCSTATUS_Reversed.equals(journal.getDocStatus()))
				;
			else
			{
				if (!journal.closeIt())
				{
					m_processMsg = "Cannot close: " + journal.getSummary();
					return false;
				}
				journal.saveEx();
			}
		}
		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;
		
		return true;
	}	//	closeIt
	
	/**
	 * 	Reverse Correction.
	 * 	Flip Dr/Cr - Use this document's date.
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
				
		MJournal[] journals = getJournals(true);
		//	check prerequisites
		for (int i = 0; i < journals.length; i++)
		{
			MJournal journal = journals[i];
			if (!journal.isActive())
				continue;
			//	All need to be closed/Completed
			if (DOCSTATUS_Completed.equals(journal.getDocStatus()))
				;
			else
			{
				m_processMsg = "All Journals need to be Completed: " + journal.getSummary();
				return false;
			}
		}
		
		//	Reverse it
		MJournalBatch reverse = new MJournalBatch (this);
		reverse.setDateDoc(getDateDoc());
		reverse.setC_Period_ID(getC_Period_ID());
		reverse.setDateAcct(getDateAcct());
		//	Reverse indicator
		StringBuilder msgd = new StringBuilder("(->").append(getDocumentNo()).append(")");
		reverse.addDescription(msgd.toString());
		reverse.setControlAmt(getControlAmt().negate());
		//[ 1948157  ]
		reverse.setReversal_ID(getGL_JournalBatch_ID());
		reverse.saveEx();
		
		//	Reverse Journals
		for (int i = 0; i < journals.length; i++)
		{
			MJournal journal = journals[i];
			if (!journal.isActive())
				continue;
			if (journal.reverseCorrectIt(reverse.getGL_JournalBatch_ID()) == null)
			{
				m_processMsg = "Could not reverse " + journal;
				return false;
			}
			journal.saveEx();
		}
		//
		if (!reverse.processIt(DocAction.ACTION_Complete))
		{
			m_processMsg = "Reversal ERROR: " + reverse.getProcessMsg();
			return false;
		}
		reverse.closeIt();
		reverse.setProcessing(false);
		reverse.setDocStatus(DOCSTATUS_Reversed);
		reverse.setDocAction(DOCACTION_None);
		reverse.saveEx(get_TrxName());
		//
		msgd = new StringBuilder("(").append(reverse.getDocumentNo()).append("<-)");
		addDescription(msgd.toString());

		setProcessed(true);
		//[ 1948157  ]
		setReversal_ID(reverse.getGL_JournalBatch_ID());
		setDocStatus(DOCSTATUS_Reversed);
		setDocAction(DOCACTION_None);
		saveEx();
		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		return true;
	}	//	reverseCorrectionIt
	
	/**
	 * 	Reverse Accrual.
	 * 	Flip Dr/Cr - Use Today's date.
	 * 	@return true if success 
	 */
	@Override
	public boolean reverseAccrualIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("reverseAccrualIt - " + toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		MJournal[] journals = getJournals(true);
		//	check prerequisites
		for (int i = 0; i < journals.length; i++)
		{
			MJournal journal = journals[i];
			if (!journal.isActive())
				continue;
			//	All need to be closed/Completed
			if (DOCSTATUS_Completed.equals(journal.getDocStatus()))
				;
			else
			{
				m_processMsg = "All Journals need to be Completed: " + journal.getSummary();
				return false;
			}
		}
		//	Reverse it
		MJournalBatch reverse = new MJournalBatch (this);
		reverse.setC_Period_ID(0);
		Timestamp reversalDate = Env.getContextAsDate(getCtx(), Env.DATE);
		if (reversalDate == null) {
			reversalDate = new Timestamp(System.currentTimeMillis());
		}
		reverse.setDateDoc(reversalDate);
		reverse.setDateAcct(reversalDate);
		//	Reverse indicator
		StringBuilder msgd = new StringBuilder("(->").append(getDocumentNo()).append(")");
		reverse.addDescription(msgd.toString());
		reverse.setReversal_ID(getGL_JournalBatch_ID());
		reverse.saveEx();
		
		//	Reverse Journals
		for (int i = 0; i < journals.length; i++)
		{
			MJournal journal = journals[i];
			if (!journal.isActive())
				continue;
			if (journal.reverseAccrualIt(reverse.getGL_JournalBatch_ID()) == null)
			{
				m_processMsg = "Could not reverse " + journal;
				return false;
			}
			journal.saveEx();
		}
		//
		if (!reverse.processIt(DocAction.ACTION_Complete))
		{
			m_processMsg = "Reversal ERROR: " + reverse.getProcessMsg();
			return false;
		}
		reverse.closeIt();
		reverse.setProcessing(false);
		reverse.setDocStatus(DOCSTATUS_Reversed);
		reverse.setDocAction(DOCACTION_None);
		reverse.saveEx(get_TrxName());
		//
		msgd = new StringBuilder("(").append(reverse.getDocumentNo()).append("<-)");
		addDescription(msgd.toString());

		setProcessed(true);
		setReversal_ID(reverse.getGL_JournalBatch_ID());
		setDocStatus(DOCSTATUS_Reversed);
		setDocAction(DOCACTION_None);
		saveEx();
		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
				
		return true;
	}	//	reverseAccrualIt
	
	/** 
	 * 	Re-activate document and delete Fact_Acct entries.
	 * 	@return true if success 
	 */
	@Override
	public boolean reActivateIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("reActivateIt - " + toString());
		
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;	
		
		for (MJournal journal : getJournals(true))
		{
			if (DOCSTATUS_Completed.equals(journal.getDocStatus()))
			{
				if (journal.processIt(DOCACTION_Re_Activate))
				{
					journal.saveEx();
				}
				else
				{
					throw new AdempiereException(journal.getProcessMsg());
				}
			}
		}
		setProcessed(false);
		setDocAction(DOCACTION_Complete);

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
		sb.append(getDocumentNo());
		//	: Total Lines = 123.00 (#1)
		sb.append(": ")
		.append(Msg.translate(getCtx(),"TotalDr")).append("=").append(getTotalDr())
		.append(" ")
		.append(Msg.translate(getCtx(),"TotalCR")).append("=").append(getTotalCr())
		.append(" (#").append(getJournals(false).length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MJournalBatch[");
		sb.append(get_ID()).append(",").append(getDescription())
			.append(",DR=").append(getTotalDr())
			.append(",CR=").append(getTotalCr())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	@Override
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		StringBuilder msgreturn = new StringBuilder().append(dt.getNameTrl()).append(" ").append(getDocumentNo());
		return msgreturn.toString();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
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
	 *	@return AD_User_ID (Created By)
	 */
	@Override
	public int getDoc_User_ID()
	{
		return getCreatedBy();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Approval Amount
	 *	@return DR amount
	 */
	@Override
	public BigDecimal getApprovalAmt()
	{
		return getTotalDr();
	}	//	getApprovalAmt
	
	/**
	 * append description to current Description text
	 * @param description
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
	}
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Set DateDoc and DateAcct to today date if still null
		if (getDateDoc() == null)
		{
			if (getDateAcct() == null)
				setDateDoc(new Timestamp(System.currentTimeMillis()));
			else
				setDateDoc(getDateAcct());
		}
		if (getDateAcct() == null)
		{
			setDateAcct(getDateDoc());
			if (CLogger.peekError() != null)
				return false;
		}
		else if (!isProcessed())
		{
			// Validate period for DateAcct
			int C_Period_ID = MPeriod.getC_Period_ID(getCtx(), getDateAcct(), getAD_Org_ID());
			if (C_Period_ID == 0)
			{
				log.saveError("PeriodNotFound", " : " + DisplayType.getDateFormat().format(getDateAcct()));
				return false;
			}
			else if (C_Period_ID != getC_Period_ID())
			{
				/* special case when assigning an adjustment period */
				MPeriod currentPeriod = MPeriod.get(getCtx(), getC_Period_ID());
				if (currentPeriod.isStandardPeriod())
					setC_Period_ID(C_Period_ID);
			}
		}
		
		return true;
	}

	/**
	 * 	Document Status is Complete or Closed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	{
		String ds = getDocStatus();
		return DOCSTATUS_Completed.equals(ds)
			|| DOCSTATUS_Closed.equals(ds)
			|| DOCSTATUS_Reversed.equals(ds);
	}	//	isComplete

}	//	MJournalBatch
