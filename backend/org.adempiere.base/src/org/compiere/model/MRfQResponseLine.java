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
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;
import org.compiere.util.TimeUtil;

/**
 *	RfQ Response Line Model	
 *	
 *  @author Jorg Janke
 *  @version $Id: MRfQResponseLine.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 *  
 *  @author Teo Sarca, teo.sarca@gmail.com
 *  		<li>BF [ 2892581 ] Cannot load RfQResponseLine
 *  			https://sourceforge.net/p/adempiere/bugs/2201/
 */
public class MRfQResponseLine extends X_C_RfQResponseLine
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 3388579962604552288L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_RfQResponseLine_UU  UUID key
     * @param trxName Transaction
     */
    public MRfQResponseLine(Properties ctx, String C_RfQResponseLine_UU, String trxName) {
        super(ctx, C_RfQResponseLine_UU, trxName);
    }

	/**
	 *	@param ctx context
	 *	@param C_RfQResponseLine_ID
	 *	@param trxName transaction
	 */
	public MRfQResponseLine (Properties ctx, int C_RfQResponseLine_ID, String trxName)
	{
		super(ctx, C_RfQResponseLine_ID, trxName);
	}	//	MRfQResponseLine

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MRfQResponseLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MRfQResponseLine
	
	/**
	 * 	Parent Constructor.<br/>
	 * 	Create and save MRfQResponseLineQty if MRfQLineQty IsRfQQty=Y.
	 *	@param response response
	 *	@param line line
	 */
	public MRfQResponseLine (MRfQResponse response, MRfQLine line)
	{
		super (response.getCtx(), 0, response.get_TrxName());
		setClientOrg(response);
		setC_RfQResponse_ID (response.getC_RfQResponse_ID());
		//
		setC_RfQLine_ID (line.getC_RfQLine_ID());
		//
		setIsSelectedWinner (false);
		setIsSelfService (false);
		//
		MRfQLineQty[] qtys = line.getQtys();
		for (int i = 0; i < qtys.length; i++)
		{
			if (qtys[i].isActive() && qtys[i].isRfQQty())
			{
				if (get_ID() == 0)	//	save this line
					saveEx();
				MRfQResponseLineQty qty = new MRfQResponseLineQty (this, qtys[i]);
				qty.saveEx();
			}
		}
	}	//	MRfQResponseLine
	
	/**	RfQ Line				*/
	private MRfQLine				m_rfqLine = null;
	/**	Quantities				*/
	private MRfQResponseLineQty[] 	m_qtys = null;
	
	/**
	 * 	Get Quantities
	 *	@return array of MRfQResponseLineQty
	 */
	public MRfQResponseLineQty[] getQtys ()
	{
		return getQtys (false);
	}	//	getQtys

	/**
	 * 	Get Quantities
	 * 	@param requery true to re-query from DB
	 *	@return array of MRfQResponseLineQty
	 */
	public MRfQResponseLineQty[] getQtys (boolean requery)
	{
		if (m_qtys != null && !requery)
			return m_qtys;
		
		ArrayList<MRfQResponseLineQty> list = new ArrayList<MRfQResponseLineQty>();
		String sql = "SELECT * FROM C_RfQResponseLineQty "
			+ "WHERE C_RfQResponseLine_ID=? AND IsActive='Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getC_RfQResponseLine_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MRfQResponseLineQty(getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		m_qtys = new MRfQResponseLineQty[list.size ()];
		list.toArray (m_qtys);
		return m_qtys;
	}	//	getQtys
	
	/**
	 * 	Get RfQ Line
	 *	@return rfq line
	 */
	public MRfQLine getRfQLine()
	{
		if (m_rfqLine == null)
			m_rfqLine = MRfQLine.get(getCtx(), getC_RfQLine_ID(), get_TrxName());
		return m_rfqLine;
	}	//	getRfQLine
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MRfQResponseLine[");
		sb.append(get_ID()).append(",Winner=").append(isSelectedWinner())
			.append ("]");
		return sb.toString ();
	}	//	toString
		
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		//	Calculate Complete Date (also used to verify)
		if (getDateWorkStart() != null && getDeliveryDays() != 0)
			setDateWorkComplete (TimeUtil.addDays(getDateWorkStart(), getDeliveryDays()));
		//	Calculate Delivery Days
		else if (getDateWorkStart() != null && getDeliveryDays() == 0 && getDateWorkComplete() != null)
			setDeliveryDays (TimeUtil.getDaysBetween(getDateWorkStart(), getDateWorkComplete()));
		//	Calculate Start Date
		else if (getDateWorkStart() == null && getDeliveryDays() != 0 && getDateWorkComplete() != null)
			setDateWorkStart (TimeUtil.addDays(getDateWorkComplete(), getDeliveryDays() * -1));

		if (!isActive())
			setIsSelectedWinner(false);
		return true;
	}	//	beforeSave	

	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{	
		if (!success)
			return success;
		if (!isActive())
		{
			// Load m_qtys (if not loaded yet)
			getQtys (false);
			// Inactive C_RfQResponseLineQty records
			for (int i = 0; i < m_qtys.length; i++)
			{
				MRfQResponseLineQty qty = m_qtys[i];
				if (qty.isActive())
				{
					qty.setIsActive(false);
					qty.saveEx();
				}
			}
		}
		return success;
	}	//	success
	
}	//	MRfQResponseLine
