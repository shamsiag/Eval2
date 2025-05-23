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
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 * 	Issue Project (and Asset Link).<br/>
 *  Note: Not fully implemented and have been marked as inactive in Application Dictionary.
 *	
 *  @author Jorg Janke
 *  @version $Id: MIssueProject.java,v 1.2 2006/07/30 00:58:18 jjanke Exp $
 *  @deprecated
 */
@Deprecated
public class MIssueProject extends X_R_IssueProject
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -9115162283984109370L;

	/**
	 * 	Get/Set Project
	 *	@param issue issue
	 *	@return project
	 */
	static public MIssueProject get (MIssue issue)
	{
		if (issue.getName() == null)
			return null;
		MIssueProject pj = null;
		String sql = "SELECT * FROM R_IssueProject WHERE Name=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setString (1, issue.getName());
			rs = pstmt.executeQuery ();
			if (rs.next ())
				pj = new MIssueProject(issue.getCtx(), rs, null);
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
		//	New
		if (pj == null)
		{
			pj = new MIssueProject(issue.getCtx(), 0, null);
			pj.setName(issue.getName());
			pj.setA_Asset_ID(issue);
		}
		pj.setSystemStatus(issue.getSystemStatus());
		pj.setStatisticsInfo(issue.getStatisticsInfo());
		pj.setProfileInfo(issue.getProfileInfo());
		if (!pj.save())
			return null;
		
		//	Set 
		issue.setR_IssueProject_ID(pj.getR_IssueProject_ID());
		if (pj.getA_Asset_ID() != 0)
			issue.setA_Asset_ID(pj.getA_Asset_ID());
		return pj;
	}	//	get
	
	/**	Logger	*/
	private static CLogger s_log = CLogger.getCLogger (MIssueProject.class);
		
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param R_IssueProject_UU  UUID key
     * @param trxName Transaction
     */
    public MIssueProject(Properties ctx, String R_IssueProject_UU, String trxName) {
        super(ctx, R_IssueProject_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param R_IssueProject_ID id
	 *	@param trxName trx
	 */
	public MIssueProject (Properties ctx, int R_IssueProject_ID, String trxName)
	{
		super (ctx, R_IssueProject_ID, trxName);
	}	//	MIssueProject

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName trx
	 */
	public MIssueProject (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MIssueProject
	
	/**
	 * 	Set A_Asset_ID
	 *	@param issue issue
	 */
	public void setA_Asset_ID (MIssue issue)
	{
		int A_Asset_ID = 0;
		super.setA_Asset_ID (A_Asset_ID);
	}	//	setA_Asset_ID
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MIssueProject[");
		sb.append (get_ID())
			.append ("-").append (getName())
			.append(",A_Asset_ID=").append(getA_Asset_ID())
			.append(",C_Project_ID=").append(getC_Project_ID())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
}	//	MIssueProject
