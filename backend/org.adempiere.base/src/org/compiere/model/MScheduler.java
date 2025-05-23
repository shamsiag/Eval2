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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Scheduler Model
 *
 *  @author Jorg Janke
 *  @version $Id: MScheduler.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 *  
 *  Contributors:
 *    Carlos Ruiz - globalqss - FR [3135351] - Enable Scheduler for buttons
 */
public class MScheduler extends X_AD_Scheduler
	implements AdempiereProcessor, AdempiereProcessor2, ImmutablePOSupport
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = -2427229109274587547L;

	/**
	 * 	Get active schedulers
	 *	@param ctx context
	 *	@return active schedulers
	 */
	public static MScheduler[] getActive (Properties ctx)
	{
		List<MScheduler> list = new Query(ctx, Table_Name, null, null)
		.setOnlyActiveRecords(true)
		.list();
		MScheduler[] retValue = new MScheduler[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getActive

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_Scheduler_UU  UUID key
     * @param trxName Transaction
     */
    public MScheduler(Properties ctx, String AD_Scheduler_UU, String trxName) {
        super(ctx, AD_Scheduler_UU, trxName);
		if (Util.isEmpty(AD_Scheduler_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param AD_Scheduler_ID id
	 *	@param trxName transaction
	 */
	public MScheduler (Properties ctx, int AD_Scheduler_ID, String trxName)
	{
		super (ctx, AD_Scheduler_ID, trxName);
		if (AD_Scheduler_ID == 0)
			setInitialDefaults();
	}	//	MScheduler

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setKeepLogDays (7);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MScheduler (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MScheduler

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MScheduler(MScheduler copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MScheduler(Properties ctx, MScheduler copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MScheduler(Properties ctx, MScheduler copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
		this.m_parameter = copy.m_parameter != null ? Arrays.stream(copy.m_parameter).map(e -> {return new MSchedulerPara(ctx, e, trxName);}).toArray(MSchedulerPara[]::new) : null;
		this.m_recipients = copy.m_recipients != null ? Arrays.stream(copy.m_recipients).map(e -> {return new MSchedulerRecipient(ctx, e, trxName);}).toArray(MSchedulerRecipient[]::new) : null;
	}
	
	/**	Process Parameter			*/
	private MSchedulerPara[] m_parameter = null;
	/** Process Recipients			*/
	private MSchedulerRecipient[]	m_recipients = null;

	/**
	 * 	Get Server ID
	 *	@return server id
	 */
	public String getServerID ()
	{
		if (get_ID() == 0 && get_IDOld() > 0)
		{
			return "Scheduler" + get_IDOld();
		}
		else
		{
			return "Scheduler" + get_ID();
		}
	}	//	getServerID

	/**
	 * 	Get Next Run time stamp
	 *	@param requery requery
	 *	@return next run time stamp
	 */
	public Timestamp getDateNextRun (boolean requery)
	{
		if (requery)
			load(get_TrxName());
		return getDateNextRun();
	}	//	getDateNextRun

	/**
	 * 	Get scheduler logs
	 *	@return scheduler logs
	 */
	public AdempiereProcessorLog[] getLogs ()
	{
		final String whereClause = MSchedulerLog.COLUMNNAME_AD_Scheduler_ID+"=?";
		List<MSchedulerLog> list = new Query(getCtx(), I_AD_SchedulerLog.Table_Name, whereClause, get_TrxName())
		.setParameters(getAD_Scheduler_ID())
		.setOrderBy("Created DESC")
		.list();
		MSchedulerLog[] retValue = new MSchedulerLog[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getLogs

	/**
	 * 	Delete old scheduler logs
	 *	@return number of records deleted
	 */
	public int deleteLog()
	{
		if (getKeepLogDays() < 1)
			return 0;
		String sql = "DELETE FROM AD_SchedulerLog "
			+ "WHERE AD_Scheduler_ID=" + getAD_Scheduler_ID()
			+ " AND (Created+" + getKeepLogDays() + ") < getDate()";
		int no = DB.executeUpdateEx(sql, get_TrxName());
		return no;
	}	//	deleteLog

	/**
	 * 	Get Process
	 *	@return process
	 */
	public MProcess getProcess()
	{
		return MProcess.getCopy(getCtx(), getAD_Process_ID(), (String)null);
	}	//	getProcess

	/**
	 * 	Get Scheduler Parameters
	 *	@param reload true to reload from DB
	 *	@return scheduler parameters
	 */
	public MSchedulerPara[] getParameters (boolean reload)
	{
		if (!reload && m_parameter != null)
			return m_parameter;
		//
		final String whereClause = MSchedulerPara.COLUMNNAME_AD_Scheduler_ID+"=?";
		List<MSchedulerPara> list = new Query(getCtx(), I_AD_Scheduler_Para.Table_Name, whereClause, get_TrxName())
		.setParameters(getAD_Scheduler_ID())
		.setOnlyActiveRecords(true)
		.list();
		if (list.size() > 0 && is_Immutable())
			list.stream().forEach(e -> e.markImmutable());
		m_parameter = new MSchedulerPara[list.size()];
		list.toArray(m_parameter);
		return m_parameter;
	}	//	getParameter

	/**
	 * 	Get Scheduler Recipients for notificationss
	 *	@param reload true to reload from DB
	 *	@return Scheduler Recipients
	 */
	public MSchedulerRecipient[] getRecipients (boolean reload)
	{
		if (!reload && m_recipients != null)
			return m_recipients;
		//
		final String whereClause = MSchedulerRecipient.COLUMNNAME_AD_Scheduler_ID+"=?";
		List<MSchedulerRecipient> list = new Query(getCtx(), I_AD_SchedulerRecipient.Table_Name, whereClause, get_TrxName())
		.setParameters(getAD_Scheduler_ID())
		.setOnlyActiveRecords(true)
		.list();
		if (list.size() > 0 && is_Immutable())
			list.stream().forEach(e -> e.markImmutable());
			
		m_recipients = new MSchedulerRecipient[list.size()];
		list.toArray(m_recipients);
		return m_recipients;
	}	//	getRecipients

	/**
	 * 	Get Recipient AD_User_IDs
	 *	@return array of recipient user IDs
	 */
	public Integer[] getRecipientAD_User_IDs()
	{
		return getRecipientAD_User_IDs(false);
	}
	
	/**
	 * 	Get Recipient AD_User_IDs
	 *  @param excludeUploadRecipient true to exclude recipient with IsUpload=Y
	 *	@return array of recipient user IDs
	 */
	public Integer[] getRecipientAD_User_IDs(boolean excludeUploadRecipient)
	{
		TreeSet<Integer> list = new TreeSet<Integer>();
		MSchedulerRecipient[] recipients = getRecipients(false);
		for (int i = 0; i < recipients.length; i++)
		{
			MSchedulerRecipient recipient = recipients[i];
			if (!recipient.isActive())
				continue;
			if (recipient.getAD_User_ID() != 0)
			{
				if (!excludeUploadRecipient || !recipient.isUpload())
					list.add(recipient.getAD_User_ID());
			}
			if (recipient.getAD_Role_ID() != 0)
			{
				MUserRoles[] urs = MUserRoles.getOfRole(getCtx(), recipient.getAD_Role_ID());
				for (int j = 0; j < urs.length; j++)
				{
					MUserRoles ur = urs[j];
					if (!ur.isActive())
						continue;
					if (!list.contains(ur.getAD_User_ID()))
						list.add(ur.getAD_User_ID());
				}
			}
		}
		//
		return list.toArray(new Integer[list.size()]);
	}	//	getRecipientAD_User_IDs

	@Override
	protected boolean beforeSave(boolean newRecord)
	{		
		if (getAD_Table_ID() > 0) {
			// Validate the table has button referencing the scheduler process
			int colid = new Query(getCtx(), MColumn.Table_Name, "AD_Table_ID=? AND AD_Reference_ID=? AND AD_Process_ID=?", get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(getAD_Table_ID(), DisplayType.Button, getAD_Process_ID())
				.firstId();
			if (colid <= 0) {
				log.saveError("Error", Msg.getMsg(getCtx(), "TableMustHaveProcessButton"));
				return false;
			}
		} else {
			setRecord_ID(-1);
		}
		
		if (getRecord_ID() != 0) {
			// Validate AD_Table_ID must be set
			if (getAD_Table_ID() <= 0) {
				log.saveError("Error", Msg.getMsg(getCtx(), "MustFillTable"));
				return false;
			}
			// Validate the record must exists on the same client of the scheduler
			MTable table = MTable.get(getCtx(), getAD_Table_ID());
			PO po = table.getPO(getRecord_ID(), get_TrxName());
			if (po == null || po.get_ID() <= 0 || po.getAD_Client_ID() != getAD_Client_ID()) {
				log.saveError("Error", Msg.getMsg(getCtx(), "NoRecordID"));
				return false;
			}
		}
		
		// Calculate DateNextRun for new record or if schedule has change
		if (newRecord || is_ValueChanged("AD_Schedule_ID")) {
			String timeZoneId = null;
			if((getAD_Client_ID() == 0 && getAD_Org_ID() == 0) || getAD_Org_ID() > 0) {
				MOrgInfo orgInfo = MOrgInfo.get(getAD_Org_ID());
				timeZoneId = orgInfo.getTimeZone();
			}
			
			if(Util.isEmpty(timeZoneId, true)) {
				MClientInfo clientInfo = MClientInfo.get(getCtx(), getAD_Client_ID());
				if (clientInfo == null)
					clientInfo = MClientInfo.get(getCtx(), getAD_Client_ID(), get_TrxName());
				timeZoneId = clientInfo.getTimeZone();
			}

			long nextWork = MSchedule.getNextRunMS(System.currentTimeMillis(), getScheduleType(), getFrequencyType(), getFrequency(), getCronPattern(),
					timeZoneId);
			if (nextWork > 0)
				setDateNextRun(new Timestamp(nextWork));
		}
		
		return true;
	}	//	beforeSave

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MScheduler[");
		sb.append (get_ID ()).append ("-").append (getName()).append ("]");
		return sb.toString ();
	}	//	toString

	@Override
	public String getFrequencyType() {
	   return MSchedule.get(getCtx(),getAD_Schedule_ID()).getFrequencyType();
	}

	@Override
	public int getFrequency() {
	   return MSchedule.get(getCtx(),getAD_Schedule_ID()).getFrequency();
	}

	@Override
	public boolean isIgnoreProcessingTime() {
	   return MSchedule.get(getCtx(),getAD_Schedule_ID()).isIgnoreProcessingTime();
	}

	@Override
	public String getScheduleType() {
	   return MSchedule.get(getCtx(),getAD_Schedule_ID()).getScheduleType();
	}

	@Override
	public String getCronPattern() {
	   return MSchedule.get(getCtx(),getAD_Schedule_ID()).getCronPattern();
	}

	@Override
	public MScheduler markImmutable() 
	{
		if (is_Immutable())
			return this;
		
		makeImmutable();
		if (m_parameter != null && m_parameter.length > 0)
			Arrays.stream(m_parameter).forEach(e -> e.markImmutable());
		if (m_recipients != null && m_recipients.length > 0)
			Arrays.stream(m_recipients).forEach(e -> e.markImmutable());
		
		return this;
	}

	/**
	 * Get scheduler upload recipients
	 * @return array of upload recipients
	 */
	public MSchedulerRecipient[] getUploadRecipients() {
		List<MSchedulerRecipient> list = new ArrayList<>();
		MSchedulerRecipient[] recipients = getRecipients(false);
		for (int i = 0; i < recipients.length; i++) {
			MSchedulerRecipient recipient = recipients[i];
			if (!recipient.isActive())
				continue;
			if (recipient.getAD_User_ID() > 0 && recipient.isUpload() && recipient.getAD_AuthorizationAccount_ID() > 0) {
				list.add(recipient);
			}
		}
		return list.toArray(new MSchedulerRecipient[0]);
	}
}	//	MScheduler
