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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.adempiere.process.UUIDGenerator;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.idempiere.cache.ImmutablePOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * 	Language Model
 *
 *  @author Jorg Janke
 *  @version $Id: MLanguage.java,v 1.4 2006/07/30 00:58:36 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>BF [ 2444851 ] MLanguage should throw an exception if there is an error
 */
public class MLanguage extends X_AD_Language implements ImmutablePOSupport
{	
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 6553711529361500744L;
	
	/**
	 * MLanguage cache key by AD_Language value
	 */
	private static final ImmutablePOCache<String, MLanguage> s_cache = new ImmutablePOCache<String, MLanguage>(Table_Name, Table_Name+"|AD_Language", 100, 0, false, 0);

	/**
	 * 	Get Language Model from Language (immutable)
	 * 	@param ctx context
	 * 	@param lang language
	 * 	@return language
	 */
	public static MLanguage get (Properties ctx, Language lang)
	{
		return get (ctx, lang.getAD_Language());
	}	//	getMLanguage

	/**
	 * 	Get Language Model from AD_Language (immutable)
	 * 	@param ctx context
	 *	@param AD_Language language e.g. en_US
	 *	@return language or null
	 */
	public static MLanguage get (Properties ctx, String AD_Language)
	{
		MLanguage retValue = s_cache.get(ctx, AD_Language, e -> new MLanguage(ctx, e));
		if (retValue != null)
			return retValue;
		
		retValue = new Query(ctx, Table_Name, COLUMNNAME_AD_Language+"=?", null)
					.setParameters(AD_Language)
					.firstOnly();
		if (retValue != null)
			s_cache.put(AD_Language, retValue, e -> new MLanguage(ctx, e));
		return retValue;
	}	//	get

	/**
	 * 	Load Languages via ISO code
	 * 	@param ctx context
	 *	@param LanguageISO language ISO code (2 letter) e.g. en
	 *	@return array of MLanguage
	 */
	public static MLanguage[] getWithLanguage (Properties ctx, String LanguageISO)
	{
		List<MLanguage> list = new Query(ctx, Table_Name, COLUMNNAME_LanguageISO+"=?", null)
								.setParameters(LanguageISO)
								.list();
		return list.toArray(new MLanguage[list.size()]);
	}	//	get

	/**
	 * 	Maintain translation of all active languages
	 * 	@param ctx context
	 */
	public static void maintain (Properties ctx)
	{
		List<MLanguage> list = new Query(ctx, Table_Name, "IsSystemLanguage=? AND IsBaseLanguage=?", null)
								.setParameters(true, false)
								.setOnlyActiveRecords(true)
								.list();
		for (MLanguage language : list) {
			language.maintain(true);
		}
	}	//	maintain

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_Language_UU  UUID key
     * @param trxName Transaction
     */
    public MLanguage(Properties ctx, String AD_Language_UU, String trxName) {
        super(ctx, AD_Language_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param AD_Language_ID id
	 *	@param trxName transaction
	 */
	public MLanguage (Properties ctx, int AD_Language_ID, String trxName)
	{
		super (ctx, AD_Language_ID, trxName);
	}	//	MLanguage

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MLanguage (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MLanguage

	/**
	 *	Create Language
	 *	@param ctx context
	 *	@param AD_Language language code
	 * 	@param Name name
	 * 	@param CountryCode country code
	 * 	@param LanguageISO language code
	 *	@param trxName transaction
	 */
	private MLanguage (Properties ctx, String AD_Language, String Name,
		String CountryCode, String LanguageISO, String trxName)
	{
		super(ctx, 0, trxName);
		setAD_Language (AD_Language);	//	en_US
		setIsBaseLanguage (false);
		setIsSystemLanguage (false);
		setName (Name);
		setCountryCode(CountryCode);	//	US
		setLanguageISO(LanguageISO);	//	en
	}	//	MLanguage

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MLanguage(Properties ctx, MLanguage copy) {
		this(ctx, copy, (String)null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MLanguage(Properties ctx, MLanguage copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
		this.m_dateFormat = copy.m_dateFormat != null ? new SimpleDateFormat(copy.m_dateFormat.toPattern()) : null; 
	}

	/**	Locale						*/
	private Locale				m_locale = null;
	/**	Date Format					*/
	private SimpleDateFormat	m_dateFormat = null;
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder("MLanguage[").append(getAD_Language()).append("-").append(getName())
				.append(",Language=").append(getLanguageISO()).append(",Country=").append(getCountryCode())
				.append("]");
		return str.toString();
	}	//	toString

	/**
	 * 	Get Locale
	 *	@return Locale
	 */
	public Locale getLocale()
	{
		if (m_locale == null)
			m_locale = new Locale (getLanguageISO(), getCountryCode());
		return m_locale;
	}	//	getLocale
	
	/**
	 *  Get (Short) Date Format.
	 *  @return date format MM/dd/yyyy - dd.MM.yyyy
	 */
	public SimpleDateFormat getDateFormat()
	{
		if (m_dateFormat != null)
			return m_dateFormat;

		if (getDatePattern() != null)
		{
			m_dateFormat = (SimpleDateFormat)DateFormat.getDateInstance
				(DateFormat.SHORT, getLocale());
			try
			{
				m_dateFormat.applyPattern(getDatePattern());
			}
			catch (Exception e)
			{
				log.severe(getDatePattern() + " - " + e);
				m_dateFormat = null;
			}
		}
		
		if (m_dateFormat == null)
		{
			//	Fix Locale Date format
			m_dateFormat = (SimpleDateFormat)DateFormat.getDateInstance
				(DateFormat.SHORT, getLocale());
			String sFormat = m_dateFormat.toPattern();
			//	some short formats have only one M and d (e.g. ths US)
			if (sFormat.indexOf("MM") == -1 && sFormat.indexOf("dd") == -1)
			{
				StringBuilder nFormat = new StringBuilder();
				for (int i = 0; i < sFormat.length(); i++)
				{
					if (sFormat.charAt(i) == 'M')
						nFormat.append("MM");
					else if (sFormat.charAt(i) == 'd')
						nFormat.append("dd");
					else
						nFormat.append(sFormat.charAt(i));
				}
				//	System.out.println(sFormat + " => " + nFormat);
				m_dateFormat.applyPattern(nFormat.toString());
			}
			//	Unknown short format => use JDBC
			if (m_dateFormat.toPattern().length() != 8)
				m_dateFormat.applyPattern("yyyy-MM-dd");

			//	4 digit year
			if (m_dateFormat.toPattern().indexOf("yyyy") == -1)
			{
				sFormat = m_dateFormat.toPattern();
				StringBuilder nFormat = new StringBuilder();
				for (int i = 0; i < sFormat.length(); i++)
				{
					if (sFormat.charAt(i) == 'y')
						nFormat.append("yy");
					else
						nFormat.append(sFormat.charAt(i));
				}
				m_dateFormat.applyPattern(nFormat.toString());
			}
		}
		//
		m_dateFormat.setLenient(true);
		return m_dateFormat;
	}   //  getDateFormat
	
	/**
	 * 	Set AD_Language_ID
	 */
	private void setAD_Language_ID()
	{
		int AD_Language_ID = getAD_Language_ID();
		if (AD_Language_ID == 0)
		{
			String sql = "SELECT NVL(MAX(AD_Language_ID), 999999) FROM AD_Language WHERE AD_Language_ID > 1000";
			AD_Language_ID = DB.getSQLValue (get_TrxName(), sql);
			setAD_Language_ID(AD_Language_ID+1);
		}
	}	//	setAD_Language_ID

	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Validate DatePattern
		String dp = getDatePattern();
		if (is_ValueChanged("DatePattern") && dp != null && dp.length() > 0)
		{
			if (dp.indexOf("MM") == -1)
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@Error@ @DatePattern@ - No Month (MM)"));
				return false;
			}
			if (dp.indexOf("dd") == -1)
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@Error@ @DatePattern@ - No Day (dd)"));
				return false;
			}
			if (dp.indexOf("yy") == -1)
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@Error@ @DatePattern@ - No Year (yy)"));
				return false;
			}
			
			m_dateFormat = (SimpleDateFormat)DateFormat.getDateInstance
				(DateFormat.SHORT, getLocale());
			try
			{
				m_dateFormat.applyPattern(dp);
			}
			catch (Exception e)
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@Error@ @DatePattern@ - " + e.getMessage()));
				m_dateFormat = null;
				return false;
			}
		}
		
		// Validate TimePattern
		String tp = getTimePattern();
		if (is_ValueChanged("TimePattern") && tp != null && tp.length() > 0)
		{
			if (tp.indexOf("HH") == -1 && tp.indexOf("hh") == -1)
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@Error@ @TimePattern@ - No Hour (HH/hh)"));
				return false;
			}
			if (tp.indexOf("mm") == -1)
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@Error@ @TimePattern@ - No Minute (mm)"));
				return false;
			}
			if (tp.indexOf("ss") == -1)
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@Error@ @TimePattern@ - No Second (ss)"));
				return false;
			}
			
			m_dateFormat = (SimpleDateFormat)DateFormat.getTimeInstance
				(DateFormat.SHORT, getLocale());
			try
			{
				m_dateFormat.applyPattern(tp);
			}
			catch (Exception e)
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@Error@ @TimePattern@ - " + e.getMessage()));
				m_dateFormat = null;
				return false;
			}
		}
		if (newRecord)
			setAD_Language_ID();
		return true;
	}	//	beforeSae
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		int no = TranslationTable.getActiveLanguages(true);
		if (log.isLoggable(Level.FINE)) log.fine("Active Languages=" + no);
		return true;
	}	//	afterSave
	
	/**
	 * 	Maintain Translation
	 *	@param add if true add missing records - otherwise delete
	 *	@return number of records deleted/inserted
	 */
	public int maintain (boolean add)
	{
		String sql = "SELECT TableName FROM AD_Table WHERE TableName LIKE '%_Trl' ORDER BY TableName";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int retNo = 0;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				if (add)
					retNo += addTable (rs.getString(1));
				else
					retNo += deleteTable (rs.getString(1));
			}
		}
		catch (SQLException e)
		{
			throw new DBException(e, sql);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return retNo;
	}	//	maintain

	/**
	 * 	Delete Translation
	 *	@param tableName table name
	 *	@return number of records deleted
	 */
	private int deleteTable (String tableName)
	{
		StringBuilder sql = new StringBuilder("DELETE  FROM  ").append(tableName).append(" WHERE AD_Language=?");
		int no = DB.executeUpdateEx(sql.toString(), new Object[]{getAD_Language()}, get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine(tableName + " #" + no);
		return no;
	}	//	deleteTable

	/**
	 * 	Add Translation to table
	 *	@param tableName table name
	 *	@return number of records inserted
	 */
	private int addTable (String tableName)
	{
		String baseTableName = tableName.substring(0, tableName.length()-4);
		MTable baseTable = MTable.get(getCtx(), baseTableName);
		StringBuilder cols = new StringBuilder();
		for (MColumn column : baseTable.getColumns(false))
		{
			if (column.isTranslated())
				cols.append(",").append(column.getColumnName());
		}
		//	Columns
		if (cols.length() == 0)
		{
			log.log(Level.SEVERE, "No Columns found for " + baseTableName);
			return 0;
		}
			
		//	Insert Statement
		int AD_User_ID = Env.getAD_User_ID(getCtx());
		String keyColumn = baseTable.getKeyColumns()[0];
		StringBuilder insert = new StringBuilder("INSERT INTO ").append(tableName)
							.append("(AD_Language,IsTranslated, AD_Client_ID,AD_Org_ID, ")
							.append("Createdby,UpdatedBy,Created,Updated, ")
							.append(keyColumn).append(cols).append(") ")
							.append("SELECT '").append(getAD_Language()).append("','N', AD_Client_ID,AD_Org_ID, ")
							.append(AD_User_ID).append(",").append(AD_User_ID).append(", getDate(), getDate(), ")
							.append(keyColumn).append(cols)
							.append(" FROM ").append(baseTableName)
							.append(" WHERE ").append(keyColumn).append(" NOT IN (SELECT ").append(keyColumn)
							.append(" FROM ").append(tableName)
							.append(" WHERE AD_Language='").append(getAD_Language()).append("')");
		int no = DB.executeUpdateEx(insert.toString(), null, get_TrxName());
		// IDEMPIERE-99 Language Maintenance does not create UUIDs
		String uucolname = PO.getUUIDColumnName(tableName);
		MTable table = MTable.get(getCtx(), tableName);
		MColumn column = table.getColumn(uucolname);
		if (column != null) {
			if (DB.isGenerateUUIDSupported()) {
				StringBuilder upduuid = new StringBuilder("UPDATE ")
						.append(tableName)
						.append(" SET ")
						.append(uucolname)
						.append("=generate_uuid() WHERE ")
						.append(uucolname)
						.append(" IS NULL");
				DB.executeUpdateEx(upduuid.toString(), get_TrxName());
			} else {
				UUIDGenerator.updateUUID(column, get_TrxName());
			}
		}
		//
		StringBuilder msglog = new StringBuilder().append(tableName).append(" #").append(no);
		if (log.isLoggable(Level.FINE)) log.fine(msglog.toString());
		return no;
	}	//	addTable

	@Override
	public MLanguage markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}

}	//	MLanguage
