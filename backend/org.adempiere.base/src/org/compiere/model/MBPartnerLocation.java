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
import java.util.List;
import java.util.Properties;

import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Partner Location Model
 * 
 * @author Jorg Janke
 * @version $Id: MBPartnerLocation.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 * @author Teo Sarca, www.arhipac.ro <li>FR [ 2788465 ]
 *         MBPartnerLocation.getForBPartner method add trxName
 *         https://sourceforge.net/p/adempiere/feature-requests/715/
 */
public class MBPartnerLocation extends X_C_BPartner_Location {
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -8412652367051443276L;

	/**
	 * Get Locations for BPartner
	 * 
	 * @param ctx
	 *            context
	 * @param C_BPartner_ID
	 *            bp
	 * @return array of locations
	 * @deprecated Since 3.5.3a. Please use
	 *             {@link #getForBPartner(Properties, int, String)}.
	 */
	@Deprecated(forRemoval = true, since = "11")
	public static MBPartnerLocation[] getForBPartner(Properties ctx,
			int C_BPartner_ID) {
		return getForBPartner(ctx, C_BPartner_ID, null);
	}

	/**
	 * Get Locations for BPartner
	 * 
	 * @param ctx
	 *            context
	 * @param C_BPartner_ID
	 *            bp
	 * @param trxName
	 * @return array of locations
	 */
	public static MBPartnerLocation[] getForBPartner(Properties ctx,
			int C_BPartner_ID, String trxName) {
		List<MBPartnerLocation> list = new Query(ctx, Table_Name,
				"C_BPartner_ID=?", trxName).setParameters(C_BPartner_ID).list();
		MBPartnerLocation[] retValue = new MBPartnerLocation[list.size()];
		list.toArray(retValue);
		return retValue;
	} // getForBPartner

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_BPartner_Location_UU  UUID key
     * @param trxName Transaction
     */
    public MBPartnerLocation(Properties ctx, String C_BPartner_Location_UU, String trxName) {
        super(ctx, C_BPartner_Location_UU, trxName);
		if (Util.isEmpty(C_BPartner_Location_UU))
			setInitialDefaults();
    }

	/**
	 * Default Constructor
	 * 
	 * @param ctx
	 *            context
	 * @param C_BPartner_Location_ID
	 *            id
	 * @param trxName
	 *            transaction
	 */
	public MBPartnerLocation(Properties ctx, int C_BPartner_Location_ID,
			String trxName) {
		super(ctx, C_BPartner_Location_ID, trxName);
		if (C_BPartner_Location_ID == 0)
			setInitialDefaults();
	} // MBPartner_Location

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setName(".");
		//
		setIsShipTo(true);
		setIsRemitTo(true);
		setIsPayFrom(true);
		setIsBillTo(true);
	}

	/**
	 * BP Parent Constructor
	 * 
	 * @param bp
	 *            partner
	 */
	public MBPartnerLocation(MBPartner bp) {
		this(bp.getCtx(), 0, bp.get_TrxName());
		setClientOrg(bp);
		// may (still) be 0
		set_ValueNoCheck("C_BPartner_ID", Integer.valueOf(bp.getC_BPartner_ID()));
	} // MBPartner_Location

	/**
	 * Constructor from ResultSet row
	 * 
	 * @param ctx
	 *            context
	 * @param rs
	 *            current row of result set to be loaded
	 * @param trxName
	 *            transaction
	 */
	public MBPartnerLocation(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	} // MBPartner_Location

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MBPartnerLocation(MBPartnerLocation copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MBPartnerLocation(Properties ctx, MBPartnerLocation copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MBPartnerLocation(Properties ctx, MBPartnerLocation copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
		this.m_location = copy.m_location != null ? new MLocation(ctx, copy.m_location, trxName) : null;
		this.m_uniqueName = copy.m_uniqueName;
		this.m_unique = copy.m_unique;
	}

	/**
	 * @param ctx
	 * @param C_BPartner_Location_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MBPartnerLocation(Properties ctx, int C_BPartner_Location_ID, String trxName, String... virtualColumns) {
		super(ctx, C_BPartner_Location_ID, trxName, virtualColumns);
	}

	/** Cached Location */
	private MLocation m_location = null;
	/** Unique Name */
	private String m_uniqueName = null;
	private int m_unique = 0;

	/**
	 * Get Location/Address
	 * 
	 * @param requery get again the location from DB
	 * @return location
	 */
	public MLocation getLocation(boolean requery) {
		if (requery || m_location == null)
			m_location = MLocation.getCopy(getCtx(), getC_Location_ID(), get_TrxName());
		if (requery && m_location != null)
			m_location.load(get_TrxName());
		return m_location;
	} // getLocation

	/**
	 * String Representation
	 * 
	 * @return info
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("MBPartner_Location[ID=")
				.append(get_ID()).append(",C_Location_ID=")
				.append(getC_Location_ID()).append(",Name=").append(getName())
				.append("]");
		return sb.toString();
	} // toString

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getC_Location_ID() == 0)
			return false;

		// Set New Name
		if (".".equals(getName()) && !isPreserveCustomName()) {
			MLocation address = getLocation(true);
			setName(getBPLocName(address));
		}
		return true;
	} // beforeSave

	/**
	 * Make name Unique
	 * 
	 * @param address
	 *            address
	 */
	private void makeUnique(MLocation address) {
		m_uniqueName = "";

		// 0 - City
		if (m_unique >= 0 || m_uniqueName.length() == 0) {
			String xx = address.getCity();
			if (xx != null && xx.length() > 0)
				m_uniqueName = xx;
		}
		// 1 + Address1
		if (m_unique >= 1 || m_uniqueName.length() == 0) {
			String xx = address.getAddress1();
			if (xx != null && xx.length() > 0) {
				if (m_uniqueName.length() > 0)
					m_uniqueName += " ";
				m_uniqueName += xx;
			}
		}
		// 2 + Address2
		if (m_unique >= 2 || m_uniqueName.length() == 0) {
			String xx = address.getAddress2();
			if (xx != null && xx.length() > 0) {
				if (m_uniqueName.length() > 0)
					m_uniqueName += " ";
				m_uniqueName += xx;
			}
		}
		// 3 - Region
		if (m_unique >= 3 || m_uniqueName.length() == 0) {
			String xx = address.getRegionName(true);
			if (xx != null && xx.length() > 0) {
				if (m_uniqueName.length() > 0)
						m_uniqueName += " ";
				m_uniqueName += xx;
			}
		}
		// 4 - ID
		if (m_unique >= 4 || m_uniqueName.length() == 0) {
			int id = get_ID();
			if (id == 0)
				id = address.get_ID();
			m_uniqueName += "#" + id;
		}
	} // makeUnique

	/**
	 * Create unique BP location name
	 * @param address
	 * @return unique BP location name for address
	 */
	public String getBPLocName(MLocation address) {

		if (isPreserveCustomName())
			return getName();

		m_uniqueName = getName();
		m_unique = MSysConfig.getIntValue(MSysConfig.START_VALUE_BPLOCATION_NAME, 0,
				getAD_Client_ID(), getAD_Org_ID());
		if (m_unique < 0 || m_unique > 4)
			m_unique = 0;
		if (m_uniqueName != null) { 
			// default
			m_uniqueName = null;
			makeUnique(address);
		}

		// Check uniqueness
		MBPartnerLocation[] locations = getForBPartner(getCtx(),
				getC_BPartner_ID(), null);
		boolean unique = locations.length == 0;
		while (!unique) {
			unique = true;
			for (int i = 0; i < locations.length; i++) {
				MBPartnerLocation location = locations[i];
				if (location.getC_BPartner_Location_ID() == get_ID())
					continue;
				if (m_uniqueName.equals(location.getName())) {
					m_unique++;
					makeUnique(address);
					unique = false;
					break;
				}
			}
		}
		return m_uniqueName.toString();
	}

} // MBPartnerLocation
