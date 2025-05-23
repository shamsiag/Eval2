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

/**
 *	LDAP Server Model
 *	
 *  @author Jorg Janke
 *  @version $Id$
 */
public class MLdapProcessor extends X_AD_LdapProcessor implements AdempiereProcessor
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -1477519989047580644L;

	/**
	 * 	Get Active LDAP Processor
	 *	@return array of processors
	 */
	public static MLdapProcessor[] getActive(Properties ctx)
	{
		ArrayList<MLdapProcessor> list = new ArrayList<MLdapProcessor>();
		String sql = "SELECT * FROM AD_LdapProcessor WHERE IsActive='Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MLdapProcessor (ctx, rs, null));
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
		MLdapProcessor[] retValue = new MLdapProcessor[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getActive
	
	/**	Logger	*/
	private static CLogger s_log = CLogger.getCLogger (MLdapProcessor.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_LdapProcessor_UU  UUID key
     * @param trxName Transaction
     */
    public MLdapProcessor(Properties ctx, String AD_LdapProcessor_UU, String trxName) {
        super(ctx, AD_LdapProcessor_UU, trxName);
    }

	/**
	 * 	Ldap Processor
	 *	@param ctx context
	 *	@param AD_LdapProcessor_ID id
	 *	@param trxName transaction
	 */
	public MLdapProcessor(Properties ctx, int AD_LdapProcessor_ID, String trxName)
	{
		super (ctx, AD_LdapProcessor_ID, trxName);
	}	//	MLdapProcessor

	/**
	 * 	Ldap Processor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MLdapProcessor(Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MLdapProcessor
	
	/** Array of Clients		*/
	private MClient[]		m_clients = null;
	/** Array of Interest Areas	*/
	private MInterestArea[]	m_interests = null;
	
	private int				m_auth = 0;
	private int				m_ok = 0;
	private int				m_error = 0;
	
	/**
	 * 	Get Server ID
	 *	@return id
	 */
	@Override
	public String getServerID ()
	{
		return "Ldap" + get_ID();
	}	//	getServerID

	/**
	 * 	Get Info
	 *	@return info
	 */
	public String getInfo()
	{
		StringBuilder msgreturn = new StringBuilder("Auth=").append(m_auth) 
				.append(", OK=").append(m_ok).append(", Error=").append(m_error);
		return msgreturn.toString();
	}	//	getInfo
	
	/**
	 * 	Get Date Next Run
	 *	@param requery requery
	 *	@return date next run
	 */
	public Timestamp getDateNextRun (boolean requery)
	{
		if (requery)
			load(get_TrxName());
		return getDateNextRun();
	}	//	getDateNextRun

	/**
	 * 	Get Logs
	 *	@return logs
	 */
	public AdempiereProcessorLog[] getLogs ()
	{
		ArrayList<MLdapProcessorLog> list = new ArrayList<MLdapProcessorLog>();
		String sql = "SELECT * "
			+ "FROM AD_LdapProcessorLog "
			+ "WHERE AD_LdapProcessor_ID=? " 
			+ "ORDER BY Created DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getAD_LdapProcessor_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new MLdapProcessorLog (getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		MLdapProcessorLog[] retValue = new MLdapProcessorLog[list.size ()];
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
		StringBuilder sql = new StringBuilder("DELETE FROM AD_LdapProcessorLog ")
			.append("WHERE AD_LdapProcessor_ID=").append(getAD_LdapProcessor_ID()) 
			.append(" AND (Created+").append(getKeepLogDays()).append(") < getDate()");
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		return no;
	}	//	deleteLog

	/**
	 * 	Get Frequency (n/a)
	 *	@return 1
	 */
	public int getFrequency()
	{
		return 1;
	}	//	getFrequency

	/**
	 * 	Get Frequency Type (n/a)
	 *	@return FREQUENCYTYPE_Minute
	 */
	public String getFrequencyType()
	{
		return X_AD_Schedule.FREQUENCYTYPE_Minute;
	}	//	getFrequencyType
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder ("MLdapProcessor[");
		sb.append (get_ID()).append ("-").append (getName())
			.append (",Port=").append (getLdapPort())
			.append ("]");
		return sb.toString ();
	}	//	toString
		
	/**
	 * 	Authenticate and Authorize
	 *  @param ldapUser MLdapUser object
	 *	@param usr user name
	 *	@param o organization = Client Name
	 *	@param ou optional organization unit = Interest Group
	 *  @return ldapUser MLdapUser with updated information
	 */
	public MLdapUser authenticate(MLdapUser ldapUser, String usr, String o, String ou)
	{
		// Ensure something to return
		if (ldapUser == null)
			ldapUser = new MLdapUser();
		
		StringBuilder error = null;
		StringBuilder info = null;

		//	User
		if (usr == null || usr.trim().length () == 0)
		{
			error = new StringBuilder("@NotFound@ User");
			ldapUser.setErrorString(error.toString());
			m_error++;
			log.warning (error.toString());
			return ldapUser;
		}
		usr = usr.trim();
		//	Client
		if (o == null || o.length () == 0)
		{
			error = new StringBuilder("@NotFound@ O");
			ldapUser.setErrorString(error.toString());
			m_error++;
			log.warning (error.toString());
			return ldapUser;
		}
		int AD_Client_ID = findClient(o);
		if (AD_Client_ID == 0)
		{
			error = new StringBuilder("@NotFound@ O=").append(o);
			ldapUser.setErrorString(error.toString());
			m_error++;
			if (log.isLoggable(Level.CONFIG)) log.config (error.toString());
			return ldapUser;
		}
		//	Optional Interest Area
		int R_InterestArea_ID = 0;
		if (ou != null && ou.length () > 0)
		{
			R_InterestArea_ID = findInterestArea (AD_Client_ID, ou);
			if (R_InterestArea_ID == 0)
			{
				error = new StringBuilder("@NotFound@ OU=").append(ou);
				ldapUser.setErrorString(error.toString());
				m_error++;
				if (log.isLoggable(Level.CONFIG)) log.config (error.toString());
				return ldapUser;
			}
		}

		m_auth++;
		//	Query 1 - Validate User
		int AD_User_ID = 0;
		String Value = null;
		String LdapUser = null;
		String EMail = null;
		String Name = null;
		String Password = null;
		boolean IsActive = false;
		String EMailVerify = null;	//	 is timestamp
		boolean isUnique = false;
		//
		String sql = "SELECT AD_User_ID, Value, LdapUser, EMail,"	//	1..4
			+ " Name, Password, IsActive, EMailVerify "
			+ "FROM AD_User "
			+ "WHERE AD_Client_ID=? AND (EMail=? OR Value=? OR LdapUser=?)";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, AD_Client_ID);
			pstmt.setString (2, usr);
			pstmt.setString (3, usr);
			pstmt.setString (4, usr);
			rs = pstmt.executeQuery ();
			if (rs.next())
			{
				AD_User_ID = rs.getInt (1);
				Value = rs.getString (2);
				LdapUser = rs.getString (3);
				EMail = rs.getString (4);
				//
				Name = rs.getString (5);
				Password = rs.getString (6);
				IsActive = "Y".equals (rs.getString (7));
				EMailVerify	= rs.getString (8);
				isUnique = rs.next();
			}
 		}
		catch (Exception e)
		{
			log.log (Level.SEVERE, sql, e);
			error = new StringBuilder("System Error");
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		if (error != null)
		{
			m_error++;
			ldapUser.setErrorString(error.toString());
			return ldapUser;
		}
		//
		if (AD_User_ID == 0)
		{
			error = new StringBuilder("@NotFound@ User=").append(usr);
			info = new StringBuilder("User not found - ").append(usr);
		}
		else if (!IsActive)
		{
			error = new StringBuilder("@NotFound@ User=").append(usr);
			info = new StringBuilder("User not active - ").append(usr);
		}
		else if (EMailVerify == null)
		{
			error = new StringBuilder("@UserNotVerified@ User=").append(usr);
			info = new StringBuilder("User EMail not verified - ").append(usr);
		}
		else if (usr.equalsIgnoreCase(LdapUser))
			info = new StringBuilder("User verified - Ldap=").append(usr) 
				.append((isUnique ? "" : " - Not Unique"));
		else if (usr.equalsIgnoreCase(Value)) 
			info = new StringBuilder("User verified - Value=").append(usr) 
				.append((isUnique ? "" : " - Not Unique"));
		else if (usr.equalsIgnoreCase(EMail)) 
			info = new StringBuilder("User verified - EMail=").append(usr) 
				.append((isUnique ? "" : " - Not Unique"));
		else 
			info = new StringBuilder("User verified ?? ").append(usr)
				.append(" - Name=").append(Name) 
				.append(", Ldap=").append(LdapUser).append(", Value=").append(Value)
				.append((isUnique ? "" : " - Not Unique"));

		//	Error
		if (error != null)	//	should use Language of the User
		{
			logAccess (AD_Client_ID, AD_User_ID, R_InterestArea_ID, info.toString(), error.toString());
			ldapUser.setErrorString(Msg.translate (getCtx(), error.toString()));
			return ldapUser;
		}
		//	Done
		if (R_InterestArea_ID == 0)
		{
			logAccess (AD_Client_ID, AD_User_ID, R_InterestArea_ID, info.toString(), null);
			ldapUser.setOrg(o);
			ldapUser.setOrgUnit(ou);
			ldapUser.setUserId(usr);
			ldapUser.setPassword(Password);
			return ldapUser;
		}
		
		//	Query 2 - Validate Subscription
		String OptOutDate = null;
		boolean found = false;
		sql = "SELECT IsActive, OptOutDate "
			+ "FROM R_ContactInterest "
			+ "WHERE R_InterestArea_ID=? AND AD_User_ID=?";
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, R_InterestArea_ID);
			pstmt.setInt (2, AD_User_ID);
			rs = pstmt.executeQuery ();
			if (rs.next())
			{
				found = true;
				IsActive = "Y".equals (rs.getString (1));
				OptOutDate	= rs.getString (2);
				isUnique = rs.next();
			}
 		}
		catch (Exception e)
		{
			log.log (Level.SEVERE, sql, e);
			error = new StringBuilder("System Error (2)");
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		//	System Error
		if (error != null)
		{
			m_error++;
			ldapUser.setErrorString(error.toString());
			return ldapUser;
		}
		
		if (!found)
		{
			error = new StringBuilder("@UserNotSubscribed@ User=").append(usr);
			info = new StringBuilder("No User Interest - ").append(usr) 
				.append(" - R_InterestArea_ID=").append(R_InterestArea_ID);
		}
		else if (OptOutDate != null)
		{
			error = new StringBuilder("@UserNotSubscribed@ User=").append(usr).append(" @OptOutDate@=").append(OptOutDate);
			info = new StringBuilder("Opted out - ").append(usr).append(" - OptOutDate=").append(OptOutDate);
		}
		else if (!IsActive)
		{
			error = new StringBuilder("@UserNotSubscribed@ User=").append(usr);
			info = new StringBuilder("User Interest Not Active - ").append(usr); 
		}
		else
			info = new StringBuilder("User subscribed - ").append(usr);
		
		
		if (error != null)	//	should use Language of the User
		{
			logAccess (AD_Client_ID, AD_User_ID, R_InterestArea_ID, info.toString(), error.toString());
			ldapUser.setErrorString(Msg.translate (getCtx(), error.toString()));
			return ldapUser;
		}
		//	Done
		logAccess (AD_Client_ID, AD_User_ID, R_InterestArea_ID, info.toString(), null);
		ldapUser.setOrg(o);
		ldapUser.setOrgUnit(ou);
		ldapUser.setUserId(usr);
		ldapUser.setPassword(Password);
		return ldapUser;
	}	//	authenticate
	
	/**
	 * 	Find Client
	 *	@param client client name
	 *	@return AD_Client_ID
	 */
	private int findClient (String client)
	{
		if (m_clients == null)
			m_clients = MClient.getAll(getCtx());
		for (int i = 0; i < m_clients.length; i++)
		{
			if ((client.equalsIgnoreCase (m_clients[i].getValue())))
				return m_clients[i].getAD_Client_ID ();
		}
		return 0;
	}	//	findClient
	
	/**
	 * 	Find Interest Area
	 *	@param AD_Client_ID
	 *  @param interestArea
	 *	@return R_InterestArea_ID
	 */
	private int findInterestArea (int AD_Client_ID, String interestArea)
	{
		if (m_interests == null)
			m_interests = MInterestArea.getAll(getCtx());
		for (int i = 0; i < m_interests.length; i++)
		{
			if (AD_Client_ID == m_interests[i].getAD_Client_ID()
				&& interestArea.equalsIgnoreCase (m_interests[i].getValue ()))
				return m_interests[i].getR_InterestArea_ID();
		}
		return 0;
	}	//	findInterestArea
	
	/**
	 * 	Log Access
	 * 	@param AD_Client_ID client
	 *	@param AD_User_ID user
	 *	@param R_InterestArea_ID interest area
	 *	@param info info
	 *	@param error error
	 */
	private void logAccess (int AD_Client_ID,
		int AD_User_ID, int R_InterestArea_ID,
		String info, String error)
	{
		if (error != null)
		{
			log.log (Level.CONFIG, info);
			m_error++;
		}
		else
		{
			log.log (Level.INFO, info);
			m_ok++;
		}
		//
		MLdapAccess access = new MLdapAccess (getCtx(), 0, null);
		access.setAD_Client_ID (AD_Client_ID);
		access.setAD_User_ID (AD_User_ID);
		access.setR_InterestArea_ID (R_InterestArea_ID);
		access.setIsError (error != null);
		access.setSummary (info);
		access.saveEx();
	}	//	logAccess

	@Override
	public String getScheduleType() {
		return MSchedule.SCHEDULETYPE_Frequency;
	}

	@Override
	public String getCronPattern() {
	   return null;
	}

}	//	MLdapProcessor
