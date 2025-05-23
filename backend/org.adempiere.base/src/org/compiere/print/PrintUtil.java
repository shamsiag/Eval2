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
package org.compiere.print;

import static org.compiere.model.SystemIDs.PRINTFORMAT_INOUT_HEADER_TEMPLATE;
import static org.compiere.model.SystemIDs.PRINTFORMAT_INOUT_LINE_TEMPLATE;
import static org.compiere.model.SystemIDs.PRINTFORMAT_INVOICE_HEADER_TEMPLATE;
import static org.compiere.model.SystemIDs.PRINTFORMAT_INVOICE_LINETAX_TEMPLATE;
import static org.compiere.model.SystemIDs.PRINTFORMAT_ORDER_HEADER_TEMPLATE;
import static org.compiere.model.SystemIDs.PRINTFORMAT_ORDER_LINETAX_TEMPLATE;
import static org.compiere.model.SystemIDs.PRINTFORMAT_PAYSELECTION_CHECK_TEMPLATE;
import static org.compiere.model.SystemIDs.PRINTFORMAT_PAYSELECTION_REMITTANCE_LINES_TEMPLATE;
import static org.compiere.model.SystemIDs.PRINTFORMAT_PAYSELECTION_REMITTANCE__TEMPLATE;

import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.PrinterJob;
import java.util.Properties;
import java.util.logging.Level;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUIFactory;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobPriority;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JDialog;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Language;
import org.compiere.util.Msg;

/**
 *  Print Utilities
 *
 *  @author     Jorg Janke
 *  @version    $Id: PrintUtil.java,v 1.2 2006/07/30 00:53:02 jjanke Exp $
 */
public class PrintUtil
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(PrintUtil.class);
	/** Default Print Request Attribute Set */
	private static PrintRequestAttributeSet     s_prats = new HashPrintRequestAttributeSet();

	/**
	 *  Get Default Print Request Attributes
	 *  @return PrintRequestAttributeSet
	 */
	public static PrintRequestAttributeSet getDefaultPrintRequestAttributes()
	{
		return s_prats;
	}   //  getDefaultPrintRequestAttributes

	/**
	 *  Get Default Application Flavor
	 *  @return Pageable
	 */
	public static DocFlavor getDefaultFlavor()
	{
		return DocFlavor.SERVICE_FORMATTED.PAGEABLE;
	}   //  getDefaultFlavor

	/**
	 * Get Print Services for all flavor and print request attributes
	 * @return print services
	 */
	public static PrintService[] getAllPrintServices() 
	{
		return PrintServiceLookup.lookupPrintServices(null,null);
	}
	
	/**
	 *  Get Print Services for standard flavor and print request attributes
	 *  @return print services
	 */
	public static PrintService[] getPrintServices ()
	{
		return PrintServiceLookup.lookupPrintServices (getDefaultFlavor(), getDefaultPrintRequestAttributes());
	}   //  getPrintServices

	/**
	 *  Get Default Print Service
	 *  @return PrintService
	 */
	public static PrintService getDefaultPrintService()
	{
		return PrintServiceLookup.lookupDefaultPrintService();
	}   //  getPrintServices

	/**
	 *  Get default PrinterJob
	 *  @return PrinterJob
	 */
	public static PrinterJob getPrinterJob()
	{
		return getPrinterJob(Ini.getProperty(Ini.P_PRINTER));
	}   //  getPrinterJob

	/**
	 *  Get PrinterJob with selected printer name.
	 *  @param printerName if null, get default printer (Ini.P_PRINTER)
	 *  @return PrinterJob
	 */
	public static PrinterJob getPrinterJob (String printerName)
	{
		PrinterJob pj = null;
		PrintService ps = null;
		try
		{
			pj = PrinterJob.getPrinterJob();

			//  find printer service
			if (printerName == null || printerName.length() == 0)
				printerName = Ini.getProperty(Ini.P_PRINTER);
			if (printerName != null && printerName.length() != 0)
			{
				PrintService[] services = getAllPrintServices();
				for (int i = 0; i < services.length; i++)
				{
					String serviceName = services[i].getName();
					if (printerName.equals(serviceName))
					{
						ps = services[i];
						break;
					}
				}
			}   //  find printer service

			try
			{
				if (ps != null)
					pj.setPrintService(ps);
			}
			catch (Exception e)
			{
				log.warning("Could not set Print Service: " + e.toString());
			}
			//
			PrintService psUsed = pj.getPrintService();
			if (psUsed == null)
				log.warning("Print Service not Found");
			else
			{
				String serviceName = psUsed.getName();
				if (printerName != null && !printerName.equals(serviceName))
					log.warning("Not found: " + printerName + " - Used: " + serviceName);
			}
		}
		catch (Exception e)
		{
			log.warning("Could not create for " + printerName + ": " + e.toString());
		}
		return pj;
	}   //  getPrinterJob
	
	/**
	 * 	Print (async)
	 * 	@param printerName optional printer name
	 *  @param jobName optional printer job name
	 * 	@param pageable pageable
	 *  @param copies number of copies
	 *  @param withDialog if true, shows printer dialog
	 */
	static public void print (Pageable pageable, String printerName, String jobName,
		int copies, boolean withDialog)
	{
		if (pageable == null)
			return;
		String name = "Adempiere_";
		if (jobName != null)
			name += jobName;
		//
		PrinterJob job = getPrinterJob(printerName);
		job.setJobName (name);
		job.setPageable (pageable);
		//	Attributes
		HashPrintRequestAttributeSet prats = new HashPrintRequestAttributeSet();
		prats.add(new Copies(copies));
		//	Set Orientation
		if (pageable.getPageFormat(0).getOrientation() == PageFormat.PORTRAIT)
			prats.add(OrientationRequested.PORTRAIT);
		else
			prats.add(OrientationRequested.LANDSCAPE);
		prats.add(new JobName(name, Language.getLoginLanguage().getLocale()));
		prats.add(getJobPriority(pageable.getNumberOfPages(), copies, withDialog));
		//
		print (job, prats, withDialog, false);
	}	//	print

	/**
	 * 	Print Async
	 *  @param pageable pageable
	 *  @param prats print request attribute set
	 */
	static public void print (Pageable pageable, PrintRequestAttributeSet prats)
	{
		PrinterJob job = getPrinterJob();
		job.setPageable(pageable);
		print (job, prats, true, false);
	}	//	print

	/**
	 * 	Print
	 * 	@param job printer job
	 *  @param prats print request attribute set
	 *  @param withDialog if true shows Dialog
	 *  @param waitForIt if false print async
	 */
	static public void print (final PrinterJob job,
		final PrintRequestAttributeSet prats,
		boolean withDialog, boolean waitForIt)
	{
		if (job == null)
			return;
		boolean printed = true;

		if (withDialog)
			printed = job.printDialog(prats);

		if (printed)
		{
			if (withDialog)
			{
				Attribute[] atts = prats.toArray();
				for (int i = 0; i < atts.length; i++)
					if (log.isLoggable(Level.FINE)) log.fine(atts[i].getName() + "=" + atts[i]);
			}
			//
			if (waitForIt)
			{
				if (log.isLoggable(Level.FINE)) log.fine("(wait) " + job.getPrintService());
				try
				{
					job.print(prats);
				}
				catch (Exception ex)
				{
					log.log(Level.SEVERE, "(wait)", ex);
				}
			}
			else	//	Async
			{
				//	Create Thread
				Thread printThread = new Thread()
				{
					public void run()
					{
						if (log.isLoggable(Level.FINE)) log.fine("print: " + job.getPrintService());
						try
						{
							job.print(prats);
						}
						catch (Exception ex)
						{
							log.log(Level.SEVERE, "print", ex);
						}
					}
				};
				printThread.start();
			}	//	Async
		}	//	printed
	}	//	printAsync

	/**
	 * 	Get Job Priority based on pages printed.<br/>
	 *  The more pages, the lower the priority.
	 * 	@param pages number of pages
	 *  @param copies number of copies
	 *  @param withDialog dialog gets lower priority than direct print
	 * 	@return Job Priority
	 */
	static public JobPriority getJobPriority (int pages, int copies, boolean withDialog)
	{
		//	Set priority (the more pages, the lower the priority)
		int priority =  copies * pages;
		if (withDialog)				//	 prefer direct print
			priority *= 2;
		priority = 100 - priority;	//	convert to 1-100 supported range
		if (priority < 10)
			priority = 10;
		else if (priority > 100)
			priority = 100;
		return new JobPriority(priority);
	}	//	getJobPriority

	/**
	 * 	Dump Printer Job info
	 * 	@param job printer job
	 */
	public static void dump (PrinterJob job)
	{
		StringBuilder sb = new StringBuilder(job.getJobName());
		sb.append("/").append(job.getUserName())
			.append(" Service=").append(job.getPrintService().getName())
			.append(" Copies=").append(job.getCopies());
		PageFormat pf = job.defaultPage();
		sb.append(" DefaultPage ")
			.append("x=").append(pf.getImageableX())
			.append(",y=").append(pf.getImageableY())
			.append(" w=").append(pf.getImageableWidth())
			.append(",h=").append(pf.getImageableHeight());
		System.out.println(sb.toString());
	}	//	dump

	/**
	 * 	Dump Print Service Attribute Set to System.out
	 * 	@param psas PS Attribute Set
	 */
	public static void dump (PrintServiceAttributeSet psas)
	{
		System.out.println("PrintServiceAttributeSet - length=" + psas.size());
		Attribute[] ats = psas.toArray();
		for (int i = 0; i < ats.length; i++)
			System.out.println(ats[i].getName() + " = " + ats[i] + "  (" + ats[i].getCategory() + ")");
	}	//	dump

	/**
	 * 	Dump Print Request Service Attribute Set to System.out
	 * 	@param prats Print Request Attribute Set
	 */
	public static void dump (PrintRequestAttributeSet prats)
	{
		System.out.println("PrintRequestAttributeSet - length=" + prats.size());
		Attribute[] ats = prats.toArray();
		for (int i = 0; i < ats.length; i++)
			System.out.println(ats[i].getName() + " = " + ats[i] + "  (" + ats[i].getCategory() + ")");
	}	//	dump

	/**
	 * 	Dump Stream Print Services
	 * 	@param docFlavor flavor
	 * 	@param outputMimeType mime
	 */
	public static void dump (DocFlavor docFlavor, String outputMimeType)
	{
		System.out.println();
		System.out.println("DocFlavor=" + docFlavor + ", Output=" + outputMimeType);
		StreamPrintServiceFactory[] spsfactories =
			StreamPrintServiceFactory.lookupStreamPrintServiceFactories(docFlavor, outputMimeType);
		for (int i = 0; i < spsfactories.length; i++)
		{
			System.out.println("- " + spsfactories[i]);
			DocFlavor dfs[] = spsfactories[i].getSupportedDocFlavors();
			for (int j = 0; j < dfs.length; j++)
			{
				System.out.println("   -> " + dfs[j]);
			}
		}
	}	//	dump

	/**
	 * 	Dump Stream Print Services
	 * 	@param docFlavor flavor
	 */
	@SuppressWarnings("unchecked")
	public static void dump (DocFlavor docFlavor)
	{
		System.out.println();
		System.out.println("DocFlavor=" + docFlavor);
		PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
		PrintService[] pss =
			PrintServiceLookup.lookupPrintServices(docFlavor, pras);
		for (int i = 0; i < pss.length; i++)
		{
			PrintService ps = pss[i];
			System.out.println("- " + ps);
			System.out.println("  Factory=" + ps.getServiceUIFactory());
			ServiceUIFactory uiF = pss[i].getServiceUIFactory();
			if (uiF != null)
			{
				System.out.println("about");
				JDialog about = (JDialog) uiF.getUI (ServiceUIFactory.ABOUT_UIROLE, ServiceUIFactory.JDIALOG_UI);
				about.setVisible(true);
				System.out.println("admin");
				JDialog admin = (JDialog) uiF.getUI (ServiceUIFactory.ADMIN_UIROLE, ServiceUIFactory.JDIALOG_UI);
				admin.setVisible(true);
				System.out.println("main");
				JDialog main = (JDialog) uiF.getUI (ServiceUIFactory.MAIN_UIROLE, ServiceUIFactory.JDIALOG_UI);
				main.setVisible(true);
				System.out.println("reserved");
				JDialog res = (JDialog) uiF.getUI (ServiceUIFactory.RESERVED_UIROLE, ServiceUIFactory.JDIALOG_UI);
				res.setVisible(true);
			}
			//
			DocFlavor dfs[] = pss[i].getSupportedDocFlavors();
			System.out.println("  - Supported Doc Flavors");
			for (int j = 0; j < dfs.length; j++)
				System.out.println("    -> " + dfs[j]);
			//	Attribute
			Class<?>[] attCat = pss[i].getSupportedAttributeCategories();
			System.out.println("  - Supported Attribute Categories");
			for (int j = 0; j < attCat.length; j++)
				System.out.println("    -> " + attCat[j].getName() 
					+ " = " + pss[i].getDefaultAttributeValue((Class<? extends Attribute>)attCat[j]));
			//
		}
	}	//	dump

	/**
	 * 	Create Print Form and Print Formats for a new Client.
	 *  - Order, Invoice, etc.
	 *  Called from VSetup
	 *  @param AD_Client_ID new Client
	 */
	public static void setupPrintForm (int AD_Client_ID)
	{
		setupPrintForm(AD_Client_ID, (String)null);
	}
	
	/**
	 * 	Create Print Form and Print Formats for a new Client.<br/>
	 *  - Order, Invoice, etc.<br/>
	 *  @param AD_Client_ID new Client
	 *  @param trxName
	 */
	public static void setupPrintForm (int AD_Client_ID, String trxName)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("AD_Client_ID=" + AD_Client_ID);
		Properties ctx = Env.getCtx();

		//	Order Template
		int Order_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_ORDER_HEADER_TEMPLATE, AD_Client_ID, trxName).get_ID();
		int OrderLine_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_ORDER_LINETAX_TEMPLATE, AD_Client_ID, trxName).get_ID();
		updatePrintFormatHeader(Order_PrintFormat_ID, OrderLine_PrintFormat_ID, trxName);
		//	Invoice
		int Invoice_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_INVOICE_HEADER_TEMPLATE, AD_Client_ID, trxName).get_ID();
		int InvoiceLine_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_INVOICE_LINETAX_TEMPLATE, AD_Client_ID, trxName).get_ID();
		updatePrintFormatHeader(Invoice_PrintFormat_ID, InvoiceLine_PrintFormat_ID, trxName);
		//	Shipment
		int Shipment_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_INOUT_HEADER_TEMPLATE, AD_Client_ID, trxName).get_ID();
		int ShipmentLine_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_INOUT_LINE_TEMPLATE, AD_Client_ID, trxName).get_ID();
		updatePrintFormatHeader(Shipment_PrintFormat_ID, ShipmentLine_PrintFormat_ID, trxName);
		//	Check
		int Check_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_PAYSELECTION_CHECK_TEMPLATE, AD_Client_ID, trxName).get_ID();
		int RemittanceLine_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_PAYSELECTION_REMITTANCE_LINES_TEMPLATE, AD_Client_ID, trxName).get_ID();
		updatePrintFormatHeader(Check_PrintFormat_ID, RemittanceLine_PrintFormat_ID, trxName);
		//	Remittance
		int Remittance_PrintFormat_ID = MPrintFormat.copyToClient(ctx, PRINTFORMAT_PAYSELECTION_REMITTANCE__TEMPLATE, AD_Client_ID, trxName).get_ID();
		updatePrintFormatHeader(Remittance_PrintFormat_ID, RemittanceLine_PrintFormat_ID, trxName);

		int AD_PrintForm_ID = DB.getNextID (AD_Client_ID, "AD_PrintForm", null);
		StringBuilder sql = new StringBuilder("INSERT INTO AD_PrintForm(AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_PrintForm_ID,AD_PrintForm_UU,"
			+ "Name,Order_PrintFormat_ID,Invoice_PrintFormat_ID,Remittance_PrintFormat_ID,Shipment_PrintFormat_ID)"
			+ " VALUES (")
			.append(AD_Client_ID).append(",0,'Y',getDate(),0,getDate(),0,").append(AD_PrintForm_ID).append(",generate_uuid(),")
			.append(DB.TO_STRING(Msg.translate(ctx, "Standard"))).append(",")
			.append(Order_PrintFormat_ID).append(",").append(Invoice_PrintFormat_ID).append(",")
			.append(Remittance_PrintFormat_ID).append(",").append(Shipment_PrintFormat_ID).append(")");
		int no = DB.executeUpdateEx(sql.toString(), trxName);
		if (no != 1)
			log.log(Level.SEVERE, "PrintForm NOT inserted");

	}	//	createDocuments

	/**
	 * 	Update the PrintFormat Header lines with Reference to Child Print Format.
	 * 	@param Header_ID AD_PrintFormat_ID for Header
	 * 	@param Line_ID AD_PrintFormat_ID for Child Print Format
	 *  @param trxName
	 */
	static private void updatePrintFormatHeader (int Header_ID, int Line_ID, String trxName)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE AD_PrintFormatItem SET AD_PrintFormatChild_ID=")
			.append(Line_ID)
			.append(" WHERE AD_PrintFormatChild_ID IS NOT NULL AND AD_PrintFormat_ID=")
			.append(Header_ID);
		@SuppressWarnings("unused")
		int no = DB.executeUpdate(sb.toString(), trxName);
	}	//	updatePrintFormatHeader

}   //  PrintUtil
