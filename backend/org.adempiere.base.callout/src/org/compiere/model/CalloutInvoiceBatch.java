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
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.Core;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;


/**
 *	Callouts for Invoice Batch
 *	
 *  @author Jorg Janke
 *  @version $Id: CalloutInvoiceBatch.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class CalloutInvoiceBatch extends CalloutEngine
{
	/**
	 *	Invoice Batch Line - DateInvoiced.
	 * 		- updates DateAcct
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab
	 *	@param mField field
	 *	@param value value
	 *	@return null or error message
	 */
	public String date (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (value == null)
			return "";
		mTab.setValue ("DateAcct", value);
		//
		setDocumentNo(ctx, WindowNo, mTab);
		return "";
	}	//	date

	
	
	/**
	 *	Invoice Batch Line - BPartner.
	 *		- C_BPartner_Location_ID
	 *		- AD_User_ID
	 *		- PaymentRule
	 *		- C_PaymentTerm_ID
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab
	 *	@param mField field
	 *	@param value value
	 *	@return null or error message
	 */
	public String bPartner (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_BPartner_ID = (Integer)value;
		if (C_BPartner_ID == null || C_BPartner_ID.intValue() == 0)
			return "";

		String sql = "SELECT p.AD_Language,p.C_PaymentTerm_ID,"
			+ " COALESCE(p.M_PriceList_ID,g.M_PriceList_ID) AS M_PriceList_ID, p.PaymentRule,p.POReference,"
			+ " p.SO_Description,p.IsDiscountPrinted,"
			+ " p.SO_CreditLimit, p.SO_CreditLimit-p.SO_CreditUsed AS CreditAvailable,"
			+ " (select max(lbill.C_BPartner_Location_ID) from C_BPartner_Location lbill where p.C_BPartner_ID=lbill.C_BPartner_ID AND lbill.IsBillTo='Y' AND lbill.IsActive='Y') AS C_BPartner_Location_ID,"
			+ " (select max(c.AD_User_ID) from AD_User c where p.C_BPartner_ID=c.C_BPartner_ID AND c.IsActive='Y') as AD_User_ID,"
			+ " COALESCE(p.PO_PriceList_ID,g.PO_PriceList_ID) AS PO_PriceList_ID, p.PaymentRulePO,p.PO_PaymentTerm_ID " 
			+ "FROM C_BPartner p"
			+ " INNER JOIN C_BP_Group g ON (p.C_BP_Group_ID=g.C_BP_Group_ID)"
			+ "WHERE p.C_BPartner_ID=? AND p.IsActive='Y'";		//	#1

		boolean IsSOTrx = Env.getContext(ctx, WindowNo, "IsSOTrx").equals("Y");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, C_BPartner_ID.intValue());
			rs = pstmt.executeQuery();
			//
			if (rs.next())
			{
				//	PaymentRule
				String s = rs.getString(IsSOTrx ? "PaymentRule" : "PaymentRulePO");
				if (s != null && s.length() != 0)
					mTab.setValue("PaymentRule", s);
				if (Env.getContext(ctx, WindowNo, "DocBaseType").endsWith("C"))	//	Credits are Payment Term
					s = X_C_Invoice.PAYMENTRULE_OnCredit;
				//  Payment Term
				Integer ii = Integer.valueOf(rs.getInt(IsSOTrx ? "C_PaymentTerm_ID" : "PO_PaymentTerm_ID"));
				if (!rs.wasNull())
					mTab.setValue("C_PaymentTerm_ID", ii);

				//	Location
				int locID = rs.getInt("C_BPartner_Location_ID");
				//	overwritten by InfoBP selection - works only if InfoWindow
				//	was used otherwise creates error (uses last value, may belong to differnt BP)
				if (C_BPartner_ID.toString().equals(Env.getContext(ctx, WindowNo, Env.TAB_INFO, "C_BPartner_ID")))
				{
					String loc = Env.getContext(ctx, WindowNo, Env.TAB_INFO, "C_BPartner_Location_ID");
					if (loc.length() > 0)
						locID = Integer.parseInt(loc);
				}
				if (locID == 0)
					mTab.setValue("C_BPartner_Location_ID", null);
				else
					mTab.setValue("C_BPartner_Location_ID", Integer.valueOf(locID));

				//	Contact - overwritten by InfoBP selection
				int contID = rs.getInt("AD_User_ID");
				if (C_BPartner_ID.toString().equals(Env.getContext(ctx, WindowNo, Env.TAB_INFO, "C_BPartner_ID")))
				{
					String cont = Env.getContext(ctx, WindowNo, Env.TAB_INFO, "AD_User_ID");
					if (cont.length() > 0)
						contID = Integer.parseInt(cont);
				}
				if (contID == 0)
					mTab.setValue("AD_User_ID", null);
				else
					mTab.setValue("AD_User_ID", Integer.valueOf(contID));

				//	CreditAvailable
				if (IsSOTrx)
				{
					double CreditLimit = rs.getDouble("SO_CreditLimit");
					if (CreditLimit != 0)
					{
						double CreditAvailable = rs.getDouble("CreditAvailable");
						if (!rs.wasNull() && CreditAvailable < 0)
							mTab.fireDataStatusEEvent("CreditLimitOver",
								DisplayType.getNumberFormat(DisplayType.Amount).format(CreditAvailable),
								false);
					}
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
			return e.getLocalizedMessage();
		}
		finally
		{
			DB.close(rs, pstmt);
		}
		//
		setDocumentNo(ctx, WindowNo, mTab);
		return tax (ctx, WindowNo, mTab, mField, value);
	}	//	bPartner

	/**
	 *	Document Type.
	 *		- called from DocType
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab
	 *	@param mField field
	 *	@param value value
	 *	@return null or error message
	 */
	public String docType (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		setDocumentNo(ctx, WindowNo, mTab);
		return "";
	}	//	docType

	/**
	 *	Set Document No (increase existing)
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 */
	private void setDocumentNo (Properties ctx, int WindowNo, GridTab mTab)
	{
		//	Get last line
		int C_InvoiceBatch_ID = Env.getContextAsInt(ctx, WindowNo, "C_InvoiceBatch_ID");
		String sql = "SELECT COALESCE(MAX(C_InvoiceBatchLine_ID),0) FROM C_InvoiceBatchLine WHERE C_InvoiceBatch_ID=?";
		int C_InvoiceBatchLine_ID = DB.getSQLValue(null, sql, C_InvoiceBatch_ID);
		if (C_InvoiceBatchLine_ID == 0)
			return;
		MInvoiceBatchLine last = new MInvoiceBatchLine(Env.getCtx(), C_InvoiceBatchLine_ID, null);
		
		//	Need to Increase when different DocType or BP
		int C_DocType_ID = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");
		int C_BPartner_ID = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_ID");
		if (C_DocType_ID == last.getC_DocType_ID()
			&& C_BPartner_ID == last.getC_BPartner_ID())
			return;

		//	New Number
		String oldDocNo = last.getDocumentNo();
		if (oldDocNo == null)
			return;
		int docNo = 0;
		try
		{
			docNo = Integer.parseInt(oldDocNo);
		}
		catch (Exception e)
		{
		}
		if (docNo == 0)
			return;
		String newDocNo = String.valueOf(docNo+1);
		mTab.setValue("DocumentNo", newDocNo);
	}	//	setDocumentNo
	
	/**
	 *	Invoice Batch Line - Charge.
	 * 		- updates PriceEntered from Charge
	 * 	Calles tax
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab
	 *	@param mField field
	 *	@param value value
	 *	@return null or error message
	 */
	public String charge (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer C_Charge_ID = (Integer)value;
		if (C_Charge_ID == null || C_Charge_ID.intValue() == 0)
			return "";

		String sql = "SELECT ChargeAmt FROM C_Charge WHERE C_Charge_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, C_Charge_ID.intValue());
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				mTab.setValue ("PriceEntered", rs.getBigDecimal (1));
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
			return e.getLocalizedMessage();
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		//
		return tax (ctx, WindowNo, mTab, mField, value);
	}	//	charge
	
	/**
	 *	Invoice Line - Tax.
	 *		- basis: Charge, BPartner Location
	 *		- sets C_Tax_ID
	 *  Calles Amount
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab
	 *	@param mField field
	 *	@param value value
	 *	@return null or error message
	 */
	public String tax (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		String column = mField.getColumnName();
		if (value == null)
			return "";

		int C_Charge_ID = 0;
		if (column.equals("C_Charge_ID"))
			C_Charge_ID = ((Integer)value).intValue();
		else
			C_Charge_ID = Env.getContextAsInt(ctx, WindowNo, "C_Charge_ID");
		if (log.isLoggable(Level.FINE)) log.fine("C_Charge_ID=" + C_Charge_ID);
		if (C_Charge_ID == 0)
			return amt (ctx, WindowNo, mTab, mField, value);	//

		//	Check Partner Location
		int C_BPartner_Location_ID = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_Location_ID");
		if (C_BPartner_Location_ID == 0)
			return amt (ctx, WindowNo, mTab, mField, value);	//
		if (log.isLoggable(Level.FINE)) log.fine("BP_Location=" + C_BPartner_Location_ID);

		//	Dates
		Timestamp billDate = Env.getContextAsDate(ctx, WindowNo, "DateInvoiced");
		if (log.isLoggable(Level.FINE)) log.fine("Bill Date=" + billDate);
		Timestamp shipDate = billDate;
		if (log.isLoggable(Level.FINE)) log.fine("Ship Date=" + shipDate);

		int AD_Org_ID = Env.getContextAsInt(ctx, WindowNo, "AD_Org_ID");
		if (log.isLoggable(Level.FINE)) log.fine("Org=" + AD_Org_ID);

		int M_Warehouse_ID = Env.getContextAsInt(ctx, Env.M_WAREHOUSE_ID);
		if (log.isLoggable(Level.FINE)) log.fine("Warehouse=" + M_Warehouse_ID);

		//
		String deliveryViaRule = getLineDeliveryViaRule(ctx, WindowNo, mTab);
		int dropshipLocationId = getDropShipLocationId(ctx, WindowNo, mTab);
		int C_Tax_ID = Core.getTaxLookup().get(ctx, 0, C_Charge_ID, billDate, shipDate,
			AD_Org_ID, M_Warehouse_ID, C_BPartner_Location_ID, C_BPartner_Location_ID, dropshipLocationId,
			Env.getContext(ctx, WindowNo, "IsSOTrx").equals("Y"), deliveryViaRule, null);
		if (log.isLoggable(Level.INFO)) log.info("Tax ID=" + C_Tax_ID);
		//
		if (C_Tax_ID == 0)
			mTab.fireDataStatusEEvent(CLogger.retrieveError());
		else
			mTab.setValue("C_Tax_ID", Integer.valueOf(C_Tax_ID));
		//
		return amt (ctx, WindowNo, mTab, mField, value);
	}	//	tax

	/**
	 * Get the drop shipment location ID from the related order
	 * @param ctx
	 * @param windowNo
	 * @param mTab
	 * @return
	 */
	private String getLineDeliveryViaRule(Properties ctx, int windowNo, GridTab mTab) {
		if (mTab.getValue("C_InvoiceLine_ID") != null) {
			int C_InvoiceLine_ID = (Integer) mTab.getValue("C_InvoiceLine_ID");
			if (C_InvoiceLine_ID > 0) {
				MInvoiceLine invoiceLine = new MInvoiceLine(ctx, C_InvoiceLine_ID, null);
				int C_OrderLine_ID = invoiceLine.getC_OrderLine_ID();
				if (C_OrderLine_ID > 0) {
					MOrderLine orderLine = new MOrderLine(ctx, C_OrderLine_ID, null);
					return orderLine.getParent().getDeliveryViaRule();
				}
				int M_InOutLine_ID = invoiceLine.getM_InOutLine_ID();
				if (M_InOutLine_ID > 0) {
					MInOutLine ioLine = new MInOutLine(ctx, M_InOutLine_ID, null);
					return ioLine.getParent().getDeliveryViaRule();
				}
			}			
		}
		if (mTab.getValue("C_Invoice_ID") != null) {
			int C_Invoice_ID = (Integer) mTab.getValue("C_Invoice_ID");
			if (C_Invoice_ID > 0) {
				MInvoice invoice = new MInvoice(ctx, C_Invoice_ID, null);
				I_C_Order order = invoice.getC_Order();
				if (order != null) {
					return order.getDeliveryViaRule();
				}
			}
		}
		return null;
	}

	/**
	 * Get the drop shipment location ID from the related order
	 * @param ctx
	 * @param windowNo
	 * @param mTab
	 * @return
	 */
	private int getDropShipLocationId(Properties ctx, int windowNo, GridTab mTab) {
		if (mTab.getValue("C_InvoiceLine_ID") != null) {
			int C_InvoiceLine_ID = (Integer) mTab.getValue("C_InvoiceLine_ID");
			if (C_InvoiceLine_ID > 0) {
				MInvoiceLine invoiceLine = new MInvoiceLine(ctx, C_InvoiceLine_ID, null);
				int C_OrderLine_ID = invoiceLine.getC_OrderLine_ID();
				if (C_OrderLine_ID > 0) {
					MOrderLine orderLine = new MOrderLine(ctx, C_OrderLine_ID, null);
					return orderLine.getParent().getDropShip_Location_ID();
				}
			}			
		}
		if (mTab.getValue("C_Invoice_ID") != null) {
			int C_Invoice_ID = (Integer) mTab.getValue("C_Invoice_ID");
			if (C_Invoice_ID > 0) {
				MInvoice invoice = new MInvoice(ctx, C_Invoice_ID, null);
				I_C_Order order = invoice.getC_Order();
				if (order != null) {
					return order.getDropShip_Location_ID();
				}
			}
		}
		return -1;
	}

	/**
	 *	Invoice - Amount.
	 *		- called from QtyEntered, PriceEntered
	 *		- calculates LineNetAmt
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab
	 *	@param mField field
	 *	@param value value
	 *	@return null or error message
	 */
	public String amt (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive() || value == null)
			return "";
		
		int StdPrecision = 2;		//	temporary

		//	get values
		BigDecimal QtyEntered = (BigDecimal)mTab.getValue("QtyEntered");
		BigDecimal PriceEntered = (BigDecimal)mTab.getValue("PriceEntered");
		if (log.isLoggable(Level.FINE)) log.fine("QtyEntered=" + QtyEntered + ", PriceEntered=" + PriceEntered);
		if (QtyEntered == null)
			QtyEntered = Env.ZERO;
		if (PriceEntered == null)
			PriceEntered = Env.ZERO;

		//	Line Net Amt
		BigDecimal LineNetAmt = QtyEntered.multiply(PriceEntered);
		if (LineNetAmt.scale() > StdPrecision)
			LineNetAmt = LineNetAmt.setScale(StdPrecision, RoundingMode.HALF_UP);

		//	Calculate Tax Amount
		boolean IsTaxIncluded = "Y".equals(Env.getContext(Env.getCtx(), WindowNo, "IsTaxIncluded"));
		
		BigDecimal TaxAmt = null;
		if (mField.getColumnName().equals("TaxAmt"))
		{
			TaxAmt = (BigDecimal)mTab.getValue("TaxAmt");
		}
		else
		{
			Integer taxID = (Integer)mTab.getValue("C_Tax_ID");
			if (taxID != null)
			{
				int C_Tax_ID = taxID.intValue();
				MTax tax = new MTax (ctx, C_Tax_ID, null);
				TaxAmt = tax.calculateTax(LineNetAmt, IsTaxIncluded, StdPrecision);
				mTab.setValue("TaxAmt", TaxAmt);
			}
		}
		
		if (TaxAmt == null)
			TaxAmt = BigDecimal.ZERO;
		//	
		if (IsTaxIncluded)
		{
			mTab.setValue("LineTotalAmt", LineNetAmt);
			mTab.setValue("LineNetAmt", LineNetAmt.subtract(TaxAmt));
		}
		else
		{
			mTab.setValue("LineNetAmt", LineNetAmt);
			mTab.setValue("LineTotalAmt", LineNetAmt.add(TaxAmt));
		}
		return "";
	}	//	amt

	
	
}	//	CalloutInvoiceBatch
