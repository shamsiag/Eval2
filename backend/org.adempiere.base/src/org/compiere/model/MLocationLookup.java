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
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DisplayType;
import org.compiere.util.KeyNamePair;
import org.compiere.util.NamePair;

/**
 *	Address Location Lookup Model.
 *
 *  @author 	Jorg Janke
 *  @version 	$Id: MLocationLookup.java,v 1.3 2006/07/30 00:58:18 jjanke Exp $
 */
public final class MLocationLookup extends Lookup
	implements Serializable
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 7238110708451510319L;

	/**
	 *	Constructor
	 *  @param ctx context
	 *  @param WindowNo window no (to derive AD_Client/Org for new records)
	 */
	public MLocationLookup(Properties ctx, int WindowNo)
	{
		super (DisplayType.TableDir, WindowNo);
		m_ctx = ctx;
	}	//	MLocation

	/**	Context					*/
	private Properties 		m_ctx;

	/**
	 *	Get Display for Value (not cached)
	 *  @param key Location_ID
	 *  @return display text
	 */
	@Override
	public String getDisplay (Object key)
	{
		if (key == null)
			return null;
		MLocation loc = getLocation (key, null);
		if (loc == null){
			StringBuilder msgreturn = new StringBuilder("<").append(key.toString()).append(">");
			return msgreturn.toString();
		}	
		return loc.toString();
	}	//	getDisplay

	/**
	 *	Get NamePair of Key Value
	 *  @param key value
	 *  @return NamePair or null
	 */
	@Override
	public NamePair get (Object key)
	{
		if (key == null)
			return null;
		MLocation loc = getLocation (key, null);
		if (loc == null)
			return null;
		return new KeyNamePair (loc.getC_Location_ID(), loc.toString());
	}	//	get

	/**
	 *  The Lookup contains the key 
	 *  @param key Location_ID
	 *  @return true if key exists
	 */
	@Override
	public boolean containsKey (Object key)
	{
		return getLocation(key, null) != null;
	}   //  containsKey

	/**
	 * Same as {@link #containsKey(Object)} in this class
	 */
	@Override
	public boolean containsKeyNoDirect (Object key)
	{
		return containsKey(key);
	}
	
	/**
	 * 	Get Location
	 * 	@param key ID as string or integer
	 *	@param trxName transaction
	 * 	@return Location or null
	 */
	public MLocation getLocation (Object key, String trxName)
	{
		if (key == null)
			return null;
		int C_Location_ID = 0;
		if (key instanceof Integer)
			C_Location_ID = ((Integer)key).intValue();
		else if (key != null)
			C_Location_ID = Integer.parseInt(key.toString());
		//
		return getLocation(C_Location_ID, trxName);
	}	//	getLocation
	
	/**
	 * 	Get Location
	 * 	@param C_Location_ID id
	 *	@param trxName transaction
	 * 	@return Location
	 */
	public MLocation getLocation (int C_Location_ID, String trxName)
	{
		return MLocation.getCopy(m_ctx, C_Location_ID, trxName);
	}	//	getC_Location_ID

	/**
	 *	Get underlying fully qualified Table.Column Name.
	 *  @return "C_Location.C_Location_ID"
	 */
	@Override
	public String getColumnName()
	{
		return "C_Location.C_Location_ID";
	}   //  getColumnName

	/**
	 *	Get Zoom - Location Window
	 *  @return Zoom AD_Window_ID
	 */
	@Override
	public int getZoom() {
		return MTable.get(MLocation.Table_ID).getAD_Window_ID();
	}
	
	/**
	 *	Return data as sorted Array - not implemented
	 *  @param mandatory mandatory
	 *  @param onlyValidated only validated
	 *  @param onlyActive only active
	 * 	@param temporary force load for temporary display
	 *  @return null
	 */
	@Override
	public ArrayList<Object> getData (boolean mandatory, boolean onlyValidated, boolean onlyActive, boolean temporary, boolean shortlist) // IDEMPIERE 90
	{
		log.log(Level.SEVERE, "not implemented");
		return null;
	}   //  getArray

}	//	MLocation
