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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * 	Project Phase Model
 *
 *	@author Jorg Janke
 *	@version $Id: MProjectPhase.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MProjectPhase extends X_C_ProjectPhase
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 5824045445920353065L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_ProjectPhase_UU  UUID key
     * @param trxName Transaction
     */
    public MProjectPhase(Properties ctx, String C_ProjectPhase_UU, String trxName) {
        super(ctx, C_ProjectPhase_UU, trxName);
		if (Util.isEmpty(C_ProjectPhase_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_ProjectPhase_ID id
	 *	@param trxName transaction
	 */
	public MProjectPhase (Properties ctx, int C_ProjectPhase_ID, String trxName)
	{
		super (ctx, C_ProjectPhase_ID, trxName);
		if (C_ProjectPhase_ID == 0)
			setInitialDefaults();
	}	//	MProjectPhase

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setCommittedAmt (Env.ZERO);
		setIsCommitCeiling (false);
		setIsComplete (false);
		setSeqNo (0);
		setQty (Env.ZERO);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MProjectPhase (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProjectPhase

	/**
	 * 	Parent Constructor
	 *	@param project parent
	 */
	public MProjectPhase (MProject project)
	{
		this (project.getCtx(), 0, project.get_TrxName());
		setClientOrg(project);
		setC_Project_ID(project.getC_Project_ID());
	}	//	MProjectPhase

	/**
	 * 	Copy Constructor
	 *	@param project parent
	 *	@param phase copy
	 */
	public MProjectPhase (MProject project, MProjectTypePhase phase)
	{
		this (project);
		//
		setC_Phase_ID (phase.getC_Phase_ID());			//	FK
		setName (phase.getName());
		setSeqNo (phase.getSeqNo());
		setDescription(phase.getDescription());
		setHelp(phase.getHelp());
		if (phase.getM_Product_ID() != 0)
			setM_Product_ID(phase.getM_Product_ID());
		setQty(phase.getStandardQty());
	}	//	MProjectPhase

	/**
	 * 	Get Project Phase Tasks.
	 *	@return Array of tasks
	 */
	public MProjectTask[] getTasks()
	{
		ArrayList<MProjectTask> list = new ArrayList<MProjectTask>();
		String sql = "SELECT * FROM C_ProjectTask WHERE C_ProjectPhase_ID=? ORDER BY SeqNo";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getC_ProjectPhase_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add(new MProjectTask (getCtx(), rs, get_TrxName()));
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
		MProjectTask[] retValue = new MProjectTask[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getTasks

	/**
	 * 	Copy project lines from other Phase
	 *	@param fromPhase phase to copy from
	 *	@return number of project line copied
	 */
	public int copyLinesFrom (MProjectPhase fromPhase)
	{
		if (fromPhase == null)
			return 0;
		int count = 0;
		//
		MProjectLine[] fromLines = fromPhase.getLines();
		//	Copy Project Lines
		for (int i = 0; i < fromLines.length; i++)
		{
				if(fromLines[i].getC_ProjectTask_ID() != 0) continue;
				MProjectLine toLine = new MProjectLine(getCtx (), 0, get_TrxName());
				PO.copyValues (fromLines[i], toLine, getAD_Client_ID (), getAD_Org_ID ());
				toLine.setC_Project_ID(getC_Project_ID ());
				toLine.setC_ProjectPhase_ID (getC_ProjectPhase_ID ());
				toLine.saveEx();
				count++;
		}
		if (fromLines.length != count)
			log.warning("Count difference - ProjectLine=" + fromLines.length + " <> Saved=" + count);

		return count;		
	}
		
	/**
	 * 	Copy Tasks from other Phase
	 *	@param fromPhase phase to copy from
	 *	@return number of tasks copied
	 */
	public int copyTasksFrom (MProjectPhase fromPhase)
	{
		if (fromPhase == null)
			return 0;
		int count = 0, countLine = 0;
		//
		MProjectTask[] myTasks = getTasks();
		MProjectTask[] fromTasks = fromPhase.getTasks();
		//	Copy Project Tasks
		for (int i = 0; i < fromTasks.length; i++)
		{
			//	Check if Task already exists
			int C_Task_ID = fromTasks[i].getC_Task_ID();
			boolean exists = false;
			if (C_Task_ID == 0)
				exists = false;
			else
			{
				for (int ii = 0; ii < myTasks.length; ii++)
				{
					if (myTasks[ii].getC_Task_ID() == C_Task_ID)
					{
						exists = true;
						break;
					}
				}
			}
			//	Phase exist
			if (exists) {
				if (log.isLoggable(Level.INFO)) log.info("Task already exists here, ignored - " + fromTasks[i]);
			} else {
				MProjectTask toTask = new MProjectTask (getCtx (), 0, get_TrxName());
				PO.copyValues (fromTasks[i], toTask, getAD_Client_ID (), getAD_Org_ID ());
				toTask.setC_ProjectPhase_ID (getC_ProjectPhase_ID ());
				toTask.saveEx();
				count++;
				//BF 3067850 - monhate
				countLine += toTask.copyLinesFrom(fromTasks[i]);
			}
		}
		if (fromTasks.length != count)
			log.warning("Count difference - ProjectPhase=" + fromTasks.length + " <> Saved=" + count);

		return count + countLine;
	}	//	copyTasksFrom

	/**
	 * 	Copy Tasks from other MProjectTypePhase
	 *	@param fromPhase MProjectTypePhase to copy from
	 *	@return number of tasks copied
	 */
	public int copyTasksFrom (MProjectTypePhase fromPhase)
	{
		if (fromPhase == null)
			return 0;
		int count = 0;
		//	Copy Type Tasks
		MProjectTypeTask[] fromTasks = fromPhase.getTasks();
		for (int i = 0; i < fromTasks.length; i++)
		{
			MProjectTask toTask = new MProjectTask (this, fromTasks[i]);
			if (toTask.save())
				count++;
		}
		if (log.isLoggable(Level.FINE)) log.fine("#" + count + " - " + fromPhase);
		if (fromTasks.length != count)
			log.log(Level.SEVERE, "Count difference - TypePhase=" + fromTasks.length + " <> Saved=" + count);

		return count;
	}	//	copyTasksFrom
	
	/**
	 * 	Get Project Lines
	 *	@return Array of lines
	 */	public MProjectLine[] getLines()
	{
		final String whereClause = "C_Project_ID=? and C_ProjectPhase_ID=?";
		List <MProjectLine> list = new Query(getCtx(), I_C_ProjectLine.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID(), getC_ProjectPhase_ID())
			.setOrderBy("Line,C_ProjectLine_ID")
			.list();
		//
		MProjectLine[] retValue = new MProjectLine[list.size()];
		list.toArray(retValue);
		return retValue;
	}

	/**
	 * 	String Representation
	 *	@return info
	 */
	 @Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MProjectPhase[");
		sb.append (get_ID())
			.append ("-").append (getSeqNo())
			.append ("-").append (getName())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
}	//	MProjectPhase
