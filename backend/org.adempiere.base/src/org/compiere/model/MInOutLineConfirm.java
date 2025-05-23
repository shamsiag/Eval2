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

import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 *	Shipment Confirmation Line Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MInOutLineConfirm.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 */
public class MInOutLineConfirm extends X_M_InOutLineConfirm
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -2753405320678781177L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_InOutLineConfirm_UU  UUID key
     * @param trxName Transaction
     */
    public MInOutLineConfirm(Properties ctx, String M_InOutLineConfirm_UU, String trxName) {
        super(ctx, M_InOutLineConfirm_UU, trxName);
		if (Util.isEmpty(M_InOutLineConfirm_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_InOutLineConfirm_ID id
	 *	@param trxName transaction
	 */
	public MInOutLineConfirm (Properties ctx, int M_InOutLineConfirm_ID, String trxName)
	{
		super (ctx, M_InOutLineConfirm_ID, trxName);
		if (M_InOutLineConfirm_ID == 0)
			setInitialDefaults();
	}	//	MInOutLineConfirm

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setDifferenceQty(Env.ZERO);
		setScrappedQty(Env.ZERO);
		setProcessed (false);
	}

	/**
	 * 	Load Construvtor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MInOutLineConfirm (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MInOutLineConfirm
	
	/**
	 * 	Parent Construvtor
	 *	@param header parent
	 */
	public MInOutLineConfirm (MInOutConfirm header)
	{
		this (header.getCtx(), 0, header.get_TrxName());
		setClientOrg(header);
		setM_InOutConfirm_ID(header.getM_InOutConfirm_ID());
	}	//	MInOutLineConfirm
	
	/** Ship Line				*/
	private MInOutLine 	m_line = null;
	
	/**
	 * 	Set Shipment Line
	 *	@param line shipment line
	 */
	public void setInOutLine (MInOutLine line)
	{
		setM_InOutLine_ID(line.getM_InOutLine_ID());
		setTargetQty(line.getMovementQty());	//	Confirmations in Storage UOM	
		setConfirmedQty (getTargetQty());		//	suggestion
		m_line = line;
	}	//	setInOutLine

	/**
	 * 	Get Shipment Line
	 *	@return line
	 */
	public MInOutLine getLine()
	{
		if (m_line == null)
			m_line = new MInOutLine (getCtx(), getM_InOutLine_ID(), get_TrxName());
		return m_line;
	}	//	getLine
	
	
	/**
	 * 	Process Confirmation Line.<br/>
	 * 	- Update InOut Line
	 * 	@param isSOTrx true for sales trx, false otherwise
	 * 	@param confirmType type (MInOutConfirm.CONFIRMTYPE_*)
	 *	@return success
	 */
	public boolean processLine (boolean isSOTrx, String confirmType)
	{
		MInOutLine line = getLine();
		
		//	Customer
		if (MInOutConfirm.CONFIRMTYPE_CustomerConfirmation.equals(confirmType))
		{
			line.setConfirmedQty(getConfirmedQty());
		}
		
		//	Drop Ship
		else if (MInOutConfirm.CONFIRMTYPE_DropShipConfirm.equals(confirmType))
		{
			
		}
		
		//	Pick or QA
		else if (MInOutConfirm.CONFIRMTYPE_PickQAConfirm.equals(confirmType))
		{
			line.setTargetQty(getTargetQty());
			line.setMovementQty(getConfirmedQty());	//	Entered NOT changed
			line.setPickedQty(getConfirmedQty());
			//
			line.setScrappedQty(getScrappedQty());
		}
		
		//	Ship or Receipt
		else if (MInOutConfirm.CONFIRMTYPE_ShipReceiptConfirm.equals(confirmType))
		{
			line.setTargetQty(getTargetQty());
			BigDecimal qty = getConfirmedQty();
			if (!isSOTrx)	//	In PO, we have the responsibility for scrapped
				qty = qty.add(getScrappedQty());
			line.setMovementQty(qty);				//	Entered NOT changed
			//
			line.setScrappedQty(getScrappedQty());
		}
		//	Vendor
		else if (MInOutConfirm.CONFIRMTYPE_VendorConfirmation.equals(confirmType))
		{
			line.setConfirmedQty(getConfirmedQty());
		}
		
		return line.save(get_TrxName());
	}	//	processConfirmation
	
	/**
	 * 	Is Fully Confirmed
	 *	@return true if Target = Confirmed qty
	 */
	public boolean isFullyConfirmed()
	{
		return getTargetQty().compareTo(getConfirmedQty()) == 0;
	}	//	isFullyConfirmed
	
	@Override
	protected boolean beforeDelete ()
	{
		// Always disallow delete
		log.saveError("Error", Msg.getMsg(getCtx(), "CannotDelete"));
		return false;
	}	//	beforeDelete
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		MInOutConfirm parent = new MInOutConfirm(getCtx(), getM_InOutConfirm_ID(), get_TrxName());
		if (newRecord && parent.isProcessed()) {
			log.saveError("ParentComplete", Msg.translate(getCtx(), "M_InOutConfirm_ID"));
			return false;
		}
		//	Calculate Difference = Target - Confirmed - Scrapped
		BigDecimal difference = getTargetQty();
		difference = difference.subtract(getConfirmedQty());
		difference = difference.subtract(getScrappedQty());
		setDifferenceQty(difference);
		//
		return true;
	}	//	beforeSave
	
}	//	MInOutLineConfirm
