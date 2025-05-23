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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.script.ScriptEngine;

import org.adempiere.apps.graph.GraphColumn;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.MeasureInterface;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * 	Performance Measure
 *	
 *  @author Jorg Janke
 *  @version $Id: MMeasure.java,v 1.2 2006/07/30 00:51:05 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>BF [ 1887674 ] Deadlock when try to modify PA Goal's Measure Target
 * @author victor.perez@e-evolution.com, www.e-evolution.com
 * 			<li>FR [ 2905227 ] Calculate Measure based on the script to PA
 * 			<li>https://sourceforge.net/p/adempiere/feature-requests/888/
 */
public class MMeasure extends X_PA_Measure implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -3584012092877837973L;

	/**
	 * 	Get MMeasure from Cache (immutable)
	 *	@param PA_Measure_ID id
	 *	@return MMeasure
	 */
	public static MMeasure get (int PA_Measure_ID)
	{
		return get(Env.getCtx(), PA_Measure_ID);
	}
	
	/**
	 * 	Get MMeasure from Cache (immutable)
	 *  @param ctx context
	 *	@param PA_Measure_ID id
	 *	@return MMeasure
	 */
	public static MMeasure get (Properties ctx, int PA_Measure_ID)
	{
		Integer key = Integer.valueOf(PA_Measure_ID);
		MMeasure retValue = s_cache.get (ctx, key, e -> new MMeasure(ctx, e));
		if (retValue != null)
			return retValue;
		retValue = new MMeasure (ctx, PA_Measure_ID, (String)null);
		if (retValue.get_ID() == PA_Measure_ID)
		{
			s_cache.put (key, retValue, e -> new MMeasure(Env.getCtx(), e));
			return retValue;
		}
		return null;
	} //	get

	/**	Cache						*/
	private static ImmutableIntPOCache<Integer, MMeasure> s_cache 
		= new ImmutableIntPOCache<Integer, MMeasure> (Table_Name, 10);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param PA_Measure_UU  UUID key
     * @param trxName Transaction
     */
    public MMeasure(Properties ctx, String PA_Measure_UU, String trxName) {
        super(ctx, PA_Measure_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param PA_Measure_ID id
	 *	@param trxName trx
	 */
	public MMeasure (Properties ctx, int PA_Measure_ID, String trxName)
	{
		super (ctx, PA_Measure_ID, trxName);
	}	//	MMeasure

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName trx
	 */
	public MMeasure (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MMeasure

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MMeasure(MMeasure copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MMeasure(Properties ctx, MMeasure copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MMeasure(Properties ctx, MMeasure copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 * Get chart columns
	 * @param goal
	 * @return list of GraphColumn
	 */
	public ArrayList<GraphColumn> getGraphColumnList(MGoal goal)
	{
		ArrayList<GraphColumn> list = new ArrayList<GraphColumn>();
		if (MMeasure.MEASURETYPE_Calculated.equals(getMeasureType()))
		{
			MMeasureCalc mc = MMeasureCalc.get(getPA_MeasureCalc_ID());
			String sql = mc.getSqlBarChart(goal.getRestrictions(false),
					goal.getMeasureDisplay(), goal.getDateFrom(),
					MRole.getDefault());	//	logged in role
			if (sql.indexOf("@") >= 0)
				sql = Env.parseContext(getCtx(), 0, sql, false, false);
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql, null);
				rs = pstmt.executeQuery ();
				ArrayList<Timestamp> dataList = new ArrayList<Timestamp>();
				while (rs.next ())
				{
					BigDecimal data = rs.getBigDecimal(1);
					Timestamp date = rs.getTimestamp(2);
					GraphColumn bgc = new GraphColumn(mc, data);
					bgc.setLabel(date, goal.getMeasureDisplay()); //TODO copy order-loop to other measures
					int pos=0;
					for (int i = 0; i <  dataList.size(); i++)
						if (dataList.get(i).before(date)) pos++;
					dataList.add(date); // list of dates
					list.add(pos, bgc);
				}
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
		}
		else if (MMeasure.MEASURETYPE_Achievements.equals(getMeasureType()))
		{
			if (MMeasure.MEASUREDATATYPE_StatusQtyAmount.equals(getMeasureDataType()))
			{
				MAchievement[] achievements = MAchievement.get(this);
				for (int i = 0; i < achievements.length; i++)
				{
					MAchievement achievement = achievements[i];
					GraphColumn bgc = new GraphColumn(achievement);
					list.add(bgc);
				}
			}
			else	//	MMeasure.MEASUREDATATYPE_QtyAmountInTime
			{
				String MeasureDisplay = goal.getMeasureDisplay();
				String trunc = "D";
				if (MGoal.MEASUREDISPLAY_Year.equals(MeasureDisplay))
					trunc = "Y";
				else if (MGoal.MEASUREDISPLAY_Quarter.equals(MeasureDisplay))
					trunc = "Q";
				else if (MGoal.MEASUREDISPLAY_Month.equals(MeasureDisplay))
					trunc = "MM";
				else if (MGoal.MEASUREDISPLAY_Week.equals(MeasureDisplay))
					trunc = "W";
				//		trunc = "D";
				trunc = "TRUNC(DateDoc,'" + trunc + "')";
				StringBuilder sql = new StringBuilder ("SELECT SUM(ManualActual), ")
				.append(trunc).append(" FROM PA_Achievement WHERE PA_Measure_ID=? AND IsAchieved='Y' ")
				.append("GROUP BY ").append(trunc)
				.append(" ORDER BY ").append(trunc);
				PreparedStatement pstmt = null;
				ResultSet rs = null;
				try
				{
					pstmt = DB.prepareStatement (sql.toString(), null);
					pstmt.setInt(1, getPA_Measure_ID());
					rs = pstmt.executeQuery ();
					while (rs.next ())
					{
						BigDecimal data = rs.getBigDecimal(1);
						Timestamp date = rs.getTimestamp(2);
						GraphColumn bgc = new GraphColumn(goal, data);
						bgc.setLabel(date, goal.getMeasureDisplay());
						list.add(bgc);
					}
				}
				catch (Exception e)
				{
					log.log (Level.SEVERE, sql.toString(), e);
				}
				finally
				{
					DB.close(rs, pstmt);
					rs = null; pstmt = null;
				}
			}	//	Achievement in time
		}	//	Achievement

		//	Request
		else if (MMeasure.MEASURETYPE_Request.equals(getMeasureType()))
		{
			MRequestType rt = MRequestType.get(Env.getCtx(), getR_RequestType_ID());
			String sql = rt.getSqlBarChart(goal.getRestrictions(false),
					goal.getMeasureDisplay(), getMeasureDataType(),
					goal.getDateFrom(), MRole.getDefault());	//	logged in role
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql, null);
				rs = pstmt.executeQuery ();
				while (rs.next ())
				{
					BigDecimal data = rs.getBigDecimal(1);
					int R_Status_ID = rs.getInt(3);
					GraphColumn bgc = new GraphColumn(rt, data, R_Status_ID);
					if (R_Status_ID == 0)
					{
						Timestamp date = rs.getTimestamp(2);
						bgc.setLabel(date, goal.getMeasureDisplay());
					}
					else
					{
						MStatus status = MStatus.get(Env.getCtx(), R_Status_ID);
						bgc.setLabel(status.getName());
					}
					list.add(bgc);
				}
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
		}	//	Request

		//	Project
		else if (MMeasure.MEASURETYPE_Project.equals(getMeasureType()))
		{
			MProjectType pt = MProjectType.get(Env.getCtx(), getC_ProjectType_ID());
			String sql = pt.getSqlBarChart(goal.getRestrictions(false),
					goal.getMeasureDisplay(), getMeasureDataType(),
					goal.getDateFrom(), MRole.getDefault());	//	logged in role
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql, null);
				rs = pstmt.executeQuery ();
				while (rs.next ())
				{
					BigDecimal data = rs.getBigDecimal(1);
					Timestamp date = rs.getTimestamp(2);
					int id = rs.getInt(3);
					GraphColumn bgc = new GraphColumn(pt, data, id);
					bgc.setLabel(date, goal.getMeasureDisplay());
					list.add(bgc);
				}
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
		}	//	Project

		return list;
	}

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MMeasure[");
		sb.append (get_ID()).append ("-").append (getName()).append ("]");
		return sb.toString ();
	}	//	toString
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Mandatory field validation by measure type.
		if (MEASURETYPE_Calculated.equals(getMeasureType())
			&& getPA_MeasureCalc_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "PA_MeasureCalc_ID"));
			return false;
		}
		else if (MEASURETYPE_Ratio.equals(getMeasureType())
			&& getPA_Ratio_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "PA_Ratio_ID"));
			return false;
		}
		else if (MEASURETYPE_UserDefined.equals(getMeasureType())
			&& (getCalculationClass() == null || getCalculationClass().length()==0))
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "CalculationClass"));
			return false;
		}
		else if (MEASURETYPE_Request.equals(getMeasureType())
			&& getR_RequestType_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "R_RequestType_ID"));
			return false;
		}
		else if (MEASURETYPE_Project.equals(getMeasureType())
			&& getC_ProjectType_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "C_ProjectType_ID"));
			return false;
		}
		return true;
	}	//	beforeSave
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		//	Update Goals with Manual Measure
		if (success && MEASURETYPE_Manual.equals(getMeasureType()))
			updateManualGoals();
		
		return success;
	}	//	afterSave
	
	/**
	 * 	Update Goals
	 * 	@return true if updated
	 */
	public boolean updateGoals()
	{
		String mt = getMeasureType();
		try
		{
			if (MEASURETYPE_Manual.equals(mt))
				return updateManualGoals();
			else if (MEASURETYPE_Achievements.equals(mt))
				return updateAchievementGoals();
			else if (MEASURETYPE_Calculated.equals(mt))
				return updateCalculatedGoals();
			else if (MEASURETYPE_Ratio.equals(mt))
				return updateRatios();
			else if (MEASURETYPE_Request.equals(mt))
				return updateRequests();
			else if (MEASURETYPE_Project.equals(mt))
				return updateProjects();
			else if(MEASURETYPE_UserDefined.equals(mt))
				return updateUserDefined();
			//	Projects
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "MeasureType=" + mt, e);
		}
		return false;
	}	//	updateGoals

	/**
	 * 	Update/save Manual Goals
	 * 	@return true if updated
	 */
	private boolean updateManualGoals()
	{
		if (!MEASURETYPE_Manual.equals(getMeasureType()))
			return false;
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			goal.setMeasureActual(getManualActual());
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateManualGoals
	
	/**
	 * 	Update/save Goals with Achievement
	 * 	@return true if updated
	 */
	private boolean updateAchievementGoals()
	{
		if (!MEASURETYPE_Achievements.equals(getMeasureType()))
			return false;
		Timestamp today = new Timestamp(System.currentTimeMillis());
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			String MeasureScope = goal.getMeasureScope();
			String trunc = TimeUtil.TRUNC_DAY;
			if (MGoal.MEASUREDISPLAY_Year.equals(MeasureScope))
				trunc = TimeUtil.TRUNC_YEAR;
			else if (MGoal.MEASUREDISPLAY_Quarter.equals(MeasureScope))
				trunc = TimeUtil.TRUNC_QUARTER;
			else if (MGoal.MEASUREDISPLAY_Month.equals(MeasureScope))
				trunc = TimeUtil.TRUNC_MONTH;
			else if (MGoal.MEASUREDISPLAY_Week.equals(MeasureScope))
				trunc = TimeUtil.TRUNC_WEEK;
			Timestamp compare = TimeUtil.trunc(today, trunc); 
			//
			MAchievement[] achievements = MAchievement.getOfMeasure(getCtx(), getPA_Measure_ID());
			BigDecimal ManualActual = Env.ZERO;
			for (int j = 0; j < achievements.length; j++)
			{
				MAchievement achievement = achievements[j];
				if (achievement.isAchieved() && achievement.getDateDoc() != null)
				{
					Timestamp ach = TimeUtil.trunc(achievement.getDateDoc(), trunc);
					if (compare.equals(ach))
						ManualActual = ManualActual.add(achievement.getManualActual());
				}
			}
			goal.setMeasureActual(ManualActual);
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateAchievementGoals

	/**
	 * 	Update Goals with Calculation measure type (MEASURETYPE_Calculated)
	 * 	@return true if updated
	 */
	private boolean updateCalculatedGoals()
	{
		if (!MEASURETYPE_Calculated.equals(getMeasureType()))
			return false;
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			//	Find Role
			MRole role = null;
			if (goal.getAD_Role_ID() != 0)
				role = MRole.get(getCtx(), goal.getAD_Role_ID());
			else if (goal.getAD_User_ID() != 0)
			{
				MUser user = MUser.get(getCtx(), goal.getAD_User_ID());
				MRole[] roles = user.getRoles(goal.getAD_Org_ID());
				if (roles.length > 0)
					role = roles[0];
			}
			if (role == null)
				role = MRole.getDefault(getCtx(), false);	//	could result in wrong data
			//
			MMeasureCalc mc = MMeasureCalc.get(getPA_MeasureCalc_ID());
			if (mc == null || mc.get_ID() == 0 || mc.get_ID() != getPA_MeasureCalc_ID())
			{
				log.log(Level.SEVERE, "Not found PA_MeasureCalc_ID=" + getPA_MeasureCalc_ID());
				return false;
			}
			String sql = mc.getSqlPI(goal.getRestrictions(false), 
				goal.getMeasureScope(), getMeasureDataType(), null, role);
			BigDecimal ManualActual = DB.getSQLValueBD(null, sql, new Object[]{});
			//	SQL may return no rows or null
			if (ManualActual == null)
			{
				ManualActual = Env.ZERO;
				if (log.isLoggable(Level.FINE)) log.fine("No Value = " + sql);
			}
			goal.setMeasureActual(ManualActual);
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateCalculatedGoals
	
	/**
	 * 	Update Goals with Ratios measure type (MEASURETYPE_Ratio)
	 * 	@return true if updated
	 */
	private boolean updateRatios()
	{
		if (!MEASURETYPE_Ratio.equals(getMeasureType()))
			return false;
		return false;
	}		//	updateRatios
	
	/**
	 * 	Update Goals with Requests measure type (MEASURETYPE_Request)
	 * 	@return true if updated
	 */
	private boolean updateRequests()
	{
		if (!MEASURETYPE_Request.equals(getMeasureType())
			|| getR_RequestType_ID() == 0)
			return false;
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			//	Find Role
			MRole role = null;
			if (goal.getAD_Role_ID() != 0)
				role = MRole.get(getCtx(), goal.getAD_Role_ID());
			else if (goal.getAD_User_ID() != 0)
			{
				MUser user = MUser.get(getCtx(), goal.getAD_User_ID());
				MRole[] roles = user.getRoles(goal.getAD_Org_ID());
				if (roles.length > 0)
					role = roles[0];
			}
			if (role == null)
				role = MRole.getDefault(getCtx(), false);	//	could result in wrong data
			//
			MRequestType rt = MRequestType.get(getCtx(), getR_RequestType_ID());
			String sql = rt.getSqlPI(goal.getRestrictions(false), 
				goal.getMeasureScope(), getMeasureDataType(), null, role);
			BigDecimal ManualActual = DB.getSQLValueBD(null, sql, new Object[]{});
			//	SQL may return no rows or null
			if (ManualActual == null)
			{
				ManualActual = Env.ZERO;
				if (log.isLoggable(Level.FINE)) log.fine("No Value = " + sql);
			}
			goal.setMeasureActual(ManualActual);
			goal.saveEx(get_TrxName());
		}
		return true;
	}		//	updateRequests

	/**
	 * 	Update Goals with Projects measure type (MEASURETYPE_Project)
	 * 	@return true if updated
	 */
	private boolean updateProjects()
	{
		if (!MEASURETYPE_Project.equals(getMeasureType())
			|| getC_ProjectType_ID() == 0)
			return false;
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (int i = 0; i < goals.length; i++)
		{
			MGoal goal = goals[i];
			//	Find Role
			MRole role = null;
			if (goal.getAD_Role_ID() != 0)
				role = MRole.get(getCtx(), goal.getAD_Role_ID());
			else if (goal.getAD_User_ID() != 0)
			{
				MUser user = MUser.get(getCtx(), goal.getAD_User_ID());
				MRole[] roles = user.getRoles(goal.getAD_Org_ID());
				if (roles.length > 0)
					role = roles[0];
			}
			if (role == null)
				role = MRole.getDefault(getCtx(), false);	//	could result in wrong data
			//
			MProjectType pt = MProjectType.get(getCtx(), getC_ProjectType_ID());
			String sql = pt.getSqlPI(goal.getRestrictions(false), 
				goal.getMeasureScope(), getMeasureDataType(), null, role);		
			BigDecimal ManualActual = DB.getSQLValueBD(null, sql, new Object[]{});
			//	SQL may return no rows or null
			if (ManualActual == null)
			{
				ManualActual = Env.ZERO;
				if (log.isLoggable(Level.FINE)) log.fine("No Value = " + sql);
			}
			goal.setMeasureActual(ManualActual);
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateProjects
	/**
	 * 	Update goals with User Defined measure type
	 * 	@return true if updated
	 */
	private boolean updateUserDefined()
	{
		MGoal[] goals = MGoal.getMeasureGoals (getCtx(), getPA_Measure_ID());
		for (MGoal goal:goals)
		{
			BigDecimal amt = Env.ZERO;
			PO po = new MTable(getCtx(),get_Table_ID(),get_TrxName()).getPO(get_ID(), get_TrxName());
			StringTokenizer st = new StringTokenizer(getCalculationClass(), ";,", false);
			while (st.hasMoreTokens())      //  for each class
			{
				String cmd = st.nextToken().trim();	
				StringBuilder retValue = new StringBuilder();
				if (cmd.toLowerCase().startsWith(MRule.SCRIPT_PREFIX)) {
					
					MRule rule = MRule.get(getCtx(), cmd.substring(MRule.SCRIPT_PREFIX.length()));
					if (rule == null) {
						retValue = new StringBuilder("Script ").append(cmd).append(" not found"); 
						log.log(Level.SEVERE, retValue.toString());
						break;
					}
					if ( !  (rule.getEventType().equals(MRule.EVENTTYPE_MeasureForPerformanceAnalysis) 
						  && rule.getRuleType().equals(MRule.RULETYPE_JSR223ScriptingAPIs))) {
						retValue = new StringBuilder("Script ").append(cmd)
							.append(" must be of type JSR 223 and event measure"); 
						log.log(Level.SEVERE, retValue.toString());
						break;
					}
					ScriptEngine engine = rule.getScriptEngine();
					if (engine == null) {
						throw new AdempiereException("Engine not found: " + rule.getEngineName());
					}
					MRule.setContext(engine, po.getCtx(), 0);
					engine.put(MRule.ARGUMENTS_PREFIX + "Ctx", po.getCtx());
					engine.put(MRule.ARGUMENTS_PREFIX + "PO", po);
					try 
					{
						Object value =  engine.eval(rule.getScript());
						amt = (BigDecimal)value;
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, "", e);
						retValue = 	new StringBuilder("Script Invalid: ").append(e.toString());
						return false;
					}	
				} 
				else 
				{
					MeasureInterface custom = null;
					try
					{
						Class<?> clazz = Class.forName(cmd);
						custom = (MeasureInterface)clazz.getDeclaredConstructor().newInstance();
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, "No custom measure class "
								+ cmd + " - " + e.toString(), e);
						return false;
					}
					
					try
					{
						amt = custom.getValue();
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, custom.toString(), e);
						return false;
					}					
				}			
				
				if (!Util.isEmpty(retValue.toString()))		//	interrupt on first error
				{
					log.severe (retValue.toString());
					return false;
				}
			}			
			goal.setMeasureActual(amt);
			goal.saveEx(get_TrxName());
		}
		return true;
	}	//	updateUserDefinedGoals
	
	@Override
	public MMeasure markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MMeasure
