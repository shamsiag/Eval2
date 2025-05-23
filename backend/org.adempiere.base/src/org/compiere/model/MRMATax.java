/******************************************************************************
 * Copyright (C) 2013 Elaine Tan                                              *
 * Copyright (C) 2013 Trek Global
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
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
 * 
 * @author Elaine
 *
 */
public class MRMATax extends X_M_RMATax 
{	
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -8702466449639865049L;

	/**
	 * 	Get Tax Line for RMA Line
	 *	@param line RMA line
	 *	@param precision currency precision
	 *	@param oldTax true to use old tax (get_ValueOld("C_Tax_ID"))
	 *	@param trxName transaction
	 *	@return existing or new MRMATax record
	 */
	public static MRMATax get (MRMALine line, int precision, 
		boolean oldTax, String trxName)
	{
		MRMATax retValue = null;
		if (line == null || line.getM_RMA_ID() == 0)
		{
			s_log.fine("No RMA");
			return null;
		}
		int C_Tax_ID = line.getC_Tax_ID();
		boolean isOldTax = oldTax && line.is_ValueChanged(MRMATax.COLUMNNAME_C_Tax_ID); 
		if (isOldTax)
		{
			Object old = line.get_ValueOld(MRMATax.COLUMNNAME_C_Tax_ID);
			if (old == null)
			{
				s_log.fine("No Old Tax");
				return null;
			}
			C_Tax_ID = ((Integer)old).intValue();
		}
		if (C_Tax_ID == 0)
		{
			s_log.fine("No Tax");
			return null;
		}
		
		String sql = "SELECT * FROM M_RMATax WHERE M_RMA_ID=? AND C_Tax_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, line.getM_RMA_ID());
			pstmt.setInt (2, C_Tax_ID);
			rs = pstmt.executeQuery ();
			if (rs.next ())
				retValue = new MRMATax (line.getCtx(), rs, trxName);
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
		retValue = new MRMATax(line.getCtx(), 0, trxName);
		retValue.set_TrxName(trxName);
		retValue.setClientOrg(line);
		retValue.setM_RMA_ID(line.getM_RMA_ID());
		retValue.setC_Tax_ID(line.getC_Tax_ID());
		retValue.setPrecision(precision);
		retValue.setIsTaxIncluded(line.getParent().isTaxIncluded());
		if (s_log.isLoggable(Level.FINE)) s_log.fine("(new) " + retValue);
		return retValue;
	}
    
	/**
	 * 	Get Child Tax Lines for RMA Line
	 *	@param line RMA line
	 *	@param precision currency precision
	 *	@param oldTax true to use old tax (get_ValueOld("C_Tax_ID"))
	 *	@param trxName transaction
	 *	@return existing or new MRMATax record or empty MRMATax array if line MTax has no child taxes
	 */
	public static MRMATax[] getChildTaxes(MRMALine line, int precision, 
		boolean oldTax, String trxName)
	{
		List<MRMATax> rmaTaxes = new ArrayList<MRMATax>();
		if (line == null || line.getM_RMA_ID() == 0)
		{
			return rmaTaxes.toArray(new MRMATax[0]);
		}
		int C_Tax_ID = line.getC_Tax_ID();
		if (oldTax)
		{
			Object old = line.get_ValueOld(MRMATax.COLUMNNAME_C_Tax_ID);
			if (old == null)
			{
				return rmaTaxes.toArray(new MRMATax[0]);
			}
			C_Tax_ID = ((Integer)old).intValue();
		}
		if (C_Tax_ID == 0)
		{
			return rmaTaxes.toArray(new MRMATax[0]);
		}
		
		MTax tax = MTax.get(C_Tax_ID);
		if (!tax.isSummary())
			return rmaTaxes.toArray(new MRMATax[0]);
		
		MTax[] cTaxes = tax.getChildTaxes(false);
		for(MTax cTax : cTaxes) {
			MRMATax rmaTax = null;
			String sql = "SELECT * FROM M_RMATax WHERE M_RMA_ID=? AND C_Tax_ID=?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql, trxName);
				pstmt.setInt (1, line.getM_RMA_ID());
				pstmt.setInt (2, cTax.getC_Tax_ID());
				rs = pstmt.executeQuery ();
				if (rs.next ())
					rmaTax = new MRMATax (line.getCtx(), rs, trxName);
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
			if (rmaTax != null)
			{
				rmaTax.setPrecision(precision);
				rmaTax.set_TrxName(trxName);
				rmaTaxes.add(rmaTax);
			}
			// If the old tax was required and there is no MOrderTax for that
			// return null, and not create another MOrderTax - teo_sarca [ 1583825 ]
			else 
			{
				if (oldTax)
					continue;
			}
			
			if (rmaTax == null)
			{
				//	Create New
				rmaTax = new MRMATax(line.getCtx(), 0, trxName);
				rmaTax.set_TrxName(trxName);
				rmaTax.setClientOrg(line);
				rmaTax.setM_RMA_ID(line.getM_RMA_ID());
				rmaTax.setC_Tax_ID(cTax.getC_Tax_ID());
				rmaTax.setPrecision(precision);
				rmaTax.setIsTaxIncluded(line.getParent().isTaxIncluded());
				rmaTaxes.add(rmaTax);
			}
		}
		return rmaTaxes.toArray(new MRMATax[0]);
	}
	
	/**	Static Logger	*/
	private static CLogger	s_log	= CLogger.getCLogger (MRMATax.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_RMATax_UU  UUID key
     * @param trxName Transaction
     */
    public MRMATax(Properties ctx, String M_RMATax_UU, String trxName) {
        super(ctx, M_RMATax_UU, trxName);
		if (Util.isEmpty(M_RMATax_UU))
			setInitialDefaults();
    }

	/**
	 *	@param ctx context
	 *	@param ignored ignored
	 *	@param trxName transaction
	 */
	public MRMATax (Properties ctx, int ignored, String trxName)
	{
		super(ctx, 0, trxName);
		if (ignored != 0)
			throw new IllegalArgumentException("Multi-Key");
		setInitialDefaults();
	}

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
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MRMATax (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}
	
	/** Tax							*/
	private MTax 		m_tax = null;
	/** Cached Precision			*/
	private Integer		m_precision = null;

	/**
	 * 	Get Precision
	 * 	@return Returns set precision or 2
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
	 * 	Calculate/Set Tax Amt from RMA Lines
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
		String sql = "SELECT LineNetAmt FROM M_RMALine WHERE M_RMA_ID=? ";
		if (parentTaxId > 0)
			sql += "AND C_Tax_ID IN (?, ?) ";
		else
			sql += "AND C_Tax_ID=? ";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getM_RMA_ID());
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
		StringBuilder sb = new StringBuilder ("MRMATax[")
			.append("M_RMA_ID=").append(getM_RMA_ID())
			.append(", C_Tax_ID=").append(getC_Tax_ID())
			.append(", Base=").append(getTaxBaseAmt())
			.append(", Tax=").append(getTaxAmt())
			.append ("]");
		return sb.toString ();
	}	//	toString
}
