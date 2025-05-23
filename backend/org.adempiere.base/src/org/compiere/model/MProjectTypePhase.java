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
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * 	Project Type Phase Model
 *
 *	@author Jorg Janke
 *	@version $Id: MProjectTypePhase.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
public class MProjectTypePhase extends X_C_Phase
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -5111329904215151458L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_Phase_UU  UUID key
     * @param trxName Transaction
     */
    public MProjectTypePhase(Properties ctx, String C_Phase_UU, String trxName) {
        super(ctx, C_Phase_UU, trxName);
		if (Util.isEmpty(C_Phase_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_Phase_ID id
	 *	@param trxName trx
	 */
	public MProjectTypePhase (Properties ctx, int C_Phase_ID, String trxName)
	{
		super (ctx, C_Phase_ID, trxName);
		if (C_Phase_ID == 0)
			setInitialDefaults();
	}	//	MProjectTypePhase

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setSeqNo (0);
		setStandardQty (Env.ZERO);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName
	 */
	public MProjectTypePhase (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProjectTypePhase

	/**
	 * 	Get Project Type Tasks
	 *	@return Array of MProjectTypeTask
	 */
	public MProjectTypeTask[] getTasks()
	{
		ArrayList<MProjectTypeTask> list = new ArrayList<MProjectTypeTask>();
		String sql = "SELECT * FROM C_Task WHERE C_Phase_ID=? AND IsActive='Y' ORDER BY SeqNo, C_Task_ID";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getC_Phase_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add(new MProjectTypeTask (getCtx(), rs, get_TrxName()));
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
		MProjectTypeTask[] retValue = new MProjectTypeTask[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getTasks

}	//	MProjectTypePhase
