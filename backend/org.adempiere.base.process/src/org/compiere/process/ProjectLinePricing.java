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

import org.adempiere.base.Core;
import org.adempiere.base.IProductPricing;
import org.compiere.model.MProcessPara;
import org.compiere.model.MProject;
import org.compiere.model.MProjectLine;
import org.compiere.util.Msg;

/**
 *  Price Project Line.
 *
 *	@author Jorg Janke
 *	@version $Id: ProjectLinePricing.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
@org.adempiere.base.annotation.Process
public class ProjectLinePricing extends SvrProcess
{
	/**	Project Line from Record			*/
	private int 		m_C_ProjectLine_ID = 0;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
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
		m_C_ProjectLine_ID = getRecord_ID();
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (m_C_ProjectLine_ID == 0)
			throw new IllegalArgumentException("No Project Line");
		MProjectLine projectLine = new MProjectLine (getCtx(), m_C_ProjectLine_ID, get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info("doIt - " + projectLine);
		if (projectLine.getM_Product_ID() == 0)
			throw new IllegalArgumentException("No Product");
		//
		MProject project = new MProject (getCtx(), projectLine.getC_Project_ID(), get_TrxName());
		if (project.getM_PriceList_ID() == 0)
			throw new IllegalArgumentException("No PriceList");
		//
		boolean isSOTrx = true;
		IProductPricing pp = Core.getProductPricing();
		pp.setInitialValues(projectLine.getM_Product_ID(), project.getC_BPartner_ID(), 
				projectLine.getPlannedQty(), isSOTrx, get_TrxName());
		pp.setM_PriceList_ID(project.getM_PriceList_ID());
		pp.setPriceDate(project.getDateContract());
		//
		projectLine.setPlannedPrice(pp.getPriceStd());
		projectLine.setPlannedMarginAmt(pp.getPriceStd().subtract(pp.getPriceLimit()));
		projectLine.saveEx();
		//
		StringBuilder retValue = new StringBuilder().append(Msg.getElement(getCtx(), "PriceList")).append(pp.getPriceList()).append(" - ")
			.append(Msg.getElement(getCtx(), "PriceStd")).append(pp.getPriceStd()).append(" - ")
			.append(Msg.getElement(getCtx(), "PriceLimit")).append(pp.getPriceLimit());
		return retValue.toString();
	}	//	doIt

}	//	ProjectLinePricing
