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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_T_DistributionRunDetail;
import org.compiere.model.MBPartner;
import org.compiere.model.MDistributionRun;
import org.compiere.model.MDistributionRunDetail;
import org.compiere.model.MDistributionRunLine;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MProcessPara;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;

/**
 *	Create Distribution	
 *	
 *  @author Jorg Janke
 *  @author  victor.perez@e-evolution.com
 * 			<li>FR Let use the Distribution List and Distribution Run for DO
 * 	@see 	https://sourceforge.net/p/adempiere/feature-requests/488/		
 *  @version $Id: DistributionRun.java,v 1.4 2006/07/30 00:51:02 jjanke Exp $
 */
@org.adempiere.base.annotation.Process
public class DistributionRun extends SvrProcess
{
	/**	The Run to execute		*/
	private int					p_M_DistributionRun_ID = 0;
	/**	Date Promised			*/
	private Timestamp			p_DatePromised = null;
	/** Document Type			*/
	private int					p_C_DocType_ID = 0;
	/** Test Mode				*/
	private boolean				p_IsTest = false;
	/** Warehouse to Distribution Order */
	private int 				p_M_Warehouse_ID = 0;
	/** Consolidate Document **/
	private boolean				p_ConsolidateDocument = false;
	/** Distribution List    **/
	@SuppressWarnings("unused")
	private int					p_M_DistributionList_ID = 0;
	/** Distribute Based in DRP Demand **/
	private boolean 			p_BasedInDamnd = false;
	
	/**	Distribution Run			*/
	private MDistributionRun		m_run = null;
	/**	Distribution Run Lines		*/
	private MDistributionRunLine[]	m_runLines = null;
	/** Distribution Run Details	*/
	private MDistributionRunDetail[]	m_details = null;

	/**	Date Ordered			*/
	private Timestamp			m_DateOrdered = null;
	/**	Orders Created			*/
	private int					m_counter = 0;
	/** Document Type			*/
	private MDocType			m_docType = null;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();

			if (para[i].getParameter() == null)
				;
			else if (name.equals("C_DocType_ID"))
			{	
				p_C_DocType_ID = ((BigDecimal)para[i].getParameter()).intValue();
				m_docType = new MDocType(getCtx(),p_C_DocType_ID, get_TrxName());
			}	
			else if (name.equals("DatePromised"))
			{	
				p_DatePromised = (Timestamp)para[i].getParameter();
			}	
			else if (name.equals("IsTest"))
				p_IsTest = "Y".equals(para[i].getParameter());
			else if (m_docType.getDocBaseType().equals(MDocType.DOCBASETYPE_DistributionOrder) && name.equals("M_Warehouse_ID"))
				p_M_Warehouse_ID=((BigDecimal)para[i].getParameter()).intValue();
			else if (m_docType.getDocBaseType().equals(MDocType.DOCBASETYPE_DistributionOrder) && name.equals("ConsolidateDocument"))
				p_ConsolidateDocument="Y".equals((String)para[i].getParameter());
			else if (m_docType.getDocBaseType().equals(MDocType.DOCBASETYPE_DistributionOrder) && name.equals("M_DistributionList_ID"))
				p_M_DistributionList_ID=para[i].getParameterAsInt();
			else if (m_docType.getDocBaseType().equals(MDocType.DOCBASETYPE_DistributionOrder) && name.equals("IsRequiredDRP"))
				p_BasedInDamnd = "Y".equals((String)para[i].getParameter());
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
		p_M_DistributionRun_ID = getRecord_ID();
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (text with variables)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("M_DistributionRun_ID=" + p_M_DistributionRun_ID 
			+ ", C_DocType_ID=" + p_C_DocType_ID
			+ ", DatePromised=" + p_DatePromised
			+ ", Test=" + p_IsTest);
		//	Distribution Run
		if (p_M_DistributionRun_ID == 0)
			throw new IllegalArgumentException ("No Distribution Run ID");
		m_run = new MDistributionRun(getCtx(), p_M_DistributionRun_ID, get_TrxName());
		if (m_run.get_ID() == 0)
			throw new Exception ("Distribution Run not found -  M_DistributionRun_ID=" +  p_M_DistributionRun_ID);
		m_runLines = m_run.getLines(true);
		if (m_runLines == null || m_runLines.length == 0)
			throw new Exception ("No active, non-zero Distribution Run Lines found");
		
		//	Document Type
		if (p_C_DocType_ID == 0)
			throw new IllegalArgumentException ("No Document Type ID");
		m_docType = new MDocType(getCtx(), p_C_DocType_ID, null);	//	outside trx
		if (m_docType.get_ID() == 0)
			throw new Exception ("Document Type not found -  C_DocType_ID=" +  p_C_DocType_ID);
		//
		m_DateOrdered = new Timestamp (System.currentTimeMillis());
		if (p_DatePromised == null)
			p_DatePromised = m_DateOrdered;
		
		if(m_docType.getDocBaseType().equals(MDocType.DOCBASETYPE_DistributionOrder) && p_M_Warehouse_ID > 0)
		{
			if(p_BasedInDamnd)
			{
				if (insertDetailsDistributionDemand() == 0)
					throw new Exception ("No Lines");
				
			}
			else  //Create Temp Lines
			{	
				if (insertDetailsDistribution() == 0)
					throw new Exception ("No Lines");
			}
		}
		else
		{	
			//	Create Temp Lines
			if (insertDetails() == 0)
				throw new Exception ("No Lines");
		}
		
		//	Order By Distribution Run Line
		m_details = MDistributionRunDetail.get(getCtx(), p_M_DistributionRun_ID, false, get_TrxName());
		//	First Run -- Add & Round
		addAllocations ();		

		//	Do Allocation
		int loops = 0;
		while (!isAllocationEqTotal ())
		{
			adjustAllocation();
			addAllocations();
			if (++loops > 10)
				throw new Exception ("Loop detected - more than 10 Allocation attempts");
		}
		
		//	Order By Business Partner
		m_details = MDistributionRunDetail.get(getCtx(), p_M_DistributionRun_ID, true, get_TrxName());
		
		//Implement Distribution Order
		if(m_docType.getDocBaseType().equals(MDocType.DOCBASETYPE_DistributionOrder))
		{
			distributionOrders();
		}
		else {
			//	Create Orders
			createOrders();
		}
		
		StringBuilder msgreturn = new StringBuilder("@Created@ #").append(m_counter);
		return msgreturn.toString();
	}	//	doIt
	

	/**
	 * 	Insert Details
	 *	@return number of rows inserted
	 */
	private int insertDetails()
	{
		//	Handle NULL
		String sql = "UPDATE M_DistributionRunLine SET MinQty = 0 WHERE MinQty IS NULL AND M_DistributionRun_ID=?";
		int no = DB.executeUpdateEx(sql,new Object[]{p_M_DistributionRun_ID} ,get_TrxName());
		sql = "UPDATE M_DistributionListLine SET MinQty = 0 WHERE MinQty IS NULL";
		no = DB.executeUpdateEx(sql, get_TrxName());
		//	Total Ratio
		sql = "UPDATE M_DistributionList l "
			+ "SET RatioTotal = (SELECT SUM(Ratio) FROM M_DistributionListLine ll "
				+ " WHERE l.M_DistributionList_ID=ll.M_DistributionList_ID) "
			+ "WHERE EXISTS (SELECT * FROM M_DistributionRunLine rl"
				+ " WHERE l.M_DistributionList_ID=rl.M_DistributionList_ID"
				+ " AND rl.M_DistributionRun_ID=?)";
		no = DB.executeUpdateEx(sql,new Object[]{p_M_DistributionRun_ID},get_TrxName());
		
		//	Delete Old
		sql = "DELETE FROM T_DistributionRunDetail WHERE M_DistributionRun_ID=?";
		no = DB.executeUpdateEx(sql,new Object[]{p_M_DistributionRun_ID}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("insertDetails - deleted #" + no);
		//	Insert New
		sql = "INSERT INTO T_DistributionRunDetail "
			+ "(M_DistributionRun_ID, M_DistributionRunLine_ID, M_DistributionList_ID, M_DistributionListLine_ID,"
			+ "AD_Client_ID,AD_Org_ID, IsActive, Created,CreatedBy, Updated,UpdatedBy,"
			+ "C_BPartner_ID, C_BPartner_Location_ID, M_Product_ID,"
			+ "Ratio, MinQty, Qty) "
			//
			+ "SELECT rl.M_DistributionRun_ID, rl.M_DistributionRunLine_ID,"
			+ "ll.M_DistributionList_ID, ll.M_DistributionListLine_ID, "
			+ "rl.AD_Client_ID,rl.AD_Org_ID, rl.IsActive, rl.Created,rl.CreatedBy, rl.Updated,rl.UpdatedBy,"
			+ "ll.C_BPartner_ID, ll.C_BPartner_Location_ID, rl.M_Product_ID, "
			+ "ll.Ratio, "
			+ "CASE WHEN rl.MinQty > ll.MinQty THEN rl.MinQty ELSE ll.MinQty END, "
			+ "(ll.Ratio/l.RatioTotal*rl.TotalQty)"
			+ "FROM M_DistributionRunLine rl"
			+ " INNER JOIN M_DistributionList l ON (rl.M_DistributionList_ID=l.M_DistributionList_ID)"
			+ " INNER JOIN M_DistributionListLine ll ON (rl.M_DistributionList_ID=ll.M_DistributionList_ID) "
			+ "WHERE rl.M_DistributionRun_ID=?"
			+ " AND l.RatioTotal<>0 AND rl.IsActive='Y' AND ll.IsActive='Y'";
		no = DB.executeUpdateEx(sql,new Object[]{p_M_DistributionRun_ID}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("inserted #" + no);
		return no;
	}	//	insertDetails

	
	/**************************************************************************
	 * 	Add up Allocations
	 */
	private void addAllocations ()
	{
		//	Reset
		for (int j = 0; j < m_runLines.length; j++)
		{
			MDistributionRunLine runLine = m_runLines[j];
			runLine.resetCalculations();
		}
		//	Add Up
		for (int i = 0; i < m_details.length; i++)
		{
			MDistributionRunDetail detail = m_details[i];
			for (int j = 0; j < m_runLines.length; j++)
			{
				MDistributionRunLine runLine = m_runLines[j];
				if (runLine.getM_DistributionRunLine_ID() == detail.getM_DistributionRunLine_ID())
				{
					//	Round
					detail.round(runLine.getUOMPrecision());
					//	Add
					runLine.addActualMin(detail.getMinQty());
					runLine.addActualQty(detail.getQty());
					runLine.addActualAllocation(detail.getActualAllocation());
					runLine.setMaxAllocation(detail.getActualAllocation(), false);
					//
					if (log.isLoggable(Level.FINE)) log.fine("RunLine=" + runLine.getLine() 
						+ ": BP_ID=" + detail.getC_BPartner_ID() 
						+ ", Min=" + detail.getMinQty()
						+ ", Qty=" + detail.getQty()
						+ ", Allocation=" + detail.getActualAllocation());
					continue;
				}
			}
		}	//	for all detail lines
		
		//	Info
		for (int j = 0; j < m_runLines.length; j++)
		{
			MDistributionRunLine runLine = m_runLines[j];
			if (log.isLoggable(Level.FINE)) log.fine("Run - " + runLine.getInfo());
		}
	}	//	addAllocations
	
	
	/**
	 * 	Is Allocation Equals Total
	 *	@return true if allocation eq total
	 *	@throws Exception
	 */
	private boolean isAllocationEqTotal() throws Exception
	{
		boolean allocationEqTotal = true;
		//	Check total min qty & delta
		for (int j = 0; j < m_runLines.length; j++)
		{
			MDistributionRunLine runLine = m_runLines[j];
			if (runLine.isActualMinGtTotal()){
				StringBuilder msg = new StringBuilder("Line ").append(runLine.getLine()) 
						.append(" Sum of Min Qty=").append(runLine.getActualMin()) 
						.append(" is greater than Total Qty=").append(runLine.getTotalQty());
				throw new Exception (msg.toString());
			}	
			if (allocationEqTotal && !runLine.isActualAllocationEqTotal())
				allocationEqTotal = false;
		}	//	for all run lines
		if (log.isLoggable(Level.INFO)) log.info("=" + allocationEqTotal);
		return allocationEqTotal;
	}	//	isAllocationEqTotal

	
	/**
	 * 	Adjust Allocation
	 * 	@throws Exception
	 */
	private void adjustAllocation() throws Exception
	{
		for (int j = 0; j < m_runLines.length; j++)
			adjustAllocation(j);
	}	//	adjustAllocation
	
	/**
	 * 	Adjust Run Line Allocation
	 * 	@param index run line index
	 * 	@throws Exception
	 */
	private void adjustAllocation(int index) throws Exception
	{
		MDistributionRunLine runLine = m_runLines[index];
		BigDecimal difference = runLine.getActualAllocationDiff();
		if (difference.compareTo(Env.ZERO) == 0)
			return;
		//	Adjust when difference is -1->1 or last difference is the same 
		boolean adjustBiggest = difference.abs().compareTo(Env.ONE) <= 0
			|| difference.abs().compareTo(runLine.getLastDifference().abs()) == 0;
		if (log.isLoggable(Level.FINE)) log.fine("Line=" + runLine.getLine() 
			+ ", Diff=" + difference + ", Adjust=" + adjustBiggest);
		//	Adjust Biggest Amount
		if (adjustBiggest)
		{
			for (int i = 0; i < m_details.length; i++)
			{
				MDistributionRunDetail detail = m_details[i];
				if (runLine.getM_DistributionRunLine_ID() == detail.getM_DistributionRunLine_ID())
				{
					if (log.isLoggable(Level.FINE)) log.fine("Biggest - DetailAllocation=" + detail.getActualAllocation()
						+ ", MaxAllocation=" + runLine.getMaxAllocation() 
						+ ", Qty Difference=" + difference);
					if (detail.getActualAllocation().compareTo(runLine.getMaxAllocation()) == 0
						&& detail.isCanAdjust())
					{
						detail.adjustQty(difference);
						detail.saveEx();
						return;
					}
				}
			}	//	for all detail lines
			StringBuilder msgexc = new StringBuilder("Cannot adjust Difference = ").append(difference) 
					.append(" - You need to change Total Qty or Min Qty");
			throw new Exception (msgexc.toString());
		}
		else	//	Distibute
		{
			//	New Total Ratio
			BigDecimal ratioTotal = Env.ZERO;
			for (int i = 0; i < m_details.length; i++)
			{
				MDistributionRunDetail detail = m_details[i];
				if (runLine.getM_DistributionRunLine_ID() == detail.getM_DistributionRunLine_ID())
				{
					if (detail.isCanAdjust())
						ratioTotal = ratioTotal.add(detail.getRatio());
				}
			}
			if (ratioTotal.compareTo(Env.ZERO) == 0){
				StringBuilder msgexc = new StringBuilder("Cannot distribute Difference = ").append(difference) 
						.append(" - You need to change Total Qty or Min Qty");
				throw new Exception (msgexc.toString());
			}	
			//	Distribute
			for (int i = 0; i < m_details.length; i++)
			{
				MDistributionRunDetail detail = m_details[i];
				if (runLine.getM_DistributionRunLine_ID() == detail.getM_DistributionRunLine_ID())
				{
					if (detail.isCanAdjust())
					{
						BigDecimal diffRatio = detail.getRatio().multiply(difference)
							.divide(ratioTotal, RoundingMode.HALF_UP);	// precision from total
						if (log.isLoggable(Level.FINE)) log.fine("Detail=" + detail.toString()
							+ ", Allocation=" + detail.getActualAllocation()
							+ ", DiffRatio=" + diffRatio);
						detail.adjustQty(diffRatio);
						detail.saveEx();
					}
				}
			}
		}
		runLine.setLastDifference(difference);
	}	//	adjustAllocation

	
	/**************************************************************************
	 * 	Create Orders
	 * 	@return true if created
	 */
	private boolean createOrders()
	{
		//	Get Counter Org/BP
		int runAD_Org_ID = m_run.getAD_Org_ID();
		if (runAD_Org_ID == 0)
			runAD_Org_ID = Env.getAD_Org_ID(getCtx());
		MOrg runOrg = MOrg.get(getCtx(), runAD_Org_ID);
		int runC_BPartner_ID = runOrg.getLinkedC_BPartner_ID(get_TrxName());
		boolean counter = !m_run.isCreateSingleOrder()	//	no single Order 
			&& runC_BPartner_ID > 0						//	Org linked to BP
			&& !m_docType.isSOTrx();					//	PO
		MBPartner runBPartner = counter ? new MBPartner(getCtx(), runC_BPartner_ID, get_TrxName()) : null;
		if (!counter || runBPartner == null || runBPartner.get_ID() != runC_BPartner_ID)
			counter = false;
		if (counter)
			if (log.isLoggable(Level.INFO)) log.info("RunBP=" + runBPartner
				+ " - " + m_docType);
		if (log.isLoggable(Level.INFO)) log.info("Single=" + m_run.isCreateSingleOrder()
			+ " - " + m_docType + ",SO=" + m_docType.isSOTrx());
		if (log.isLoggable(Level.FINE)) log.fine("Counter=" + counter 
			+ ",C_BPartner_ID=" + runC_BPartner_ID + "," + runBPartner);
		//
		MBPartner bp = null;
		MOrder singleOrder = null;
		MProduct product = null;
		//	Consolidated Order
		if (m_run.isCreateSingleOrder())
		{
			bp = new MBPartner (getCtx(), m_run.getC_BPartner_ID(), get_TrxName());
			if (bp.get_ID() == 0)
				throw new IllegalArgumentException("Business Partner not found - C_BPartner_ID=" + m_run.getC_BPartner_ID());
			//
			if (!p_IsTest)
			{
				singleOrder = new MOrder (getCtx(), 0, get_TrxName());
				singleOrder.setC_DocTypeTarget_ID(m_docType.getC_DocType_ID());
				singleOrder.setC_DocType_ID(m_docType.getC_DocType_ID());
				singleOrder.setIsSOTrx(m_docType.isSOTrx());
				singleOrder.setBPartner(bp);
				if (m_run.getC_BPartner_Location_ID() != 0)
					singleOrder.setC_BPartner_Location_ID(m_run.getC_BPartner_Location_ID());
				singleOrder.setDateOrdered(m_DateOrdered);
				singleOrder.setDatePromised(p_DatePromised);
				if (!singleOrder.save())
				{
					log.log(Level.SEVERE, "Order not saved");
					return false;
				}
				m_counter++;
			}
		}

		int lastC_BPartner_ID = 0;
		int lastC_BPartner_Location_ID = 0;
		MOrder order = null;
		//	For all lines
		for (int i = 0; i < m_details.length; i++)
		{
			MDistributionRunDetail detail = m_details[i];
			
			//	Create Order Header
			if (m_run.isCreateSingleOrder())
				order = singleOrder;
			//	New Business Partner
			else if (lastC_BPartner_ID != detail.getC_BPartner_ID()
				|| lastC_BPartner_Location_ID != detail.getC_BPartner_Location_ID())
			{
				//	finish order
				order = null;
			}
			lastC_BPartner_ID = detail.getC_BPartner_ID();
			lastC_BPartner_Location_ID = detail.getC_BPartner_Location_ID();
			
			//	New Order
			if (order == null)
			{
				bp = new MBPartner (getCtx(), detail.getC_BPartner_ID(), get_TrxName());
				if (!p_IsTest)
				{
					order = new MOrder (getCtx(), 0, get_TrxName());
					order.setC_DocTypeTarget_ID(m_docType.getC_DocType_ID());
					order.setC_DocType_ID(m_docType.getC_DocType_ID());
					order.setIsSOTrx(m_docType.isSOTrx());
					//	Counter Doc
					if (counter && bp.getAD_OrgBP_ID() > 0)
					{
						if (log.isLoggable(Level.FINE)) log.fine("Counter - From_BPOrg=" + bp.getAD_OrgBP_ID() 
							+ "-" + bp + ", To_BP=" + runBPartner);
						order.setAD_Org_ID(bp.getAD_OrgBP_ID());
						MOrgInfo oi = MOrgInfo.get(getCtx(), bp.getAD_OrgBP_ID(), get_TrxName());
						if (oi.getM_Warehouse_ID() > 0)
							order.setM_Warehouse_ID(oi.getM_Warehouse_ID());
						order.setBPartner(runBPartner);
					}
					else	//	normal
					{
						if (log.isLoggable(Level.FINE)) log.fine("From_Org=" + runAD_Org_ID 
							+ ", To_BP=" + bp);
						order.setAD_Org_ID(runAD_Org_ID);
						order.setBPartner(bp);
						if (detail.getC_BPartner_Location_ID() != 0)
							order.setC_BPartner_Location_ID(detail.getC_BPartner_Location_ID());
					}
					order.setDateOrdered(m_DateOrdered);
					order.setDatePromised(p_DatePromised);
					if (!order.save())
					{
						log.log(Level.SEVERE, "Order not saved");
						return false;
					}
				}
			}
			
			//	Line
			if (product == null || product.getM_Product_ID() != detail.getM_Product_ID())
				product = MProduct.get (getCtx(), detail.getM_Product_ID());
			if (p_IsTest)
			{
				StringBuilder msglog = new StringBuilder().append(bp.getName()).append(" - ").append(product.getName());
				addLog(0,null, detail.getActualAllocation(), msglog.toString());
				continue;
			}

			//	Create Order Line
			MOrderLine line = new MOrderLine(order);
			if (counter && bp.getAD_OrgBP_ID() > 0)
				;	//	don't overwrite counter doc
			else	//	normal - optionally overwrite
			{
				line.setC_BPartner_ID(detail.getC_BPartner_ID());
				if (detail.getC_BPartner_Location_ID() != 0)
					line.setC_BPartner_Location_ID(detail.getC_BPartner_Location_ID());
			}
			//
			line.setProduct(product);
			line.setQty(detail.getActualAllocation());
			line.setPrice();
			if (!line.save())
			{
				log.log(Level.SEVERE, "OrderLine not saved");
				return false;
			}
			StringBuilder msglog = new StringBuilder().append(order.getDocumentNo()).append(": ").append(bp.getName()).append(" - ").append(product.getName());
			addLog(0,null, detail.getActualAllocation(), msglog.toString());
		}
		//	finish order
		order = null;

		
		return true;
	}	//	createOrders
	
	
	/**
	 * 	Insert Details
	 *	@return number of rows inserted
	 */
	private int insertDetailsDistributionDemand()
	{
		//	Handle NULL
		StringBuilder sql = new StringBuilder("UPDATE M_DistributionRunLine SET MinQty = 0 WHERE MinQty IS NULL");
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		
		sql = new StringBuilder("UPDATE M_DistributionListLine SET MinQty = 0 WHERE MinQty IS NULL");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		
		//	Delete Old
		sql = new StringBuilder("DELETE FROM T_DistributionRunDetail WHERE M_DistributionRun_ID=")
			.append(p_M_DistributionRun_ID);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("insertDetails - deleted #" + no);

		//	Insert New
		sql = new StringBuilder("INSERT INTO T_DistributionRunDetail ")
			.append("(M_DistributionRun_ID, M_DistributionRunLine_ID, M_DistributionList_ID, M_DistributionListLine_ID,")
			.append("AD_Client_ID,AD_Org_ID, IsActive, Created,CreatedBy, Updated,UpdatedBy,")
			.append("C_BPartner_ID, C_BPartner_Location_ID, M_Product_ID,")
			.append("Ratio, MinQty, Qty) "			)
			.append("SELECT MAX(rl.M_DistributionRun_ID), MAX(rl.M_DistributionRunLine_ID),MAX(ll.M_DistributionList_ID), MAX(ll.M_DistributionListLine_ID), ")
			.append("MAX(rl.AD_Client_ID),MAX(rl.AD_Org_ID), MAX(rl.IsActive), MAX(rl.Created),MAX(rl.CreatedBy), MAX(rl.Updated),MAX(rl.UpdatedBy), ")
			.append("MAX(ll.C_BPartner_ID), MAX(ll.C_BPartner_Location_ID), MAX(rl.M_Product_ID),")
			// Ration for this process is equal QtyToDeliver
			.append("COALESCE (SUM(ol.QtyOrdered-ol.QtyDelivered-TargetQty), 0) , ")
			// Min Qty for this process is equal to TargetQty
			.append(" 0 , 0 FROM M_DistributionRunLine rl ")
			.append("INNER JOIN M_DistributionList l ON (rl.M_DistributionList_ID=l.M_DistributionList_ID) ")
			.append("INNER JOIN M_DistributionListLine ll ON (rl.M_DistributionList_ID=ll.M_DistributionList_ID) ")
			.append("INNER JOIN DD_Order o ON (o.C_BPartner_ID=ll.C_BPartner_ID AND o.DocStatus IN ('DR','IN')) ")
			.append("INNER JOIN DD_OrderLine ol ON (ol.DD_Order_ID=o.DD_Order_ID AND ol.M_Product_ID=rl.M_Product_ID) ")		
			.append("INNER JOIN M_Locator loc ON (loc.M_Locator_ID=ol.M_Locator_ID AND loc.M_Warehouse_ID=").append(p_M_Warehouse_ID).append(") ")
			.append("WHERE rl.M_DistributionRun_ID=").append(p_M_DistributionRun_ID).append(" AND rl.IsActive='Y' AND ll.IsActive='Y' AND ol.DatePromised <= ").append(DB.TO_DATE(p_DatePromised))
			.append(" GROUP BY o.M_Shipper_ID , ll.C_BPartner_ID, ol.M_Product_ID");
			//+ " BETWEEN "+ DB.TO_DATE(p_DatePromised)  +" AND "+ DB.TO_DATE(p_DatePromised_To) 	
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			
			List<MDistributionRunDetail> records = new Query(getCtx(), 
												   MDistributionRunDetail.Table_Name,
												   MDistributionRunDetail.COLUMNNAME_M_DistributionRun_ID + "=?",												  
												   get_TrxName()).setParameters( new Object[]{p_M_DistributionRun_ID}).list();
			
			for(MDistributionRunDetail record : records)
			{
				
					MDistributionRunLine drl = (MDistributionRunLine) MTable.get(getCtx(), MDistributionRunLine.Table_ID).getPO(record.getM_DistributionRunLine_ID(), get_TrxName());
					MProduct product = MProduct.get(getCtx(), record.getM_Product_ID());					
					BigDecimal ration = record.getRatio();
					BigDecimal totalration = getQtyDemand(record.getM_Product_ID());
					if (log.isLoggable(Level.INFO)){
						log.info("Value:" + product.getValue());
						log.info("Product:" + product.getName());
						log.info("Qty To Deliver:" + record.getRatio());
						log.info("Qty Target:" + record.getMinQty());
						log.info("Qty Total Available:" + drl.getTotalQty());
						log.info("Qty Total Demand:" +  totalration);
					}			
					BigDecimal factor = ration.divide(totalration, 12 , RoundingMode.HALF_UP);
					record.setQty(drl.getTotalQty().multiply(factor));
					record.saveEx();
			}			
		if (log.isLoggable(Level.FINE)) log.fine("inserted #" + no);
		return no;
	}	//	insertDetails
	
	private BigDecimal getQtyDemand(int M_Product_ID)
	{
		String sql = "SELECT SUM (QtyOrdered-QtyDelivered-TargetQty) " +
				"FROM DD_OrderLine ol " +
				"INNER JOIN M_Locator l ON (l.M_Locator_ID=ol.M_Locator_ID) " +
				"INNER JOIN DD_Order o ON (o.DD_Order_ID=ol.DD_Order_ID) " +
				" WHERE o.DocStatus IN ('DR','IN') " +
				"AND ol.DatePromised <= ? " +
				"AND l.M_Warehouse_ID=? " +
				"AND ol.M_Product_ID=? " +
				"GROUP BY M_Product_ID, l.M_Warehouse_ID";
 	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
 	    try
 	    {
 	            pstmt = DB.prepareStatement (sql, get_TrxName());
 	    		pstmt.setTimestamp(1, p_DatePromised);
 	    		pstmt.setInt(2, p_M_Warehouse_ID);
 	    		pstmt.setInt(3, M_Product_ID);
 	    		
 	            rs = pstmt.executeQuery();
 	            while (rs.next())
 	            {
 	            	return rs.getBigDecimal(1);
 	            }
 		}
 	    catch (Exception e)
 		{
 	            	log.log(Level.SEVERE,"doIt - " + sql, e);
 	                return Env.ZERO;
 		}
 		finally
 		{
 			DB.close(rs, pstmt);
 			rs = null;
 			pstmt = null;
 		}	    
    	 
    	return Env.ZERO; 
     }
	
	
	
	/**
	 * 	Insert Details
	 *	@return number of rows inserted
	 */
	private int insertDetailsDistribution()
	{
		//	Handle NULL
		StringBuilder sql = new StringBuilder("UPDATE M_DistributionRunLine SET MinQty = 0 WHERE MinQty IS NULL");
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		
		sql = new StringBuilder("UPDATE M_DistributionListLine SET MinQty = 0 WHERE MinQty IS NULL");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		
		//	Delete Old
		sql = new StringBuilder("DELETE FROM T_DistributionRunDetail WHERE M_DistributionRun_ID=")
			.append(p_M_DistributionRun_ID);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("insertDetails - deleted #" + no);
		
		//	Insert New
		sql = new StringBuilder("INSERT INTO T_DistributionRunDetail ")
			.append("(M_DistributionRun_ID, M_DistributionRunLine_ID, M_DistributionList_ID, M_DistributionListLine_ID,")
			.append("AD_Client_ID,AD_Org_ID, IsActive, Created,CreatedBy, Updated,UpdatedBy,")
			.append("C_BPartner_ID, C_BPartner_Location_ID, M_Product_ID,")
			.append("Ratio, MinQty, Qty) ")
			.append("SELECT rl.M_DistributionRun_ID, rl.M_DistributionRunLine_ID,ll.M_DistributionList_ID, ll.M_DistributionListLine_ID, ")
			.append("rl.AD_Client_ID,rl.AD_Org_ID, rl.IsActive, rl.Created,rl.CreatedBy, rl.Updated,rl.UpdatedBy, ")
			.append("ll.C_BPartner_ID, ll.C_BPartner_Location_ID, rl.M_Product_ID, 0 , ")
			.append("ol.TargetQty AS MinQty , 0 FROM M_DistributionRunLine rl ")
			.append("INNER JOIN M_DistributionList l ON (rl.M_DistributionList_ID=l.M_DistributionList_ID) ")
			.append("INNER JOIN M_DistributionListLine ll ON (rl.M_DistributionList_ID=ll.M_DistributionList_ID) ")
			.append("INNER JOIN DD_Order o ON (o.C_BPartner_ID=ll.C_BPartner_ID) ")
			.append("INNER JOIN DD_OrderLine ol ON (ol.DD_Order_ID=o.DD_Order_ID AND ol.M_Product_ID=rl.M_Product_ID) AND ol.DatePromised")
			.append( "<=").append(DB.TO_DATE(p_DatePromised))
			.append(" INNER JOIN M_Locator loc ON (loc.M_Locator_ID=ol.M_Locator_ID AND loc.M_Warehouse_ID=").append(p_M_Warehouse_ID).append(") ")
			.append(" WHERE rl.M_DistributionRun_ID=").append(p_M_DistributionRun_ID).append(" AND l.RatioTotal<>0 AND rl.IsActive='Y' AND ll.IsActive='Y'");	
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			
			Query query = MTable.get(getCtx(), I_T_DistributionRunDetail.Table_ID).
			createQuery(MDistributionRunDetail.COLUMNNAME_M_DistributionRun_ID + "=?", get_TrxName());
			query.setParameters(p_M_DistributionRun_ID);
			
			List<MDistributionRunDetail> records = query.list();
			
			for(MDistributionRunDetail record : records)
			{
					BigDecimal total_ration = DB.getSQLValueBD(get_TrxName(), 
					"SELECT SUM(Ratio) FROM T_DistributionRunDetail WHERE M_DistributionRun_ID=? AND M_Product_ID=? GROUP BY  M_Product_ID"
					, p_M_DistributionRun_ID, record.getM_Product_ID());
					MDistributionRunLine drl = (MDistributionRunLine) MTable.get(getCtx(), MDistributionRunLine.Table_ID).getPO(record.getM_DistributionRunLine_ID(), get_TrxName());
					BigDecimal ration = record.getRatio();
					BigDecimal factor = ration.divide(total_ration,RoundingMode.HALF_UP);
					record.setQty(factor.multiply(drl.getTotalQty()));
					record.saveEx();
			}			
		if (log.isLoggable(Level.FINE)) log.fine("inserted #" + no);
		return no;
	}	//	insertDetails
	
	
	/**************************************************************************
	 * 	Create Orders
	 * 	@return true if created
	 */
	private boolean distributionOrders()
	{
		//The Quantity Available is distribute with respect to Distribution Order Demand
		if (p_BasedInDamnd)	
		{	
			int M_Warehouse_ID = 0;
			if (p_M_Warehouse_ID <= 0)
			{
				MOrgInfo oi_source = MOrgInfo.get(getCtx(), m_run.getAD_Org_ID(), get_TrxName());
				MWarehouse m_source = MWarehouse.get(getCtx(), oi_source.getM_Warehouse_ID());
				if(m_source == null)
					throw new AdempiereException("Do not exist Defautl Warehouse Source");
				M_Warehouse_ID = m_source.getM_Warehouse_ID();
			}
			else 
				M_Warehouse_ID = p_M_Warehouse_ID;
			
			//			For all lines
			for (int i = 0; i < m_details.length; i++)
			{
				MDistributionRunDetail detail = m_details[i];
				
				String sql = "SELECT * FROM DD_OrderLine ol INNER JOIN DD_Order o ON (o.DD_Order_ID=ol.DD_Order_ID)  INNER JOIN M_Locator l ON (l.M_Locator_ID=ol.M_Locator_ID) ";
				//sql.append(" WHERE o.DocStatus IN ('DR','IN') AND o.C_BPartner_ID = ? AND M_Product_ID=? AND  l.M_Warehouse_ID=?  AND ol.DatePromised BETWEEN ? AND ? ");
				sql = sql + " WHERE o.DocStatus IN ('DR','IN') AND o.C_BPartner_ID = ? AND M_Product_ID=? AND  l.M_Warehouse_ID=?  AND ol.DatePromised <=?";
				
		 	    PreparedStatement pstmt = null;
			    ResultSet rs = null; 
		 	    try
		 	    {
		 	    		pstmt = DB.prepareStatement (sql.toString(),get_TrxName());
		 	    		pstmt.setInt(1, detail.getC_BPartner_ID());
		 	    		pstmt.setInt(2, detail.getM_Product_ID());
		 	    		pstmt.setInt(3, M_Warehouse_ID);
		 	    		pstmt.setTimestamp(4, p_DatePromised);
		 	    		//pstmt.setTimestamp(5, p_DatePromised_To);

		 	            rs = pstmt.executeQuery();
		 	            while (rs.next())
		 	            {           	
			 	   			//	Create Order Line
			 	   			MDDOrderLine line = new MDDOrderLine(getCtx(), rs , get_TrxName());
			 	   			line.setM_Product_ID(detail.getM_Product_ID());
			 	   			line.setConfirmedQty(line.getTargetQty().add(detail.getActualAllocation()));
			 	   			if(p_M_Warehouse_ID>0)
			 	   			line.setDescription(Msg.translate(getCtx(), "PlannedQty"));
			 	   			else 
			 	   			line.setDescription(m_run.getName());
			 	   			line.saveEx();
			 	   			break;
		 	            }
	 	           
		 		}
		 	    catch (Exception e)
		 		{
		 	            	log.log(Level.SEVERE,"doIt - " + sql, e);
		 	                return false;
		 		}
		 		finally
		 		{
		 			DB.close(rs, pstmt);
		 			rs = null;
		 			pstmt = null;
		 		}	
			}	
			return true;
		}
		
		//		Get Counter Org/BP
		int runAD_Org_ID = m_run.getAD_Org_ID();
		if (runAD_Org_ID == 0)
			runAD_Org_ID = Env.getAD_Org_ID(getCtx());
		MOrg runOrg = MOrg.get(getCtx(), runAD_Org_ID);
		int runC_BPartner_ID = runOrg.getLinkedC_BPartner_ID(get_TrxName());
		boolean counter = !m_run.isCreateSingleOrder()	//	no single Order 
			&& runC_BPartner_ID > 0						//	Org linked to BP
			&& !m_docType.isSOTrx();					//	PO
		MBPartner runBPartner = counter ? new MBPartner(getCtx(), runC_BPartner_ID, get_TrxName()) : null;
		if (!counter || runBPartner == null || runBPartner.get_ID() != runC_BPartner_ID)
			counter = false;
		if (counter)
			if (log.isLoggable(Level.INFO)) log.info("RunBP=" + runBPartner
				+ " - " + m_docType);
		if (log.isLoggable(Level.INFO)) log.info("Single=" + m_run.isCreateSingleOrder()
			+ " - " + m_docType + ",SO=" + m_docType.isSOTrx());
		if (log.isLoggable(Level.FINE)) log.fine("Counter=" + counter 
			+ ",C_BPartner_ID=" + runC_BPartner_ID + "," + runBPartner);
		//
		MBPartner bp = null;
		MDDOrder singleOrder = null;
		MProduct product = null;
		
		MWarehouse 	 m_source = null;
		MLocator m_locator= null ;
		MWarehouse  m_target= null;
		MLocator m_locator_to= null;
		MWarehouse[] ws = null;
		
		MOrgInfo oi_source = MOrgInfo.get(getCtx(), m_run.getAD_Org_ID(), get_TrxName());
		m_source = MWarehouse.get(getCtx(), oi_source.getM_Warehouse_ID());
		if(m_source == null)
			throw new AdempiereException("Do not exist Defautl Warehouse Source");
		
		m_locator =  MLocator.getDefault(m_source);
		
		//get the warehouse in transit
		ws = MWarehouse.getInTransitForOrg(getCtx(), m_source.getAD_Org_ID());
		
		if(ws==null)
			throw new AdempiereException("Warehouse Intransit do not found");
		
		
		//	Consolidated Single Order 
		if (m_run.isCreateSingleOrder())
		{
			bp = new MBPartner (getCtx(), m_run.getC_BPartner_ID(), get_TrxName());
			if (bp.get_ID() == 0)
				throw new IllegalArgumentException("Business Partner not found - C_BPartner_ID=" + m_run.getC_BPartner_ID());
			//
			if (!p_IsTest)
			{
				singleOrder = new MDDOrder (getCtx(), 0, get_TrxName());
				singleOrder.setC_DocType_ID(m_docType.getC_DocType_ID());
				singleOrder.setIsSOTrx(m_docType.isSOTrx());
				singleOrder.setBPartner(bp);
				if (m_run.getC_BPartner_Location_ID() != 0)
					singleOrder.setC_BPartner_Location_ID(m_run.getC_BPartner_Location_ID());
				singleOrder.setDateOrdered(m_DateOrdered);
				singleOrder.setDatePromised(p_DatePromised);
				singleOrder.setM_Warehouse_ID(ws[0].getM_Warehouse_ID());
				if (!singleOrder.save())
				{
					log.log(Level.SEVERE, "Order not saved");
					return false;
				}
				m_counter++;
			}
		}
		
		int lastC_BPartner_ID = 0;
		int lastC_BPartner_Location_ID = 0;
		MDDOrder order = null;

		
		//	For all lines
		for (int i = 0; i < m_details.length; i++)
		{
			MDistributionRunDetail detail = m_details[i];
			
			//	Create Order Header
			if (m_run.isCreateSingleOrder())
				order = singleOrder;
			//	New Business Partner
			else if (lastC_BPartner_ID != detail.getC_BPartner_ID()
				|| lastC_BPartner_Location_ID != detail.getC_BPartner_Location_ID())
			{
				//	finish order
				order = null;
			}
			lastC_BPartner_ID = detail.getC_BPartner_ID();
			lastC_BPartner_Location_ID = detail.getC_BPartner_Location_ID();
			
			bp = new MBPartner (getCtx(), detail.getC_BPartner_ID(), get_TrxName());
			MOrgInfo oi_target = MOrgInfo.get(getCtx(), bp.getAD_OrgBP_ID(), get_TrxName());
			m_target = MWarehouse.get(getCtx(), oi_target.getM_Warehouse_ID());
			if(m_target==null)
				throw new AdempiereException("Do not exist Default Warehouse Target");
			
			m_locator_to = MLocator.getDefault(m_target); 

			if (m_locator == null || m_locator_to == null)
			{
				throw new AdempiereException("Do not exist default Locator for Warehouses");
			}
			
			if(p_ConsolidateDocument)
			{
				
				StringBuilder whereClause = new StringBuilder("DocStatus IN ('DR','IN') AND AD_Org_ID=").append(bp.getAD_OrgBP_ID()).append(" AND ")	
									    .append(MDDOrder.COLUMNNAME_C_BPartner_ID).append("=? AND ")
									    .append(MDDOrder.COLUMNNAME_M_Warehouse_ID).append("=?  AND ")
									    .append(MDDOrder.COLUMNNAME_DatePromised).append("<=? ");
				
				order = new Query(getCtx(), MDDOrder.Table_Name, whereClause.toString(), get_TrxName())
							.setParameters(new Object[]{lastC_BPartner_ID, ws[0].getM_Warehouse_ID(), p_DatePromised})
							.setOrderBy(MDDOrder.COLUMNNAME_DatePromised +" DESC")
							.first();
		}
			
			//	New Order
			if (order == null)
			{
				if (!p_IsTest)
				{
					order = new MDDOrder (getCtx(), 0, get_TrxName());
					order.setAD_Org_ID(bp.getAD_OrgBP_ID());
					order.setC_DocType_ID(m_docType.getC_DocType_ID());
					order.setIsSOTrx(m_docType.isSOTrx());					

					//	Counter Doc
					if (counter && bp.getAD_OrgBP_ID() > 0)
					{
						if (log.isLoggable(Level.FINE)) log.fine("Counter - From_BPOrg=" + bp.getAD_OrgBP_ID() 
							+ "-" + bp + ", To_BP=" + runBPartner);
						order.setAD_Org_ID(bp.getAD_OrgBP_ID());
						if (ws[0].getM_Warehouse_ID() > 0)
						order.setM_Warehouse_ID(ws[0].getM_Warehouse_ID());
						order.setBPartner(runBPartner);
					}
					else	//	normal
					{
						if (log.isLoggable(Level.FINE)) log.fine("From_Org=" + runAD_Org_ID 
							+ ", To_BP=" + bp);
						order.setAD_Org_ID(bp.getAD_OrgBP_ID());
						order.setBPartner(bp);
						if (detail.getC_BPartner_Location_ID() != 0)
							order.setC_BPartner_Location_ID(detail.getC_BPartner_Location_ID());
					}
					order.setM_Warehouse_ID(ws[0].getM_Warehouse_ID());
					order.setDateOrdered(m_DateOrdered);
					order.setDatePromised(p_DatePromised);
					order.setIsInDispute(false);
					order.setIsInTransit(false);
					if (!order.save())
					{
						log.log(Level.SEVERE, "Order not saved");
						return false;
					}
				}
			}
			
			//	Line
			if (product == null || product.getM_Product_ID() != detail.getM_Product_ID())
				product = MProduct.get (getCtx(), detail.getM_Product_ID());
			if (p_IsTest)
			{
				StringBuilder msglog = new StringBuilder().append(bp.getName()).append(" - ").append(product.getName());
				addLog(0,null, detail.getActualAllocation(), msglog.toString());
				continue;
			}

			if(p_ConsolidateDocument)
			{

				String sql = "SELECT DD_OrderLine_ID FROM DD_OrderLine ol INNER JOIN DD_Order o ON (o.DD_Order_ID=ol.DD_Order_ID) WHERE o.DocStatus IN ('DR','IN') AND o.C_BPartner_ID = ? AND M_Product_ID=? AND  ol.M_Locator_ID=?  AND ol.DatePromised <= ?";				
				int DD_OrderLine_ID = DB.getSQLValueEx(get_TrxName(), sql, new Object[]{detail.getC_BPartner_ID(),product.getM_Product_ID(), m_locator.getM_Locator_ID(), p_DatePromised});	
				if (DD_OrderLine_ID  <= 0)
				{	
					MDDOrderLine line = new MDDOrderLine(order);
					line.setAD_Org_ID(bp.getAD_OrgBP_ID());
					line.setM_Locator_ID(m_locator.getM_Locator_ID());
					line.setM_LocatorTo_ID(m_locator_to.getM_Locator_ID());
					line.setIsInvoiced(false);
					line.setProduct(product);
					BigDecimal QtyAllocation = detail.getActualAllocation();
					if(QtyAllocation == null)
						QtyAllocation = Env.ZERO;
					
					line.setQty(QtyAllocation);
					line.setQtyEntered(QtyAllocation);
					line.setTargetQty(Env.ZERO);
					String Description ="";
					if (m_run.getName() != null)
						Description =Description.concat(m_run.getName());
					StringBuilder msgline = new StringBuilder(Description).append(" ").append(Msg.translate(getCtx(), "Qty"))
							.append(" = ").append(QtyAllocation).append(" ");
					line.setDescription(msgline.toString());
					line.saveEx();
				}
				else 
				{
					MDDOrderLine line = new MDDOrderLine(getCtx(), DD_OrderLine_ID, get_TrxName());		
					BigDecimal QtyAllocation = detail.getActualAllocation();
					if(QtyAllocation == null)
						QtyAllocation = Env.ZERO;
					String Description = line.getDescription();
					if (Description ==  null)
						Description ="";
					if (m_run.getName() != null)
						Description =Description.concat(m_run.getName());
					StringBuilder msgline = new StringBuilder(Description).append(" ").append(Msg.translate(getCtx(), "Qty")).append(" = ").append(QtyAllocation).append(" ");
					line.setDescription(msgline.toString());
					line.setQty(line.getQtyEntered().add(QtyAllocation));
					line.saveEx();
				}
			}
			else
			{	
				//	Create Order Line
				MDDOrderLine line = new MDDOrderLine(order);
				if (counter && bp.getAD_OrgBP_ID() > 0)
					;	//	don't overwrite counter doc
				//
				line.setAD_Org_ID(bp.getAD_OrgBP_ID());
				line.setM_Locator_ID(m_locator.getM_Locator_ID());
				line.setM_LocatorTo_ID(m_locator_to.getM_Locator_ID());
				line.setIsInvoiced(false);
				line.setProduct(product);
				line.setQty(detail.getActualAllocation());
				line.setQtyEntered(detail.getActualAllocation());
				line.setTargetQty(Env.ZERO);
				String Description ="";
				if (m_run.getName() != null)
					Description =Description.concat(m_run.getName());
				StringBuilder msgline = new StringBuilder(Description).append(" ").append(Msg.translate(getCtx(), "Qty")).append(" = ")
							.append(detail.getActualAllocation()).append(" ");
				line.setDescription(msgline.toString());
				line.saveEx();
				
			}	
			StringBuilder msglog = new StringBuilder().append(order.getDocumentNo()) 
					.append(": ").append(bp.getName()).append(" - ").append(product.getName());
			addLog(0,null, detail.getActualAllocation(), msglog.toString());
		}
		//	finish order
		order = null;
		return true;
	}
	
}	//	DistributionRun
