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
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * 	Project Model
 *
 *	@author Jorg Janke
 *	@version $Id: MProject.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
public class MProject extends X_C_Project
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 8631795136761641303L;

	/**
	 * 	Create new project by copying from another project
	 * 	@param ctx context
	 *	@param C_Project_ID project to copy from
	 * 	@param dateDoc date of the document date
	 *	@param trxName transaction
	 *	@return new Project instance
	 */
	public static MProject copyFrom (Properties ctx, int C_Project_ID, Timestamp dateDoc, String trxName)
	{
		MProject from = new MProject (ctx, C_Project_ID, trxName);
		if (from.getC_Project_ID() == 0)
			throw new IllegalArgumentException ("From Project not found C_Project_ID=" + C_Project_ID);
		//
		MProject to = new MProject (ctx, 0, trxName);
		PO.copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.set_ValueNoCheck ("C_Project_ID", I_ZERO);
		//	Set Value with Time
		String Value = to.getValue() + " ";
		String Time = dateDoc.toString();
		int length = Value.length() + Time.length();
		if (length <= 40)
			Value += Time;
		else
			Value += Time.substring (length-40);
		to.setValue(Value);
		to.setInvoicedAmt(Env.ZERO);
		to.setProjectBalanceAmt(Env.ZERO);
		to.setProcessed(false);
		//
		if (!to.save())
			throw new IllegalStateException("Could not create Project");

		if (to.copyDetailsFrom(from) == 0)
			throw new IllegalStateException("Could not create Project Details");

		return to;
	}	//	copyFrom
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_Project_UU  UUID key
     * @param trxName Transaction
     */
    public MProject(Properties ctx, String C_Project_UU, String trxName) {
        super(ctx, C_Project_UU, trxName);
		if (Util.isEmpty(C_Project_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_Project_ID id
	 *	@param trxName transaction
	 */
	public MProject (Properties ctx, int C_Project_ID, String trxName)
	{
		super (ctx, C_Project_ID, trxName);
		if (C_Project_ID == 0)
			setInitialDefaults();
	}	//	MProject

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setCommittedAmt (Env.ZERO);
		setCommittedQty (Env.ZERO);
		setInvoicedAmt (Env.ZERO);
		setInvoicedQty (Env.ZERO);
		setPlannedAmt (Env.ZERO);
		setPlannedMarginAmt (Env.ZERO);
		setPlannedQty (Env.ZERO);
		setProjectBalanceAmt (Env.ZERO);
		setProjInvoiceRule(PROJINVOICERULE_None);
		setProjectLineLevel(PROJECTLINELEVEL_Project);
		setIsCommitCeiling (false);
		setIsCommitment (false);
		setIsSummary (false);
		setProcessed (false);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MProject (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProject

	/**	Cached PL			*/
	private int		m_M_PriceList_ID = 0;

	/**
	 * 	Get Project Type as Int (is Button).
	 *	@return C_ProjectType_ID id
	 *  @deprecated
	 */
	@Deprecated
	public int getC_ProjectType_ID_Int()
	{
		return getC_ProjectType_ID();		
	}	//	getC_ProjectType_ID_Int

	/**
	 *	String Representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder ("MProject[").append(get_ID())
			.append("-").append(getValue()).append(",ProjectCategory=").append(getProjectCategory())
			.append("]");
		return sb.toString();
	}	//	toString

	/**
	 * 	Get Price List from Price List Version
	 *	@return M_PriceList_ID or 0
	 */
	public int getM_PriceList_ID()
	{
		if (getM_PriceList_Version_ID() == 0)
			return 0;
		if (m_M_PriceList_ID > 0)
			return m_M_PriceList_ID;
		//
		String sql = "SELECT M_PriceList_ID FROM M_PriceList_Version WHERE M_PriceList_Version_ID=?";
		m_M_PriceList_ID = DB.getSQLValue(null, sql, getM_PriceList_Version_ID());
		return m_M_PriceList_ID;
	}	//	getM_PriceList_ID

	/**
	 * 	Set PL Version
	 *	@param M_PriceList_Version_ID id
	 */
	public void setM_PriceList_Version_ID (int M_PriceList_Version_ID)
	{
		super.setM_PriceList_Version_ID(M_PriceList_Version_ID);
		m_M_PriceList_ID = 0;	//	reset
	}	//	setM_PriceList_Version_ID

	/**
	 * 	Get Project Lines
	 *	@return Array of lines
	 */
	public MProjectLine[] getLines()
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		final String whereClause = "C_Project_ID=?";
		List <MProjectLine> list = new Query(getCtx(), I_C_ProjectLine.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("Line,C_ProjectLine_ID")
			.list();
		//
		MProjectLine[] retValue = new MProjectLine[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getLines

	/**
	 * 	Get Project Lines of a Phase
	 *  @param phase C_ProjectPhase_ID
	 *	@return Array of lines of a Phase
	 */
	public MProjectLine[] getPhaseLines(int phase)
	{
		final String whereClause = "C_Project_ID=? and C_ProjectPhase_ID=?";
		List <MProjectLine> list = new Query(getCtx(), I_C_ProjectLine.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID(), phase)
			.setOrderBy("Line")
			.list();
		//
		MProjectLine[] retValue = new MProjectLine[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getPhaseLines

	/**
	 * 	Get Project Issue records
	 *	@return Array of project issue
	 */
	public MProjectIssue[] getIssues()
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		String whereClause = "C_Project_ID=?";
		List <MProjectIssue> list = new Query(getCtx(), I_C_ProjectIssue.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("Line")
			.list();
		//
		MProjectIssue[] retValue = new MProjectIssue[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getIssues

	/**
	 * 	Get Project Phase records
	 *	@return Array of project phase
	 */
	public MProjectPhase[] getPhases()
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		String whereClause = "C_Project_ID=?";
		List <MProjectPhase> list = new Query(getCtx(), I_C_ProjectPhase.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("SeqNo")
			.list();
		//
		MProjectPhase[] retValue = new MProjectPhase[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getPhases
	
	/**
	 * 	Copy Lines/Phase/Task from other Project
	 *	@param project project
	 *	@return number of lines copied
	 */
	public int copyDetailsFrom (MProject project)
	{
		if (isProcessed() || project == null)
			return 0;
		int count = copyLinesFrom(project)
			+ copyPhasesFrom(project);
		return count;
	}	//	copyDetailsFrom

	/**
	 * 	Copy Project Lines From other Project
	 *	@param project project
	 *	@return number of project lines copied
	 */
	public int copyLinesFrom (MProject project)
	{
		if (isProcessed() || project == null)
			return 0;
		int count = 0;
		MProjectLine[] fromLines = project.getLines();
		for (int i = 0; i < fromLines.length; i++)
		{
			//BF 3067850 - monhate
			if((fromLines[i].getC_ProjectPhase_ID() != 0)||
			   (fromLines[i].getC_ProjectTask_ID() != 0)) continue;
			
			MProjectLine line = new MProjectLine (getCtx(), 0, project.get_TrxName());
			PO.copyValues(fromLines[i], line, getAD_Client_ID(), getAD_Org_ID());
			line.setC_Project_ID(getC_Project_ID());
			line.setInvoicedAmt(Env.ZERO);
			line.setInvoicedQty(Env.ZERO);
			line.setC_OrderPO_ID(0);
			line.setC_Order_ID(0);
			line.setProcessed(false);
			if (line.save())
				count++;
		}
		if (fromLines.length != count)
			log.log(Level.SEVERE, "Lines difference - Project=" + fromLines.length + " <> Saved=" + count);

		return count;
	}	//	copyLinesFrom

	/**
	 * 	Copy Phases/Tasks from other Project
	 *	@param fromProject project to copy from
	 *	@return number of phase/task line copied
	 */
	public int copyPhasesFrom (MProject fromProject)
	{
		if (isProcessed() || fromProject == null)
			return 0;
		int count = 0;
		int taskCount = 0, lineCount = 0;
		//	Get Phases
		MProjectPhase[] myPhases = getPhases();
		MProjectPhase[] fromPhases = fromProject.getPhases();
		//	Copy Phases
		for (int i = 0; i < fromPhases.length; i++)
		{
			//	Check if Phase already exists
			int C_Phase_ID = fromPhases[i].getC_Phase_ID();
			boolean exists = false;
			if (C_Phase_ID == 0)
				exists = false;
			else
			{
				for (int ii = 0; ii < myPhases.length; ii++)
				{
					if (myPhases[ii].getC_Phase_ID() == C_Phase_ID)
					{
						exists = true;
						break;
					}
				}
			}
			//	Phase exist
			if (exists) {
				if (log.isLoggable(Level.INFO)) log.info("Phase already exists here, ignored - " + fromPhases[i]);
			} else {
				MProjectPhase toPhase = new MProjectPhase (getCtx (), 0, get_TrxName());
				PO.copyValues (fromPhases[i], toPhase, getAD_Client_ID (), getAD_Org_ID ());
				toPhase.setC_Project_ID (getC_Project_ID ());
				toPhase.setC_Order_ID (0);
				toPhase.setIsComplete (false);
				toPhase.saveEx();
				count++;
				taskCount += toPhase.copyTasksFrom (fromPhases[i]);
				//BF 3067850 - monhate
				lineCount += toPhase.copyLinesFrom(fromPhases[i]);
			}
		}
		if (fromPhases.length != count)
			log.warning("Count difference - Project=" + fromPhases.length + " <> Saved=" + count);

		return count + taskCount + lineCount;
	}	//	copyPhasesFrom

	/**
	 *	Set Project Type and Category.<br/>
	 * 	If project type is Service Project (PROJECTCATEGORY_ServiceChargeProject), copy project Phase/Tasks from project type.
	 *	@param type project type
	 */
	public void setProjectType (MProjectType type)
	{
		if (type == null)
			return;
		setC_ProjectType_ID(type.getC_ProjectType_ID());
		setProjectCategory(type.getProjectCategory());
		if (PROJECTCATEGORY_ServiceChargeProject.equals(getProjectCategory()))
			copyPhasesFrom(type);
	}	//	setProjectType

	/**
	 *	Copy Phases from Project Type
	 *	@param type Project Type
	 *	@return number of line copy
	 */
	public int copyPhasesFrom (MProjectType type)
	{
		//	create phases
		int count = 0;
		int taskCount = 0;
		MProjectTypePhase[] typePhases = type.getPhases();
		for (int i = 0; i < typePhases.length; i++)
		{
			MProjectPhase toPhase = new MProjectPhase (this, typePhases[i]);
			if (toPhase.save())
			{
				count++;
				taskCount += toPhase.copyTasksFrom(typePhases[i]);
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine("#" + count + "/" + taskCount 
			+ " - " + type);
		if (typePhases.length != count)
			log.log(Level.SEVERE, "Count difference - Type=" + typePhases.length + " <> Saved=" + count);
		return count;
	}	//	copyPhasesFrom

	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (getAD_User_ID() == -1)	//	Summary Project in Dimensions
			setAD_User_ID(0);
		
		//	Set Currency from price list
		if (is_ValueChanged("M_PriceList_Version_ID") && getM_PriceList_Version_ID() != 0)
		{
			MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID(), null);
			if (pl != null && pl.get_ID() != 0)
				setC_Currency_ID(pl.getC_Currency_ID());
		}
		
		return true;
	}	//	beforeSave
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		// Create accounting and tree record
		if (newRecord)
		{
			insert_Accounting("C_Project_Acct", "C_AcctSchema_Default", null);
			insert_Tree(MTree_Base.TREETYPE_Project);
		}
		// Update driven by value tree
		if (newRecord || is_ValueChanged(COLUMNNAME_Value))
			update_Tree(MTree_Base.TREETYPE_Project);

		//	Value/Name change, update Combination and Description of C_ValidCombination
		if (!newRecord 
			&& (is_ValueChanged("Value") || is_ValueChanged("Name")))
			MAccount.updateValueDescription(getCtx(), "C_Project_ID=" + getC_Project_ID(), get_TrxName());

		return success;
	}	//	afterSave

	@Override
	protected boolean afterDelete (boolean success)
	{
		// Delete tree record
		if (success)
			delete_Tree(MTree_Base.TREETYPE_Project);
		return success;
	}	//	afterDelete
	
	/**
	 * 	Get Invoices Generated for this Project
	 *	@return array of invoice
	 *	author monhate
	 */	
	public MInvoice[] getMInvoices(){
		StringBuilder sb = new StringBuilder();
		sb.append(MInvoice.COLUMNNAME_C_Project_ID).append("=?");
		List<MInvoice> list = new Query(getCtx(), MInvoice.Table_Name, sb.toString(), get_TrxName())
			.setParameters(getC_Project_ID())
			.list();
		return list.toArray(new MInvoice[list.size()]);
	}

}	//	MProject
