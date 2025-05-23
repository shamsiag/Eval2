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
package org.compiere.impexp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.I_AD_ImpFormat;
import org.compiere.model.I_I_BPartner;
import org.compiere.model.I_I_ElementValue;
import org.compiere.model.I_I_Product;
import org.compiere.model.I_I_ReportLine;
import org.compiere.model.X_AD_ImpFormat;
import org.compiere.model.X_I_GLJournal;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Import implementation using {@link MImpFormat} and {@link MImpFormatRow}.
 *
 *  @author Jorg Janke
 *  @author Trifon Trifonov, Catura AG (www.catura.de)
 *				<li>FR [ 3010957 ] Custom Separator Character, https://sourceforge.net/p/adempiere/feature-requests/975/ </li>
 *  @author eugen.hanussek@klst.com
 *  			<li>BF [ 3564464 ] Import File Loader discards input records , https://sourceforge.net/p/adempiere/bugs/2727/ </li>
 *
 *  @version $Id$
 */
public final class ImpFormat
{
	/**
	 *	Format
	 *  @param name name
	 *  @param AD_Table_ID table
	 *  @param formatType format type
	 */
	public ImpFormat (String name, int AD_Table_ID, String formatType)
	{
		setName(name);
		setTable(AD_Table_ID);
		setFormatType(formatType);
	}	//	ImpFormat
	
	/**	Logger			*/
	private static final CLogger	log = CLogger.getCLogger(ImpFormat.class);

	private String 		m_name;
	private String 		m_formatType;

	/**	The Table to be imported		*/
	private int 		m_AD_Table_ID;
	private String		m_tableName;
	private String		m_tablePK;
	private String 		m_tableUnique1;
	private String 		m_tableUnique2;
	private String 		m_tableUniqueParent;
	private String 		m_tableUniqueChild;
	//
	private String 		m_BPartner;
	private ArrayList<ImpFormatRow>	m_rows	= new ArrayList<ImpFormatRow>();
	//
	private String separatorChar;
	
	/**
	 *	Set Name
	 *  @param newName new name
	 */
	public void setName(String newName)
	{
		if (newName == null || newName.length() == 0)
			throw new IllegalArgumentException("Name must be at least 1 char");
		else
			m_name = newName;
	}

	/**
	 *  Get Name
	 *  @return name
	 */
	public String getName()
	{
		return m_name;
	}   //  getName
	
	/**
	 * Set separator character for column
	 * @param newChar
	 */
	public void setSeparatorChar(String newChar) {
		if (newChar == null || newChar.length() == 0) {
			throw new IllegalArgumentException("Separator Character must be 1 char");
		} else {
			separatorChar = newChar;
		}
	}
	
	/**
	 * @return separator character for column
	 */
	public String getSeparatorChar() {
		return separatorChar;
	}
	
	/**
	 *	Set Import Table
	 *  @param AD_Table_ID table
	 */
	public void setTable (int AD_Table_ID)
	{
		m_AD_Table_ID = AD_Table_ID;
		m_tableName = null;
		m_tablePK = null;
		String sql = "SELECT t.TableName,c.ColumnName "
				+ "FROM AD_Table t INNER JOIN AD_Column c ON (t.AD_Table_ID=c.AD_Table_ID AND c.IsKey='Y') "
				+ "WHERE t.AD_Table_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, AD_Table_ID);
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				m_tableName = rs.getString(1);
				m_tablePK = rs.getString(2);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "ImpFormat.setTable", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		if (m_tableName == null || m_tablePK == null)
			log.log(Level.SEVERE, "Data not found for AD_Table_ID=" + AD_Table_ID);

		//	Set Additional Table Info
		m_tableUnique1 = "";
		m_tableUnique2 = "";
		m_tableUniqueParent = "";
		m_tableUniqueChild = "";

		if (m_AD_Table_ID == I_I_Product.Table_ID)		//	I_Product
		{
			m_tableUnique1 = "UPC";						//	UPC = unique
			m_tableUnique2 = "Value";
			m_tableUniqueChild = "VendorProductNo";		//	Vendor No may not be unique !
			m_tableUniqueParent = "BPartner_Value";		//			Makes it unique
		}
		else if (m_AD_Table_ID == I_I_BPartner.Table_ID)		//	I_BPartner
		{
			;
		}
		else if (m_AD_Table_ID == I_I_ElementValue.Table_ID)		//	I_ElementValue
		{
			m_tableUniqueParent = "ElementName";			//	the parent key
			m_tableUniqueChild = "Value";					//	the key
		}
		else if (m_AD_Table_ID == I_I_ReportLine.Table_ID)		//	I_ReportLine
		{
			m_tableUniqueParent = "ReportLineSetName";		//	the parent key
			m_tableUniqueChild = "Name";					//	the key
		}
	}   //  setTable

	/**
	 *  Get Import Table Name
	 *  @return AD_Table_ID
	 */
	public int getAD_Table_ID()
	{
		return m_AD_Table_ID;
	}   //  getAD_Table_ID

	/**
	 *  Set Format Type
	 *  @param newFormatType - F/C/T/X
	 */
	public void setFormatType(String newFormatType)
	{
		if (newFormatType.equals(X_AD_ImpFormat.FORMATTYPE_FixedPosition) || newFormatType.equals(X_AD_ImpFormat.FORMATTYPE_CommaSeparated)
			|| newFormatType.equals(X_AD_ImpFormat.FORMATTYPE_TabSeparated) || newFormatType.equals(X_AD_ImpFormat.FORMATTYPE_XML)
			|| newFormatType.equals(X_AD_ImpFormat.FORMATTYPE_CustomSeparatorChar)
			)
			m_formatType = newFormatType;
		else
			throw new IllegalArgumentException("FormatType must be F/C/T/X/U");
	}   //  setFormatType

	/**
	 *  Set Format Type
	 *  @return format type  - F/C/T/X
	 */
	public String getFormatType()
	{
		return m_formatType;
	}   //  getFormatType

	/**
	 *  Set Business Partner
	 *  @param newBPartner (value)
	 *  @deprecated
	 */
	@Deprecated
	public void setBPartner(String newBPartner)
	{
		m_BPartner = newBPartner;
	}   //  setBPartner

	/**
	 *  Get Business Partner
	 *  @return BPartner (value)
	 *  @deprecated
	 */
	@Deprecated
	public String getBPartner()
	{
		return m_BPartner;
	}   //  getVPartner

	/**
	 *	Add Format Row
	 *  @param row row
	 */
	public void addRow (ImpFormatRow row)
	{
		m_rows.add (row);
	}	//	addRow

	/**
	 *	Get Format Row
	 *  @param index index
	 *  @return Import Format Row or null (if index is not valid)
	 */
	public ImpFormatRow getRow (int index)
	{
		if (index >=0 && index < m_rows.size())
			return (ImpFormatRow)m_rows.get(index);
		return null;
	}	//	getRow

	/**
	 *	Get Format Row Count
	 *  @return format row count
	 */
	public int getRowCount()
	{
		return m_rows.size();
	}	//	getRowCount

	/**
	 *	Load import format
	 *  @param Id id
	 *  @return Import Format
	 */
	public static ImpFormat load (int Id)
	{
		if (log.isLoggable(Level.CONFIG))log.config(String.valueOf(Id));
		ImpFormat retValue = null;
		String sql = "SELECT * FROM AD_ImpFormat WHERE AD_Impformat_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt (1, Id);
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				retValue = new ImpFormat (rs.getString("Name"), rs.getInt("AD_Table_ID"), rs.getString("FormatType"));
				if (X_AD_ImpFormat.FORMATTYPE_CustomSeparatorChar.equals(rs.getString(I_AD_ImpFormat.COLUMNNAME_FormatType))) {
					retValue.setSeparatorChar(rs.getString(I_AD_ImpFormat.COLUMNNAME_SeparatorChar));
				}
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
			return null;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		loadRows (retValue, Id);
		return retValue;
	}	//	getFormat

	/**
	 *	Load Format Rows via import format id
	 *  @param format format
	 *  @param ID import format id
	 */
	private static void loadRows (ImpFormat format, int ID)
	{
		String sql = "SELECT f.SeqNo,c.ColumnName,f.StartNo,f.EndNo,f.DataType,c.FieldLength,"		//	1..6
					+ "f.DataFormat,f.DecimalPoint,f.DivideBy100,f.ConstantValue,f.Callout,"		//	7..11
					+ "f.Name, f.importprefix "														//  12..13
					+ "FROM AD_ImpFormat_Row f,AD_Column c "
					+ "WHERE f.AD_ImpFormat_ID=? AND f.AD_Column_ID=c.AD_Column_ID AND f.IsActive='Y' "
					+ "ORDER BY f.SeqNo";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt (1, ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				ImpFormatRow row = new ImpFormatRow (rs.getInt(1),
					rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getString(5), rs.getInt(6), rs.getString(12));
				//
				row.setFormatInfo(rs.getString(7), rs.getString(8),
					rs.getString(9).equals("Y"),
					rs.getString(10), rs.getString(11), rs.getString(13));
				//
				format.addRow (row);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}	//	loadLines

	/**
	 *	Parse line and returns list of values
	 *
	 *  @param line line
	 *  @param withLabel true if with label
	 *  @param trace create trace info
	 * 	@param ignoreEmpty - ignore empty fields
	 *  @return Array of values
	 */
	public String[] parseLine (String line, boolean withLabel, boolean trace, boolean ignoreEmpty)
	{
		if (trace)
			if (log.isLoggable(Level.CONFIG)) log.config("" + line);

		ArrayList<String> list = new ArrayList<String>();
		//	for all columns
		for (int i = 0; i < m_rows.size(); i++)
		{
			ImpFormatRow row = (ImpFormatRow)m_rows.get(i);
			StringBuilder entry = new StringBuilder ();
			//	Label-Start
			if (withLabel)
			{
				//start concat mechanic
				boolean concat = false;
				
				//only act if we combine String or Constant
				if (row.isString() || row.isConstant())
					//if the list contains an entry for the same column, remove the old one and concatenate the two
					for (int j = 0; j < list.size(); j++) {
						if (list.get(j).startsWith(row.getColumnName() + "=")) {
							concat = true;
							entry.append(list.get(j));
							
							if (entry.charAt(entry.length()-1) == '\'')
								entry.deleteCharAt(entry.length()-1); //remove "'" for strings
							
							list.remove(j);
							break;
						}
					} //end concat mechanic
				
				if (!concat) {
					entry.append(row.getColumnName());
					entry.append("=");
					if (row.isString()) {
						entry.append("'");
					} else if (row.isDate()) {
						if (DB.isPostgreSQL())
							entry.append("TO_TIMESTAMP('");
						else
							entry.append("TO_DATE('");
					}
				}
			}

			//	Get Data
			String info = null;
			if (row.isConstant())
				info = "Constant";
			else if (m_formatType.equals(X_AD_ImpFormat.FORMATTYPE_FixedPosition))
			{
				//	check length
				if (row.getStartNo() > 0 && row.getEndNo() <= line.length())
					info = line.substring(row.getStartNo()-1, row.getEndNo());
			}
			else
			{
				info = parseFlexFormat (line, m_formatType, row.getStartNo());
			}

			if (info == null)
				info = "";

			//	Interpret Data
			entry.append(row.parse(info));

			//	Label-End
			if (withLabel)
			{
				if (row.isString())
					entry.append("'");
				else if (row.isDate())
					entry.append("','YYYY-MM-DD HH24:MI:SS')");		//	JDBC Timestamp format w/o miliseconds
			}

			if (!ignoreEmpty || (ignoreEmpty && info.length() != 0))
				list.add(entry.toString());
			//
			if (trace)
				if (log.isLoggable(Level.FINE)) log.fine(info + "=>" + entry.toString() + " (Length=" + info.length() + ")");
		}	//	for all columns

		String[] retValue = new String[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	parseLine

	/**
	 *  Parse flexible line format.<br/>
	 *  A bit inefficient as it always starts from the start.
	 *
	 *  @param line the line to be parsed
	 *  @param formatType Comma or Tab
	 *  @param fieldNo number of field to be returned
	 *  @return field in lime or ""
	 *  @throws IllegalArgumentException if format unknowns
	 */
	private String parseFlexFormat (String line, String formatType, int fieldNo)
	{
		final char QUOTE = '"';
		//  check input
		char delimiter = ' ';
		if (formatType.equals(X_AD_ImpFormat.FORMATTYPE_CommaSeparated)) {
			delimiter = ',';
		} else if (formatType.equals(X_AD_ImpFormat.FORMATTYPE_TabSeparated)) {
			delimiter = '\t';
		} else if (formatType.equals(X_AD_ImpFormat.FORMATTYPE_CustomSeparatorChar)) {
			delimiter = getSeparatorChar().charAt(0);
		} else {
			throw new IllegalArgumentException ("ImpFormat.parseFlexFormat - unknown format: " + formatType);
		}
		if (line == null || line.length() == 0 || fieldNo < 0)
			return "";

		//  We need to read line sequentially as the fields may be delimited
		//  with quotes (") when fields contain the delimiter
		//  Example:    "Artikel,bez","Artikel,""nr""",DEM,EUR
		//  needs to result in - Artikel,bez - Artikel,"nr" - DEM - EUR
		int pos = 0;
		int length = line.length();
		for (int field = 1; field <= fieldNo && pos < length; field++)
		{
			StringBuilder content = new StringBuilder();
			//  two delimiter directly after each other
			if (line.charAt(pos) == delimiter)
			{
				pos++;
				continue;
			}
			//  Handle quotes
			if (line.charAt(pos) == QUOTE)
			{
				pos++;  //  move over beginning quote
				while (pos < length)
				{
					//  double quote
					if (line.charAt(pos) == QUOTE && pos+1 < length && line.charAt(pos+1) == QUOTE)
					{
						content.append(line.charAt(pos++));
						pos++;
					}
					//  end quote
					else if (line.charAt(pos) == QUOTE)
					{
						pos++;
						break;
					}
					//  normal character
					else
						content.append(line.charAt(pos++));
				}
				//  we should be at end of line or a delimiter
				if (pos < length && line.charAt(pos) != delimiter)
					if (log.isLoggable(Level.INFO)) log.info("Did not find delimiter at pos " + pos + " " + line);
				pos++;  //  move over delimiter
			}
			else // plain copy
			{
				while (pos < length && line.charAt(pos) != delimiter)
					content.append(line.charAt(pos++));
				pos++;  //  move over delimiter
			}
			if (field == fieldNo)
				return content.toString();
		}

		//  nothing found
		return "";
	}   //  parseFlexFormat

	/**
	 *	Insert/Update Database.
	 *  @param ctx context
	 *  @param line line
	 *  @param trxName transaction
	 *	@return true if inserted/updated
	 */
	public boolean updateDB (Properties ctx, String line, String trxName)
	{
		if (line == null || line.trim().length() == 0)
		{
			if (log.isLoggable(Level.FINEST))
				log.finest("No Line");
			return false;
		}
		String[] nodes = parseLine (line, true, false, true);	//	with label, no trace, ignore empty
		if (nodes.length == 0)
		{
			if (log.isLoggable(Level.FINEST)) log.finest("Nothing parsed from: " + line);
			return false;
		}

		//  Standard Fields
		int AD_Client_ID = Env.getAD_Client_ID(ctx);
		int AD_Org_ID = Env.getAD_Org_ID(ctx);
		if (getAD_Table_ID() == X_I_GLJournal.Table_ID)
			AD_Org_ID = 0;
		int UpdatedBy = Env.getAD_User_ID(ctx);

		//	Check if the record is already there ------------------------------
		StringBuilder sql = new StringBuilder ("SELECT COUNT(*), MAX(")
			.append(m_tablePK).append(") FROM ").append(m_tableName)
			.append(" WHERE AD_Client_ID=").append(AD_Client_ID).append(" AND (");
		//
		String where1 = null;
		String where2 = null;
		String whereParentChild = null;
		for (int i = 0; i < nodes.length; i++)
		{
			if (nodes[i].endsWith("=''") || nodes[i].endsWith("=0"))
				;
			else if (nodes[i].startsWith(m_tableUnique1 + "="))
				where1 = nodes[i];
			else if (nodes[i].startsWith(m_tableUnique2 + "="))
				where2 = nodes[i];
			else if (nodes[i].startsWith(m_tableUniqueParent + "=") || nodes[i].startsWith(m_tableUniqueChild + "="))
			{
				if (whereParentChild == null)
					whereParentChild = nodes[i];
				else
					whereParentChild += " AND " + nodes[i];
			}
		}
		StringBuilder find = new StringBuilder();
		if (where1 != null)
			find.append(where1);
		if (where2 != null)
		{
			if (find.length() > 0)
				find.append(" OR ");
			find.append(where2);
		}
		if (whereParentChild != null && whereParentChild.indexOf(" AND ") != -1)	//	need to have both criteria
		{
			if (find.length() > 0)
				find.append(" OR (").append(whereParentChild).append(")");	//	may have only one
			else
				find.append(whereParentChild);
		}
		sql.append(find).append(")");
		int count = 0;
		int ID = 0;
		if (find.length() > 0)
		{
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql.toString(), trxName);
				rs = pstmt.executeQuery();
				if (rs.next())
				{
					count = rs.getInt(1);
					if (count == 1)
						ID = rs.getInt(2);
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, sql.toString(), e);
				return false;
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}
		}

		//	Insert Basic Record -----------------------------------------------
		if (ID == 0)
		{
			ID = DB.getNextID(ctx, m_tableName, null);		//	get ID
			sql = new StringBuilder("INSERT INTO ")
				.append(m_tableName).append("(").append(m_tablePK).append(",")
				.append("AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,IsActive")	//	StdFields
				.append(") VALUES (").append(ID).append(",")
				.append(AD_Client_ID).append(",").append(AD_Org_ID)
				.append(",getDate(),").append(UpdatedBy).append(",getDate(),").append(UpdatedBy).append(",'Y'")
				.append(")");
			//
			int no = DB.executeUpdate(sql.toString(), trxName);
			if (no != 1)
			{
				log.log(Level.SEVERE, "Insert records=" + no + "; SQL=" + sql.toString());
				return false;
			}
			if (log.isLoggable(Level.FINER)) log.finer("New ID=" + ID + " " + find);
		}
		else {
			if (log.isLoggable(Level.WARNING))
				log.warning("Not Inserted, Old ID=" + ID + " " + find);
			return false;
		}

		//	Update Info -------------------------------------------------------
		sql = new StringBuilder ("UPDATE ")
			.append(m_tableName).append(" SET ");
		for (int i = 0; i < nodes.length; i++)
			sql.append(nodes[i]).append(",");		//	column=value
		sql.append("IsActive='Y',Processed='N',I_IsImported='N',Updated=getDate(),UpdatedBy=").append(UpdatedBy);
		sql.append(" WHERE ").append(m_tablePK).append("=").append(ID);
		//  Update Cmd
		int no = DB.executeUpdate(sql.toString(), trxName);
		if (no != 1)
		{
			log.log(Level.SEVERE, m_tablePK + "=" + ID + " - rows updated=" + no);
			return false;
		}
		return true;
	}	//	updateDB

}	//	ImpFormat
