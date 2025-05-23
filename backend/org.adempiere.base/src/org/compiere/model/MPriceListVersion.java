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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Price List Version Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MPriceListVersion.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MPriceListVersion extends X_M_PriceList_Version implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 1625884461739604147L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_PriceList_Version_UU  UUID key
     * @param trxName Transaction
     */
    public MPriceListVersion(Properties ctx, String M_PriceList_Version_UU, String trxName) {
        super(ctx, M_PriceList_Version_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_PriceList_Version_ID id
	 *	@param trxName transaction
	 */
	public MPriceListVersion(Properties ctx, int M_PriceList_Version_ID, String trxName)
	{
		super(ctx, M_PriceList_Version_ID, trxName);
	}	//	MPriceListVersion

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MPriceListVersion(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MPriceListVersion

	/**
	 * 	Parent Constructor
	 *	@param pl parent
	 */
	public MPriceListVersion (MPriceList pl)
	{
		this (pl.getCtx(), 0, pl.get_TrxName());
		setClientOrg(pl);
		setM_PriceList_ID(pl.getM_PriceList_ID());
	}	//	MPriceListVersion
	
	/**
	 * Copy constructor
	 * @param copy
	 */
	public MPriceListVersion(MPriceListVersion copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MPriceListVersion(Properties ctx, MPriceListVersion copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MPriceListVersion(Properties ctx, MPriceListVersion copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
		this.m_pl = null;
		this.m_pp = copy.m_pp != null ? Arrays.stream(copy.m_pp).map(e -> {return new MProductPrice(ctx, e, trxName);}).toArray(MProductPrice[]::new) : null;
	}
	
	/** Product Prices			*/
	private MProductPrice[] m_pp = null;
	/** Price List				*/
	protected MPriceList		m_pl = null;

	/**
	 * 	Get Parent PriceList
	 *	@return price List
	 */
	public MPriceList getPriceList()
	{
		if (m_pl == null && getM_PriceList_ID() != 0)
		{
			if (is_Immutable())
				m_pl = MPriceList.get (getCtx(), getM_PriceList_ID(), null);
			else
				m_pl = MPriceList.getCopy(getCtx(), getM_PriceList_ID(), get_TrxName());
		}
		return m_pl;
	}	//	PriceList
	
	
	/**
	 * 	Get Product Price
	 * 	@param refresh true if refresh
	 *	@return product price
	 */
	public MProductPrice[] getProductPrice (boolean refresh)
	{
		if (m_pp != null && !refresh)
			return m_pp;
		m_pp = getProductPrice(null);
		if (m_pp != null && m_pp.length > 0 && is_Immutable())
			Arrays.stream(m_pp).forEach(e -> e.markImmutable());
		return m_pp;
	}	//	getProductPrice
	
	/**
	 * 	Get Product Price
	 * 	@param whereClause optional where clause
	 * 	@return product price
	 */
	public MProductPrice[] getProductPrice (String whereClause)
	{
		String localWhereClause = I_M_ProductPrice.COLUMNNAME_M_PriceList_Version_ID+"=?";
		if (whereClause != null)
			localWhereClause += " " + whereClause;
		List<MProductPrice> list = new Query(getCtx(),I_M_ProductPrice.Table_Name,localWhereClause,get_TrxName())
			.setParameters(getM_PriceList_Version_ID())
			.list();
		MProductPrice[] pp = new MProductPrice[list.size()];
		list.toArray(pp);
		return pp;
	}	//	getProductPrice
	
	/**
	 * 	Set Name to Valid From Date.<br/>
	 * 	If valid from is null, set valid from to today date.
	 */
	public void setName()
	{
		if (getValidFrom() == null)
			setValidFrom (TimeUtil.getDay(null));
		if (getName() == null)
		{
			String name = DisplayType.getDateFormat(DisplayType.Date)
				.format(getValidFrom());
			setName(name);
		}
	}	//	setName
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		setName();
		
		return true;
	}	//	beforeSave
	
	@Override
	public MPriceListVersion markImmutable() 
	{
		if (is_Immutable())
			return this;
		
		makeImmutable();
		if (m_pl != null)
			m_pl.markImmutable();
		if (m_pp != null && m_pp.length > 0)
			Arrays.stream(m_pp).forEach(e -> e.markImmutable());
		return this;
	}

}	//	MPriceListVersion
