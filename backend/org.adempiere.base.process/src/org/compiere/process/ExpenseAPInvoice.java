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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MPriceList;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 *	Create AP Invoices from Expense Reports
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: ExpenseAPInvoice.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
@org.adempiere.base.annotation.Process
public class ExpenseAPInvoice extends SvrProcess
{
	private int			m_C_BPartner_ID = 0;
	private Timestamp	m_DateFrom = null;
	private Timestamp	m_DateTo = null;
	private int			m_noInvoices = 0;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null && para[i].getParameter_To() == null)
				;
			else if (name.equals("C_BPartner_ID"))
				m_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("DateReport"))
			{
				m_DateFrom = (Timestamp)para[i].getParameter();
				m_DateTo = (Timestamp)para[i].getParameter_To();
			}
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws java.lang.Exception
	{
		StringBuilder sql = new StringBuilder ("SELECT * ")
			.append("FROM S_TimeExpense e ")
			.append("WHERE e.Processed='Y'")
			.append(" AND e.AD_Client_ID=?");				//	#1
		if (m_C_BPartner_ID != 0)
			sql.append(" AND e.C_BPartner_ID=?");	//	#2
		if (m_DateFrom != null)
			sql.append(" AND e.DateReport >= ?");	//	#3
		if (m_DateTo != null)
			sql.append(" AND e.DateReport <= ?");	//	#4
		sql.append(" AND EXISTS (SELECT * FROM S_TimeExpenseLine el ")
			.append("WHERE e.S_TimeExpense_ID=el.S_TimeExpense_ID")
			.append(" AND el.C_InvoiceLine_ID IS NULL")
			.append(" AND el.ConvertedAmt<>0) ")
			.append("ORDER BY e.C_BPartner_ID, e.S_TimeExpense_ID");
		//
		int old_BPartner_ID = -1;
		MInvoice invoice = null;
		//		
		try (PreparedStatement pstmt = DB.prepareStatement (sql.toString (), get_TrxName());)
		{			
			int par = 1;
			pstmt.setInt(par++, getAD_Client_ID());
			if (m_C_BPartner_ID != 0)
				pstmt.setInt (par++, m_C_BPartner_ID);
			if (m_DateFrom != null)
				pstmt.setTimestamp (par++, m_DateFrom);
			if (m_DateTo != null)
				pstmt.setTimestamp (par++, m_DateTo);
			ResultSet rs = pstmt.executeQuery ();
			while (rs.next())				//	********* Expense Line Loop
			{
				MTimeExpense te = new MTimeExpense (getCtx(), rs, get_TrxName());

				//	New BPartner - New Order
				if (te.getC_BPartner_ID() != old_BPartner_ID)
				{
					completeInvoice (invoice);
					MBPartner bp = new MBPartner (getCtx(), te.getC_BPartner_ID(), get_TrxName());
					//
					if (log.isLoggable(Level.INFO)) log.info("New Invoice for " + bp);
					invoice = new MInvoice (getCtx(), 0, get_TrxName());
					invoice.setClientOrg(te.getAD_Client_ID(), te.getAD_Org_ID());
					invoice.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_APInvoice);	//	API
					invoice.setDocumentNo (te.getDocumentNo());
					//
					invoice.setBPartner(bp);
					if (invoice.getC_BPartner_Location_ID() == 0)
					{
						StringBuilder msglog = new StringBuilder("No BP Location: ").append(bp);
						log.log(Level.SEVERE, msglog.toString());
						msglog = new StringBuilder("No Location: ").append(te.getDocumentNo()).append(" ").append(bp.getName());
						addLog(0, te.getDateReport(), null, msglog.toString() );
						invoice = null;
						break;
					}
					invoice.setM_PriceList_ID(te.getM_PriceList_ID());
					
					MPriceList pl = MPriceList.get(getCtx(), te.getM_PriceList_ID(), get_TrxName());
					invoice.setIsTaxIncluded(pl.isTaxIncluded());

					invoice.setSalesRep_ID(te.getDoc_User_ID());
					StringBuilder descr = new StringBuilder().append(Msg.translate(getCtx(), "S_TimeExpense_ID")) 
						.append(": ").append(te.getDocumentNo()).append(" " )
						.append(DisplayType.getDateFormat(DisplayType.Date).format(te.getDateReport()));  
					invoice.setDescription(descr.toString());
					if (!invoice.save())
						throw new IllegalStateException("Cannot save Invoice");
					old_BPartner_ID = bp.getC_BPartner_ID();
				}
				MTimeExpenseLine[] tel = te.getLines(false);
				for (int i = 0; i < tel.length; i++)
				{
					MTimeExpenseLine line = tel[i];
					
					//	Already Invoiced or nothing to be reimbursed
					if (line.getC_InvoiceLine_ID() != 0
						|| Env.ZERO.compareTo(line.getQtyReimbursed()) == 0
						|| Env.ZERO.compareTo(line.getPriceReimbursed()) == 0)
						continue;

					//	Update Header info
					if (line.getC_Activity_ID() != 0 && line.getC_Activity_ID() != invoice.getC_Activity_ID())
						invoice.setC_Activity_ID(line.getC_Activity_ID());
					if (line.getC_Campaign_ID() != 0 && line.getC_Campaign_ID() != invoice.getC_Campaign_ID())
						invoice.setC_Campaign_ID(line.getC_Campaign_ID());
					if (line.getC_Project_ID() != 0 && line.getC_Project_ID() != invoice.getC_Project_ID())
						invoice.setC_Project_ID(line.getC_Project_ID());
					if (!invoice.save())
						throw new IllegalStateException("Cannot save Invoice");
					
					//	Create OrderLine
					MInvoiceLine il = new MInvoiceLine (invoice);
					//
					if (line.getM_Product_ID() != 0)
						il.setM_Product_ID(line.getM_Product_ID(), true);
					il.setQty(line.getQtyReimbursed());		//	Entered/Invoiced
					il.setDescription(line.getDescription());
					//
					il.setC_Project_ID(line.getC_Project_ID());
					il.setC_ProjectPhase_ID(line.getC_ProjectPhase_ID());
					il.setC_ProjectTask_ID(line.getC_ProjectTask_ID());
					il.setC_Activity_ID(line.getC_Activity_ID());
					il.setC_Campaign_ID(line.getC_Campaign_ID());
					//
					il.setPrice(line.getPriceReimbursed());	//
					
					il.setTax();
					if (!il.save())
						throw new IllegalStateException("Cannot save Invoice Line");
					//	Update TEL
					line.setC_InvoiceLine_ID(il.getC_InvoiceLine_ID());
					line.saveEx();
				}	//	for all expense lines
			}								//	********* Expense Line Loop
 		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		completeInvoice (invoice);
		StringBuilder msgreturn = new StringBuilder("@Created@=").append(m_noInvoices);
		return msgreturn.toString();
	}	//	doIt

	/**
	 * 	Complete Invoice
	 *	@param invoice invoice
	 */
	private void completeInvoice (MInvoice invoice)
	{
		if (invoice == null)
			return;
		invoice.setDocAction(DocAction.ACTION_Prepare);
		if (!invoice.processIt(DocAction.ACTION_Prepare)) {
			StringBuilder msglog = new StringBuilder("Invoice Process Failed: ").append(invoice).append(" - ").append(invoice.getProcessMsg());
			log.warning(msglog.toString());
			throw new IllegalStateException(msglog.toString());
			
		}
		if (!invoice.save())
			throw new IllegalStateException("Cannot save Invoice");
		//
		m_noInvoices++;
		addBufferLog(invoice.get_ID(), invoice.getDateInvoiced(), 
			invoice.getGrandTotal(), invoice.getDocumentNo(), invoice.get_Table_ID(), invoice.getC_Invoice_ID());
	}	//	completeInvoice

}	//	ExpenseAPInvoice
