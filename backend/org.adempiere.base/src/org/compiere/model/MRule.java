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
 * Contributor(s): Carlos Ruiz - globalqss                                    *
 *****************************************************************************/
package org.compiere.model;

import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.script.ScriptEngine;

import org.adempiere.base.Core;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Application Rule Model
 *  @author Carlos Ruiz
 *  @version $Id: MRule.java
 *  
 */
public class MRule extends X_AD_Rule implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -288947666359685155L;
	//global or login context variable prefix
	public final static String GLOBAL_CONTEXT_PREFIX = "G_";
	//window context variable prefix
	public final static String WINDOW_CONTEXT_PREFIX = "W_";
	//method call arguments prefix
	public final static String ARGUMENTS_PREFIX = "A_";
	//process parameters prefix
	public final static String PARAMETERS_PREFIX = "P_";
	
	public static final String SCRIPT_PREFIX = "@script:";

	/**
	 * 	Get Rule from Cache (immutable)
	 *	@param AD_Rule_ID id
	 *	@return MRule
	 */
	public static MRule get (int AD_Rule_ID)
	{
		return get(Env.getCtx(), AD_Rule_ID);
	}
	
	/**
	 * 	Get Rule from Cache (immutable)
	 *	@param ctx context
	 *	@param AD_Rule_ID id
	 *	@return MRule
	 */
	public static MRule get (Properties ctx, int AD_Rule_ID)
	{
		Integer key = Integer.valueOf(AD_Rule_ID);
		MRule retValue = s_cache.get (ctx, key, e -> new MRule(ctx, e));
		if (retValue != null)
			return retValue;
		retValue = new MRule (ctx, AD_Rule_ID, (String)null);
		if (retValue.get_ID () == AD_Rule_ID)
		{
			s_cache.put (key, retValue, e -> new MRule(Env.getCtx(), e));
			return retValue;
		}
		return null;
	}	//	get

	/**
	 * 	Get Rule from Cache
	 *	@param ctx context
	 *	@param ruleValue case sensitive rule Value
	 *	@return MRule or null
	 */
	public static MRule get (Properties ctx, String ruleValue)
	{
		if (ruleValue == null)
			return null;
		MRule[] it = s_cache.values().toArray(new MRule[0]);
		for (MRule retValue : it)
		{
			if (ruleValue.equals(retValue.getValue()))
				return retValue;
		}
		//
		final String whereClause = "Value=?";
		MRule retValue = new Query(ctx,I_AD_Rule.Table_Name,whereClause,null)
		.setParameters(ruleValue)
		.setOnlyActiveRecords(true)
		.first();
		
		if (retValue != null)
		{
			Integer key = Integer.valueOf(retValue.getAD_Rule_ID());
			s_cache.put (key, retValue);
		}
		return retValue;
	}	//	get
	
	/**
	 * 	Get Login Rules
	 *	@param ctx context
	 *	@return list of rule or null
	 */
	public static List<MRule> getModelValidatorLoginRules (Properties ctx)
	{
		final String whereClause = "EventType=?";
		List<MRule> rules = new Query(ctx,I_AD_Rule.Table_Name,whereClause,null)
		.setParameters(EVENTTYPE_ModelValidatorLoginEvent)
		.setOnlyActiveRecords(true)
		.list();
		if (rules != null && rules.size() > 0)
			return rules;
		else
			return null;
	}	//	getModelValidatorLoginRules

	/**	Cache						*/
	private static ImmutableIntPOCache<Integer,MRule> s_cache = new ImmutableIntPOCache<Integer,MRule>(Table_Name, 20);
	
	/**	Static Logger	*/
	@SuppressWarnings("unused")
	private static CLogger	s_log	= CLogger.getCLogger (MRule.class);
	
	/* The Engine */
	protected ScriptEngine engine = null;
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_Rule_UU  UUID key
     * @param trxName Transaction
     */
    public MRule(Properties ctx, String AD_Rule_UU, String trxName) {
        super(ctx, AD_Rule_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param AD_Rule_ID id
	 *	@param trxName transaction
	 */
	public MRule (Properties ctx, int AD_Rule_ID, String trxName)
	{
		super (ctx, AD_Rule_ID, trxName);
	}	//	MRule

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MRule (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MRule
	
	/**
	 * Copy constructor
	 * @param copy
	 */
	public MRule(MRule copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MRule(Properties ctx, MRule copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MRule(Properties ctx, MRule copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
		this.engine = copy.engine;
	}
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Validate format for Value
		// must be engine:name
		// where engine can be groovy, jython or beanshell
		if (getRuleType().equals(RULETYPE_JSR223ScriptingAPIs)) {
			String engineName = getEngineName();
			if (engineName == null || 
					(!   (engineName.equalsIgnoreCase("groovy")
							|| engineName.equalsIgnoreCase("jython") 
							|| engineName.equalsIgnoreCase("beanshell")))) {
				log.saveError("Error", Msg.getMsg(getCtx(), "WrongScriptValue"));
				return false;
			}
		}
		return true;
	}	//	beforeSave
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder ("MRule[");
		sb.append (get_ID()).append ("-").append (getValue()).append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	Script Engine for this rule
	 *	@return ScriptEngine or null
	 */
	public ScriptEngine getScriptEngine() {
		String engineName = getEngineName();
		if (engineName != null)
			engine = Core.getScriptEngine(engineName);
		return engine;
	}

	/**
	 * Get engine name from Value (format is engineName:scriptSearchKey)
	 * @return script engine name or null
	 */
	public String getEngineName() {
		int colonPosition = getValue().indexOf(":");
		if (colonPosition < 0)
			return null;
		return getValue().substring(0, colonPosition);
	}
	
	/**
	 *	Add context entries as variable binding to the script engine based on windowNo
	 *  @param engine Script Engine
	 *  @param ctx context
	 *  @param windowNo window number
	 */
	public static void setContext(ScriptEngine engine, Properties ctx, int windowNo) {
		Enumeration<Object> en = ctx.keys();
		while (en.hasMoreElements())
		{
			String key = en.nextElement().toString();
			//  filter
			if (key == null || key.length() == 0
					|| key.startsWith("P")              //  Preferences
					|| (key.indexOf('|') != -1 && !key.startsWith(String.valueOf(windowNo)))    //  other Window Settings
					|| (key.indexOf('|') != -1 && key.indexOf('|') != key.lastIndexOf('|')) //other tab
			)
				continue;
			Object value = ctx.get(key);
			if (value != null) {
				if (value instanceof Boolean)
					engine.put(convertKey(key, windowNo), ((Boolean)value).booleanValue());
				else if (value instanceof Integer)
					engine.put(convertKey(key, windowNo), ((Integer)value).intValue());
				else if (value instanceof Double)
					engine.put(convertKey(key, windowNo), ((Double)value).doubleValue());
				else
					engine.put(convertKey(key, windowNo), value);
			}
		}
	}

	/**
	 *  Convert context key to script engine variable name<br/>
	 *  # -&gt; _
	 *  @param key
	 *  @param m_windowNo 
	 *  @return context key converted to script engine variable name
	 */
	public static String convertKey (String key, int m_windowNo)
	{
		String k = m_windowNo + "|";
		if (key.startsWith(k))
		{
			String retValue = WINDOW_CONTEXT_PREFIX + key.substring(k.length());
			retValue = Util.replace(retValue, "|", "_");
			return retValue;
		}
		else
		{
			String retValue = null;
			if (key.startsWith("#"))
				retValue = GLOBAL_CONTEXT_PREFIX + key.substring(1);
			else
				retValue = key;
			retValue = Util.replace(retValue, "#", "_");
			return retValue;
		}
	}   //  convertKey

	@Override
	public MRule markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MRule