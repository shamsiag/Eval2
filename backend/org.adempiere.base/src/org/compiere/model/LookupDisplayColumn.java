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

import org.compiere.util.Util;


/**
 *  Lookup Display Column Value Object
 *
 *  @author Jorg Janke
 *  @version $Id: LookupDisplayColumn.java,v 1.3 2006/07/30 00:58:18 jjanke Exp $
 * 
 * @author Teo Sarca
 * 		<li>BF [ 2933367 ] Virtual Column Identifiers are not working
 * 			https://sourceforge.net/p/adempiere/bugs/2291/
 */
public class LookupDisplayColumn implements Serializable
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 5876427657897043394L;

	/**
	 *	Lookup Column Value Object
	 * 	@param columnName column name
	 * 	@param isTranslated translated
	 * 	@param ad_Reference_ID display type
	 * 	@param ad_Reference_Value_ID table/list reference id
	 * @deprecated Please use {@link #LookupDisplayColumn(String, String, boolean, int, int)}
	 */
	@Deprecated
	public LookupDisplayColumn(String columnName, boolean isTranslated,
		int ad_Reference_ID, int ad_Reference_Value_ID)
	{
		this(columnName, null, isTranslated, ad_Reference_ID, ad_Reference_Value_ID);
	}	//
	
	/**
	 * @param columnName column name
	 * @param columnSQL column SQL (in case is virtual column)
	 * @param isTranslated translated
	 * @param ad_Reference_ID display type
	 * @param ad_Reference_Value_ID table/list reference id
	 */
	public LookupDisplayColumn(String columnName, String columnSQL, boolean isTranslated,
		int ad_Reference_ID, int ad_Reference_Value_ID)
	{
		ColumnName = columnName;
		IsTranslated = isTranslated;
		DisplayType = ad_Reference_ID;
		AD_Reference_ID = ad_Reference_Value_ID;
		if (columnSQL != null && columnSQL.length() > 0 && (columnSQL.startsWith(MColumn.VIRTUAL_UI_COLUMN_PREFIX) || columnSQL.startsWith(MColumn.VIRTUAL_SEARCH_COLUMN_PREFIX)))
			ColumnSQL = "NULL";
		else
			ColumnSQL = columnSQL;
		IsVirtual = !Util.isEmpty(ColumnSQL, true);
	}	//

	/** Column Name		*/
	public String 	ColumnName;
	/** Translated		*/
	public boolean 	IsTranslated;
	/** Display Type	*/
	public int 		DisplayType;
	/** table/list reference id (AD_Reference_Value_ID)	*/
	public int 		AD_Reference_ID;
	/** Column SQL		*/
	public final String		ColumnSQL;
	/** Is Virtual Column */
	public final boolean	IsVirtual;

	/**
	 * 	String Representation
	 * 	@return info
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder("LookupDisplayColumn[");
		sb.append("ColumnName=").append(ColumnName);
		if (IsVirtual)
			sb.append(",ColumnSQL=").append(ColumnSQL);
		if (IsTranslated)
			sb.append(",IsTranslated");
		sb.append(",DisplayType=").append(DisplayType);
		if (AD_Reference_ID != 0)
			sb.append(",AD_Reference_ID=").append(AD_Reference_ID);
		sb.append("]");
		return sb.toString();
	}	//	toString

}	//	LookupDisplayColumn
