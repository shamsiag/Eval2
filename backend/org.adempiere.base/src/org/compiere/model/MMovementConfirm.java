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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;
import org.compiere.wf.MWorkflow;

/**
 *	Inventory Movement Confirmation
 *	
 *  @author Jorg Janke
 *
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see https://sourceforge.net/p/adempiere/feature-requests/631/
 *  @version $Id: MMovementConfirm.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MMovementConfirm extends X_M_MovementConfirm implements DocAction
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = -3617284116557414217L;

	/**
	 * 	Create Confirmation or return existing one
	 *	@param move movement
	 *	@param checkExisting if false, new confirmation is created
	 *	@return Confirmation
	 */
	public static MMovementConfirm create (MMovement move, boolean checkExisting)
	{
		if (checkExisting)
		{
			MMovementConfirm[] confirmations = move.getConfirmations(false);
			if (confirmations.length > 0)
			{
				MMovementConfirm confirm = confirmations[0];
				return confirm;
			}
		}

		MMovementConfirm confirm = new MMovementConfirm (move);
		confirm.saveEx(move.get_TrxName());
		MMovementLine[] moveLines = move.getLines(false);
		for (int i = 0; i < moveLines.length; i++)
		{
			MMovementLine mLine = moveLines[i];
			MMovementLineConfirm cLine = new MMovementLineConfirm (confirm);
			cLine.setMovementLine(mLine);
			cLine.saveEx(move.get_TrxName());
		}
		return confirm;
	}	//	create
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_MovementConfirm_UU  UUID key
     * @param trxName Transaction
     */
    public MMovementConfirm(Properties ctx, String M_MovementConfirm_UU, String trxName) {
        super(ctx, M_MovementConfirm_UU, trxName);
		if (Util.isEmpty(M_MovementConfirm_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_MovementConfirm_ID id
	 *	@param trxName transaction
	 */
	public MMovementConfirm (Properties ctx, int M_MovementConfirm_ID, String trxName)
	{
		super (ctx, M_MovementConfirm_ID, trxName);
		if (M_MovementConfirm_ID == 0)
			setInitialDefaults();
	}	//	MMovementConfirm

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setDocAction (DOCACTION_Complete);
		setDocStatus (DOCSTATUS_Drafted);
		setIsApproved (false);	// N
		setProcessed (false);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MMovementConfirm (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MMovementConfirm

	/**
	 * 	Parent Constructor
	 *	@param move movement
	 */
	public MMovementConfirm (MMovement move)
	{
		this (move.getCtx(), 0, move.get_TrxName());
		setClientOrg(move);
		setM_Movement_ID(move.getM_Movement_ID());
	}	//	MMovementConfirm
	
	/**	Confirm Lines					*/
	protected MMovementLineConfirm[]	m_lines = null;
	
	/**	Physical Inventory From	*/
	protected MInventory				m_inventoryFrom = null;
	/**	Physical Inventory To	*/
	protected MInventory				m_inventoryTo = null;
	/**	Physical Inventory Info	*/
	protected String					m_inventoryInfo = null;
	protected List<MInventory>		m_inventoryDoc = null;		

	/**
	 * 	Get Lines
	 *	@param requery true to requery from DB
	 *	@return array of lines
	 */
	public MMovementLineConfirm[] getLines (boolean requery)
	{
		if (m_lines != null && !requery) {
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		String sql = "SELECT * FROM M_MovementLineConfirm "
			+ "WHERE M_MovementConfirm_ID=?";
		ArrayList<MMovementLineConfirm> list = new ArrayList<MMovementLineConfirm>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getM_MovementConfirm_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add(new MMovementLineConfirm(getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e); 
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		m_lines = new MMovementLineConfirm[list.size ()];
		list.toArray (m_lines);
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
	 * 	Set Approved
	 *	@param IsApproved approval
	 */
	@Override
	public void setIsApproved (boolean IsApproved)
	{
		if (IsApproved && !isApproved())
		{
			int AD_User_ID = Env.getAD_User_ID(getCtx());
			MUser user = MUser.get(getCtx(), AD_User_ID);
			String info = user.getName() 
				+ ": "
				+ Msg.translate(getCtx(), "IsApproved")
				+ " - " + new Timestamp(System.currentTimeMillis());
			addDescription(info);
		}
		super.setIsApproved (IsApproved);
	}	//	setIsApproved
		
	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	@Override
	public String getDocumentInfo()
	{
		return Msg.getElement(getCtx(), "M_MovementConfirm_ID") + " " + getDocumentNo();
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
	}	//	createPDF

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
		if (!MPeriod.isOpen(getCtx(), getUpdated(), MDocType.DOCBASETYPE_MaterialMovement, getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		
		if (!MAcctSchema.isBackDateTrxAllowed(getCtx(), getUpdated(), get_TrxName()))
		{
			m_processMsg = "@BackDateTrxNotAllowed@";
			return DocAction.STATUS_Invalid;
		}
		
		MMovementLineConfirm[] lines = getLines(true);
		if (lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		for (int i = 0; i < lines.length; i++)
		{
			if (!lines[i].isFullyConfirmed())
			{
				break;
			}
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//
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
		//
		m_inventoryDoc = new ArrayList<MInventory>();
		MMovement move = new MMovement (getCtx(), getM_Movement_ID(), get_TrxName());
		MMovementLineConfirm[] lines = getLines(false);
		for (int i = 0; i < lines.length; i++)
		{
			MMovementLineConfirm confirm = lines[i];
			confirm.set_TrxName(get_TrxName());
			if (!confirm.processLine ())
			{
				m_processMsg = "ShipLine not saved - " + confirm;
				return DocAction.STATUS_Invalid;
			}
			if (confirm.isFullyConfirmed() && confirm.getScrappedQty().signum() == 0)
			{
				confirm.setProcessed(true);
				confirm.saveEx(get_TrxName());
			}
			else
			{
				if (createDifferenceDoc (move, confirm))
				{
					confirm.setProcessed(true);
					confirm.saveEx(get_TrxName());
				}
				else
				{
					log.log(Level.SEVERE, "completeIt - Scrapped=" + confirm.getScrappedQty()
						+ " - Difference=" + confirm.getDifferenceQty());
					
					if (m_processMsg == null)
						m_processMsg = "Difference Doc not created";
					return DocAction.STATUS_Invalid;
				}
			}
		}	//	for all lines
		
		//complete movement
		setProcessed(true);
		saveEx();
		ProcessInfo processInfo = MWorkflow.runDocumentActionWorkflow(move, DocAction.ACTION_Complete);
		if (processInfo.isError()) 
		{
			m_processMsg = processInfo.getSummary();
			setProcessed(false);
			return DocAction.STATUS_Invalid;
		}
				
		if (m_inventoryInfo != null)
		{
			//complete inventory doc
			for(MInventory inventory : m_inventoryDoc)
			{
				processInfo = MWorkflow.runDocumentActionWorkflow(inventory, DocAction.ACTION_Complete);
				if (processInfo.isError()) 
				{
					m_processMsg = processInfo.getSummary();
					setProcessed(false);
					return DocAction.STATUS_Invalid;
				}
			}
			
			m_processMsg = " @M_Inventory_ID@: " + m_inventoryInfo;
			addDescription(Msg.translate(getCtx(), "M_Inventory_ID") 
				+ ": " + m_inventoryInfo);
		}				
		
		m_inventoryDoc = null;
		
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			setProcessed(false);
			return DocAction.STATUS_Invalid;
		}
		
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	/**
	 * 	Create inventory movement line for difference and scrap quantity.<br/>
	 *  Difference - add line to source movement document.<br/>
	 *  Scrap - add line to target movement document.
	 * 	@param move movement
	 *	@param confirm confirm line
	 *	@return true if created
	 */
	protected boolean createDifferenceDoc (MMovement move, MMovementLineConfirm confirm)
	{
		MMovementLine mLine = confirm.getLine();
		
		//	Difference - Create Inventory Difference for Source Location
		if (Env.ZERO.compareTo(confirm.getDifferenceQty()) != 0)
		{
			//	Get Warehouse for Source
			MLocator loc = MLocator.get(getCtx(), mLine.getM_Locator_ID());
			if (m_inventoryFrom != null 
				&& m_inventoryFrom.getM_Warehouse_ID() != loc.getM_Warehouse_ID())
				m_inventoryFrom = null;
			
			if (m_inventoryFrom == null)
			{
				MWarehouse wh = MWarehouse.get(getCtx(), loc.getM_Warehouse_ID());
				m_inventoryFrom = new MInventory (wh, get_TrxName());
				m_inventoryFrom.setDescription(Msg.translate(getCtx(), "M_MovementConfirm_ID") + " " + getDocumentNo());
				setInventoryDocType(m_inventoryFrom);
				if (!m_inventoryFrom.save(get_TrxName()))
				{
					updateProcessMsg("Inventory not created");
					return false;
				}
				//	First Inventory
				if (getM_Inventory_ID() == 0)
				{
					setM_Inventory_ID(m_inventoryFrom.getM_Inventory_ID());
					m_inventoryInfo = m_inventoryFrom.getDocumentNo();
				}
				else
					m_inventoryInfo += "," + m_inventoryFrom.getDocumentNo();
				m_inventoryDoc.add(m_inventoryFrom);
			}
			
			if (log.isLoggable(Level.INFO)) log.info("createDifferenceDoc - Difference=" + confirm.getDifferenceQty());
			MInventoryLine line = new MInventoryLine (m_inventoryFrom, 
					mLine.getM_Locator_ID(), mLine.getM_Product_ID(), mLine.getM_AttributeSetInstance_ID(),
					confirm.getDifferenceQty(), Env.ZERO);
			line.setDescription(Msg.translate(getCtx(), "DifferenceQty"));
			if (!line.save(get_TrxName()))
			{
				updateProcessMsg("Inventory Line not created");
				return false;
			}
			confirm.setM_InventoryLine_ID(line.getM_InventoryLine_ID());
		}	//	Difference
		
		//	Scrapped - Create Inventory Difference for Target Location
		if (Env.ZERO.compareTo(confirm.getScrappedQty()) != 0)
		{
			//	Get Warehouse for Target
			MLocator loc = MLocator.get(getCtx(), mLine.getM_LocatorTo_ID());
			if (m_inventoryTo != null
				&& m_inventoryTo.getM_Warehouse_ID() != loc.getM_Warehouse_ID())
				m_inventoryTo = null;
		
			if (m_inventoryTo == null)
			{
				MWarehouse wh = MWarehouse.get(getCtx(), loc.getM_Warehouse_ID());
				m_inventoryTo = new MInventory (wh, get_TrxName());
				m_inventoryTo.setDescription(Msg.translate(getCtx(), "M_MovementConfirm_ID") + " " + getDocumentNo());
				setInventoryDocType(m_inventoryTo);
				if (!m_inventoryTo.save(get_TrxName()))
				{				
					updateProcessMsg("Inventory not created");
					return false;
				}
				//	First Inventory
				if (getM_Inventory_ID() == 0)
				{
					setM_Inventory_ID(m_inventoryTo.getM_Inventory_ID());
					m_inventoryInfo = m_inventoryTo.getDocumentNo();
				}
				else
					m_inventoryInfo += "," + m_inventoryTo.getDocumentNo();
				m_inventoryDoc.add(m_inventoryTo);
			}
			
			if (log.isLoggable(Level.INFO)) log.info("createDifferenceDoc - Scrapped=" + confirm.getScrappedQty());
			MInventoryLine line = new MInventoryLine (m_inventoryTo, 
				mLine.getM_LocatorTo_ID(), mLine.getM_Product_ID(), mLine.getM_AttributeSetInstance_ID(),
				confirm.getScrappedQty(), Env.ZERO);
			line.setDescription(Msg.translate(getCtx(), "ScrappedQty"));
			if (!line.save(get_TrxName()))
			{
				updateProcessMsg("Inventory Line not created");
				return false;
			}
			confirm.setM_InventoryLine_ID(line.getM_InventoryLine_ID());
		}	//	Scrapped
		
		return true;
	}	//	createDifferenceDoc

	/**
	 * add msg to process message.
	 * @param msg 
	 */
	protected void updateProcessMsg(String msg) {
		if (m_processMsg != null)
			m_processMsg = m_processMsg + " " + msg;
		else
			m_processMsg = msg;
		ValueNamePair error = CLogger.retrieveError();
		if (error != null)
			m_processMsg = m_processMsg + ": " + Msg.getMsg(Env.getCtx(), error.getValue()) + " " + error.getName();
	}

	/**
	 * Set physical inventory doc type id
	 * @param inventory 
	 */
	protected void setInventoryDocType(MInventory inventory) {
		MDocType[] doctypes = MDocType.getOfDocBaseType(Env.getCtx(), X_C_DocType.DOCBASETYPE_MaterialPhysicalInventory);
		for(MDocType doctype : doctypes)
		{
			if (X_C_DocType.DOCSUBTYPEINV_PhysicalInventory.equals(doctype.getDocSubTypeInv()))
			{
				inventory.setC_DocType_ID(doctype.getC_DocType_ID());
				break;
			}
		}
	}

	/**
	 * 	Void Document.
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

		
		MMovement move = new MMovement (getCtx(), getM_Movement_ID(), get_TrxName());
		for (MMovementLineConfirm confirmLine : getLines(true))
		{
			confirmLine.setTargetQty(Env.ZERO);
			confirmLine.setConfirmedQty(Env.ZERO);
			confirmLine.setScrappedQty(Env.ZERO);
			confirmLine.setDifferenceQty(Env.ZERO);
			confirmLine.setProcessed(true);
			confirmLine.saveEx();
		}
		
		// set confirmation as processed to allow voiding the inventory move
		setProcessed(true);
		saveEx();

		// voiding the confirmation voids also the inventory move
		ProcessInfo processInfo = MWorkflow.runDocumentActionWorkflow(move, DocAction.ACTION_Void);
		if (processInfo.isError())
		{
			m_processMsg = processInfo.getSummary();
			setProcessed(false);
			return false;
		}

		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;

		setDocAction(DOCACTION_None);
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

		//	Close Not delivered Qty
		setDocAction(DOCACTION_None);

		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;
		return true;
	}	//	closeIt
	
	/**
	 * 	Reverse Correction
	 * 	@return not implemented, always return false 
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
	 * 	@return not implemented, always return false 
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
	 * 	@return not implemented, always return false 
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
		return getUpdatedBy();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Currency
	 *	@return 0
	 */
	@Override
	public int getC_Currency_ID()
	{
		return 0;
	}	//	getC_Currency_ID

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

}	//	MMovementConfirm
