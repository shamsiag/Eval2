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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

/**
 * 	Replication Run Model
 *
 *  @author Jorg Janke
 *  @version $Id: MReplicationRun.java,v 1.2 2006/07/30 00:51:05 jjanke Exp $
 */
public class MReplicationRun extends X_AD_Replication_Run
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 7609847179225634980L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_Replication_Run_UU  UUID key
     * @param trxName Transaction
     */
    public MReplicationRun(Properties ctx, String AD_Replication_Run_UU, String trxName) {
        super(ctx, AD_Replication_Run_UU, trxName);
    }

	/**
	 * 	Create new Replication Run
	 *	@param ctx context
	 *	@param AD_Replication_ID id
	 *	@param dateRun date
	 *  @param trxName
	 */
	public MReplicationRun (Properties ctx, int AD_Replication_ID, Timestamp dateRun, String trxName)
	{
		super(ctx, 0, trxName);
		setAD_Replication_ID (AD_Replication_ID);
		setName (dateRun.toString());
		super.setIsReplicated (false);
	}	//	MReplicationRun

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MReplicationRun (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}

	/**
	 * 	Set Replication Flag
	 * 	@param IsReplicated replicated
	 */
	@Override
	public void setIsReplicated (boolean IsReplicated)
	{
		super.setIsReplicated(IsReplicated);
	}	//	setIsReplicated

}	//	MReplicationRun
