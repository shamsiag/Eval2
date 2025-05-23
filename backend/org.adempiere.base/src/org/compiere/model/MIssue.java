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

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.compiere.Adempiere;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Secure;
import org.compiere.util.Util;

/**
 * 	Issue Report Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MIssue.java,v 1.3 2006/07/30 00:58:37 jjanke Exp $
 */
public class MIssue extends X_AD_Issue
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -3680542992654002121L;

	/**
	 * 	Create and report issue
	 *	@param record log record
	 *	@return reported issue or null
	 */
	public static MIssue create (LogRecord record)
	{
		if (s_log.isLoggable(Level.CONFIG))
			s_log.config(record.getMessage());
		if (!DB.isConnected())
			return null;
		MSystem system = MSystem.get(Env.getCtx()); 
		if (system == null || !system.isAutoErrorReport())
			return null;
		//
		MIssue issue = new MIssue(record);
		String error = issue.report();
		issue.saveEx();
		if (error != null)
			return null;
		return issue;
	}	//	create
	
	/**
	 * 	Create from decoded hash map string
	 *	@param ctx context
	 *	@param hexInput hex string
	 *	@return issue
	 *  @deprecated
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public static MIssue create (Properties ctx, String hexInput)
	{
		HashMap<String,String> hmIn = null;
		try		//	encode in report
		{
			byte[] byteArray = Secure.convertHexString(hexInput);	
			ByteArrayInputStream bIn = new ByteArrayInputStream(byteArray);
			ObjectInputStream oIn = new ObjectInputStream(bIn);
			hmIn = (HashMap<String,String>)oIn.readObject();
		
		}
		catch (Exception e) 
		{
			s_log.log(Level.SEVERE, "",e);
			return null;
		}

		MIssue issue = new MIssue(ctx, hmIn);
		return issue;
	}	//	create
	
	/**	Logger	*/
	private static CLogger s_log = CLogger.getCLogger (MIssue.class);
	
	/** Answer Delimiter		*/
	public static String	DELIMITER = "|";
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_Issue_UU  UUID key
     * @param trxName Transaction
     */
    public MIssue(Properties ctx, String AD_Issue_UU, String trxName) {
        super(ctx, AD_Issue_UU, trxName);
		if (Util.isEmpty(AD_Issue_UU))
			setInitialDefaults(ctx);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param AD_Issue_ID issue
	 *	@param trxName transaction
	 */
	public MIssue (Properties ctx, int AD_Issue_ID, String trxName)
	{
		super (ctx, AD_Issue_ID, trxName);
		if (AD_Issue_ID == 0)
			setInitialDefaults(ctx);
	}	//	MIssue

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults(Properties ctx) {
		setProcessed (false);	// N
		setSystemStatus(SYSTEMSTATUS_Evaluation);
		try
		{
			init(ctx);
		}
		catch (Exception e)
		{
			e.getStackTrace();
		}
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName trx
	 */
	public MIssue (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MIssue
	
	/**
	 * 	Log Record Constructor
	 *	@param record
	 */
	public MIssue (LogRecord record)
	{
		this (Env.getCtx(), 0, null);
		String summary = record.getMessage();
		setSourceClassName(record.getSourceClassName());
		setSourceMethodName(record.getSourceMethodName());
		setLoggerName(record.getLoggerName());
		Throwable t = record.getThrown();
		if (t != null)
		{
			if (summary != null && summary.length() > 0)
				summary = t.toString() + " " + summary;
			if (summary == null || summary.length() == 0)
				summary = t.toString();
			//
			StringBuilder error = new StringBuilder();
			StackTraceElement[] tes = t.getStackTrace();
			int count = 0;
			for (int i = 0; i < tes.length; i++)
			{
				StackTraceElement element = tes[i];
				String s = element.toString();
				if (s.indexOf("adempiere") != -1)
				{
					error.append(s).append("\n");
					if (count == 0)
					{
						StringBuilder source = new StringBuilder().append(element.getClassName())
							.append(".").append(element.getMethodName());
						setSourceClassName(source.toString());
						setLineNo(element.getLineNumber());
					}
					count++;
				}
				if (count > 5 || error.length() > 2000)
					break;
			}
			setErrorTrace(error.toString());
			//	Stack
			CharArrayWriter cWriter = new CharArrayWriter();
			PrintWriter pWriter = new PrintWriter(cWriter);
			t.printStackTrace(pWriter);
			setStackTrace(cWriter.toString());
		}
		if (summary == null || summary.length() == 0)
			summary = "??";
		setIssueSummary(summary.toString());
		setRecord_ID(1);
	}	//	MIssue

	/**
	 * 	HashMap Constructor
	 *	@param ctx context
	 *	@param hmIn hash map
	 *  @deprecated
	 */
	@Deprecated
	public MIssue (Properties ctx, HashMap<String,String> hmIn)
	{
		super (ctx, 0, null);
		load(hmIn);
		setRecord_ID(0);
	}	//	MIssue

	/**
	 * 	Initialize
	 * 	@param ctx context
	 * 	@throws Exception
	 */
	private void init(Properties ctx) throws Exception
	{
		MSystem system = MSystem.get(ctx);
		setName(system.getName());
		setUserName(system.getUserName());
		setDBAddress(system.getDBAddress());
		setSystemStatus(system.getSystemStatus());
		setReleaseNo(system.getReleaseNo());	//	DB
		setVersion(Adempiere.DATE_VERSION);		//	Code
		setDatabaseInfo(DB.getDatabaseInfo());
		setOperatingSystemInfo(Adempiere.getOSInfo());
		setJavaInfo(Adempiere.getJavaInfo());
		setReleaseTag(Adempiere.getImplementationVersion());
		setLocal_Host(InetAddress.getLocalHost().toString());
		if (system.isAllowStatistics())
		{
			setStatisticsInfo(system.getStatisticsInfo(false));
			setProfileInfo(system.getProfileInfo(false));
		}
	}	//	init
	
	/** Length of Info Fields			*/
	private static final int	INFOLENGTH = 2000;
	
	/**
	 * 	Set Issue Summary.
	 * 	Truncate to 2000 char.
	 *	@param IssueSummary summary
	 */
	public void setIssueSummary (String IssueSummary)
	{
		if (IssueSummary == null)
			return;
		IssueSummary = IssueSummary.replace("java.lang.", "");
		IssueSummary = IssueSummary.replace("java.sql.", "");
		if (IssueSummary.length() > INFOLENGTH)
			IssueSummary = IssueSummary.substring(0,INFOLENGTH-1);
		super.setIssueSummary (IssueSummary);
	}	//	setIssueSummary
	
	/**
	 * 	Set Stack Trace.
	 * 	Truncate to 2000 char.
	 *	@param StackTrace trace
	 */
	public void setStackTrace (String StackTrace)
	{
		if (StackTrace == null)
			return;
		StackTrace = StackTrace.replace("java.lang.", "");
		StackTrace = StackTrace.replace("java.sql.", "");
		if (StackTrace.length() > INFOLENGTH)
			StackTrace = StackTrace.substring(0,INFOLENGTH-1);
		super.setStackTrace (StackTrace);
	}	//	setStackTrace
		
	/**
	 * 	Set Error Trace.
	 * 	Truncate to 2000 char.
	 *	@param ErrorTrace trace
	 */
	public void setErrorTrace (String ErrorTrace)
	{
		if (ErrorTrace == null)
			return;
		ErrorTrace = ErrorTrace.replace("java.lang.", "");
		ErrorTrace = ErrorTrace.replace("java.sql.", "");
		if (ErrorTrace.length() > INFOLENGTH)
			ErrorTrace = ErrorTrace.substring(0,INFOLENGTH-1);
		super.setErrorTrace (ErrorTrace);
	}	//	setErrorTrace

	/**
	 * 	Add Comments
	 *	@param Comments
	 */
	public void addComments (String Comments)
	{
		if (Comments == null || Comments.length() == 0)
			return;
		String old = getComments();
		if (old == null || old.length() == 0)
			setComments (Comments);
		else if (!old.equals(Comments) 
			&& old.indexOf(Comments) == -1){	//	 something new
			StringBuilder msgc = new StringBuilder(Comments).append(" | ").append(old);
			setComments (msgc.toString());
		}	
	}	//	addComments
	
	/**
	 * 	Set Comments.
	 * 	Truncate to 2000 char.
	 *	@param Comments
	 */
	public void setComments (String Comments)
	{
		if (Comments == null)
			return;
		if (Comments.length() > INFOLENGTH)
			Comments = Comments.substring(0,INFOLENGTH-1);
		super.setComments (Comments);
	}	//	setComments
	
	/**
	 * 	Set ResponseText.
	 * 	Truncate to 2000 char.
	 *	@param ResponseText
	 */
	public void setResponseText (String ResponseText)
	{
		if (ResponseText == null)
			return;
		if (ResponseText.length() > INFOLENGTH)
			ResponseText = ResponseText.substring(0,INFOLENGTH-1);
		super.setResponseText(ResponseText);
	}	//	setResponseText

	/**
	 * 	Process Request.
	 * 	@return answer
	 *  @deprecated
	 */
	@Deprecated
	public String process()
	{
		MIssueProject.get(this);	//	sets also Asset
		MIssueSystem.get(this);
		MIssueUser.get(this);
		return createAnswer();
	}	//	process
	
	/**
	 * 	Create Answer to send to User
	 *	@return answer
	 *  @deprecated
	 */
	@Deprecated
	public String createAnswer()
	{
		StringBuilder sb = new StringBuilder();
		if (getA_Asset_ID() != 0)
			sb.append("Sign up for support at http://www.adempiere.org to receive answers.");
		else
		{
			if (getR_IssueKnown_ID() != 0)
				sb.append("Known Issue\n");
			if (getR_Request_ID() != 0)
				sb.append("Request: ")
					.append(getRequest().getDocumentNo())
					.append("\n");
		}
		return sb.toString();
	}	//	createAnswer
	
	/**
	 * 	Get Request
	 *	@return request or null
	 */
	public X_R_Request getRequest()
	{
		if (getR_Request_ID() == 0)
			return null;
		return new X_R_Request(getCtx(), getR_Request_ID(), null);
	}	//	getRequest

	/**
	 * 	Get Request Document No
	 *	@return request Document No
	 */
	public String getRequestDocumentNo()
	{
		if (getR_Request_ID() == 0)
			return "";
		X_R_Request r = getRequest();
		return r.getDocumentNo();
	}	//	getRequestDocumentNo
	
	/**
	 * 	Get System Status
	 *	@return system status or SYSTEMSTATUS_Evaluation if not set by user
	 */
	@Override
	public String getSystemStatus ()
	{
		String s = super.getSystemStatus ();
		if (s == null || s.length() == 0)
			s = SYSTEMSTATUS_Evaluation;
		return s;
	}	//	getSystemStatus
		
	/**
	 * 	Report/Update Issue.
	 *	@return error message
	 *  @deprecated not implemented
	 */
	@Deprecated
	public String report()
	{
		return null;
	}	//	report
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MIssue[");
		sb.append (get_ID())
			.append ("-").append (getIssueSummary())
			.append (",Record=").append (getRecord_ID())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
	
}	//	MIssue
