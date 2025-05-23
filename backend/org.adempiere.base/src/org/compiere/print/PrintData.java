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
package org.compiere.print;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.compiere.Adempiere;
import org.compiere.print.util.SerializableMatrix;
import org.compiere.print.util.SerializableMatrixImpl;
import org.compiere.report.MReportLine;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Trace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *	Print Data Structure.<br/>
 * 	Created by DataEngine.<br/>
 *  A Structure has rows, which contain elements.<br/>
 *  Elements can be end nodes (PrintDataElements) or data structures (PrintData).<br/>
 *  The row data is sparse - i.e. null if not existing.<br/>
 *  A Structure has optional meta info about content (PrintDataColumn).
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: PrintData.java,v 1.3 2006/07/30 00:53:02 jjanke Exp $
 */
public class PrintData implements Serializable
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 3493453909439452289L;

	/**
	 * 	@param ctx context
	 * 	@param name data element name
	 */
	public PrintData (Properties ctx, String name)
	{
		if (name == null)
			throw new IllegalArgumentException("Name cannot be null");
		m_ctx = ctx;
		m_name = name;
		m_matrix = new SerializableMatrixImpl<Serializable>(name);
	}	//	PrintData

	/**
	 * 	@param ctx context
	 * 	@param name data element name
	 *  @param nodes ArrayList with nodes (content not checked)
	 */
	public PrintData (Properties ctx, String name, ArrayList<Serializable> nodes)
	{
		if (name == null)
			throw new IllegalArgumentException("Name cannot be null");
		m_ctx = ctx;
		m_name = name;		
		m_matrix = new SerializableMatrixImpl<Serializable>(name);
		if (nodes != null)
			addRow(false, 0, nodes);
	}	//	PrintData

	private SerializableMatrix<Serializable> m_matrix;
	
	/**	Context						*/
	private Properties	m_ctx;
	/**	Data Structure Name			*/
	private String 		m_name;
	/**	List of Function Rows		*/
	private ArrayList<Integer>	m_functionRows = new ArrayList<Integer>();

	/**	Table has LevelNo			*/
	private boolean		m_hasLevelNo = false;
	/**	Level Number Indicator		*/
	private static final String	LEVEL_NO = "LEVELNO";

	/**	Optional Column Meta Data	*/
	private PrintDataColumn[]	m_columnInfo = null;
	/**	Optional sql				*/
	private String				m_sql = null;
	/** Optional TableName			*/
	private String				m_TableName = null;

	/**	XML Element Name			*/
	public static final String	XML_TAG = "adempiereData";
	/**	XML Row Name				*/
	public static final String	XML_ROW_TAG = "row";
	/**	XML Attribute Name			*/
	public static final String	XML_ATTRIBUTE_NAME = "name";
	/** XML Attribute Count			*/
	public static final String	XML_ATTRIBUTE_COUNT = "count";
	/** XML Attribute Number		*/
	public static final String	XML_ATTRIBUTE_NO = "no";
	/** XML Attribute Function Row	*/
	public static final String	XML_ATTRIBUTE_FUNCTION_ROW = "function_row";

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(PrintData.class);
	
	/**
	 * 	Get Context
	 * 	@return context
	 */
	public Properties getCtx()
	{
		return m_ctx;
	}	//	getName

	/**
	 * 	Get Name
	 * 	@return name
	 */
	public String getName()
	{
		return m_name;
	}	//	getName

	/**
	 * 	Set optional Column Info
	 * 	@param newInfo Column Info
	 */
	public void setColumnInfo (PrintDataColumn[] newInfo)
	{
		m_columnInfo = newInfo;
	}	//	setColumnInfo

	/**
	 * 	Get optional Column Info
	 * 	@return Column Info
	 */
	public PrintDataColumn[] getColumnInfo()
	{
		return m_columnInfo;
	}	//	getColumnInfo

	/**
	 * 	Set SQL (optional)
	 * 	@param sql SQL
	 */
	public void setSQL (String sql)
	{
		m_sql = sql;
	}	//	setSQL

	/**
	 * 	Get optional SQL
	 * 	@return SQL
	 */
	public String getSQL()
	{
		return m_sql;
	}	//	getSQL

	/**
	 * 	Set TableName (optional)
	 * 	@param TableName TableName
	 */
	public void setTableName (String TableName)
	{
		m_TableName = TableName;
	}	//	setTableName

	/**
	 * 	Get optional TableName
	 * 	@return TableName
	 */
	public String getTableName()
	{
		return m_TableName;
	}	//	getTableName

	/**
	 * 	String representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("PrintData[");
		sb.append(m_name).append(",Rows=").append(m_matrix.getRowCount());
		if (m_TableName != null)
			sb.append(",TableName=").append(m_TableName);
		sb.append("]");
		return sb.toString();
	}	//	toString

	/**
	 * 	Is with 0 rows or 0 nodes/columns
	 * 	@return true if 0 rows or 0 nodes/columns
	 */
	public boolean isEmpty()
	{
		return m_matrix.getRowCount() == 0 || m_matrix.getRowData().isEmpty();
	}	//	isEmpty

	/**
	 * 	Get Number of nodes/columns in row
	 * 	@return number of nodes/columns in row
	 */
	public int getNodeCount()
	{
		if (m_matrix.getRowCount() == 0)
			return 0;
		return m_matrix.getRowData().size();
	}	//	getNodeCount

	/**
	 * Add data row
	 * @param functionRow
	 * @param levelNo
	 */
	public void addRow (boolean functionRow, int levelNo)
	{
		addRow(functionRow, levelNo, new ArrayList<Serializable>());
	}
	
	/**
	 * 	Add Data Row
	 *  @param functionRow true if function row
	 * 	@param levelNo	Line detail Level Number 0=Normal
	 */
	public void addRow (boolean functionRow, int levelNo, List<Serializable> nodes)
	{
		m_matrix.addRow(nodes);
		if (functionRow)
			m_functionRows.add(Integer.valueOf(m_matrix.getRowIndex()));
		if (m_hasLevelNo && levelNo != 0)
			addNode(new PrintDataElement(0, LEVEL_NO, Integer.valueOf(levelNo), DisplayType.Integer, null));
	}	//	addRow

	/**
	 * 	Set Row Index
	 * 	@param row row index
	 * 	@return true if success
	 */
	public boolean setRowIndex (int row)
	{
		return m_matrix.setRowIndex(row);
	}

	/**
	 * 	Set Row Index to next
	 * 	@return true if success
	 */
	public boolean setRowNext()
	{
		return m_matrix.setRowNext();
	}	//	setRowNext

	/**
	 * 	Get Row Count
	 * 	@return row count
	 */
	public int getRowCount()
	{
		return getRowCount(true);
	}	//	getRowCount

	/**
	 * Get row count
	 * @param includeFunctionRows
	 * @return row count
	 */
	public int getRowCount(boolean includeFunctionRows) {
		return includeFunctionRows ? m_matrix.getRowCount() : m_matrix.getRowCount() - m_functionRows.size();
	}
	
	/**
	 * 	Get Current Row Index
	 * 	@return row index
	 */
	public int getRowIndex()
	{
		return m_matrix.getRowIndex();
	}	//	getRowIndex

	/**
	 * 	Is the Row a Function Row
	 * 	@param row row no
	 * 	@return true if function row
	 */
	public boolean isFunctionRow (int row)
	{
		return m_functionRows.contains(Integer.valueOf(row));
	}	//	isFunctionRow

	/**
	 * 	Is current Row a Function Row
	 * 	@return true if current row is a function row
	 */
	public boolean isFunctionRow ()
	{
		return m_functionRows.contains(Integer.valueOf(m_matrix.getRowIndex()));
	}	//	isFunctionRow

	/**
	 * 	Is current Row a page break row
	 * 	@return true if current row is a page break row
	 */
	public boolean isPageBreak ()
	{
		//	page break requires function and meta data
		List<Serializable> nodes = m_matrix.getRowData();
		if (isFunctionRow() && nodes != null)
		{
			for (int i = 0; i < nodes.size(); i++)
			{
				Object o = nodes.get(i);
				if (o instanceof PrintDataElement)
				{
					PrintDataElement pde = (PrintDataElement)o;
					if (pde.isPageBreak())
						return true;
				}
			}
		}
		return false;
	}	//	isPageBreak

	/**
	 * 	Set PrintData has Level No
	 * 	@param hasLevelNo true if sql includes LevelNo
	 */
	public void setHasLevelNo (boolean hasLevelNo)
	{
		m_hasLevelNo = hasLevelNo;
	}	//	hasLevelNo

	/**
	 * 	Is PrintData has Level No
	 * 	@return true if sql includes LevelNo
	 */
	public boolean hasLevelNo()
	{
		return m_hasLevelNo;
	}	//	hasLevelNo

	/**
	 * 	Get Line Level Number for current row
	 * 	@return line level no, 0 = default
	 */
	public int getLineLevelNo ()
	{
		List<Serializable> nodes = m_matrix.getRowData();
		if (nodes == null || !m_hasLevelNo)
			return 0;

		for (int i = 0; i < nodes.size(); i++)
		{
			Object o = nodes.get (i);
			if (o instanceof PrintDataElement)
			{
				PrintDataElement pde = (PrintDataElement)o;
				if (LEVEL_NO.equals (pde.getColumnName()))
				{
					Integer ii = (Integer)pde.getValue();
					return ii.intValue();
				}
			}
		}
		return 0;
	}	//	getLineLevel

	/**
	 * 	Add parent node to current row
	 * 	@param parent parent
	 */
	public void addNode (PrintData parent)
	{
		if (parent == null)
			throw new IllegalArgumentException("Parent cannot be null");
		List<Serializable> nodes = m_matrix.getRowData();
		if (nodes == null) {
			nodes = new ArrayList<Serializable>();
			addRow(false, 0, nodes);
		}
		nodes.add (parent);
	}	//	addNode

	/**
	 * 	Add node to current row
	 * 	@param node node
	 */
	public void addNode (PrintDataElement node)
	{
		if (node == null)
			throw new IllegalArgumentException("Node cannot be null");
		List<Serializable> nodes = m_matrix.getRowData();
		if (nodes == null) {
			nodes = new ArrayList<Serializable>();
			addRow(false, 0, nodes);
		}
		nodes.add (node);
	}	//	addNode

	/**
	 * 	Get node with index in current row
	 * 	@param index index
	 * 	@return PrintData(Element) of index or null
	 */
	public Object getNode (int index)
	{
		List<Serializable> nodes = m_matrix.getRowData();
		if (nodes == null || index < 0 || index >= nodes.size())
			return null;
		return nodes.get(index);
	}	//	getNode

	/**
	 * 	Get node with name in current row
	 * 	@param name node name
	 * 	@return PrintData(Element) with Name or null
	 */
	public Object getNode (String name)
	{
		int index = getIndex (name);
		if (index < 0)
			return null;
		List<Serializable> nodes = m_matrix.getRowData();
		return nodes.get(index);
	}	//	getNode

	/**
	 * 	Get Node with AD_Column_ID in current row
	 * 	@param AD_Column_ID AD_Column_ID
	 * 	@return PrintData(Element) with AD_Column_ID or null
	 *  @deprecated replace by {@link #getNodeByPrintFormatItemId(int)}
	 */
	@Deprecated
	public Object getNode (Integer AD_Column_ID)
	{
		int index = getIndex (AD_Column_ID.intValue());
		if (index < 0)
			return null;
		List<Serializable> nodes = m_matrix.getRowData();
		return nodes.get(index);
	}	//	getNode

	/**
	 * Get node with print format item id in current row
	 * @param item
	 * @return PrintData(Element) with AD_PrintFormatItem_ID or null
	 */
	public Object getNodeByPrintFormatItem(MPrintFormatItem item)
	{
		return getNodeByPrintFormatItemId(item.getAD_PrintFormatItem_ID());
	}
	
	/**
	 * 	Get Node with AD_PrintFormatItem_ID in current row
	 * 	@param AD_PrintFormatItem_ID AD_PrintFormatItem_ID
	 * 	@return PrintData(Element) with AD_PrintFormatItem_ID or null
	 */
	public Object getNodeByPrintFormatItemId (int AD_PrintFormatItem_ID)
	{
		int index = getIndexOfPrintFormatItem(AD_PrintFormatItem_ID);
		if (index < 0)
			return null;
		List<Serializable> nodes = m_matrix.getRowData();
		return nodes.get(index);
	}	//	getNode
	
	/**
	 * 	Get Primary Key node in current row
	 * 	@return PK or null
	 */
	public PrintDataElement getPKey()
	{
		List<Serializable> nodes = m_matrix.getRowData();
		if (nodes == null)
			return null;
		for (int i = 0; i < nodes.size(); i++)
		{
			Object o = nodes.get(i);
			if (o instanceof PrintDataElement)
			{
				PrintDataElement pde = (PrintDataElement)o;
				if (pde.isPKey())
					return pde;
			}
		}
		return null;
	}	//	getPKey

	/**
	 * 	Get Index of Node in current row
	 * 	@param columnName name
	 * 	@return index or -1
	 */
	public int getIndex (String columnName)
	{
		List<Serializable> nodes = m_matrix.getRowData();
		if (nodes == null)
			return -1;
		for (int i = 0; i < nodes.size(); i++)
		{
			Object o = nodes.get(i);
			if (o instanceof PrintDataElement)
			{
				if (columnName.equals(((PrintDataElement)o).getColumnName()))
					return i;
			}
			else if (o instanceof PrintData)
			{
				if (columnName.equals(((PrintData)o).getName()))
					return i;
			}
			else
				log.log(Level.SEVERE, "Element not PrintData(Element) " + o.getClass().getName());
		}
		//	As Data is stored sparse, there might be lots of NULL values
		return -1;
	}	//	getIndex

	/**
	 * 	Get Index of Node in current row
	 * 	@param AD_Column_ID AD_Column_ID
	 * 	@return index or -1
	 */
	@Deprecated
	public int getIndex (int AD_Column_ID)
	{
		if (m_columnInfo == null)
			return -1;
		for (int i = 0; i < m_columnInfo.length; i++)
		{
			if (m_columnInfo[i].getAD_Column_ID() == AD_Column_ID)
				return getIndex(m_columnInfo[i].getColumnName());
		}
		log.log(Level.SEVERE, "Column not found - AD_Column_ID=" + AD_Column_ID);
		if (AD_Column_ID == 0)
			Trace.printStack();
		return -1;
	}	//	getIndex

	/**
	 * 	Get Index of Node in current row
	 * 	@param AD_PrintFormatItem_ID AD_PrintFormatItem_ID
	 * 	@return index or -1
	 */
	public int getIndexOfPrintFormatItem(int AD_PrintFormatItem_ID)
	{
		List<Serializable> nodes = m_matrix.getRowData();
		if (nodes == null)
			return -1;
		for (int i = 0; i < nodes.size(); i++)
		{
			Object o = nodes.get(i);
			if (o instanceof PrintDataElement)
			{
				if (AD_PrintFormatItem_ID == ((PrintDataElement)o).getAD_PrintFormatItem_ID())
					return i;
			}
			else if (o instanceof PrintData)
			{
				continue;
			}
			else
				log.log(Level.SEVERE, "Element not PrintData(Element) " + o.getClass().getName());
		}
		//	As Data is stored sparse, there might be lots of NULL values
		return -1;
	}
	
	/**
	 * 	Dump All Data - header and rows
	 */
	public void dump()
	{
		dump(this);
	}	//	dump

	/**
	 * 	Dump All Data
	 */
	public void dumpHeader()
	{
		dumpHeader(this);
	}	//	dumpHeader

	/**
	 * 	Dump All Data
	 */
	public void dumpCurrentRow()
	{
		dumpRow(this, m_matrix.getRowIndex());
	}	//	dump

	/**
	 * 	Dump all PrintData - header and rows
	 *  @param pd print data
	 */
	private static void dump (PrintData pd)
	{
		dumpHeader(pd);
		for (int i = 0; i < pd.getRowCount(); i++)
			dumpRow(pd, i);
	}	//	dump

	/**
	 * 	Dump PrintData Header
	 *  @param pd print data
	 */
	private static void dumpHeader (PrintData pd)
	{
		if (log.isLoggable(Level.INFO)) log.info(pd.toString());
		if (pd.getColumnInfo() != null)
		{
			for (int i = 0; i < pd.getColumnInfo().length; i++)
				if (log.isLoggable(Level.CONFIG)) log.config(i + ": " + pd.getColumnInfo()[i]);
		}
	}	//	dump

	/**
	 * 	Dump Row
	 *  @param pd print data
	 * 	@param row row
	 */
	private static void dumpRow (PrintData pd, int row)
	{
		if (log.isLoggable(Level.INFO)) log.info("Row #" + row);
		if (row < 0 || row >= pd.getRowCount())
		{
			log.warning("- invalid -");
			return;
		}
		pd.setRowIndex(row);
		if (pd.getNodeCount() == 0)
		{
			log.config("- n/a -");
			return;
		}
		for (int i = 0; i < pd.getNodeCount(); i++)
		{
			Object obj = pd.getNode(i);
			if (obj == null)
				log.config("- NULL -");
			else if (obj instanceof PrintData)
			{
				log.config("- included -");
				dump((PrintData)obj);
			}
			else if (obj instanceof PrintDataElement)
			{
				if (log.isLoggable(Level.CONFIG)) log.config(((PrintDataElement)obj).toStringX());
			}
			else
				if (log.isLoggable(Level.CONFIG)) log.config("- INVALID: " + obj);
		}
	}	//	dumpRow

	/**
	 * 	Get XML Document representation
	 * 	@return XML document
	 */
	public Document getDocument()
	{
		Document document = null;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			//	System.out.println(factory.getClass().getName());
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.newDocument();
			document.appendChild(document.createComment(Adempiere.getSummaryAscii()));
		}
		catch (Exception e)
		{
			System.err.println(e);
			e.printStackTrace();
		}
		//	Root
		if (document != null) {
			Element root = document.createElement(PrintData.XML_TAG);
			root.setAttribute(XML_ATTRIBUTE_NAME, getName());
			root.setAttribute(XML_ATTRIBUTE_COUNT, String.valueOf(getRowCount()));
			document.appendChild(root);
			processXML (this, document, root);
		}
		
		return document;
	}	//	getDocument

	/**
	 * 	Process PrintData Tree and append to XML document
	 * 	@param pd Print Data
	 * 	@param document document
	 *  @param root element to add to
	 */
	private static void processXML (PrintData pd, Document document, Element root)
	{
		for (int r = 0; r < pd.getRowCount(); r++)
		{
			pd.setRowIndex(r);
			Element row = document.createElement(PrintData.XML_ROW_TAG);
			row.setAttribute(XML_ATTRIBUTE_NO, String.valueOf(r));
			if (pd.isFunctionRow())
				row.setAttribute(XML_ATTRIBUTE_FUNCTION_ROW, "yes");
			root.appendChild(row);
			//
			for (int i = 0; i < pd.getNodeCount(); i++)
			{
				Object o = pd.getNode(i);
				if (o instanceof PrintData)
				{
					PrintData pd_x = (PrintData)o;
					Element element = document.createElement(PrintData.XML_TAG);
					element.setAttribute(XML_ATTRIBUTE_NAME, pd_x.getName());
					element.setAttribute(XML_ATTRIBUTE_COUNT, String.valueOf(pd_x.getRowCount()));
					row.appendChild(element);
					processXML (pd_x, document, element);		//	recursive call
				}
				else if (o instanceof PrintDataElement)
				{
					PrintDataElement pde = (PrintDataElement)o;
					if (!pde.isNull())
					{
						Element element = document.createElement(PrintDataElement.XML_TAG);
						element.setAttribute(PrintDataElement.XML_ATTRIBUTE_PRINTFORMATITEM_ID, Integer.toString(pde.getAD_PrintFormatItem_ID()));
						element.setAttribute(PrintDataElement.XML_ATTRIBUTE_NAME, pde.getColumnName());
						if (pde.hasKey())
							element.setAttribute(PrintDataElement.XML_ATTRIBUTE_KEY, pde.getValueKey());
						element.appendChild(document.createTextNode(pde.getValueDisplay(null)));	//	not formatted
						row.appendChild(element);
					}
				}
				else
					log.log(Level.SEVERE, "Element not PrintData(Element) " + o.getClass().getName());
			}	//	columns
		}	//	rows
	}	//	processTree

	/**
	 * 	Create XML representation to StreamResult
	 * 	@param result StreamResult
	 * 	@return true if success
	 */
	public boolean createXML (StreamResult result)
	{
		try
		{
			DOMSource source = new DOMSource(getDocument());
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.transform (source, result);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "(StreamResult)", e);
			return false;
		}
		return true;
	}	//	createXML

	/**
	 * 	Create XML representation to File
	 * 	@param fileName file name
	 * 	@return true if success
	 */
	public boolean createXML (String fileName)
	{
		try
		{
			File file = new File(fileName);
			file.createNewFile();
			StreamResult result = new StreamResult(file);
			createXML (result);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "(file)", e);
			return false;
		}
		return true;
	}	//	createXMLFile
	
	/**
	 *	Create PrintData from XML
	 *	@param ctx context
	 * 	@param input InputSource
	 * 	@return PrintData
	 */
	public static PrintData parseXML (Properties ctx, File input)
	{
		if (log.isLoggable(Level.CONFIG)) log.config(input.toString());
		PrintData pd = null;
		try
		{
			PrintDataHandler handler = new PrintDataHandler(ctx);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			parser.parse(input, handler);
			pd = handler.getPrintData();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "", e);
		}
		return pd;
	}	//	parseXML

	/**
	 * Get MReportLine for current row
	 * @return MReportLine
	 */
	public MReportLine getMReportLine()
	{
		List<Serializable> nodes = m_matrix.getRowData();

		if (nodes == null || !m_hasLevelNo)
			return null;

		for (int i = 0; i < nodes.size(); i++)
		{
			Object o = nodes.get(i);
			if (o instanceof PrintDataElement)
			{
				PrintDataElement pde = (PrintDataElement) o;
				if (MReportLine.COLUMNNAME_PA_ReportLine_ID.equals(pde.getColumnName()))
				{
					Integer ii = (Integer) pde.getValue();
					if (ii > 0)
					{
						return new MReportLine(m_ctx, ii, null);
					}
				}
			}
		}
		return null;
	} // getMReportLine

	/**
	 * Add row
	 * @param functionRow
	 * @param levelNo
	 * @param reportLineID
	 */
	public void addRow(boolean functionRow, int levelNo, int reportLineID)
	{
		addRow(functionRow, levelNo);
		if (m_hasLevelNo && reportLineID != 0)
			addNode(new PrintDataElement(0, "PA_ReportLine_ID", reportLineID, DisplayType.Integer, null));
	}

}	//	PrintData
