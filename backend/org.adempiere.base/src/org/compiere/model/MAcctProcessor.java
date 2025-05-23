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
import java.util.List;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.util.Util;


/**
 *	Accounting Processor Model
 *	
 *  @author Jorg Janke
 *  @author     victor.perez@e-evolution.com, www.e-evolution.com
 *    			<li>RF [ 2214883 ] Remove SQL code and Replace for Query https://sourceforge.net/p/adempiere/feature-requests/557/
 *  @version $Id: MAcctProcessor.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class MAcctProcessor extends X_C_AcctProcessor
	implements AdempiereProcessor, AdempiereProcessor2
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -4760475718973777369L;

	/**
	 * 	Get Active
	 *	@param ctx context
	 *	@return active processors
	 */
	public static MAcctProcessor[] getActive (Properties ctx)
	{
		List<MAcctProcessor> list = new Query(ctx, I_C_AcctProcessor.Table_Name, null, null)
										.setOnlyActiveRecords(true)
										.list();
		return list.toArray(new MAcctProcessor[list.size()]);		
	}	//	getActive
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_AcctProcessor_UU  UUID key
     * @param trxName Transaction
     */
    public MAcctProcessor(Properties ctx, String C_AcctProcessor_UU, String trxName) {
        super(ctx, C_AcctProcessor_UU, trxName);
		if (Util.isEmpty(C_AcctProcessor_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Construvtor
	 *	@param ctx context
	 *	@param C_AcctProcessor_ID id
	 *	@param trxName transaction
	 */
	public MAcctProcessor (Properties ctx, int C_AcctProcessor_ID, String trxName)
	{
		super (ctx, C_AcctProcessor_ID, trxName);
		if (C_AcctProcessor_ID == 0)
			setInitialDefaults();
	}	//	MAcctProcessor

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setKeepLogDays (7);	// 7
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MAcctProcessor (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MAcctProcessor

	/**
	 * 	Parent Constructor
	 *	@param client parent
	 *	@param Supervisor_ID admin
	 */
	public MAcctProcessor (MClient client, int Supervisor_ID)
	{
		this (client.getCtx(), 0, client.get_TrxName());
		setClientOrg(client);
		StringBuilder msgset = new StringBuilder().append(client.getName()).append(" - ") 
							.append(Msg.translate(getCtx(), "C_AcctProcessor_ID"));
		setName (msgset.toString());
		setSupervisor_ID (Supervisor_ID);
	}	//	MAcctProcessor
	
	@Override
	protected boolean beforeSave(boolean newRecord)
	{
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
			
			long nextWork = MSchedule.getNextRunMS(System.currentTimeMillis(), getScheduleType(), getFrequencyType(),
					getFrequency(), getCronPattern(), timeZoneId);
			if (nextWork > 0)
				setDateNextRun(new Timestamp(nextWork));
		}
		
		return true;
	}	//	beforeSave

	/**
	 * 	Get Server ID
	 *	@return id
	 */
	@Override
	public String getServerID ()
	{
		StringBuilder msgreturn = new StringBuilder("AcctProcessor").append(get_ID());
		return msgreturn.toString();
	}	//	getServerID

	/**
	 * 	Get Date Next Run
	 *	@param requery requery
	 *	@return date next run
	 */
	@Override
	public Timestamp getDateNextRun (boolean requery)
	{
		if (requery)
			load(get_TrxName());
		return getDateNextRun();
	}	//	getDateNextRun

	/**
	 * 	Get Logs
	 *	@return logs
	 */
	@Override
	public AdempiereProcessorLog[] getLogs ()
	{
		String whereClause = "C_AcctProcessor_ID=? ";
		List<MAcctProcessor> list = new Query(getCtx(), I_C_AcctProcessorLog.Table_Name,whereClause,get_TrxName())
		.setParameters(getC_AcctProcessor_ID())
		.setOrderBy("Created DESC")
		.list();
		return list.toArray(new MAcctProcessorLog[list.size()]);		
	}	//	getLogs

	/**
	 * 	Delete old Request Log
	 *	@return number of records
	 */
	public int deleteLog()
	{
		if (getKeepLogDays() < 1)
			return 0;
		StringBuilder sql = new StringBuilder("DELETE FROM C_AcctProcessorLog ")
					.append("WHERE C_AcctProcessor_ID=").append(getC_AcctProcessor_ID()) 
					.append(" AND (Created+").append(getKeepLogDays()).append(") < getDate()");
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		return no;
	}	//	deleteLog

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

}	//	MAcctProcessor
