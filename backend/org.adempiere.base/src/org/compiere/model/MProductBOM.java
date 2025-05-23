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
import java.util.List;
import java.util.Properties;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Product BOM Model (old).
 *	M_Product_ID = the parent 
 *	M_Product_BOM_ID = the BOM line
 *	M_ProductBOM_ID = the BOM line product
 *	
 *  Replace by MPPProductBOM and MPPProductBOMLine since version 9
 *  @deprecated
 *  @author Jorg Janke
 *  @version $Id: MProductBOM.java,v 1.5 2006/07/30 00:51:02 jjanke Exp $
 */
@Deprecated
public class MProductBOM extends X_M_Product_BOM
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2615029184168566124L;

	/**
	 * 	Get BOM Lines for Product
	 *	@param product product
	 *	@return array of BOMs
	 */
	public static MProductBOM[] getBOMLines (MProduct product)
	{
		return getBOMLines(product.getCtx(), product.getM_Product_ID(), product.get_TrxName());
	}	//	getBOMLines
	
	/**
	 * 	Get BOM Lines for Product
	 * 	@param ctx context
	 *	@param M_Product_ID product
	 *	@param trxName transaction
	 *	@return array of BOMs
	 */
	public static MProductBOM[] getBOMLines (Properties ctx, int M_Product_ID, String trxName)
	{
 		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		final String whereClause = "M_Product_ID=?";
		List <MProductBOM> list = new Query(ctx, I_M_Product_BOM.Table_Name, whereClause, trxName)
		.setParameters(M_Product_ID)
		.setOrderBy("Line")
		.list();
 
		MProductBOM[] retValue = new MProductBOM[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getBOMLines

	/** Static Logger					*/
	@SuppressWarnings("unused")
	private static CLogger s_log = CLogger.getCLogger(MProductBOM.class);
	
	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_Product_BOM_ID id
	 *	@param trxName transaction
	 */
	public MProductBOM (Properties ctx, int M_Product_BOM_ID, String trxName)
	{
		super (ctx, M_Product_BOM_ID, trxName);
		if (M_Product_BOM_ID == 0)
		{
			setBOMQty (Env.ZERO);	// 1
		}
	}	//	MProductBOM

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MProductBOM (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProductBOM

	/**	Included Product		*/
	private MProduct m_product = null;


	/**
	 * 	Get BOM Product
	 *	@return product
	 */
	public MProduct getProduct()
	{
		if (m_product == null && getM_ProductBOM_ID() != 0)
			m_product = MProduct.getCopy(getCtx(), getM_ProductBOM_ID(), get_TrxName());
		return m_product;
	}	//	getProduct

	/**
	 * 	Set included Product
	 *	@param M_ProductBOM_ID product ID
	 */
	public void setM_ProductBOM_ID(int M_ProductBOM_ID)
	{
		super.setM_ProductBOM_ID (M_ProductBOM_ID);
		m_product = null;
	}	//	setM_ProductBOM_ID

	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder("MProductBOM[");
		sb.append(get_ID()).append(",Line=").append(getLine())
			.append(",Type=").append(getBOMType()).append(",Qty=").append(getBOMQty());
		if (m_product == null)
			sb.append(",M_Product_ID=").append(getM_ProductBOM_ID());
		else
			sb.append(",").append(m_product);
		sb.append("]");
		return sb.toString();
	}	//	toString

	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		MProduct product = new MProduct (getCtx(), getM_Product_ID(), get_TrxName());
		if (get_TrxName() != null)
			product.load(get_TrxName());
		if (product.isVerified())
		{
			if (   newRecord 
				|| is_ValueChanged("M_ProductBOM_ID") //	Product Line was changed
				|| (is_ValueChanged("IsActive") && isActive())) // line was activated
			{
				//	Invalidate BOM
				product.setIsVerified(false);
				product.saveEx(get_TrxName());
			}
			if (product.isVerified() && is_ValueChanged("IsActive") && !isActive()) {  // line was inactivated
				if (! hasActiveComponents(getM_Product_ID())) {
					product.setIsVerified(false);
					product.saveEx(get_TrxName());
				}
			}
		}
		return success;
	}	//	afterSave

	@Override
	protected boolean afterDelete(boolean success) {
		if (!success)
			return success;
		MProduct product = new MProduct (getCtx(), getM_Product_ID(), get_TrxName());
		if (get_TrxName() != null)
			product.load(get_TrxName());
		if (product.isVerified())
		{
			if (! hasActiveComponents(getM_Product_ID())) {
				product.setIsVerified(false);
				product.saveEx(get_TrxName());
			}
		}
		return success;
	}
	
	private boolean hasActiveComponents(int productID) {
		int cnt = DB.getSQLValue(get_TrxName(),
				"SELECT COUNT(*) FROM M_Product_BOM WHERE IsActive='Y' AND M_Product_ID=? AND M_Product_BOM_ID!=?",
				productID, getM_Product_BOM_ID());
		return cnt > 0;
	}

}	//	MProductBOM
