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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.NamePair;
import org.compiere.util.Util;

/**
 *	Warehouse Locator Lookup Model.
 *
 *  @author 	Jorg Janke
 *  @version 	$Id: MLocatorLookup.java,v 1.3 2006/07/30 00:58:04 jjanke Exp $
 * 
 *  @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 				<li>BF [ 1892920 ] Locators fieldshould be ordered by Warehouse/Value
 *              <li>FR [ 2306161 ] Removed limit of 200 on max number of locators.
 */
public final class MLocatorLookup extends Lookup implements Serializable
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -6041455174391573888L;

	/**
	 *	Constructor
	 *  @param ctx context
	 *  @param WindowNo window no
	 */
	public MLocatorLookup(Properties ctx, int WindowNo)
	{
		this(ctx, WindowNo, null);
	}
	
	/**
	 *	Constructor
	 *  @param ctx context
	 *  @param WindowNo window no
	 *  @param validationCode Lookup validation code
	 */
	public MLocatorLookup(Properties ctx, int WindowNo, String validationCode)
	{
		super (DisplayType.TableDir, WindowNo);
		m_ctx = ctx;
		m_validationCode = validationCode;
	}	//	MLocator

	/**	Context						*/
	private Properties          m_ctx;
	protected int				C_Locator_ID;
	private Loader				m_loader;
	private String				m_validationCode;
	private String				m_parsedValidation;
	private int 				m_warehouseActiveCount;

	/**	Only Warehouse					*/
	private int					m_only_Warehouse_ID = 0;
	/**	Only Product					*/
	private int					m_only_Product_ID = 0;

	/** Storage of data  MLookups		*/
	private volatile LinkedHashMap<Integer,MLocator> m_lookup = new LinkedHashMap<Integer,MLocator>();

	/**
	 *  Dispose
	 */
	public void dispose()
	{
		if (log.isLoggable(Level.FINE)) log.fine("C_Locator_ID=" + C_Locator_ID);
		if (m_loader != null)
		{
			while (m_loader.isAlive())
				m_loader.interrupt();
		}
		m_loader = null;
		if (m_lookup != null)
			m_lookup.clear();
		m_lookup = null;
		//
		super.dispose();
	}   //  dispose

	/**
	 * 	Set Warehouse restriction
	 *	@param only_Warehouse_ID warehouse
	 */
	public void setOnly_Warehouse_ID (int only_Warehouse_ID)
	{
		m_only_Warehouse_ID = only_Warehouse_ID;
	}	//	setOnly_Warehouse_ID

	/**
	 * 	Get Only Wahrehouse
	 *	@return	M_Warehouse_ID
	 */
	public int getOnly_Warehouse_ID()
	{
		return m_only_Warehouse_ID;
	}	//	getOnly_Warehouse_ID

	/**
	 * 	Set Product restriction
	 *	@param only_Product_ID Product
	 */
	public void setOnly_Product_ID (int only_Product_ID)
	{
		m_only_Product_ID = only_Product_ID;
	}	//	setOnly_Product_ID

	/**
	 * 	Get Only Product
	 *	@return	M_Product_ID
	 */
	public int getOnly_Product_ID()
	{
		return m_only_Product_ID;
	}	//	getOnly_Product_ID

	/**
	 *  Wait until async Load Complete
	 */
	@Override
	public void loadComplete()
	{
		if (m_loader != null)
		{
			try
			{
				m_loader.join();
			}
			catch (InterruptedException ie)
			{
				log.log(Level.SEVERE, "Join interrupted", ie);
			}
		}
	}   //  loadComplete

	/**
	 *	Get KeyNamePair via key value.
	 *  Delegate to {@link #getDirect(Object, boolean, String)} if key not in local lookup cache({@link #m_lookup}).
	 *  @param key key value
	 *  @return KeyNamePair
	 */
	@Override
	public NamePair get (Object key)
	{
		if (key == null)
			return null;

		//	try cache
		MLocator loc = (MLocator) m_lookup.get(key);
		if (loc != null)
			return new KeyNamePair (loc.getM_Locator_ID(), loc.toString());

		//	Not found and waiting for loader
		if (m_loader != null && m_loader.isAlive())
		{
			log.fine("Waiting for Loader");
			loadComplete();
			//	is most current
			loc = (MLocator) m_lookup.get(key);
		}
		if (loc != null)
			return new KeyNamePair (loc.getM_Locator_ID(), loc.toString());

		//	Try to get it directly
		return getDirect(key, true, null);
	}	//	get

	/**
	 *	Get Display text
	 *  @param key key value
	 *  @return display text
	 */
	@Override
	public String getDisplay (Object key)
	{
		if (key == null)
			return "";
		//
		NamePair display = get (key);
		if (display == null){
			StringBuilder msgreturn = new StringBuilder("<").append(key.toString()).append(">");
			return msgreturn.toString();
		}
		return display.toString();
	}	//	getDisplay

	/**
	 *  The Lookup contains the key
	 *  @param key key
	 *  @return true, if lookup contains key
	 */
	@Override
	public boolean containsKey (Object key)
	{
		return m_lookup.containsKey(key);
	}   //  containsKey

	/**
	 * Same as {@link #containsKey(Object)} in this lookup implementation.
	 */
	@Override
	public boolean containsKeyNoDirect (Object key)
	{
		return containsKey(key);
	}

	/**
	 *	Get from MLocator cache or DB
	 *  @param keyValue integer key value
	 *  @param saveInCache true save in lookup cache
	 *  @param trxName transaction
	 *  @return KeyNamePair
	 */
	public NamePair getDirect (Object keyValue, boolean saveInCache, String trxName)
	{
		MLocator loc = getMLocator (keyValue, trxName);
		if (loc == null)
			return null;
		//
		int key = loc.getM_Locator_ID();
		if (saveInCache)
			m_lookup.put(Integer.valueOf(key), loc);
		NamePair retValue = new KeyNamePair(key, loc.toString());
		return retValue;
	}	//	getDirect

	/**
	 *	Get from MLocator cache or DB
	 *  @param keyValue integer key value
	 *  @param trxName transaction
	 *  @return MLocator
	 */
	public MLocator getMLocator (Object keyValue, String trxName)
	{
		int M_Locator_ID = -1;
		try
		{
			M_Locator_ID = Integer.parseInt(keyValue.toString());
		}
		catch (Exception e)
		{}
		if (M_Locator_ID == -1)
		{
			log.log(Level.SEVERE, "Invalid key=" + keyValue);
			return null;
		}
		//
		return MLocator.getCopy(m_ctx, M_Locator_ID, trxName);
	}	//	getMLocator

	/**
	 * @return  a string representation of the object.
	 */
	@Override
	public String toString()
	{
		StringBuilder msgreturn = new StringBuilder("MLocatorLookup[Size=").append(m_lookup.size()).append("]");
		return msgreturn.toString();
	}	//	toString


	/**
	 * 	Is Locator with key valid (Warehouse)
	 *	@param key key
	 *	@return true if valid
	 */
	public boolean isValid (Object key)
	{
		if (key == null)
			return true;
		//	try cache
		MLocator loc = (MLocator) m_lookup.get(key);
		if (loc == null)
			loc = getMLocator(key, null);
		return isValid(loc);
	}	//	isValid

	/**
	 * 	Is Locator with key valid (Warehouse)
	 *	@param locator locator
	 *	@return true if valid
	 */
	public boolean isValid (MLocator locator)
	{
		if (locator == null || getOnly_Warehouse_ID() == 0)
			return true;
		//	Warehouse OK - Product check
		if (getOnly_Warehouse_ID() == locator.getM_Warehouse_ID())
			return locator.isCanStoreProduct(getOnly_Product_ID());
		return false;
	}	//	isValid

	/**
	 * @return true if local lookup cache need refresh
	 */
	private boolean isNeedRefresh()
	{
		if (!Util.isEmpty(m_validationCode))
		{
			Properties ctx = new Properties(m_ctx);
			Env.setContext(ctx, getWindowNo(), "M_Product_ID", getOnly_Product_ID());
			Env.setContext(ctx, getWindowNo(), "M_Warehouse_ID", getOnly_Warehouse_ID());
			String parseValidation = Env.parseContext(ctx, getWindowNo(), m_validationCode, false, false);
			if ((!Util.isEmpty(parseValidation) && !parseValidation.equals(m_parsedValidation)) || (!Util.isEmpty(m_parsedValidation) && !m_parsedValidation.equals(parseValidation)))
				return true;
		}
		else if (!Util.isEmpty(m_parsedValidation))
		{
			m_parsedValidation = null;
			return true;
		}
		
		if (m_only_Warehouse_ID==0)
		{
			int activeCount = DB.getSQLValue(null, "SELECT Count(*) FROM M_Warehouse WHERE IsActive='Y' AND AD_Client_ID=?", Env.getAD_Client_ID(m_ctx));
			if (m_warehouseActiveCount != activeCount)
			{
				m_warehouseActiveCount = activeCount;
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 *	Data Loader
	 */
	class Loader extends Thread implements Serializable
	{
		/**
		 * generated serial id
		 */
		private static final long serialVersionUID = 3472186635409000236L;

		/**
		 * 	Loader
		 */
		public Loader()
		{
			super("MLocatorLookup");
		}	//	Loader

		/**
		 *	Load Lookup Data
		 */
		public void run()
		{
			//	Set Info	- see VLocator.actionText
			int local_only_warehouse_id = getOnly_Warehouse_ID(); // [ 1674891 ] MLocatorLookup - weird error 
			int local_only_product_id = getOnly_Product_ID();
			
			StringBuilder sql = new StringBuilder("SELECT M_Locator.* FROM M_Locator ")
				.append(" INNER JOIN M_Warehouse wh ON (wh.M_Warehouse_ID=M_Locator.M_Warehouse_ID) ")
				.append(" WHERE M_Locator.IsActive='Y' ")
				.append(" AND wh.IsActive='Y'");
			
			if (local_only_warehouse_id != 0)
				sql.append(" AND M_Locator.M_Warehouse_ID=?");
			else
				m_warehouseActiveCount = DB.getSQLValue(null, "SELECT Count(*) FROM M_Warehouse WHERE IsActive='Y' AND AD_Client_ID=?", Env.getAD_Client_ID(m_ctx));
			if (local_only_product_id != 0)
				sql.append(" AND (M_Locator.IsDefault='Y' ")	//	Default Locator
					.append("OR EXISTS (SELECT * FROM M_Product p ")	//	Product Locator
					.append("WHERE p.M_Locator_ID=M_Locator.M_Locator_ID AND p.M_Product_ID=?) ")
					.append("OR EXISTS (SELECT * FROM M_Storage s ")	//	Storage Locator
					.append("WHERE s.M_Locator_ID=M_Locator.M_Locator_ID AND s.M_Product_ID=?))");
			m_parsedValidation = null;
			if (!Util.isEmpty(m_validationCode))
			{
				Properties ctx = new Properties(m_ctx);
				Env.setContext(ctx, getWindowNo(), "M_Product_ID", getOnly_Product_ID());
				Env.setContext(ctx, getWindowNo(), "M_Warehouse_ID", getOnly_Warehouse_ID());
				String parseValidation = Env.parseContext(ctx, getWindowNo(), m_validationCode, false, false);				
				m_parsedValidation = parseValidation;
				if (!Util.isEmpty(parseValidation))
				{
					sql.append(" AND ( ").append(parseValidation).append(" ) ");
				}
			}
			sql.append(" ORDER BY ");
			if (local_only_warehouse_id == 0)
				sql.append("wh.Name,");
			sql.append("M_Locator.Value");
			String finalSql = MRole.getDefault(m_ctx, false).addAccessSQL(
				sql.toString(), "M_Locator", MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);
			if (isInterrupted())
			{
				log.log(Level.SEVERE, "Interrupted");
				return;
			}

			//	Reset
			m_lookup.clear();
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(finalSql, null);
				int index = 1;
				if (local_only_warehouse_id != 0)
					pstmt.setInt(index++, getOnly_Warehouse_ID());
				if (local_only_product_id != 0)
				{
					pstmt.setInt(index++, getOnly_Product_ID());
					pstmt.setInt(index++, getOnly_Product_ID());
				}
				rs = pstmt.executeQuery();
				//
				while (rs.next())
				{
					MLocator loc = new MLocator(Env.getCtx(), rs, null);
					int M_Locator_ID = loc.getM_Locator_ID();
					m_lookup.put(Integer.valueOf(M_Locator_ID), loc);
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, finalSql, e);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
			}
			if (log.isLoggable(Level.FINE)) log.fine("Complete #" + m_lookup.size());
			if (m_lookup.size() == 0)
				log.finer(finalSql);
		}	//	run
	}	//	Loader

	/**
	 *	Return data as Locator ArrayList, waits for loader to finish if loading is in progress.
	 *  @return Collection of MLocator
	 */
	public Collection<MLocator> getData ()
	{
		if (m_loader == null)
		{
			refresh();
		}
		else if (m_loader.isAlive())
		{
			if (log.isLoggable(Level.FINE)) log.fine("Waiting for Loader");
			try
			{
				m_loader.join();
			}
			catch (InterruptedException ie)
			{
				log.severe ("Join interrupted - " + ie.getMessage());
			}
		}
		return m_lookup.values();
	}	//	getData

	/**
	 *	Get lookup data
	 *  @param mandatory mandatory
	 *  @param onlyValidated only validated
	 *  @param onlyActive only active
	 * 	@param temporary force load for temporary display
	 *  @return ArrayList of lookup values
	 */
	@Override
	public ArrayList<Object> getData (boolean mandatory, boolean onlyValidated, boolean onlyActive, boolean temporary, boolean shortlist) // IDEMPIERE 90
	{
		//	create list
		Collection<MLocator> collection = getData();
		ArrayList<Object> list = new ArrayList<Object>(collection.size());
		Iterator<MLocator> it = collection.iterator();
		while (it.hasNext())
		{
			MLocator loc = it.next();
			if (isValid(loc))				//	only valid warehouses
				list.add(loc);
		}

		return list;
	}	//	getArray

	/**
	 *	Refresh local lookup cache
	 *  @return new size of lookup
	 */
	@Override
	public int refresh()
	{
		if (log.isLoggable(Level.FINE)) log.fine("start");
		m_loader = new Loader();
		m_loader.start();
		try
		{
			m_loader.join();
		}
		catch (InterruptedException ie)
		{
		}
		if (log.isLoggable(Level.INFO)) log.info("#" + m_lookup.size());
		return m_lookup.size();
	}	//	refresh

	/**
	 * Call {@link #refresh()} if {@link #isNeedRefresh()} return true.
	 * @return lookup size
	 */
	public int refreshIfNeeded()
	{
		if (m_loader != null && m_loader.isAlive())
			return m_lookup.size();
		else if (isNeedRefresh())
			return refresh();
		else
			return m_lookup.size();
	}
	
	/**
	 *	Get underlying fully qualified Table.Column Name
	 *  @return Table.ColumnName
	 */
	public String getColumnName()
	{
		return "M_Locator.M_Locator_ID";
	}	//	getColumnName

	public void dynamicDisplay(Properties ctx) 
	{
		m_ctx = ctx;
		m_parsedValidation = null;		
	}
	
	/**
	 * Set SQL validation code for lookup
	 * @param validationCode
	 */
	public void setValidationCode(String validationCode) 
	{
		m_validationCode = validationCode;
	}
}	//	MLocatorLookup
