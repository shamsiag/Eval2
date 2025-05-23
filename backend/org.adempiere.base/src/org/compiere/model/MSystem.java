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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.db.CConnection;
import org.compiere.db.Database;
import org.compiere.db.LDAP;
import org.compiere.util.CCache;
import org.compiere.util.CLogMgt;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.TimeUtil;

/**
 * 	System Record (there should be just one AD_System record in the DB)
 *
 *  @author Jorg Janke
 *  @version $Id: MSystem.java,v 1.3 2006/10/09 00:22:28 jjanke Exp $
 *  
 *  @author Teo Sarca, www.arhipac.ro
 *  		<li>FR [ 2214883 ] Remove SQL code and Replace for Query
 */
public class MSystem extends X_AD_System
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -1917493005917422880L;

	/**
	 * 	Load System Record
	 *	@param ctx context
	 *	@return System
	 */
	public synchronized static MSystem get (Properties ctx)
	{
		if (s_system.get(0) != null)
			return new MSystem(ctx, s_system.get(0));
		//
		MSystem system = new Query(ctx, Table_Name, null, null)
						.setOrderBy(COLUMNNAME_AD_System_ID)
						.firstOnly();
		if (system == null)
			return null;
		//
		if (!Ini.isClient() && system.setInfo())
		{
			system.saveCrossTenantSafeEx();
		}
		s_system.put(0, new MSystem(Env.getCtx(), system));
		return system;
	}	//	get
	
	/** System - cached					*/
	private static CCache<Integer,MSystem>	s_system = new CCache<Integer,MSystem>(Table_Name, 1, 0, false, 0);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_System_UU  UUID key
     * @param trxName Transaction
     */
    public MSystem(Properties ctx, String AD_System_UU, String trxName) {
        super(ctx, AD_System_UU, trxName);
    }

	/**
	 * 	Default Constructor
	 *	@param ctx context
	 *	@param ignored ignore
	 *	@param mtrxName transaction
	 */
	public MSystem (Properties ctx, int ignored, String mtrxName)
	{
		super(ctx, 0, mtrxName);
		String trxName = null;
		load(trxName);	//	load ID=0
		if (s_system.get(0) == null)
			s_system.put(0, this);
	}	//	MSystem

	/**
	 * 	Load Constructor
	 * 	@param ctx context
	 * 	@param rs result set
	 * 	@param trxName transaction
	 */
	public MSystem (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
		if (s_system.get(0) == null)
			s_system.put(0, this);
	}	//	MSystem

	/**
	 * Copy constructor 
	 * @param copy
	 */
	public MSystem(MSystem copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MSystem(Properties ctx, MSystem copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MSystem(Properties ctx, MSystem copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 * 	Is LDAP Authentication defined
	 *	@return true if ldap defined
	 */
	public boolean isLDAP()
	{
		String host = getLDAPHost();
		if (host == null || host.length() == 0)
			return false;
		String domain = getLDAPDomain();
		return domain != null 
			&& domain.length() > 0;
	}	//	isLDAP	
	
	/**
	 * 	LDAP Authentication. Assumes that LDAP is defined.
	 *	@param userName user name
	 *	@param password password
	 *	@return true if ldap authenticated
	 */
	public boolean isLDAP (String userName, String password)
	{
		return LDAP.validate(getLDAPHost(), getLDAPDomain(), userName, password);
	}	//	isLDAP

	/**
	 * 	Get DB Address
	 *	@return DB connection URL
	 */
	public String getDBAddress ()
	{
		String s = super.getDBAddress ();
		if (s == null || s.length() == 0)
			s = CConnection.get().getConnectionURL(); 
		return s;
	}	//	getDBAddress
	
	/**
	 * 	Get Statistics Info
	 * 	@param recalc recalculate
	 *	@return statistics
	 */
	public String getStatisticsInfo (boolean recalc)
	{
		String s = super.getStatisticsInfo ();
		if (s == null || recalc)
		{
			String sql = "SELECT 'C'||(SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM AD_Client)"
				+ "||'U'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM AD_User)"
				+ "||'B'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM C_BPartner)"
				+ "||'P'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM M_Product)"
				+ "||'I'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM C_Invoice)"
				+ "||'L'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM C_InvoiceLine)"
				+ "||'M'|| (SELECT " + DB.TO_CHAR("COUNT(*)", DisplayType.Number, Env.getAD_Language(Env.getCtx())) + " FROM M_Transaction)"
				+ " FROM AD_System";
			s = DB.getSQLValueString(null, sql);
		}
		return s;
	}	//	getStatisticsInfo
	
	/**
	 * 	Get Profile Info
	 * 	@param recalc recalculate
	 *	@return List of active AD_Client.Value separated by the '|' character
	 */
	public String getProfileInfo (boolean recalc)
	{
		String s = super.getProfileInfo ();
		if (s == null || recalc)
		{
			final String sql = "SELECT Value FROM AD_Client "
								+ " WHERE IsActive='Y' ORDER BY AD_Client_ID DESC";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			StringBuilder sb = new StringBuilder();
			try
			{
				pstmt = DB.prepareStatement (sql, null);
				rs = pstmt.executeQuery ();
				while (rs.next ())
				{
					sb.append(rs.getString(1)).append('|');
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
			s = sb.toString();
		}
		return s;
	}	//	getProfileInfo
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Mandatory Values
		if (get_Value(COLUMNNAME_IsAutoErrorReport) == null)
			setIsAutoErrorReport (true);
		//
		boolean userChange = Ini.isClient() &&
			(is_ValueChanged(COLUMNNAME_Name)
			|| is_ValueChanged(COLUMNNAME_UserName)
			|| is_ValueChanged(COLUMNNAME_Password)
			|| is_ValueChanged(COLUMNNAME_LDAPHost)
			|| is_ValueChanged(COLUMNNAME_LDAPDomain)
			|| is_ValueChanged(COLUMNNAME_CustomPrefix)
			);
		if (userChange)
		{
			String name = getName();
			if (name.equals("?") || name.length() < 2)
			{
				throw new AdempiereException("Define a unique System name (e.g. Company name) not " + name);
			}
			if (getUserName().equals("?") || getUserName().length() < 2)
			{
				throw new AdempiereException("Use the same EMail address as in the Adempiere Web Store");
			}
			if (getPassword().equals("?") || getPassword().length() < 2)
			{
				throw new AdempiereException("Use the same Password as in the Adempiere Web Store");
			}
		}
		//
		setInfo();
		return true;
	}	//	beforeSave
	
	/**
	 * 	Save Record (ID=0).
	 *  Override to always perform update.
	 * 	@return true if saved
	 */
	@Override
	public boolean save()
	{
		if (!beforeSave(false))
			return false;
		return saveUpdate();
	}	//	save

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString()
	{
		return "MSystem[" + getName()
			+ ",User=" + getUserName()
			+ ",ReleaseNo=" + getReleaseNo()
			+ "]";
	}	//	toString
	
	/**
	 * 	Check validity
	 *	@return true if valid
	 *  @deprecated
	 */
	@Deprecated
	public boolean isValid()
	{
		if (getName() == null || getName().length() < 2)
		{
			log.log(Level.WARNING, "Name not valid: " + getName());
			return false;
		}
		if (getPassword() == null || getPassword().length() < 2)
		{
			log.log(Level.WARNING, "Password not valid: " + getPassword());
			return false;
		}
		if (getInfo() == null || getInfo().length() < 2)
		{
			log.log(Level.WARNING, "Need to run Migration once");
			return false;
		}
		return true;
	}	//	isValid

	/**
	 * 	Is there a PDF License
	 *	@return true if there is a PDF License
	 *  @deprecated
	 */
	@Deprecated
	public boolean isPDFLicense()
	{
		String key = getSummary();
		return key != null && key.length() > 25;
	}	//	isPDFLicense
		
	/**
	 * 	Update System Info if last update (Updated) is more then a day old
	 * 	@return true if updated
	 */
	public boolean setInfo()
	{
		if (!TimeUtil.getDay(getUpdated()).before(TimeUtil.getDay(null)))
			return false;	
		try
		{
			setDBInfo();
			if (isAllowStatistics())
			{
				setInternalUsers();
				setStatisticsInfo(getStatisticsInfo(true));
				setProfileInfo(getProfileInfo(true));
			}
		}
		catch (Exception e)
		{
			setSupportUnits(9999);
			setInfo(e.getLocalizedMessage());
			log.log(Level.SEVERE, "", e);
		}
		return true;
	}	//	setInfo
	
	/**
	 * 	Set Internal User Count
	 */
	private void setInternalUsers()
	{
		final String sql = "SELECT COUNT(DISTINCT (u.AD_User_ID)) AS iu "
			+ "FROM AD_User u"
			+ " INNER JOIN AD_User_Roles ur ON (u.AD_User_ID=ur.AD_User_ID) "
			+ "WHERE u.AD_Client_ID<>11"			//	no Demo
			+ " AND u.AD_User_ID NOT IN (?,?,?)";	//	no System/SuperUser
		int internalUsers = DB.getSQLValue(null, sql, SystemIDs.USER_SYSTEM_DEPRECATED, SystemIDs.USER_SYSTEM, SystemIDs.USER_SUPERUSER);
		setSupportUnits(internalUsers);
	}	//	setInternalUsers

	/**
	 * 	Set DB Info
	 */
	private void setDBInfo()
	{
		String dbAddress = CConnection.get().getConnectionURL();
		setDBAddress(dbAddress.toLowerCase());
		//
		if (!Ini.isClient())
		{
			int noProcessors = Runtime.getRuntime().availableProcessors();
			setNoProcessors(noProcessors);
		}
		//
		String dbName = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = null;
		try
		{
			String dbType = CConnection.get().getDatabase().getName();
			sql = getDBInfoSQL(dbType);
			pstmt = DB.prepareStatement (sql, null);
			rs = pstmt.executeQuery ();
			if (rs.next())
			{
				dbName = rs.getString(2);
				setDBInstance(dbName.toLowerCase());
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
	}	//	setDBInfo
	
	/**
	 * 	Get DB Info SQL. Only implemented for Oracle.
	 *	@param dbType database type
	 *	@return sql
	 */
	public static String getDBInfoSQL (String dbType)
	{
		if (Database.DB_ORACLE.equals(dbType))
			return "SELECT SYS_CONTEXT('USERENV','HOST') || '/' || SYS_CONTEXT('USERENV','IP_ADDRESS') AS DBAddress,"
				+ "	SYS_CONTEXT('USERENV','CURRENT_USER') || '.' || SYS_CONTEXT('USERENV','DB_NAME')"
				+ " || '.' || SYS_CONTEXT('USERENV','DB_DOMAIN') AS DBName "
				+ "FROM DUAL";
		//
		return "SELECT NULL,NULL FROM DUAL WHERE 1=0";
	}	//	getDBInfoSQL
		
	/**
	 * 	Print info
	 *  @deprecated
	 */
	@Deprecated
	public void info()
	{
		if (!CLogMgt.isLevelFine())
			return;

		//	Runtime
		@SuppressWarnings("unused")
		RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
		
		if (CLogMgt.isLevelFiner())
		{
			List<MemoryPoolMXBean> list = ManagementFactory.getMemoryPoolMXBeans();
			Iterator<MemoryPoolMXBean> it = list.iterator();
			while (it.hasNext())
			{
				@SuppressWarnings("unused")
				MemoryPoolMXBean pool = (MemoryPoolMXBean)it.next();
			}
		}
		else
		{
			@SuppressWarnings("unused")
			MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		}
		//	Thread
		@SuppressWarnings("unused")
		ThreadMXBean th = ManagementFactory.getThreadMXBean();
	}	//	info
	
	/*
	 * Allow remember me feature
	 * ZK_LOGIN_ALLOW_REMEMBER_ME and SWING_ALLOW_REMEMBER_ME parameter allow the next values
	 *   U - Allow remember the username (default for zk)
	 *   P - Allow remember the username and password (default for swing)
	 *   N - None
	 *   
	 *	@return boolean representing if remember me feature is allowed
	 */
	private static final String SYSTEM_ALLOW_REMEMBER_USER = "U";
	private static final String SYSTEM_ALLOW_REMEMBER_PASSWORD = "P";

	public static boolean isZKRememberUserAllowed() {
		String ca = MSysConfig.getValue(MSysConfig.ZK_LOGIN_ALLOW_REMEMBER_ME, SYSTEM_ALLOW_REMEMBER_USER);
		return (ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_USER) || ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_PASSWORD));
	}

	public static boolean isZKRememberPasswordAllowed() {
		String ca = MSysConfig.getValue(MSysConfig.ZK_LOGIN_ALLOW_REMEMBER_ME, SYSTEM_ALLOW_REMEMBER_USER);
		return (ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_PASSWORD) && !MSysConfig.getBooleanValue(MSysConfig.USER_PASSWORD_HASH, false));
	}

	public static boolean isSwingRememberUserAllowed() {
		String ca = MSysConfig.getValue(MSysConfig.SWING_LOGIN_ALLOW_REMEMBER_ME, SYSTEM_ALLOW_REMEMBER_PASSWORD);
		return (ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_USER) || ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_PASSWORD));
	}

	public static boolean isSwingRememberPasswordAllowed() {
		String ca = MSysConfig.getValue(MSysConfig.SWING_LOGIN_ALLOW_REMEMBER_ME, SYSTEM_ALLOW_REMEMBER_PASSWORD);
		return (ca.equalsIgnoreCase(SYSTEM_ALLOW_REMEMBER_PASSWORD) && !MSysConfig.getBooleanValue(MSysConfig.USER_PASSWORD_HASH, false));
	}

	/**
	 * <pre>
	 * Verify if the system manages properties in a more secure way.
	 * For Windows and swing client, the properties are managed as before.
	 * For other systems (like Linux), the default is to manage it with more security.
	 * This can be overridden by passing the parameter -DIDEMPIERE_SECURE_PROPERTIES=false to the JVM.
	 * </pre>
	 * @return true if properties needs to be managed in a more secure way
	 */
	public static boolean isSecureProps() {
		if (Env.isWindows() || Ini.isClient())
			return false;
		return SystemProperties.isSecureProperties();
	}

	/**
	 * The system allows to use login prefix for tenant
	 * @return true if login support the use of prefix for identification of tenant 
	 */
	public static boolean isUseLoginPrefix() {
		String loginWithTenantPrefix = MSysConfig.getValue(MSysConfig.LOGIN_WITH_TENANT_PREFIX, "N");
		return "F".equals(loginWithTenantPrefix) || "A".equals(loginWithTenantPrefix);
	}

	/**
	 * The system forces to use login prefix for tenant
	 * @return true if the use of prefix for identification of tenant is mandatory
	 */
	public static boolean isLoginPrefixMandatory() {
		String loginWithTenantPrefix = MSysConfig.getValue(MSysConfig.LOGIN_WITH_TENANT_PREFIX, "N");
		return "F".equals(loginWithTenantPrefix);
	}

}	//	MSystem
