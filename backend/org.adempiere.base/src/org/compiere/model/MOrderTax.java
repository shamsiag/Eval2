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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 *	Order Tax Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MOrderTax.java,v 1.4 2006/07/30 00:51:04 jjanke Exp $
 */
public class MOrderTax extends X_C_OrderTax
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -6776007249310373908L;


	/**
	 * 	Get Tax Line for Order Line
	 *	@param line Order line
	 *	@param precision currency precision
	 *	@param oldTax true to get old tax
	 *	@param trxName transaction name
	 *	@return existing or new tax
	 */
	public static MOrderTax get (MOrderLine line, int precision, 
		boolean oldTax, String trxName)
	{
		MOrderTax retValue = null;
		if (line == null || line.getC_Order_ID() == 0)
		{
			s_log.fine("No Order");
			return null;
		}
		int C_Tax_ID = line.getC_Tax_ID();
		boolean isOldTax = oldTax && line.is_ValueChanged(MOrderTax.COLUMNNAME_C_Tax_ID); 
		if (isOldTax)
		{
			Object old = line.get_ValueOld(MOrderTax.COLUMNNAME_C_Tax_ID);
			if (old == null)
			{
				s_log.fine("No Old Tax");
				return null;
			}
			C_Tax_ID = ((Integer)old).intValue();
		}
		if (C_Tax_ID == 0)
		{
			if (!line.isDescription())
				s_log.fine("No Tax");
			return null;
		}
		
		String sql = "SELECT * FROM C_OrderTax WHERE C_Order_ID=? AND C_Tax_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, line.getC_Order_ID());
			pstmt.setInt (2, C_Tax_ID);
			rs = pstmt.executeQuery ();
			if (rs.next ())
				retValue = new MOrderTax (line.getCtx(), rs, trxName);
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
		if (retValue != null)
		{
			retValue.setPrecision(precision);
			retValue.set_TrxName(trxName);
			if (s_log.isLoggable(Level.FINE)) s_log.fine("(old=" + oldTax + ") " + retValue);
			return retValue;
		}
		// If the old tax was required and there is no MOrderTax for that
		// return null, and not create another MOrderTax - teo_sarca [ 1583825 ]
		else {
			if (isOldTax)
				return null;
		}
		
		//	Create New
		retValue = new MOrderTax(line.getCtx(), 0, trxName);
		retValue.set_TrxName(trxName);
		retValue.setClientOrg(line);
		retValue.setC_Order_ID(line.getC_Order_ID());
		retValue.setC_Tax_ID(line.getC_Tax_ID());
		retValue.setPrecision(precision);
		retValue.setIsTaxIncluded(line.isTaxIncluded());
		if (s_log.isLoggable(Level.FINE)) s_log.fine("(new) " + retValue);
		return retValue;
	}	//	get
	
	/**
	 * 	Get Child Tax Line for Order Line
	 *	@param line Order line
	 *	@param precision currency precision
	 *	@param oldTax true to get old tax
	 *	@param trxName transaction name
	 *	@return existing or new child tax lines
	 */
	public static MOrderTax[] getChildTaxes(MOrderLine line, int precision, 
		boolean oldTax, String trxName)
	{
		List<MOrderTax> orderTaxes = new ArrayList<MOrderTax>();
		
		if (line == null || line.getC_Order_ID() == 0)
		{
			return orderTaxes.toArray(new MOrderTax[0]);
		}
		
		int C_Tax_ID = line.getC_Tax_ID();
		if (oldTax)
		{
			Object old = line.get_ValueOld(MOrderTax.COLUMNNAME_C_Tax_ID);
			if (old == null)
			{
				return orderTaxes.toArray(new MOrderTax[0]);
			}
			C_Tax_ID = ((Integer)old).intValue();
		}
		if (C_Tax_ID == 0)
		{
			return orderTaxes.toArray(new MOrderTax[0]);
		}
		
		MTax tax = MTax.get(C_Tax_ID);
		if (!tax.isSummary())
			return orderTaxes.toArray(new MOrderTax[0]);
		
		MTax[] cTaxes = tax.getChildTaxes(false);
		for(MTax cTax : cTaxes) {
			MOrderTax orderTax = null;
			String sql = "SELECT * FROM C_OrderTax WHERE C_Order_ID=? AND C_Tax_ID=?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql, trxName);
				pstmt.setInt (1, line.getC_Order_ID());
				pstmt.setInt (2, cTax.getC_Tax_ID());
				rs = pstmt.executeQuery ();
				if (rs.next ())
					orderTax = new MOrderTax (line.getCtx(), rs, trxName);
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
			if (orderTax != null)
			{
				orderTax.setPrecision(precision);
				orderTax.set_TrxName(trxName);
				orderTaxes.add(orderTax);
			}
			// If the old tax was required and there is no MOrderTax for that
			// return null, and not create another MOrderTax - teo_sarca [ 1583825 ]
			else 
			{
				if (oldTax)
					continue;
			}
			
			if (orderTax == null)
			{
				//	Create New
				orderTax = new MOrderTax(line.getCtx(), 0, trxName);
				orderTax.set_TrxName(trxName);
				orderTax.setClientOrg(line);
				orderTax.setC_Order_ID(line.getC_Order_ID());
				orderTax.setC_Tax_ID(cTax.getC_Tax_ID());
				orderTax.setPrecision(precision);
				orderTax.setIsTaxIncluded(line.isTaxIncluded());
				orderTaxes.add(orderTax);
			}
		}
		
		return orderTaxes.toArray(new MOrderTax[0]);
	}
	
	/**	Static Logger	*/
	private static CLogger	s_log	= CLogger.getCLogger (MOrderTax.class);
		
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_OrderTax_UU  UUID key
     * @param trxName Transaction
     */
    public MOrderTax(Properties ctx, String C_OrderTax_UU, String trxName) {
        super(ctx, C_OrderTax_UU, trxName);
		if (Util.isEmpty(C_OrderTax_UU))
			setInitialDefaults();
    }

	/**
	 *	@param ctx context
	 *	@param ignored ignored
	 *	@param trxName transaction
	 */
	public MOrderTax (Properties ctx, int ignored, String trxName)
	{
		super(ctx, 0, trxName);
		if (ignored != 0)
			throw new IllegalArgumentException("Multi-Key");
		setInitialDefaults();
	}	//	MOrderTax

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setTaxAmt (Env.ZERO);
		setTaxBaseAmt (Env.ZERO);
		setIsTaxIncluded(false);
	}

	/**
	 * 	Load Constructor.
	 * 	Set Precision and TaxIncluded for tax calculations!
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MOrderTax (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MOrderTax
	
	/** Tax							*/
	private MTax 		m_tax = null;
	/** Cached Precision			*/
	private Integer		m_precision = null;

	/**
	 * 	Get Precision
	 * 	@return Returns the set precision or 2
	 */
	private int getPrecision ()
	{
		if (m_precision == null)
			return 2;
		return m_precision.intValue();
	}	//	getPrecision

	/**
	 * 	Set Precision
	 *	@param precision The precision to set.
	 */
	protected void setPrecision (int precision)
	{
		m_precision = Integer.valueOf(precision);
	}	//	setPrecision

	/**
	 * 	Get Tax (immutable)
	 *	@return tax
	 */
	protected MTax getTax()
	{
		if (m_tax == null)
			m_tax = MTax.get(getCtx(), getC_Tax_ID());
		return m_tax;
	}	//	getTax
	
	/**
	 * 	Calculate/Set Tax Amt from Order Lines
	 * 	@return true if calculated
	 */
	public boolean calculateTaxFromLines ()
	{
		BigDecimal taxBaseAmt = Env.ZERO;
		BigDecimal taxAmt = Env.ZERO;
		//
		boolean documentLevel = getTax().isDocumentLevel();
		MTax tax = getTax();
		int parentTaxId = tax.getParent_Tax_ID();
		//
		String sql = "SELECT LineNetAmt FROM C_OrderLine WHERE C_Order_ID=? ";
		if (parentTaxId > 0)
			sql += "AND C_Tax_ID IN (?, ?) ";
		else
			sql += "AND C_Tax_ID=? ";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getC_Order_ID());
			pstmt.setInt (2, getC_Tax_ID());
			if (parentTaxId > 0)
				pstmt.setInt(3,  parentTaxId);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				BigDecimal baseAmt = rs.getBigDecimal(1);
				taxBaseAmt = taxBaseAmt.add(baseAmt);
				//
				if (!documentLevel)		// calculate line tax
					taxAmt = taxAmt.add(tax.calculateTax(baseAmt, isTaxIncluded(), getPrecision()));
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, get_TrxName(), e);
			taxBaseAmt = null;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//
		if (taxBaseAmt == null)
			return false;
		
		//	Calculate Tax
		if (documentLevel)		//	document level
			taxAmt = tax.calculateTax(taxBaseAmt, isTaxIncluded(), getPrecision());
		setTaxAmt(taxAmt);

		//	Set Base
		if (isTaxIncluded())
			setTaxBaseAmt (taxBaseAmt.subtract(taxAmt));
		else
			setTaxBaseAmt (taxBaseAmt);
		if (log.isLoggable(Level.FINE)) log.fine(toString());
		return true;
	}	//	calculateTaxFromLines

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MOrderTax[")
			.append("C_Order_ID=").append(getC_Order_ID())
			.append(", C_Tax_ID=").append(getC_Tax_ID())
			.append(", Base=").append(getTaxBaseAmt())
			.append(", Tax=").append(getTaxAmt())
			.append ("]");
		return sb.toString ();
	}	//	toString

}	//	MOrderTax
