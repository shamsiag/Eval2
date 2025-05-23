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
import java.util.List;
import java.util.Properties;

import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 *	Commission Run Amounts
 *	
 *  @author Jorg Janke
 *  @version $Id: MCommissionAmt.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MCommissionAmt extends X_C_CommissionAmt
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 1747802539808391638L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_CommissionAmt_UU  UUID key
     * @param trxName Transaction
     */
    public MCommissionAmt(Properties ctx, String C_CommissionAmt_UU, String trxName) {
        super(ctx, C_CommissionAmt_UU, trxName);
		if (Util.isEmpty(C_CommissionAmt_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_CommissionAmt_ID id
	 *	@param trxName transaction
	 */
	public MCommissionAmt(Properties ctx, int C_CommissionAmt_ID, String trxName)
	{
		super(ctx, C_CommissionAmt_ID, trxName);
		if (C_CommissionAmt_ID == 0)
			setInitialDefaults();
	}	//	MCommissionAmt

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setActualQty (Env.ZERO);
		setCommissionAmt (Env.ZERO);
		setConvertedAmt (Env.ZERO);
	}

	/**
	 * 	Parent Constructor
	 *	@param run parent
	 *	@param C_CommissionLine_ID line
	 */
	public MCommissionAmt (MCommissionRun run, int C_CommissionLine_ID)
	{
		this (run.getCtx(), 0, run.get_TrxName());
		setClientOrg (run);
		setC_CommissionRun_ID (run.getC_CommissionRun_ID());
		setC_CommissionLine_ID (C_CommissionLine_ID);
	}	//	MCommissionAmt

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MCommissionAmt(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MCommissionAmt

	/**
	 * @param ctx
	 * @param C_CommissionAmt_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MCommissionAmt(Properties ctx, int C_CommissionAmt_ID, String trxName, String... virtualColumns) {
		super(ctx, C_CommissionAmt_ID, trxName, virtualColumns);
	}

	/**
	 * 	Get Details
	 *	@return array of details
	 */
	public MCommissionDetail[] getDetails()
	{
		final String whereClause = I_C_CommissionDetail.COLUMNNAME_C_CommissionAmt_ID+"=?";
	List<MCommissionDetail> list = new Query(getCtx(),I_C_CommissionDetail.Table_Name, whereClause, get_TrxName())
		.setParameters(getC_CommissionAmt_ID())
		.list();
		//	Convert
		MCommissionDetail[] retValue = new MCommissionDetail[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getDetails

	/**
	 * 	Calculate Commission
	 */
	public void calculateCommission()
	{
		MCommissionDetail[] details = getDetails();
		BigDecimal ConvertedAmt = Env.ZERO;
		BigDecimal ActualQty = Env.ZERO;
		for (int i = 0; i < details.length; i++)
		{
			MCommissionDetail detail = details[i];
			BigDecimal amt = detail.getConvertedAmt();
			if (amt == null)
				amt = Env.ZERO;
			ConvertedAmt = ConvertedAmt.add(amt);
			ActualQty = ActualQty.add(detail.getActualQty());
		}
		setConvertedAmt(ConvertedAmt);
		setActualQty(ActualQty);
		//
		MCommissionLine cl = new MCommissionLine(getCtx(), getC_CommissionLine_ID(), get_TrxName());
		//	Qty
		BigDecimal qty = getActualQty().subtract(cl.getQtySubtract());
		if (cl.isPositiveOnly() && qty.signum() < 0)
			qty = Env.ZERO;
		qty = qty.multiply(cl.getQtyMultiplier());
		//	Amt
		BigDecimal amt = getConvertedAmt().subtract(cl.getAmtSubtract());
		if (cl.isPositiveOnly() && amt.signum() < 0)
			amt = Env.ZERO;
		amt = amt.multiply(cl.getAmtMultiplier());
		//
		setCommissionAmt(amt.add(qty));
	}	//	calculateCommission
		
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		if (!newRecord)
			updateRunHeader();
		return success;
	}	//	afterSave
	
	@Override
	protected boolean afterDelete (boolean success)
	{
		if (success)
			updateRunHeader();
		return success;
	}	//	afterDelete
	
	/**
	 * 	Update Header (MCommissionRun) amount
	 */
	private void updateRunHeader()
	{
		MCommissionRun run = new MCommissionRun(getCtx(), getC_CommissionRun_ID(),get_TrxName());
		run.updateFromAmt();
		run.saveEx();
	}	//	updateRunHeader

}	//	MCommissionAmt
