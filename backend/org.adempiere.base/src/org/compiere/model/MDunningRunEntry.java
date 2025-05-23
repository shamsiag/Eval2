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
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.BPartnerNoAddressException;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 *	Dunning Run Entry Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MDunningRunEntry.java,v 1.2 2006/07/30 00:51:05 jjanke Exp $
 *  
 *  @author Teo Sarca - BF [ 1739022 ], BF [ 1739096 ]
 *  
 *  FR 2872010 - Dunning Run for a complete Dunning (not just level) - Developer: Carlos Ruiz - globalqss - Sponsor: Metas
 */
public class MDunningRunEntry extends X_C_DunningRunEntry
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -3838792682143065656L;

	/** Logger								*/
	private static CLogger		s_log = CLogger.getCLogger (MPayment.class);

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_DunningRunEntry_UU  UUID key
     * @param trxName Transaction
     */
    public MDunningRunEntry(Properties ctx, String C_DunningRunEntry_UU, String trxName) {
        super(ctx, C_DunningRunEntry_UU, trxName);
		if (Util.isEmpty(C_DunningRunEntry_UU))
			setInitialDefaults();
    }

	/**
	 * Standard Constructor
	 * @param ctx context
	 * @param C_DunningRunEntry_ID id
	 * @param trxName transaction
	 */
	public MDunningRunEntry (Properties ctx, int C_DunningRunEntry_ID, String trxName)
	{
		super (ctx, C_DunningRunEntry_ID, trxName);
		if (C_DunningRunEntry_ID == 0)
			setInitialDefaults();
	}	//	MDunningRunEntry

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setAmt (Env.ZERO);
		setQty (Env.ZERO);
		setProcessed (false);
	}

	/**
	 * Load Constructor
	 * @param ctx context
	 * @param rs result set
	 * @param trxName transaction
	 */
	public MDunningRunEntry (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MDunningRunEntry
	
	/**
	 * Parent Constructor
	 * @param parent parent 
	 */
	public MDunningRunEntry (MDunningRun parent)
	{
		this (parent.getCtx(), 0, parent.get_TrxName());
		setClientOrg(parent);
		setC_DunningRun_ID(parent.getC_DunningRun_ID());
		m_parent = parent;
	}	//	MDunningRunEntry

	/** Parent				*/
	@SuppressWarnings("unused")
	private MDunningRun		m_parent = null;
	
	/**
	 * Set BPartner
	 * @param bp partner
	 * @param isSOTrx
	 */
	public void setBPartner (MBPartner bp, boolean isSOTrx)
	{
		setC_BPartner_ID(bp.getC_BPartner_ID());
		MBPartnerLocation[] locations = bp.getLocations(false);
		//	Location
		if (locations.length == 1)
			setC_BPartner_Location_ID (locations[0].getC_BPartner_Location_ID());
		else
		{
			MBPartnerLocation firstActive = null;
			MBPartnerLocation firstBillTo = null;
			for (int i = 0; i < locations.length; i++)
			{
				MBPartnerLocation location = locations[i];
				if (!location.isActive())
				{
					continue;
				}
				else if (firstActive == null)
				{
					firstActive = location;
				}
				if ((location.isPayFrom() && isSOTrx)
					|| (location.isRemitTo() && !isSOTrx))
				{
					setC_BPartner_Location_ID (location.getC_BPartner_Location_ID());
					break;
				}
				else if (firstBillTo == null && location.isBillTo())
				{
					firstBillTo = location;
				}
			}

			// If IsPayFrom or isRemitTo is not found among the BP Locations, check IsBillTo
			// if that isn't available, we should use any active location
			if(getC_BPartner_Location_ID() == 0)
			{
				if (firstBillTo != null)
				{
					setC_BPartner_Location_ID (firstBillTo.getC_BPartner_Location_ID());
				}
				else if (firstActive != null)
				{
					StringBuilder msg = new StringBuilder("@C_BPartner_ID@ ").append(bp.getName());
					if (isSOTrx)
						msg.append(" @No@ @IsPayFrom@");
					else
						msg.append(" @No@ @IsRemitTo@");
					msg.append(" & @IsBillTo@");
					if (log.isLoggable(Level.INFO)) log.info(msg.toString());
					setC_BPartner_Location_ID (firstActive.getC_BPartner_Location_ID());
				}
			}
		}
		if (getC_BPartner_Location_ID() == 0)
		{
			throw new BPartnerNoAddressException(bp);
		}
		//	User with location
		MUser[] users = MUser.getOfBPartner(getCtx(), bp.getC_BPartner_ID(), get_TrxName());
		if (users.length == 1)
			setAD_User_ID (users[0].getAD_User_ID());
		else
		{
			for (int i = 0; i < users.length; i++)
			{
				MUser user = users[i];
				if (user.getC_BPartner_Location_ID() == getC_BPartner_Location_ID())
				{
					setAD_User_ID (users[i].getAD_User_ID());
					break;
				}
			}
		}
		//
		int SalesRep_ID = bp.getSalesRep_ID();
		if (SalesRep_ID != 0)
			setSalesRep_ID (SalesRep_ID);
	}	//	setBPartner
	
	/**
	 * Get Lines
	 * @return Array of all lines for this Run
	 */
	public MDunningRunLine[] getLines() 
	{
		return getLines(false); 
	}	//	getLines

	/**
	 * 	Get Lines
	 *  @param onlyInvoices true for entries with invoice only 
	 *	@return Array of all lines for this Run
	 */
	public MDunningRunLine[] getLines (boolean onlyInvoices) 
	{
		ArrayList<MDunningRunLine> list = new ArrayList<MDunningRunLine>();
		StringBuilder sql = new StringBuilder("SELECT * FROM C_DunningRunLine WHERE C_DunningRunEntry_ID=?");
		if (onlyInvoices)
			sql.append(" AND C_Invoice_ID IS NOT NULL");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setInt(1, get_ID ());
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add(new MDunningRunLine(getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			s_log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//
		MDunningRunLine[] retValue = new MDunningRunLine[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getLines

	/**
	 * 	Check whether has Invoices
	 *	@return true if C_Invoice_ID > 0
	 */
	public boolean hasInvoices() 
	{
		boolean retValue = false;
		String sql = "SELECT COUNT(*) FROM C_DunningRunLine WHERE C_DunningRunEntry_ID=? AND C_Invoice_ID IS NOT NULL";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, get_ID ());
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				if (rs.getInt(1) > 0) 
					retValue = true;
			}
		}
		catch (Exception e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return retValue;
	}	//	hasInvoices

	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		I_C_DunningLevel level = getC_DunningLevel();
		if (isProcessed ())
		{
			// Propagate Processed to Lines
			MDunningRunLine[] theseLines = getLines();
			for (int i=0;i<theseLines.length;i++) 
			{
				theseLines[i].setProcessed (true);
				theseLines[i].saveEx(get_TrxName());
			}
			// Update CreditStop and PaymentTerm of business partner.
			if (level.isSetCreditStop () || level.isSetPaymentTerm ()) 
			{
				MBPartner thisBPartner = MBPartner.get (getCtx(), getC_BPartner_ID());
				if (level.isSetCreditStop ())
					thisBPartner.setSOCreditStatus (X_C_BPartner.SOCREDITSTATUS_CreditStop);
				if (level.isSetPaymentTerm ())
					thisBPartner.setC_PaymentTerm_ID (level.getC_PaymentTerm_ID ());
				thisBPartner.saveEx();
			}
		}
		return true;
	}	//	beforeSave
	
}	//	MDunningRunEntry
