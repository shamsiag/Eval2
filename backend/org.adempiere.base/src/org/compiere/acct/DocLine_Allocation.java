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
package org.compiere.acct;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.model.MAllocationLine;
import org.compiere.model.MPayment;
import org.compiere.util.DB;

/**
 *	DocLine for {@link MAllocationLine}
 *	
 *  @author Jorg Janke
 *  @version $Id: DocLine_Allocation.java,v 1.2 2006/07/30 00:53:33 jjanke Exp $
 */
public class DocLine_Allocation extends DocLine
{
	/**
	 * 	DocLine_Allocation
	 *	@param line allocation line
	 *	@param doc header
	 */
	public DocLine_Allocation (MAllocationLine line, Doc doc)
	{
		super (line, doc);
		m_C_Payment_ID = line.getC_Payment_ID();
		m_C_CashLine_ID = line.getC_CashLine_ID();
		m_C_Invoice_ID = line.getC_Invoice_ID();
		m_C_Order_ID = line.getC_Order_ID();
		// adaxa-pb
		Object obj = line.get_Value("C_Charge_ID");
		if  ( obj != null )
			m_C_Charge_ID = (Integer) line.get_Value("C_Charge_ID");
		else m_C_Charge_ID = 0;
		// end adaxa-pb
		setAmount(line.getAmount());
		m_DiscountAmt = line.getDiscountAmt();
		m_WriteOffAmt = line.getWriteOffAmt();
		m_OverUnderAmt = line.getOverUnderAmt();

		//	Get Payment Conversion Rate
		if (line.getC_Payment_ID() != 0)
		{
			MPayment payment = new MPayment (doc.getCtx(), line.getC_Payment_ID(), doc.getTrxName());
			int C_ConversionType_ID = payment.getC_ConversionType_ID();
			this.setC_ConversionType_ID(C_ConversionType_ID);
		}
	}	//	DocLine_Allocation

	private int 		m_C_Invoice_ID;
	private int 		m_C_Payment_ID;
	private int 		m_C_CashLine_ID;
	private int 		m_C_Order_ID;
	private int 		m_C_Charge_ID; // adaxa-pb
	private BigDecimal	m_DiscountAmt; 
	private BigDecimal	m_WriteOffAmt; 
	private BigDecimal	m_OverUnderAmt; 
		
	/**
	 * 	Get Invoice C_Currency_ID
	 *	@return C_Currency_ID (0 if no invoice, -1 if not found)
	 */
	public int getInvoiceC_Currency_ID()
	{
		if (m_C_Invoice_ID == 0)
			return 0;
		String sql = "SELECT C_Currency_ID "
			+ "FROM C_Invoice "
			+ "WHERE C_Invoice_ID=?";
		return  DB.getSQLValue(null, sql, m_C_Invoice_ID);

	}	//	getInvoiceC_Currency_ID

	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("DocLine_Allocation[");
		sb.append(get_ID())
			.append(",Amt=").append(getAmtSource())
			.append(",Discount=").append(getDiscountAmt())
			.append(",WriteOff=").append(getWriteOffAmt())
			.append(",OverUnderAmt=").append(getOverUnderAmt())
			.append(" - C_Payment_ID=").append(m_C_Payment_ID)
			.append(",C_CashLine_ID=").append(m_C_CashLine_ID)
			.append(",C_Invoice_ID=").append(m_C_Invoice_ID)
			.append("]");
		return sb.toString ();
	}	//	toString
		
	/**
	 * @return C_Order_ID.
	 */
	public int getC_Order_ID ()
	{
		return m_C_Order_ID;
	}
	
	/**
	 * @return discountAmt.
	 */
	public BigDecimal getDiscountAmt ()
	{
		return m_DiscountAmt;
	}
	
	/**
	 * @return overUnderAmt.
	 */
	public BigDecimal getOverUnderAmt ()
	{
		return m_OverUnderAmt;
	}
	
	/**
	 * @return writeOffAmt.
	 */
	public BigDecimal getWriteOffAmt ()
	{
		return m_WriteOffAmt;
	}
	
	/**
	 * @return C_CashLine_ID.
	 */
	public int getC_CashLine_ID ()
	{
		return m_C_CashLine_ID;
	}
	
	/**
	 * @return C_Invoice_ID.
	 */
	public int getC_Invoice_ID ()
	{
		return m_C_Invoice_ID;
	}
	
	/**
	 * @return C_Payment_ID.
	 */
	public int getC_Payment_ID ()
	{
		return m_C_Payment_ID;
	}
	
	/** 
	 * @return C_Charge_ID.
	 */
	public int getC_Charge_ID ()
	{
		return m_C_Charge_ID;
	}
	
	@Override
	public Timestamp getDateConv() 
	{
		if (getC_Payment_ID() > 0)
		{
			MPayment payment = new MPayment(p_po.getCtx(), getC_Payment_ID(), p_po.get_TrxName());
			return payment.getDateAcct();  // use payment date
		}
		return super.getDateConv();
	}
	
}	//	DocLine_Allocation
