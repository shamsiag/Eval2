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
import java.util.List;
import java.util.Properties;

import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * 	Project Phase Task Model
 *
 *	@author Jorg Janke
 *	@version $Id: MProjectTask.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 */
public class MProjectTask extends X_C_ProjectTask
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 6714520156233475723L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_ProjectTask_UU  UUID key
     * @param trxName Transaction
     */
    public MProjectTask(Properties ctx, String C_ProjectTask_UU, String trxName) {
        super(ctx, C_ProjectTask_UU, trxName);
		if (Util.isEmpty(C_ProjectTask_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_ProjectTask_ID id
	 *	@param trxName transaction
	 */
	public MProjectTask (Properties ctx, int C_ProjectTask_ID, String trxName)
	{
		this (ctx, C_ProjectTask_ID, trxName, (String[]) null);
	}	//	MProjectTask

	/**
	 * @param ctx
	 * @param C_ProjectTask_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MProjectTask(Properties ctx, int C_ProjectTask_ID, String trxName, String... virtualColumns) {
		super(ctx, C_ProjectTask_ID, trxName, virtualColumns);
		if (C_ProjectTask_ID == 0)
			setInitialDefaults();
	}

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setSeqNo (0);
		setQty (Env.ZERO);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MProjectTask (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProjectTask

	/**
	 * 	Parent Constructor
	 * 	@param phase parent
	 */
	public MProjectTask (MProjectPhase phase)
	{
		this (phase.getCtx(), 0, phase.get_TrxName());
		setClientOrg(phase);
		setC_ProjectPhase_ID(phase.getC_ProjectPhase_ID());
	}	//	MProjectTask

	/**
	 * 	Copy Constructor
	 *	@param phase parent
	 *	@param task type copy
	 */
	public MProjectTask (MProjectPhase phase, MProjectTypeTask task)
	{
		this (phase);
		//
		setC_Task_ID (task.getC_Task_ID());			//	FK
		setSeqNo (task.getSeqNo());
		setName (task.getName());
		setDescription(task.getDescription());
		setHelp(task.getHelp());
		if (task.getM_Product_ID() != 0)
			setM_Product_ID(task.getM_Product_ID());
		setQty(task.getStandardQty());
	}	//	MProjectTask
	
	/**
	 * 	Get Project Lines
	 *	@return Array of project lines
	 */	public MProjectLine[] getLines()
	{
		final String whereClause = "C_ProjectPhase_ID=? and C_ProjectTask_ID=? ";
		List <MProjectLine> list = new Query(getCtx(), I_C_ProjectLine.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_ProjectPhase_ID(), getC_ProjectTask_ID())
			.setOrderBy("Line,C_ProjectLine_ID")
			.list();		
		//
		MProjectLine[] retValue = new MProjectLine[list.size()];
		list.toArray(retValue);
		return retValue;
	}
	 
	/**
	 * 	Copy project lines from other Task
	 *	@param fromTask Project Task to copy from
	 *	@return number of project lines copied
	 */
	public int copyLinesFrom (MProjectTask fromTask)
	{
		if (fromTask == null)
			return 0;
		int count = 0;
		//
		MProjectLine[] fromLines = fromTask.getLines();
		//	Copy Project Lines
		for (int i = 0; i < fromLines.length; i++)
		{
				MProjectLine toLine = new MProjectLine(getCtx (), 0, get_TrxName());
				PO.copyValues (fromLines[i], toLine, getAD_Client_ID (), getAD_Org_ID ());
				toLine.setC_Project_ID(getC_Project_ID(false));
				toLine.setC_ProjectPhase_ID (getC_ProjectPhase_ID ());
				toLine.setC_ProjectTask_ID(getC_ProjectTask_ID ());
				toLine.saveEx();
				count++;
		}
		if (fromLines.length != count)
			log.warning("Count difference - ProjectLine=" + fromLines.length + " <> Saved=" + count);

		return count;		
	}

	private int C_Project_ID = 0;

	/**
	 * @param reQuery true to re-query from DB
	 * @return C_Project_ID
	 */
	private int getC_Project_ID(boolean reQuery) {
		if (C_Project_ID==0 || reQuery)
			C_Project_ID = getC_ProjectPhase().getC_Project_ID();
		return C_Project_ID;
	}

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MProjectTask[");
		sb.append (get_ID())
			.append ("-").append (getSeqNo())
			.append ("-").append (getName())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
}	//	MProjectTask
