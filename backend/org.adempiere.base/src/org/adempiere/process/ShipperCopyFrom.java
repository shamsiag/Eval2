/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 **********************************************************************/
package org.adempiere.process;

import java.math.BigDecimal;
import java.util.logging.Level;

import org.compiere.model.MProcessPara;
import org.compiere.model.X_M_ShipperCfg;
import org.compiere.model.X_M_ShipperLabelsCfg;
import org.compiere.model.X_M_ShipperPackagingCfg;
import org.compiere.model.X_M_ShipperPickupTypesCfg;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 * Process to copy shipping configuration records from another M_ShipperCfg record.
 */
@org.adempiere.base.annotation.Process
public class ShipperCopyFrom extends SvrProcess
{
	private int		p_M_ShipperCfg_ID = 0;

	@Override
	protected void prepare() 
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals(X_M_ShipperCfg.COLUMNNAME_M_ShipperCfg_ID))
				p_M_ShipperCfg_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}

	@Override
	protected String doIt() throws Exception 
	{
		int To_M_ShipperCfg_ID = getRecord_ID();
		if (log.isLoggable(Level.INFO)) log.info("From M_ShipperCfg_ID=" + p_M_ShipperCfg_ID + " to " + To_M_ShipperCfg_ID);
		if (To_M_ShipperCfg_ID == 0)
			throw new IllegalArgumentException("Target M_ShipperCfg_ID == 0");
		if (p_M_ShipperCfg_ID == 0)
			throw new IllegalArgumentException("Source M_ShipperCfg_ID == 0");
		
		createShipperPackaging(To_M_ShipperCfg_ID);
		createShipperLabels(To_M_ShipperCfg_ID);
		createShipperPickupTypes(To_M_ShipperCfg_ID);
		
		return "OK";
	}
	
	/**
	 * Copy M_ShipperPackagingCfg records
	 * @param To_M_ShipperCfg_ID
	 */
	private void createShipperPackaging(int To_M_ShipperCfg_ID)
	{
		StringBuilder whereClause = new StringBuilder();
		whereClause.append("M_ShipperCfg_ID=" + p_M_ShipperCfg_ID + " ");
		whereClause.append("AND IsActive='Y' ");
		whereClause.append("AND M_ShipperPackagingCfg_ID NOT IN ( ");
		whereClause.append("SELECT M_ShipperPackagingCfg_ID ");
		whereClause.append("FROM M_ShipperPackagingCfg ");
		whereClause.append("WHERE M_ShipperCfg_ID=" + To_M_ShipperCfg_ID + ")");
		
		int[] xspIds = X_M_ShipperPackagingCfg.getAllIDs(X_M_ShipperPackagingCfg.Table_Name, whereClause.toString(), get_TrxName());		
		for (int xspId : xspIds)
		{
			X_M_ShipperPackagingCfg xsp = new X_M_ShipperPackagingCfg(getCtx(), xspId, get_TrxName());
			X_M_ShipperPackagingCfg sp = new X_M_ShipperPackagingCfg(getCtx(), 0, null);
			sp.setDescription(xsp.getDescription());
			sp.setIsActive(xsp.isActive());
			sp.setIsDefault(xsp.isDefault());
			sp.setName(xsp.getName());
			sp.setValue(xsp.getValue());
			sp.setWeight(xsp.getWeight());
			sp.setM_ShipperCfg_ID(To_M_ShipperCfg_ID);
			sp.saveEx();				
		}
	}
	
	/**
	 * Copy M_ShipperLabelsCfg records
	 * @param To_M_ShipperCfg_ID
	 */
	private void createShipperLabels(int To_M_ShipperCfg_ID)
	{
		StringBuilder whereClause = new StringBuilder();
		whereClause.append("M_ShipperCfg_ID=" + p_M_ShipperCfg_ID + " ");
		whereClause.append("AND IsActive='Y' ");
		whereClause.append("AND M_ShipperLabelsCfg_ID NOT IN ( ");
		whereClause.append("SELECT M_ShipperLabelsCfg_ID ");
		whereClause.append("FROM M_ShipperLabelsCfg ");
		whereClause.append("WHERE M_ShipperCfg_ID=" + To_M_ShipperCfg_ID + ")");
		
		int[] xslIds = X_M_ShipperLabelsCfg.getAllIDs(X_M_ShipperLabelsCfg.Table_Name, whereClause.toString(), get_TrxName());		
		for (int xslId : xslIds)
		{
			X_M_ShipperLabelsCfg xsl = new X_M_ShipperLabelsCfg(getCtx(), xslId, get_TrxName());
			X_M_ShipperLabelsCfg sl = new X_M_ShipperLabelsCfg(getCtx(), 0, null);
			sl.setDescription(xsl.getDescription());
			sl.setHeight(xsl.getHeight());
			sl.setIsActive(xsl.isActive());
			sl.setIsDefault(xsl.isDefault());
			sl.setLabelPrintMethod(xsl.getLabelPrintMethod());
			sl.setName(xsl.getName());
			sl.setValue(xsl.getValue());
			sl.setWidth(xsl.getWidth());
			sl.setM_ShipperCfg_ID(To_M_ShipperCfg_ID);
			sl.saveEx();				
		}
	}
	
	/**
	 * Copy M_ShipperPickupTypesCfg records
	 * @param To_M_ShipperCfg_ID
	 */
	private void createShipperPickupTypes(int To_M_ShipperCfg_ID)
	{
		StringBuilder whereClause = new StringBuilder();
		whereClause.append("M_ShipperCfg_ID=" + p_M_ShipperCfg_ID + " ");
		whereClause.append("AND IsActive='Y' ");
		whereClause.append("AND M_ShipperPickupTypesCfg_ID NOT IN ( ");
		whereClause.append("SELECT M_ShipperPickupTypesCfg_ID ");
		whereClause.append("FROM M_ShipperPickupTypesCfg ");
		whereClause.append("WHERE M_ShipperCfg_ID=" + To_M_ShipperCfg_ID + ")");
		
		int[] xsptIds = X_M_ShipperPickupTypesCfg.getAllIDs(X_M_ShipperPickupTypesCfg.Table_Name, whereClause.toString(), get_TrxName());		
		for (int xsptId : xsptIds)
		{
			X_M_ShipperPickupTypesCfg xspt = new X_M_ShipperPickupTypesCfg(getCtx(), xsptId, get_TrxName());
			X_M_ShipperPickupTypesCfg spt = new X_M_ShipperPickupTypesCfg(getCtx(), 0, null);
			spt.setDescription(xspt.getDescription());
			spt.setIsActive(xspt.isActive());
			spt.setIsDefault(xspt.isDefault());
			spt.setName(xspt.getName());
			spt.setValue(xspt.getValue());
			spt.setM_ShipperCfg_ID(To_M_ShipperCfg_ID);			
			spt.saveEx();				
		}
	}

}
