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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 *	Request Processor Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MRequestProcessor.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
public class MRequestProcessor extends X_R_RequestProcessor 
	implements AdempiereProcessor, AdempiereProcessor2
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 8231854734466233461L;

	/**
	 * 	Get Active Request Processors
	 *	@param ctx context
	 *	@return array of MRequestProcessor 
	 */
	public static MRequestProcessor[] getActive (Properties ctx)
	{
		ArrayList<MRequestProcessor> list = new ArrayList<MRequestProcessor>();
		String sql = "SELECT * FROM R_RequestProcessor WHERE IsActive='Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MRequestProcessor (ctx, rs, null));
		}
		catch (Exception e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		MRequestProcessor[] retValue = new MRequestProcessor[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getActive
	
	/**	Static Logger	*/
	private static CLogger	s_log	= CLogger.getCLogger (MRequestProcessor.class);

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param R_RequestProcessor_UU  UUID key
     * @param trxName Transaction
     */
    public MRequestProcessor(Properties ctx, String R_RequestProcessor_UU, String trxName) {
        super(ctx, R_RequestProcessor_UU, trxName);
		if (Util.isEmpty(R_RequestProcessor_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param R_RequestProcessor_ID id
	 */
	public MRequestProcessor (Properties ctx, int R_RequestProcessor_ID, String trxName)
	{
		super (ctx, R_RequestProcessor_ID, trxName);
		if (R_RequestProcessor_ID == 0)
			setInitialDefaults();
	}	//	MRequestProcessor

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setKeepLogDays (7);
		setOverdueAlertDays (0);
		setOverdueAssignDays (0);
		setRemindDays (0);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 */
	public MRequestProcessor (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MRequestProcessor

	/**
	 * 	Parent Constructor
	 *	@param parent parent
	 *	@param Supervisor_ID Supervisor
	 */
	public MRequestProcessor (MClient parent, int Supervisor_ID)
	{
		this (parent.getCtx(), 0, parent.get_TrxName());
		setClientOrg(parent);
		setName (parent.getName() + " - " 
			+ Msg.translate(getCtx(), "R_RequestProcessor_ID"));
		setSupervisor_ID (Supervisor_ID);
	}	//	MRequestProcessor
	
	
	/**	The Routes						*/
	private MRequestProcessorRoute[]	m_routes = null;

	/**
	 * 	Get Routes
	 *	@param reload true to reload from DB
	 *	@return array of routes
	 */
	public MRequestProcessorRoute[] getRoutes (boolean reload)
	{
		if (m_routes != null && !reload)
			return m_routes;
		
		String sql = "SELECT * FROM R_RequestProcessor_Route WHERE R_RequestProcessor_ID=? ORDER BY SeqNo";
		ArrayList<MRequestProcessorRoute> list = new ArrayList<MRequestProcessorRoute>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getR_RequestProcessor_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MRequestProcessorRoute (getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//
		m_routes = new MRequestProcessorRoute[list.size ()];
		list.toArray (m_routes);
		return m_routes;
	}	//	getRoutes
	
	/**
	 * 	Get Logs
	 *	@return Array of Logs
	 */
	public AdempiereProcessorLog[] getLogs()
	{
		ArrayList<MRequestProcessorLog> list = new ArrayList<MRequestProcessorLog>();
		String sql = "SELECT * "
			+ "FROM R_RequestProcessorLog "
			+ "WHERE R_RequestProcessor_ID=? " 
			+ "ORDER BY Created DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getR_RequestProcessor_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MRequestProcessorLog (getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		MRequestProcessorLog[] retValue = new MRequestProcessorLog[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getLogs
	
	/**
	 * 	Delete old Request Log
	 *	@return number of records
	 */
	public int deleteLog()
	{
		if (getKeepLogDays() < 1)
			return 0;
		String sql = "DELETE FROM R_RequestProcessorLog "
			+ "WHERE R_RequestProcessor_ID=" + getR_RequestProcessor_ID() 
			+ " AND (Created+" + getKeepLogDays() + ") < getDate()";
		int no = DB.executeUpdate(sql, get_TrxName());
		return no;
	}	//	deleteLog
	
	/**
	 * 	Get next run date
	 * 	@param requery true to re-query database
	 * 	@return next run date
	 */
	public Timestamp getDateNextRun (boolean requery)
	{
		if (requery)
			load(get_TrxName());
		return getDateNextRun();
	}	//	getDateNextRun

	/**
	 * 	Get Unique Server ID
	 *	@return Unique Server ID
	 */
	public String getServerID()
	{
		return "RequestProcessor" + get_ID();
	}	//	getServerID

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
			long nextWork = MSchedule.getNextRunMS(System.currentTimeMillis(), getScheduleType(), getFrequencyType(), getFrequency(), getCronPattern(),
					timeZoneId);
			if (nextWork > 0)
				setDateNextRun(new Timestamp(nextWork));
		}
		
		return true;
	}	//	beforeSave

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

}	//	MRequestProcessor
