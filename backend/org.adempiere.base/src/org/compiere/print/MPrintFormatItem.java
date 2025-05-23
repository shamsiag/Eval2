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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.GridField;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.X_AD_PrintFormatItem;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Print Format Item Model.
 * 	Caches Column Name.
 *	(Add missing Items with PrintFormatUtil)
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: MPrintFormatItem.java,v 1.3 2006/08/03 22:17:17 jjanke Exp $
 */
public class MPrintFormatItem extends X_AD_PrintFormatItem implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 2950704375830865408L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_PrintFormatItem_UU  UUID key
     * @param trxName Transaction
     */
    public MPrintFormatItem(Properties ctx, String AD_PrintFormatItem_UU, String trxName) {
        super(ctx, AD_PrintFormatItem_UU, trxName);
		if (Util.isEmpty(AD_PrintFormatItem_UU))
			setInitialDefaults();
    }

	/**
	 *	Constructor
	 *  @param ctx context
	 *  @param AD_PrintFormatItem_ID AD_PrintFormatItem_ID
	 *	@param trxName transaction
	 */
	public MPrintFormatItem (Properties ctx, int AD_PrintFormatItem_ID, String trxName)
	{
		super (ctx, AD_PrintFormatItem_ID, trxName);
		//	Default Setting
		if (AD_PrintFormatItem_ID == 0)
			setInitialDefaults();
	}	//	MPrintFormatItem

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setFieldAlignmentType(FIELDALIGNMENTTYPE_Default);
		setLineAlignmentType(LINEALIGNMENTTYPE_None);
		setPrintFormatType(PRINTFORMATTYPE_Text);
		setPrintAreaType(PRINTAREATYPE_Content);
		setShapeType(SHAPETYPE_NormalRectangle);
		//
		setIsCentrallyMaintained(true);
		setIsRelativePosition(true);
		setIsNextLine(false);
		setIsNextPage(false);
		setIsSetNLPosition(false);
		setIsFilledRectangle(false);
		setIsImageField(false);
		setXSpace(0);
		setYSpace(0);
		setXPosition(0);
		setYPosition(0);
		setMaxWidth(0);
		setIsFixedWidth(false);
		setIsHeightOneLine(false);
		setMaxHeight(0);
		setLineWidth(1);
		setArcDiameter(0);
		//
		setIsOrderBy(false);
		setSortNo(0);
		setIsGroupBy(false);
		setIsPageBreak(false);
		setIsSummarized(false);
		setIsAveraged(false);
		setIsCounted(false);
		setIsMinCalc(false);
		setIsMaxCalc(false);
		setIsVarianceCalc(false);
		setIsDeviationCalc(false);
		setIsRunningTotal(false);
		setImageIsAttached(false);
		setIsSuppressNull(false);
	}

	/**
	 *	Load Constructor
	 *  @param ctx context
	 *  @param rs ResultSet
	 *	@param trxName transaction
	 */
	public MPrintFormatItem (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MPrintFormatItem

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MPrintFormatItem(MPrintFormatItem copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MPrintFormatItem(Properties ctx, MPrintFormatItem copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MPrintFormatItem(Properties ctx, MPrintFormatItem copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
		this.m_columnName = copy.m_columnName;
		this.m_newTranslationLabel = copy.m_newTranslationLabel;
		this.m_translationLabelChanged = copy.m_translationLabelChanged;
		this.m_translationLabel = copy.m_translationLabel != null ? new HashMap<String, String>(copy.m_translationLabel) : null;
		this.m_translationSuffix = copy.m_translationSuffix != null ? new HashMap<String, String>(copy.m_translationSuffix) : null;
	}
	
	/**	Locally cached column name			*/
	private String 					m_columnName = null;
	/** Locally cached label translations			*/
	private HashMap<String,String>	m_translationLabel;
	String m_newTranslationLabel = null;
	boolean m_translationLabelChanged = false;
	/** Locally cached suffix translations			*/
	private HashMap<String,String>	m_translationSuffix;

	private static CLogger		s_log = CLogger.getCLogger (MPrintFormatItem.class);

	/**
	 *	Get print name with language
	 * 	@param language language - ignored if IsMultiLingualDocument not 'Y'
	 * 	@return print name
	 */
	public String getPrintName (Language language)
	{
		if (language == null || Env.isBaseLanguage(language, "AD_PrintFormatItem"))
			return getPrintName();
		loadTranslations();
		String retValue = (String)m_translationLabel.get(language.getAD_Language());
		if (retValue == null || retValue.length() == 0)
			return getPrintName();
		return retValue;
	}	//	getPrintName

	/**
	 *	Set print name for language
	 * 	@param language language - ignored if IsMultiLingualDocument not 'Y'
	 */
	public void setPrintName (Language language, String printName)
	{
		if (language == null || Env.isBaseLanguage(language, "AD_PrintFormatItem")) {
			setPrintName(printName);
			return;
		}
		loadTranslations();
		String retValue = (String)m_translationLabel.get(language.getAD_Language());
		if ((retValue != null && ! retValue.equals(printName))
			|| (retValue == null && printName != null)) {
			m_newTranslationLabel = printName;
			m_translationLabelChanged = true;
			m_translationLabel.put(language.getAD_Language(), printName);
		}
	}	//	getPrintName

	/**
	 *	Get print name suffix with language
	 * 	@param language language - ignored if IsMultiLingualDocument not 'Y'
	 * 	@return print name suffix
	 */
	public String getPrintNameSuffix (Language language)
	{
		if (language == null || Env.isBaseLanguage(language, "AD_PrintFormatItem"))
			return getPrintNameSuffix();
		loadTranslations();
		String retValue = (String)m_translationSuffix.get(language.getAD_Language());
		if (retValue == null || retValue.length() == 0)
			return getPrintNameSuffix();
		return retValue;
	}	//	getPrintNameSuffix

	/**
	 * 	Load Translations
	 */
	private void loadTranslations()
	{
		if (m_translationLabel == null)
		{
			m_translationLabel = new HashMap<String,String>();
			m_translationSuffix = new HashMap<String,String>();
			String sql = "SELECT AD_Language, PrintName, PrintNameSuffix FROM AD_PrintFormatItem_Trl WHERE AD_PrintFormatItem_ID=?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql, get_TrxName());
				pstmt.setInt(1, get_ID());
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					m_translationLabel.put (rs.getString (1), rs.getString (2));
					m_translationSuffix.put (rs.getString (1), rs.getString (3));
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, "loadTrl", e);
			}
			finally {
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
			}
		}
	}	//	loadTranslations

	/**
	 * 	Is of Type Script
	 *	@return true if of type script
	 */
	public boolean isTypeScript()
	{
		return getPrintFormatType().equals(PRINTFORMATTYPE_Script);
	}

	/**
	 * 	Is of Type Field
	 *	@return true if of type field
	 */
	public boolean isTypeField()
	{
		return getPrintFormatType().equals(PRINTFORMATTYPE_Field);
	}
	
	/**
	 * 	Is of Type Text
	 *	@return true if of type text
	 */
	public boolean isTypeText()
	{
		return getPrintFormatType().equals(PRINTFORMATTYPE_Text);
	}
	
	/**
	 * 	Is of Type Print Format
	 *	@return true if of type print format
	 */
	public boolean isTypePrintFormat()
	{
		return getPrintFormatType().equals(PRINTFORMATTYPE_PrintFormat);
	}
	
	/**
	 * 	Is of Type Image
	 *	@return true if of type image
	 */
	public boolean isTypeImage()
	{
		return getPrintFormatType().equals(PRINTFORMATTYPE_Image);
	}
	
	/**
	 * 	Is of Type Box (Line or Rectangle)
	 *	@return true if of type box
	 */
	public boolean isTypeBox()
	{
		return getPrintFormatType().equals(PRINTFORMATTYPE_Line)
			|| getPrintFormatType().equals(PRINTFORMATTYPE_Rectangle);
	}

	/**
	 * 	Is Field Alignment Center
	 *	@return true if field alignment is center
	 */
	public boolean isFieldCenter()
	{
		return getFieldAlignmentType().equals(FIELDALIGNMENTTYPE_Center);
	}
	
	/**
	 * 	Is Field Alignment Leading Left
	 *	@return true if field alignment is leading left
	 */
	public boolean isFieldAlignLeading()
	{
		return getFieldAlignmentType().equals(FIELDALIGNMENTTYPE_LeadingLeft);
	}
	
	/**
	 * 	Field Alignment Trailing Right
	 *	@return true if field alignment is trailing right
	 */
	public boolean isFieldAlignTrailing()
	{
		return getFieldAlignmentType().equals(FIELDALIGNMENTTYPE_TrailingRight);
	}
	
	/**
	 * 	Is Field Alignment Block
	 *	@return true if field alignment is block
	 */
	public boolean isFieldAlignBlock()
	{
		return getFieldAlignmentType().equals(FIELDALIGNMENTTYPE_Block);
	}
	
	/**
	 * 	Is Field Alignment Default
	 *	@return true if field is with default alignment
	 */
	public boolean isFieldAlignDefault()
	{
		return getFieldAlignmentType().equals(FIELDALIGNMENTTYPE_Default);
	}
	
	/**
	 * 	Is Line Alignment Center
	 *	@return true if line alignment is center
	 */
	public boolean isLineAlignCenter()
	{
		return getLineAlignmentType().equals(LINEALIGNMENTTYPE_Center);
	}
	
	/**
	 * 	Is Line Alignment Leading Left
	 *	@return true if line alignment is leading left
	 */
	public boolean isLineAlignLeading()
	{
		return getLineAlignmentType().equals(LINEALIGNMENTTYPE_LeadingLeft);
	}
	
	/**
	 * 	Is Line Alignment Trailing Right
	 *	@return true if line alignment is trailing right
	 */
	public boolean isLineAlignTrailing()
	{
		return getLineAlignmentType().equals(LINEALIGNMENTTYPE_TrailingRight);
	}

	/**
	 * 	Is Header Area
	 *	@return true if area is header
	 */
	public boolean isHeader()
	{
		return getPrintAreaType().equals(PRINTAREATYPE_Header);
	}
	/**
	 * 	Is Content Area
	 *	@return true if area is content
	 */
	public boolean isContent()
	{
		return getPrintAreaType().equals(PRINTAREATYPE_Content);
	}
	/**
	 * 	Is Footer Area
	 *	@return true if area is footer
	 */
	public boolean isFooter()
	{
		return getPrintAreaType().equals(PRINTAREATYPE_Footer);
	}
	
	/**
	 * 	Is Type Barcode 
	 *	@return true if item is of type barcode
	 */
	public boolean isBarcode()
	{
		String s = getBarcodeType();
		return s != null && s.length() > 0;
	}
		
	/**
	 * 	String representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("MPrintFormatItem[");
		sb.append("ID=").append(get_ID())
			.append(",Name=").append(getName())
			.append(",Print=").append(getPrintName())
			.append(", Seq=").append(getSeqNo())
			.append(",Sort=").append(getSortNo())
			.append(", Area=").append(getPrintAreaType())
			.append(", MaxWidth=").append(getMaxWidth())
			.append(",MaxHeight=").append(getMaxHeight())
			.append(",OneLine=").append(isHeightOneLine())
			.append(", Relative=").append(isRelativePosition());
		if (isRelativePosition())
			sb.append(",X=").append(getXSpace()).append(",Y=").append(getYSpace())
				.append(",LineAlign=").append(getLineAlignmentType())
				.append(",NewLine=").append(isNextLine())
				.append(",NewPage=").append(isPageBreak());
		else
			sb.append(",X=").append(getXPosition()).append(",Y=").append(getYPosition());
		sb.append(",FieldAlign=").append(getFieldAlignmentType());
		//
		sb.append(", Type=").append(getPrintFormatType());
		if (isTypeText())
			;
		else if (isTypeField())
			sb.append(",AD_Column_ID=").append(getAD_Column_ID());
		else if (isTypePrintFormat())
			sb.append(",AD_PrintFormatChild_ID=").append(getAD_PrintFormatChild_ID())
				.append(",AD_Column_ID=").append(getAD_Column_ID());
		else if (isTypeImage())
			sb.append(",ImageIsAttached=").append(isImageIsAttached()).append(",ImageURL=").append(getImageURL());
		//
		sb.append(", Printed=").append(isPrinted())
			.append(",SeqNo=").append(getSeqNo())
			.append(",OrderBy=").append(isOrderBy())
			.append(",SortNo=").append(getSortNo())
			.append(",Summarized=").append(isSummarized());
		sb.append("]");
		return sb.toString();
	}	//	toString

	/**	Lookup Map of AD_Column_ID for ColumnName	*/
	private static CCache<Integer,String>	s_columns = new CCache<Integer,String>(Table_Name, 200);

	/**
	 * 	Get ColumnName from AD_Column_ID
	 *  @return ColumnName
	 */
	public String getColumnName()
	{
		if (m_columnName == null)	//	Get Column Name from AD_Column not index
			m_columnName = getColumnName (Integer.valueOf(getAD_Column_ID()));
		return m_columnName;
	}	//	getColumnName

	/**
	 * 	Get Column Name from AD_Column_ID.
	 *  Be careful not to confuse it with PO method getAD_Column_ID (index)
	 * 	@param AD_Column_ID column
	 * 	@return Column Name
	 */
	private static String getColumnName (Integer AD_Column_ID)
	{
		if (AD_Column_ID == null || AD_Column_ID.intValue() == 0)
			return null;
		//
		String retValue = (String)s_columns.get(AD_Column_ID);
		if (retValue == null)
		{
			String sql = "SELECT ColumnName FROM AD_Column WHERE AD_Column_ID=?";
			retValue = DB.getSQLValueString(null, sql, AD_Column_ID.intValue());
			if (retValue != null)
				s_columns.put(AD_Column_ID, retValue);
		}
		return retValue;
	}	//	getColumnName
	
	/**
	 * 	Create and Save Print Format Item from Column
	 *  @param format parent
	 * 	@param AD_Column_ID column
	 *  @param seqNo sequence of display if 0 it is not printed
	 * 	@return Print Format Item
	 */
	public static MPrintFormatItem createFromColumn (MPrintFormat format, int AD_Column_ID, int seqNo)
	{
		MPrintFormatItem pfi = new MPrintFormatItem (format.getCtx(), 0, format.get_TrxName());
		pfi.setAD_PrintFormat_ID (format.getAD_PrintFormat_ID());
		pfi.setClientOrg(format);
		pfi.setAD_Column_ID(AD_Column_ID);
		pfi.setPrintFormatType(PRINTFORMATTYPE_Field);

		//	translation is dome by trigger
		String sql = "SELECT c.ColumnName,e.Name,e.PrintName, "		//	1..3
			+ "c.AD_Reference_ID,c.IsKey,c.SeqNo "					//	4..6
			+ "FROM AD_Column c, AD_Element e "
			+ "WHERE c.AD_Column_ID=?"
			+ " AND c.AD_Element_ID=e.AD_Element_ID";
		//	translate base entry if single language - trigger copies to trl tables
		Language language = format.getLanguage();
		boolean trl = !Env.isMultiLingualDocument(format.getCtx()) && !language.isBaseLanguage();
		if (trl)
			sql = "SELECT c.ColumnName,e.Name,e.PrintName, "		//	1..3
				+ "c.AD_Reference_ID,c.IsKey,c.SeqNo "				//	4..6
				+ "FROM AD_Column c, AD_Element_Trl e "
				+ "WHERE c.AD_Column_ID=?"
				+ " AND c.AD_Element_ID=e.AD_Element_ID"
				+ " AND e.AD_Language=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, format.get_TrxName());
			pstmt.setInt(1, AD_Column_ID);
			if (trl)
				pstmt.setString(2, language.getAD_Language());
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String ColumnName = rs.getString(1);
				pfi.setName(rs.getString(2));
				pfi.setPrintName(rs.getString(3));
				int displayType = rs.getInt(4);
				if (DisplayType.isNumeric(displayType))
					pfi.setFieldAlignmentType(FIELDALIGNMENTTYPE_TrailingRight);
				else if (displayType == DisplayType.Text || displayType == DisplayType.Memo )
					pfi.setFieldAlignmentType(FIELDALIGNMENTTYPE_Block);
				else
					pfi.setFieldAlignmentType(FIELDALIGNMENTTYPE_LeadingLeft);
				boolean isKey = "Y".equals(rs.getString(5));
				//
				if (isKey
					|| ColumnName.startsWith("Created") || ColumnName.startsWith("Updated")
					|| ColumnName.equals("AD_Client_ID") || ColumnName.equals("AD_Org_ID")
					|| ColumnName.equals("IsActive")
					|| displayType == DisplayType.Button || displayType == DisplayType.Binary
					|| displayType == DisplayType.ID || displayType == DisplayType.Image
					|| displayType == DisplayType.RowID || displayType == DisplayType.Chart
					|| seqNo == 0 )
				{
					pfi.setIsPrinted(false);
					pfi.setSeqNo(0);
				}
				else
				{
					pfi.setIsPrinted(true);
					pfi.setSeqNo(seqNo);
				}
				int idSeqNo = rs.getInt(6);	//	IsIdentifier SortNo
				if (idSeqNo > 0)
				{
					pfi.setIsOrderBy(true);
					pfi.setSortNo(idSeqNo);
				}
				if (displayType == DisplayType.ImageURL)
				{
					pfi.setPrintFormatType(PRINTFORMATTYPE_Image);
					pfi.setIsImageField(true);
					pfi.setMaxWidth(MSysConfig.getIntValue(MSysConfig.ZK_THUMBNAIL_IMAGE_WIDTH, 100, Env.getAD_Client_ID(Env.getCtx())));
					pfi.setMaxHeight(MSysConfig.getIntValue(MSysConfig.ZK_THUMBNAIL_IMAGE_HEIGHT, 100, Env.getAD_Client_ID(Env.getCtx())));
				}
			}
			else
				s_log.log(Level.SEVERE, "Not Found AD_Column_ID=" + AD_Column_ID
					+ " Trl=" + trl + " " + language.getAD_Language());
		}
		catch (SQLException e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		if (!pfi.save())
			return null;
	//	pfi.dump();
		return pfi;
	}	//	createFromColumn
	
	/**
	 * Create and save print format item from GridField
	 * @param format
	 * @param gridField
	 * @param seqNo
	 * @return new print format item instance
	 */
	public static MPrintFormatItem createFromGridField (MPrintFormat format, GridField gridField, int seqNo)
	{
		if (gridField.getAD_Column_ID() <= 0)
			return null;
		
		MPrintFormatItem pfi = new MPrintFormatItem (format.getCtx(), 0, format.get_TrxName());
		pfi.setAD_PrintFormat_ID (format.getAD_PrintFormat_ID());
		pfi.setClientOrg(format);
		pfi.setAD_Column_ID(gridField.getAD_Column_ID());
		pfi.setPrintFormatType(PRINTFORMATTYPE_Field);

		//	translation is dome by trigger
		String sql = "SELECT c.ColumnName,e.Name,e.PrintName, "		//	1..3
			+ "c.AD_Reference_ID,c.IsKey,c.SeqNo "					//	4..6
			+ "FROM AD_Column c, AD_Element e "
			+ "WHERE c.AD_Column_ID=?"
			+ " AND c.AD_Element_ID=e.AD_Element_ID";
		//	translate base entry if single language - trigger copies to trl tables
		Language language = format.getLanguage();
		boolean trl = !Env.isMultiLingualDocument(format.getCtx()) && !language.isBaseLanguage();
		if (trl)
			sql = "SELECT c.ColumnName,e.Name,e.PrintName, "		//	1..3
				+ "c.AD_Reference_ID,c.IsKey,c.SeqNo "				//	4..6
				+ "FROM AD_Column c, AD_Element_Trl e "
				+ "WHERE c.AD_Column_ID=?"
				+ " AND c.AD_Element_ID=e.AD_Element_ID"
				+ " AND e.AD_Language=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, format.get_TrxName());
			pstmt.setInt(1, gridField.getAD_Column_ID());
			if (trl)
				pstmt.setString(2, language.getAD_Language());
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				pfi.setName(rs.getString(2));
				pfi.setPrintName(rs.getString(3));
				int displayType = rs.getInt(4);
				if (DisplayType.isNumeric(displayType))
					pfi.setFieldAlignmentType(FIELDALIGNMENTTYPE_TrailingRight);
				else if (displayType == DisplayType.Text || displayType == DisplayType.Memo )
					pfi.setFieldAlignmentType(FIELDALIGNMENTTYPE_Block);
				else
					pfi.setFieldAlignmentType(FIELDALIGNMENTTYPE_LeadingLeft);
				//
				if (displayType == DisplayType.Button || displayType == DisplayType.Binary
					|| displayType == DisplayType.ID || displayType == DisplayType.Image
					|| displayType == DisplayType.RowID || displayType == DisplayType.Chart
					|| seqNo == 0)
				{
					pfi.setIsPrinted(false);
					pfi.setSeqNo(0);
				}
				else
				{
					pfi.setIsPrinted(true);
					pfi.setSeqNo(seqNo);
				}
				int idSeqNo = rs.getInt(6);	//	IsIdentifier SortNo
				if (idSeqNo > 0)
				{
					pfi.setIsOrderBy(true);
					pfi.setSortNo(idSeqNo);
				}
				if (displayType == DisplayType.ImageURL)
				{
					pfi.setPrintFormatType(PRINTFORMATTYPE_Image);
					pfi.setIsImageField(true);
					pfi.setMaxWidth(MSysConfig.getIntValue(MSysConfig.ZK_THUMBNAIL_IMAGE_WIDTH, 100, Env.getAD_Client_ID(Env.getCtx())));
					pfi.setMaxHeight(MSysConfig.getIntValue(MSysConfig.ZK_THUMBNAIL_IMAGE_HEIGHT, 100, Env.getAD_Client_ID(Env.getCtx())));
				}
			}
			else
				s_log.log(Level.SEVERE, "Not Found AD_Column_ID=" + gridField.getAD_Column_ID()
					+ " Trl=" + trl + " " + language.getAD_Language());
		}
		catch (SQLException e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		if (!pfi.save())
			return null;
	//	pfi.dump();
		return pfi;
	}

	/**
	 * 	Copy existing Definition To Client
	 * 	@param To_Client_ID to client
	 *  @param AD_PrintFormat_ID parent print format
	 * 	@return print format item
	 */
	public MPrintFormatItem copyToClient (int To_Client_ID, int AD_PrintFormat_ID)
	{
		return copyToClient(To_Client_ID, AD_PrintFormat_ID, (String)null);
	}
	
	/**
	 * 	Copy existing Definition To Client
	 * 	@param To_Client_ID to client
	 *  @param AD_PrintFormat_ID parent print format
	 * 	@return print format item
	 */
	public MPrintFormatItem copyToClient (int To_Client_ID, int AD_PrintFormat_ID, String trxName)
	{
		MPrintFormatItem to = new MPrintFormatItem (p_ctx, 0, trxName);
		MPrintFormatItem.copyValues(this, to);
		to.setClientOrg(To_Client_ID, 0);
		to.setAD_PrintFormat_ID(AD_PrintFormat_ID);
		to.saveEx();
		return to;
	}	//	copyToClient

	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Reset SortNo, IsGroupBy and IsPageBreak if IsOrderBy=false
		if (!isOrderBy())
		{
			setSortNo(0);
			setIsGroupBy(false);
			setIsPageBreak(false);
		}
		if (isRelativePosition())
		{
			setXPosition(0);
			setYPosition(0);
		}
		else
		{
			setXSpace(0);
			setYSpace(0);
		}
		// Reset ImageIsAttached and ImageURL if IsImageField=true
		if (isImageField())
		{
			setImageIsAttached(false);
			setImageURL(null);
		}
		if (!isTypeField() && !isTypePrintFormat() && !isImageField()) {
			setAD_Column_ID(0);
		}
		
		if(!isTypeScript() && !Util.isEmpty(getScript())) {
			setScript(null);
		}
		
		// Only advanced role can edit Script field
		if(!Util.isEmpty(getScript())
		   && is_ValueChanged(MPrintFormatItem.COLUMNNAME_Script)
		   && !MRole.getDefault().isAccessAdvanced()) {
			log.saveError("Error", Msg.getMsg(getCtx(), "ActionNotAllowedHere"));
			return false;
		}
		
		return true;
	}	//	beforeSave
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		//	Set Translation from Element
		if (newRecord 
			&& getPrintName() != null && getPrintName().length() > 0)
		{
			String sql = "UPDATE AD_PrintFormatItem_Trl "
				+ "SET PrintName = (SELECT e.PrintName "
					+ "FROM AD_Element_Trl e, AD_Column c "
					+ "WHERE e.AD_Language=AD_PrintFormatItem_Trl.AD_Language"
					+ " AND e.AD_Element_ID=c.AD_Element_ID"
					+ " AND c.AD_Column_ID=" + getAD_Column_ID() + ") "
				+ "WHERE AD_PrintFormatItem_ID = " + get_ID()
				+ " AND EXISTS (SELECT * "
					+ "FROM AD_Element_Trl e, AD_Column c "
					+ "WHERE e.AD_Language=AD_PrintFormatItem_Trl.AD_Language"
					+ " AND e.AD_Element_ID=c.AD_Element_ID"
					+ " AND c.AD_Column_ID=" + getAD_Column_ID()
					+ " AND AD_PrintFormatItem_Trl.AD_PrintFormatItem_ID = " + get_ID() + ")"
				+ " AND EXISTS (SELECT * FROM AD_Client "
					+ "WHERE AD_Client_ID=AD_PrintFormatItem_Trl.AD_Client_ID AND IsMultiLingualDocument='Y')";
			int no = DB.executeUpdateEx(sql, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("translations updated #" + no);
		}
		
		// Update AD_PrintFormatItem_Trl.PrintName with m_newTranslationLabel
		if (m_translationLabelChanged) {
			String sql = "UPDATE AD_PrintFormatItem_Trl "
					+ "SET PrintName = ? "
					+ "WHERE AD_PrintFormatItem_ID = ?"
					+ " AND AD_Language=?";
			int no = DB.executeUpdateEx(sql, new Object[] {m_newTranslationLabel, get_ID(), Language.getLoginLanguage().getAD_Language()}, get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("translations updated #" + no);
			
			m_newTranslationLabel = null;
			m_translationLabelChanged = false;
		}

		return success;
	}	//	afterSave
	
	@Override
	public boolean is_Changed() {
		if (m_translationLabelChanged)
			return true;
		return super.is_Changed();
	}

	@Override
	public MPrintFormatItem markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MPrintFormatItem
