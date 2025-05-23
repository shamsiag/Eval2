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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.util.CLogger;

/**
 *  MultiMap allows multiple keys with their values.
 *  It accepts null values as keys and values.
 *  (implemented as two array lists)
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: MultiMap.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 *	@param <K> Key
 *	@param <V> Value
 */
public final class MultiMap<K,V> implements Map<K,V>, Serializable
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -947723044316916542L;

	/**
	 *  Constructor with 10 initial Capacity (same as ArrayList)
	 */
	public MultiMap()
	{
		this(10);
	}   //  MultiMap

	/**
	 *  Constructor
	 *  @param initialCapacity initial capacity
	 */
	public MultiMap(int initialCapacity)
	{
		m_keys = new ArrayList<K>(initialCapacity);
		m_values = new ArrayList<V>(initialCapacity);
	}   //  MultiMap

	private ArrayList<K>	m_keys = null;
	private ArrayList<V>	m_values = null;

	/**
	 *  Get number of elements
	 *  @return size
	 */
	@Override
	public int size()
	{
		return m_keys.size();
	}   //  size

	/**
	 *  Is Empty
	 *  @return true if empty
	 */
	@Override
	public boolean isEmpty()
	{
		return (m_keys.size() == 0);
	}   //  isEmpty

	/**
	 *  Contains Key
	 *	@param key key value 
	 *	@return true if key exist
	 */
	@Override
	public boolean containsKey(Object key)
	{
		return m_keys.contains(key);
	}   //  containsKey

	/**
	 *  Contains Value
	 *	@param value element value
	 *	@return true if value exists 
	 */
	@Override
	public boolean containsValue(Object value)
	{
		return m_values.contains(value);
	}   //  containsKey

	/**
	 *  Get ArrayList of Values of Key
	 *	@param key key 
	 *	@return value
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key)
	{
		return (V)getValues(key);
	}   //  get

	/**
	 *  Get ArrayList of Values of Key
	 *	@param key key 
	 *	@return array list of values
	 */
	public ArrayList<V> getValues (Object key)
	{
		ArrayList<V> list = new ArrayList<V>();
		//  We don't have it
		if (!m_keys.contains(key))
			return list;
		//  go through keys
		int size = m_keys.size();
		for (int i = 0; i < size; i++)
		{
			if (m_keys.get(i).equals(key))
				if (!list.contains(m_values.get(i)))
					list.add(m_values.get(i));
		}
		return list;
	}   //  getValues

	/**
	 *  Get ArrayList of Keys with Value
	 *	@param value value 
	 *	@return array list of keys 
	 */
	public ArrayList<K> getKeys (Object value)
	{
		ArrayList<K> list = new ArrayList<K>();
		//  We don't have it
		if (!m_values.contains(value))
			return list;
		//  go through keys
		int size = m_values.size();
		for (int i = 0; i < size; i++)
		{
			if (m_values.get(i).equals(value))
				if (!list.contains(m_keys.get(i)))
					list.add(m_keys.get(i));
		}
		return list;
	}   //  getKeys

	/**
	 *  Put Key and Value
	 *  @param key 
	 *  @param value 
	 *  @return always null
	 */
	@Override
	public V put (K key, V value)
	{
		m_keys.add(key);
		m_values.add(value);
		return null;
	}   //  put

	/**
	 *  Remove key
	 *	@param key key
	 * 	@return removed value 
	 */
	@Override
	public V remove (Object key)
	{
		throw new java.lang.UnsupportedOperationException("Method remove() not implemented.");
	}   //  remove

	/**
	 *  Put all
	 * @param t 
	 */
	@Override
	public void putAll(Map<? extends K,? extends V> t)
	{
		throw new java.lang.UnsupportedOperationException("Method putAll() not implemented.");
	}   //  putAll

	/**
	 *  Clear content
	 */
	@Override
	public void clear()
	{
		m_keys.clear();
		m_values.clear();
	}   //  clear

	/**
	 *  Return HashSet of Keys
	 *	@return key set 
	 */
	@Override
	public Set<K> keySet()
	{
		HashSet<K> keys = new HashSet<K>(m_keys);
		return keys;
	}   //  keySet

	/**
	 *  Return Collection of values
	 *	@return values 
	 */
	@Override
	public Collection<V> values()
	{
		return m_values;
	}	//	values

	/**
	 * 	Get entry set - not implemented
	 *	@return entry set 
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet()
	{
		throw new java.lang.UnsupportedOperationException("Method entrySet() not implemented.");
	}

	/**
	 * 	Equals - not implemented
	 * 	@param o test object 
	 *	@return true if equal 
	 */
	@Override
	public boolean equals(Object o)
	{
		throw new java.lang.UnsupportedOperationException("Method equals() not implemented.");
	}
	
	@Override
	public int hashCode()
	{
	  assert false : "hashCode not designed";
	  return 42; // any arbitrary constant will do
	}

	/**
	 *  Returns class name and number of entries
	 *  @return info
	 */
	@Override
	public String toString()
	{
		return "MultiMap #" + m_keys.size();
	}	//	toString

	/**
	 *  dump all keys - values to log
	 */
	public void printToLog()
	{
		CLogger	log = CLogger.getCLogger(getClass());
		log.fine("MultiMap.printToLog");
		int size = m_keys.size();
		for (int i = 0; i < size; i++)
		{
			Object k = m_keys.get(i);
			Object v = m_values.get(i);
			if (log.isLoggable(Level.FINEST)) log.finest(k==null ? "null" : k.toString() + "=" + v==null ? "null" : v.toString());
		}
	}   //  printToLog

}   //  MultiMap
