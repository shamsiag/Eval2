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
package org.compiere.process;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.logging.Level;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MProcessPara;
import org.compiere.model.MWarehouse;
import org.compiere.util.DB;
import org.compiere.util.TimeUtil;


/**
 *  Inventory Valuation.
 *  Process to fill T_InventoryValue
 *
 *  @author     Jorg Janke
 *  @version    $Id: InventoryValue.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 *  @author 	Michael Judd (mjudd) Akuna Ltd - BF [ 2685127 ]
 *  
 */
@org.adempiere.base.annotation.Process
public class InventoryValue extends SvrProcess
{
	/** Price List Used         */
	private int         p_M_PriceList_Version_ID;
	/** Valuation Date          */
	private Timestamp   p_DateValue;
	/** Warehouses               */
	private int[]       p_M_Warehouse_IDs;
	/** Currency                */
	private int         p_C_Currency_ID;
	/** Optional Cost Element	*/
	private int			p_M_CostElement_ID;

	/**
	 *  Prepare - get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_PriceList_Version_ID"))
				p_M_PriceList_Version_ID = para[i].getParameterAsInt();
			else if (name.equals("DateValue"))
				p_DateValue = (Timestamp)para[i].getParameter();
			else if (name.equals("M_Warehouse_IDs"))
				p_M_Warehouse_IDs = para[i].getParameterAsIntArray();
			else if (name.equals("C_Currency_ID"))
				p_C_Currency_ID = para[i].getParameterAsInt();
			else if (name.equals("M_CostElement_ID"))
				p_M_CostElement_ID = para[i].getParameterAsInt();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
		if (p_DateValue == null)
			p_DateValue = new Timestamp (System.currentTimeMillis());
	}   //  prepare

	/**
	 *  Perform process.
	 *  <pre>
	 *  - Fill Table with QtyOnHand for Warehouse and Valuation Date
	 *  - Perform Price Calculations
	 *  </pre>
	 * @return Message
	 * @throws Exception
	 */
	protected String doIt() throws Exception
	{
	  if (log.isLoggable(Level.INFO)) log.info("M_Warehouse_IDs=" + Arrays.toString(p_M_Warehouse_IDs)
			+ ",C_Currency_ID=" + p_C_Currency_ID
			+ ",DateValue=" + p_DateValue
			+ ",M_PriceList_Version_ID=" + p_M_PriceList_Version_ID
			+ ",M_CostElement_ID=" + p_M_CostElement_ID);

	  //  Delete (just to be sure)
	  final String sql0 = "DELETE FROM T_InventoryValue WHERE AD_PInstance_ID=?";
	  int no = DB.executeUpdateEx(sql0, new Object[] {getAD_PInstance_ID()}, get_TrxName());

	  MClient c = MClient.get(getCtx(), getAD_Client_ID());
	  MAcctSchema as = c.getAcctSchema();
	  String msg = "";

	  if (p_M_Warehouse_IDs == null) // process all warehouses
		  p_M_Warehouse_IDs = DB.getIDsEx(get_TrxName(), "SELECT M_Warehouse_ID FROM M_Warehouse WHERE AD_Client_ID=? AND IsActive='Y'", getAD_Client_ID());

	  for (int l_M_Warehouse_ID: p_M_Warehouse_IDs) {
		MWarehouse wh = MWarehouse.get(getCtx(), l_M_Warehouse_ID);

		//	Insert Standard Costs
		final String sql1 = "INSERT INTO T_InventoryValue "
			+ "(AD_PInstance_ID, M_Warehouse_ID, M_Product_ID, M_AttributeSetInstance_ID,"
			+ " AD_Client_ID, AD_Org_ID, CostStandard, T_InventoryValue_UU) "
			+ "SELECT ? "
			+ ", w.M_Warehouse_ID, c.M_Product_ID, c.M_AttributeSetInstance_ID,"
			+ " w.AD_Client_ID, w.AD_Org_ID, c.CurrentCostPrice, generate_UUID() "
			+ "FROM M_Warehouse w"
			+ " INNER JOIN AD_ClientInfo ci ON (w.AD_Client_ID=ci.AD_Client_ID)"
			+ " INNER JOIN C_AcctSchema acs ON (ci.C_AcctSchema1_ID=acs.C_AcctSchema_ID)"
			+ " INNER JOIN M_Cost c ON (acs.C_AcctSchema_ID=c.C_AcctSchema_ID AND acs.M_CostType_ID=c.M_CostType_ID AND c.AD_Org_ID IN (0, w.AD_Org_ID))"
			+ " INNER JOIN M_CostElement ce ON (c.M_CostElement_ID=ce.M_CostElement_ID AND ce.CostingMethod='S' AND ce.CostElementType='M') "
			+ "WHERE w.M_Warehouse_ID=?";
		int noInsertStd = DB.executeUpdateEx(sql1, new Object[] {getAD_PInstance_ID(), l_M_Warehouse_ID}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Inserted Std=" + noInsertStd);

		//	Insert addl Costs
		int noInsertCost = 0;
		if (p_M_CostElement_ID != 0)
		{
			final String sql2 = "INSERT INTO T_InventoryValue "
				+ "(AD_PInstance_ID, M_Warehouse_ID, M_Product_ID, M_AttributeSetInstance_ID,"
				+ " AD_Client_ID, AD_Org_ID, CostStandard, Cost, M_CostElement_ID, T_InventoryValue_UU) "
				+ "SELECT ?"
				+ ", w.M_Warehouse_ID, c.M_Product_ID, c.M_AttributeSetInstance_ID,"
				+ " w.AD_Client_ID, w.AD_Org_ID, 0, c.CurrentCostPrice, c.M_CostElement_ID, generate_UUID() "
				+ "FROM M_Warehouse w"
				+ " INNER JOIN AD_ClientInfo ci ON (w.AD_Client_ID=ci.AD_Client_ID)"
				+ " INNER JOIN C_AcctSchema acs ON (ci.C_AcctSchema1_ID=acs.C_AcctSchema_ID)"
				+ " INNER JOIN M_Cost c ON (acs.C_AcctSchema_ID=c.C_AcctSchema_ID AND acs.M_CostType_ID=c.M_CostType_ID AND c.AD_Org_ID IN (0, w.AD_Org_ID)) "
				+ "WHERE w.M_Warehouse_ID=?"
				+ " AND c.M_CostElement_ID=?"
				+ " AND NOT EXISTS (SELECT * FROM T_InventoryValue iv "
					+ "WHERE iv.AD_PInstance_ID=?"
					+ " AND iv.M_Warehouse_ID=w.M_Warehouse_ID"
					+ " AND iv.M_Product_ID=c.M_Product_ID"
					+ " AND iv.M_AttributeSetInstance_ID=c.M_AttributeSetInstance_ID)";
			noInsertCost = DB.executeUpdateEx(sql2.toString(), new Object[] {getAD_PInstance_ID(), l_M_Warehouse_ID, p_M_CostElement_ID, getAD_PInstance_ID()}, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Inserted Cost=" + noInsertCost);
			//	Update Std Cost Records
			final String sql3 = "UPDATE T_InventoryValue iv "
				+ "SET (Cost, M_CostElement_ID)="
					+ "(SELECT c.CurrentCostPrice, c.M_CostElement_ID "
					+ "FROM M_Warehouse w"
					+ " INNER JOIN AD_ClientInfo ci ON (w.AD_Client_ID=ci.AD_Client_ID)"
					+ " INNER JOIN C_AcctSchema acs ON (ci.C_AcctSchema1_ID=acs.C_AcctSchema_ID)"
					+ " INNER JOIN M_Cost c ON (acs.C_AcctSchema_ID=c.C_AcctSchema_ID"
						+ " AND acs.M_CostType_ID=c.M_CostType_ID AND c.AD_Org_ID IN (0, w.AD_Org_ID)) "
					+ "WHERE c.M_CostElement_ID=?"
					+ " AND iv.M_Warehouse_ID=w.M_Warehouse_ID"
					+ " AND iv.M_Product_ID=c.M_Product_ID"
					+ " AND iv.AD_PInstance_ID=? "
					+ " AND iv.M_AttributeSetInstance_ID=c.M_AttributeSetInstance_ID) "
				+ "WHERE M_Warehouse_ID=? AND EXISTS (SELECT * FROM T_InventoryValue ivv "
					+ "WHERE ivv.AD_PInstance_ID=?"
					+ " AND ivv.M_CostElement_ID IS NULL"
					+ " AND ivv.M_Warehouse_ID=?)";
			int noUpdatedCost = DB.executeUpdateEx(sql3, new Object[] {p_M_CostElement_ID, getAD_PInstance_ID(), l_M_Warehouse_ID, getAD_PInstance_ID(), l_M_Warehouse_ID}, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Updated Cost=" + noUpdatedCost);
		}		
		if ((noInsertStd+noInsertCost) == 0)
			return "No Costs found in warehouse " + wh.getName();
		
		//  Update Constants
		Timestamp l_DateValue = new Timestamp(TimeUtil.addDays(TimeUtil.getDay(p_DateValue), 1).getTime() - 1); // 23:59:59 from param date
		final String sql4 = "UPDATE T_InventoryValue SET DateValue=?,"
			+ "M_PriceList_Version_ID=?,"
			+ "C_Currency_ID=?"
			+ " WHERE AD_PInstance_ID=? AND M_Warehouse_ID=?";
		no = DB.executeUpdateEx(sql4.toString(), new Object[] {l_DateValue, p_M_PriceList_Version_ID, p_C_Currency_ID, getAD_PInstance_ID(), l_M_Warehouse_ID}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Constants=" + no);

		//  Get current QtyOnHand with ASI
		final String sql5 = "UPDATE T_InventoryValue iv SET QtyOnHand = "
				+ "(SELECT SUM(QtyOnHand) FROM M_StorageOnHand s"
				+ " INNER JOIN M_Locator l ON (l.M_Locator_ID=s.M_Locator_ID) "
				+ "WHERE iv.M_Product_ID=s.M_Product_ID"
				+ " AND iv.M_Warehouse_ID=l.M_Warehouse_ID"
				+ " AND iv.M_AttributeSetInstance_ID=s.M_AttributeSetInstance_ID) "
			+ "WHERE M_Warehouse_ID=? AND AD_PInstance_ID=?"
			+ " AND iv.M_AttributeSetInstance_ID<>0";
		no = DB.executeUpdateEx(sql5, new Object[] {l_M_Warehouse_ID, getAD_PInstance_ID()}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("QtHand with ASI=" + no);
		//  Get current QtyOnHand without ASI
		final String sql6 = "UPDATE T_InventoryValue iv SET QtyOnHand = "
				+ "(SELECT SUM(QtyOnHand) FROM M_StorageOnHand s"
				+ " INNER JOIN M_Locator l ON (l.M_Locator_ID=s.M_Locator_ID) "
				+ "WHERE iv.M_Product_ID=s.M_Product_ID"
				+ " AND iv.M_Warehouse_ID=l.M_Warehouse_ID) "
			+ "WHERE iv.M_Warehouse_ID=? AND iv.AD_PInstance_ID=?"
			+ " AND iv.M_AttributeSetInstance_ID=0";
		no = DB.executeUpdateEx(sql6, new Object[] {l_M_Warehouse_ID, getAD_PInstance_ID()}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("QtHand w/o ASI=" + no);
		
		//  Adjust for Valuation Date
		final String sql7 = "UPDATE T_InventoryValue iv "
			+ "SET QtyOnHand="
				+ "(SELECT iv.QtyOnHand - NVL(SUM(t.MovementQty), 0) "
				+ "FROM M_Transaction t"
				+ " INNER JOIN M_Locator l ON (t.M_Locator_ID=l.M_Locator_ID) "
				+ "WHERE t.M_Product_ID=iv.M_Product_ID"
				+ " AND t.M_AttributeSetInstance_ID=iv.M_AttributeSetInstance_ID"
				+ " AND t.MovementDate > iv.DateValue"
				+ " AND l.M_Warehouse_ID=iv.M_Warehouse_ID) "
			+ "WHERE iv.M_AttributeSetInstance_ID<>0"
			+ " AND iv.M_Warehouse_ID=? AND iv.AD_PInstance_ID=?";
		no = DB.executeUpdateEx(sql7, new Object[] {l_M_Warehouse_ID, getAD_PInstance_ID()}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Update with ASI=" + no);
		//
		final String sql8 = "UPDATE T_InventoryValue iv "
			+ "SET QtyOnHand="
				+ "(SELECT iv.QtyOnHand - NVL(SUM(t.MovementQty), 0) "
				+ "FROM M_Transaction t"
				+ " INNER JOIN M_Locator l ON (t.M_Locator_ID=l.M_Locator_ID) "
				+ "WHERE t.M_Product_ID=iv.M_Product_ID"
				+ " AND t.MovementDate > iv.DateValue"
				+ " AND l.M_Warehouse_ID=iv.M_Warehouse_ID) "
			+ "WHERE iv.M_AttributeSetInstance_ID=0 "
			+ "AND iv.M_Warehouse_ID=? AND iv.AD_PInstance_ID=?";
		no = DB.executeUpdateEx(sql8, new Object[] {l_M_Warehouse_ID, getAD_PInstance_ID()}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Update w/o ASI=" + no);
		
		//  Delete Records w/o OnHand Qty
		final String sql9 = "DELETE FROM T_InventoryValue "
			+ "WHERE (QtyOnHand=0 OR QtyOnHand IS NULL) AND M_Warehouse_ID=? AND AD_PInstance_ID=?";
		int noQty = DB.executeUpdateEx (sql9, new Object[] {l_M_Warehouse_ID, getAD_PInstance_ID()}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("NoQty Deleted=" + noQty);

		//  Update Prices
		final String sql10 = "UPDATE T_InventoryValue iv "
			+ "SET PricePO = "
				+ "(SELECT MAX(currencyConvert (po.PriceList,po.C_Currency_ID,iv.C_Currency_ID,iv.DateValue,null, po.AD_Client_ID,po.AD_Org_ID))"
				+ " FROM M_Product_PO po WHERE po.M_Product_ID=iv.M_Product_ID"
				+ " AND po.IsCurrentVendor='Y'), "
			+ "PriceList = "
				+ "(SELECT currencyConvert(pp.PriceList,pl.C_Currency_ID,iv.C_Currency_ID,iv.DateValue,null, pl.AD_Client_ID,pl.AD_Org_ID)"
				+ " FROM M_PriceList pl, M_PriceList_Version plv, M_ProductPrice pp"
				+ " WHERE pp.M_Product_ID=iv.M_Product_ID AND pp.M_PriceList_Version_ID=iv.M_PriceList_Version_ID"
				+ " AND pp.M_PriceList_Version_ID=plv.M_PriceList_Version_ID"
				+ " AND plv.M_PriceList_ID=pl.M_PriceList_ID), "
			+ "PriceStd = "
				+ "(SELECT currencyConvert(pp.PriceStd,pl.C_Currency_ID,iv.C_Currency_ID,iv.DateValue,null, pl.AD_Client_ID,pl.AD_Org_ID)"
				+ " FROM M_PriceList pl, M_PriceList_Version plv, M_ProductPrice pp"
				+ " WHERE pp.M_Product_ID=iv.M_Product_ID AND pp.M_PriceList_Version_ID=iv.M_PriceList_Version_ID"
				+ " AND pp.M_PriceList_Version_ID=plv.M_PriceList_Version_ID"
				+ " AND plv.M_PriceList_ID=pl.M_PriceList_ID), "
			+ "PriceLimit = "
				+ "(SELECT currencyConvert(pp.PriceLimit,pl.C_Currency_ID,iv.C_Currency_ID,iv.DateValue,null, pl.AD_Client_ID,pl.AD_Org_ID)"
				+ " FROM M_PriceList pl, M_PriceList_Version plv, M_ProductPrice pp"
				+ " WHERE pp.M_Product_ID=iv.M_Product_ID AND pp.M_PriceList_Version_ID=iv.M_PriceList_Version_ID"
				+ " AND pp.M_PriceList_Version_ID=plv.M_PriceList_Version_ID"
				+ " AND plv.M_PriceList_ID=pl.M_PriceList_ID)"
				+ " WHERE iv.M_Warehouse_ID=? AND iv.AD_PInstance_ID=?";
		no = DB.executeUpdateEx (sql10, new Object[] {l_M_Warehouse_ID, getAD_PInstance_ID()}, get_TrxName());
		if (no == 0)
			msg = msg + " / No Prices in warehouse " + wh.getName();

		//	Convert if different Currency
		if (as.getC_Currency_ID() != p_C_Currency_ID)
		{
			final String sql11 = "UPDATE T_InventoryValue iv "
				+ "SET CostStandard= "
					+ "(SELECT currencyConvert(iv.CostStandard,acs.C_Currency_ID,iv.C_Currency_ID,iv.DateValue,null, iv.AD_Client_ID,iv.AD_Org_ID) "
					+ "FROM C_AcctSchema acs WHERE acs.C_AcctSchema_ID=?),"
				+ "	Cost= "
					+ "(SELECT currencyConvert(iv.Cost,acs.C_Currency_ID,iv.C_Currency_ID,iv.DateValue,null, iv.AD_Client_ID,iv.AD_Org_ID) "
					+ "FROM C_AcctSchema acs WHERE acs.C_AcctSchema_ID=?) "
				+ "WHERE iv.M_Warehouse_ID=? AND iv.AD_PInstance_ID=?";
			no = DB.executeUpdateEx (sql11, new Object[] {as.getC_AcctSchema_ID(), as.getC_AcctSchema_ID(), l_M_Warehouse_ID, getAD_PInstance_ID()}, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Converted=" + no);
		}
		
		//  Update Values
		final String dbeux = "UPDATE T_InventoryValue SET "
				+ "PricePOAmt = QtyOnHand * PricePO, "
				+ "PriceListAmt = QtyOnHand * PriceList, "
				+ "PriceStdAmt = QtyOnHand * PriceStd, "
				+ "PriceLimitAmt = QtyOnHand * PriceLimit, "
				+ "CostStandardAmt = QtyOnHand * CostStandard, "
				+ "CostAmt = QtyOnHand * Cost "
				+ "WHERE M_Warehouse_ID=? AND AD_PInstance_ID=?";
		no = DB.executeUpdateEx(dbeux.toString(), new Object[] {l_M_Warehouse_ID, getAD_PInstance_ID()}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Calculation=" + no);
	  }
	  //
	  return "@OK@";
	}   //  doIt

}   //  InventoryValue
