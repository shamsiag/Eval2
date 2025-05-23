/**********************************************************************
 * This file is part of Adempiere ERP Bazaar                          * 
 * http://www.adempiere.org                                           * 
 *                                                                    * 
 * Copyright (C) Trifon Trifonov.                                     * 
 * Copyright (C) Contributors                                         * 
 *                                                                    * 
 * This program is free software; you can redistribute it and/or      * 
 * modify it under the terms of the GNU General Public License        * 
 * as published by the Free Software Foundation; either version 2     * 
 * of the License, or (at your option) any later version.             * 
 *                                                                    * 
 * This program is distributed in the hope that it will be useful,    * 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of     * 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the       * 
 * GNU General Public License for more details.                       * 
 *                                                                    * 
 * You should have received a copy of the GNU General Public License  * 
 * along with this program; if not, write to the Free Software        * 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,         * 
 * MA 02110-1301, USA.                                                * 
 *                                                                    * 
 * Contributors:                                                      * 
 *  - Trifon Trifonov (trifonnt@users.sourceforge.net)                *
 *                                                                    *
 * Sponsors:                                                          *
 *  - E-evolution (http://www.e-evolution.com/)                       *
 **********************************************************************/
package org.compiere.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Util;

/**
 * Import processor model for replication
 * @author Trifon Trifonov
 */
public class MIMPProcessor extends X_IMP_Processor implements AdempiereProcessor 
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 4477942100661801354L;

	private static CLogger	s_log	= CLogger.getCLogger (MIMPProcessor.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param IMP_Processor_UU  UUID key
     * @param trxName Transaction
     */
    public MIMPProcessor(Properties ctx, String IMP_Processor_UU, String trxName) {
        super(ctx, IMP_Processor_UU, trxName);
		if (Util.isEmpty(IMP_Processor_UU))
			setInitialDefaults();
    }

	public MIMPProcessor(Properties ctx, int EXP_ReplicationProcessor_ID, String trxName) 
	{
		super(ctx, EXP_ReplicationProcessor_ID, trxName);
		if (EXP_ReplicationProcessor_ID == 0)
			setInitialDefaults();
	}
	
	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setName (/*client.getName() + " - " +*/ "Default Import Processor");
		setFrequencyType (FREQUENCYTYPE_Hour);
		setFrequency (1);
		setKeepLogDays (7);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MIMPProcessor(Properties ctx, ResultSet rs, String trxName) 
	{
		super(ctx, rs, trxName);
	}
	
	/**
	 * @param requery
	 * @return next run time stamp
	 */
	public Timestamp getDateNextRun (boolean requery)
	{
		if (requery)
			load(get_TrxName());
		return getDateNextRun();
	}

	/**
	 * 	Get Logs
	 *	@return logs
	 */
	public AdempiereProcessorLog[] getLogs ()
	{
		ArrayList<MIMPProcessorLog> list = new ArrayList<MIMPProcessorLog>();
		String sql = "SELECT * "
			+ "FROM " + X_IMP_ProcessorLog.Table_Name + " "
			+ "WHERE " + X_IMP_Processor.COLUMNNAME_IMP_Processor_ID + "=? " // # 1 
			+ "ORDER BY Created DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getIMP_Processor_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MIMPProcessorLog (getCtx(), rs, get_TrxName()));
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
		MIMPProcessorLog[] retValue = new MIMPProcessorLog[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getLogs

	/**
	 * 	Delete old processor logs
	 *	@return number of records deleted
	 */
	public int deleteLog()
	{
		if (getKeepLogDays() < 1)
			return 0;
		StringBuilder sql = new StringBuilder("DELETE FROM ").append(X_IMP_ProcessorLog.Table_Name).append(" ")
			.append("WHERE ").append(X_IMP_ProcessorLog.COLUMNNAME_IMP_Processor_ID).append("=").append(getIMP_Processor_ID()) 
			.append(" AND (Created+").append(getKeepLogDays()).append(") < getDate()");
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		return no;
	}

	@Override
	public String getServerID() {
		StringBuilder msgreturn = new StringBuilder("ReplicationProcessor").append(get_ID());
		return msgreturn.toString();
	}
	
	/**
	 * @param trxName
	 * @return processor parameters
	 */
	public X_IMP_ProcessorParameter[] getIMP_ProcessorParameters(String trxName) {
		List<X_IMP_ProcessorParameter> resultList = new ArrayList<X_IMP_ProcessorParameter>();
		                   
		StringBuilder sql = new StringBuilder("SELECT * ")
			.append(" FROM ").append(X_IMP_ProcessorParameter.Table_Name)
			.append(" WHERE ").append(X_IMP_ProcessorParameter.COLUMNNAME_IMP_Processor_ID).append("=?") // # 1
			.append(" AND IsActive = ?")  // # 2
		;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		X_IMP_ProcessorParameter processorParameter = null;
		try {
			pstmt = DB.prepareStatement (sql.toString(), trxName);
			pstmt.setInt(1, getIMP_Processor_ID());
			pstmt.setString(2, "Y");
			rs = pstmt.executeQuery ();
			while ( rs.next() ) {
				processorParameter = new X_IMP_ProcessorParameter (getCtx(), rs, trxName);
				resultList.add(processorParameter);
			}
		} catch (SQLException e) {
			s_log.log(Level.SEVERE, sql.toString(), e);
		} finally{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		X_IMP_ProcessorParameter[] result = (X_IMP_ProcessorParameter[])resultList.toArray( new X_IMP_ProcessorParameter[0]);
		return result;
	}
	
	/**
	 * @param ctx
	 * @return active import processors
	 */
	public static MIMPProcessor[] getActive(Properties ctx)
	{
		ArrayList<MIMPProcessor> list = new ArrayList<MIMPProcessor>();
		String sql = "SELECT * FROM "+X_IMP_Processor.Table_Name+" WHERE IsActive='Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MIMPProcessor (ctx, rs, null));

		}
		catch (Exception e)
		{
			s_log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		MIMPProcessor[] retValue = new MIMPProcessor[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getActive

	@Override
	public String getScheduleType() {
		return MSchedule.SCHEDULETYPE_Frequency;
	}
	
	@Override
	public String getCronPattern() {
	   return null;
	}

}
