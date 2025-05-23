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
package org.compiere.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.sql.RowSet;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.adempiere.util.ProcessUtil;
import org.compiere.Adempiere;
import org.compiere.db.AdempiereDatabase;
import org.compiere.db.CConnection;
import org.compiere.db.Database;
import org.compiere.db.ProxyFactory;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MLanguage;
import org.compiere.model.MRole;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.model.MSystem;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POResultSet;
import org.compiere.model.SystemIDs;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;

/**
 *  Static methods for JDBC interface
 *
 *  @author     Jorg Janke
 *  @version    $Id: DB.java,v 1.8 2006/10/09 00:22:29 jjanke Exp $
 *  ---
 *  @author Ashley Ramdass (Posterita)
 *		<li>Modifications: removed static references to database connection and instead always
 *			get a new connection from database pool manager which manages all connections
 *			set rw/ro properties for the connection accordingly.
 *
 *  @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 		<li>BF [ 1647864 ] WAN: delete record error
 * 		<li>FR [ 1884435 ] Add more DB.getSQLValue helper methods
 * 		<li>FR [ 1904460 ] DB.executeUpdate should handle Boolean params
 * 		<li>BF [ 1962568 ] DB.executeUpdate should handle null params
 * 		<li>FR [ 1984268 ] DB.executeUpdateEx should throw DBException
 * 		<li>FR [ 1986583 ] Add DB.executeUpdateEx(String, Object[], String)
 * 		<li>BF [ 2030233 ] Remove duplicate code from DB class
 * 		<li>FR [ 2107062 ] Add more DB.getKeyNamePairs methods
 *		<li>FR [ 2448461 ] Introduce DB.getSQLValue*Ex methods
 *		<li>FR [ 2781053 ] Introduce DB.getValueNamePairs
 *		<li>FR [ 2818480 ] Introduce DB.createT_Selection helper method
 *			https://sourceforge.net/p/adempiere/feature-requests/757/
 *  @author Teo Sarca, teo.sarca@gmail.com
 * 		<li>BF [ 2873324 ] DB.TO_NUMBER should be a static method
 * 			https://sourceforge.net/p/adempiere/bugs/2160/
 * 		<li>FR [ 2873891 ] DB.getKeyNamePairs should use trxName
 * 			https://sourceforge.net/p/adempiere/feature-requests/847/
 *  @author Paul Bowden, phib BF 2900767 Zoom to child tab - inefficient queries
 *  @see https://sourceforge.net/p/adempiere/bugs/2222/
 */
public final class DB
{
	/** Connection Descriptor           */
	private static CConnection      s_cc = null;
	/**	Logger							*/
	private static CLogger			log = CLogger.getCLogger (DB.class);
	/** Lock object for mutual access to {@link #s_cc} */
	private static Object			s_ccLock = new Object();

	/** SQL Statement Separator "; "	*/
	public static final String SQLSTATEMENT_SEPARATOR = "; ";

	/**
	 * 	Check need for post Upgrade
	 * 	@param ctx context
	 *	@return true if post upgrade ran - false if there was no need
	 */
	@Deprecated(forRemoval = true, since = "11")
	public static boolean afterMigration (Properties ctx)
	{
		//	UPDATE AD_System SET IsJustMigrated='Y'
		MSystem system = MSystem.get(ctx);
		if (!system.isJustMigrated())
			return false;

		//	Role update
		log.info("Role");
		String sql = "SELECT * FROM AD_Role";
		PreparedStatement pstmt = null;
        ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MRole role = new MRole (ctx, rs, null);
				role.updateAccessRecords();
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "(1)", e);
		}
        finally
        {
            close(rs);
            close(pstmt);
            rs= null;
            pstmt = null;
        }
		//	Release Specif stuff & Print Format
		try
		{
			Class<?> clazz = Class.forName("org.compiere.MigrateData");
			clazz.getDeclaredConstructor().newInstance();
		}
		catch (Exception e)
		{
			log.log (Level.SEVERE, "Data", e);
		}

		//	Language check
		log.info("Language");
		MLanguage.maintain(ctx);

		//	Sequence check
		log.info("Sequence");
		ProcessInfo processInfo = new ProcessInfo("Sequence Check", 0);
		processInfo.setClassName("org.compiere.process.SequenceCheck");
		processInfo.setParameter(new ProcessInfoParameter[0]);
		ProcessUtil.startJavaProcess(ctx, processInfo, null);

		//	Costing Setup
		log.info("Costing");
		MAcctSchema[] ass = MAcctSchema.getClientAcctSchema(ctx, 0);
		for (int i = 0; i < ass.length; i++)
		{
			ass[i].checkCosting();
			ass[i].saveEx();
		}

		//	Reset Flag
		system.setIsJustMigrated(false);
		return system.save();
	}	//	afterMigration

	/**
	 * 	Update Mail Settings for System Client and System User (idempiereEnv.properties)
	 */
	public static void updateMail()
	{
		//	Get Property File
		String envName = Ini.getAdempiereHome();
		if (envName == null)
			return;
		envName += File.separator + "idempiereEnv.properties";
		File envFile = new File(envName);
		if (!envFile.exists())
			return;

		Properties env = new Properties();
		try
		{
			FileInputStream in = new FileInputStream(envFile);
			env.load(in);
			in.close();
		}
		catch (Exception e)
		{
			return;
		}
		String updated = env.getProperty("ADEMPIERE_MAIL_UPDATED");
		if (updated != null && updated.equals("Y"))
			return;

		//	See org.compiere.install.ConfigurationData
		String server = env.getProperty("ADEMPIERE_MAIL_SERVER");
		if (server == null || server.length() == 0)
			return;
		String adminEMail = env.getProperty("ADEMPIERE_ADMIN_EMAIL");
		if (adminEMail == null || adminEMail.length() == 0)
			return;
		String mailUser = env.getProperty("ADEMPIERE_MAIL_USER");
		if (mailUser == null || mailUser.length() == 0)
			return;
		String mailPassword;
		if (!env.containsKey("ADEMPIERE_MAIL_PASSWORD") && MSystem.isSecureProps())
			mailPassword = Ini.getVar("ADEMPIERE_MAIL_PASSWORD");
		else
			mailPassword = env.getProperty("ADEMPIERE_MAIL_PASSWORD");
		//
		StringBuilder sql = new StringBuilder("UPDATE AD_Client SET")
			.append(" SMTPHost=").append(DB.TO_STRING(server))
			.append(", RequestEMail=").append(DB.TO_STRING(adminEMail))
			.append(", RequestUser=").append(DB.TO_STRING(mailUser))
			.append(", RequestUserPW=").append(DB.TO_STRING(mailPassword))
			.append(", IsSMTPAuthorization='Y' WHERE AD_Client_ID=0");
		int no = DB.executeUpdate(sql.toString(), null);
		if (log.isLoggable(Level.FINE)) log.fine("Client #"+no);
		//
		sql = new StringBuilder("UPDATE AD_User SET ")
			.append(" EMail=").append(DB.TO_STRING(adminEMail))
			.append(", EMailUser=").append(DB.TO_STRING(mailUser))
			.append(", EMailUserPW=").append(DB.TO_STRING(mailPassword))
			.append(" WHERE AD_User_ID IN (?,?,?)");
		no = DB.executeUpdate(sql.toString(), new Object[]{SystemIDs.USER_SYSTEM_DEPRECATED, SystemIDs.USER_SYSTEM, SystemIDs.USER_SUPERUSER}, false, null);
		if (log.isLoggable(Level.FINE)) log.fine("User #"+no);
		//
		try (FileOutputStream out = new FileOutputStream(envFile))
		{
			env.setProperty("ADEMPIERE_MAIL_UPDATED", "Y");
			env.store(out, "");
			out.flush();
		}
		catch (Exception e)
		{
		}

	}	//	updateMail

	/**
	 *  Set active connection profile
	 *  @param cc connection profile
	 */
	public synchronized static void setDBTarget (CConnection cc)
	{
		if (cc == null)
			throw new IllegalArgumentException("Connection is NULL");

		if (s_cc != null && s_cc.equals(cc))
			return;

		DB.closeTarget();
		//
		synchronized(s_ccLock)
		{
			s_cc = cc;
		}

		s_cc.setDataSource();

		if (log.isLoggable(Level.CONFIG)) log.config(s_cc + " - DS=" + s_cc.isDataSource());
	}   //  setDBTarget

	/**
	 * Connect to database and initialise all connections.
	 * @return True if success, false otherwise
	 */
	@Deprecated
	public static boolean connect() {
		//direct connection
		boolean success =false;
		try
		{
            Connection conn = getConnection();
            if (conn != null)
            {
                s_cc.readInfo(conn);
                conn.close();
            }

            success = (conn != null);
		}
        catch (Exception e)
		{
        	//logging here could cause infinite loop
            //log.log(Level.SEVERE, "Could not connect to DB", e);
        	System.err.println("Could not connect to DB - " + e.getLocalizedMessage());
        	e.printStackTrace();
            success = false;
		}
		return success;
	}

	/**
	 * Is connected to DB.
	 * @return true, if connected to database
	 */
	public static boolean isConnected()
	{
		//bug [1637432]
		if (s_cc == null) return false;

		//get connection
		boolean success = false;
		try
		{
            Connection conn = getConnection();   //  try to get a connection
            if (conn != null)
            {
                conn.close();
                success = true;
            }
            else success = false;
		}
		catch (Exception e)
		{
			success = false;
		}
		return success;
	}

	/**
	 *  Replace by {@link #isConnected()}
	 * 
	 *  Is there a connection to the database ?
	 *  @param createNew ignore
	 *  @return true, if connected to database
	 *  @deprecated
	 */
	@Deprecated (since="10", forRemoval=true)
	public static boolean isConnected(boolean createNew)
	{
		return isConnected();
	}   //  isConnected

	/**
	 * Get auto commit connection from connection pool.
	 * @return {@link Connection}
	 */
	public static Connection getConnection() 
	{
		return getConnection(true);
	}
	
	/**
	 * Get auto or not auto commit connection from connection pool.<br/>
	 * Usually, developer should use @{@link #getConnection()} instead to get auto commit connection 
	 * and use {@link Trx} to works with not autoCommit connection.
	 * @param autoCommit
	 * @return {@link Connection}
	 */
	public static Connection getConnection(boolean autoCommit) 
	{
		return createConnection(autoCommit, Connection.TRANSACTION_READ_COMMITTED);
	}
	
	/**
	 * Replace by @{@link #getConnection()} 
	 * 
	 * @return Connection (r/w)
	 * @deprecated
	 */
	@Deprecated (since="10", forRemoval=true)
	public static Connection getConnectionRW()
	{
		return getConnection();
	}

	/**
	 *  Replace by @{@link #getConnection()}
	 *  
	 *	Return (pooled) r/w AutoCommit, Serializable connection.
	 *	For Transaction control use Trx.getConnection()
	 *  @param createNew ignore
	 *  @return Connection (r/w)
	 *  @deprecated
	 */
	@Deprecated (since="10", forRemoval=true)
	public static Connection getConnectionRW (boolean createNew)
	{
        return getConnection();
	}   //  getConnectionRW

	/**
	 *  Replace by @{@link #getConnection(boolean)}. 
	 *  Note that this is intended for internal use only from the beginning.
	 *  
	 *	Return everytime a new r/w no AutoCommit, Serializable connection.
	 *	To be used to ID
	 *  @return Connection (r/w)
	 *  @deprecated
	 */
	@Deprecated (since="10", forRemoval=true)
	public static Connection getConnectionID ()
	{
        return getConnection(false);
	}   //  getConnectionID

	/**
	 *  Replace by @{@link #getConnection()}. Use {@link Trx} instead for readonly transaction.
	 *  
	 *	Return read committed, read/only from pool.
	 *  @return Connection (r/o)
	 *  @deprecated
	 */
	@Deprecated (since="10", forRemoval=true)
	public static Connection getConnectionRO ()
	{
        return getConnection();
	}	//	getConnectionRO

	/**
	 *	Return a replica connection if possible, otherwise from pool.
	 *  @return Connection (r/o)
	 */
	public static Connection getReportingConnectionRO ()
	{
		Connection conn = DBReadReplica.getConnectionRO();
		if (conn == null)
			conn = getConnection();
        return conn;
	}	//	getReportingConnectionRO

	/**
	 *	Create new Connection.<br/>
	 *  The connection must be closed explicitly by the caller.<br/>
	 *  Usually, developer should not call this directly.
	 *
	 *  @param autoCommit auto commit
	 *  @param trxLevel - Connection.TRANSACTION_READ_UNCOMMITTED, Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_REPEATABLE_READ, or Connection.TRANSACTION_READ_COMMITTED.
	 *  @return Connection connection
	 */
	public static Connection createConnection (boolean autoCommit, int trxLevel)
	{
		Connection conn = s_cc.getConnection (autoCommit, trxLevel);

		if (conn == null)
        {
            throw new IllegalStateException("DB.createConnection - @NoDBConnection@");
        }
		
		//hengsin: failed to set autocommit can lead to severe lock up of the system
        try {
	        if (conn != null && conn.getAutoCommit() != autoCommit)
	        {
	        	throw new IllegalStateException("Failed to set the requested auto commit mode on connection. [autoCommit=" + autoCommit +"]");
	        }
        } catch (SQLException e) {}

		return conn;
	}	//	createConnection

    /**
     *  Replace by {@link #createConnection(boolean, int)}.
     *  Use {@link Trx} instead for readonly transaction.
     *  
     *  Create new Connection.
     *  The connection must be closed explicitly by the application.
     *
     *  @param autoCommit auto commit
     *  @param readOnly ignore
     *  @param trxLevel - Connection.TRANSACTION_READ_UNCOMMITTED, Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_REPEATABLE_READ, or Connection.TRANSACTION_READ_COMMITTED.
     *  @return Connection connection
     *  @deprecated
     */
	@Deprecated (since="10", forRemoval=true)
    public static Connection createConnection (boolean autoCommit, boolean readOnly, int trxLevel)
    {
        return createConnection(autoCommit, trxLevel);
    }   //  createConnection

	/**
	 *  Get Database Adapter.<br/>
	 *  Access to database specific functionality.
	 *  @return iDempiere Database Adapter
	 */
	public static AdempiereDatabase getDatabase()
	{
		if (s_cc != null)
			return s_cc.getDatabase();
		log.severe("No Database Connection");
		return null;
	}   //  getDatabase

	/**
	 *  Get Database Adapter.<br/>
	 *  Access to database specific functionality.
	 *  @param URL JDBC connection url
	 *  @return iDempiere Database Adapter
	 */
	public static AdempiereDatabase getDatabase(String URL)
	{
		return Database.getDatabaseFromURL(URL);
	}   //  getDatabase

	/**
	 * 	Is connected to Oracle DB  ?
	 *	@return true if connected to Oracle
	 */
	public static boolean isOracle()
	{
		if (s_cc != null)
			return s_cc.isOracle();
		log.severe("No Database Connection");
		return false;
	}	//	isOracle

	/**
	 * 	Is connected to PostgreSQL DB ?
	 *	@return true if connected to PostgreSQL
	 */
	public static boolean isPostgreSQL()
	{
		if (s_cc != null)
			return s_cc.isPostgreSQL();
		log.severe("No Database");
		return false;
	}	//	isPostgreSQL

	/**
	 * 	Get Database Info
	 *	@return info
	 */
	public static String getDatabaseInfo()
	{
		if (s_cc != null)
			return s_cc.getDBInfo();
		return "No Database";
	}	//	getDatabaseInfo

	/**
	 *  Check database Version with Code version
	 *  @param ctx context
	 *  @return true if Database version (date) is the same
	 *  @deprecated
	 */
	@Deprecated (since="10", forRemoval=true)
	public static boolean isDatabaseOK (Properties ctx)
	{
		// Check Version
        String version = "?";
        String sql = "SELECT Version FROM AD_System";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try
        {
            pstmt = prepareStatement(sql, null);
            rs = pstmt.executeQuery();
            if (rs.next())
                version = rs.getString(1);
        }
        catch (SQLException e)
        {
            log.log(Level.SEVERE, "Problem with AD_System Table - Run system.sql script - " + e.toString());
            return false;
        }
        finally
        {
            close(rs);
            close(pstmt);
            rs= null;
            pstmt = null;
        }
        if (log.isLoggable(Level.INFO)) log.info("DB_Version=" + version);
        //  Identical DB version
        if (Adempiere.DB_VERSION.equals(version))
            return true;

        String AD_Message = "DatabaseVersionError";
        //  Code assumes Database version {0}, but Database has Version {1}.
        String msg = Msg.getMsg(ctx, AD_Message, new Object[] {Adempiere.DB_VERSION, version});   //  complete message
        System.err.println(msg);
        return false;
	}   //  isDatabaseOK

	/**
	 *  Check Build Version of Database against running client
	 *  @param ctx context
	 *  @return true if Database version (date) is the same
	 */
	public static boolean isBuildOK (Properties ctx)
	{
		//  Check Build
        String buildClient = Adempiere.getVersion();
        String buildDatabase = "";
        boolean failOnBuild = false;
        String sql = "SELECT LastBuildInfo, IsFailOnBuildDiffer FROM AD_System";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try
        {
            pstmt = prepareStatement(sql, null);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                buildDatabase = rs.getString(1);
                failOnBuild = rs.getString(2).equals("Y");
            }
        }
        catch (SQLException e)
        {
            log.log(Level.SEVERE, "Problem with AD_System Table - Run system.sql script - " + e.toString());
            return false;
        }
        finally
        {
            close(rs);
            close(pstmt);
            rs= null;
            pstmt = null;
        }
        if (log.isLoggable(Level.INFO)){
        	log.info("Build DB=" + buildDatabase);
        	log.info("Build Cl=" + buildClient);
        }
        //  Identical DB version
        if (buildClient.equals(buildDatabase))
            return true;

        String AD_Message = "BuildVersionError";
        // The program assumes build version {0}, but database has build Version {1}.
        String msg = Msg.getMsg(ctx, AD_Message, new Object[] {buildClient, buildDatabase});   //  complete message
        if (! failOnBuild) {
        	log.warning(msg);
        	return true;
        }
        
        log.log(Level.SEVERE, msg);
        return false;
	}   //  isDatabaseOK

	/**
	 *	Close DB connection profile
	 */
	public static void closeTarget()
	{

        boolean closed = false;

        //  CConnection
        if (s_cc != null)
        {
            closed = true;
            s_cc.setDataSource(null);
        }
        s_cc = null;
        if (closed)
            log.fine("closed");
	}	//	closeTarget

	/**
	 *	Create callable statement proxy
	 *  @param sql
	 *  @return Callable Statement
	 */
	public static CallableStatement prepareCall(String sql)
	{
		return prepareCall(sql, ResultSet.CONCUR_UPDATABLE, null);
	}

	/**
	 *	Create callable statement proxy
	 *  @param SQL
	 *  @param resultSetConcurrency
	 *  @param trxName
	 *  @return Callable Statement
	 */
	public static CallableStatement prepareCall(String SQL, int resultSetConcurrency, String trxName)
	{
		if (SQL == null || SQL.length() == 0)
			throw new IllegalArgumentException("Required parameter missing - " + SQL);
		return ProxyFactory.newCCallableStatement(ResultSet.TYPE_FORWARD_ONLY, resultSetConcurrency, SQL,
				trxName);
	}	//	prepareCall

	/**
	 *	Prepare Statement
	 *  @param sql
	 *  @return Prepared Statement
	 *  @deprecated
	 */
	@Deprecated
	public static CPreparedStatement prepareStatement (String sql)
	{
		return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, null);
	}	//	prepareStatement

	/**
	 *	Create prepare Statement proxy
	 *  @param sql
	 * 	@param trxName transaction
	 *  @return Prepared Statement
	 */
	public static CPreparedStatement prepareStatement (String sql, String trxName)
	{
		return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, trxName);
	}	//	prepareStatement

	/**
	 *	Create prepare Statement proxy
	 *  @param connection
	 *  @param sql
	 *  @return Prepared Statement
	 */
	public static CPreparedStatement prepareStatement (Connection connection, String sql)
	{
		return prepareStatement(connection, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	}	//	prepareStatement
	
	/**
	 *	Prepare Statement.
	 *  @param sql
	 *  @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE
	 *  @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 *  @return Prepared Statement
	 *  @deprecated
	 */
	@Deprecated
	public static CPreparedStatement prepareStatement (String sql,
		int resultSetType, int resultSetConcurrency)
	{
		return prepareStatement(sql, resultSetType, resultSetConcurrency, null);
	}	//	prepareStatement

	/**
	 *	Create prepare Statement proxy
	 *  @param sql
	 *  @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE
	 *  @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 * 	@param trxName transaction name
	 *  @return Prepared Statement
	 */
	public static CPreparedStatement prepareStatement(String sql,
		int resultSetType, int resultSetConcurrency, String trxName)
	{
		if (sql == null || sql.length() == 0)
			throw new IllegalArgumentException("No SQL");
		//
		return ProxyFactory.newCPreparedStatement(resultSetType, resultSetConcurrency, sql, trxName);
	}	//	prepareStatement

	/**
	 *	Create prepare Statement proxy
	 *  @param connection
	 *  @param sql sql statement
	 *  @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE
	 *  @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 *  @return Prepared Statement r/o or r/w depending on concur
	 */
	public static CPreparedStatement prepareStatement(Connection connection, String sql,
		int resultSetType, int resultSetConcurrency)
	{
		if (sql == null || sql.length() == 0)
			throw new IllegalArgumentException("No SQL");
		//
		return ProxyFactory.newCPreparedStatement(resultSetType, resultSetConcurrency, sql, connection);
	}	//	prepareStatement
	
	/**
	 *	Create Statement proxy
	 *  @return Statement
	 */
	public static Statement createStatement()
	{
		return createStatement (ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, null);
	}	//	createStatement

	/**
	 *	Create Statement Proxy.
	 *  @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE
	 *  @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 * 	@param trxName transaction name
	 *  @return Statement
	 */
	public static Statement createStatement(int resultSetType, int resultSetConcurrency, String trxName)
	{
		return ProxyFactory.newCStatement(resultSetType, resultSetConcurrency, trxName);
	}	//	createStatement

	/**
	 * Set parameters for given statement
	 * @param stmt statements
	 * @param params parameters array; if null or empty array, no parameters are set
	 */
	public static void setParameters(PreparedStatement stmt, Object[] params)
	throws SQLException
	{
		if (params == null || params.length == 0) {
			return;
		}
		//
		for (int i = 0; i < params.length; i++)
		{
			setParameter(stmt, i+1, params[i]);
		}
	}

	/**
	 * Set parameters for given statement
	 * @param stmt statements
	 * @param params parameters list; if null or empty list, no parameters are set
	 */
	public static void setParameters(PreparedStatement stmt, List<?> params)
	throws SQLException
	{
		if (params == null || params.size() == 0)
		{
			return;
		}
		for (int i = 0; i < params.size(); i++)
		{
			setParameter(stmt, i+1, params.get(i));
		}
	}

	/**
	 * Set PreparedStatement's parameter.<br/>
	 * Similar with calling <code>pstmt.setObject(index, param)</code>
	 * @param pstmt
	 * @param index
	 * @param param
	 * @throws SQLException
	 */
	public static void setParameter(PreparedStatement pstmt, int index, Object param)
	throws SQLException
	{
		if (param == null)
			pstmt.setObject(index, null);
		else if (param instanceof String)
			pstmt.setString(index, (String)param);
		else if (param instanceof Integer)
			pstmt.setInt(index, ((Integer)param).intValue());
		else if (param instanceof BigDecimal)
			pstmt.setBigDecimal(index, (BigDecimal)param);
		else if (param instanceof Timestamp)
			pstmt.setTimestamp(index, (Timestamp)param);
		else if (param instanceof Boolean)
			pstmt.setString(index, ((Boolean)param).booleanValue() ? "Y" : "N");
		else if (param instanceof byte[])
			pstmt.setBytes(index, (byte[]) param);
		else if (param instanceof Clob)
			pstmt.setClob(index, (Clob) param);
		else if (param.getClass().getName().equals("oracle.sql.BLOB"))
			pstmt.setObject(index, param);
		else
			throw new DBException("Unknown parameter type "+index+" - "+param);
	}

	/**
	 *	Execute Update.
	 *  saves "DBExecuteError" in Log
	 *  @param sql
	 *  @return number of rows updated or -1 if error
	 *  @deprecated
	 */
	@Deprecated
	public static int executeUpdate (String sql)
	{
		return executeUpdate(sql, null, false, null);
	}	//	executeUpdate

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, String)} instead.
	 *  @param sql
	 * 	@param trxName optional transaction name
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, String trxName)
	{
		return executeUpdate(sql, trxName, 0);
	}	//	executeUpdate

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, String, int)} instead.
	 *  @param sql
	 * 	@param trxName optional transaction name
	 *  @param timeOut optional timeout parameter
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, String trxName, int timeOut)
	{
		return executeUpdate(sql, null, false, trxName, timeOut);
	}	//	executeUpdate

	/**
	 *	Execute Update.
	 *  saves "DBExecuteError" in Log
	 *  @param sql
	 * 	@param ignoreError if true, no execution error is reported
	 *  @return number of rows updated or -1 if error
	 *  @deprecated
	 */
	@Deprecated
	public static int executeUpdate (String sql, boolean ignoreError)
	{
		return executeUpdate (sql, null, ignoreError, null);
	}	//	executeUpdate

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, String)} instead.
	 *  @param sql
	 * 	@param ignoreError if true, no execution error is reported
	 * 	@param trxName transaction
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, boolean ignoreError, String trxName)
	{
		return executeUpdate (sql, ignoreError, trxName, 0);
	}	//	executeUpdate

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, String, int)} instead.
	 *  @param sql
	 * 	@param ignoreError if true, no execution error is reported
	 * 	@param trxName transaction
	 *  @param timeOut optional timeOut parameter
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, boolean ignoreError, String trxName, int timeOut)
	{
		return executeUpdate (sql, null, ignoreError, trxName, timeOut);
	}

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, Object[], String)} instead.
	 *  @param sql
	 *  @param param int param
	 * 	@param trxName transaction
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, int param, String trxName)
	{
		return executeUpdate (sql, param, trxName, 0);
	}	//	executeUpdate

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, Object[], String, int)} instead.
	 *  @param sql
	 *  @param param int param
	 * 	@param trxName transaction
	 *  @param timeOut optional timeOut parameter
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, int param, String trxName, int timeOut)
	{
		return executeUpdate (sql, new Object[]{Integer.valueOf(param)}, false, trxName, timeOut);
	}	//	executeUpdate

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, Object[], String)} instead.
	 *  @param sql
	 *  @param param int parameter
	 * 	@param ignoreError if true, no execution error is reported
	 * 	@param trxName transaction
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, int param, boolean ignoreError, String trxName)
	{
		return executeUpdate (sql, param, ignoreError, trxName, 0);
	}	//	executeUpdate

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.
	 *  Developer is recommended to call {@link #executeUpdateEx(String, Object[], String, int)} instead.
	 *  @param sql
	 *  @param param int parameter
	 * 	@param ignoreError if true, no execution error is reported
	 * 	@param trxName transaction
	 *  @param timeOut optional timeOut parameter
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, int param, boolean ignoreError, String trxName, int timeOut)
	{
		return executeUpdate (sql, new Object[]{Integer.valueOf(param)}, ignoreError, trxName, timeOut);
	}	//	executeUpdate

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, Object[], String)} instead.
	 *  @param sql
	 *  @param params array of parameters
	 * 	@param ignoreError if true, no execution error is reported
	 * 	@param trxName optional transaction name
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, Object[] params, boolean ignoreError, String trxName)
	{
		return executeUpdate(sql, params, ignoreError, trxName, 0);
	}

	/**
	 *	Execute Update.<br/>
	 *  Saves "DBExecuteError" in Log.<br/>
	 *  Developer is recommended to call {@link #executeUpdateEx(String, Object[], String, int)} instead.
	 *  @param sql
	 *  @param params array of parameters
	 * 	@param ignoreError if true, no execution error is reported
	 * 	@param trxName optional transaction name
	 *  @param timeOut optional timeOut parameter
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdate (String sql, Object[] params, boolean ignoreError, String trxName, int timeOut)
	{
		if (sql == null || sql.length() == 0)
			throw new IllegalArgumentException("Required parameter missing - " + sql);
		verifyTrx(trxName);
		//
		int no = -1;
		CPreparedStatement cs = ProxyFactory.newCPreparedStatement(ResultSet.TYPE_FORWARD_ONLY,
			ResultSet.CONCUR_UPDATABLE, sql, trxName);	//	converted in call

		try
		{
			setParameters(cs, params);
			//set timeout
			if (timeOut > 0)
			{
				cs.setQueryTimeout(timeOut);
			}
			no = cs.executeUpdate();
		}
		catch (Exception e)
		{
			e = getSQLException(e);
			if (ignoreError)
				log.log(Level.SEVERE, cs.getSql() + " [" + trxName + "] - " +  e.getMessage());
			else
			{
				log.log(Level.SEVERE, cs.getSql() + " [" + trxName + "]", e);
				String msg = DBException.getDefaultDBExceptionMessage(e);
				log.saveError (msg != null ? msg : "DBExecuteError", e);
			}
		}
		finally
		{
			//  Always close cursor
			close(cs);
			cs = null;
		}
		return no;
	}	//	executeUpdate

	/**
	 * Execute update and throw DBException if there are errors.
	 * @param sql
	 * @param params statement parameters
	 * @param trxName transaction
	 * @return number of rows updated
	 * @throws SQLException
	 */
	public static int executeUpdateEx (String sql, Object[] params, String trxName) throws DBException
	{
		return executeUpdateEx(sql, params, trxName, 0);
	}

	/**
	 * Execute update and throw DBException if there are errors.
	 * @param sql
	 * @param params statement parameters
	 * @param trxName transaction
	 * @param timeOut optional timeOut parameter
	 * @return number of rows updated
	 * @throws DBException
	 */
	public static int executeUpdateEx (String sql, Object[] params, String trxName, int timeOut) throws DBException
	{
		if (sql == null || sql.length() == 0)
			throw new IllegalArgumentException("Required parameter missing - " + sql);
		//
		verifyTrx(trxName);
		int no = -1;
		CPreparedStatement cs = ProxyFactory.newCPreparedStatement(ResultSet.TYPE_FORWARD_ONLY,
			ResultSet.CONCUR_UPDATABLE, sql, trxName);	//	converted in call

		try
		{
			setParameters(cs, params);
			if (timeOut > 0)
			{
				{
					cs.setQueryTimeout(timeOut);
				}
			}
			no = cs.executeUpdate();
		}
		catch (Exception e)
		{
			throw new DBException(e);
		}
		finally
		{
			close(cs);
			cs = null;
		}
		return no;
	}

	/**
	 *	Execute multiple Update statements.<br/>
	 *  Saves (last) "DBExecuteError" in Log.
	 *  @param sql multiple sql statements separated by "; " SQLSTATEMENT_SEPARATOR
	 * 	@param ignoreError if true, no execution error is reported
	 * 	@param trxName optional transaction name
	 *  @return number of rows updated or -1 if error
	 */
	public static int executeUpdateMultiple (String sql, boolean ignoreError, String trxName)
	{
		if (sql == null || sql.length() == 0)
			throw new IllegalArgumentException("Required parameter missing - " + sql);
		int index = sql.indexOf(SQLSTATEMENT_SEPARATOR);
		if (index == -1)
			return executeUpdate(sql, null, ignoreError, trxName);
		int no = 0;
		//
		String statements[] = sql.split(SQLSTATEMENT_SEPARATOR);
		for (int i = 0; i < statements.length; i++)
		{
			if (log.isLoggable(Level.FINE)) log.fine(statements[i]);
			no += executeUpdate(statements[i], null, ignoreError, trxName);
		}

		return no;
	}	//	executeUpdareMultiple

	/**
	 * Execute update and throw DBException if there are errors.
	 * @param sql
	 * @param trxName
	 * @see {@link #executeUpdateEx(String, Object[], String)}
	 */
	public static int executeUpdateEx (String sql, String trxName) throws DBException
	{
		return executeUpdateEx(sql, trxName, 0);
	}	//	executeUpdateEx

	/**
	 * Execute update and throw DBException if there are errors.
	 * @param sql
	 * @param trxName
	 * @param timeOut
	 * @see {@link #executeUpdateEx(String, Object[], String)}
	 */
	public static int executeUpdateEx (String sql, String trxName, int timeOut) throws DBException
	{
		return executeUpdateEx(sql, null, trxName, timeOut);
	}	//	executeUpdateEx

	/**
	 *	Commit transaction
	 *  @param throwException if true, re-throws exception
	 * 	@param trxName transaction name
	 *  @return true if not needed (trxName is null) or success
	 *  @throws SQLException
	 */
	public static boolean commit (boolean throwException, String trxName) throws SQLException,IllegalStateException
	{
        // Not on transaction scope, Connection are thus auto commit
        if (trxName == null)
        {
            return true;
        }

		try
		{
			Trx trx = Trx.get(trxName, false);
			if (trx != null)
				return trx.commit(true);

            if (throwException)
            {
                throw new IllegalStateException("Could not load transation with identifier: " + trxName);
            }
            else
            {
                return false;
            }
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "[" + trxName + "]", e);
			if (throwException)
				throw e;
			return false;
		}
	}	//	commit

	/**
	 *	Rollback transaction
	 *  @param throwException if true, re-throws exception
	 * 	@param trxName transaction name
	 *  @return true if not needed (trxName is null) or success
	 *  @throws SQLException
	 */
	public static boolean rollback (boolean throwException, String trxName) throws SQLException
	{
		// Not on transaction scope, Connection are thus auto commit/rollback
        if (trxName == null)
        {
            return true;
        }
        
		try
		{
			Trx trx = Trx.get(trxName, false);
			if (trx != null)
				return trx.rollback(true);
			
			if (throwException)
            {
                throw new IllegalStateException("Could not load transation with identifier: " + trxName);
            }
            else
            {
                return false;
            }
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "[" + trxName + "]", e);
			if (throwException)
				throw e;
			return false;
		}
	}	//	commit

	/**
	 * 	Get Row Set.<br/>
	 * 	When a Rowset is closed, it also closes the underlying connection.
	 *	@param sql
	 *	@return row set or null
	 */
	public static RowSet getRowSet (String sql)
	{
		CStatementVO info = new CStatementVO (RowSet.TYPE_SCROLL_INSENSITIVE, RowSet.CONCUR_READ_ONLY, DB.getDatabase().convertStatement(sql));
		CPreparedStatement stmt = null;
		RowSet retValue = null;
		try {
			stmt = ProxyFactory.newCPreparedStatement(info);
			retValue = stmt.getRowSet();
		} finally {
			close(stmt);			
		}
		return retValue;
	}	//	getRowSet

    /**
     * Get int Value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return first value or -1 if not found
     * @throws DBException if there is any SQLException
     */
    public static int getSQLValueEx (String trxName, String sql, Object... params) throws DBException
    {
    	int retValue = -1;
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	Connection conn = null; 
    	if (trxName == null)
    		conn = DB.createConnection(true, Connection.TRANSACTION_READ_COMMITTED);
    	try
    	{
    		if (conn != null)
    		{
    			conn.setAutoCommit(false);
    			conn.setReadOnly(true);
    		}
    		
    		if (conn != null)
    			pstmt = prepareStatement(conn, sql);
    		else
    			pstmt = prepareStatement(sql, trxName);
    		setParameters(pstmt, params);
    		rs = pstmt.executeQuery();
    		if (rs.next())
    			retValue = rs.getInt(1);
    		else
    			if (log.isLoggable(Level.FINE)) log.fine("No Value " + sql);
    	}
    	catch (SQLException e)
    	{
    		if (conn != null)
    		{
    			try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
    		}
    		throw new DBException(e, sql);
    	}
    	finally
    	{
    		close(rs, pstmt);
    		rs = null; pstmt = null;
    		if (conn != null)
    		{
    			closeAndResetReadonlyConnection(conn);
    		}
    	}
    	return retValue;
    }

    /**
     * Reset connection's auto commit to true and read only to false before closing it.
     * @param conn
     */
	private static void closeAndResetReadonlyConnection(Connection conn) {
		try {
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			conn.setReadOnly(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}		
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

    /**
     * Get int value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params collection of parameters
     * @return first value or -1
     * @throws DBException if there is any SQLException
     */
    public static int getSQLValueEx (String trxName, String sql, List<Object> params)
    {
		return getSQLValueEx(trxName, sql, params.toArray(new Object[params.size()]));
    }

    /**
     * Get int Value from sql.<br/>
     * Developer is recommended to call {@link #getSQLValueEx(String, String, Object...)} instead.
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return first value or -1 if not found or error
     */
    public static int getSQLValue (String trxName, String sql, Object... params)
    {
    	int retValue = -1;
    	try
    	{
    		retValue = getSQLValueEx(trxName, sql, params);
    	}
    	catch (Exception e)
    	{
    		log.log(Level.SEVERE, sql, getSQLException(e));
    	}
    	return retValue;
    }

    /**
     * Get int value from sql.<br/>
     * Developer is recommended to call {@link #getSQLValueEx(String, String, List)} instead.
     * @param trxName optional transaction name
     * @param sql
     * @param params collection of parameters
     * @return first value or null
     */
    public static int getSQLValue (String trxName, String sql, List<Object> params)
    {
		return getSQLValue(trxName, sql, params.toArray(new Object[params.size()]));
    }

    /**
     * Get string value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return first value or null
     * @throws DBException if there is any SQLException
     */
    public static String getSQLValueStringEx (String trxName, String sql, Object... params)
    {
    	String retValue = null;
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	Connection conn = null;
    	if (trxName == null)
    		conn = DB.createConnection(true, Connection.TRANSACTION_READ_COMMITTED);
    	try
    	{
    		if (conn != null)
    		{
    			conn.setAutoCommit(false);
    			conn.setReadOnly(true);
    		}
    		
    		if (conn != null)
    			pstmt = prepareStatement(conn, sql);
    		else
    			pstmt = prepareStatement(sql, trxName);
    		setParameters(pstmt, params);
    		rs = pstmt.executeQuery();
    		if (rs.next())
    			retValue = rs.getString(1);
    		else
    			if (log.isLoggable(Level.FINE)) log.fine("No Value " + sql);
    	}
    	catch (SQLException e)
    	{
    		if (conn != null)
    		{
    			try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
    		}
    		throw new DBException(e, sql);
    	}
    	finally
    	{
    		close(rs, pstmt);
    		rs = null; pstmt = null;
    		if (conn != null)
    		{
    			closeAndResetReadonlyConnection(conn);
    		}
    	}
    	return retValue;
    }

    /**
     * Get String Value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params collection of parameters
     * @return first value or null
     * @throws DBException if there is any SQLException
     */
    public static String getSQLValueStringEx (String trxName, String sql, List<Object> params)
    {
		return getSQLValueStringEx(trxName, sql, params.toArray(new Object[params.size()]));
    }

    /**
     * Get String Value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return first value or null
     */
    public static String getSQLValueString (String trxName, String sql, Object... params)
    {
    	String retValue = null;
    	try
    	{
    		retValue = getSQLValueStringEx(trxName, sql, params);
    	}
    	catch (Exception e)
    	{
    		log.log(Level.SEVERE, sql, getSQLException(e));
    	}
    	return retValue;
    }

    /**
     * Get string value from sql.<br/>
     * Developer is recommended to call {@link #getSQLValueStringEx(String, String, List)} instead.
     * @param trxName optional transaction name
     * @param sql
     * @param params collection of parameters
     * @return first value or null
     */
    public static String getSQLValueString (String trxName, String sql, List<Object> params)
    {
		return getSQLValueString(trxName, sql, params.toArray(new Object[params.size()]));
    }

    /**
     * Get BigDecimal value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return first value or null if not found
     * @throws DBException if there is any SQLException
     */
    public static BigDecimal getSQLValueBDEx (String trxName, String sql, Object... params) throws DBException
    {
    	BigDecimal retValue = null;
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	Connection conn = null;
    	if (trxName == null)
    		conn = DB.createConnection(true, Connection.TRANSACTION_READ_COMMITTED);
    	try
    	{
    		if (conn != null)
    		{
    			conn.setAutoCommit(false);
    			conn.setReadOnly(true);
    		}
    		
    		if (conn != null)
    			pstmt = prepareStatement(conn, sql);
    		else
    			pstmt = prepareStatement(sql, trxName);
    		setParameters(pstmt, params);
    		rs = pstmt.executeQuery();
    		if (rs.next())
    			retValue = rs.getBigDecimal(1);
    		else
    			if (log.isLoggable(Level.FINE)) log.fine("No Value " + sql);
    	}
    	catch (SQLException e)
    	{
    		if (conn != null)
    		{
    			try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
    		}
    		throw new DBException(e, sql);
    	}
    	finally
    	{
    		close(rs, pstmt);
    		rs = null; pstmt = null;
    		if (conn != null)
    		{
    			closeAndResetReadonlyConnection(conn);
    		}
    	}
    	return retValue;
    }

    /**
     * Get BigDecimal Value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params collection of parameters
     * @return first value or null if not found
     * @throws DBException if there is any SQLException
     */
    public static BigDecimal getSQLValueBDEx (String trxName, String sql, List<Object> params) throws DBException
    {
		return getSQLValueBDEx(trxName, sql, params.toArray(new Object[params.size()]));
    }

    /**
     * Get BigDecimal Value from sql.<br/>
     * Developer is recommended to call {@link #getSQLValueBDEx(String, String, Object...)} instead.
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return first value or null
     */
    public static BigDecimal getSQLValueBD (String trxName, String sql, Object... params)
    {
    	try
    	{
    		return getSQLValueBDEx(trxName, sql, params);
    	}
    	catch (Exception e)
    	{
    		log.log(Level.SEVERE, sql, getSQLException(e));
    	}
    	return null;
    }

    /**
     * Get BigDecimal Value from sql.<br/>
     * Developer is recommended to call {@link #getSQLValueBDEx(String, String, List)} instead.
     * @param trxName optional transaction name
     * @param sql
     * @param params collection of parameters
     * @return first value or null
     */
    public static BigDecimal getSQLValueBD (String trxName, String sql, List<Object> params)
    {
		return getSQLValueBD(trxName, sql, params.toArray(new Object[params.size()]));
    }

    /**
     * Get Timestamp Value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return first value or null
     * @throws DBException if there is any SQLException
     */
    public static Timestamp getSQLValueTSEx (String trxName, String sql, Object... params)
    {
    	Timestamp retValue = null;
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	Connection conn = null;
    	if (trxName == null)
    		conn = DB.createConnection(true, Connection.TRANSACTION_READ_COMMITTED);
    	try
    	{
    		if (conn != null)
    		{
    			conn.setAutoCommit(false);
    			conn.setReadOnly(true);
    		}
    		
    		if (conn != null)
    			pstmt = prepareStatement(conn, sql);
    		else
    			pstmt = prepareStatement(sql, trxName);
    		setParameters(pstmt, params);
    		rs = pstmt.executeQuery();
    		if (rs.next())
    			retValue = rs.getTimestamp(1);
    		else
    			if (log.isLoggable(Level.FINE)) log.fine("No Value " + sql);
    	}
    	catch (SQLException e)
    	{
    		if (conn != null)
    		{
    			try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
    		}
    		throw new DBException(e, sql);
    	}
    	finally
    	{
    		close(rs, pstmt);
    		rs = null; pstmt = null;
    		if (conn != null)
    		{
    			closeAndResetReadonlyConnection(conn);
    		}
    	}
    	return retValue;
    }

    /**
     * Get Timestamp Value from sql
     * @param trxName optional transaction name
     * @param sql
     * @param params collection of parameters
     * @return first value or null if not found
     * @throws DBException if there is any SQLException
     */
    public static Timestamp getSQLValueTSEx (String trxName, String sql, List<Object> params) throws DBException
    {
		return getSQLValueTSEx(trxName, sql, params.toArray(new Object[params.size()]));
    }

    /**
     * Get Timestamp Value from sql.<br/>
     * Developer is recommended to call {@link #getSQLValueTSEx(String, String, Object...)} instead.
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return first value or null
     */
    public static Timestamp getSQLValueTS (String trxName, String sql, Object... params)
    {
    	try
    	{
    		return getSQLValueTSEx(trxName, sql, params);
    	}
    	catch (Exception e)
    	{
    		log.log(Level.SEVERE, sql, getSQLException(e));
    	}
    	return null;
    }

    /**
     * Get Timestamp Value from sql.<br/>
     * Developer is recommended to call {@link #getSQLValueTSEx(String, String, List)} instead.
     * @param trxName optional transaction name
     * @param sql
     * @param params collection of parameters
     * @return first value or null
     */
    public static Timestamp getSQLValueTS (String trxName, String sql, List<Object> params)
    {
		Object[] arr = new Object[params.size()];
		params.toArray(arr);
		return getSQLValueTS(trxName, sql, arr);
    }

	/**
	 * Get Array of Key Name Pairs
	 * @param sql select with id / name as first / second column
	 * @param optional if true (-1,"") is added
	 * @return array of {@link KeyNamePair}
	 * @see #getKeyNamePairs(String, boolean, Object...)
	 */
	public static KeyNamePair[] getKeyNamePairs(String sql, boolean optional)
	{
		return getKeyNamePairs(sql, optional, (Object[])null);
	}

	/**
	 * Get Array of Key Name Pairs
	 * @param sql select with id / name as first / second column
	 * @param optional if true (-1,"") is added
	 * @return array of {@link KeyNamePair}
	 * @see #getKeyNamePairs(String, boolean, Object...)
	 */
	public static KeyNamePair[] getKeyNamePairsEx(String sql, boolean optional)
	{
		return getKeyNamePairsEx(sql, optional, (Object[])null);
	}
	
	/**
	 * Get Array of Key Name Pairs
	 * @param sql select with id / name as first / second column
	 * @param optional if true (-1,"") is added
	 * @param params query parameters
	 */
	public static KeyNamePair[] getKeyNamePairs(String sql, boolean optional, Object ... params)
	{
		return getKeyNamePairs(null, sql, optional, params);
	}

	/**
	 * Get Array of Key Name Pairs
	 * @param sql select with id / name as first / second column
	 * @param optional if true (-1,"") is added
	 * @param params query parameters
	 */
	public static KeyNamePair[] getKeyNamePairsEx(String sql, boolean optional, Object ... params)
	{
		return getKeyNamePairsEx(null, sql, optional, params);
	}
	
	/**
	 * Get Array of Key Name Pairs
	 * @param trxName
	 * @param sql select with id / name as first / second column
	 * @param optional if true (-1,"") is added
	 * @param params query parameters
	 * @return Array of Key Name Pairs
	 */
	public static KeyNamePair[] getKeyNamePairs(String trxName, String sql, boolean optional, Object ... params)
	{
		try 
		{
			return getKeyNamePairsEx(trxName, sql, optional, params);		
		} 
		catch (Exception e)
        {
            log.log(Level.SEVERE, sql, getSQLException(e));
        }
		return new KeyNamePair[0];
	}
	
	/**
	 * Get Array of Key Name Pairs
	 * @param trxName
	 * @param sql select with id / name as first / second column
	 * @param optional if true (-1,"") is added
	 * @param params query parameters
	 * @return Array of Key Name Pairs
	 */
	public static KeyNamePair[] getKeyNamePairsEx(String trxName, String sql, boolean optional, Object ... params)
	{
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Connection conn = null; 
    	if (trxName == null)
    		conn = DB.createConnection(true, Connection.TRANSACTION_READ_COMMITTED);
        ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();
        if (optional)
        {
            list.add (new KeyNamePair(-1, ""));
        }
        try
        {
        	if (conn != null)
    		{
    			conn.setAutoCommit(false);
    			conn.setReadOnly(true);
    		}
        	if (conn != null)
        		pstmt = prepareStatement(conn, sql);
        	else
        		pstmt = DB.prepareStatement(sql, trxName);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();
            while (rs.next())
            {
                list.add(new KeyNamePair(rs.getInt(1), rs.getString(2)));
            }
        }
        catch (SQLException e)
        {
        	if (conn != null)
    		{
    			try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
    		}
            throw new DBException(e.getMessage(), e);
        }
        finally
        {
            close(rs, pstmt);
            rs= null;
            pstmt = null;
            if (conn != null)
    		{
    			closeAndResetReadonlyConnection(conn);
    		}
        }
        KeyNamePair[] retValue = new KeyNamePair[list.size()];
        list.toArray(retValue);
        return retValue;
	}	//	getKeyNamePairs

	/**
	 * Get Array of IDs
	 * @param trxName
	 * @param sql select with id as first column
	 * @param params query parameters
     * @throws DBException if there is any SQLException
     */
	public static int[] getIDsEx(String trxName, String sql, Object ... params) throws DBException
	{
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList<Integer> list = new ArrayList<Integer>();
        try
        {
            pstmt = DB.prepareStatement(sql, trxName);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();
            while (rs.next())
            {
                list.add(rs.getInt(1));
            }
        }
        catch (SQLException e)
        {
    		throw new DBException(e, sql);
        }
        finally
        {
            close(rs, pstmt);
            rs= null;
            pstmt = null;
        }
		//	Convert to array
		int[] retValue = new int[list.size()];
		for (int i = 0; i < retValue.length; i++)
		{
			retValue[i] = list.get(i);
		}
        return retValue;
	}	//	getIDsEx
	
	/**
	 * 	Is Sales Order Trx.<br/>
	 * 	Assumes Sales Order. Query IsSOTrx value of table with where clause
	 *	@param TableName table
	 *	@param whereClause where clause
	 *  @param windowNo
	 *	@return true (default) or false if tested that not SO
	 */
	public static boolean isSOTrx (String TableName, String whereClause, int windowNo)
	{
        if (TableName == null || TableName.length() == 0)
        {
            log.severe("No TableName");
            return true;
        }
        if (whereClause == null || whereClause.length() == 0)
        {
            log.severe("No Where Clause");
            return true;
        }
        //
        Boolean isSOTrx = null;
        boolean noIsSOTrxColumn = false;
        if (MTable.get(Env.getCtx(), TableName).getColumn("IsSOTrx") == null) {
        	noIsSOTrxColumn = true;
        } else {
        	String sql = "SELECT IsSOTrx FROM " + TableName
        	+ " WHERE " + whereClause;
        	PreparedStatement pstmt = null;
        	ResultSet rs = null;
        	try
        	{
        		pstmt = DB.prepareStatement (sql, null);
        		rs = pstmt.executeQuery ();
        		if (rs.next ())
        			isSOTrx = Boolean.valueOf("Y".equals(rs.getString(1)));
        	}
        	catch (Exception e)
        	{
        		noIsSOTrxColumn = true;
        	}
        	finally
        	{
        		close(rs, pstmt);
        		rs= null;
        		pstmt = null;
        	}
        }
        if (noIsSOTrxColumn && TableName.endsWith("Line")) {
        	noIsSOTrxColumn = false;
        	String hdr = TableName.substring(0, TableName.indexOf("Line"));
        	if (MTable.get(Env.getCtx(), hdr) == null || MTable.get(Env.getCtx(), hdr).getColumn("IsSOTrx") == null) {
        		noIsSOTrxColumn = true;
        	} else {
        		// use IN instead of EXISTS as the subquery should be highly selective
        		String sql = "SELECT IsSOTrx FROM " + hdr
        		+ " h WHERE h." + hdr + "_ID IN (SELECT l." + hdr + "_ID FROM " + TableName
        		+ " l WHERE " + whereClause + ")";
        		PreparedStatement pstmt2 = null;
        		ResultSet rs2 = null;
        		try
        		{
        			pstmt2 = DB.prepareStatement (sql, null);
        			rs2 = pstmt2.executeQuery ();
        			if (rs2.next ())
        				isSOTrx = Boolean.valueOf("Y".equals(rs2.getString(1)));
        		}
        		catch (Exception ee)
        		{
        			noIsSOTrxColumn = true;
        		}
        		finally
        		{
        			close(rs2, pstmt2);
        			rs2= null;
        			pstmt2 = null;
        		}
        	}
        }
        if (noIsSOTrxColumn)
        	if (log.isLoggable(Level.FINEST))log.log(Level.FINEST, TableName + " - No SOTrx");
        if (isSOTrx == null) {
        	if (windowNo >= 0) {
        		// check context
        		isSOTrx = Boolean.valueOf("Y".equals(Env.getContext(Env.getCtx(), windowNo, "IsSOTrx")));
        	} else {
            	isSOTrx = Boolean.TRUE;
        	}
        }
        return isSOTrx.booleanValue();
	}	//	isSOTrx

	/**
	 * Delegate to {@link #isSOTrx(String, String, int)} with -1 for windowNo parameter.
	 * @param TableName
	 * @param whereClause
	 * @return true (default) or false if tested that not SO
	 */
	public static boolean isSOTrx (String TableName, String whereClause) {
		return isSOTrx (TableName, whereClause, -1);
	}

	/**
	 *	Get next id for table
	 *  @param ctx client
	 *  @param TableName table name
	 * 	@param trxName optional transaction name
	 *  @return next id no
	 */
	public static int getNextID (Properties ctx, String TableName, String trxName)
	{
		if (ctx == null)
			throw new IllegalArgumentException("Context missing");
		if (TableName == null || TableName.length() == 0)
			throw new IllegalArgumentException("TableName missing");
		return getNextID(Env.getAD_Client_ID(ctx), TableName, trxName);
	}	//	getNextID

	/**
	 *	Get next id for table
	 *  @param AD_Client_ID client
	 *  @param TableName table name
	 * 	@param trxName optional Transaction Name
	 *  @return next id no
	 *  @see {@link MSequence#getNextID(int, String, String)}
	 */
	public static int getNextID (int AD_Client_ID, String TableName, String trxName)
	{
		return MSequence.getNextID (AD_Client_ID, TableName, trxName);
	}	//	getNextID

	/**
	 * 	Get Document No based on Document Type (backward compatibility)
	 *	@param C_DocType_ID document type
	 * 	@param trxName optional Transaction Name
	 *	@return document no or null
	 *  @deprecated
	 */
	@Deprecated
	public static String getDocumentNo(int C_DocType_ID, String trxName)
	{
		return MSequence.getDocumentNo (C_DocType_ID, trxName, false);
	}	//	getDocumentNo

	/**
	 * 	Get Document No based on Document Type
	 *	@param C_DocType_ID document type
	 * 	@param trxName optional Transaction Name
	 *  @param definite asking for a definitive or temporary sequence
	 *	@return document no or null
	 */
	public static String getDocumentNo(int C_DocType_ID, String trxName, boolean definite) {
		return getDocumentNo(C_DocType_ID, trxName, definite, null);
	}

	/**
	 * 	Get Document No based on Document Type
	 *	@param C_DocType_ID document type
	 * 	@param trxName optional Transaction Name
	 *  @param definite asking for a definitive or temporary sequence
	 *  @param po PO
	 *	@return document no or null
	 *  @see {@link MSequence#getDocumentNo(int, String, boolean, PO)}
	 */
	public static String getDocumentNo(int C_DocType_ID, String trxName, boolean definite, PO po)
	{
		return MSequence.getDocumentNo (C_DocType_ID, trxName, definite, po);
	}	//	getDocumentNo

	/**
	 * 	Get Document No for table
	 *	@param AD_Client_ID client
	 *	@param TableName table name
	 * 	@param trxName optional Transaction Name
	 *	@return document no or null
	 */
	public static String getDocumentNo (int AD_Client_ID, String TableName, String trxName)
	{
		return getDocumentNo(AD_Client_ID, TableName, trxName, null);
	}

	/**
	 * 	Get Document No for table
	 *	@param AD_Client_ID client
	 *	@param TableName table name
	 * 	@param trxName optional Transaction Name
	 *  @param po
	 *	@return document no or null
	 *  @see {@link MSequence#getDocumentNo(int, String, String, PO)}
	 */
	public static String getDocumentNo (int AD_Client_ID, String TableName, String trxName, PO po)
	{
		String dn = MSequence.getDocumentNo (AD_Client_ID, TableName, trxName, po);
		if (dn == null)
			throw new DBException ("No DocumentNo");
		return dn;
	}	//	getDocumentNo

	/**
	 *	Get Document Number for current document.
	 *  <br>
	 *  - first search for DocumentNo based on DocType from environment context<br/>
	 *  - then search for DocumentNo based on TableName
	 *  @param ctx context
	 *  @param WindowNo window
	 *  @param TableName table
	 *  @param onlyDocType Do not search for document no based on TableName
	 * 	@param trxName optional Transaction Name
	 *	@return DocumentNo or null, if no doc number defined
	 */
	public static String getDocumentNo (Properties ctx, int WindowNo,
		String TableName, boolean onlyDocType, String trxName)
	{
		if (ctx == null || TableName == null || TableName.length() == 0)
			throw new IllegalArgumentException("Required parameter missing");
		int AD_Client_ID = Env.getContextAsInt(ctx, WindowNo, "AD_Client_ID");

		//	Get C_DocType_ID from context - NO Defaults -
		int C_DocType_ID = Env.getContextAsInt(ctx, WindowNo + "|C_DocTypeTarget_ID");
		if (C_DocType_ID == 0)
			C_DocType_ID = Env.getContextAsInt(ctx, WindowNo + "|C_DocType_ID");
		if (C_DocType_ID == 0)
		{
			if (log.isLoggable(Level.FINE)) log.fine("Window=" + WindowNo
				+ " - Target=" + Env.getContextAsInt(ctx, WindowNo + "|C_DocTypeTarget_ID") + "/" + Env.getContextAsInt(ctx, WindowNo, "C_DocTypeTarget_ID")
				+ " - Actual=" + Env.getContextAsInt(ctx, WindowNo + "|C_DocType_ID") + "/" + Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID"));
			return getDocumentNo (AD_Client_ID, TableName, trxName);
		}

		String retValue = getDocumentNo (C_DocType_ID, trxName, false);
		if (!onlyDocType && retValue == null)
			return getDocumentNo (AD_Client_ID, TableName, trxName);
		return retValue;
	}	//	getDocumentNo

	/**
	 * 	Is this a remote client connection.
	 *
	 *  Deprecated, always return false.
	 *	@return true if client and RMI or Objects on Server
	 *  @deprecated
	 */
	@Deprecated (forRemoval=true)
	public static boolean isRemoteObjects()
	{
		return false;
	}	//	isRemoteObjects

	/**
	 * 	Is this a remote client connection
	 *
	 *  Deprecated, always return false.
	 *	@return true if client and RMI or Process on Server
	 *  @deprecated
	 */
	@Deprecated (forRemoval=true)
	public static boolean isRemoteProcess()
	{
		return false;
	}	//	isRemoteProcess

	/**
	 *	Print SQL Warnings.
	 *  <br>
	 *		Usage: DB.printWarning("comment", rs.getWarnings());
	 *  @param comment comment
	 *  @param warning warning
	 */
	public static void printWarning (String comment, SQLWarning warning)
	{
		if (comment == null || warning == null || comment.length() == 0)
			return;
		log.warning(comment);
		//
		SQLWarning warn = warning;
		while (warn != null)
		{
			StringBuilder buffer = new StringBuilder();
			buffer.append(warn.getMessage())
				.append("; State=").append(warn.getSQLState())
				.append("; ErrorCode=").append(warn.getErrorCode());
			log.warning(buffer.toString());
			warn = warn.getNextWarning();
		}
	}	//	printWarning

	/**
	 *  Create SQL TO Date String from Timestamp
	 *
	 *  @param  time Date to be converted
	 *  @param  dayOnly true if time set to 00:00:00
	 *
	 *  @return TO_DATE('2001-01-30 18:10:20',''YYYY-MM-DD HH24:MI:SS')
	 *      or  TO_DATE('2001-01-30',''YYYY-MM-DD')
	 */
	public static String TO_DATE (Timestamp time, boolean dayOnly)
	{
		return s_cc.getDatabase().TO_DATE(time, dayOnly);
	}   //  TO_DATE

	/**
	 *  Create SQL TO Date String from Timestamp
	 *  @param day day time
	 *  @return TO_DATE String (day only)
	 */
	public static String TO_DATE (Timestamp day)
	{
		return TO_DATE(day, true);
	}   //  TO_DATE

	/**
	 *  Create SQL for formatted Date, Number
	 *
	 *  @param  columnName  the column name in the SQL
	 *  @param  displayType Display Type
	 *  @param  AD_Language 6 character language setting (from Env.LANG_*)
	 *
	 *  @return TRIM(TO_CHAR(columnName,'999G999G999G990D00','NLS_NUMERIC_CHARACTERS='',.'''))
	 *      or TRIM(TO_CHAR(columnName,'TM9')) depending on DisplayType and Language
	 *  @see org.compiere.util.DisplayType
	 *  @see org.compiere.util.Env
	 *
	 *   */
	public static String TO_CHAR (String columnName, int displayType, String AD_Language)
	{
		if (columnName == null || AD_Language == null || columnName.length() == 0)
			throw new IllegalArgumentException("Required parameter missing");
		return s_cc.getDatabase().TO_CHAR(columnName, displayType, AD_Language);
	}   //  TO_CHAR

	/**
	 * 	Return number as string for INSERT statements with correct precision
	 *	@param number number
	 *	@param displayType display Type
	 *	@return number as string
	 */
	public static String TO_NUMBER (BigDecimal number, int displayType)
	{
		return s_cc.getDatabase().TO_NUMBER(number, displayType);
	}	//	TO_NUMBER

	/**
	 *  Package Strings for SQL command in quotes
	 *  @param txt  String with text
	 *  @return escaped string for sql statement (NULL if null)
	 */
	public static String TO_STRING (String txt)
	{
		return TO_STRING (txt, 0);
	}   //  TO_STRING

	/**
	 *	Package Strings for SQL command in quotes.
	 *  <pre>
	 *	    -	include in ' (single quotes)
	 *	    -	replace ' with ''
	 *  </pre>
	 *  @param txt  String with text
	 *  @param maxLength    Maximum Length of content or 0 to ignore
	 *  @return escaped string for sql statement (NULL if null)
	 */
	public static String TO_STRING (String txt, int maxLength)
	{
		if (txt == null || txt.length() == 0)
			return "NULL";

		//  Length
		String text = txt;
		if (maxLength != 0 && text.length() > maxLength)
			text = txt.substring(0, maxLength);

		//  copy characters		(we need to look through anyway)
		StringBuilder out = new StringBuilder();
		out.append(QUOTE);		//	'
		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (c == QUOTE)
				out.append("''");
			else
				out.append(c);
		}
		out.append(QUOTE);		//	'
		//
		return out.toString();
	}	//	TO_STRING
	
	/**
	 * 	Return string as JSON object for INSERT statements with correct precision
	 *	@param value
	 *	@return value as json
	 */
	public static String TO_JSON (String value)
	{
		return s_cc.getDatabase().TO_JSON(value);
	}
	
	/**
	 *	@return string with right casting for JSON inserts
	 */
	public static String getJSONCast()
	{
		return s_cc.getDatabase().getJSONCast();
	}

	/**
	 * Convenient method to close result set
	 * @param rs
	 */
	public static void close( ResultSet rs) {
        try {
            if (rs!=null) rs.close();
        } catch (SQLException e) {
            ;
        }
    }

	/**
	 * Convenient method to close statement
	 * @param st
	 */
    public static void close( Statement st) {
        try {
            if (st!=null) st.close();
        } catch (SQLException e) {
            ;
        }
    }

    /**
     * Convenient method to close result set and statement
     * @param rs result set
     * @param st statement
     * @see #close(ResultSet)
     * @see #close(Statement)
     */
    public static void close(ResultSet rs, Statement st) {
    	close(rs);
    	close(st);
    }

    /**
     * Convenient method to close a {@link POResultSet}
     * @param rs result set
     * @see POResultSet#close()
     */
    public static void close(POResultSet<?> rs) {
    	if (rs != null)
    		rs.close();
    }

	/**
	 * Try to get the SQLException from Exception
	 * @param e Exception
	 * @return SQLException if found or provided exception elsewhere
	 */
    public static Exception getSQLException(Exception e)
    {
    	Throwable e1 = e;
    	while (e1 != null)
    	{
	    	if (e1 instanceof SQLException)
	    		return (SQLException)e1;
	    	e1 = e1.getCause();
    	}
    	return e;
    }

	/** Quote			*/
	private static final char QUOTE = '\'';

    // Following methods are kept for BeanShell compatibility.
	// See BF [ 2030233 ] Remove duplicate code from DB class
    // TODO: remove this when BeanShell will support varargs methods
    public static int getSQLValue (String trxName, String sql)
    {
    	return getSQLValue(trxName, sql, new Object[]{});
    }
    public static int getSQLValue (String trxName, String sql, int int_param1)
    {
    	return getSQLValue(trxName, sql, new Object[]{int_param1});
    }
    public static int getSQLValue (String trxName, String sql, int int_param1, int int_param2)
    {
    	return getSQLValue(trxName, sql, new Object[]{int_param1, int_param2});
    }
    public static int getSQLValue (String trxName, String sql, String str_param1)
    {
    	return getSQLValue(trxName, sql, new Object[]{str_param1});
    }
    public static int getSQLValue (String trxName, String sql, int int_param1, String str_param2)
    {
    	return getSQLValue(trxName, sql, new Object[]{int_param1, str_param2});
    }
    public static String getSQLValueString (String trxName, String sql, int int_param1)
    {
    	return getSQLValueString(trxName, sql, new Object[]{int_param1});
    }
    public static BigDecimal getSQLValueBD (String trxName, String sql, int int_param1)
    {
    	return getSQLValueBD(trxName, sql, new Object[]{int_param1});
    }
    //End BeanShell compatibility.

	/**
	 * Get Array of ValueNamePair items.
	 * <pre> Example:
	 * String sql = "SELECT Name, Description FROM AD_Ref_List WHERE AD_Reference_ID=?";
	 * ValueNamePair[] list = DB.getValueNamePairs(sql, false, params);
	 * </pre>
	 * @param sql SELECT Value_Column, Name_Column FROM ...
	 * @param optional if {@link ValueNamePair#EMPTY} is added
	 * @param params query parameters
	 * @return array of {@link ValueNamePair} or empty array
     * @throws DBException if there is any SQLException
	 */
	public static ValueNamePair[] getValueNamePairs(String sql, boolean optional, List<Object> params)
	{
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList<ValueNamePair> list = new ArrayList<ValueNamePair>();
        if (optional)
        {
            list.add (ValueNamePair.EMPTY);
        }
        try
        {
            pstmt = DB.prepareStatement(sql, null);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();
            while (rs.next())
            {
                list.add(new ValueNamePair(rs.getString(1), rs.getString(2)));
            }
        }
        catch (SQLException e)
        {
            throw new DBException(e, sql);
        }
        finally
        {
            close(rs, pstmt);
            rs = null; pstmt = null;
        }
		return list.toArray(new ValueNamePair[list.size()]);
	}

	/**
	 * Get Array of KeyNamePair items.
	 * <pre> Example:
	 * String sql = "SELECT C_City_ID, Name FROM C_City WHERE C_City_ID=?";
	 * KeyNamePair[] list = DB.getKeyNamePairs(sql, false, params);
	 * </pre>
	 * @param sql SELECT ID_Column, Name_Column FROM ...
	 * @param optional if {@link ValueNamePair#EMPTY} is added
	 * @param params query parameters
	 * @return array of {@link KeyNamePair} or empty array
     * @throws DBException if there is any SQLException
	 */
	public static KeyNamePair[] getKeyNamePairs(String sql, boolean optional, List<Object> params)
	{
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();
        if (optional)
        {
            list.add (KeyNamePair.EMPTY);
        }
        try
        {
            pstmt = DB.prepareStatement(sql, null);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();
            while (rs.next())
            {
                list.add(new KeyNamePair(rs.getInt(1), rs.getString(2)));
            }
        }
        catch (SQLException e)
        {
            throw new DBException(e, sql);
        }
        finally
        {
            close(rs, pstmt);
            rs = null; pstmt = null;
        }
		return list.toArray(new KeyNamePair[list.size()]);
	}

	/**
	 * Insert selection into T_Selection table.<br/>
	 * Keeping this method for backward compatibility.
	 * refer: IDEMPIERE-1970
	 * @param AD_PInstance_ID
	 * @param selection
	 * @param trxName
	 */
	public static void createT_Selection(int AD_PInstance_ID, Collection<Integer> selection, String trxName)
	{
		StringBuilder insert = new StringBuilder();
		insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ");
		int counter = 0;
		for(Integer selectedId : selection)
		{
			counter++;
			if (counter > 1)
				insert.append(" UNION ");
			insert.append("SELECT ");
			insert.append(AD_PInstance_ID);
			insert.append(", ");
			insert.append(selectedId);
			insert.append(" FROM DUAL ");

			if (counter >= 1000)
			{
				DB.executeUpdateEx(insert.toString(), trxName);
				insert = new StringBuilder();
				insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ");
				counter = 0;
			}
		}
		if (counter > 0)
		{
			DB.executeUpdateEx(insert.toString(), trxName);
		}
	}

	/**
	 * Insert selection into T_Selection table.<br/>
	 * saveKeys is map with rowID as key and list of viewID as value. 
	 * @param AD_PInstance_ID
	 * @param saveKeys - Collection of KeyNamePair
	 * @param trxName
	 */
	public static void createT_SelectionNew (int AD_PInstance_ID, Collection<KeyNamePair> saveKeys, String trxName) {
		Collection<NamePair> saveKeysNP = new ArrayList<NamePair>();
		for (NamePair saveKey : saveKeys)
			saveKeysNP.add(saveKey);
		createT_SelectionNewNP(AD_PInstance_ID, saveKeysNP, trxName);
	}

	/**
	 * Insert selection into T_Selection table.<br/>
	 * saveKeys is map with rowID as key and list of viewID as value. 
	 * @param AD_PInstance_ID
	 * @param saveKeys can receive a Collection of KeyNamePair (IDs) or ValueNamePair (UUIDs)
	 * @param trxName
	 */
	public static void createT_SelectionNewNP (int AD_PInstance_ID, Collection<NamePair> saveKeys, String trxName)
	{
		String initialInsert = "INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID, T_SELECTION_UU, ViewID) ";
		StringBuilder insert = new StringBuilder(initialInsert);
		int counter = 0;
		for(NamePair saveKey : saveKeys)
		{
			Object selectedId;
			if (saveKey instanceof KeyNamePair)
				selectedId = ((KeyNamePair)saveKey).getKey();
			else if (saveKey instanceof ValueNamePair)
				selectedId = ((ValueNamePair)saveKey).getValue();
			else
				throw new AdempiereException("NamePair type not allowed in DB.createT_SelectionNewNP, just KeyNamePair or ValueNamePair are allowed");
			counter++;
			if (counter > 1)
				insert.append(" UNION ");
			insert.append("SELECT ");
			insert.append(AD_PInstance_ID);
			insert.append(", ");
			if (selectedId instanceof Integer) {
				insert.append((Integer)selectedId);
				insert.append(", ' '");
			} else {
				insert.append("0, ");
				insert.append(DB.TO_STRING(selectedId.toString()));
			}
			insert.append(", ");
			
			String viewIDValue = saveKey.getName();
			// when no process have viewID or this process have no viewID or value of viewID is null
			if (viewIDValue == null){
				insert.append("NULL");
			}else{
				insert.append(DB.TO_STRING(viewIDValue));
			}
			
			insert.append(" FROM DUAL ");

			if (counter >= 1000)
			{
				DB.executeUpdateEx(insert.toString(), trxName);
				insert.delete(0,  insert.length());
				insert.append(initialInsert);
				counter = 0;
			}
		}
		if (counter > 0)
		{
			DB.executeUpdateEx(insert.toString(), trxName);
		}
	}

	private static boolean m_isUUIDVerified = false;
	private static boolean m_isUUIDSupported = false;
	
	/**
	 * Is DB support generate_uuid function
	 * @return true if current db have working generate_uuid function. generate_uuid doesn't work on 64 bit postgresql
	 * on windows yet.
	 */
	public static boolean isGenerateUUIDSupported() {
		if (! m_isUUIDVerified) {
			String uuidTest = null;
			try {
				uuidTest = getSQLValueStringEx(null, "SELECT Generate_UUID() FROM Dual");
			} catch (Exception e) {}
			m_isUUIDSupported = uuidTest != null && uuidTest.trim().length() == 36;
			m_isUUIDVerified = true;
		}
		return m_isUUIDSupported;
	}

	/**
	 * Throw DBException if trxName doesn't return an existing Trx instance.
	 * @param trxName
	 */
	private static void verifyTrx(String trxName) {
		if (trxName != null && Trx.get(trxName, false) == null) {
			// Using a trx that was previously closed or never opened
			// probably timed out - throw Exception (IDEMPIERE-644)
			String msg = "Transaction closed or never opened ("+trxName+") => (maybe timed out)";
			log.severe(msg); // severe
			throw new DBException(msg);
		}
	}

	/**
	 * Is table or view exists
	 * @param tableName
	 * @return true if table or view with name=tableName exists in db
	 */
	public static boolean isTableOrViewExists(String tableName) {
		Connection conn = getConnection();
		ResultSet rs = null;
		try {
			DatabaseMetaData metadata = conn.getMetaData();
			String tblName;
			if (metadata.storesUpperCaseIdentifiers())
				tblName = tableName.toUpperCase();
			else if (metadata.storesLowerCaseIdentifiers())
				tblName = tableName.toLowerCase();
			else
				tblName = tableName;
			rs = metadata.getTables(null, null, tblName, null);
			if (rs.next()) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DB.close(rs);
			try {
				conn.close();
			} catch (SQLException e) {}
		}
		return false;
	}

    /**
     * Get a list of objects from sql (one per each column in the select clause), column indexing starts with 0
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return null if not found
     * @throws DBException if there is any SQLException
     */
	public static List<Object> getSQLValueObjectsEx(String trxName, String sql, Object... params) {
		List<Object> retValue = new ArrayList<Object>();
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	Connection conn = null;
    	if (trxName == null)
    		conn = DB.createConnection(true, Connection.TRANSACTION_READ_COMMITTED);
    	try
    	{
    		if (conn != null)
    		{
    			conn.setAutoCommit(false);
    			conn.setReadOnly(true);
    		}
    		
    		if (conn != null)
    			pstmt = prepareStatement(conn, sql);
    		else
    			pstmt = prepareStatement(sql, trxName);
    		setParameters(pstmt, params);
    		rs = pstmt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
    		if (rs.next()) {
    			for (int i=1; i<=rsmd.getColumnCount(); i++) {
    				Object obj = rs.getObject(i);
        			if (rs.wasNull())
        				retValue.add(null);
        			else
        				retValue.add(obj);
    			}
    		} else {
    			retValue = null;
    		}
    	}
    	catch (SQLException e)
    	{
    		if (conn != null)
    		{
    			try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
    		}
    		throw new DBException(e, sql);
    	}
    	finally
    	{
    		close(rs, pstmt);
    		rs = null; pstmt = null;
    		if (conn != null)
    		{
    			closeAndResetReadonlyConnection(conn);
    		}
    	}
    	return retValue;
	}

    /**
     * Get a list of object list from sql (one object list per each row, and in the object list, one object per each column in the select clause), 
     * column indexing starts with 0.<br/>
     * WARNING: This method must be used just for queries returning few records, using it for many records implies heavy memory consumption
     * @param trxName optional transaction name
     * @param sql
     * @param params array of parameters
     * @return null if not found
     * @throws DBException if there is any SQLException
     */
	public static List<List<Object>> getSQLArrayObjectsEx(String trxName, String sql, Object... params) {
		List<List<Object>> rowsArray = new ArrayList<List<Object>>();
    	PreparedStatement pstmt = null;
    	ResultSet rs = null;
    	Connection conn = null;
    	if (trxName == null)
    		conn = DB.createConnection(true, Connection.TRANSACTION_READ_COMMITTED);
    	try
    	{
    		if (conn != null)
    		{
    			conn.setAutoCommit(false);
    			conn.setReadOnly(true);
    		}
    		
    		if (conn != null)
    			pstmt = prepareStatement(conn, sql);
    		else
    			pstmt = prepareStatement(sql, trxName);
    		setParameters(pstmt, params);
    		rs = pstmt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
    		while (rs.next()) {
    			List<Object> retValue = new ArrayList<Object>();
    			for (int i=1; i<=rsmd.getColumnCount(); i++) {
    				Object obj = rs.getObject(i);
        			if (rs.wasNull())
        				retValue.add(null);
        			else
        				retValue.add(obj);
    			}
    			rowsArray.add(retValue);
    		}
    	}
    	catch (SQLException e)
    	{
    		if (conn != null)
    		{
    			try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
    		}
    		throw new DBException(e, sql);
    	}
    	finally
    	{
    		close(rs, pstmt);
    		rs = null; pstmt = null;
    		if (conn != null)
    		{
    			closeAndResetReadonlyConnection(conn);
    		}
    	}
    	if (rowsArray.size() == 0)
    		return null;
    	return rowsArray;
	}

	/**
	 *	Create Read Replica Prepared Statement proxy
	 *  @param sql
	 * 	@param trxName transaction
	 *  @return Prepared Statement (from replica if possible, otherwise normal statement)
	 */
	public static PreparedStatement prepareNormalReadReplicaStatement(String sql, String trxName) {
		return prepareNormalReadReplicaStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, trxName);
	}

	/**
	 *	Create Read Replica Prepared Statement proxy
	 *  @param sql
	 *  @param resultSetType - ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE
	 *  @param resultSetConcurrency - ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE
	 * 	@param trxName transaction name
	 *  @return Prepared Statement (from replica if possible, otherwise normal statement)
	 */
	private static PreparedStatement prepareNormalReadReplicaStatement(String sql, int resultSetType, int resultSetConcurrency, String trxName) {
		if (sql == null || sql.length() == 0)
			throw new IllegalArgumentException("No SQL");
		boolean useReadReplica = MSysConfig.getValue(MSysConfig.DB_READ_REPLICA_URLS) != null;
		if (   trxName == null
			&& useReadReplica
			&& resultSetType == ResultSet.TYPE_FORWARD_ONLY
			&& resultSetConcurrency == ResultSet.CONCUR_READ_ONLY) {
			// this is a candidate for a read replica connection (read-only, forward-only, no-trx), try to obtain one, otherwise fallback to normal
			CPreparedStatement stmt = ProxyFactory.newReadReplicaPreparedStatement(resultSetType, resultSetConcurrency, sql);
			if (stmt != null) {
				return stmt;
			}
		}
		//
		return ProxyFactory.newCPreparedStatement(resultSetType, resultSetConcurrency, sql, trxName);
	}
	
	/**
	 * Create IN clause for csv value
	 * @param columnName
	 * @param csv comma separated value
	 * @return IN clause
	 */
	public static String inClauseForCSV(String columnName, String csv) 
	{
		return inClauseForCSV(columnName, csv, false);
	}
	
	/**
	 * Create IN clause for csv value
	 * @param columnName
	 * @param csv comma separated value
	 * @param isNotClause true to append NOT before IN
	 * @return IN clause
	 */
	public static String inClauseForCSV(String columnName, String csv, boolean isNotClause) 
	{
		StringBuilder builder = new StringBuilder();
		builder.append(columnName);
		
		if(isNotClause)
			builder.append(" NOT ");
		
		builder.append(" IN (");
		String[] values = csv.split("[,]");
		for(int i = 0; i < values.length; i++)
		{
			if (i > 0)
				builder.append(",");
			String key = values[i];
			if (columnName.endsWith("_ID")) 
			{
				builder.append(key);
			}
			else
			{
				if (key.startsWith("\"") && key.endsWith("\"")) 
				{
					key = key.substring(1, key.length()-1);
				}
				builder.append(TO_STRING(key));
			}
		}
		builder.append(")");
		return builder.toString();
	}
	
	/**
	 * Create subset clause for csv value (i.e columnName is a subset of the csv value set)
	 * @param columnName
	 * @param csv
	 * @return subset sql clause
	 */
	public static String subsetClauseForCSV(String columnName, String csv)
	{
		return getDatabase().subsetClauseForCSV(columnName, csv);
	}
	
	/**
	 * Create intersect clause for csv value (i.e columnName is an intersect with the csv value set)
	 * @param columnName
	 * @param csv
	 * @return intersect sql clause
	 */
	public static String intersectClauseForCSV(String columnName, String csv)
	{
		return intersectClauseForCSV(columnName, csv, false);
	}
	
	/**
	 * Create intersect clause for csv value (i.e columnName is an intersect with the csv value set)
	 * @param columnName
	 * @param csv
	 * @param isNotClause true to append NOT before the intersect clause
	 * @return intersect sql clause
	 */
	public static String intersectClauseForCSV(String columnName, String csv, boolean isNotClause)
	{
		return getDatabase().intersectClauseForCSV(columnName, csv, isNotClause);
	}
	
	/**
	 * Is sql a SELECT statement
	 * @param sql
	 * @return true if it is a SELECT statement
	 */
	public static boolean isSelectStatement(String sql) {
		String removeComments = "/\\*(?:.|[\\n\\r])*?\\*/";
		String removeQuotedStrings = "'(?:.|[\\n\\r])*?'";
		String removeLeadingSpaces = "^\\s+";
		String cleanSql = sql.toLowerCase().replaceAll(removeComments, "").replaceAll(removeQuotedStrings, "").replaceFirst(removeLeadingSpaces, "");
		if(cleanSql.matches("^select\\s.*$") && !cleanSql.contains(";"))
			return true;
		else
			return false;
	}

}	//	DB
