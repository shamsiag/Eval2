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
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.process.UUIDGenerator;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;
import org.idempiere.expression.logic.LogicEvaluator;

/**
 *  Process Parameter Model
 *
 *  @author Jorg Janke
 *  @version $Id: MProcessPara.java,v 1.3 2006/07/30 00:58:37 jjanke Exp $
 */
public class MProcessPara extends X_AD_Process_Para implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -1116840975434565353L;

	/** Static Logger					*/
	private static CLogger		s_log = CLogger.getCLogger (MProcessPara.class);

	/**
	 * 	Get MProcessPara from Cache (immutable)
	 *	@param AD_Process_Para_ID id
	 *	@return MProcessPara
	 */
	public static MProcessPara get (int AD_Process_Para_ID)
	{
		return get(Env.getCtx(), AD_Process_Para_ID);
	}
	
	/**
	 * 	Get MProcessPara from Cache (immutable)
	 *	@param ctx context
	 *	@param AD_Process_Para_ID id
	 *	@return MProcessPara
	 */
	public static MProcessPara get (Properties ctx, int AD_Process_Para_ID)
	{
		Integer key = Integer.valueOf(AD_Process_Para_ID);
		MProcessPara retValue = s_cache.get (ctx, key, e -> new MProcessPara(ctx, e));
		if (retValue != null)
			return retValue;
		retValue = new MProcessPara (ctx, AD_Process_Para_ID, (String)null);
		if (retValue.get_ID () == AD_Process_Para_ID)
		{
			s_cache.put (key, retValue, e -> new MProcessPara(Env.getCtx(), e));
			return retValue;
		}
		return null;
	}	//	get

	/**	Cache						*/
	private static ImmutableIntPOCache<Integer, MProcessPara> s_cache 
		= new ImmutableIntPOCache<Integer, MProcessPara> (Table_Name, 20);
		
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_Process_Para_UU  UUID key
     * @param trxName Transaction
     */
    public MProcessPara(Properties ctx, String AD_Process_Para_UU, String trxName) {
        super(ctx, AD_Process_Para_UU, trxName);
		if (Util.isEmpty(AD_Process_Para_UU))
			setInitialDefaults();
    }

	/**
	 * 	Constructor
	 *	@param ctx context
	 *	@param AD_Process_Para_ID id
	 *	@param trxName transaction
	 */
	public MProcessPara (Properties ctx, int AD_Process_Para_ID, String trxName)
	{
		super (ctx, AD_Process_Para_ID, trxName);
		if (AD_Process_Para_ID == 0)
			setInitialDefaults();
	}	//	MProcessPara

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setFieldLength (0);
		setSeqNo (0);
		setIsCentrallyMaintained (true);
		setIsRange (false);
		setIsMandatory (false);
		setEntityType (ENTITYTYPE_UserMaintained);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MProcessPara (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProcessPara

	/**
	 * Parent constructor
	 * @param parent process
	 */
	public MProcessPara(MProcess parent) {
		
		this (parent.getCtx(), 0, parent.get_TrxName());
		setClientOrg(parent);
		setAD_Process_ID(parent.getAD_Process_ID());
		setEntityType(parent.getEntityType());
	}

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MProcessPara(MProcessPara copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MProcessPara(Properties ctx, MProcessPara copy)
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MProcessPara(Properties ctx, MProcessPara copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/** Virtual Window No - 999	*/
	public static int		WINDOW_NO = 999;
	/** Virtual Tab No - 0		*/
	public static int		TAB_NO = 0;
	
	/**	The Lookup				*/
	private Lookup		m_lookup = null;
	
	/**
	 *  Is this field a Lookup?.
	 *  @return true if lookup field
	 */
	public boolean isLookup()
	{
		boolean retValue = false;
		int displayType = getAD_Reference_ID(); 
		if (DisplayType.isLookup(displayType))
			retValue = true;
		else if (displayType == DisplayType.Location
			|| displayType == DisplayType.Locator
			|| displayType == DisplayType.Account
			|| displayType == DisplayType.PAttribute)
			retValue = true;
		return retValue;
	}   //  isLookup

	/**
	 *  Load Lookup for column with lookup
	 */
	public void loadLookup()
	{
		if (!isLookup())
			return;
		if (log.isLoggable(Level.FINE)) log.fine("(" + getColumnName() + ")");
		int displayType = getAD_Reference_ID();
		if (DisplayType.isLookup(displayType))
		{
			MLookupInfo  lookupInfo = MLookupFactory.getLookupInfo(getCtx(), 0, 
				getAD_Process_Para_ID(), getAD_Reference_ID(), 
				Env.getLanguage(getCtx()), getColumnName(), 
				getAD_Reference_Value_ID(), false, "");
			if (lookupInfo == null)
			{
				log.log(Level.SEVERE, "(" + getColumnName() + ") - No LookupInfo");
				return;
			}
			//	Prevent loading of CreatedBy/UpdatedBy
			if (displayType == DisplayType.Table
				&& (getColumnName().equals("CreatedBy") || getColumnName().equals("UpdatedBy")) )
			{
				lookupInfo.IsCreadedUpdatedBy = true;
				lookupInfo.DisplayType = DisplayType.Search;
			}
			//
			MLookup ml = new MLookup (lookupInfo, TAB_NO);
			m_lookup = ml;
		}
		else if (displayType == DisplayType.Location)   //  not cached
		{
			MLocationLookup ml = new MLocationLookup (getCtx(), WINDOW_NO);
			m_lookup = ml;
		}
		else if (displayType == DisplayType.Locator)
		{
			MLocatorLookup ml = new MLocatorLookup (getCtx(), WINDOW_NO);
			m_lookup = ml;
		}
		else if (displayType == DisplayType.Account)    //  not cached
		{
			MAccountLookup ma = new MAccountLookup (getCtx(), WINDOW_NO);
			m_lookup = ma;
		}
		else if (displayType == DisplayType.PAttribute)    //  not cached
		{
			MPAttributeLookup pa = new MPAttributeLookup (getCtx(), WINDOW_NO);
			m_lookup = pa;
		}
		//
		if (m_lookup != null)
			m_lookup.loadComplete();
	}   //  loadLookup

	/**
	 * 	Get Lookup for Parameter
	 *	@return lookup or null
	 */
	public Lookup getLookup()
	{
		if (m_lookup == null && isLookup())
			loadLookup();
		return m_lookup;
	}	//	getLookup
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MProcessPara[")
			.append (get_ID ())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
	/**
	 * Copy settings from another process parameter and save
	 * @param source 
	 */
	public void copyFrom (MProcessPara source)
	{
		if (log.isLoggable(Level.FINE))log.log(Level.FINE, "Copying from:" + source + ", to: " + this);
		setAD_Element_ID(source.getAD_Element_ID());
		setAD_Reference_ID(source.getAD_Reference_ID());
		setAD_Reference_Value_ID(source.getAD_Reference_Value_ID());
		setAD_Val_Rule_ID(source.getAD_Val_Rule_ID());
		setColumnName(source.getColumnName());
		setDefaultValue(source.getDefaultValue());
		setDefaultValue2(source.getDefaultValue2());
		setDescription(source.getDescription());
		setDisplayLogic(source.getDisplayLogic());
		setFieldLength(source.getFieldLength());
		setHelp(source.getHelp());
		setIsActive(source.isActive());
		setIsCentrallyMaintained(source.isCentrallyMaintained());
		setIsMandatory(source.isMandatory());
		setIsRange(source.isRange());
		setName(source.getName());
		setReadOnlyLogic(source.getReadOnlyLogic());
		setSeqNo(source.getSeqNo());
		setValueMax(source.getValueMax());
		setValueMin(source.getValueMin());
		setVFormat(source.getVFormat());
		
		saveEx();
		
		// delete new translations and copy translations from source
		String sql = "DELETE FROM AD_Process_Para_Trl WHERE AD_Process_Para_ID = ?";
		int count = DB.executeUpdateEx(sql, new Object[] { getAD_Process_Para_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))log.log(Level.FINE, "AD_Process_Para_Trl deleted: " + count);
		
		sql = "INSERT INTO AD_Process_Para_Trl (AD_Process_Para_ID, AD_Language, " +
				" AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy, " +
				" Name, Description, Help, IsTranslated) " +
				" SELECT ?, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, " +
				" Updated, UpdatedBy, Name, Description, Help, IsTranslated " +
				" FROM AD_Process_Para_Trl WHERE AD_Process_Para_ID = ? ";
		count = DB.executeUpdateEx(sql, new Object[] { getAD_Process_Para_ID(), source.getAD_Process_Para_ID() }, get_TrxName());
		if (log.isLoggable(Level.FINE))log.log(Level.FINE, "AD_Process_Para_Trl inserted: " + count);
		if (DB.isGenerateUUIDSupported())
			DB.executeUpdateEx("UPDATE AD_Process_Para_Trl SET AD_Process_Para_Trl_UU=generate_uuid() WHERE AD_Process_Para_Trl_UU IS NULL", get_TrxName());
		else
			UUIDGenerator.updateUUID(MColumn.get(getCtx(), "AD_Process_Para_Trl", "AD_Process_Para_Trl_UU"), get_TrxName());		
		
	}

	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (isCentrallyMaintained() && getAD_Element_ID() == 0)
			setIsCentrallyMaintained(false);	// IDEMPIERE 109 - param without element can't be centrally maintained

		//	Sync Terminology with AD_Element
		if ((newRecord || is_ValueChanged ("AD_Element_ID")) 
			&& getAD_Element_ID() != 0 && isCentrallyMaintained())
		{
			M_Element element = new M_Element (getCtx(), getAD_Element_ID (), get_TrxName());
			setColumnName (element.getColumnName());
			setName (element.getName());
			setDescription (element.getDescription());
			setHelp (element.getHelp());
		}

		// Validate read only and display logic expression
		if (newRecord || is_ValueChanged(COLUMNNAME_ReadOnlyLogic)) {
			if (isActive() && !Util.isEmpty(getReadOnlyLogic(), true) && !getReadOnlyLogic().startsWith(MColumn.VIRTUAL_UI_COLUMN_PREFIX)) {
				LogicEvaluator.validate(getReadOnlyLogic());
			}
		}
		if (newRecord || is_ValueChanged(COLUMNNAME_DisplayLogic)) {
			if (isActive() && !Util.isEmpty(getDisplayLogic(), true) && !getDisplayLogic().startsWith(MColumn.VIRTUAL_UI_COLUMN_PREFIX)) {
				LogicEvaluator.validate(getDisplayLogic());
			}
		}

		// set IsShowNegateButton to true for report
		if (newRecord && DisplayType.isChosenMultipleSelection(getAD_Reference_ID())) {
			MProcess p = MProcess.get(getAD_Process_ID());
			if (Util.isEmpty(p.getClassname()) && Util.isEmpty(p.getProcedureName()) && Util.isEmpty(p.getJasperReport()))
				setIsShowNegateButton(true);
		}

		// Validate ValueMin for Date and Number
		if (getValueMin() != null) {
			try {
				if (getAD_Reference_ID() == DisplayType.Date) { // Date
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					new Timestamp(dateFormat.parse(getValueMin()).getTime());
				} else if (DisplayType.isNumeric(getAD_Reference_ID())) {
					new BigDecimal(getValueMin());
				}
			} catch (Exception e) {
				throw new AdempiereException("Min Value : "+ e.getLocalizedMessage());
			}
		}

		// Validate ValueMax for Date and Number
		if (getValueMax() != null) {
			try {
				if (getAD_Reference_ID() == DisplayType.Date) { // Date
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					new Timestamp(dateFormat.parse(getValueMax()).getTime());
				} else if (DisplayType.isNumeric(getAD_Reference_ID())) {
					new BigDecimal(getValueMax());
				}
			} catch (Exception e) {
				throw new AdempiereException("Max Value : "+ e.getLocalizedMessage());
			}
		}

		return true;
	}	//	beforeSave

	/**
	 * Get reference table name for lookup and list field
	 * @return reference table name or null
	 */
	public String getReferenceTableName() {
		String foreignTable = null;
		int refid = getAD_Reference_ID();
		if (DisplayType.TableDir == refid || DisplayType.TableDirUU == refid || ((DisplayType.Search == refid || DisplayType.SearchUU == refid) && getAD_Reference_Value_ID() == 0)) {
			foreignTable = getColumnName().substring(0, getColumnName().length()-3);
		} else if (DisplayType.Table == refid || DisplayType.TableUU == refid || DisplayType.Search == refid || DisplayType.SearchUU == refid) {
			MReference ref = MReference.get(getCtx(), getAD_Reference_Value_ID(), get_TrxName());
			if (MReference.VALIDATIONTYPE_TableValidation.equals(ref.getValidationType())) {
				MRefTable rt = MRefTable.get(getCtx(), getAD_Reference_Value_ID(), get_TrxName());
				if (rt != null)
					foreignTable = rt.getAD_Table().getTableName();
			}
		} else 	if (DisplayType.isList(refid)) {
			foreignTable = "AD_Ref_List";
		}

		return foreignTable;
	}

	@Override
	public MProcessPara markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

	/**
	 * Write in server log when an unexpected parameter is processed.<br/>
	 * If the parameter is defined in dictionary log at INFO level as a custom parameter.<br/>
	 * Otherwise log at SEVERE level as unknown parameter.
	 * @param processId
	 * @param para
	 */
	public static void validateUnknownParameter(int processId, ProcessInfoParameter para) {
		MProcess process = MProcess.get(processId);
		StringBuilder msg = new StringBuilder("Process ").append(process.getValue()).append(" - ");
		Level level;
		if (process.getParameter(para.getParameterName()) == null) {
			msg.append("Unknown");
			level = Level.SEVERE;
		} else {
			msg.append("Custom");
			level = Level.INFO;
		}
		msg.append(" Parameter: ").append(para.getParameterName()).append("=").append(para.getInfo());
		if (s_log.isLoggable(level))
			s_log.log(level, msg.toString());			
	}

}	//	MProcessPara
