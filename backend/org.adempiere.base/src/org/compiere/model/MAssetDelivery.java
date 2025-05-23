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
import java.sql.Timestamp;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.compiere.util.EMail;
import org.compiere.util.Util;

/**
 *  Asset Delivery Model
 *
 *  @author Jorg Janke
 *  @version $Id: MAssetDelivery.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MAssetDelivery extends X_A_Asset_Delivery
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -1731010685101745675L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param A_Asset_Delivery_UU  UUID key
     * @param trxName Transaction
     */
    public MAssetDelivery(Properties ctx, String A_Asset_Delivery_UU, String trxName) {
        super(ctx, A_Asset_Delivery_UU, trxName);
		if (Util.isEmpty(A_Asset_Delivery_UU))
			setInitialDefaults();
    }

	/**
	 * 	Constructor
	 * 	@param ctx context
	 * 	@param A_Asset_Delivery_ID id or 0
	 * 	@param trxName trx
	 */
	public MAssetDelivery (Properties ctx, int A_Asset_Delivery_ID, String trxName)
	{
		super (ctx, A_Asset_Delivery_ID, trxName);
		if (A_Asset_Delivery_ID == 0)
			setInitialDefaults();
	}	//	MAssetDelivery

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setMovementDate (new Timestamp (System.currentTimeMillis ()));
	}

	/**
	 *  Load Constructor
	 *  @param ctx context
	 *  @param rs result set record
	 *	@param trxName transaction
	 */
	public MAssetDelivery (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MAssetDelivery

	/**
	 * 	Create Asset Delivery for HTTP Request
	 * 	@param asset asset
	 * 	@param request request
	 * 	@param AD_User_ID BP Contact
	 */
	public MAssetDelivery (MAsset asset, 
		HttpServletRequest request, int AD_User_ID)
	{
		super (asset.getCtx(), 0, asset.get_TrxName());
		setAD_Client_ID(asset.getAD_Client_ID());
		setAD_Org_ID(asset.getAD_Org_ID());
		//	Asset Info
		setA_Asset_ID (asset.getA_Asset_ID());
		setLot(asset.getLot());
		setSerNo(asset.getSerNo());
		setVersionNo(asset.getVersionNo());
		//
		setMovementDate (new Timestamp (System.currentTimeMillis ()));
		//	Request
		setURL(request.getRequestURL().toString());
		setReferrer(request.getHeader("Referer"));
		setRemote_Addr(request.getRemoteAddr());
		setRemote_Host(request.getRemoteHost());
		//	Who
		setAD_User_ID(AD_User_ID);
		//
		saveEx();
	}	//	MAssetDelivery

	/**
	 * 	Create Asset Delivery for EMail
	 * 	@param asset asset
	 * 	@param email email
	 * 	@param AD_User_ID BP Contact
	 */
	public MAssetDelivery (MAsset asset, EMail email, int AD_User_ID)
	{
		super (asset.getCtx(), 0, asset.get_TrxName());
		//	Asset Info
		setA_Asset_ID (asset.getA_Asset_ID());
		setLot(asset.getLot());
		setSerNo(asset.getSerNo());
		setVersionNo(asset.getVersionNo());
		//
		setMovementDate (new Timestamp (System.currentTimeMillis ()));
		//	EMail
		setEMail(email.getTo().toString());
		setMessageID(email.getMessageID());
		//	Who
		setAD_User_ID(AD_User_ID);
		//
		saveEx();
	}	//	MAssetDelivery

	/**
	 * 	String representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MAssetDelivery[")
			.append (get_ID ())
			.append(",A_Asset_ID=").append(getA_Asset_ID())
			.append(",MovementDate=").append(getMovementDate())
			.append ("]");
		return sb.toString ();
	}	//	toString

}	//	MAssetDelivery

