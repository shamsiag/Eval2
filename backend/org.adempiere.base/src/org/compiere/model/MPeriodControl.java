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
import java.util.Properties;

import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Period Control Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MPeriodControl.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MPeriodControl extends X_C_PeriodControl implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -7818843756246170549L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_PeriodControl_UU  UUID key
     * @param trxName Transaction
     */
    public MPeriodControl(Properties ctx, String C_PeriodControl_UU, String trxName) {
        super(ctx, C_PeriodControl_UU, trxName);
		if (Util.isEmpty(C_PeriodControl_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_PeriodControl_ID 0
	 *	@param trxName transaction
	 */
	public MPeriodControl (Properties ctx, int C_PeriodControl_ID, String trxName)
	{
		super(ctx, C_PeriodControl_ID, trxName);
		if (C_PeriodControl_ID == 0)
			setInitialDefaults();
	}	//	MPeriodControl

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setPeriodAction (PERIODACTION_NoAction);
		setPeriodStatus (PERIODSTATUS_NeverOpened);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MPeriodControl (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MPeriodControl

	/**
	 * 	Parent Constructor
	 *	@param period parent
	 *	@param DocBaseType doc base type
	 */
	public MPeriodControl (MPeriod period, String DocBaseType)
	{
		this (period.getCtx(), period.getAD_Client_ID(), period.getC_Period_ID(),  
			DocBaseType, period.get_TrxName());
		setClientOrg(period);
	}	//	MPeriodControl

	/**
	 * 	New Constructor
	 *	@param ctx context
	 *	@param AD_Client_ID client
	 *	@param C_Period_ID period
	 *	@param DocBaseType doc base type
	 *	@param trxName transaction
	 */
	public MPeriodControl (Properties ctx, int AD_Client_ID, int C_Period_ID, 
		String DocBaseType, String trxName)
	{
		this (ctx, 0, trxName);
		setClientOrg(AD_Client_ID, 0);
		setC_Period_ID (C_Period_ID);
		setDocBaseType (DocBaseType);
	}	//	MPeriodControl

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MPeriodControl(MPeriodControl copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MPeriodControl(Properties ctx, MPeriodControl copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MPeriodControl(Properties ctx, MPeriodControl copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 * 	Is Open
	 *	@return true if open
	 */
	public boolean isOpen()
	{
		return isOpen(false);
	}	//	isOpen

	/**
	 * 	Is Open
	 *  @param forPosting - true to check if the period is open for posting, false is for DocAction
	 *	@return true if open
	 */
	public boolean isOpen(boolean forPosting)
	{
		if (forPosting)
			return PERIODSTATUS_Open.equals(getPeriodStatus())
				|| PERIODSTATUS_DocumentClosed.equals(getPeriodStatus());
		else
			return PERIODSTATUS_Open.equals(getPeriodStatus());
	}	//	isOpen

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MPeriodControl[");
		sb.append(get_ID()).append(",").append(getDocBaseType())
			.append(",Status=").append(getPeriodStatus())
			.append ("]");
		return sb.toString ();
	}	//	toString

	@Override
	public MPeriodControl markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MPeriodControl

