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

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Performance Goal
 *	
 *  @author Jorg Janke
 *  @version $Id: MGoal.java,v 1.2 2006/07/30 00:51:03 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>BF [ 1887674 ] Deadlock when try to modify PA Goal's Measure Target
 * 			<li>BF [ 1760482 ] New Dashboard broke old functionality
 * 			<li>BF [ 1887691 ] I get NPE if the PA Goal's target is 0
 */
public class MGoal extends X_PA_Goal
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -4612113288233473730L;

	/**
	 * 	Get User Goals (will call updateGoal for each record)
	 *	@param ctx context
	 *	@param AD_User_ID user
	 *	@return array of goals
	 */
	public static MGoal[] getUserGoals(Properties ctx, int AD_User_ID)
	{
		if (AD_User_ID < 0)
			return getTestGoals(ctx);
		ArrayList<MGoal> list = new ArrayList<MGoal>();
		String sql = "SELECT * FROM PA_Goal g "
			+ "WHERE IsActive='Y'"
			+ " AND AD_Client_ID=?"		//	#1
			+ " AND ((AD_User_ID IS NULL AND AD_Role_ID IS NULL)"
				+ " OR AD_User_ID=?"	//	#2
				+ " OR EXISTS (SELECT * FROM AD_User_Roles ur "
					+ "WHERE ur.AD_User_ID=? AND g.AD_Role_ID=ur.AD_Role_ID AND ur.IsActive='Y')) "
			+ "ORDER BY SeqNo";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, Env.getAD_Client_ID(ctx));
			pstmt.setInt (2, AD_User_ID);
			pstmt.setInt (3, AD_User_ID);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MGoal goal = new MGoal (ctx, rs, null);
				goal.updateGoal(false);
				list.add (goal);
			}
		}
		catch (Exception e)
		{
			s_log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		if (list.size() == 0)
			s_log.log (Level.INFO, Msg.getMsg(ctx, "FindZeroRecords"));
		MGoal[] retValue = new MGoal[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getUserGoals

	/**
	 * 	Get Accessible Goals ((will call updateGoal for each record)
	 *	@param ctx context
	 *	@return array of goals
	 */
	public static MGoal[] getGoals(Properties ctx)
	{
		List<MGoal> list = new Query(ctx,I_PA_Goal.Table_Name,null,null)
		.setOrderBy("SeqNo")
		.setApplyAccessFilter(false,true)
		.setOnlyActiveRecords(true)
		.list();
		for(MGoal goal:list)
				goal.updateGoal(false);
			
		MGoal[] retValue = new MGoal[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getGoals
	
	/**
	 * 	Create Dummy Test Goals
	 *	@param ctx context
	 *	@return array of goals
	 */
	public static MGoal[] getTestGoals(Properties ctx)
	{
		MGoal[] retValue = new MGoal[4];
		retValue[0] = new MGoal (ctx, "Test 1", "Description 1", new BigDecimal (1000), null); 
		retValue[0].setMeasureActual(new BigDecimal (200)); 
		retValue[1] = new MGoal (ctx, "Test 2", "Description 2", new BigDecimal (1000), null); 
		retValue[1].setMeasureActual(new BigDecimal (900)); 
		retValue[2] = new MGoal (ctx, "Test 3", "Description 3", new BigDecimal (1000), null); 
		retValue[2].setMeasureActual(new BigDecimal (1200)); 
		retValue[3] = new MGoal (ctx, "Test 4", "Description 4", new BigDecimal (1000), null); 
		retValue[3].setMeasureActual(new BigDecimal (3200)); 
		return retValue;
	}	//	getTestGoals

	/**
	 * 	Get Goals for a performance measurement
	 *	@param ctx context
	 *	@param PA_Measure_ID performance measurement
	 *	@return goals
	 */
	public static MGoal[] getMeasureGoals (Properties ctx, int PA_Measure_ID)
	{
		ArrayList<MGoal> list = new ArrayList<MGoal>();
		String sql = "SELECT * FROM PA_Goal WHERE IsActive='Y' AND PA_Measure_ID=? "
			+ "ORDER BY SeqNo";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, PA_Measure_ID);
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MGoal (ctx, rs, null));
		}
		catch (Exception e)
		{
			s_log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		MGoal[] retValue = new MGoal[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getMeasureGoals
	
	/**
	 * 	Get Multiplier for the goal's combination of Measurement Scope and Measurement Display
	 *	@param goal goal
	 *	@return multiplier value or 1 (measure display is null) or null (for MEASURESCOPE_Total and MEASUREDISPLAY_Total)
	 */
	public static BigDecimal getMultiplier (MGoal goal)
	{
		String MeasureScope = goal.getMeasureScope();
		String MeasureDisplay = goal.getMeasureDisplay();
		if (MeasureDisplay == null
			|| MeasureScope.equals(MeasureDisplay))
			return Env.ONE;		//	1:1
		
		if (MeasureScope.equals(MEASURESCOPE_Total) 
			||  MeasureDisplay.equals(MEASUREDISPLAY_Total))
			return null;		//	Error

		BigDecimal Multiplier = null;
		if (MeasureScope.equals(MEASURESCOPE_Year))
		{
			if (MeasureDisplay.equals(MEASUREDISPLAY_Quarter))
				Multiplier = BigDecimal.valueOf(1.0/4.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Month))
				Multiplier = BigDecimal.valueOf(1.0/12.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Week))
				Multiplier = BigDecimal.valueOf(1.0/52.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Day))
				Multiplier = BigDecimal.valueOf(1.0/364.0);
		}
		else if (MeasureScope.equals(MEASURESCOPE_Quarter))
		{
			if (MeasureDisplay.equals(MEASUREDISPLAY_Year))
				Multiplier = BigDecimal.valueOf(4.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Month))
				Multiplier = BigDecimal.valueOf(1.0/3.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Week))
				Multiplier = BigDecimal.valueOf(1.0/13.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Day))
				Multiplier = BigDecimal.valueOf(1.0/91.0);
		}
		else if (MeasureScope.equals(MEASURESCOPE_Month))
		{
			if (MeasureDisplay.equals(MEASUREDISPLAY_Year))
				Multiplier = BigDecimal.valueOf(12.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Quarter))
				Multiplier = BigDecimal.valueOf(3.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Week))
				Multiplier = BigDecimal.valueOf(1.0/4.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Day))
				Multiplier = BigDecimal.valueOf(1.0/30.0);
		}
		else if (MeasureScope.equals(MEASURESCOPE_Week))
		{
			if (MeasureDisplay.equals(MEASUREDISPLAY_Year))
				Multiplier = BigDecimal.valueOf(52.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Quarter))
				Multiplier = BigDecimal.valueOf(13.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Month))
				Multiplier = BigDecimal.valueOf(4.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Day))
				Multiplier = BigDecimal.valueOf(1.0/7.0);
		}
		else if (MeasureScope.equals(MEASURESCOPE_Day))
		{
			if (MeasureDisplay.equals(MEASUREDISPLAY_Year))
				Multiplier = BigDecimal.valueOf(364.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Quarter))
				Multiplier = BigDecimal.valueOf(91.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Month))
				Multiplier = BigDecimal.valueOf(30.0);
			else if (MeasureDisplay.equals(MEASUREDISPLAY_Week))
				Multiplier = BigDecimal.valueOf(7.0);
		}
		return Multiplier;
	}	//	getMultiplier
	
	/**	Logger	*/
	private static CLogger s_log = CLogger.getCLogger (MGoal.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param PA_Goal_UU  UUID key
     * @param trxName Transaction
     */
    public MGoal(Properties ctx, String PA_Goal_UU, String trxName) {
        super(ctx, PA_Goal_UU, trxName);
		if (Util.isEmpty(PA_Goal_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param PA_Goal_ID id
	 *	@param trxName trx
	 */
	public MGoal (Properties ctx, int PA_Goal_ID, String trxName)
	{
		super (ctx, PA_Goal_ID, trxName);
		if (PA_Goal_ID == 0)
			setInitialDefaults();
	}	//	MGoal

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setSeqNo (0);
		setIsSummary (false);
		setMeasureScope (MEASUREDISPLAY_Year);
		setGoalPerformance (Env.ZERO);
		setRelativeWeight (Env.ONE);
		setMeasureTarget (Env.ZERO);
		setMeasureActual (Env.ZERO);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName trx
	 */
	public MGoal (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MGoal

	/**
	 *	@param ctx context
	 *	@param Name Name
	 *	@param Description Description
	 *	@param MeasureTarget target
	 *	@param trxName trx
	 */
	public MGoal (Properties ctx, String Name, String Description,
		BigDecimal MeasureTarget, String trxName)
	{
		super (ctx, 0, trxName);
		setName(Name);
		setDescription(Description);
		setMeasureTarget(MeasureTarget);
	}	//	MGoal
	
	/** Restrictions					*/
	private MGoalRestriction[] 	m_restrictions = null;
	/** Performance Color				*/
	private Color				m_color = null;

	/**
	 * 	Get Restriction Lines
	 *	@param reload true to reload data
	 *	@return array of lines
	 */
	public MGoalRestriction[] getRestrictions (boolean reload)
	{
		if (m_restrictions != null && !reload)
			return m_restrictions;
		ArrayList<MGoalRestriction> list = new ArrayList<MGoalRestriction>();
		//
		String sql = "SELECT * FROM PA_GoalRestriction "
			+ "WHERE PA_Goal_ID=? AND IsActive='Y' "
			+ "ORDER BY Org_ID, C_BPartner_ID, M_Product_ID";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getPA_Goal_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MGoalRestriction (getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		//
		m_restrictions = new MGoalRestriction[list.size ()];
		list.toArray (m_restrictions);
		return m_restrictions;
	}	//	getRestrictions

	/**
	 * 	Get Measure
	 *	@return measure or null
	 */
	public MMeasure getMeasure()
	{
		if (getPA_Measure_ID() != 0)
			return MMeasure.get(getPA_Measure_ID());
		return null;
	}	//	getMeasure
		
	/**
	 * 	Update/save measurement for Goals
	 * 	@param force force to update goal even if it has not reach the next update interval (default is 30 minutes interval)
	 * 	@return true if updated
	 */
	public boolean updateGoal(boolean force)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Force=" + force);
		if (Env.isReadOnlySession())
			return false;
		MMeasure measure = MMeasure.get(getPA_Measure_ID());
		
		boolean isUpdateByInterfal = false;
		if (getDateLastRun() != null){
			// default 30 minute 1800000
			long interval = MSysConfig.getIntValue(MSysConfig.ZK_DASHBOARD_PERFORMANCE_REFRESH_INTERVAL, 1800000, Env.getAD_Client_ID(Env.getCtx()));
			isUpdateByInterfal = (System.currentTimeMillis() - getDateLastRun().getTime()) > interval;
		}
		
		if (force 
			|| getDateLastRun() == null
			|| isUpdateByInterfal)
		{
			measure = new MMeasure(Env.getCtx(), measure, get_TrxName());
			if (measure.updateGoals())		//	saves
			{
				load(get_ID(), get_TrxName());
				return true;
			}
		}
		return false;
	}	//	updateGoal
	
	/**
	 * 	Set Actual Measurement value
	 *	@param MeasureActual actual
	 */
	@Override
	public void setMeasureActual (BigDecimal MeasureActual)
	{
		if (MeasureActual == null)
			return;
		super.setMeasureActual (MeasureActual);
		setDateLastRun(new Timestamp(System.currentTimeMillis()));
		setGoalPerformance();
	}	//	setMeasureActual
	
	/**
	 * Calculate Performance Goal
	 */
	public void setGoalPerformance ()
	{
		BigDecimal MeasureTarget = getMeasureTarget();
		BigDecimal MeasureActual = getMeasureActual();
		BigDecimal GoalPerformance = Env.ZERO;
		if (MeasureTarget.signum() != 0)
			GoalPerformance = MeasureActual.divide(MeasureTarget, 6, RoundingMode.HALF_UP);
		super.setGoalPerformance (GoalPerformance);
		m_color = null;
	}	//	setGoalPerformance
	
	/**
	 * 	Get Goal Performance value as Double
	 *	@return goal performance value
	 */
	public double getGoalPerformanceDouble()
	{
		BigDecimal bd = getGoalPerformance();
		return bd.doubleValue();
	}	//	getGoalPerformanceDouble
	
	/**
	 * 	Get Goal Performance value in Percent
	 *	@return goal performance value in percent (i.e 5 percent if value is 0.05)
	 */
	public int getPercent()
	{
		BigDecimal bd = getGoalPerformance().multiply(Env.ONEHUNDRED);
		return bd.intValue();
	}	//	getPercent

	/**
	 * 	Get Color
	 *	@return color - white if no target
	 */
	public Color getColor()
	{
		if (m_color == null)
		{
			if (getMeasureTarget().signum() == 0)
				m_color = Color.white;
			else
				m_color = MColorSchema.getColor(getCtx(), getPA_ColorSchema_ID(), getPercent());
		}
		return m_color;
	}	//	getColor
	
    /**
     * Get the color schema for this goal.
     * @return the color schema
     */
    public MColorSchema getColorSchema()
    {
    	return MColorSchema.getCopy(getCtx(), getPA_ColorSchema_ID(), get_TrxName());
    }
	
	/**
	 * 	Get Measure Display
	 *	@return Measure Display (MEASUREDISPLAY_*)
	 */
	public String getMeasureDisplay ()
	{
		String s = super.getMeasureDisplay ();
		if (s == null)
		{
			if (MEASURESCOPE_Week.equals(getMeasureScope()))
				s = MEASUREDISPLAY_Week;
			else if (MEASURESCOPE_Day.equals(getMeasureScope()))
				s = MEASUREDISPLAY_Day;
			else
				s = MEASUREDISPLAY_Month;
		}
		return s;
	}	//	getMeasureDisplay
	
	/**
	 * 	Get Measure Display Text
	 *	@return Measure Display Text for X axis
	 */
	public String getXAxisText ()
	{
		MMeasure measure = getMeasure();
		if (measure != null 
			&& MMeasure.MEASUREDATATYPE_StatusQtyAmount.equals(measure.getMeasureDataType()))
		{
			if (MMeasure.MEASURETYPE_Request.equals(measure.getMeasureType()))
				return Msg.getElement(getCtx(), "R_Status_ID");
			if (MMeasure.MEASURETYPE_Project.equals(measure.getMeasureType()))
				return Msg.getElement(getCtx(), "C_Phase_ID");
		}
		String value = getMeasureDisplay();
		String display = MRefList.getListName(getCtx(), MEASUREDISPLAY_AD_Reference_ID, value);
		return display==null ? value : display;
	}	//	getMeasureDisplayText
	
	/**
	 * 	Goal has Target
	 *	@return true if has measurement target value
	 */
	public boolean isTarget()
	{
		return getMeasureTarget().signum() != 0;
	}	//	isTarget
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MGoal[");
		sb.append (get_ID ())
			.append ("-").append (getName())
			.append(",").append(getGoalPerformance())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		//	Measure required if not Summary
		if (!isSummary() && getPA_Measure_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "PA_Measure_ID"));
			return false;
		}
		if (isSummary() && getPA_Measure_ID() != 0)
			setPA_Measure_ID(0);
		
		//	Validate user and role
		if ((newRecord || is_ValueChanged("AD_User_ID") || is_ValueChanged("AD_Role_ID"))
			&& getAD_User_ID() != 0)
		{
			MUser user = MUser.get(getCtx(), getAD_User_ID());
			MRole[] roles = user.getRoles(getAD_Org_ID());
			if (roles.length == 0)		//	No Role
				setAD_Role_ID(0);
			else if (roles.length == 1)	//	One
				setAD_Role_ID(roles[0].getAD_Role_ID());
			else
			{
				int AD_Role_ID = getAD_Role_ID();
				if (AD_Role_ID != 0)	//	validate
				{
					boolean found = false;
					for (int i = 0; i < roles.length; i++)
					{
						if (AD_Role_ID == roles[i].getAD_Role_ID())
						{
							found = true;
							break;
						}
					}
					if (!found)
						AD_Role_ID = 0;
				}
				if (AD_Role_ID == 0)		//	set to first one
					setAD_Role_ID(roles[0].getAD_Role_ID());
			}	//	multiple roles
		}	//	user check

		return true;
	}	//	beforeSave

	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		
		//	Update Goal if Target / Scope Changed
		if (newRecord 
			|| is_ValueChanged("MeasureTarget") 
			|| is_ValueChanged("MeasureScope"))
			updateGoal(true);

		return success;
	}
		
}	//	MGoal
