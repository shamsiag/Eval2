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
import java.util.logging.Level;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

import org.compiere.util.CLogger;
import org.compiere.util.KeyNamePair;
import org.compiere.util.NamePair;
import org.compiere.util.ValueNamePair;

/**
 *	Base Class for MLookup, MLocator, MLocation and MAccount (only single value).<br/>
 *  Maintains selectable data as NamePairs in ArrayList.<br/>
 *  The objects itself may be shared by the lookup implementation (usually HashMap).
 *
 *  @author 	Jorg Janke
 *  @version 	$Id: Lookup.java,v 1.3 2006/07/30 00:58:18 jjanke Exp $
 */
public abstract class Lookup extends AbstractListModel<Object>
	implements MutableComboBoxModel<Object>, Serializable
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -2500275921218601088L;

	/**
	 *  Lookup
	 * 	@param displayType display type
	 * 	@param windowNo window no
	 */
	public Lookup (int displayType, int windowNo)
	{
		m_displayType = displayType;
		m_WindowNo = windowNo;
	}   //  Lookup

	/** The Data List           */
	protected volatile ArrayList<Object>   p_data = new ArrayList<Object>();

	/** The Selected Item       */
	private volatile Object         m_selectedObject;

	/** Temporary Data          */
	private Object[]                m_tempData = null;

	/**	Logger					*/
	protected CLogger				log = CLogger.getCLogger(getClass());

	/**	Display Type			*/
	private int						m_displayType;
	/**	Window No				*/
	private int						m_WindowNo;
	
	private boolean 				m_mandatory;
	
	private boolean					m_loaded;

	private boolean					m_shortList;	// IDEMPIERE 90

	/**
	 * 	Get Display Type
	 *	@return display type
	 */
	public int getDisplayType()
	{
		return m_displayType;
	}	//	getDisplayType

	/**
	 * 	Get Window No
	 *	@return Window No
	 */
	public int getWindowNo()
	{
		return m_WindowNo;
	}	//	getWindowNo
	
	/**
	 * Set selected item by value. The selected item may be null.
	 * <p>
	 * @param anObject Value of selected item or null for no selection.
	 */
	@Override
	public void setSelectedItem(Object anObject)
	{
		if ((m_selectedObject != null && !m_selectedObject.equals( anObject ))
			|| m_selectedObject == null && anObject != null)
		{
			if (p_data.contains(anObject) || anObject == null)
			{
				m_selectedObject = anObject;
			}
			else
			{
				m_selectedObject = null;
				if (log.isLoggable(Level.FINE)) log.fine(getColumnName() + ": setSelectedItem - Set to NULL");
			}
			fireContentsChanged(this, -1, -1);
		}
	}   //  setSelectedItem

	/**
	 *  Return selected Item
	 *  @return value of selected item
	 */
	public Object getSelectedItem()
	{
		return m_selectedObject;
	}   //  getSelectedItem

	/**
	 *  Get Size of Model
	 *  @return size
	 */
	public int getSize()
	{
		return p_data.size();
	}   //  getSize

	/**
	 *  Get Element at Index
	 *  @param index index
	 *  @return value at index
	 */
	public Object getElementAt (int index)
	{
		return p_data.get(index);
	}   //  getElementAt

	/**
	 * Returns the index-position of the specified object in the list.
	 *
	 * @param anObject object
	 * @return index position, where 0 is the first position
	 */
	public int getIndexOf (Object anObject)
	{
		return p_data.indexOf(anObject);
	}   //  getIndexOf

	/**
	 *  Add Element at the end
	 *  @param anObject object
	 */
	public void addElement (Object anObject)
	{
		p_data.add(anObject);
		fireIntervalAdded (this, p_data.size()-1, p_data.size()-1);
		if (p_data.size() == 1 && m_selectedObject == null && anObject != null)
			setSelectedItem (anObject);
	}   //  addElement

	/**
	 *  Insert Element At
	 *  @param anObject object
	 *  @param index index
	 */
	public void insertElementAt (Object anObject, int index)
	{
		p_data.add (index, anObject);
		fireIntervalAdded (this, index, index);
	}   //  insertElementAt

	/**
	 *  Remove Item at index
	 *  @param index index
	 */
	public void removeElementAt (int index)
	{
		if (getElementAt(index) == m_selectedObject)
		{
			if (index == 0)
				setSelectedItem (getSize() == 1 ? null : getElementAt( index + 1 ));
			else
				setSelectedItem (getElementAt (index - 1));
		}
		p_data.remove(index);
		fireIntervalRemoved (this, index, index);
	}   //  removeElementAt

	/**
	 *  Remove Item
	 *  @param anObject object
	 */
	public void removeElement (Object anObject)
	{
		int index = p_data.indexOf (anObject);
		if (index != -1)
			removeElementAt(index);
	}   //  removeItem

	/**
	 *  Empties the list.
	 */
	public void removeAllElements()
	{
		if (p_data.size() > 0)
		{
			int firstIndex = 0;
			int lastIndex = p_data.size() - 1;
			p_data.clear();
			m_selectedObject = null;
			fireIntervalRemoved (this, firstIndex, lastIndex);
		}
		m_loaded = false;
	}   //  removeAllElements

	/**
	 *	Append {@link ValueNamePair} to list
	 *  @param key key
	 *  @param value value
	 */
	public void put (String key, String value)
	{
		NamePair pp = new ValueNamePair (key, value);
		addElement(pp);
	}	//	put

	/**
	 *	Append {@link KeyNamePair} to list
	 *  @param key key
	 *  @param value value
	 */
	public void put (int key, String value)
	{
		NamePair pp = new KeyNamePair (key, value);
		addElement(pp);
	}	//	put

	/**
	 *  Fill ComboBox with lookup data (asynchronous using Worker).<br/>
	 *  - try to maintain selected item
	 *  @param mandatory  has mandatory data only (i.e. no "null" selection)
	 *  @param onlyValidated only validated
	 *  @param onlyActive onlt active
	 *  @param temporary  save current values - restore via fillComboBox (true)
	 *  @param shortList
	 */
	public void fillComboBox (boolean mandatory, boolean onlyValidated, 
		boolean onlyActive, boolean temporary, boolean shortList) // IDEMPIERE 90
	{
		long startTime = System.currentTimeMillis();
		m_loaded = false;
		//  Save current data
		if (temporary)
		{
			int size = p_data.size();
			m_tempData = new Object[size];
			//  We need to do a deep copy, so store it in Array
			p_data.toArray(m_tempData);
		}

		Object obj = m_selectedObject;
		p_data.clear();

		//  may cause delay *** The Actual Work ***
		p_data = getData (mandatory, onlyValidated, onlyActive, temporary, shortList); // IDEMPIERE 90
		
		//  Selected Object changed
		if (obj != m_selectedObject)
		{
			if (log.isLoggable(Level.FINEST)) log.finest(getColumnName() + ": SelectedValue Changed=" + obj + "->" + m_selectedObject);
			obj = m_selectedObject;
		}

		m_loaded = true; 
		fireContentsChanged(this, 0, p_data.size());
		if (p_data.size() == 0) {
			if (log.isLoggable(Level.FINE)) log.fine(getColumnName() + ": #0 - ms=" 
				+ String.valueOf(System.currentTimeMillis()-startTime));
		} else {
			if (log.isLoggable(Level.FINE)) log.fine(getColumnName() + ": #" + p_data.size() + " - ms=" 
				+ String.valueOf(System.currentTimeMillis()-startTime));
		}
		
	}   //  fillComboBox

	/**
	 *  Fill UI component with temporary buffered data (if exists) or load from DB
	 *  @param restore if true, use temporary buffered data - else load from DB
	 */
	public void fillComboBox (boolean restore)
	{
		if (restore && m_tempData != null)
		{
			Object obj = m_selectedObject;
			p_data.clear();
			//  restore old data
			p_data = new ArrayList<Object>(m_tempData.length);
			for (int i = 0; i < m_tempData.length; i++)
				p_data.add(m_tempData[i]);
			m_tempData = null;

			//  if nothing selected, select first
			if (obj == null && p_data.size() > 0)
				obj = p_data.get(0);
			setSelectedItem(obj);
			
			fireContentsChanged(this, 0, p_data.size());
			return;
		}
		if (p_data != null)
			fillComboBox(isMandatory(), true, true, false, false); // IDEMPIERE 90
	}   //  fillComboBox
	
	/**
	 *	Get Display Text of Key Value
	 *  @param key key value
	 *  @return Display Text
	 */
	public abstract String getDisplay (Object key);

	/**
	 *	Get {@link NamePair} of Key Value
	 *  @param key key value
	 *  @return NamePair or null
	 */
	public abstract NamePair get (Object key);

	/**
	 *  Load Data (Value/KeyNamePair) from DB.
	 *  @param mandatory  has mandatory data only (i.e. no "null" selection)
	 *  @param onlyValidated only validated
	 *  @param onlyActive only active
	 * 	@param temporary force load for temporary display
	 *  @param shortlist
	 *  @return ArrayList
	 */
	public abstract ArrayList<Object> getData (boolean mandatory, 
		boolean onlyValidated, boolean onlyActive, boolean temporary, boolean shortlist); // IDEMPIERE 90

	/**
	 *	Get underlying fully qualified Table.Column Name.
	 *  @return column name
	 */
	public abstract String getColumnName();

	/**
	 *  The Lookup contains the key
	 *  @param key key
	 *  @return true if contains key
	 */
	public abstract boolean containsKey (Object key);

	/**
	 *  The Lookup contains the key (do not check the direct lookup list)
	 *  @param key key
	 *  @return true if contains key
	 */
	public abstract boolean containsKeyNoDirect (Object key);
	
	/**
	 *	Refresh Values - default implementation
	 *  @return size
	 */
	public int refresh()
	{
		return 0;
	}	//	refresh

	/**
	 *	Is Validated - default implementation
	 *  @return true if validated
	 */
	public boolean isValidated()
	{
		return true;
	}	//	isValidated

	/**
	 *  Get dynamic Validation SQL (none)
	 *  @return validation SQL
	 */
	public String getValidation()
	{
		return "";
	}   //  getValidation

	/**
	 *  Has Inactive records - default implementation
	 *  @return true if has inactive records
	 */
	public boolean hasInactive()
	{
		return false;
	}

	/**
	 *	Get Zoom - default implementation
	 *  @return Zoom AD_Window_ID
	 */
	public int getZoom()
	{
		return 0;
	}	//	getZoom

	/**
	 * @param isSOTrx
	 * @return Zoom AD_Window_ID
	 */
	public int getZoom(boolean isSOTrx)
	{
		return 0;
	}
	
	/**
	 *	Get Zoom - default implementation
	 * 	@param query query
	 *  @return Zoom AD_Window_ID
	 */
	public int getZoom(MQuery query)
	{
		return 0;
	}	//	getZoom

	/**
	 *	Get Zoom Query Object
	 *  @return Zoom Query
	 */
	public MQuery getZoomQuery()
	{
		return null;
	}	//	getZoomQuery

	/**
	 *	Perform Direct Lookup from Table.
	 *  @param key key value
	 *  @param saveInCache save in cache for r/w
	 * 	@param cacheLocal cache locally for r/o
	 * 	@param trxName the transaction name
	 *  @return NamePair
	 */
	public NamePair getDirect (Object key, boolean saveInCache, boolean cacheLocal, String trxName)
	{
		return get (key);
	}	//	getDirect

	/**
	 * Perform Direct Lookup from Table.
	 * @param key
	 * @param saveInCache
	 * @param cacheLocal
	 * @return NamePair
	 */
	public NamePair getDirect (Object key, boolean saveInCache, boolean cacheLocal)
	{
		return get (key);
	}	//	getDirect

	/**
	 * Perform direct lookup for keys
	 * @param keys
	 * @return name pair arrays
	 */
	public NamePair[] getDirect(Object[] keys)
	{
		List<NamePair> list = new ArrayList<NamePair>();
		for (Object key : keys)
		{
			list.add(getDirect(key, false, isValidated()));			
		}
		return list.toArray(new NamePair[0]);
	}
	
	/**
	 *  Dispose - clear items w/o firing events
	 */
	public void dispose()
	{
		if (p_data != null)
			p_data.clear();
		p_data = null;
		m_selectedObject = null;
		m_tempData = null;
		m_loaded = false;
	}   //  dispose

	/**
	 *  Wait until asynchronous loading complete
	 */
	public void loadComplete()
	{
	}   //  loadComplete
	
	/**
	 * Set lookup model as mandatory, use in loading data
	 * @param flag
	 */
	public void setMandatory(boolean flag)
	{
		m_mandatory = flag;
	}
	
	/**
	 * Is lookup model mandatory
	 * @return boolean
	 */
	public boolean isMandatory()
	{
		return m_mandatory;
	}
	
	/**
	 * Is this lookup model populated
	 * @return boolean
	 */
	public boolean isLoaded() 
	{
		return m_loaded;
	}

	// IDEMPIERE 90
	public void setShortList(boolean shortlist)
	{
		m_shortList = shortlist;
	}

	/**
	 * @return true if lookup should return a short list
	 */
	public boolean isShortList()
	{
		return m_shortList;
	}
	// IDEMPIERE 90
}	//	Lookup
