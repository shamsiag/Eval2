/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 **********************************************************************/
package org.compiere.process;

import java.util.logging.Level;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.MPInstance;
import org.compiere.model.MRule;
import org.compiere.model.MPInstance.PInstanceInfo;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.print.ServerReportCtl;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.wf.MWFProcess;

/**
 * Controller for running of process
 */
public class ServerProcessCtl implements Runnable {
	
	/**	Static Logger	*/
	private static CLogger	log	= CLogger.getCLogger (ServerProcessCtl.class);
	
	/** Process Info */
	ProcessInfo m_pi;
	private Trx				m_trx;
	private boolean managedTrxForJavaProcess = true;
	
	/**
	 *  Constructor
	 *  @param pi Process info
	 *  @param trx Transaction
	 */
	public ServerProcessCtl (ProcessInfo pi, Trx trx)
	{
		m_pi = pi;
		m_trx = trx;	//	handled correctly
	}   //  ProcessCtl
	
	/**
	 *	Run a process
	 *  <br/>
	 *	<ol>- Get Instance ID</ol>
	 *	<ol>- Get Parameters</ol>
	 *	<ol>- execute (lock - start process - unlock)</ol>
	 *  <br>
	 *
	 *  @param pi ProcessInfo process info
	 *  @param trx Transaction
	 *  @return worker started ServerProcessCtl instance
	 */
	public static ServerProcessCtl process (ProcessInfo pi, Trx trx)
	{
		return process(pi, trx, true);
	}
	
	/**
	 *	Run a process
	 *  <br/>
	 *	<ol>- Get Instance ID</ol>
	 *	<ol>- Get Parameters</ol>
	 *	<ol>- execute (lock - start process - unlock)</ol>
	 *  <br>
	 *
	 *  @param pi process info
	 *  @param trx Transaction
	 *  @param managedTrxForJavaProcess true to perform rollback/commit of transaction
	 *  @return worker started ServerProcessCtl instance
	 */
	public static ServerProcessCtl process (ProcessInfo pi, Trx trx, boolean managedTrxForJavaProcess)
	{
		if (log.isLoggable(Level.FINE)) log.fine("ServerProcess - " + pi);

		MPInstance instance = null; 
		if (pi.getAD_PInstance_ID() <= 0)
		{
			try 
			{ 
				instance = new MPInstance(Env.getCtx(), pi.getAD_Process_ID(), pi.getTable_ID(), pi.getRecord_ID(), pi.getRecord_UU());
				// Get PrintFormat
				MPrintFormat format = (MPrintFormat)pi.getTransientObject();
				if (format != null)
				{
					instance.updatePrintFormatAndLanguageIfEmpty(format);
					ReportEngine.setDefaultReportTypeToPInstance(Env.getCtx(), instance, instance.getAD_PrintFormat_ID());
				}
			} 
			catch (Exception e) 
			{ 
				pi.setSummary (e.getLocalizedMessage()); 
				pi.setError (true); 
				log.warning(pi.toString()); 
				return null; 
			} 
			catch (Error e) 
			{ 
				pi.setSummary (e.getLocalizedMessage()); 
				pi.setError (true); 
				log.warning(pi.toString()); 
				return null; 
			}
			if (!instance.save())
			{
				pi.setSummary (Msg.getMsg(Env.getCtx(), "ProcessNoInstance"));
				pi.setError (true);
				return null;
			}
			pi.setAD_PInstance_ID (instance.getAD_PInstance_ID());
		}
		else
		{
			instance = new MPInstance(Env.getCtx(), pi.getAD_PInstance_ID(), null);
		}

		//	execute
		ServerProcessCtl worker = new ServerProcessCtl(pi, trx);
		worker.setManagedTrxForJavaProcess(managedTrxForJavaProcess);
		worker.run();
		
		return worker;
	}	//	execute
	
	/**
	 * Run this process in a new thread
	 * @deprecated
	 */
	@Deprecated
	public void start()
	{
		Thread thread = new Thread(this);
		// Set thread name
		if (m_pi != null)
			thread.setName(m_pi.getTitle()+"-"+m_pi.getAD_PInstance_ID());
		thread.start();
	}

	/**
	 *	Execute Process Instance and Lock UI.
	 *  <p>
	 *	<ol>- Get Process Information</ol>
	 *  <ol>- Call Class</ol>
	 *	<ol>- Submit SQL Procedure</ol>
	 *	<ol>- Run SQL Procedure</ol>
	 */
	public void run ()
	{
		if (log.isLoggable(Level.FINE)) log.fine("AD_PInstance_ID=" + m_pi.getAD_PInstance_ID()
			+ ", Record_ID=" + m_pi.getRecord_ID());
		
		String 	ProcedureName = "";
		String  JasperReport = "";
		int     AD_ReportView_ID = 0;
		int		AD_Workflow_ID = 0;
		boolean IsReport = false;
		try
		{
			PInstanceInfo info = MPInstance.getPInstanceInfo(m_pi.getAD_PInstance_ID());
			if (info != null)
			{
				m_pi.setTitle (info.name);
				ProcedureName = info.procedureName;
				m_pi.setClassName (info.className);
				m_pi.setAD_Process_ID (info.AD_Process_ID);
				m_pi.setAD_Process_UU (info.AD_Process_UU);
				//	Report
				if (info.isReport)
				{
					IsReport = true;
				}
				AD_ReportView_ID = info.AD_ReportView_ID;
				AD_Workflow_ID = info.AD_Workflow_ID;
				//
				int estimate = info.estimate;
				if (estimate != 0)
				{
					m_pi.setEstSeconds (estimate + 1);     //  admin overhead
				}
				JasperReport = info.jasperReport;
			}
			else
				log.log(Level.SEVERE, "No AD_PInstance_ID=" + m_pi.getAD_PInstance_ID());
		}
		catch (Throwable e)
		{
			m_pi.setSummary (Msg.getMsg(Env.getCtx(), "ProcessNoProcedure") + " " + e.getLocalizedMessage(), true);
			log.log(Level.SEVERE, "run", e);
			return;
		}

		//  No PL/SQL Procedure
		if (ProcedureName == null)
			ProcedureName = "";

		/**********************************************************************
		 *	Workflow
		 */
		if (AD_Workflow_ID > 0)	
		{
			startWorkflow (AD_Workflow_ID);
			return;
		}

		// Clear Jasper Report class if default - to be executed later
		boolean isJasper = false;
		if (JasperReport != null && JasperReport.trim().length() > 0) {
			isJasper = true;
			if (ProcessUtil.JASPER_STARTER_CLASS.equals(m_pi.getClassName())) {
				m_pi.setClassName(null);
			}
		}
		
		/**********************************************************************
		 *	Start Java Process Class
		 */
		if (m_pi.getClassName() != null)
		{
			if (isJasper)
			{
				m_pi.setReportingProcess(true);
			}
			
			//	Run Class
			if (!startProcess())
			{
				return;
			}

			//  No Optional SQL procedure ... done
			if (!IsReport && ProcedureName.length() == 0)
			{
				return;
			}
			//  No Optional Report ... done
			if (IsReport && AD_ReportView_ID == 0 && !isJasper)
			{
				return;
			}
		}

		/**********************************************************************
		 *	Report submission
		 */
		//	Optional Pre-Report DB Process
		if (IsReport && ProcedureName.length() > 0)
		{
			m_pi.setReportingProcess(true);
			if (!startDBProcess(ProcedureName))
			{
				return;
			}
		}	//	Pre-Report

		if (isJasper)
		{
			m_pi.setReportingProcess(true);
			m_pi.setClassName(ProcessUtil.JASPER_STARTER_CLASS);
			startProcess();
			if (m_pi.isError()) {
				MPInstance pinstance = new MPInstance(Env.getCtx(), m_pi.getAD_PInstance_ID(), null);
				pinstance.setErrorMsg(m_pi.getSummary());
				pinstance.setJsonData(m_pi.getJsonData());
				pinstance.saveEx();
			}
			return;
		}
		
		if (IsReport)
		{
			m_pi.setReportingProcess(true);
			//	Start Report	-----------------------------------------------
			boolean ok = ServerReportCtl.start(m_pi);
			String summ = Util.cleanAmp(Msg.getMsg(Env.getCtx(), "Report"));
			m_pi.setSummary(summ, !ok);
		}
		/**********************************************************************
		 * 	Start DB Process
		 */
		else
		{
			if (!startDBProcess (ProcedureName))
			{
				return;
			}
			//	Success - getResult
			ProcessInfoUtil.setSummaryFromDB(m_pi);
		}			//	*** Process submission ***
	}   //  run
	
	/**
	 *  Start Workflow.
	 *
	 *  @param AD_Workflow_ID workflow id
	 *  @return true if started
	 */
	protected boolean startWorkflow (int AD_Workflow_ID)
	{
		if (log.isLoggable(Level.FINE)) log.fine(AD_Workflow_ID + " - " + m_pi);
		boolean started = false;
		if (m_trx != null)
			m_pi.setTransactionName(m_trx.getTrxName());
		MWFProcess wfProcess = ProcessUtil.startWorkFlow(Env.getCtx(), m_pi, AD_Workflow_ID);
		started = wfProcess != null;
		return started;
	}   //  startWorkflow

	/**
	 *  Start Java or Script Process.<br/>
	 *
	 *  @return true if success
	 */
	protected boolean startProcess ()
	{
		if (log.isLoggable(Level.FINE)) log.fine(m_pi.toString());
		if (m_pi.getClassName().toLowerCase().startsWith(MRule.SCRIPT_PREFIX)) {
			return ProcessUtil.startScriptProcess(Env.getCtx(), m_pi, m_trx);
		} else {
			return ProcessUtil.startJavaProcess(Env.getCtx(), m_pi, m_trx, managedTrxForJavaProcess);
		}
	}   //  startProcess

	/**
	 *  Start Database Process
	 *  @param ProcedureName PL/SQL procedure name
	 *  @return true if success
	 */
	protected boolean startDBProcess (String ProcedureName)
	{
		//  execute on this thread/connection
		if (log.isLoggable(Level.FINE)) log.fine(ProcedureName + "(" + m_pi.getAD_PInstance_ID() + ")");
		return ProcessUtil.startDatabaseProcedure(m_pi, ProcedureName, m_trx);
	}   //  startDBProcess
	
	/**
	 * set whether java process call will commit/rollback trx (default is true)
	 * @param managedTrx
	 */
	public void setManagedTrxForJavaProcess(boolean managedTrx)
	{
		this.managedTrxForJavaProcess = managedTrx;
	}
	
	/**
	 * Is java process call will commit/rollback trx
	 * @return true if java process call will commit/rollback trx
	 */
	public boolean isManagedTrxForJavaProcess()
	{
		return managedTrxForJavaProcess;
	}
}
