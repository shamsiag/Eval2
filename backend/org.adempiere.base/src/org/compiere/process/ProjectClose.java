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
package org.compiere.process;

import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.model.MProject;
import org.compiere.model.MProjectLine;
 
/**
 *  Process to Close a Project.
 *
 *	@author Jorg Janke
 *	@version $Id: ProjectClose.java,v 1.2 2006/07/30 00:51:01 jjanke Exp $
 *
 *  @author Teo Sarca, wwww.arhipac.ro
 * 			<li>FR [ 2791635 ] Use saveEx whenever is possible
 * 				https://sourceforge.net/p/adempiere/feature-requests/722/
 */
@org.adempiere.base.annotation.Process
public class ProjectClose extends SvrProcess
{
	/**	Project from Record			*/
	private int 		m_C_Project_ID = 0;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			if (para[i].getParameter() == null)
				;
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
		m_C_Project_ID = getRecord_ID();
	}	//	prepare

	/**
	 *  Close a project by setting processed to true.
	 *  @return empty string
	 *  @throws Exception if not successful
	 */
	@Override
	protected String doIt() throws Exception
	{
		MProject project = new MProject (getCtx(), m_C_Project_ID, get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("doIt - " + project);

		MProjectLine[] projectLines = project.getLines();
		if (MProject.PROJECTCATEGORY_WorkOrderJob.equals(project.getProjectCategory())
			|| MProject.PROJECTCATEGORY_AssetProject.equals(project.getProjectCategory()))
		{
			/** @todo Check if we should close it */
		}

		//	Close lines
		for (int line = 0; line < projectLines.length; line++)
		{
			projectLines[line].setProcessed(true);
			projectLines[line].saveEx();
		}

		project.setProcessed(true);
		project.saveEx();

		return "";
	}	//	doIt

}	//	ProjectClose
