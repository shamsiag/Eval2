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
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.Core;
import org.adempiere.base.IProductPricing;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 *	Requisition Line Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MRequisitionLine.java,v 1.2 2006/07/30 00:51:03 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>BF [ 2419978 ] Voiding PO, requisition don't set on NULL
 * 			<li>BF [ 2608617 ] Error when I want to delete a PO document
 * 			<li>BF [ 2609604 ] Add M_RequisitionLine.C_BPartner_ID
 * 			<li>FR [ 2841841 ] Requisition Improvements
 * 				https://sourceforge.net/p/adempiere/feature-requests/792/
 */
public class MRequisitionLine extends X_M_RequisitionLine
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = -2567343619431184322L;

	/**
	 * Get corresponding Requisition Line for given Order
	 * @param ctx
	 * @param C_Order_ID order
	 * @param trxName
	 * @return Requisition Line array
	 */
	public static MRequisitionLine[] forC_Order_ID(Properties ctx, int C_Order_ID, String trxName)
	{
		final String whereClause = "EXISTS (SELECT 1 FROM C_OrderLine ol"
										+" WHERE ol.C_OrderLine_ID=M_RequisitionLine.C_OrderLine_ID"
										+" AND ol.C_Order_ID=?)";
		List<MRequisitionLine> list = new Query(ctx, I_M_RequisitionLine.Table_Name, whereClause, trxName)
			.setParameters(C_Order_ID)
			.list();
		return list.toArray(new MRequisitionLine[list.size()]);
	}
	
	/**
	 * UnLink Requisition Lines from Order
	 * @param ctx
	 * @param C_Order_ID
	 * @param trxName
	 */
	public static void unlinkC_Order_ID(Properties ctx, int C_Order_ID, String trxName)
	{
		for (MRequisitionLine line : MRequisitionLine.forC_Order_ID(ctx, C_Order_ID, trxName))
		{
			line.setC_OrderLine_ID(0);
			line.saveEx();
		}
	}
	
	/**
	 * Get corresponding Requisition Line(s) for given Order Line
	 * @param ctx
	 * @param C_OrderLine_ID order line
	 * @param trxName
	 * @return array of Requisition Line(s)
	 */
	public static MRequisitionLine[] forC_OrderLine_ID(Properties ctx, int C_OrderLine_ID, String trxName)
	{
		final String whereClause = COLUMNNAME_C_OrderLine_ID+"=?";
		List<MRequisitionLine> list = new Query(ctx, I_M_RequisitionLine.Table_Name, whereClause, trxName)
			.setParameters(C_OrderLine_ID)
			.list();
		return list.toArray(new MRequisitionLine[list.size()]);
	}

	/**
	 * UnLink Requisition Lines from Order Line
	 * @param ctx
	 * @param C_OrderLine_ID
	 * @param trxName
	 */
	public static void unlinkC_OrderLine_ID(Properties ctx, int C_OrderLine_ID, String trxName)
	{
		for (MRequisitionLine line : forC_OrderLine_ID(ctx, C_OrderLine_ID, trxName))
		{
			line.setC_OrderLine_ID(0);
			line.saveEx();
		}
	}

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_RequisitionLine_UU  UUID key
     * @param trxName Transaction
     */
    public MRequisitionLine(Properties ctx, String M_RequisitionLine_UU, String trxName) {
        super(ctx, M_RequisitionLine_UU, trxName);
		if (Util.isEmpty(M_RequisitionLine_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_RequisitionLine_ID id
	 *	@param trxName transaction
	 */
	public MRequisitionLine (Properties ctx, int M_RequisitionLine_ID, String trxName)
	{
		this (ctx, M_RequisitionLine_ID, trxName, (String[]) null);
	}	//	MRequisitionLine

	/**
	 * @param ctx
	 * @param M_RequisitionLine_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MRequisitionLine(Properties ctx, int M_RequisitionLine_ID, String trxName, String... virtualColumns) {
		super(ctx, M_RequisitionLine_ID, trxName, virtualColumns);
		if (M_RequisitionLine_ID == 0)
			setInitialDefaults();
	}

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setLine (0);	// @SQL=SELECT COALESCE(MAX(Line),0)+10 AS DefaultValue FROM M_RequisitionLine WHERE M_Requisition_ID=@M_Requisition_ID@
		setLineNetAmt (Env.ZERO);
		setPriceActual (Env.ZERO);
		setQty (Env.ONE);	// 1
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MRequisitionLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MRequisitionLine

	/**
	 * 	Parent Constructor
	 *	@param req requisition
	 */
	public MRequisitionLine (MRequisition req)
	{
		this (req.getCtx(), 0, req.get_TrxName());
		setClientOrg(req);
		setM_Requisition_ID(req.getM_Requisition_ID());
		m_M_PriceList_ID = req.getM_PriceList_ID();
		m_parent = req;
	}	//	MRequisitionLine

	/** Parent					*/
	private MRequisition	m_parent = null;
	
	/**	PriceList				*/
	private int 	m_M_PriceList_ID = 0;
	
	/**
	 * Get Ordered Qty
	 * @return Ordered Qty
	 */
	public BigDecimal getQtyOrdered()
	{
		if (getC_OrderLine_ID() > 0)
			return getQty();
		else
			return Env.ZERO;
	}
	
	/**
	 * 	Get Parent
	 *	@return parent
	 */
	public MRequisition getParent()
	{
		if (m_parent == null)
			m_parent = new MRequisition (getCtx(), getM_Requisition_ID(), get_TrxName());
		return m_parent;
	}	//	getParent
	
	@Override
	public I_M_Requisition getM_Requisition()
	{
		return getParent();
	}

	/**
	 * @return Date when this product is required by planner
	 * @see MRequisition#getDateRequired()
	 */
	public Timestamp getDateRequired()
	{
		return getParent().getDateRequired();
	}
	
	/**
	 * 	Set PriceActual to charge amount or standard product price
	 */
	public void setPrice()
	{
		if (getC_Charge_ID() != 0)
		{
			MCharge charge = MCharge.get(getCtx(), getC_Charge_ID());
			setPriceActual(charge.getChargeAmt());
		}
		if (getM_Product_ID() == 0)
			return;
		if (m_M_PriceList_ID == 0)
			m_M_PriceList_ID = getParent().getM_PriceList_ID();
		if (m_M_PriceList_ID == 0)
		{
			throw new AdempiereException("PriceList unknown!");
		}
		setPrice (m_M_PriceList_ID);
	}	//	setPrice
	
	/**
	 * 	Set Price for Product and PriceList
	 * 	@param M_PriceList_ID price list
	 */
	public void setPrice (int M_PriceList_ID)
	{
		if (getM_Product_ID() == 0)
			return;
		//
		if (log.isLoggable(Level.FINE)) log.fine("M_PriceList_ID=" + M_PriceList_ID);
		IProductPricing pp = Core.getProductPricing();
		pp.setRequisitionLine(this, get_TrxName());
		pp.setM_PriceList_ID(M_PriceList_ID);
		//
		setPriceActual (pp.getPriceStd());
	}	//	setPrice

	/**
	 * 	Calculate Line Net Amt
	 */
	public void setLineNetAmt ()
	{
		BigDecimal lineNetAmt = getQty().multiply(getPriceActual());
		super.setLineNetAmt (lineNetAmt);
	}	//	setLineNetAmt
		
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (newRecord && getParent().isProcessed()) {
			log.saveError("ParentComplete", Msg.translate(getCtx(), "M_Requisition_ID"));
			return false;
		}
		// Set Line
		if (getLine() == 0)
		{
			String sql = "SELECT COALESCE(MAX(Line),0)+10 FROM M_RequisitionLine WHERE M_Requisition_ID=?";
			int ii = DB.getSQLValueEx (get_TrxName(), sql, getM_Requisition_ID());
			setLine (ii);
		}
		//	Set C_Charge_ID to 0 this is a product line
		if (getM_Product_ID() != 0 && getC_Charge_ID() != 0)
			setC_Charge_ID(0);
		// Set M_AttributeSetInstance_ID to 0 if this is a charge line
		if (getM_AttributeSetInstance_ID() != 0 && getC_Charge_ID() != 0)
			setM_AttributeSetInstance_ID(0);
		// Default UOM to product UOM
		if (getM_Product_ID() > 0 && getC_UOM_ID() <= 0)
		{
			setC_UOM_ID(getM_Product().getC_UOM_ID());
		}
		// Set price actual
		if (getPriceActual().signum() == 0)
			setPrice();
		setLineNetAmt();

		/* Carlos Ruiz - globalqss
		 * IDEMPIERE-178 Orders and Invoices must disallow amount lines without product/charge
		 */
		if (getParent().getC_DocType().isChargeOrProductMandatory()) {
			if (getC_Charge_ID() == 0 && getM_Product_ID() == 0 && (getPriceActual().signum() != 0 || getQty().signum() != 0)) {
				log.saveError("FillMandatory", Msg.translate(getCtx(), "ChargeOrProductMandatory"));
				return false;
			}
		}
		
		return true;
	}	//	beforeSave
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		return updateHeader();
	}	//	afterSave
	
	@Override
	protected boolean afterDelete (boolean success)
	{
		if (!success)
			return success;
		return updateHeader();
	}	//	afterDelete
	
	@Override
	public I_M_Product getM_Product()
	{
		return MProduct.getCopy(getCtx(), getM_Product_ID(), get_TrxName());
	}

	/**
	 * 	Update Header (M_Requisition) Total
	 *	@return header updated
	 */
	private boolean updateHeader()
	{
		if (log.isLoggable(Level.FINE)) log.fine("");
		String sql = "UPDATE M_Requisition r"
			+ " SET TotalLines="
				+ "(SELECT COALESCE(SUM(LineNetAmt),0) FROM M_RequisitionLine rl "
				+ "WHERE r.M_Requisition_ID=rl.M_Requisition_ID) "
			+ "WHERE M_Requisition_ID=?";
		int no = DB.executeUpdateEx(sql, new Object[]{getM_Requisition_ID()}, get_TrxName());
		if (no != 1)
			log.log(Level.SEVERE, "Header update #" + no);
		m_parent = null;
		return no == 1;
	}	//	updateHeader
	
}	//	MRequisitionLine
