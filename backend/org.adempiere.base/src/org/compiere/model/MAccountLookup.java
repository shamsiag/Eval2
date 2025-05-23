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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.NamePair;

/**
 *	Account Model Lookup - Maintains ValidCombination Info for Display and Edit - not cached
 *
 *  @author 	Jorg Janke
 *  @author     victor.perez@e-evolution.com, www.e-evolution.com
 *    			<li>RF [ 2214883 ] Remove SQL code and Replace for Query https://sourceforge.net/p/adempiere/feature-requests/557/
 *  @version 	$Id: MAccountLookup.java,v 1.3 2006/07/30 00:54:54 jjanke Exp $
 */
public final class MAccountLookup extends Lookup implements Serializable
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -6307559127917670193L;

	/**
	 *	Constructor
	 *  @param ctx context
	 *  @param WindowNo window no
	 */
	public MAccountLookup (Properties ctx, int WindowNo)
	{
		super (DisplayType.TableDir, WindowNo);
		m_ctx = ctx;
	}	//	MAccountLookup

	/**	Context				*/
	private Properties  m_ctx;
	/** Account_ID			*/
	public int 		    C_ValidCombination_ID;
	private String		Combination;
	private String		Description;

	/**
	 *	Get Display Text for Value
	 *  @param value value
	 *  @return Display text
	 */
	@Override
	public String getDisplay (Object value)
	{
		if (!containsKey (value)){
			StringBuilder msgreturn = new StringBuilder("<").append(value.toString()).append(">");
			return msgreturn.toString();
		}
		return toString();
	}	//	getDisplay

	/**
	 *	Get KeyNamePair of Key Value
	 *  @param value key value
	 *  @return KeyNamePair or null
	 */
	@Override
	public NamePair get (Object value)
	{
		if (value == null)
			return null;
		if (!containsKey (value))
			return null;
		return new KeyNamePair (C_ValidCombination_ID, toString());
	}	//	get

	/**
	 *  The Lookup contains the key
	 *  @param key key
	 *  @return true if exists
	 */
	@Override
	public boolean containsKey (Object key)
	{
		int intValue = 0;
		if (key instanceof Integer)
			intValue = ((Integer)key).intValue();
		else if (key != null)
			intValue = Integer.parseInt(key.toString());
		//
		return load (intValue);
	}   //  containsKey

	/**
	 * Delegate to {@link #containsKey(Object)}
	 */
	@Override
	public boolean containsKeyNoDirect (Object key)
	{
		return containsKey(key);
	}

	/**
	 *  Get Description
	 *  @return Description
	 */
	public String getDescription()
	{
		return Description;
	}   //  getDescription

	/**
	 *	Return String representation
	 *  @return Combination
	 */
	public String toString()
	{
		if (C_ValidCombination_ID == 0)
			return "";
		return Combination;
	}	//	toString

	/**
	 *	Load C_ValidCombination with ID
	 *  @param ID C_ValidCombination_ID
	 *  @return true if found
	 */
	public boolean load (int ID)
	{
		if (ID == 0)						//	new
		{
			C_ValidCombination_ID = 0;
			Combination = "";
			Description = "";
			return true;
		}
		if (ID == C_ValidCombination_ID)	//	already loaded
			return true;
		
		MAccount account = MAccount.get(Env.getCtx(), ID);
		
		if(account == null || account.getC_ValidCombination_ID() != ID)
			return false;
		
		C_ValidCombination_ID = account.getC_ValidCombination_ID();
		Combination = account.getCombination();
		Description = account.getDescription();
		return true;
	}	//	load

	/**
	 *	Get underlying fully qualified Table.Column Name
	 *  @return ""
	 */
	@Override
	public String getColumnName()
	{
		return "";
	}   //  getColumnName

	/**
	 *	Return data as sorted Array.
	 *  @param mandatory mandatory
	 *  @param onlyValidated only valid
	 *  @param onlyActive only active
	 * 	@param temporary force load for temporary display
	 *  @param isshortlist ignore
	 *  @return ArrayList with KeyNamePair
	 */
	@Override
	public ArrayList<Object> getData (boolean mandatory, boolean onlyValidated, 
		boolean onlyActive, boolean temporary, boolean isshortlist) // IDEMPIERE 90
	{
		ArrayList<Object> list = new ArrayList<Object>();
		if (!mandatory)
			list.add(new KeyNamePair (-1, ""));
		//
		ArrayList<Object> params = new ArrayList<Object>();
		final String whereClause = "AD_Client_ID=?";
		params.add(Env.getAD_Client_ID(m_ctx));
		
		List<MAccount> accounts = new Query(Env.getCtx(),I_C_ValidCombination.Table_Name,whereClause,null)
		.setParameters(params)
		.setOrderBy(I_C_ValidCombination.COLUMNNAME_Combination)
		.setOnlyActiveRecords(onlyActive)
		.list();
		
		for(MAccount account :accounts)
		{
			StringBuilder msglist = new StringBuilder().append(account.getCombination()).append(" - ") 
												.append(account.getDescription());
			list.add (new KeyNamePair(account.getC_ValidCombination_ID(), msglist.toString()));
		}
		//  Sort & return
		return list;
	}   //  getData

}	//	MAccountLookup
