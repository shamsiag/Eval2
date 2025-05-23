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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.adempiere.util.ServerContext;
import org.compiere.Adempiere;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.MSort;
import org.compiere.util.Msg;
import org.compiere.util.SecureEngine;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;

/**
 *	Grid Table Model for JDBC table access, including buffering.
 *  <pre>
 *		The following data types are handled
 *			Integer		for all IDs
 *			BigDecimal	for all Numbers
 *			Timestamp	for all Dates
 *			String		for all others
 *  The data is read via r/o resultset and cached in m_buffer. Writes/updates
 *  are via dynamically constructed SQL INSERT/UPDATE statements. The record
 *  is re-read via the resultset to get results of triggers.
 *
 *  </pre>
 *  The model maintains and fires the requires TableModelEvent changes,
 *  the DataChanged events (loading, changed, etc.) as well as Vetoable Change event "RowChange"
 *  (for row changes initiated by moving the row in the table grid).
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: GridTable.java,v 1.9 2006/08/09 16:38:25 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>BF [ 1901192 ] LogMigrationScripts: GridTable.dataSave: manual update
 *			<li>BF [ 1943682 ] Copy Record should not copy IsApproved and IsGenerated
 *			<li>BF [ 1949543 ] Window freeze if there is a severe exception
 *			<li>BF [ 1984310 ] GridTable.getClientOrg() doesn't work for AD_Client/AD_Org
 *  @author victor.perez@e-evolution.com,www.e-evolution.com
 *  		<li>BF [ 2910358 ] Error in context when a field is found in different tabs.
 *  			https://sourceforge.net/p/adempiere/bugs/2255/
 *     		<li>BF [ 2910368 ] Error in context when IsActive field is found in different
 *  			https://sourceforge.net/p/adempiere/bugs/2256/
 */
public class GridTable extends AbstractTableModel
	implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1852095729689900949L;

	protected static final String SORTED_DSE_EVENT = "Sorted";
	
	public static final int DEFAULT_GRIDTABLE_LOAD_TIMEOUT_IN_SECONDS = 30;
	public static final int DEFAULT_GRIDTABLE_COUNT_TIMEOUT_IN_SECONDS = 1;

	public static final String LOAD_TIMEOUT_ERROR_MESSAGE = "GridTabLoadTimeoutError";
	public static final String COUNT_QUERY_TIMEOUT_ERROR_MESSAGE = "CountQueryTimeoutLoadBackground";
	public static final String FIND_OVER_MAX_ERROR_MESSAGE = "FindOverMax";
	public static final String BACKGROUND_LOAD_FINISHED_INFO_MESSAGE = "BackgroundLoadFinished";

	public static final String DATA_REFRESH_MESSAGE = "Refreshed";
	public static final String DATA_UPDATE_COPIED_MESSAGE = "UpdateCopied";
	public static final String DATA_INSERTED_MESSAGE = "Inserted";
	public static final String DATA_IGNORED_MESSAGE = "Ignored";
	public static final String DATA_SAVED_MESSAGE = "Saved";

	/**
	 *	JDBC Based Buffered Table
	 *
	 *  @param ctx Properties
	 *  @param AD_Table_ID table id
	 *  @param TableName table name
	 *  @param WindowNo window no
	 *  @param TabNo tab no
	 *  @param withAccessControl    if true adds AD_Client/Org restrictions
	 */
	public GridTable(Properties ctx, int AD_Table_ID, String TableName, int WindowNo, int TabNo,
		boolean withAccessControl)
	{
		this(ctx, AD_Table_ID, TableName, WindowNo, TabNo, withAccessControl, false);
	}

	/**
	 *	JDBC Based Buffered Table
	 *
	 *  @param ctx Properties
	 *  @param AD_Table_ID table id
	 *  @param TableName table name
	 *  @param WindowNo window no
	 *  @param TabNo tab no
	 *  @param withAccessControl    if true adds AD_Client/Org restrictuins
	 *  @param virtual use virtual table mode if table is mark as high volume
	 */
	public GridTable(Properties ctx, int AD_Table_ID, String TableName, int WindowNo, int TabNo,
		boolean withAccessControl, boolean virtual)
	{
		super();
		if (log.isLoggable(Level.INFO)) log.info(TableName);
		m_ctx = ctx;
		m_AD_Table_ID = AD_Table_ID;
		setTableName(TableName);
		m_WindowNo = WindowNo;
		m_TabNo = TabNo;
		m_withAccessControl = withAccessControl;
		m_virtual = virtual;
	}	//	MTable

	private static CLogger		log = CLogger.getCLogger(GridTable.class);
	private Properties          m_ctx;
	private int					m_AD_Table_ID;
	private String 		        m_tableName = "";
	private int				    m_WindowNo;
	/** Tab No 0..				*/
	private int				    m_TabNo;
	private boolean			    m_withAccessControl;
	private boolean			    m_readOnly = true;
	private boolean			    m_deleteable = true;
	//virtual table state variables
	private boolean				m_virtual;
	public static final String CTX_KeyColumnName = "KeyColumnName";
	//

	/**	Rowcount                    */
	private int				    m_rowCount = 0;
	private boolean				m_rowCountTimeout = false;
	private boolean				m_rowLoadTimeout = false;
	/**	Has Data changed?           */
	private boolean			    m_changed = false;
	/** Index of changed row via SetValueAt */
	private int				    m_rowChanged = -1;
	/** Insert mode active          */
	private boolean			    m_inserting = false;
	/** Inserted Row number         */
	private int                 m_newRow = -1;
	
	/**	Is the Resultset open?      */
	private boolean			    m_open = false;
	/**	Compare to DB before save	*/
	@Deprecated
	private boolean				m_compareDB = true;		//	set to true after every save

	/** Data buffer */
	private volatile ArrayList<Object[]>	m_buffer = new ArrayList<Object[]>(100);
	/** Sort order for {@link #m_buffer} */
	private volatile ArrayList<MSort>		m_sort = new ArrayList<MSort>(100);
	/** Record ID:Values */
	private volatile Map<Integer, Object[]> m_virtualBuffer = new HashMap<Integer, Object[]>(100);
	/** Original row data               */
	private Object[]			m_rowData = null;
	/** Original data [row,col,data]    */
	private Object[]            m_oldValue = null;
	//
	private Loader		        m_loader = null;

	/**	Columns                 		*/
	private ArrayList<GridField>	m_fields = new ArrayList<GridField>(30);
	private ArrayList<Object>	m_parameterSELECT = new ArrayList<Object>(5);
	private ArrayList<Object>	m_parameterWHERE = new ArrayList<Object>(5);

	/** Complete SQL statement          */
	private String 		        m_SQL;
	/** SQL Statement for Row Count     */
	private String 		        m_SQL_Count;
	/** The SELECT clause with FROM     */
	private String 		        m_SQL_Select;
	/** The static where clause         */
	private String 		        m_whereClause = "2=3";
	/** Show only Processed='N' and last 24h records    */
	private boolean		        m_onlyCurrentRows = false;
	/** Show only Not processed and x days				*/
	private int					m_onlyCurrentDays = 1;
	/** Static ORDER BY clause          */
	private String		        m_orderClause = "";
	/** Max Rows to query or 0 for all	*/
	private int					m_maxRows = 0;

	/** Index of Key Column                 */
	private int			        m_indexKeyColumn = -1;
	/** Index of UUID Column                */
	private int			        m_indexUUIDColumn = -1;
	/** Index of Color Column               */
	private int			        m_indexColorColumn = -1;
	/** Index of Processed Column           */
	private int                 m_indexProcessedColumn = -1;
	/** Index of IsActive Column            */
	private int                 m_indexActiveColumn = -1;
	/** Index of AD_Client_ID Column        */
	private int					m_indexClientColumn = -1;
	/** Index of AD_Org_ID Column           */
	private int					m_indexOrgColumn = -1;

	/** Vetoable Change Bean support    */
	private VetoableChangeSupport   m_vetoableChangeSupport = new VetoableChangeSupport(this);
	private Future<?> m_loaderFuture;
	/** Property of Vetoable Bean support "RowChange" */
	public static final String  PROPERTY = "MTable-RowSave";

	private final static Integer NEW_ROW_ID = Integer.valueOf(-1);
	private static final int DEFAULT_FETCH_SIZE = 200;

	/** Keep track of last sorted column index and sort direction */
	private int 				m_lastSortColumnIndex = -1;
	private boolean 			m_lastSortedAscending = true;

	/**
	 *	Set Table Name
	 *  @param newTableName table name
	 */
	public void setTableName(String newTableName)
	{
		if (m_open)
		{
			log.log(Level.SEVERE, "Table already open - ignored");
			return;
		}
		if (newTableName == null || newTableName.length() == 0)
			return;
		m_tableName = newTableName;
	}	//	setTableName

	/**
	 *	Get Table Name
	 *  @return table name
	 */
	public String getTableName()
	{
		return m_tableName;
	}	//	getTableName

	/**
	 *	Set Where Clause (w/o the WHERE keyword and w/o History).
	 *  @param newWhereClause sql where clause
	 *  @param onlyCurrentRows only current rows
	 *  @param onlyCurrentDays how many days back for current
	 *	@return true if where clase set
	 */
	public boolean setSelectWhereClause(String newWhereClause, boolean onlyCurrentRows, int onlyCurrentDays)
	{
		if (m_open)
		{
			log.log(Level.SEVERE, "Table already open - ignored");
			return false;
		}
		//
		m_whereClause = newWhereClause;
		m_onlyCurrentRows = onlyCurrentRows;
		m_onlyCurrentDays = onlyCurrentDays;
		if (m_whereClause == null)
			m_whereClause = "";
		return true;
	}	//	setWhereClause

	/**
	 *	Get Where Clause (w/o the WHERE keyword and w/o History)
	 *  @return where clause
	 */
	public String getSelectWhereClause()
	{
		return m_whereClause;
	}	//	getWhereClause

	/**
	 *	Is History displayed
	 *  @return true if history displayed
	 */
	public boolean isOnlyCurrentRowsDisplayed()
	{
		return !m_onlyCurrentRows;
	}	//	isHistoryDisplayed

	/**
	 *	Set Order Clause (w/o the ORDER BY keyword)
	 *  @param newOrderClause sql order by clause
	 */
	public void setOrderClause(String newOrderClause)
	{
		m_orderClause = newOrderClause;
		if (m_orderClause == null)
			m_orderClause = "";
	}	//	setOrderClause

	/**
	 *	Get Order By Clause (w/o the ORDER BY keyword)
	 *  @return order by clause
	 */
	public String getOrderClause()
	{
		return m_orderClause;
	}	//	getOrderClause

	/**
	 *	Assemble & store {@link #m_SQL}, {@link #m_SQL_Select} and {@link #m_SQL_Count}.
	 *  @return m_SQL
	 */
	private String createSelectSql()
	{
		if (m_fields.size() == 0 || m_tableName == null || m_tableName.equals(""))
			return "";

		//	Create SELECT Part
		StringBuilder select = new StringBuilder("SELECT ");
		for (int i = 0; i < m_fields.size(); i++)
		{
			if (i > 0)
				select.append(",");
			GridField field = (GridField)m_fields.get(i);
			select.append(field.isVirtualColumn() ? field.getColumnSQL(true) : DB.getDatabase().quoteColumnName(field.getColumnSQL(true)));	//	ColumnName or Virtual Column
		}
		//
		select.append(" FROM ").append(m_tableName);
		m_SQL_Select = select.toString();
		m_SQL_Count = "SELECT COUNT(*) FROM " + m_tableName;
		//BF [ 2910358 ] 
		//Restore the Original Value for Key Column Name based in Tab Context Value
		int parentTabNo = getParentTabNo();
		String parentKey = Env.getContext(m_ctx, m_WindowNo, parentTabNo, GridTab.CTX_KeyColumnName, true);
		String valueKey = null;
		String currKey = null;
		if (parentKey != null && parentKey.length() > 0) {
			valueKey = Env.getContext(m_ctx, m_WindowNo, parentTabNo, parentKey, true);
			currKey = Env.getContext(m_ctx, m_WindowNo, parentKey);
			if (currKey == null)
				currKey = new String("");
			if (valueKey != null && valueKey.length() > 0 && parentKey != null && parentKey.length() > 0 && ! currKey.equals(valueKey))
			{
				Env.setContext(m_ctx, m_WindowNo,  parentKey, valueKey);
			}	
		}
		
		StringBuilder where = new StringBuilder("");
		//	WHERE
		if (m_whereClause.length() > 0)
		{
			where.append(" WHERE (");
			if (m_whereClause.indexOf('@') == -1)
				where.append(m_whereClause);
			else    //  replace variables
			{
				String context = Env.parseContext(m_ctx, m_WindowNo, m_whereClause, false);
				if(context != null && context.trim().length() > 0)
				{
					where.append(context);
				}
				else
				{
					log.log(Level.WARNING, "Failed to parse where clause. whereClause="+m_whereClause);
					where.append(" 1 = 2 ");
				}
			}
			where.append(")");
		}
		if (m_onlyCurrentRows && m_TabNo == 0)
		{
			if (where.toString().indexOf(" WHERE ") == -1)
				where.append(" WHERE ");
			else
				where.append(" AND ");
			//	Show only unprocessed or the one updated within x days
			where.append("(Processed='N' OR Updated>");
			where.append("getDate()-1");
			where.append(")");
		}

		//	RO/RW Access
		m_SQL = m_SQL_Select + where.toString();
		m_SQL_Count += where.toString();
		if (m_withAccessControl)
		{
			m_SQL = MRole.getDefault(m_ctx, false).addAccessSQL(m_SQL, 
				m_tableName, MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);
			m_SQL_Count = MRole.getDefault(m_ctx, false).addAccessSQL(m_SQL_Count, 
				m_tableName, MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);
		}

		//	ORDER BY
		if (!m_orderClause.equals(""))
		{
			m_SQL += " ORDER BY " + m_orderClause;
		}
		
		//IDEMPIERE-5193 Add Limit to Query
		if(m_maxRows > 0 && DB.getDatabase().isPagingSupported())
		{
			// set to maxRows plus one to trigger FindOverMax on overflow
			m_SQL = DB.getDatabase().addPagingSQL(m_SQL, 1, m_maxRows+1);
		}
		
		//
		if (log.isLoggable(Level.FINE))
			log.fine(m_SQL_Count);
		Env.setContext(m_ctx, m_WindowNo, m_TabNo, GridTab.CTX_SQL, m_SQL);
		return m_SQL;
	}	//	createSelectSql

	/**
	 *	Add Field to Table
	 *  @param field field
	 */
	public void addField (GridField field)
	{
		if (log.isLoggable(Level.FINE)) log.fine("(" + m_tableName + ") - " + field.getColumnName());
		if (m_open)
		{
			log.log(Level.SEVERE, "Table already open - ignored: " + field.getColumnName());
			return;
		}
		if (!MRole.getDefault(m_ctx, false).isColumnAccess (m_AD_Table_ID, field.getAD_Column_ID(), true))
		{
			if (log.isLoggable(Level.FINE)) log.fine("No Column Access " + field.getColumnName());
			return;			
		}
		//  Set Index for Key column
		if (field.isKey())
			m_indexKeyColumn = m_fields.size();
		else if (field.isUUID())
			m_indexUUIDColumn = m_fields.size();
		//  Set Index of other standard columns
		if (field.getColumnName().equals("IsActive"))
			m_indexActiveColumn = m_fields.size();
		else if (field.getColumnName().equals("Processed"))
			m_indexProcessedColumn = m_fields.size();
		else if (field.getColumnName().equals("AD_Client_ID"))
			m_indexClientColumn = m_fields.size();
		else if (field.getColumnName().equals("AD_Org_ID"))
			m_indexOrgColumn = m_fields.size();
		//
		m_fields.add(field);
	}	//	addColumn

	/**
	 *  Returns database column name
	 *
	 *  @param index  column index
	 *  @return column name or empty string (if index is invalid)
	 */
	public String getColumnName (int index)
	{
		if (index < 0 || index > m_fields.size())
		{
			log.log(Level.SEVERE, "Invalid index=" + index);
			return "";
		}
		//
		GridField field = (GridField)m_fields.get(index);
		return field.getColumnName();
	}   //  getColumnName

	/**
	 * Find column index via column name.
	 *
	 * @param columnName string containing name of column to be located
	 * @return the column index with <code>columnName</code>, or -1 if not found
	 */
	public int findColumn (String columnName)
	{
		for (int i = 0; i < m_fields.size(); i++)
		{
			GridField field = (GridField)m_fields.get(i);
			if (columnName.equals(field.getColumnName()))
				return i;
		}
		return -1;
	}   //  findColumn

	/**
	 *  Returns Class of database column/field
	 *
	 *  @param index column index
	 *  @return Java class for column
	 */
	public Class<?> getColumnClass (int index)
	{
		if (index < 0 || index >= m_fields.size())
		{
			log.log(Level.SEVERE, "Invalid index=" + index);
			return null;
		}
		GridField field = (GridField)m_fields.get(index);
		return DisplayType.getClass(field.getDisplayType(), false);
	}   //  getColumnClass

	/**
	 *	Set Select Clause Parameter.
	 *	Assumes that you set parameters starting from index zero
	 *  @param index index
	 *  @param parameter parameter
	 */
	public void setParameterSELECT (int index, Object parameter)
	{
		if (index >= m_parameterSELECT.size())
			m_parameterSELECT.add(parameter);
		else
			m_parameterSELECT.set(index, parameter);
	}	//	setParameterSELECT

	/**
	 *	Set Where Clause Parameter.
	 *	Assumes that you set parameters starting from index zero
	 *  @param index index
	 *  @param parameter parameter
	 */
	public void setParameterWHERE (int index, Object parameter)
	{
		if (index >= m_parameterWHERE.size())
			m_parameterWHERE.add(parameter);
		else
			m_parameterWHERE.set(index, parameter);
	}	//	setParameterWHERE


	/**
	 *	Get GridField at index
	 *  @param index index
	 *  @return GridField
	 */
	public GridField getField (int index)
	{
		if (index < 0 || index >= m_fields.size())
			return null;
		return (GridField)m_fields.get(index);
	}	//	getField

	/**
	 *	Get GridField with ColumnName
	 *  @param identifier column name
	 *  @return GridField or null
	 */
	protected GridField getField (String identifier)
	{
		if (identifier == null || identifier.length() == 0)
			return null;
		int cols = m_fields.size();
		for (int i = 0; i < cols; i++)
		{
			GridField field = (GridField)m_fields.get(i);
			if (identifier.equalsIgnoreCase(field.getColumnName()))
				return field;
		}
		return null;
	}	//	getField

	/**
	 *  Get all Fields
	 *  @return GridField[]
	 */
	public GridField[] getFields ()
	{
		GridField[] retValue = new GridField[m_fields.size()];
		m_fields.toArray(retValue);
		return retValue;
	}   //  getField
	
	/**
	 *	Open connection to db and load data from table.
	 *  If already opened, data is refreshed.<br/>
	 *  Loading of data is perform asynchronously in background thread. 
	 *	@param maxRows maximum number of rows or 0 for all
	 *	@return true if success
	 */
	public boolean open (int maxRows)
	{
		if (log.isLoggable(Level.INFO)) log.info("MaxRows=" + maxRows);
		m_maxRows = maxRows;
		if (m_open)
		{
			if (log.isLoggable(Level.FINE))
				log.fine("already open");
			dataRefreshAll();
			return true;
		}

		if (m_virtual)
		{
			verifyVirtual();
		}

		//	create m_SQL and m_countSQL
		createSelectSql();
		if (m_SQL == null || m_SQL.equals(""))
		{
			log.log(Level.SEVERE, "No SQL");
			return false;
		}

		if (!m_open)
		{
			m_open = true;
		}

		//	Start Loading
		m_loader = new Loader();
		m_loaderFuture = null;
		m_rowCount = m_loader.open(maxRows);
		if (m_virtual)
		{
			m_buffer = null;
			m_virtualBuffer = new HashMap<Integer, Object[]>(210);
		}
		else
		{
			m_buffer = new ArrayList<Object[]>(m_rowCount+10);
		}
		m_sort = new ArrayList<MSort>(m_rowCount+10);
		// actual row count or timeout
		if (m_rowCount > 0 || m_rowCountTimeout) 
		{
			m_loader.setContext(ServerContext.getCurrentInstance());
			m_loaderFuture = Adempiere.getThreadPoolExecutor().submit(m_loader);
		}
		else
			m_loader.close();
		//
		m_changed = false;
		m_rowChanged = -1;
		m_inserting = false;
		return true;
	}	//	open

	/**
	 * Verify whether use of virtual buffer is supported
	 */
	private void verifyVirtual()
	{
		if (m_indexKeyColumn == -1)
		{
			m_virtual = false;
			return;
		}
		GridField[] fields = getFields();
		for(int i = 0; i < fields.length; i++)
		{
			if (fields[i].isKey() && i != m_indexKeyColumn)
			{
				m_virtual = false;
				return;
			}
		}
	}

	/**
	 *  Wait until asynchronous loading of Table and Lookup Fields is complete.
	 */
	public void loadComplete()
	{
		//  Wait for loader
		if (m_loaderFuture != null)
		{
			if (!m_loaderFuture.isDone())
			{
				try
				{
					m_loaderFuture.get();
				}
				catch (Exception ie)
				{
					log.log(Level.SEVERE, "Interrupted", ie);
				}
			}
		}
		//  wait for field lookup loaders
		for (int i = 0; i < m_fields.size(); i++)
		{
			GridField field = (GridField)m_fields.get(i);
			field.lookupLoadComplete();
		}
	}   //  loadComplete

	/**
	 *  Is Loading
	 *  @return true if loading is in progress
	 */
	public boolean isLoading()
	{
		if (m_loaderFuture != null && !m_loaderFuture.isDone())
			return true;
		return false;
	}   //  isLoading

	/**
	 * wait for the loading of data
	 * @param timeout timeout in milisecond. pass 0 or negative value for infinite wait
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException 
	 */
	public void waitLoading(long timeout) throws InterruptedException, ExecutionException, TimeoutException
	{
		if (m_loaderFuture != null && !m_loaderFuture.isDone()) {
			if (timeout > 0)
				m_loaderFuture.get(timeout, TimeUnit.MILLISECONDS);
			else
				m_loaderFuture.get();
		}
	}
	
	/**
	 *	Is it open?
	 *  @return true if opened
	 */
	public boolean isOpen()
	{
		return m_open;
	}	//	isOpen

	/**
	 *	Close Resultset
	 *  @param finalCall true for final call and perform clean up
	 */
	public void close (boolean finalCall)
	{
		if (!m_open)
			return;
		if (log.isLoggable(Level.FINE)) log.fine("final=" + finalCall);

		//  remove listeners
		if (finalCall)
		{
			DataStatusListener evl[] = (DataStatusListener[])listenerList.getListeners(DataStatusListener.class);
			for (int i = 0; i < evl.length; i++)
				listenerList.remove(DataStatusListener.class, evl[i]);
			TableModelListener ev2[] = (TableModelListener[])listenerList.getListeners(TableModelListener.class);
			for (int i = 0; i < ev2.length; i++)
				listenerList.remove(TableModelListener.class, ev2[i]);
			VetoableChangeListener vcl[] = m_vetoableChangeSupport.getVetoableChangeListeners();
			for (int i = 0; i < vcl.length; i++)
				m_vetoableChangeSupport.removeVetoableChangeListener(vcl[i]);
		}

		//	Stop loader
		while (m_loaderFuture != null && !m_loaderFuture.isDone())
		{
			if (log.isLoggable(Level.FINE))
				log.fine("Interrupting Loader ...");
			m_loaderFuture.cancel(true);
			try
			{
				Thread.sleep(200);		//	.2 second
			}
			catch (InterruptedException ie)
			{}
			m_loaderFuture = null;
		}

		if (!m_inserting)
			dataSave(false);	//	not manual

		if (m_buffer != null)
		{
			m_buffer.clear();
		}
		if (m_sort != null)
		{
			m_sort.clear();
		}
		if (m_virtualBuffer != null)
		{
			m_virtualBuffer.clear();
		}

		if (finalCall) {
			dispose();
			m_buffer = null;
			m_sort = null;
			m_virtualBuffer = null;
		}

		//  Fields are disposed from MTab
		if (log.isLoggable(Level.FINE))
			log.fine("");
		m_open = false;
	}	//	close

	/**
	 *  Clean up.
	 *  Called by close-final.
	 */
	private void dispose()
	{
		//  GridFields
		for (int i = 0; i < m_fields.size(); i++)
			((GridField)m_fields.get(i)).dispose();
		m_fields.clear();
		m_fields = null;
		//
		m_vetoableChangeSupport = null;
		//
		m_parameterSELECT.clear();
		m_parameterSELECT = null;
		m_parameterWHERE.clear();
		m_parameterWHERE = null;
		//  clear data arrays
		m_buffer = null;
		m_virtualBuffer = null;
		m_sort = null;
		m_rowData = null;
		m_oldValue = null;
		m_loader = null;
		m_loaderFuture = null;
	}   //  dispose

	/**
	 *	Get column count
	 *  @return column count
	 */
	public int getColumnCount()
	{
		return m_fields.size();
	}	//	getColumnCount

	/**
	 *	Get field count
	 *  @return field count
	 */
	public int getFieldCount()
	{
		return m_fields.size();
	}	//	getFieldCount

	/**
	 *  Return number of rows
	 *  @return Number of rows or 0 if not opened
	 */
	public int getRowCount()
	{
		return m_rowCount;
	}	//	getRowCount

	/**
	 *	Set the Column to determine the color of the row
	 *  @param columnName column name
	 */
	public void setColorColumn (String columnName)
	{
		m_indexColorColumn = findColumn(columnName);
	}	//  setColorColumn

	/**
	 *	Get ColorCode for Row.
	 *  <pre>
	 *	If numerical value in compare column is
	 *		negative = -1,
	 *      positive = 1,
	 *      otherwise = 0
	 *  </pre>
	 *  @see #setColorColumn
	 *  @param row row
	 *  @return color code
	 */
	public int getColorCode (int row)
	{
		if (m_indexColorColumn  == -1)
			return 0;
		Object data = getValueAt(row, m_indexColorColumn);
		//	We need to have a Number
		if (data == null || !(data instanceof BigDecimal))
			return 0;
		BigDecimal bd = (BigDecimal)data;
		return bd.signum();
	}	//	getColorCode


	/**
	 *	Sort records by Column.
	 *  <p>
	 *  Actually the rows are not sorted, just the access pointer ArrayList
	 *  with the same size as m_buffer ({@link #m_sort}).
	 *  @param col col
	 *  @param ascending ascending
	 */
	public void sort (int col, boolean ascending)
	{
		if (log.isLoggable(Level.INFO)) log.info("#" + col + " " + ascending);
		if (col < 0) {
			return;
		}
		if (getRowCount() == 0)
			return;

		GridField field = getField(col);

		// Ignoring new record while sorting
		if (field.getGridTab().isQuickForm())
			dataIgnore();

		boolean isSameSortEntries = (col == m_lastSortColumnIndex && ascending == m_lastSortedAscending);
		if (!isSameSortEntries)
		{
			m_lastSortColumnIndex = col;
			m_lastSortedAscending = ascending;
		}

		//cache changed row
		MSort changedRow = m_rowChanged >= 0 ? (MSort)m_sort.get(m_rowChanged) : null;
		if (m_rowChanged == m_newRow)
			changedRow = null;
		Object[] changedRowData = changedRow != null ? getDataAtRow(m_rowChanged) : null;
		
		MSort newRow = m_newRow >= 0 ? (MSort)m_sort.get(m_newRow) : null;
		
		MSort currentRow = m_currentRow >= 0 && m_currentRow < m_sort.size() ? (MSort)m_sort.get(m_currentRow) : null;

		//	RowIDs are not sorted
		if (field.getDisplayType() == DisplayType.RowID)
			return;
		boolean isLookup = DisplayType.isLookup(field.getDisplayType());
		boolean isASI = DisplayType.PAttribute == field.getDisplayType();

		//	fill MSort entities with data entity
		for (int i = 0; i < m_sort.size(); i++)
		{
			MSort sort = (MSort)m_sort.get(i);
			Object[] rowData = getDataAtRow(i);
			if (rowData[col] == null)
				sort.data = null;
			else if (isLookup || isASI)
				sort.data = field.getLookup().getDisplay(rowData[col]);	//	lookup
			else
				sort.data = rowData[col];								//	data
		}
		if (log.isLoggable(Level.INFO)) log.info(field.toString() + " #" + m_sort.size());

		//	sort it
		MSort sort = new MSort(0, null);
		sort.setSortAsc(ascending);
		Collections.sort(m_sort, sort);
		if (m_virtual)
		{
			Object[] newRowData = newRow != null ? m_virtualBuffer.get(NEW_ROW_ID) : null;
			m_virtualBuffer.clear();
			if (newRow != null)
				m_virtualBuffer.put(NEW_ROW_ID, newRowData);

			if (changedRow != null)
			{				
				for(int i = 0; i < m_sort.size(); i++)
				{
					if (m_sort.get(i) == changedRow)
					{
						m_rowChanged = i;
						m_virtualBuffer.put(changedRow.index, changedRowData);
						break;
					}
				}
			}

			//release sort memory
			for (int i = 0; i < m_sort.size(); i++)
			{
				m_sort.get(i).data = null;
				if (newRow != null && m_sort.get(i) == newRow)
				{
					if (m_rowChanged == m_newRow)
						m_rowChanged = i;
					m_newRow = i;
				}
				
				if (currentRow != null && m_sort.get(i) == currentRow)
					m_currentRow = i;
			}
		}
		else
		{
			for (int i = 0; i < m_sort.size(); i++)
			{
				if (newRow != null && m_sort.get(i) == newRow)
				{
					if (m_rowChanged == m_newRow)
						m_rowChanged = i;
					m_newRow = i;
				}
				
				if (currentRow != null && m_sort.get(i) == currentRow)
					m_currentRow = i;
			}
		}
		
		if (!isSameSortEntries)
		{
			//  Info detected by MTab.dataStatusChanged and current row set to 0
			fireDataStatusIEvent(SORTED_DSE_EVENT, "#" + m_sort.size());
			//	update UI
			fireTableDataChanged();
		}
	}	//	sort

	/**
	 *	Get Key ID or -1 of none
	 *  @param row row index
	 *  @return ID or -1
	 */
	public int getKeyID (int row)
	{
		if (m_indexKeyColumn != -1)
		{
			try
			{
				Integer ii = (Integer)getValueAt(row, m_indexKeyColumn);
				if (ii == null)
					return -1;
				return ii.intValue();
			}
			catch (Exception e)     //  Alpha Key
			{
				return -1;
			}
		}
		return -1;
	}	//	getKeyID

	/**
	 *	Get Key UUID or null of none
	 *  @param row row index
	 *  @return UUID or null
	 */
	public String getKeyUUID (int row)
	{
		if (m_indexUUIDColumn != -1)
		{
			try
			{
				String ii = (String)getValueAt(row, m_indexUUIDColumn);
				if (ii == null)
					return null;
				return ii;
			}
			catch (Exception e)
			{
				return null;
			}
		}
		return null;
	}	//	getKeyUUID

	/**
	 *	Get UUID or null of none
	 *  @param row row index
	 *  @return UUID or null
	 */
	public UUID getUUID (int row)
	{
		String keyUUID = getKeyUUID(row);
		if (keyUUID != null)
			return UUID.fromString(keyUUID);
		return null;
	}	//	getUUID

	/**
	 *	Get Key ColumnName
	 *  @return key column name
	 */
	public String getKeyColumnName()
	{
		if (m_indexKeyColumn != -1)
			return getColumnName(m_indexKeyColumn);
		return "";
	}	//	getKeyColumnName

	/**
	 * 	Get Value at row and column
	 *  @param row row
	 *  @param col col
	 *  @return Value at row/column
	 */
	public Object getValueAt (int row, int col)
	{
		if (!m_open || row < 0 || col < 0 || row >= m_rowCount)
		{
			return null;
		}

		waitLoadingForRow(row);

		//	empty buffer
		if (row >= m_sort.size())
		{
			return null;
		}

		//	return Data item
		Object[] rowData = getDataAtRow(row);
		//	out of bounds
		if (rowData == null || col > rowData.length)
		{
			return null;
		}
		return rowData[col];
	}	//	getValueAt

	/**
	 * wait for loading of row
	 * @param row
	 */
	public void waitLoadingForRow(int row) {
		// optimization - this method is called too frequently
		// avoid processing when the criteria of whiles and if's below is not  met
		if ( ! (row >= m_sort.size() || m_sort.size() == 0) )
			return;
		//	need to wait for data read into buffer
		int loops = 0;
		//wait for [timeout] seconds
		int timeout = MSysConfig.getIntValue(MSysConfig.GRIDTABLE_LOAD_TIMEOUT_IN_SECONDS, DEFAULT_GRIDTABLE_LOAD_TIMEOUT_IN_SECONDS, Env.getAD_Client_ID(Env.getCtx()));
		if (timeout <= 0) {
			timeout = 120;
			log.warning("Setting GRIDTABLE_LOAD_TIMEOUT_IN_SECONDS = 0 is not recommended as long queries can keep running in the database and affects performance, "
					+ " in this case the system assigns a wait of 2 minutes to load records of a window");
		} else {
			timeout += 5; /* give 5 seconds extra for the query to timeout first in the database */
		}
		while (row >= m_sort.size() && m_loaderFuture != null && !m_loaderFuture.isDone() && !m_rowLoadTimeout && loops < timeout)
		{
			if (log.isLoggable(Level.FINE)) log.fine("Waiting for loader row=" + row + ", size=" + m_sort.size());
			try
			{
				m_loaderFuture.get(1000, TimeUnit.MILLISECONDS);
			}
			catch (Exception ie)
			{}
			loops++;
		}
		if (m_sort.size() == 0) {
			// check if there is a DB error saved to show
			Exception savedEx = CLogger.retrieveException();
			if (savedEx != null)
				throw new IllegalStateException(savedEx);
		}
		// zero rows found without load timeout
		if (row == 0 && m_sort.size() == 0 && m_rowCountTimeout && !m_rowLoadTimeout)
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "FindZeroRecords"));
		if (row >= m_sort.size()) {
			log.warning("Reached " + timeout + " seconds timeout loading row " + (row+1) + " for SQL=" + m_SQL);
			//adjust row count
			m_rowCount = m_sort.size();
			m_rowLoadTimeout = true; // at this moment this must be set, but just to be sure that the warning is raised at doRun
		}
	}

	/**
	 * @param row row index
	 * @return data at row index
	 */
	private Object[] getDataAtRow(int row)
	{
		return getDataAtRow(row, true);
	}

	/**
	 * @param row row index
	 * @param fetchIfNotFound
	 * @return data at row index
	 */
	private Object[] getDataAtRow(int row, boolean fetchIfNotFound)
	{
		waitLoadingForRow(row);
		MSort sort = (MSort)m_sort.get(row);
		Object[] rowData = null;
		if (m_virtual)
		{
			if (sort.index != NEW_ROW_ID && !(m_virtualBuffer.containsKey(sort.index)) && fetchIfNotFound)
			{
				fillBuffer(row, DEFAULT_FETCH_SIZE);
			}
			rowData = (Object[])m_virtualBuffer.get(sort.index);
		}
		else
		{
			rowData = (Object[])m_buffer.get(sort.index);
		}
		return rowData;
	}

	/**
	 * @param row row index
	 * @param rowData
	 */
	private void setDataAtRow(int row, Object[] rowData) {
		MSort sort = m_sort.get(row);
		if (m_virtual)
		{
			if (sort.index != NEW_ROW_ID && !(m_virtualBuffer.containsKey(sort.index)))
			{
				fillBuffer(row, DEFAULT_FETCH_SIZE);
			}
			m_virtualBuffer.put(sort.index, rowData);
		}
		else
		{
			m_buffer.set(sort.index, rowData);
		}

	}

	/**
	 * Fill virtual buffer ({@link #m_virtualBuffer}.
	 * @param start
	 * @param fetchSize
	 */
	private void fillBuffer(int start, int fetchSize)
	{
		//adjust start if needed
		if (start > 0)
		{
			if (start + fetchSize >= m_sort.size())
			{
				start = start - (fetchSize - ( m_sort.size() - start ));
				if (start < 0)
					start = 0;
			}
		}
		StringBuilder sql = new StringBuilder();
		sql.append(m_SQL_Select)
			.append(" WHERE ")
			.append(getKeyColumnName())
			.append(" IN (");
		Map<Integer, Integer>rowmap = new LinkedHashMap<Integer, Integer>(DEFAULT_FETCH_SIZE);
		int count = 0;
		for(int i = start; i < start+fetchSize && i < m_sort.size(); i++)
		{
			if (m_sort.get(i).index == NEW_ROW_ID)
					continue;
			if(count > 0)
				sql.append(",");
			sql.append(m_sort.get(i).index);
			rowmap.put(m_sort.get(i).index, i);
			count++;
		}
		sql.append(")");

		Object[] newRow = m_virtualBuffer.get(NEW_ROW_ID);
		//cache changed row
		Object[] changedRow = m_rowChanged >= 0 ? getDataAtRow(m_rowChanged, false) : null;
		m_virtualBuffer = new HashMap<Integer, Object[]>(210);
		if (newRow != null && newRow.length > 0)
			m_virtualBuffer.put(NEW_ROW_ID, newRow);		

		PreparedStatement stmt = null;
		ResultSet rs = null;
		m_rowLoadTimeout = false;
		try
		{
			stmt = DB.prepareStatement(sql.toString(), null);
			int timeout = MSysConfig.getIntValue(MSysConfig.GRIDTABLE_LOAD_TIMEOUT_IN_SECONDS, DEFAULT_GRIDTABLE_LOAD_TIMEOUT_IN_SECONDS, Env.getAD_Client_ID(Env.getCtx()));
			if (timeout > 0)
				stmt.setQueryTimeout(timeout);
			rs = stmt.executeQuery();
			while(rs.next())
			{
				Object[] data = readData(rs);
				rowmap.remove(data[m_indexKeyColumn]);
				m_virtualBuffer.put((Integer)data[m_indexKeyColumn], data);
			}
			if (!rowmap.isEmpty())
			{
				List<Integer> toremove = new ArrayList<Integer>();
				for(Map.Entry<Integer, Integer> entry : rowmap.entrySet())
				{
					toremove.add(entry.getValue());
				}
				Collections.reverse(toremove);
				for(Integer row : toremove)
				{
					m_sort.remove(row.intValue());
				}
			}
			
			if (changedRow != null && changedRow.length > 0)
			{
				if (changedRow[m_indexKeyColumn] != null && (Integer)changedRow[m_indexKeyColumn] > 0)
				{
					m_virtualBuffer.put((Integer)changedRow[m_indexKeyColumn], changedRow);
				}
			}
		}
		catch (SQLException e)
		{
			if (DB.getDatabase().isQueryTimeout(e))
				m_rowLoadTimeout = true;
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		finally
		{
			DB.close(rs, stmt);
		}
	}
	
	/**
	 *	Indicate that there will be a change
	 *  @param changed changed
	 */
	public void setChanged (boolean changed)
	{
		//	Can we edit?
		if (!m_open || m_readOnly)
			return;

		//	Indicate Change
		m_changed = changed;
		if (!changed)
			m_rowChanged = -1;
	}	//	setChanged

	/**
	 * 	Set value at row and column
	 *
	 *  @param  value value to assign to cell
	 *  @param  row row index of cell
	 *  @param  col column index of cell
	 */
	public final void setValueAt (Object value, int row, int col)
	{
		setValueAt (value, row, col, false, false);
	}	//	setValueAt

	/**
	 * 	call {@link #setValueAt(Object, int, int, boolean, boolean)} with isInitEdit = false
	 *
	 *  @param  value value to assign to cell
	 *  @param  row row index of cell
	 *  @param  col column index of cell
	 * 	@param	force force setting new value
	 */
	public final void setValueAt (Object value, int row, int col, boolean force)
	{
		setValueAt (value, row, col, force, false);
	}	//	setValueAt
	
	/**
	 * 	Set value in row data and update GridField.
	 *
	 *  @param  value value to assign to cell
	 *  @param  row row index of cell
	 *  @param  col column index of cell
	 * 	@param	force force setting new value
	 *  @param	isInitEdit indicate event rise by start edit a field. just want change status to edit, don't change anything else
	 */
	public final void setValueAt (Object value, int row, int col, boolean force, boolean isInitEdit)
	{
		//	Can we edit?
		if (!m_open || m_readOnly       //  not accessible
				|| row < 0 || col < 0   //  invalid index
				|| m_rowCount == 0	//  no rows
				|| row >= m_rowCount )     //invalid row
		{
			if (log.isLoggable(Level.FINEST)) log.finest("r=" + row + " c=" + col + " - R/O=" + m_readOnly + ", Rows=" + m_rowCount + " - Ignored");
			return;
		}

		dataSave(row, false);

		//	Has anything changed?
		Object oldValue = getValueAt(row, col);
		if (!force && !isInitEdit && !isValueChanged(oldValue, value) )
		{
			if (log.isLoggable(Level.FINEST)) log.finest("r=" + row + " c=" + col + " - New=" + value + "==Old=" + oldValue + " - Ignored");
			return;
		}

		if (log.isLoggable(Level.FINE)) log.fine("r=" + row + " c=" + col + " = " + value + " (" + oldValue + ")");

		//  Save old value
		m_oldValue = new Object[3];
		m_oldValue[0] = Integer.valueOf(row);
		m_oldValue[1] = Integer.valueOf(col);
		m_oldValue[2] = oldValue;

		//	Set Data item
		
		Object[] rowData = getDataAtRow(row);
		m_rowChanged = row;

		//	save original value - shallow copy
		if (m_rowData == null)
		{
			int size = m_fields.size();
			m_rowData = new Object[size];
			for (int i = 0; i < size; i++)
				m_rowData[i] = rowData[i];
		}

		//	save & update
		rowData[col] = value;
		setDataAtRow(row, rowData);
		//  update Table
		fireTableCellUpdated(row, col);
		//  update GridField
		GridField field = getField(col);
		field.setValue(value, m_inserting);
		//  inform
		DataStatusEvent evt = createDSE();
		evt.setIsInitEdit(isInitEdit);
		evt.setChangedColumn(col, field.getColumnName());
		fireDataStatusChanged(evt);
	}	//	setValueAt

	/**
	 *  Get Old Value
	 *  @param row row
	 *  @param col col
	 *  @return old value
	 */
	public Object getOldValue (int row, int col)
	{
		if (m_oldValue == null)
			return null;
		if (((Integer)m_oldValue[0]).intValue() == row
				&& ((Integer)m_oldValue[1]).intValue() == col)
			return m_oldValue[2];
		return null;
	}   // getOldValue

	/**
	 *	Check if {@link #m_rowChanged} needs to be saved.
	 *  @param  onlyRealChange if true the value of a field was actually changed
	 *  (e.g. for new records, which have not been changed) - default false
	 *	@return true if needs to be saved
	 */
	public boolean needSave(boolean onlyRealChange)
	{
		return needSave(m_rowChanged, onlyRealChange);
	}   //  needSave

	/**
	 *	Check if {@link #m_rowChanged} needs to be saved.
	 *	@return true if needs to be saved
	 */
	public boolean needSave()
	{
		return needSave(m_rowChanged, false);
	}   //  needSave

	/**
	 *	Check if newRow needs to be saved.
	 *	@param	newRow to check
	 *	@return true if needs to be saved
	 */
	public boolean needSave(int newRow)
	{
		return needSave(newRow, false);
	}   //  needSave

	/**
	 *	Check if the row needs to be saved.
	 *  - only when row changed
	 *  - only if nothing was changed
	 *	@param	newRow to check
	 *  @param  onlyRealChange if true, only if the value of a field was actually changed
	 *  (e.g. for new record with default value, which have not been changed) - default false
	 *	@return true it needs to be saved
	 */
	public boolean needSave(int newRow, boolean onlyRealChange)
	{
		if (log.isLoggable(Level.FINE))
			log.fine("Row=" + newRow +
					", Changed=" + m_rowChanged + "/" + m_changed);  //  m_rowChanged set in setValueAt
		//  nothing done
		if (!m_changed && m_rowChanged == -1)
			return false;
		//  E.g. New unchanged records
		if (m_changed && m_rowChanged == -1 && onlyRealChange)
			return false;
		//  same row
		if (newRow == m_rowChanged)
			return false;

		return true;
	}	//	needSave

	/** Save OK - O		*/
	public static final char	SAVE_OK = 'O';			//	the only OK condition
	/** Save Error - E	*/
	public static final char	SAVE_ERROR = 'E';
	/** Save Access Error - A	*/
	public static final char	SAVE_ACCESS = 'A';
	/** Save Mandatory Error - M	*/
	public static final char	SAVE_MANDATORY = 'M';
	/** Save Abort Error - U	*/
	public static final char	SAVE_ABORT = 'U';

	/**
	 *	Check if it needs to be saved and save it.
	 *  @param newRow row index
	 *  @param manualCmd true if initiated from user action
	 *	@return true if not needed to be saved or successful saved
	 */
	public boolean dataSave (int newRow, boolean manualCmd)
	{
		if (log.isLoggable(Level.FINE)) log.fine("Row=" + newRow +
			", Changed=" + m_rowChanged + "/" + m_changed);  //  m_rowChanged set in setValueAt
		//  nothing done
		if (!m_changed && m_rowChanged == -1)
			return true;
		//  same row, don't save yet
		if (newRow == m_rowChanged)
			return true;

		return (dataSave(manualCmd) == SAVE_OK);
	}   //  dataSave

	/**
	 *	Save changes.
	 *  @param manualCmd if true (i.e initiated from user action), no vetoable PropertyChange will be fired for save confirmation
	 *	@return OK Or Error condition
	 *  Error info (Access*, FillMandatory, SaveErrorNotUnique,
	 *  SaveErrorRowNotFound, SaveErrorDataChanged) is saved in the log
	 */
	public char dataSave (boolean manualCmd)
	{
		//	cannot save
		if (!m_open)
		{
			log.warning ("Error - Open=" + m_open);
			return SAVE_ERROR;
		}
		//	no need - not changed - row not positioned - no Value changed
		if (m_rowChanged == -1)
		{
			if (log.isLoggable(Level.CONFIG)) log.config("NoNeed - Changed=" + m_changed + ", Row=" + m_rowChanged);
			if (!manualCmd)
				return SAVE_OK;
		}
		//  Value not changed
		if (m_rowData == null)
		{
			//reset out of sync variable
			m_rowChanged = -1;
			if (log.isLoggable(Level.FINE))
				log.fine("No Changes");
			return SAVE_ERROR;
		}

		if (m_readOnly)
		//	If Processed - not editable (Find always editable)  -> ok for changing payment terms, etc.
		{
			if (log.isLoggable(Level.WARNING))
				log.warning("IsReadOnly - ignored");
			dataIgnore();
			return SAVE_ACCESS;
		}

		//	row not positioned - no Value changed
		if (m_rowChanged == -1)
		{
			if (m_newRow != -1)     //  new row and nothing changed - might be OK
				m_rowChanged = m_newRow;
			else
			{
				fireDataStatusEEvent("SaveErrorNoChange", "", true);
				return SAVE_ERROR;
			}
		}

		//	Can we change?
		int[] co = getClientOrg(m_rowChanged);
		int AD_Client_ID = co[0]; 
		int AD_Org_ID = co[1];
		if (!MRole.getDefault(m_ctx, false).canUpdate(AD_Client_ID, AD_Org_ID, m_AD_Table_ID, 0, true))
		{
			fireDataStatusEEvent(CLogger.retrieveError());
			dataIgnore();
			return SAVE_ACCESS;
		}

		if (log.isLoggable(Level.INFO)) log.info("Row=" + m_rowChanged);

		//  inform about data save action, if not manually initiated
		try
		{
			if (!manualCmd)
				m_vetoableChangeSupport.fireVetoableChange(PROPERTY, -1, m_rowChanged);
		}
		catch (PropertyVetoException pve)
		{
			log.warning(pve.getMessage());
			return SAVE_ABORT;
		}

		//	get updated row data
		Object[] rowData = getDataAtRow(m_rowChanged);

		//	Check Mandatory
		String missingColumns = getMandatory(rowData);
		if (missingColumns.length() != 0)
		{
			fireDataStatusEEvent("FillMandatory", missingColumns + "\n", true);
			return SAVE_MANDATORY;
		}

		/**
		 *	Update row *****
		 */
		int Record_ID = 0;
		if (!m_inserting)
			Record_ID = getKeyID(m_rowChanged);
		try
		{
			return dataSavePO (Record_ID);
		}
		catch (Throwable e)
		{
			if (e instanceof ClassNotFoundException)
				log.warning(m_tableName + " - " + e.getLocalizedMessage());
			else
			{
				log.log(Level.SEVERE, "Persistency Issue - " 
					+ m_tableName + ": " + e.getLocalizedMessage(), e);
				log.saveError("Error", e.getLocalizedMessage());
			}
		}
		return SAVE_ERROR;
	}	//	dataSave

	/**
	 * 	Save via PO
	 *	@param Record_ID
	 *	@return SAVE_ERROR or SAVE_OK
	 *	@throws Exception
	 */
	private char dataSavePO (int Record_ID) throws Exception
	{
		if (log.isLoggable(Level.FINE)) log.fine("ID=" + Record_ID);
		//
		Object[] rowData = getDataAtRow(m_rowChanged);
		//
		MTable table = MTable.get (m_ctx, m_AD_Table_ID);
		PO po = null;
		if (! m_importing) // Just use trx when importing
			m_trxName = null;
		if (Record_ID != -1)
		{
			if (Record_ID == 0 && !m_inserting && MTable.isZeroIDTable(table.getTableName())) {
				String uuidFromZeroID = table.getUUIDFromZeroID();
				po = table.getPOByUU(uuidFromZeroID, m_trxName);
			} else {
				po = table.getPO(Record_ID, m_trxName);
			}
		}
		else	//	Multi - Key
			po = table.getPO(getWhereClause(rowData), m_trxName);
		//	No Persistent Object
		if (po == null)
			throw new ClassNotFoundException ("No Persistent Object");
		
		if (!po.is_new())
		{
			if (hasChanged(po))
			{				
				// return error stating that current record has changed and it cannot be saved
				String adMessage = "CurrentRecordModified";
				String msg = Msg.getMsg(Env.getCtx(), adMessage);
				fireDataStatusEEvent(adMessage, msg, true);
				return SAVE_ERROR;
			}
		}
		
		int size = m_fields.size();
		for (int col = 0; col < size; col++)
		{
			GridField field = (GridField)m_fields.get (col);
			if (field.isVirtualColumn())
				continue;
			String columnName = field.getColumnName ();
			Object value = rowData[col];
			Object oldValue = m_rowData[col];
			//	RowID
			if (field.getDisplayType() == DisplayType.RowID)
				; 	//	ignore

			//	Nothing changed & null
			else if (oldValue == null && value == null)
				;	//	ignore
			
			//	***	Data changed ***
			else if (m_inserting || isValueChanged(oldValue, value) )
			{
				//	Check existence
				int poIndex = po.get_ColumnIndex(columnName);
				if (poIndex < 0)
				{
					//	Custom Fields not in PO
					po.set_CustomColumn(columnName, value);
				//	log.log(Level.SEVERE, "Column not found: " + columnName);
					continue;
				}
				
				Object dbValue = po.get_Value(poIndex);
				if (m_inserting 
					|| !m_compareDB
					//	Original == DB
					|| (oldValue == null && dbValue == null)
					|| (oldValue != null && oldValue.equals (dbValue))
					//	Target == DB (changed by trigger to new value already)
					|| (value == null && dbValue == null)
					|| (value != null && value.equals (dbValue)) 
					|| ((oldValue != null && dbValue != null && oldValue.getClass().equals(byte[].class) && dbValue.getClass().equals(byte[].class)) && Arrays.equals((byte[])oldValue, (byte[])dbValue))
					|| ((value != null && dbValue != null && value.getClass().equals(byte[].class) && dbValue.getClass().equals(byte[].class)) && Arrays.equals((byte[])oldValue, (byte[])dbValue)) 
						)
				{
					if (!po.set_ValueNoCheck (columnName, value))
					{
						ValueNamePair lastError = CLogger.retrieveError();
						if (lastError != null) {
							String adMessage = lastError.getValue();
							String adMessageArgument = lastError.getName().trim();
							
							StringBuilder info = new StringBuilder(adMessageArgument);
							
							if (!adMessageArgument.endsWith(";")) info.append(";");
							info.append(field.getHeader());
							
							fireDataStatusEEvent(adMessage, info.toString(), true);
						} else {
							fireDataStatusEEvent("Set value failed", field.getHeader(), true);
						}
						return SAVE_ERROR;
					}
				}
				//	Original != DB
				else
				{
					String msg = columnName 
						+ "= " + oldValue 
							+ (oldValue==null ? "" : "(" + oldValue.getClass().getName() + ")")
						+ " != DB: " + dbValue 
							+ (dbValue==null ? "" : "(" + dbValue.getClass().getName() + ")")
						+ " -> New: " + value 
							+ (value==null ? "" : "(" + value.getClass().getName() + ")");

					dataRefresh(m_rowChanged);
					fireDataStatusEEvent("SaveErrorDataChanged", msg, true);
					return SAVE_ERROR;
				}
			}	//	Data changed

		}	//	for every column

		if (!po.save())
		{
			String msg = "SaveError";
			String info = "";
			ValueNamePair ppE = CLogger.retrieveError();
			if (ppE != null)
			{
				msg = ppE.getValue();
				info = ppE.getName();
				if ("DBExecuteError".equals(msg))
					info = "DBExecuteError:" + info;
			}
			fireDataStatusEEvent(msg, info, true);
			return SAVE_ERROR;
		}
		else if (m_virtual && po.get_ID() > 0)
		{
			//update ID
			MSort sort = m_sort.get(m_rowChanged);
			int oldid = sort.index;
			if (oldid != po.get_ID())
			{
				sort.index = po.get_ID();
				Object[] data = m_virtualBuffer.remove(oldid);
				data[m_indexKeyColumn] = sort.index;
				m_virtualBuffer.put(sort.index, data);
			}
		}
		
		//	Refresh - update buffer
		String whereClause = po.get_WhereClause(true);
		if (log.isLoggable(Level.FINE)) log.fine("Reading ... " + whereClause);
		StringBuilder refreshSQL = new StringBuilder(m_SQL_Select)
			.append(" WHERE ").append(whereClause);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(refreshSQL.toString(), get_TrxName());
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				Object[] rowDataDB = readData(rs);
				//	update buffer
				setDataAtRow(m_rowChanged, rowDataDB);
				fireTableRowsUpdated(m_rowChanged, m_rowChanged);
			}
		}
		catch (SQLException e)
		{
			String msg = "SaveError";
			log.log(Level.SEVERE, refreshSQL.toString(), e);
			fireDataStatusEEvent(msg, e.getLocalizedMessage(), true);
			return SAVE_ERROR;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	everything ok
		m_rowData = null;
		m_changed = false;
		m_compareDB = true;
		m_rowChanged = -1;
		m_newRow = -1;
		m_inserting = false;
		//
		ValueNamePair pp = CLogger.retrieveWarning();
		if (pp != null)
		{
			String msg = pp.getValue();
			String info = pp.getName();
			fireDataStatusEEvent(msg, info, false);
		}
		else
		{
			pp = CLogger.retrieveInfo();
			String msg = "Saved";
			String info = "";
			if (pp != null)
			{
				msg = pp.getValue();
				info = pp.getName();
			}
			fireDataStatusIEvent(msg, info);
		}
		//
		log.config("fini");
		return SAVE_OK;
	}	//	dataSavePO
	
	/**
	 * 	Get Record Where Clause from data (single key or multi-key)
	 *	@param rowData data
	 *	@return where clause or null
	 */
	private String getWhereClause (Object[] rowData)
	{
		int size = m_fields.size();
		StringBuilder singleRowWHERE = null;
		StringBuilder singleRowUUWHERE = null;
		StringBuilder multiRowWHERE = null;
		String tableName = getTableName();
		int uidColumn = -1;
		for (int col = 0; col < size; col++)
		{
			GridField field = (GridField)m_fields.get (col);
			String columnName = field.getColumnName();
			if (field.isKey())
			{
				Object value = rowData[col]; 
				if (value == null)
				{
					log.log(Level.WARNING, "PK data is null - " + columnName);
					return null;
				}
				if (columnName.endsWith ("_ID"))
					singleRowWHERE = new StringBuilder(tableName).append(".").append(columnName)
						.append ("=").append (value);
				else
					singleRowWHERE = new StringBuilder(tableName).append(".").append(columnName)
						.append ("=").append (DB.TO_STRING(value.toString()));
				break;
			}
			else if (field.isParentColumn())
			{
				Object value = rowData[col]; 
				if (value == null)
				{
					if (log.isLoggable(Level.INFO))log.log(Level.INFO, "FK data is null - " + columnName);
					continue;
				}
				if (multiRowWHERE == null)
					multiRowWHERE = new StringBuilder();
				else
					multiRowWHERE.append(" AND ");
				if (columnName.endsWith ("_ID"))
					multiRowWHERE.append (tableName).append(".").append(columnName)
						.append ("=").append (value);
				else if (value instanceof Timestamp) {
					multiRowWHERE.append (tableName).append(".").append(columnName)
					.append ("=").append (DB.TO_DATE((Timestamp)value, false));
				}else
					multiRowWHERE.append (tableName).append(".").append(columnName)
						.append ("=").append (DB.TO_STRING(value.toString()));
			}
			else if (columnName.equals(PO.getUUIDColumnName(tableName)))
			{
				uidColumn = col;
			}
		}	//	for all columns
						
		if (singleRowWHERE != null)
			return singleRowWHERE.toString();

		if (multiRowWHERE != null)
			return multiRowWHERE.toString();

		if (uidColumn >= 0)
		{
			Object value = rowData[uidColumn]; 
			if (value == null && multiRowWHERE == null)
			{
				log.log(Level.WARNING, "UUID data is null - " + uidColumn);
				return null;
			}
			else
			{
				singleRowUUWHERE = new StringBuilder(tableName).append(".").append(PO.getUUIDColumnName(tableName))
						.append ("=").append (DB.TO_STRING(value.toString()));
			}
		}
		if (singleRowUUWHERE != null)
			return singleRowUUWHERE.toString();

		log.log(Level.WARNING, "No key Found");
		return null;
	}	//	getWhereClause
	
	/**
	 *	Get Mandatory empty columns
	 *  @param rowData row data
	 *  @return Mandatory columns that's empty (separated by comma)
	 */
	private String getMandatory(Object[] rowData)
	{
		//  see also => ProcessParameter.saveParameter
		StringBuilder sb = new StringBuilder();

		//	Check all columns
		int size = m_fields.size();
		for (int i = 0; i < size; i++)
		{
			GridField field = (GridField)m_fields.get(i);
			if (field.isMandatory(true))        //  check context
			{
				if (rowData[i] == null || rowData[i].toString().length() == 0)
				{
					field.setInserting (true);  //  set editable otherwise deadlock
					field.setError(true);
					if (sb.length() > 0)
						sb.append(", ");
					sb.append(field.getHeader());
				}
				else
					field.setError(false);
			}
		}

		if (sb.length() == 0)
			return "";
		return sb.toString();
	}	//	getMandatory

	/**
	 * @return true if need save and all mandatory field has value
	 */
	public boolean isNeedSaveAndMandatoryFill() {
		if (!m_open)
		{
			return false;
		}
		//	no need - not changed - row not positioned - no Value changed
		if (m_rowChanged == -1)
		{
			return false;
		}
		//  Value not changed
		if (m_rowData == null)
		{
			return false;
		}

		if (m_readOnly)
		{
			return false;
		}

		//	row not positioned - no Value changed
		if (m_rowChanged == -1)
		{
			if (m_newRow != -1)     //  new row and nothing changed - might be OK
				m_rowChanged = m_newRow;
			else
			{
				return false;
			}
		}
		
		//	get updated row data
		Object[] rowData = getDataAtRow(m_rowChanged);

		//	Check Mandatory
		String missingColumns = getMandatory(rowData);
		if (missingColumns.length() != 0) {
			return false;
		}
		
		return true;
	}
	
	// IDEMPIERE-454 Easy import
	private boolean m_importing = false;
	private String m_trxName = null;

	private int m_currentRow = -1;

	/**
	 *	Append new row after current row
	 *  @param currentRow row
	 *  @param copyCurrent true to copy value from current row
	 *  @return true if success -
	 *  Error info (Access*, AccessCannotInsert) is saved in the log
	 */
	public boolean dataNew (int currentRow, boolean copyCurrent)
	{
		if (log.isLoggable(Level.INFO)) log.info("Current=" + currentRow + ", Copy=" + copyCurrent);
		//  Read only
		if (m_readOnly)
		{
			fireDataStatusEEvent("AccessCannotInsert", "", true);
			return false;
		}

		//  see if we need to save
		dataSave(-2, false);

		m_inserting = true;
		
		// Setup the buffer first so that event will be handle properly
		// Create default data
		int size = m_fields.size();
		m_rowData = new Object[size];	//	"original" data
		Object[] rowData = new Object[size];
		
		m_changed = true;
		m_compareDB = true;		
		m_newRow = currentRow + 1;
		//  if there is no record, the current row could be 0 (and not -1)
		if (m_sort.size() < m_newRow)
			m_newRow = m_sort.size();

		//	add Data at end of buffer
		MSort newSort = m_virtual
				? new MSort(NEW_ROW_ID, null)
				: new MSort(m_sort.size(), null);	//	index
		if (m_virtual)
		{
			m_virtualBuffer.put(NEW_ROW_ID, rowData);
		}
		else
		{
			m_buffer.add(rowData);
		}
		//	add Sort pointer
		m_sort.add(m_newRow, newSort);
		m_rowCount++;
		
		//	fill data
		if (copyCurrent)
		{
			boolean hasDocTypeTargetField = (getField("C_DocTypeTarget_ID") != null);
			Object[] origData = getDataAtRow(currentRow);
			for (int i = 0; i < size; i++)
			{
				GridField field = (GridField)m_fields.get(i);
				MColumn column = null;
				if (field.getAD_Column_ID() > 0)
					column = MColumn.get(m_ctx, field.getAD_Column_ID());
				if (field.isVirtualColumn())
					;
				else if (   field.isKey()		//	KeyColumn
						  || (column != null && column.isUUIDColumn()) // IDEMPIERE-67
						  || (column != null && column.isStandardColumn() && !column.getColumnName().equals("AD_Org_ID")) // AD_Org_ID can be copied
						  // Bug [ 1807947 ]
						  || (hasDocTypeTargetField && field.getColumnName().equals("C_DocType_ID"))
						  || ! field.isAllowCopy())
				{
					Object value = field.getDefault();
					field.setValue(value, m_inserting);
					field.validateValueNoDirect();
					rowData[i] = field.getValue();
				}
				else {
					Object value = origData[i];
					field.setValue(value, m_inserting);
					field.validateValueNoDirect();
					rowData[i] = field.getValue();
				}
			}
		}
		else	//	new
		{
			for (int i = 0; i < size; i++)
			{
				GridField field = (GridField)m_fields.get(i);
				if (field.getGridTab() != null) {
					//avoid getting default from previous row
					String key = field.getVO().WindowNo+"|"+field.getVO().TabNo+"|"+field.getVO().ColumnName;
					field.getVO().ctx.remove(key);
				}
				Object value = field.getDefault();
				field.setValue(value, m_inserting);
				field.validateValueNoDirect();
				rowData[i] = field.getValue();
			}
		}
		
		m_rowChanged = -1;  //  only changed in setValueAt

		//	inform
		if (log.isLoggable(Level.FINE)) log.fine("Current=" + currentRow + ", New=" + m_newRow);
		fireTableRowsInserted(m_newRow, m_newRow);
		fireDataStatusIEvent(copyCurrent ? DATA_UPDATE_COPIED_MESSAGE : DATA_INSERTED_MESSAGE, "");
		if (log.isLoggable(Level.FINE)) log.fine("Current=" + currentRow + ", New=" + m_newRow + " - complete");
		return true;
	}	//	dataNew

	/**
	 *	Delete data at row index
	 *  @param row row index
	 *  @return true if success -
	 *  Error info (Access*, AccessNotDeleteable, DeleteErrorDependent,
	 *  DeleteError) is saved in the log
	 */
	public boolean dataDelete (int row)
	{
		if (log.isLoggable(Level.INFO)) log.info("Row=" + row);
		if (row < 0)
			return false;

		//	Tab R/O
		if (m_readOnly)
		{
			fireDataStatusEEvent("AccessCannotDelete", "", true);	//	privileges
			return false;
		}

		//	Is this record deletable?
		if (!m_deleteable)
		{
			fireDataStatusEEvent("AccessNotDeleteable", "", true);	//	audit
			return false;
		}

		//	Processed Column and not an Import Table
		if (m_indexProcessedColumn > 0 && !m_tableName.startsWith("I_"))
		{
			Boolean processed = (Boolean)getValueAt(row, m_indexProcessedColumn);
			if (processed != null && processed.booleanValue())
			{
				fireDataStatusEEvent("CannotDeleteTrx", "", true);
				return false;
			}
		}
		// Carlos Ruiz - globalqss - IDEMPIERE-111
		// Check if the role has access to this client
		//	Can we change?
		int[] co = getClientOrg(row);
		int AD_Client_ID = co[0];
		int AD_Org_ID = co[1];
		if (!MRole.getDefault(m_ctx, false).canUpdate(AD_Client_ID, AD_Org_ID, m_AD_Table_ID, 0, true))
		{
			fireDataStatusEEvent("AccessCannotDelete", "", true);
			return false;
		}

		MSort sort = (MSort)m_sort.get(row);
		Object[] rowData = getDataAtRow(row);
		//
		PO po = getPO(row);
		
		//	Delete via PO 
		if (po != null)
		{
			boolean ok = false;
			try
			{
				ok = po.delete(false);
			}
			catch (Throwable t)
			{
				log.log(Level.SEVERE, "Delete", t);
			}
			if (!ok)
			{
				ValueNamePair vp = CLogger.retrieveError();
				if (vp != null && !(Util.isEmpty(vp.getValue()) || Util.isEmpty(vp.getName())))
					fireDataStatusEEvent(vp);
				else
					fireDataStatusEEvent("DeleteError", "", true);
				return false;
			}
		}
		else	//	Delete via SQL
		{
			StringBuilder sql = new StringBuilder("DELETE FROM ");
			sql.append(m_tableName).append(" WHERE ").append(getWhereClause(rowData));
			int no = 0;
			PreparedStatement pstmt = null;
			try
			{
				pstmt = DB.prepareStatement (sql.toString(), 
						ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, null);
				no = pstmt.executeUpdate();
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, sql.toString(), e);
				String msg = "DeleteError";
				String dbMsg = DBException.getDefaultDBExceptionMessage(e);
				if (!Util.isEmpty(dbMsg))
					msg = dbMsg;
				fireDataStatusEEvent(msg, e.getLocalizedMessage(), true);
				return false;
			}
			finally
			{
				DB.close(pstmt);
				pstmt = null;
			}
			//	Check Result
			if (no != 1)
			{
				log.log(Level.SEVERE, "Number of deleted rows = " + no);
				return false;
			}
		}

		//	Get Sort
		if (m_virtual)
		{
			m_virtualBuffer.remove(sort.index);
		}
		else
		{
			//	Delete row in Buffer and shifts all below up
			m_buffer.remove(sort.index);
		}
		m_rowCount--;

		//	Delete row in Sort
		m_sort.remove(row);
		if (!m_virtual)
		{
			//	Correct pointer in Sort
			for (int i = 0; i < m_sort.size(); i++)
			{
				MSort ptr = (MSort)m_sort.get(i);
				if (ptr.index > sort.index)
					ptr.index--;	//	move up
			}
		}

		//	inform
		m_changed = false;
		m_rowChanged = -1;
		fireTableRowsDeleted(row, row);
		fireDataStatusIEvent("Deleted", "");
		if (log.isLoggable(Level.FINE)) log.fine("Row=" + row + " complete");
		return true;
	}	//	dataDelete
	
	/**
	 *	Ignore/Undo changes
	 */
	public void dataIgnore()
	{
		if (!m_inserting && !m_changed && m_rowChanged < 0)
		{
			if (log.isLoggable(Level.FINE))
				log.fine("Nothing to ignore");
			m_newRow = -1;
			return;
		}
		if (log.isLoggable(Level.INFO)) log.info("Inserting=" + m_inserting);

		//	Inserting - delete new row
		if (m_inserting)
		{
			//	Get Sort
			MSort sort = (MSort)m_sort.get(m_newRow);
			if (m_virtual)
			{
				m_virtualBuffer.remove(NEW_ROW_ID);
			}
			else
			{
				//	Delete row in Buffer and shifts all below up
				m_buffer.remove(sort.index);
			}
			m_rowCount--;
			//	Delete row in Sort
			m_sort.remove(m_newRow);	//	pintint to the last column, so no adjustment
			//
			m_changed = false;
			m_rowData = null;
			m_rowChanged = -1;
			m_inserting = false;
			//	inform
			fireTableRowsDeleted(m_newRow, m_newRow);
		}
		else
		{
			//	update buffer
			if (m_rowData != null)
			{
				setDataAtRow(m_rowChanged, m_rowData);
			}
			m_changed = false;
			m_rowData = null;
			m_rowChanged = -1;
			m_inserting = false;
			//	inform
		//	fireTableRowsUpdated(m_rowChanged, m_rowChanged); >> messes up display?? (clearSelection)
		}
		m_newRow = -1;
		fireDataStatusIEvent(DATA_IGNORED_MESSAGE, "");
	}	//	dataIgnore

	/**
	 *	Refresh Row - ignore changes
	 *  @param row row
	 */
	public void dataRefresh (int row)
	{
		dataRefresh(row, true);
	}

	/**
	 * get where clause for row
	 * @param row
	 * @return where clause
	 */
	public String getWhereClause(int row)
	{
		if (row < 0 || m_sort.size() == 0 || m_inserting)
			return null;

		Object[] rowData = getDataAtRow(row);
		if (rowData == null)
			return null;

		String where = getWhereClause(rowData);

		return where;
	}

	/**
	 *	Refresh Row - ignore changes
	 *  @param row row
	 *  @param fireStatusEvent
	 */
	public void dataRefresh (int row, boolean fireStatusEvent)
	{
		if (log.isLoggable(Level.INFO)) log.info("Row=" + row);

		if (row < 0 || m_sort.size() == 0 || m_inserting)
			return;

		Object[] rowData = getDataAtRow(row);

		//  ignore
		dataIgnore();

		//	Create SQL
		String where = getWhereClause(rowData);
		if (where == null || where.length() == 0)
			where = "1=2";
		String sql = m_SQL_Select + " WHERE " + where;
		Object[] rowDataDB = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql,get_TrxName());
			rs = pstmt.executeQuery();
			//	only one row
			if (rs.next())
				rowDataDB = readData(rs);
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
			fireTableRowsUpdated(row, row);
			fireDataStatusEEvent("RefreshError", sql, true);
			return;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	update buffer
		if (rowDataDB!=null)
			setDataAtRow(row, rowDataDB);
		//	info
		m_rowData = null;
		m_changed = false;
		m_rowChanged = -1;
		m_inserting = false;
		fireTableRowsUpdated(row, row);
		if (fireStatusEvent)
			fireDataStatusIEvent(DATA_REFRESH_MESSAGE, "");
	}	//	dataRefresh


	/**
	 *	Refresh all Rows - ignore changes
	 */
	public void dataRefreshAll()
	{
		dataRefreshAll(true);
	}

	/**
	 *	Refresh all Rows - ignore changes
	 *  @param fireStatusEvent
	 */
	public void dataRefreshAll(boolean fireStatusEvent)
	{
		dataRefreshAll(fireStatusEvent, -1);
	}

	/**
	 *	Refresh all Rows - ignore changes
	 *  @param fireStatusEvent
	 *  @param rowToRetained
	 */
	public void dataRefreshAll(boolean fireStatusEvent, int rowToRetained)
	{
		m_inserting = false;	//	should not happen
		dataIgnore();
		String retainedWhere = null;
		if (rowToRetained >= 0)
		{
			retainedWhere = getWhereClause(rowToRetained);
		}
		close(false);
		if (retainedWhere != null)
		{
			if (m_whereClause != null && m_whereClause.trim().length() > 0)
			{
				StringBuilder orRetainedWhere = new StringBuilder(") OR (").append(retainedWhere).append(")) ");
				if (! m_whereClause.contains(orRetainedWhere.toString()))
					m_whereClause = "((" + m_whereClause + orRetainedWhere.toString();
			}
			open(m_maxRows);
		}
		else
		{
			open(m_maxRows);
		}
		//	Info
		m_rowData = null;
		m_changed = false;
		m_rowChanged = -1;
		m_inserting = false;
		if (m_lastSortColumnIndex >= 0)
		{
			loadComplete();
			sort(m_lastSortColumnIndex, m_lastSortedAscending);
		}
		fireTableDataChanged();
		if (fireStatusEvent)
			fireDataStatusIEvent(DATA_REFRESH_MESSAGE, "");
	}	//	dataRefreshAll


	/**
	 *	Re-query with new whereClause
	 *  @param whereClause sql where clause
	 *  @param onlyCurrentRows only current rows
	 *  @param onlyCurrentDays how many days back
	 *  @param fireEvents if tabledatachanged and datastatusievent must be fired
	 *  @return true if success
	 */
	public boolean dataRequery (String whereClause, boolean onlyCurrentRows, int onlyCurrentDays, boolean fireEvents)
	{
		if (log.isLoggable(Level.INFO)) log.info(whereClause + "; OnlyCurrent=" + onlyCurrentRows);
		close(false);
		m_onlyCurrentDays = onlyCurrentDays;
		setSelectWhereClause(whereClause, onlyCurrentRows, m_onlyCurrentDays);
		open(m_maxRows);
		//  Info
		m_rowData = null;
		m_changed = false;
		m_rowChanged = -1;
		m_inserting = false;
		if (m_lastSortColumnIndex >= 0)
		{
			loadComplete();
			sort(m_lastSortColumnIndex, m_lastSortedAscending);
		}
		if (fireEvents) {
			fireTableDataChanged();
			fireDataStatusIEvent(DATA_REFRESH_MESSAGE, "");
		}
		return true;
	}	//	dataRequery

	/**
	 * Delegate to {@link #dataRequery(String, boolean, int, boolean)} with fireEvents=true
	 * @param whereClause
	 * @param onlyCurrentRows
	 * @param onlyCurrentDays
	 * @return true if success
	 */
	public boolean dataRequery (String whereClause, boolean onlyCurrentRows, int onlyCurrentDays)
	{
		return dataRequery (whereClause, onlyCurrentRows, onlyCurrentDays, true);
	}	//	dataRequery

	/**
	 *	Is Cell Editable.
	 *  @param  row row index
	 *  @param  col column index
	 *  @return true, if editable
	 */
	public boolean isCellEditable (int row, int col)
	{
		//	Entire Table not editable
		if (m_readOnly)
			return false;
		//	Key not editable
		if (col == m_indexKeyColumn)
			return false;
		/** @todo check link columns */

		//	Check column range
		if (col < 0 && col >= m_fields.size())
			return false;
		//  IsActive Column always editable if no processed exists
		if (col == m_indexActiveColumn && m_indexProcessedColumn == -1)
			return true;
		//	Row
		if (!isRowEditable(row))
			return false;

		//	Column
		return ((GridField)m_fields.get(col)).isEditable(false);
	}	//	IsCellEditable

	/**
	 *	Is row editable
	 *  @param row row index
	 *  @return true if editable
	 */
	public boolean isRowEditable (int row)
	{
		//	Entire Table not editable or no row
		if (m_readOnly || row < 0)
			return false;
		//	If not Active - not editable
		if (m_indexActiveColumn > 0)		//	&& m_TabNo != Find.s_TabNo)
		{
			Object value = getValueAt(row, m_indexActiveColumn);
			if (value instanceof Boolean)
			{
				if (!((Boolean)value).booleanValue())
					return false;
			}
			else if ("N".equals(value)) 
				return false;
		}
		//	If Processed - not editable (Find always editable)
		if (m_indexProcessedColumn > 0)		//	&& m_TabNo != Find.s_TabNo)
		{
			Object processed = getValueAt(row, m_indexProcessedColumn);
			if (processed instanceof Boolean)
			{
				if (((Boolean)processed).booleanValue())
					return false;
			}
			else if ("Y".equals(processed)) 
				return false;
		}
		//
		int[] co = getClientOrg(row);
		int AD_Client_ID = co[0]; 
		int AD_Org_ID = co[1];
		int Record_ID = getKeyID(row);
		return MRole.getDefault(m_ctx, false).canUpdate
			(AD_Client_ID, AD_Org_ID, m_AD_Table_ID, Record_ID, false);
	}	//	isRowEditable

	/**
	 * 	Get Client Org for row
	 *	@param row row index
	 *	@return array [0] = Client [1] = Org - a value of -1 is not defined/found
	 */
	private int[] getClientOrg (int row)
	{
		int AD_Client_ID = -1;
		if (m_indexClientColumn != -1)
		{
			Integer ii = (Integer)getValueAt(row, m_indexClientColumn);
			if (ii != null)
				AD_Client_ID = ii.intValue();
		}
		int AD_Org_ID = 0;
		if (m_indexOrgColumn != -1)
		{
			Integer ii = (Integer)getValueAt(row, m_indexOrgColumn);
			if (ii != null)
				AD_Org_ID = ii.intValue();
		}
		return new int[] {AD_Client_ID, AD_Org_ID};
	}	//	getClientOrg

	/**
	 *	Set entire table as read only
	 *  @param value new read only value
	 */
	public void setReadOnly (boolean value)
	{
		if (log.isLoggable(Level.FINE)) log.fine("ReadOnly=" + value);
		m_readOnly = value;
	}	//	setReadOnly

	/**
	 *  Is entire Table Read/Only
	 *  @return true if read only
	 */
	public boolean isReadOnly()
	{
		return m_readOnly;
	}   //  isReadOnly

	/**
	 *  Is inserting
	 *  @return true if inserting
	 */
	public boolean isInserting()
	{
		return m_inserting;
	}   //  isInserting

	/**
	 *	Set Compare DB.
	 * 	If Set to false, save overwrites the record, regardless of DB changes.
	 *  (When a payment is changed in Sales Order, the payment reversal clears the payment id)
	 * 	@param compareDB compare DB - false forces overwrite
	 *  @deprecated
	 */
	@Deprecated
	public void setCompareDB (boolean compareDB)
	{
		m_compareDB = compareDB;
	}  	//	setCompareDB

	/**
	 *	Get Compare DB.
	 * 	@return false if save overwrites the record, regardless of DB changes
	 * 	(false forces overwrite).
	 *  @deprecated
	 */
	@Deprecated
	public boolean getCompareDB ()
	{
		return m_compareDB;
	}  	//	getCompareDB

	/**
	 *	Can Table rows be deleted
	 *  @param value new deleteable value
	 */
	public void setDeleteable (boolean value)
	{
		if (log.isLoggable(Level.FINE)) log.fine("Deleteable=" + value);
		m_deleteable = value;
	}	//	setDeleteable
	
	/**
	 *	Read data from result set
	 *  @param rs result set
	 *  @return Data Array
	 */
	private Object[] readData (ResultSet rs)
	{
		int size = m_fields.size();
		Object[] rowData = new Object[size];
		String columnName = null;
		int displayType = 0;

		//	Types see also GridField.createDefault
		try
		{
			//	get row data
			for (int j = 0; j < size; j++)
			{
				//	Column Info
				GridField field = (GridField)m_fields.get(j);
				columnName = field.getColumnName();
				displayType = field.getDisplayType();
				//	Integer, ID, Lookup
				if (displayType == DisplayType.Integer || (DisplayType.isID(displayType) && !(columnName.equals("EntityType") || columnName.equals("AD_Language"))))
				{
					rowData[j] = Integer.valueOf(rs.getInt(j+1));	//	Integer
					if (rs.wasNull())
						rowData[j] = null;
				}
				//	Number
				else if (DisplayType.isNumeric(displayType))
					rowData[j] = rs.getBigDecimal(j+1);			//	BigDecimal
				//	Date
				else if (DisplayType.isDate(displayType))
					rowData[j] = rs.getTimestamp(j+1);			//	Timestamp
				//	RowID or Key (and Selection)
				else if (displayType == DisplayType.RowID)
					rowData[j] = null;
				//	YesNo
				else if (displayType == DisplayType.YesNo)
				{
					String str = rs.getString(j+1);
					if (field.isEncryptedColumn())
						str = (String)decrypt(str, getAD_Client_ID(rs));
					rowData[j] = Boolean.valueOf("Y".equals(str));	//	Boolean
				}
				//	LOB
				else if (DisplayType.isLOB(displayType))
				{
					Object value = rs.getObject(j+1);
					if (rs.wasNull())
						rowData[j] = null;
					else if (value instanceof Clob) 
					{
						Clob lob = (Clob)value;
						long length = lob.length();
						rowData[j] = lob.getSubString(1, (int)length);
					}
					else if (value instanceof Blob)
					{
						Blob lob = (Blob)value;
						long length = lob.length();
						rowData[j] = lob.getBytes(1, (int)length);
					}
					else if (value instanceof String)
						rowData[j] = value;
					else if (value instanceof byte[])
						rowData[j] = value;
				}
				//	String
				else
					rowData[j] = rs.getString(j+1);				//	String
				//	Encrypted
				if (field.isEncryptedColumn() && displayType != DisplayType.YesNo)
					rowData[j] = decrypt(rowData[j], getAD_Client_ID(rs));
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, columnName + ", DT=" + displayType, e);
		}
		return rowData;
	}	//	readData

	/**
	 * 	Decrypt
	 *	@param yy encrypted data
	 *  @param AD_Client_ID
	 *	@return clear data
	 */
	private Object decrypt (Object yy, int AD_Client_ID)
	{
		if (yy == null)
			return null;
		return SecureEngine.decrypt(yy, AD_Client_ID);
	}	//	decrypt
	
	/**
	 * @param rs
	 * @return AD_Client_ID or -1
	 */
	private int getAD_Client_ID(ResultSet rs) {
		int AD_Client_ID = -1;
		try {
			AD_Client_ID = rs.getInt("AD_Client_ID");
			if (rs.wasNull())
				AD_Client_ID = -1;
		} catch (SQLException e) {
			AD_Client_ID = -1;
		}
		if (AD_Client_ID == -1)
			AD_Client_ID = getAD_Client_ID();
		return AD_Client_ID;
	}

	/**
	 * @return AD_Client_ID
	 */
	private int getAD_Client_ID() 
	{
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		GridField field = getField("AD_Client_ID");
		if (field != null && field.getValue() != null) {
			AD_Client_ID = ((Number)field.getValue()).intValue();
		}
		return AD_Client_ID;
	}
	
	/**
	 *	Remove Data Status Listener
	 *  @param l listener
	 */
	public synchronized void removeDataStatusListener(DataStatusListener l)
	{
		listenerList.remove(DataStatusListener.class, l);
	}	//	removeDataStatusListener

	/**
	 *	Add Data Status Listener
	 *  @param l listener
	 */
	public synchronized void addDataStatusListener(DataStatusListener l)
	{
		listenerList.add(DataStatusListener.class, l);
	}	//	addDataStatusListener

	/**
	 *	Fire data status changed event
	 *  @param e event
	 */
	private void fireDataStatusChanged (DataStatusEvent e)
	{
		DataStatusListener[] listeners = listenerList.getListeners(DataStatusListener.class);
        for (int i = 0; i < listeners.length; i++) 
        	listeners[i].dataStatusChanged(e);
	}	//	fireDataStatusChanged

	/**
	 *  Create new Data Status Event instance
	 *  @return new data status event instance
	 */
	private DataStatusEvent createDSE()
	{
		boolean changed = m_changed;
		if (m_rowChanged != -1)
			changed = true;
		DataStatusEvent dse = new DataStatusEvent(this, m_rowCount, changed,
			Env.isAutoCommit(m_ctx, m_WindowNo), m_inserting);
		dse.AD_Table_ID = m_AD_Table_ID;
		dse.Record_ID = null;
		return dse;
	}   //  createDSE

	/**
	 *  Create and fire Data Status Info Event
	 *  @param AD_Message message
	 *  @param info additional info
	 */
	protected void fireDataStatusIEvent (String AD_Message, String info)
	{
		DataStatusEvent e = createDSE();
		e.setInfo(AD_Message, info, false,false);
		if (SORTED_DSE_EVENT.equals(AD_Message) && m_currentRow >= 0)
			e.setCurrentRow(m_currentRow);
		fireDataStatusChanged (e);
	}   //  fireDataStatusIEvent

	/**
	 *  Create and fire Data Status Error Event
	 *  @param AD_Message message
	 *  @param info info
	 *  @param isError error
	 */
	protected void fireDataStatusEEvent (String AD_Message, String info, boolean isError)
	{
		//
		DataStatusEvent e = createDSE();
		if (info != null && info.startsWith("DBExecuteError:")) {
			String firstline;
			int nl = info.indexOf("\n");
			if (nl > 0)
				firstline = info.substring(0, nl);
			else
				firstline = info;
			String newinfo = Msg.getMsg(m_ctx, firstline);
			if (firstline.equals(newinfo))
				newinfo = info.substring(15); // size of "DBExecuteError:"
			e.setInfo(AD_Message, newinfo, isError, !isError);
		} else {
			e.setInfo(AD_Message, info, isError, !isError);
		}
		if (isError)
			log.saveWarning(AD_Message, info);
		fireDataStatusChanged (e);
	}   //  fireDataStatusEEvent

	/**
	 *  Create and fire Data Status Error Event (from Error Log)
	 *  @param errorLog error log info
	 */
	protected void fireDataStatusEEvent (ValueNamePair errorLog)
	{
		if (errorLog != null)
			fireDataStatusEEvent (errorLog.getValue(), errorLog.getName(), true);
	}   //  fireDataStatusEEvent
	
	/**
	 *  Remove Vetoable change listener for row changes
	 *  @param l listener
	 */
	public synchronized void removeVetoableChangeListener(VetoableChangeListener l)
	{
		m_vetoableChangeSupport.removeVetoableChangeListener(l);
	}   //  removeVetoableChangeListener

	/**
	 *  Add Vetoable change listener for row changes
	 *  @param l listener
	 */
	public synchronized void addVetoableChangeListener(VetoableChangeListener l)
	{
		m_vetoableChangeSupport.addVetoableChangeListener(l);
	}   //  addVetoableChangeListener

	/**
	 *  Fire Vetoable change listener for row changes
	 *  @param e event
	 *  @throws PropertyVetoException
	 */
	protected void fireVetoableChange(PropertyChangeEvent e) throws java.beans.PropertyVetoException
	{
		m_vetoableChangeSupport.fireVetoableChange(e);
	}   //  fireVetoableChange

	/**
	 *  toString
	 *  @return String representation
	 */
	public String toString()
	{
		return new StringBuilder("MTable[").append(m_tableName)
			.append(",WindowNo=").append(m_WindowNo)
			.append(",Tab=").append(m_TabNo).append("]").toString();
	}   //  toString

	/**
	 * 
	 * @return new row added
	 */
	public int getNewRow()
	{
		return m_newRow;
	}
	
	/**
	 *	Asynchronous Loader
	 */
	protected class Loader implements Serializable, Runnable
	{
		/**
		 * generated serial id
		 */
		private static final long serialVersionUID = -6866671239509705988L;

		/**
		 *  Construct Loader
		 */
		public Loader()
		{
			super();
		}	//	Loader

		private PreparedStatement   m_pstmt = null;
		private ResultSet 		    m_rs = null;
		private Trx trx = null;
		private Properties m_context = null;
		private int maxRows;
		private int rows;
		
		public void setContext(Properties context)
		{
			m_context = context;
		}

		/**
		 *	Open ResultSet
		 *	@param maxRows maximum number of rows or 0 for all
		 *	@return number of records
		 */
		protected int open (int maxRows)
		{
			m_rowLoadTimeout = false;
			m_rowCountTimeout = false;
			this.maxRows = maxRows;

			//	Get Number of Rows
			rows = 0;
			PreparedStatement pstmt = null;
			ResultSet rs = null;		
			m_rowCountTimeout = false;
			try
			{
				pstmt = DB.prepareStatement(m_SQL_Count, get_TrxName());
				setParameter (pstmt, true);
		        int timeout = MSysConfig.getIntValue(MSysConfig.GRIDTABLE_INITIAL_COUNT_TIMEOUT_IN_SECONDS, 
		        		DEFAULT_GRIDTABLE_COUNT_TIMEOUT_IN_SECONDS, Env.getAD_Client_ID(Env.getCtx()));
				if (timeout > 0)
					pstmt.setQueryTimeout(timeout);
				rs = pstmt.executeQuery();
				if (rs.next())
					rows = rs.getInt(1);
			}
			catch (SQLException e0)
			{
				if (DB.getDatabase().isQueryTimeout(e0))
				{
					if (log.isLoggable(Level.INFO)) log.info("Count query timed out -> " + m_SQL_Count);
					m_rowCountTimeout = true;
					return 0;
				}
				else
					throw new DBException(e0);
			}
			finally
			{
				DB.close(rs, pstmt);				
			}
			StringBuilder info = new StringBuilder("Rows=");
			info.append(rows);
			if (rows == 0)
				info.append(" - ").append(m_SQL_Count);
				
			if (maxRows > 0 && rows > maxRows)
			{
				info.append(" - MaxRows=").append(maxRows);					
				rows = maxRows;
			}
					
			if (log.isLoggable(Level.FINE)) log.fine(info.toString());
			
			return rows;
		}	//	open

		private void openResultSet() {
			m_rowLoadTimeout = false;
			String trxName = get_TrxName();
			//postgresql need trx to use cursor based resultset
			//https://jdbc.postgresql.org/documentation/head/query.html#query-with-cursor
			if (trxName == null) {
				trxName = m_virtual ? Trx.createTrxName("Loader") : null;
				trx  = trxName != null ? Trx.get(trxName, true) : null;
				if (trx != null)
					trx.setDisplayName(getClass().getName()+"_openResultSet");
			}
			//	open Statement (closed by Loader.close)
			try
			{
				m_pstmt = DB.prepareStatement(m_SQL, trxName);
				//ensure not all rows are fetch into memory for virtual table
				if (m_virtual)
					m_pstmt.setFetchSize(100);
				setParameter (m_pstmt, false);
				int timeout = MSysConfig.getIntValue(MSysConfig.GRIDTABLE_LOAD_TIMEOUT_IN_SECONDS, DEFAULT_GRIDTABLE_LOAD_TIMEOUT_IN_SECONDS, Env.getAD_Client_ID(Env.getCtx()));
				if (timeout > 0)
					m_pstmt.setQueryTimeout(timeout);
				m_rs = m_pstmt.executeQuery();
			}
			catch (SQLException e)
			{
				if (DB.getDatabase().isQueryTimeout(e)) {
					m_rowLoadTimeout = true;
					throw new AdempiereException(Msg.getMsg(Env.getCtx(), LOAD_TIMEOUT_ERROR_MESSAGE), e);
				} else {
					log.severe(e.getLocalizedMessage() + " -> " + m_SQL);
					log.saveError(e.getLocalizedMessage(), e);
					throw new DBException(e);
				}
			}
		}

		/**
		 *	Close RS and Statement
		 */
		private void close()
		{
			DB.close(m_rs, m_pstmt);
			m_rs = null;
			m_pstmt = null;
			if (trx != null)
			{
				trx.close();
				trx = null;
			}
		}	//	close

		/**
		 *	Fill Buffer to include Row
		 */
		public void run()
		{
			try {
				if (m_context != null)
					ServerContext.setCurrentInstance(m_context);
				doRun();
			} finally {
				if (m_context != null)
					ServerContext.dispose();
			}
		}	//	run

		/**
		 * Fill buffer from result set
		 */
		private void doRun() {
			boolean isFindOverMax = false;
			boolean backgroundLoad = false;
			try
			{
				openResultSet();
				if (m_rs == null)
					return;

				while (m_rs.next())
				{
					if (maxRows > 0 && m_sort.size() == maxRows) {
						isFindOverMax = true;
			            break;
					}

					if (Thread.interrupted())
					{
						if (log.isLoggable(Level.FINE)) log.fine("Interrupted");
						close();
						return;
					}
					//  Get Data
					int recordId = 0;
					Object[] rowData = null;
					if (m_virtual)
						recordId = m_rs.getInt(getKeyColumnName());
					else
						rowData = readData(m_rs);
					//	add Data
					MSort sort = m_virtual
						? new MSort(recordId, null)
						: new MSort(m_buffer.size(), null);	//	index
					if (!m_virtual)
					{
						m_buffer.add(rowData);
					}
					m_sort.add(sort);

					// Start with rowCount=0, inform loading of first row
					if (m_rowCountTimeout)
					{
						m_rowCount++;
						if (m_rowCount == 1)
						{
							DataStatusEvent evt = createDSE();
							evt.setLoading(m_sort.size());
							evt.setInfo(COUNT_QUERY_TIMEOUT_ERROR_MESSAGE, null, false, false);
							fireDataStatusChanged(evt);
						}
					}

					//	Statement all 1000 rows & sleep
					if (m_sort.size() % 1000 == 0)
					{
						backgroundLoad = true;
						DataStatusEvent evt = createDSE();
						evt.setLoading(m_sort.size());
						fireDataStatusChanged(evt);
						//	give the other processes a chance
						try
						{
							Thread.yield();
							Thread.sleep(10);		//	.01 second
						}
						catch (InterruptedException ie)
						{
							if (log.isLoggable(Level.FINE)) log.fine("Interrupted while sleeping");
							close();
							return;
						}
					}
				}	//	while(rs.next())
			}
			catch (Exception e)
			{
				if (! DBException.isTimeout(e))
					throw new AdempiereException(e);
				if (log.isLoggable(Level.INFO)) log.info("Query timed out -> " + m_SQL);
			}
			finally
			{
				close();
			}
			
			// Background loading without initial rowCount, inform final loaded rows
			if (backgroundLoad && m_sort.size() > 0)
			{
				DataStatusEvent evt = createDSE();
				evt.setLoading(m_sort.size());
				if (isFindOverMax)
					evt.setInfo(FIND_OVER_MAX_ERROR_MESSAGE, " > " + m_sort.size(), false, true);
				else
					evt.setInfo(BACKGROUND_LOAD_FINISHED_INFO_MESSAGE, null, false, false);
				fireDataStatusChanged(evt);
			} else if (m_rowLoadTimeout)
			{
				DataStatusEvent evt = createDSE();
				evt.setLoading(m_sort.size());
				evt.setInfo(LOAD_TIMEOUT_ERROR_MESSAGE, null, false, true);
				fireDataStatusChanged(evt);
			} else if (isFindOverMax)
			{
				DataStatusEvent evt = createDSE();
				evt.setLoading(m_sort.size());
				evt.setInfo(FIND_OVER_MAX_ERROR_MESSAGE, " > " + m_sort.size(), false, true);
				fireDataStatusChanged(evt);
			}

			fireDataStatusIEvent("", "");
		}

		/**
		 *	Set Parameter for Query.
		 *		elements must be Integer, BigDecimal, String (default)
		 *  @param pstmt prepared statement
		 *  @param countSQL count
		 */
		private void setParameter (PreparedStatement pstmt, boolean countSQL)
		{
			if (m_parameterSELECT.size() == 0 && m_parameterWHERE.size() == 0)
				return;
			try
			{
				int pos = 1;	//	position in Statement
				//	Select Clause Parameters
				for (int i = 0; !countSQL && i < m_parameterSELECT.size(); i++)
				{
					Object para = m_parameterSELECT.get(i);
					if (para != null)
						if (log.isLoggable(Level.FINE)) log.fine("Select " + i + "=" + para);
					//
					if (para == null)
						;
					else if (para instanceof Integer)
					{
						Integer ii = (Integer)para;
						pstmt.setInt (pos++, ii.intValue());
					}
					else if (para instanceof BigDecimal)
						pstmt.setBigDecimal (pos++, (BigDecimal)para);
					else
						pstmt.setString(pos++, para.toString());
				}
				//	Where Clause Parameters
				for (int i = 0; i < m_parameterWHERE.size(); i++)
				{
					Object para = m_parameterWHERE.get(i);
					if (para != null)
						if (log.isLoggable(Level.FINE)) log.fine("Where " + i + "=" + para);
					//
					if (para == null)
						;
					else if (para instanceof Integer)
					{
						Integer ii = (Integer)para;
						pstmt.setInt (pos++, ii.intValue());
					}
					else if (para instanceof BigDecimal)
						pstmt.setBigDecimal (pos++, (BigDecimal)para);
					else
						pstmt.setString(pos++, para.toString());
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, "parameter", e);
			}
		}	//	setParameter

	}	//	Loader

	/**
	 * Feature Request [1707462]
	 * Enable runtime change of VFormat
	 * @param identifier column name
	 * @param strNewFormat new input mask
	 * author fer_luck
	 */
	protected void setFieldVFormat (String identifier, String strNewFormat)
	{
		int cols = m_fields.size();
		for (int i = 0; i < cols; i++)
		{
			GridField field = (GridField)m_fields.get(i);
			if (identifier.equalsIgnoreCase(field.getColumnName())){
				field.setVFormat(strNewFormat);
				m_fields.set(i, field);
				break;
			}
		}
	}	//	setFieldVFormat	

	/**
	 * Verify if the record at row has been changed at DB (by other user or process)
	 * @param row row index
	 * @return true if record at row has been changed at DB
	 */
	public boolean hasChanged(int row) {
		// not so aggressive (it can has still concurrency problems)
		// compare Updated, IsProcessed
		if (getKeyID(row) > 0) {
			int colUpdated = findColumn("Updated");
			int colProcessed = findColumn("Processed");
			
			boolean hasUpdated = (colUpdated >= 0);
			boolean hasProcessed = (colProcessed >= 0);
			
			String columns = null;
			if (hasUpdated && hasProcessed) {
				columns = new String("Updated, Processed");
			} else if (hasUpdated) {
				columns = new String("Updated");
			} else if (hasProcessed) {
				columns = new String("Processed");
			} else {
				// no columns updated or processed to compare
				return false;
			}
						
			// todo: temporary fix for carlos assumption that all windows have _id column
			if ( findColumn(m_tableName + "_ID") == -1)
				return false;

	    	Timestamp dbUpdated = null;
	    	String dbProcessedS = null;
	    	PreparedStatement pstmt = null;
	    	ResultSet rs = null;
	    	String sql = "SELECT " + columns + " FROM " + m_tableName + " WHERE " + m_tableName + "_ID=?";
	    	try
	    	{
	    		pstmt = DB.prepareStatement(sql, get_TrxName());
	    		pstmt.setInt(1, getKeyID(row));
	    		rs = pstmt.executeQuery();
	    		if (rs.next()) {
	    			int idx = 1;
	    			if (hasUpdated)
	    				dbUpdated = rs.getTimestamp(idx++);
	    			if (hasProcessed)
	    				dbProcessedS = rs.getString(idx++);
	    		}
	    		else
	    			if (log.isLoggable(Level.INFO)) log.info("No Value " + sql);
	    	}
	    	catch (SQLException e)
	    	{
	    		throw new DBException(e, sql);
	    	}
	    	finally
	    	{
	    		DB.close(rs, pstmt);
	    		rs = null; pstmt = null;
	    	}
	    	
	    	if (hasUpdated) {
				Timestamp memUpdated = null;
				memUpdated = (Timestamp) getOldValue(row, colUpdated);
				if (memUpdated == null)
					memUpdated = (Timestamp) getValueAt(row, colUpdated);

				if (memUpdated != null && ! memUpdated.equals(dbUpdated))
					return true;
	    	}
	    	
	    	if (hasProcessed) {
				Boolean memProcessed = null;
				memProcessed = (Boolean) getOldValue(row, colProcessed);
				if (memProcessed == null){
					if(getValueAt(row, colProcessed) instanceof Boolean )
					   memProcessed = (Boolean) getValueAt(row, colProcessed); 
					else if (getValueAt(row, colProcessed) instanceof String )
					   memProcessed = Boolean.valueOf((String)getValueAt(row, colProcessed)); 
				}
	    			
				Boolean dbProcessed = Boolean.TRUE;
				if (! dbProcessedS.equals("Y"))
					dbProcessed = Boolean.FALSE;
				if (memProcessed != null && ! memProcessed.equals(dbProcessed))
					return true;
	    	}
		}

		// @TODO: configurable aggressive - compare each column with the DB
		return false;
	}

	/**
	 * Verify if po has been changed at DB (by other user or process)
	 * @param po
	 * @return true if po has been changed at DB
	 */
	private boolean hasChanged(PO po) {
		if (m_rowChanged < 0)
			return false;
		
		// not so aggressive (it can has still concurrency problems)
		// compare Updated, IsProcessed
		int colUpdated = findColumn("Updated");
		int colProcessed = findColumn("Processed");
		
		boolean hasUpdated = colUpdated >= 0;
		boolean hasProcessed = colProcessed >= 0;

		if (!hasUpdated && !hasProcessed) {
			// no columns updated or processed to compare
			return false;
		}
				
    	Timestamp dbUpdated = (Timestamp) po.get_Value("Updated");
    	if (hasUpdated) {
			Timestamp memUpdated = null;
			memUpdated = (Timestamp) getOldValue(m_rowChanged, colUpdated);
			if (memUpdated == null)
				memUpdated = (Timestamp) getValueAt(m_rowChanged, colUpdated);

			if (memUpdated != null && ! memUpdated.equals(dbUpdated))
				return true;
    	}
    	
    	if (hasProcessed) {
			Boolean memProcessed = null;
			memProcessed = (Boolean) getOldValue(m_rowChanged, colProcessed);
			if (memProcessed == null){
				if(getValueAt(m_rowChanged, colProcessed) instanceof Boolean )
				   memProcessed = (Boolean) getValueAt(m_rowChanged, colProcessed); 
				else if (getValueAt(m_rowChanged, colProcessed) instanceof String )
				   memProcessed = Boolean.valueOf((String)getValueAt(m_rowChanged, colProcessed)); 
			}
    			
			Boolean dbProcessed = po.get_ValueAsBoolean("Processed");
			if (memProcessed != null && ! memProcessed.equals(dbProcessed))
				return true;
    	}

		return false;
	}
		
	/**
	 * get Parent Tab No
	 * @return Parent Tab No
	 */
	private int getParentTabNo()
	{
		int tabNo = m_TabNo;
		int currentLevel = Env.getContextAsInt(m_ctx, m_WindowNo, tabNo, GridTab.CTX_TabLevel);
		int parentLevel = currentLevel-1;
		if (parentLevel < 0)
			return tabNo;
			while (parentLevel != currentLevel)
			{
				tabNo--;				
				currentLevel = Env.getContextAsInt(m_ctx, m_WindowNo, tabNo, GridTab.CTX_TabLevel);
				if (tabNo == 0)
					break;
			}
		return tabNo;
	}

	/**
	 * get Tab No
	 * @return Tab No
	 */
	public int getTabNo()
	{
		return m_TabNo;
	}
	
	/**
	 * @param value
	 * @return true if value is not null and is empty string
	 */
	private boolean isNotNullAndIsEmpty (Object value) {
		if (value != null 
				&& (value instanceof String) 
				&& value.toString().equals("")
			) 
		{
			return true;
		} else {
			return false;
		}

	}
	
	/**
	 * @param oldValue
	 * @param value
	 * @return true if oldValue and value is different
	 */
	@SuppressWarnings("unchecked")
	private boolean	isValueChanged(Object oldValue, Object value)
	{
		if ( isNotNullAndIsEmpty(oldValue) ) {
			oldValue = null;
		}

		if ( isNotNullAndIsEmpty(value) ) {
			value = null;
		}

		boolean bChanged = (oldValue == null && value != null) 
							|| (oldValue != null && value == null);

		if (!bChanged && oldValue != null)
		{
			if (oldValue.getClass().equals(value.getClass()))
			{
				if (oldValue instanceof Comparable<?>)
				{
					bChanged = (((Comparable<Object>)oldValue).compareTo(value) != 0);
				}
				else
				{
					bChanged = !oldValue.equals(value);
				}
			}
			else if(value != null)
			{
				bChanged = !(oldValue.toString().equals(value.toString()));
			}
		}
		return bChanged;	
	}

	/**
	 * Load PO for row
	 * @param row row index
	 * @return PO
	 */
	public PO getPO(int row) {
		MTable table = MTable.get (m_ctx, m_AD_Table_ID);
		PO po = null;
		int Record_ID = getKeyID(row);
		if (Record_ID != -1)
		{
			if (Record_ID == 0 && MTable.isZeroIDTable(table.getTableName())) {
				String uuidFromZeroID = table.getUUIDFromZeroID();
				po = table.getPOByUU(uuidFromZeroID, m_trxName);
			} else {
				po = table.getPO(Record_ID, m_trxName);
			}
		}
		else	//	Multi - Key
			po = table.getPO(getWhereClause(getDataAtRow(row)), m_trxName);
		return po;
	}

	/**
	 * @param importing import mode
	 * @param trxName optional trx name
	 */
	public void setImportingMode(boolean importing, String trxName) {
		m_importing = importing;
		m_trxName = trxName;
	}

	/**
	 * @return true if it is in import mode
	 */
	public boolean isImporting() {
		return m_importing;
	}
	
	/**
	 * @return trx name
	 */
	public String get_TrxName() {
		return m_trxName;
	}
	
	/**
	 * reset the cache sort state ( sort column and sort ascending )
	 */
	public void resetCacheSortState() {
		m_lastSortColumnIndex = -1;
		m_lastSortedAscending = true;
	}

	/**
	 * @return index of primary key column
	 */
	public int getKeyColumnIndex() {
		return m_indexKeyColumn;
	}

	/**
	 * Index of change row
	 */
	public int getRowChanged()
	{
		return m_rowChanged;
	}
	
	/**
	 * reset to empty
	 */
	public void reset() 
	{
		if (m_buffer != null)
			m_buffer.clear();
		m_changed = false;
		m_rowChanged = -1;
		if (m_sort != null)
			m_sort.clear();
		if (m_virtualBuffer != null)
			m_virtualBuffer.clear();
		m_rowCount = 0;
		m_rowData = null;
		m_oldValue = null;
		m_inserting = false;
		m_lastSortColumnIndex = -1;
		m_lastSortedAscending = false;
	}

	/**
	 * set current row of gridtable container (gridtab). use in sort to create dse event with new current row (after sort) data
	 * @param m_currentRow
	 */
	protected void setCurrentRow(int m_currentRow) {
		this.m_currentRow  = m_currentRow;
	}
}
