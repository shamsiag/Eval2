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
 *	Commission Run
 *	
 *  @author Jorg Janke
 *  @version $Id: MCommissionRun.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class MCommissionRun extends X_C_CommissionRun
{
    /**
     * generated serial id 
     */
    private static final long serialVersionUID = -3103035295526318283L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_CommissionRun_UU  UUID key
     * @param trxName Transaction
     */
    public MCommissionRun(Properties ctx, String C_CommissionRun_UU, String trxName) {
        super(ctx, C_CommissionRun_UU, trxName);
		if (Util.isEmpty(C_CommissionRun_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_CommissionRun_ID id
	 *	@param trxName transaction
	 */
	public MCommissionRun (Properties ctx, int C_CommissionRun_ID, String trxName)
	{
		super(ctx, C_CommissionRun_ID, trxName);
		if (C_CommissionRun_ID == 0)
			setInitialDefaults();
	}	//	MCommissionRun

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setGrandTotal (Env.ZERO);
		setProcessed (false);
	}

	/**
	 * 	Parent Constructor
	 *	@param commission parent
	 */
	public MCommissionRun (MCommission commission)
	{
		this (commission.getCtx(), 0, commission.get_TrxName());
		setClientOrg (commission);
		setC_Commission_ID (commission.getC_Commission_ID());
	}	//	MCommissionRun

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MCommissionRun(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MCommissionRun

	/**
	 * 	Get Amounts
	 *	@return array of amounts (MCommissionAmt)
	 */
	public MCommissionAmt[] getAmts()
	{
		final String whereClause = I_C_CommissionAmt.COLUMNNAME_C_CommissionRun_ID+"=?";
 		List<MCommissionAmt> list = new Query(getCtx(),I_C_CommissionAmt.Table_Name,whereClause,get_TrxName())
		.setParameters(getC_CommissionRun_ID())
		.list();
		//	Convert
		MCommissionAmt[] retValue = new MCommissionAmt[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getAmts

	/**
	 * @param ctx
	 * @param C_CommissionRun_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MCommissionRun(Properties ctx, int C_CommissionRun_ID, String trxName, String... virtualColumns) {
		super(ctx, C_CommissionRun_ID, trxName, virtualColumns);
	}

	/**
	 * 	Update From Amt (MCommissionAmt)
	 */
	public void updateFromAmt()
	{
		MCommissionAmt[] amts = getAmts();
		BigDecimal GrandTotal = Env.ZERO;
		for (int i = 0; i < amts.length; i++)
		{
			MCommissionAmt amt = amts[i];
			GrandTotal = GrandTotal.add(amt.getCommissionAmt());
		}
		setGrandTotal(GrandTotal);
	}	//	updateFromAmt
	
}	//	MCommissionRun
