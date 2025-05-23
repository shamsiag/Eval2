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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * 	Project Type Model
 *
 *	@author Jorg Janke
 *	@version $Id: MProjectType.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MProjectType extends X_C_ProjectType implements ImmutablePOSupport
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = 2378841146277294989L;

	/**
	 * 	Get MProjectType from Cache (immutable)
	 *	@param C_ProjectType_ID id
	 *	@return MProjectType
	 */
	public static MProjectType get (int C_ProjectType_ID)
	{
		return get(Env.getCtx(), C_ProjectType_ID);
	}

	/**
	 * 	Get MProjectType from Cache (immutable)
	 *	@param ctx context
	 *	@param C_ProjectType_ID id
	 *	@return MProjectType
	 */
	public static MProjectType get (Properties ctx, int C_ProjectType_ID)
	{
		Integer key = Integer.valueOf(C_ProjectType_ID);
		MProjectType retValue = s_cache.get (ctx, key, e -> new MProjectType(ctx, e));
		if (retValue != null)
			return retValue;
		retValue = new MProjectType (ctx, C_ProjectType_ID, (String)null);
		if (retValue.get_ID() == C_ProjectType_ID)
		{
			s_cache.put (key, retValue, e -> new MProjectType(Env.getCtx(), e));
			return retValue;
		}
		return null;
	} //	get

	/**	Cache						*/
	private static ImmutableIntPOCache<Integer, MProjectType> s_cache 
		= new ImmutableIntPOCache<Integer, MProjectType> (Table_Name, 20);
		
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_ProjectType_UU  UUID key
     * @param trxName Transaction
     */
    public MProjectType(Properties ctx, String C_ProjectType_UU, String trxName) {
        super(ctx, C_ProjectType_UU, trxName);
    }

	/**
	 * 	Standrad Constructor
	 *	@param ctx context
	 *	@param C_ProjectType_ID id
	 *	@param trxName trx
	 */
	public MProjectType (Properties ctx, int C_ProjectType_ID, String trxName)
	{
		super (ctx, C_ProjectType_ID, trxName);
	}	//	MProjectType

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName trx
	 */
	public MProjectType (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProjectType

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MProjectType(MProjectType copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MProjectType(Properties ctx, MProjectType copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MProjectType(Properties ctx, MProjectType copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 * 	String Representation
	 *	@return	info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder ("MProjectType[")
			.append(get_ID())
			.append("-").append(getName())
			.append("]");
		return sb.toString();
	}	//	toString
	
	/**
	 * 	Get Project Type Phases
	 *	@return Array of MProjectTypePhase
	 */
	public MProjectTypePhase[] getPhases()
	{
		ArrayList<MProjectTypePhase> list = new ArrayList<MProjectTypePhase>();
		String sql = "SELECT * FROM C_Phase WHERE C_ProjectType_ID=? AND IsActive='Y' ORDER BY SeqNo, C_Phase_ID";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getC_ProjectType_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add(new MProjectTypePhase (getCtx(), rs, get_TrxName()));
		}
		catch (SQLException ex)
		{
			log.log(Level.SEVERE, sql, ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//
		MProjectTypePhase[] retValue = new MProjectTypePhase[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getPhases

	/**
	 * 	Get SQL for Performance Indicator
	 *	@param restrictions array of goal restrictions
	 *	@param MeasureScope measurement display scope (MGoal.MEASUREDISPLAY_*)  
	 *	@param MeasureDataType measurement data type (MMeasure.MEASUREDATATYPE_*)
	 *	@param reportDate optional report date, null to use current date
	 *	@param role role
	 *	@return SQL for performance indicator
	 */
	public String getSqlPI (MGoalRestriction[] restrictions, 
		String MeasureScope, String MeasureDataType, Timestamp reportDate, MRole role)
	{
		String dateColumn = "Created";
		String orgColumn = "AD_Org_ID";
		String bpColumn = "C_BPartner_ID";
		String pColumn = null;
		//	PlannedAmt -> PlannedQty -> Count
		StringBuilder sb = new StringBuilder("SELECT COALESCE(SUM(PlannedAmt),COALESCE(SUM(PlannedQty),COUNT(*))) "
			+ "FROM C_Project WHERE C_ProjectType_ID=" + getC_ProjectType_ID()
			+ " AND Processed<>'Y')");
		//	Date Restriction
		
		if (MMeasure.MEASUREDATATYPE_QtyAmountInTime.equals(MeasureDataType)
			&& !MGoal.MEASUREDISPLAY_Total.equals(MeasureScope))
		{
			if (reportDate == null)
				reportDate = new Timestamp(System.currentTimeMillis());
			@SuppressWarnings("unused")
			String dateString = DB.TO_DATE(reportDate);
			String trunc = "D";
			if (MGoal.MEASUREDISPLAY_Year.equals(MeasureScope))
				trunc = "Y";
			else if (MGoal.MEASUREDISPLAY_Quarter.equals(MeasureScope))
				trunc = "Q";
			else if (MGoal.MEASUREDISPLAY_Month.equals(MeasureScope))
				trunc = "MM";
			else if (MGoal.MEASUREDISPLAY_Week.equals(MeasureScope))
				trunc = "W";
			sb.append(" AND TRUNC(")
				.append(dateColumn).append(",'").append(trunc).append("')=TRUNC(")
				.append(DB.TO_DATE(reportDate)).append(",'").append(trunc).append("')");
		}	//	date
		//
		String sql = MMeasureCalc.addRestrictions(sb.toString(), false, restrictions, role, 
			"C_Project", orgColumn, bpColumn, pColumn);
		
		log.fine(sql);
		return sql;
	}	//	getSql
	
	/**
	 * 	Get SQL for bar chart
	 *	@param restrictions array of goal restrictions
	 *	@param MeasureDisplay measurement display scope (MGoal.MEASUREDISPLAY_*)  
	 *	@param MeasureDataType measurement data type (MMeasure.MEASUREDATATYPE_*)
	 *	@param startDate optional report start date
	 *	@param role role
	 *	@return SQL for Bar Chart
	 */
	public String getSqlBarChart (MGoalRestriction[] restrictions, 
		String MeasureDisplay, String MeasureDataType, 
		Timestamp startDate, MRole role)
	{
		String dateColumn = "Created";
		String orgColumn = "AD_Org_ID";
		String bpColumn = "C_BPartner_ID";
		String pColumn = null;
		//
		StringBuilder sb = new StringBuilder("SELECT COALESCE(SUM(PlannedAmt),COALESCE(SUM(PlannedQty),COUNT(*))), ");
		String orderBy = null;
		String groupBy = null;
		//
		if (MMeasure.MEASUREDATATYPE_QtyAmountInTime.equals(MeasureDataType)
			&& !MGoal.MEASUREDISPLAY_Total.equals(MeasureDisplay))
		{
			String trunc = "D";
			if (MGoal.MEASUREDISPLAY_Year.equals(MeasureDisplay))
				trunc = "Y";
			else if (MGoal.MEASUREDISPLAY_Quarter.equals(MeasureDisplay))
				trunc = "Q";
			else if (MGoal.MEASUREDISPLAY_Month.equals(MeasureDisplay))
				trunc = "MM";
			else if (MGoal.MEASUREDISPLAY_Week.equals(MeasureDisplay))
				trunc = "W";
			orderBy = "TRUNC(" + dateColumn + ",'" + trunc + "')";
			groupBy = orderBy + ", 0 ";
			sb.append(groupBy)
				.append("FROM C_Project ");
		}
		else
		{
			orderBy = "p.SeqNo"; 
			groupBy = "NVL(p.Name,'-'), p.C_Phase_ID, p.SeqNo ";
			sb.append(groupBy)
				.append("FROM C_Project LEFT OUTER JOIN C_Phase p ON (C_Project.C_Phase_ID=p.C_Phase_ID) ");
		}
		//	Where
		sb.append("WHERE C_Project.C_ProjectType_ID=").append(getC_ProjectType_ID())
			.append(" AND C_Project.Processed<>'Y'");
		//	Date Restriction
		if (startDate != null
			&& !MGoal.MEASUREDISPLAY_Total.equals(MeasureDisplay))
		{
			String dateString = DB.TO_DATE(startDate);
			sb.append(" AND ").append(dateColumn)
				.append(">=").append(dateString);
		}	//	date
		//
		String sql = MMeasureCalc.addRestrictions(sb.toString(), false, restrictions, role, 
			"C_Project", orgColumn, bpColumn, pColumn);
		if (groupBy != null)
			sql += " GROUP BY " + groupBy + " ORDER BY " + orderBy;
		//
		log.fine(sql);
		return sql;
	}	//	getSqlBarChart
	
	/**
	 * 	Get Zoom Query
	 * 	@param restrictions restrictions
	 * 	@param MeasureDisplay display
	 * 	@param date date
	 * 	@param C_Phase_ID phase
	 * 	@param role role 
	 *	@return query
	 */
	public MQuery getQuery(MGoalRestriction[] restrictions, 
		String MeasureDisplay, Timestamp date, int C_Phase_ID, MRole role)
	{
		String dateColumn = "Created";
		String orgColumn = "AD_Org_ID";
		String bpColumn = "C_BPartner_ID";
		String pColumn = null;
		//
		MQuery query = new MQuery("C_Project");
		query.addRangeRestriction("C_ProjectType_ID", "=", getC_ProjectType_ID());
		//
		String where = null;
		if (C_Phase_ID != 0)
			where = "C_Phase_ID=" + C_Phase_ID;
		else
		{
			String trunc = "D";
			if (MGoal.MEASUREDISPLAY_Year.equals(MeasureDisplay))
				trunc = "Y";
			else if (MGoal.MEASUREDISPLAY_Quarter.equals(MeasureDisplay))
				trunc = "Q";
			else if (MGoal.MEASUREDISPLAY_Month.equals(MeasureDisplay))
				trunc = "MM";
			else if (MGoal.MEASUREDISPLAY_Week.equals(MeasureDisplay))
				trunc = "W";

			where = "TRUNC(" + dateColumn + ",'" + trunc
				+ "')=TRUNC(" + DB.TO_DATE(date) + ",'" + trunc + "')";
		}
		String sql = MMeasureCalc.addRestrictions(where + " AND Processed<>'Y' ",
			true, restrictions, role, 
			"C_Project", orgColumn, bpColumn, pColumn);
		query.addRestriction(sql);
		query.setRecordCount(1);
		return query;
	}	//	getQuery

	@Override
	public MProjectType markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MProjectType
