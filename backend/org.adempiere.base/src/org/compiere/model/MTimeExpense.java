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

import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Time + Expense Model
 *
 *	@author Jorg Janke
 *
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see https://sourceforge.net/p/adempiere/feature-requests/631/
 *	@version $Id: MTimeExpense.java,v 1.4 2006/07/30 00:51:03 jjanke Exp $
 */
public class MTimeExpense extends X_S_TimeExpense implements DocAction
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 1567303438502090279L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param S_TimeExpense_UU  UUID key
     * @param trxName Transaction
     */
    public MTimeExpense(Properties ctx, String S_TimeExpense_UU, String trxName) {
        super(ctx, S_TimeExpense_UU, trxName);
		if (Util.isEmpty(S_TimeExpense_UU))
			setInitialDefaults();
    }

	/**
	 * 	Default Constructor
	 *	@param ctx context
	 *	@param S_TimeExpense_ID id
	 *	@param trxName transaction
	 */
	public MTimeExpense (Properties ctx, int S_TimeExpense_ID, String trxName)
	{
		super (ctx, S_TimeExpense_ID, trxName);
		if (S_TimeExpense_ID == 0)
			setInitialDefaults();
	}	//	MTimeExpense

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setDateReport (new Timestamp (System.currentTimeMillis ()));
		setIsApproved (false);
		super.setProcessed (false);
		setProcessing(false);
	}

	/**
	 * 	Load Constructor
	 * 	@param ctx context
	 * 	@param rs result set
	 *	@param trxName transaction
	 */
	public MTimeExpense (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MTimeExpense

	/** Default Locator				*/
	private int					m_M_Locator_ID = 0;
	/**	Lines						*/
	private MTimeExpenseLine[]	m_lines = null;
	/** Cached User					*/
	private int					m_AD_User_ID = 0;
	
	/**
	 * 	Get Lines Convenience Wrapper
	 *	@return array of lines
	 */
	public MTimeExpenseLine[] getLines ()
	{
		return getLines(true);
	}

	/**
	 * 	Get Lines
	 * 	@param requery true to reload from DB
	 *	@return array of lines
	 */
	public MTimeExpenseLine[] getLines (boolean requery)
	{
		if (m_lines != null && !requery) {
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		//
		int C_Currency_ID = getC_Currency_ID();
		ArrayList<MTimeExpenseLine> list = new ArrayList<MTimeExpenseLine>();
		//
		String sql = "SELECT * FROM S_TimeExpenseLine WHERE S_TimeExpense_ID=? ORDER BY Line,S_TimeExpenseLine_ID";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getS_TimeExpense_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MTimeExpenseLine te = new MTimeExpenseLine(getCtx(), rs, get_TrxName());
				te.setC_Currency_Report_ID(C_Currency_ID);
				list.add(te);
			}
		}
		catch (SQLException ex)
		{
			log.log(Level.SEVERE, "getLines", ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//
		m_lines = new MTimeExpenseLine[list.size()];
		list.toArray(m_lines);
		return m_lines;
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
		else
			setDescription(desc + " | " + description);
	}	//	addDescription

	/**
	 *	Get Default Locator (from Warehouse)
	 *	@return locator
	 */
	public int getM_Locator_ID()
	{
		if (m_M_Locator_ID != 0)
			return m_M_Locator_ID;
		//
		String sql = "SELECT M_Locator_ID FROM M_Locator "
			+ "WHERE M_Warehouse_ID=? AND IsActive='Y' ORDER BY IsDefault DESC, Created";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, getM_Warehouse_ID());
			rs = pstmt.executeQuery ();
			if (rs.next ())
				m_M_Locator_ID = rs.getInt(1);
		}
		catch (SQLException ex)
		{
			log.log(Level.SEVERE, "getM_Locator_ID", ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//
		return m_M_Locator_ID;
	}	//	getM_Locator_ID

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
		String sql = "UPDATE S_TimeExpenseLine SET Processed='"
			+ (processed ? "Y" : "N")
			+ "' WHERE S_TimeExpense_ID=" + getS_TimeExpense_ID();
		int noLine = DB.executeUpdate(sql, get_TrxName());
		m_lines = null;
		if (log.isLoggable(Level.FINE)) log.fine(processed + " - Lines=" + noLine);
	}	//	setProcessed
	
	/**
	 * 	Get Document Info
	 *	@return document info
	 */
	@Override
	public String getDocumentInfo()
	{
		return Msg.getElement(getCtx(), "S_TimeExpense_ID") + " " + getDocumentNo();
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
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
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
	}	//	processIt
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;

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

		//	Std Period open? - AP (Reimbursement) Invoice
		if (!MPeriod.isOpen(getCtx(), getDateReport(), MDocType.DOCBASETYPE_APInvoice, getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		
		MTimeExpenseLine[] lines = getLines(false);
		if (lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		//	Add up Amounts
		BigDecimal amt = Env.ZERO;
		for (int i = 0; i < lines.length; i++)
		{
			MTimeExpenseLine line = lines[i];
			amt = amt.add(line.getApprovalAmt());
		}
		setApprovalAmt(amt);

		//	Invoiced but no BP
		for (int i = 0; i < lines.length; i++)
		{
			MTimeExpenseLine line = lines[i];
			if (line.isInvoiced() && line.getC_BPartner_ID() == 0)
			{
				m_processMsg = "@Line@ " + line.getLine() + ": Invoiced, but no Business Partner";
				return DocAction.STATUS_Invalid;
			}
		}
		
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
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		if (log.isLoggable(Level.INFO)) log.info("completeIt - " + toString());

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
	 * 	Void Document.
	 * 	Same as Close.
	 * 	@return true if success 
	 */
	@Override
	public boolean voidIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("voidIt - " + toString());

		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;
		
		if (!closeIt())
			return false;
		
		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;
		
		return true;
	}	//	voidIt
	
	/**
	 * 	Close Document.
	 * 	Cancel not delivered Quantities.
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
		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;

		return true;
	}	//	closeIt
	
	/**
	 * 	Reverse Correction.<br/>
	 *  Not implemented, always return false.
	 * 	@return false 
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
	 * 	Reverse Accrual. <br/>
	 *  Not implemented, always return false.
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
	 * 	Re-activate. <br/>
	 *  Not implemented, always return false.
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
		
		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;
		
		return false;
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
			.append(Msg.translate(getCtx(),"ApprovalAmt")).append("=").append(getApprovalAmt())
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
		if (m_AD_User_ID != 0)
			return m_AD_User_ID;
		if (getC_BPartner_ID() != 0)
		{
			MUser[] users = MUser.getOfBPartner(getCtx(), getC_BPartner_ID(), get_TrxName());
			if (users.length > 0)
			{
				m_AD_User_ID = users[0].getAD_User_ID();
				return m_AD_User_ID;
			}
		}
		return getCreatedBy();
	}	//	getDoc_User_ID
	
	/**
	 * 	Get Document Currency
	 *	@return C_Currency_ID of price list
	 */
	@Override
	public int getC_Currency_ID()
	{
		MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID(), get_TrxName());
		return pl.getC_Currency_ID();
	}	//	getC_Currency_ID

	/**
	 * 	Document Status is Complete, Closed or Reverse
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	{
		String ds = getDocStatus();
		return DOCSTATUS_Completed.equals(ds) 
			|| DOCSTATUS_Closed.equals(ds)
			|| DOCSTATUS_Reversed.equals(ds);
	}	//	isComplete
	
}	//	MTimeExpense
