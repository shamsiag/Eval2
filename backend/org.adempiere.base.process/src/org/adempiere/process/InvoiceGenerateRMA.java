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

package org.adempiere.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Generate invoice for customer RMA
 * @author  Ashley Ramdass
 *
 * Based on org.compiere.process.InvoiceGenerate
 */
@org.adempiere.base.annotation.Process
public class InvoiceGenerateRMA extends SvrProcess
{
    /** Manual Selection        */
    private boolean     p_Selection = false;
    /** Invoice Document Action */
    private String      p_docAction = DocAction.ACTION_Complete;
    
    /** Number of Invoices      */
    private int         m_created = 0;
    /** Invoice Date            */
    private Timestamp   m_dateinvoiced = null;

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
            else if (name.equals("Selection"))
                p_Selection = "Y".equals(para[i].getParameter());
            else if (name.equals("DateInvoiced"))
            	m_dateinvoiced = (Timestamp)para[i].getParameter();
            else if (name.equals("DocAction"))
                p_docAction = (String)para[i].getParameter();
            else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
        }
        
        if (m_dateinvoiced == null) {
	    	m_dateinvoiced = Env.getContextAsDate(getCtx(), Env.DATE);
	        if (m_dateinvoiced == null)
	        {
	        	m_dateinvoiced = new Timestamp(System.currentTimeMillis());
	        }
        }
        if (getProcessInfo().getAD_InfoWindow_ID() > 0) p_Selection=true;
    }

    protected String doIt() throws Exception
    {
        if (!p_Selection)
        {
            throw new IllegalStateException("Invoice can only be generated from selection");
        }

        String sql = "SELECT rma.M_RMA_ID FROM M_RMA rma, T_Selection "
            + "WHERE rma.DocStatus='CO' AND rma.IsSOTrx='Y' AND rma.AD_Client_ID=? "
            + "AND rma.M_RMA_ID = T_Selection.T_Selection_ID " 
            + "AND T_Selection.AD_PInstance_ID=? ";
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try
        {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            pstmt.setInt(1, Env.getAD_Client_ID(getCtx()));
            pstmt.setInt(2, getAD_PInstance_ID());
            rs = pstmt.executeQuery();
            
            while (rs.next())
            {
                generateInvoice(rs.getInt(1));
            }
        }
        catch (Exception ex)
        {
			throw new AdempiereException(ex);
        }
        finally
        {
            DB.close(rs,pstmt);
            rs = null;pstmt = null;
        }
        StringBuilder msgreturn = new StringBuilder("@Created@ = ").append(m_created);
        return msgreturn.toString();
    }
    
    private int getInvoiceDocTypeId(int M_RMA_ID)
    {
        String docTypeSQl = "SELECT dt.C_DocTypeInvoice_ID FROM C_DocType dt "
            + "INNER JOIN M_RMA rma ON dt.C_DocType_ID=rma.C_DocType_ID "
            + "WHERE rma.M_RMA_ID=?";

        int docTypeId = DB.getSQLValue(get_TrxName(), docTypeSQl, M_RMA_ID);

        return docTypeId;
    }
    
    private MInvoice createInvoice(MRMA rma)
    {
        int docTypeId = getInvoiceDocTypeId(rma.get_ID());

        if (docTypeId == -1)
        {
            throw new IllegalStateException("Could not get invoice document type for Customer RMA");
        }

        MInvoice invoice = new MInvoice(getCtx(), 0, get_TrxName());
        invoice.setRMA(rma);
        
        invoice.setC_DocTypeTarget_ID(docTypeId);
        invoice.setDateInvoiced(m_dateinvoiced);
        invoice.setDateAcct(m_dateinvoiced);
        if (!invoice.save())
        {
            throw new IllegalStateException("Could not create invoice");
        }
        
        return invoice;
    }
    
    private MInvoiceLine[] createInvoiceLines(MRMA rma, MInvoice invoice)
    {
        ArrayList<MInvoiceLine> invLineList = new ArrayList<MInvoiceLine>();
        
        MRMALine rmaLines[] = rma.getLines(true);
        
        for (MRMALine rmaLine : rmaLines)
        {
            if (rmaLine.getM_InOutLine_ID() == 0 && rmaLine.getC_Charge_ID() == 0)
            {
                StringBuilder msgiste = new StringBuilder("No customer return line - RMA = ") 
                        .append(rma.getDocumentNo()).append(", Line = ").append(rmaLine.getLine());
            	throw new IllegalStateException(msgiste.toString());
            }
            
            MInvoiceLine invLine = new MInvoiceLine(invoice);
            invLine.setRMALine(rmaLine);
            
            if (!invLine.save())
            {
                throw new IllegalStateException("Could not create invoice line");
            }
            
            invLineList.add(invLine);
        }
        
        MInvoiceLine invLines[] = new MInvoiceLine[invLineList.size()];
        invLineList.toArray(invLines);
        
        return invLines;
    }
    
    
    private void generateInvoice(int M_RMA_ID)
    {
        MRMA rma = new MRMA(getCtx(), M_RMA_ID, get_TrxName());
        statusUpdate(Msg.getMsg(getCtx(), "Processing") + " " + rma.getDocumentInfo());
        
        MInvoice invoice = createInvoice(rma);
        MInvoiceLine invoiceLines[] = createInvoiceLines(rma, invoice);
        
        if (invoiceLines.length == 0)
        {
            StringBuilder msglog = new StringBuilder("No invoice lines created: M_RMA_ID=")
                    .append(M_RMA_ID).append(", M_Invoice_ID=").append(invoice.get_ID());
        	log.log(Level.WARNING, msglog.toString());
        }
        
        StringBuilder processMsg = new StringBuilder().append(invoice.getDocumentNo());
        
        if (!invoice.processIt(p_docAction))
        {
            processMsg.append(" (NOT Processed)");
            StringBuilder msg = new StringBuilder("Invoice Processing failed: ").append(invoice).append(" - ").append(invoice.getProcessMsg());
            log.warning(msg.toString());
            throw new IllegalStateException(msg.toString());
        }
        
        if (!invoice.save())
        {
            throw new IllegalStateException("Could not update invoice");
        }
        
        // Add processing information to process log
        String message = Msg.parseTranslation(getCtx(), "@InvoiceProcessed@ " + processMsg.toString()); 
        addBufferLog(invoice.getC_Invoice_ID(), invoice.getDateInvoiced(), null, message, invoice.get_Table_ID(), invoice.getC_Invoice_ID());
        m_created++;
    }
}
