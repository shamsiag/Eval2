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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.Core;
import org.adempiere.base.IProductPricing;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * 	Project Line Model
 *
 *	@author Jorg Janke
 *	@version $Id: MProjectLine.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class MProjectLine extends X_C_ProjectLine
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 2668549463273628848L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_ProjectLine_UU  UUID key
     * @param trxName Transaction
     */
    public MProjectLine(Properties ctx, String C_ProjectLine_UU, String trxName) {
        super(ctx, C_ProjectLine_UU, trxName);
		if (Util.isEmpty(C_ProjectLine_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_ProjectLine_ID id
	 *	@param trxName transaction
	 */
	public MProjectLine (Properties ctx, int C_ProjectLine_ID, String trxName)
	{
		this (ctx, C_ProjectLine_ID, trxName, (String[]) null);
	}	//	MProjectLine

	/**
	 * @param ctx
	 * @param C_ProjectLine_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MProjectLine(Properties ctx, int C_ProjectLine_ID, String trxName, String... virtualColumns) {
		super(ctx, C_ProjectLine_ID, trxName, virtualColumns);
		if (C_ProjectLine_ID == 0)
			setInitialDefaults();
	}

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setLine (0);
		setIsPrinted(true);
		setProcessed(false);
		setInvoicedAmt (Env.ZERO);
		setInvoicedQty (Env.ZERO);
		//
		setPlannedAmt (Env.ZERO);
		setPlannedMarginAmt (Env.ZERO);
		setPlannedPrice (Env.ZERO);
		setPlannedQty (Env.ONE);
	}

	/**
	 * 	Load Constructor
	 * 	@param ctx context
	 * 	@param rs result set
	 *	@param trxName transaction
	 */
	public MProjectLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProjectLine

	/**
	 * 	Parent Constructor
	 *	@param project parent
	 */
	public MProjectLine (MProject project)
	{
		this (project.getCtx(), 0, project.get_TrxName());
		setClientOrg(project);
		setC_Project_ID (project.getC_Project_ID());	// Parent
		setLine();
	}	//	MProjectLine

	/** Parent				*/
	private MProject	m_parent = null;
	
	/**
	 *	Get and set next Line No
	 */
	private void setLine()
	{
		setLine(DB.getSQLValue(get_TrxName(), 
			"SELECT COALESCE(MAX(Line),0)+10 FROM C_ProjectLine WHERE C_Project_ID=?", getC_Project_ID()));
	}	//	setLine

	/**
	 * 	Set Product, committed qty, etc.
	 *	@param pi project issue
	 */
	public void setMProjectIssue (MProjectIssue pi)
	{
		setC_ProjectIssue_ID(pi.getC_ProjectIssue_ID());
		setM_Product_ID(pi.getM_Product_ID());
		setCommittedQty(pi.getMovementQty());
		if (getDescription() != null)
			setDescription(pi.getDescription());
	}	//	setMProjectIssue

	/**
	 *	Set purchase order
	 *	@param C_OrderPO_ID purchase order id
	 */
	public void setC_OrderPO_ID (int C_OrderPO_ID)
	{
		super.setC_OrderPO_ID(C_OrderPO_ID);
	}	//	setC_OrderPO_ID

	/**
	 * 	Get Project
	 *	@return parent
	 */
	public MProject getProject()
	{
		if (m_parent == null && getC_Project_ID() != 0)
		{
			m_parent = new MProject (getCtx(), getC_Project_ID(), get_TrxName());
			if (get_TrxName() != null)
				m_parent.load(get_TrxName());
		}
		return m_parent;
	}	//	getProject
	
	/**
	 * 	Get Limit Price if exists
	 *	@return limit price (limit price of product or planned price of this record)
	 */
	public BigDecimal getLimitPrice()
	{
		BigDecimal limitPrice = getPlannedPrice();
		if (getM_Product_ID() == 0)
			return limitPrice;
		if (getProject() == null)
			return limitPrice;
		IProductPricing pp = Core.getProductPricing();
		pp.setProjectLine(this, get_TrxName());
		pp.setM_PriceList_ID(m_parent.getM_PriceList_ID());
		if (pp.calculatePrice())
			limitPrice = pp.getPriceLimit();
		return limitPrice;
	}	//	getLimitPrice
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MProjectLine[");
			sb.append (get_ID()).append ("-")
				.append (getLine())
				.append(",C_Project_ID=").append(getC_Project_ID())
				.append(",C_ProjectPhase_ID=").append(getC_ProjectPhase_ID())
				.append(",C_ProjectTask_ID=").append(getC_ProjectTask_ID())
				.append(",C_ProjectIssue_ID=").append(getC_ProjectIssue_ID())
				.append(", M_Product_ID=").append(getM_Product_ID())
				.append(", PlannedQty=").append(getPlannedQty())
				.append ("]");
		return sb.toString ();
	}	//	toString
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (getLine() == 0)
			setLine();
		
		// Calculate Planned Amount
		setPlannedAmt(getPlannedQty().multiply(getPlannedPrice()));
		
		// Calculate Planned Margin
		if (is_ValueChanged("M_Product_ID") || is_ValueChanged("M_Product_Category_ID")
			|| is_ValueChanged("PlannedQty") || is_ValueChanged("PlannedPrice"))
		{
			if (getM_Product_ID() != 0)
			{
				BigDecimal marginEach = getPlannedPrice().subtract(getLimitPrice());
				setPlannedMarginAmt(marginEach.multiply(getPlannedQty()));
			}
			else if (getM_Product_Category_ID() != 0)
			{
				MProductCategory category = MProductCategory.get(getCtx(), getM_Product_Category_ID());
				BigDecimal marginEach = category.getPlannedMargin();
				setPlannedMarginAmt(marginEach.multiply(getPlannedQty()));
			}
		}
		
		//	Set C_ProjectPhase_ID from C_ProjectTask 
		if (is_ValueChanged("C_ProjectTask_ID") && getC_ProjectTask_ID() != 0)
		{
			MProjectTask pt = new MProjectTask(getCtx(), getC_ProjectTask_ID(), get_TrxName());
			if (pt == null || pt.get_ID() == 0)
			{
				log.warning("Project Task Not Found - ID=" + getC_ProjectTask_ID());
				return false;
			}
			else
				setC_ProjectPhase_ID(pt.getC_ProjectPhase_ID());
		}
		// Set C_Project_ID from C_ProjectPhase 
		if (is_ValueChanged("C_ProjectPhase_ID") && getC_ProjectPhase_ID() != 0)
		{
			MProjectPhase pp = new MProjectPhase(getCtx(), getC_ProjectPhase_ID(), get_TrxName());
			if (pp == null || pp.get_ID() == 0)
			{
				log.warning("Project Phase Not Found - " + getC_ProjectPhase_ID());
				return false;
			}
			else
				setC_Project_ID(pp.getC_Project_ID());
		}
		
		return true;
	}	//	beforeSave	
		
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		updateHeader();
		return success;
	}	//	afterSave
		
	@Override
	protected boolean afterDelete (boolean success)
	{
		if (!success)
			return success;
		updateHeader();
		return success;
	}	//	afterDelete
	
	/**
	 * 	Update Header (C_Project, C_ProjectPhase and C_ProjectTask)
	 */
	private void updateHeader()
	{
		String sql = "UPDATE C_Project p "
			+ "SET (PlannedAmt,PlannedQty,PlannedMarginAmt,"
				+ "	CommittedAmt,CommittedQty,"
				+ " InvoicedAmt, InvoicedQty) = "
				+ "(SELECT COALESCE(SUM(pl.PlannedAmt),0),COALESCE(SUM(pl.PlannedQty),0),COALESCE(SUM(pl.PlannedMarginAmt),0),"
				+ " COALESCE(SUM(pl.CommittedAmt),0),COALESCE(SUM(pl.CommittedQty),0),"
				+ " COALESCE(SUM(pl.InvoicedAmt),0), COALESCE(SUM(pl.InvoicedQty),0) "
				+ "FROM C_ProjectLine pl "
				+ "WHERE pl.C_Project_ID=p.C_Project_ID AND pl.IsActive='Y') "
			+ "WHERE C_Project_ID=" + getC_Project_ID();
		int no = DB.executeUpdate(sql, get_TrxName());
		if (no != 1)
			log.log(Level.SEVERE, "updateHeader project - #" + no);
		/*onhate + globalqss BF 3060367*/
		if (getC_ProjectPhase_ID() != 0) {
			sql ="UPDATE C_ProjectPhase x SET " +
				"	(PlannedAmt, CommittedAmt) = " +
				"(SELECT " +
				"	COALESCE(SUM(l.PlannedAmt),0), " +
				"	COALESCE(SUM(l.CommittedAmt),0) " +
				"FROM C_ProjectLine l " +
				"WHERE l.C_Project_ID=x.C_Project_ID " +
				"  AND l.C_ProjectPhase_ID=x.C_ProjectPhase_ID " +
				"  AND l.IsActive='Y') " +
				"WHERE x.C_Project_ID=" + getC_Project_ID() +
				"  AND x.C_ProjectPhase_ID=" + getC_ProjectPhase_ID();
			no = DB.executeUpdate(sql, get_TrxName());
			if (no != 1)
				log.log(Level.SEVERE, "updateHeader project phase - #" + no);
		}
		if (getC_ProjectTask_ID() != 0) {
			sql = "UPDATE C_ProjectTask x SET " +
					"	(PlannedAmt, CommittedAmt) = " +
					"(SELECT " +
					"	COALESCE(SUM(l.PlannedAmt),0), " +
					"	COALESCE(SUM(l.CommittedAmt),0) " +
					"FROM C_ProjectLine l " +
					"WHERE l.C_ProjectPhase_ID=x.C_ProjectPhase_ID " +
					"  AND l.C_ProjectTask_ID=x.C_ProjectTask_ID " +
					"  AND l.IsActive='Y') " +
					"WHERE x.C_ProjectPhase_ID=" + getC_ProjectPhase_ID() + 
					"  AND x.C_ProjectTask_ID=" + getC_ProjectTask_ID();
			no = DB.executeUpdate(sql, get_TrxName());
			if (no != 1)
				log.log(Level.SEVERE, "updateHeader project task - #" + no);
		}
	} // updateHeader

} // MProjectLine
