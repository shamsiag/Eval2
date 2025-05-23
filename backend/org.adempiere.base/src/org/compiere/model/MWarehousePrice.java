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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 *	Product Warehouse Availability and Price Model.<br/>
 *	The Ownership (Client, Org) is determined by the Warehouse.<br/>
 *	Active is determined if the product is discontinued (the product/price/warehouse need to be active).<br/>
 *	Created.. is determined by the price list version.<br/>
 *	
 *  @author Jorg Janke
 *  @version $Id: MWarehousePrice.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class MWarehousePrice extends X_RV_WarehousePrice
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -4902324773162437140L;

	/**
	 * 	Find Products in Warehouse with Price
	 * 	@param ctx context
	 *	@param M_PriceList_Version_ID mandatory price list
	 *	@param M_Warehouse_ID mandatory warehouse
	 *	@param Value optional value
	 *	@param Name optional name
	 *	@param UPC optional fully match upc
	 *	@param SKU optional full match sku
	 *	@param trxName transaction
	 *	@return array of product prices and warehouse availability
	 */
	public static MWarehousePrice[] find (Properties ctx,
		int M_PriceList_Version_ID, int M_Warehouse_ID,
		String Value, String Name, String UPC, String SKU, String trxName)
	{
		StringBuilder sql = new StringBuilder ("SELECT * FROM RV_WarehousePrice "
			+ "WHERE M_PriceList_Version_ID=? AND M_Warehouse_ID=?");
		StringBuilder sb = new StringBuilder();
		Value = getFindParameter (Value);
		if (Value != null)
			sb.append("UPPER(Value) LIKE ?");
		Name = getFindParameter (Name);
		if (Name != null)
		{
			if (sb.length() > 0)
				sb.append(" OR ");
			sb.append("UPPER(Name) LIKE ?");
		}
		if (UPC != null && UPC.length() > 0)
		{
			if (sb.length() > 0)
				sb.append(" OR ");
			sb.append("UPC=?");
		}
		if (SKU != null && SKU.length() > 0)
		{
			if (sb.length() > 0)
				sb.append(" OR ");
			sb.append("SKU=?");
		}
		if (sb.length() > 0)
			sql.append(" AND (").append(sb).append(")");
		sql.append(" ORDER BY Value");
		//
		String finalSQL = MRole.getDefault().addAccessSQL(sql.toString(), 
			"RV_WarehousePrice", MRole.SQL_NOTQUALIFIED, MRole.SQL_RO);
		if (s_log.isLoggable(Level.FINE)) s_log.fine("find - M_PriceList_Version_ID=" + M_PriceList_Version_ID 
			+ ", M_Warehouse_ID=" + M_Warehouse_ID
			+ " - " + finalSQL);
		ArrayList<MWarehousePrice> list = new ArrayList<MWarehousePrice>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(finalSQL, trxName);
			int index = 1;
			pstmt.setInt(index++, M_PriceList_Version_ID);
			pstmt.setInt(index++, M_Warehouse_ID);
			if (Value != null)
				pstmt.setString(index++, Value);
			if (Name != null)
				pstmt.setString(index++, Name);
			if (UPC != null && UPC.length() > 0)
				pstmt.setString(index++, UPC);
			if (SKU != null && SKU.length() > 0)
				pstmt.setString(index++, SKU);
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add(new MWarehousePrice(ctx, rs, trxName));
		}
		catch (Exception e)
		{
			s_log.log(Level.SEVERE, finalSQL, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		} 
		//
		if (s_log.isLoggable(Level.FINE)) s_log.fine("find - #" + list.size());
		MWarehousePrice[] retValue = new MWarehousePrice[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	find

	/**
	 * 	Find Products in Warehouse with Price for customer
	 * 	@param bPartner business partner
	 *	@param IsSOTrx if true SO
	 *	@param valid the date the price must be valid
	 *	@param M_Warehouse_ID mandatory warehouse
	 *	@param Value optional value
	 *	@param Name optional name
	 *	@param UPC optional upc
	 *	@param SKU optional sku
	 *	@param trxName transaction
	 *	@return array of product prices and warehouse availability or null
	 */
	public static MWarehousePrice[] find (MBPartner bPartner,
		boolean IsSOTrx, Timestamp valid, int M_Warehouse_ID,
		String Value, String Name, String UPC, String SKU, String trxName)
	{
		int M_PriceList_ID = IsSOTrx ? bPartner.getM_PriceList_ID() : bPartner.getPO_PriceList_ID();
		MPriceList pl = null;
		if (M_PriceList_ID == 0)
			pl = MPriceList.getDefault(bPartner.getCtx(), IsSOTrx);
		else
			pl = MPriceList.get(bPartner.getCtx(), M_PriceList_ID, trxName);
		if (pl == null)
		{
			s_log.severe ("No PriceList found");
			return null;
		}
		MPriceListVersion plv = pl.getPriceListVersion (valid);
		if (plv == null)
		{
			s_log.severe ("No PriceListVersion found for M_PriceList_ID=" + pl.getM_PriceList_ID());
			return null;
		}
		//
		return find (bPartner.getCtx(), plv.getM_PriceList_Version_ID(), M_Warehouse_ID,
			Value, Name, UPC, SKU, trxName);
	}	//	find

	/**
	 * 	Get MWarehouse Price
	 *	@param product product
	 *	@param M_PriceList_Version_ID
	 *	@param M_Warehouse_ID
	 *	@param trxName transaction
	 *	@return warehouse price
	 */
	public static MWarehousePrice get (MProduct product,
		int M_PriceList_Version_ID, int M_Warehouse_ID, String trxName)
	{
		MWarehousePrice retValue = null;
		String sql = "SELECT * FROM RV_WarehousePrice "
			+ "WHERE M_Product_ID=? AND M_PriceList_Version_ID=? AND M_Warehouse_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, product.getM_Product_ID());
			pstmt.setInt(2, M_PriceList_Version_ID);
			pstmt.setInt(3, M_Warehouse_ID);
			rs = pstmt.executeQuery ();
			if (rs.next ())
				retValue = new MWarehousePrice(product.getCtx(), rs, trxName);
		}
		catch (Exception e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return retValue;
	}	//	get

	/** Static Logger					*/
	private static CLogger 	s_log = CLogger.getCLogger(MWarehousePrice.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param RV_WarehousePrice_UU  UUID key
     * @param trxName Transaction
     */
    public MWarehousePrice(Properties ctx, String RV_WarehousePrice_UU, String trxName) {
        super(ctx, RV_WarehousePrice_UU, trxName);
    }

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MWarehousePrice (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MWarehousePrice
	
	/**
	 * 	Is Product Available
	 *	@return true if available qty &gt; 0
	 */
	public boolean isAvailable()
	{
		return getQtyAvailable().signum() == 1;
	}	//	isAvailable

}	//	MWarehousePrice
