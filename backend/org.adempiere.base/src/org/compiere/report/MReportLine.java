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
package org.compiere.report;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.X_PA_ReportLine;
import org.compiere.util.DB;
import org.compiere.util.Util;

/**
 *	Financial Report Line Model
 *
 *  @author Jorg Janke
 *  @version $Id: MReportLine.java,v 1.3 2006/08/03 22:16:52 jjanke Exp $
 */
public class MReportLine extends X_PA_ReportLine
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -6310984172477566729L;

	private BasicStroke			overline_Stroke;
	private Stroke				underline_Stroke;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param PA_ReportLine_UU  UUID key
     * @param trxName Transaction
     */
    public MReportLine(Properties ctx, String PA_ReportLine_UU, String trxName) {
        super(ctx, PA_ReportLine_UU, trxName);
		if (Util.isEmpty(PA_ReportLine_UU))
			setInitialDefaults();
		else
			loadSources();
    }

	/**
	 * 	Constructor
	 * 	@param ctx context
	 * 	@param PA_ReportLine_ID id
	 * 	@param trxName transaction
	 */
	public MReportLine (Properties ctx, int PA_ReportLine_ID, String trxName)
	{
		super (ctx, PA_ReportLine_ID, trxName);
		if (PA_ReportLine_ID == 0)
			setInitialDefaults();
		else
			loadSources();
	}	//	MReportLine

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setSeqNo (0);
		//	setIsSummary (false);		//	not active in DD 
		setIsPrinted (false);
	}

	/**
	 * 	Constructor
	 * 	@param ctx context
	 * 	@param rs ResultSet to load from
	 * 	@param trxName transaction
	 */
	public MReportLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
		loadSources();
	}	//	MReportLine

	/**	Contained Sources				*/
	private MReportSource[]		m_sources = null;
	/** Cache result					*/
	private String				m_whereClause = null;

	/**
	 * 	Load contained Sources (MReportSource)
	 */
	private void loadSources()
	{
		ArrayList<MReportSource> list = new ArrayList<MReportSource>();
		String sql = "SELECT * FROM PA_ReportSource WHERE PA_ReportLine_ID=? AND IsActive='Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getPA_ReportLine_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add(new MReportSource (getCtx(), rs, null));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, null, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//
		m_sources = new MReportSource[list.size()];
		list.toArray(m_sources);
		if (log.isLoggable(Level.FINEST)) log.finest("ID=" + getPA_ReportLine_ID()
			+ " - Size=" + list.size());
	}	//	loadSources

	/**
	 * 	Get Report Sources
	 * 	@return report sources
	 */
	public MReportSource[] getSources()
	{
		return m_sources;
	}	//	getSources

	/**
	 * 	List Info
	 */
	public void list()
	{
		System.out.println("- " + toString());
		if (m_sources == null)
			return;
		for (int i = 0; i < m_sources.length; i++)
			System.out.println("  - " + m_sources[i].toString());
	}	//	list

	/**
	 * 	Get Source Column Name
	 * 	@return Source ColumnName
	 */
	public String getSourceColumnName()
	{
		String ColumnName = null;
		for (int i = 0; i < m_sources.length; i++)
		{
			String col = MAcctSchemaElement.getColumnName (m_sources[i].getElementType());
			if (ColumnName == null || ColumnName.length() == 0)
				ColumnName = col;
			else if (!ColumnName.equals(col))
			{
				if (log.isLoggable(Level.CONFIG)) log.config("More than one: " + ColumnName + " - " + col);
				return null;
			}
		}
		return ColumnName;
	}	//	getColumnName

	/**
	 *  Get Value Query for Segment Type
	 * 	@return Query for first source element or null
	 */
	public String getSourceValueQuery()
	{
		if (m_sources != null && m_sources.length > 0)
			return MAcctSchemaElement.getValueQuery(m_sources[0].getElementType());
		return null;
	}	//

	/**
	 * 	Get Column Clause for Select statement
	 * 	@param withSum with SUM() function
	 * 	@return column clause for select - AmtAcctCR+AmtAcctDR/etc or "null" if not defined
	 */
	public String getSelectClause (boolean withSum)
	{
		String at = getPAAmountType().substring(0,1);	//	first letter
		StringBuilder sb = new StringBuilder();
		if (withSum)
			sb.append("SUM(");
		if (PAAMOUNTTYPE_BalanceExpectedSign.equals(at))
		//	sb.append("AmtAcctDr-AmtAcctCr");
			sb.append("acctBalance(Account_ID,AmtAcctDr,AmtAcctCr)");
		else if (PAAMOUNTTYPE_BalanceAccountedSign.equals(at))
			sb.append("AmtAcctDr-AmtAcctCr");
		else if (PAAMOUNTTYPE_CreditOnly.equals(at))
			sb.append("AmtAcctCr");
		else if (PAAMOUNTTYPE_DebitOnly.equals(at))
			sb.append("AmtAcctDr");
		else if (PAAMOUNTTYPE_QuantityAccountedSign.equals(at))
			sb.append("Qty");
		else if (PAAMOUNTTYPE_QuantityExpectedSign.equals(at))
				sb.append("acctBalance(Account_ID,Qty,0)");		
		else
		{
			log.log(Level.SEVERE, "AmountType=" + getPAAmountType () + ", at=" + at);
			return "NULL";
		}
		if (withSum)
			sb.append(")");
		return sb.toString();
	}	//	getSelectClause

	/**
	 * 	Is it PAPERIODTYPE_Period ?
	 * 	@return true if PAPERIODTYPE_Period
	 */
	public boolean isPeriod()
	{
		String pt = getPAPeriodType();
		if (pt == null)
			return false;
		return PAPERIODTYPE_Period.equals(pt);
	}	//	isPeriod

	/**
	 * 	Is it PAPERIODTYPE_Year ?
	 * 	@return true if PAPERIODTYPE_Year
	 */
	public boolean isYear()
	{
		String pt = getPAPeriodType();
		if (pt == null)
			return false;
		return PAPERIODTYPE_Year.equals(pt);
	}	//	isYear

	/**
	 * 	Is it PAPERIODTYPE_Total ?
	 * 	@return true if PAPERIODTYPE_Total
	 */
	public boolean isTotal()
	{
		String pt = getPAPeriodType();
		if (pt == null)
			return false;
		return PAPERIODTYPE_Total.equals(pt);
	}	//	isTotal
	
	/**
	 * Is it natural balance (PAPERIODTYPE_Natural) ?
	 * Natural balance means year balance for profit and loss a/c, total balance for balance sheet account
	 * @return true if Natural Balance Amount Type (PAPERIODTYPE_Natural)
	 */
	public boolean isNatural()
	{
		String pt = getPAPeriodType();
		if (pt == null)
			return false;
		
		return PAPERIODTYPE_Natural.equals(pt);
	}

	/**
	 * 	Get SQL where clause (sources, posting type)
	 * 	@param PA_Hierarchy_ID hierarchy
	 * 	@return where clause
	 */
	public String getWhereClause(int PA_Hierarchy_ID)
	{
		if (m_sources == null)
			return "";
		if (m_whereClause == null)
		{
			//	Only one
			if (m_sources.length == 0)
				m_whereClause = "";
			else if (m_sources.length == 1)
				m_whereClause = m_sources[0].getWhereClause(PA_Hierarchy_ID);
			else
			{
				//	Multiple
				StringBuilder sb = new StringBuilder ("(");
				for (int i = 0; i < m_sources.length; i++)
				{
					if (i > 0)
						sb.append (" OR ");
					sb.append (m_sources[i].getWhereClause(PA_Hierarchy_ID));
				}
				sb.append (")");
				m_whereClause = sb.toString ();
			}
			//	Posting Type
			String PostingType = getPostingType();
			if (PostingType != null && PostingType.length() > 0)
			{
				if (m_whereClause.length() > 0)
					m_whereClause += " AND ";
				m_whereClause += "PostingType='" + PostingType + "'";
				// globalqss - CarlosRuiz
				if (PostingType.equals(MReportLine.POSTINGTYPE_Budget)) {
					if (getGL_Budget_ID() > 0)
						m_whereClause += " AND GL_Budget_ID=" + getGL_Budget_ID();
				}
				// end globalqss
			}
			log.fine(m_whereClause);
		}
		return m_whereClause;
	}	//	getWhereClause

	/**
	 * 	Has Posting Type
	 *	@return true if posting type is not null
	 */
	public boolean isPostingType()
	{
		String PostingType = getPostingType();
		return (PostingType != null && PostingType.length() > 0);
	}	//	isPostingType

	/**
	 * 	String Representation
	 * 	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MReportLine[")
			.append(get_ID()).append(" - ").append(getName()).append(" - ").append(getDescription())
			.append(", SeqNo=").append(getSeqNo()).append(", AmountType=").append(getPAAmountType())
			.append(", PeriodType=").append(getPAPeriodType())
			.append(" - LineType=").append(getLineType());
		if (isLineTypeCalculation())
			sb.append(" - Calculation=").append(getCalculationType())
				.append(" - ").append(getOper_1_ID()).append(" - ").append(getOper_2_ID());
		else	//	SegmentValue
			sb.append(" - SegmentValue - PostingType=").append(getPostingType())
				.append(", AmountType=").append(getPAAmountType())
				.append(", PeriodType=").append(getPAPeriodType());
		sb.append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	Line Type Calculation
	 *	@return true if calculation
	 */
	public boolean isLineTypeCalculation()
	{
		return LINETYPE_Calculation.equals(getLineType());
	}
	/**
	 * 	Line Type Segment Value
	 *	@return true if segment value
	 */
	public boolean isLineTypeSegmentValue()
	{
		return LINETYPE_SegmentValue.equals(getLineType());
	}

	/**
	 * Line Type Blank Line
	 * @return true if Blank Line
	 */
	public boolean isLineTypeBlankLine() 
	{
		return LINETYPE_BlankLine.equals(getLineType());
	}

	/**
	 * 	Calculation Type Range
	 *	@return true if range
	 */
	public boolean isCalculationTypeRange()
	{
		return CALCULATIONTYPE_AddRangeOp1ToOp2.equals(getCalculationType());
	}
	/**
	 * 	Calculation Type Add
	 *	@return true if add
	 */
	public boolean isCalculationTypeAdd()
	{
		return CALCULATIONTYPE_AddOp1PlusOp2.equals(getCalculationType());
	}
	/**
	 * 	Calculation Type Subtract
	 *	@return true if subtract
	 */
	public boolean isCalculationTypeSubtract()
	{
		return CALCULATIONTYPE_SubtractOp1_Op2.equals(getCalculationType());
	}
	/**
	 * 	Calculation Type Percent
	 *	@return true if percent
	 */
	public boolean isCalculationTypePercent()
	{
		return CALCULATIONTYPE_PercentageOp1OfOp2.equals(getCalculationType());
	}
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		// Reset CalculationType, Oper_1_ID and Oper_2_ID if line type is SegmentValue
		if (LINETYPE_SegmentValue.equals(getLineType()))
		{
			if (getCalculationType() != null)
				setCalculationType(null);
			if (getOper_1_ID() != 0)
				setOper_1_ID(0);
			if (getOper_2_ID() != 0)
				setOper_2_ID(0);
		}
		return true;
	}	//	beforeSave
	
	/**
	 * 	Create new Report Line instance from source
	 * 	@param ctx context
	 * 	@param AD_Client_ID parent
	 * 	@param AD_Org_ID parent
	 * 	@param PA_ReportLineSet_ID parent
	 * 	@param source source to copy from
	 * 	@param trxName transaction
	 * 	@return new Report Line instance
	 */
	public static MReportLine copy (Properties ctx, int AD_Client_ID, int AD_Org_ID, 
		int PA_ReportLineSet_ID, MReportLine source, String trxName)
	{
		MReportLine retValue = new MReportLine (ctx, 0, trxName);
		MReportLine.copyValues(source, retValue, AD_Client_ID, AD_Org_ID);
		//
		retValue.setPA_ReportLineSet_ID(PA_ReportLineSet_ID);
		retValue.setOper_1_ID(0);
		retValue.setOper_2_ID(0);
		return retValue;
	}	//	copy

	/**
	 * Get overline style 0 - none, 1 - single, 2 - double
	 * 
	 * @return int - overline style No
	 */
	public int getOverline( )
	{
		if (OVERLINESTROKETYPE_Dotted.equals(getOverlineStrokeType())	|| OVERLINESTROKETYPE_Solid.equals(getOverlineStrokeType())
			|| OVERLINESTROKETYPE_Dashed.equals(getOverlineStrokeType()))
			return 1;
		else if (OVERLINESTROKETYPE_DoubleDotted.equals(getOverlineStrokeType())	|| OVERLINESTROKETYPE_DoubleSolid.equals(getOverlineStrokeType())
					|| OVERLINESTROKETYPE_DoubleDashed.equals(getOverlineStrokeType()))
			return 2;
		return 0;
	} // getOverline

	/**
	 * Get OverLine Stroke
	 * 
	 * @return line based on line (1/2 of) width and stroke (default dotted 1/2p
	 */
	public Stroke getOverlineStroke(BigDecimal stroke)
	{
		if (overline_Stroke == null)
		{
			float width = stroke.floatValue() / 2;
			// . . .
			if (UNDERLINESTROKETYPE_Dotted.equals(getOverlineStrokeType()) || UNDERLINESTROKETYPE_DoubleDotted.equals(getOverlineStrokeType()))
				overline_Stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, getPatternDotted(width), 0.0f);
			// -
			else if (UNDERLINESTROKETYPE_Solid.equals(getOverlineStrokeType()) || UNDERLINESTROKETYPE_DoubleSolid.equals(getOverlineStrokeType()))
				overline_Stroke = new BasicStroke(width);
			// - -
			else if (UNDERLINESTROKETYPE_Dashed.equals(getOverlineStrokeType()) || UNDERLINESTROKETYPE_DoubleDashed.equals(getOverlineStrokeType()))
				overline_Stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, getPatternDashed(width), 0.0f);
		}
		return overline_Stroke;
	} // getOverlineStroke

	/**
	 * Get underline style 0 - none 1 - single 2 - double
	 * 
	 * @return underline style No
	 */
	public int getUnderline( )
	{
		if (UNDERLINESTROKETYPE_Dotted.equals(getUnderlineStrokeType())	|| UNDERLINESTROKETYPE_Solid.equals(getUnderlineStrokeType())
			|| UNDERLINESTROKETYPE_Dashed.equals(getUnderlineStrokeType()))
			return 1;
		else if (UNDERLINESTROKETYPE_DoubleDotted.equals(getUnderlineStrokeType())	|| UNDERLINESTROKETYPE_DoubleSolid.equals(getUnderlineStrokeType())
					|| UNDERLINESTROKETYPE_DoubleDashed.equals(getUnderlineStrokeType()))
			return 2;
		return 0;
	} // getUnderline

	/**
	 * Get UnderLine Stroke
	 * 
	 * @return line based on line (1/2 of) width and stroke (default dotted 1/2p
	 */
	public Stroke getUnderlineStroke(BigDecimal stroke)
	{
		if (underline_Stroke == null)
		{
			float width = stroke.floatValue() / 2;
			// . . .
			if (UNDERLINESTROKETYPE_Dotted.equals(getUnderlineStrokeType()) || UNDERLINESTROKETYPE_DoubleDotted.equals(getUnderlineStrokeType()))
				underline_Stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, getPatternDotted(width), 0.0f);
			// -
			else if (UNDERLINESTROKETYPE_Solid.equals(getUnderlineStrokeType()) || UNDERLINESTROKETYPE_DoubleSolid.equals(getUnderlineStrokeType()))
				underline_Stroke = new BasicStroke(width);
			// - -
			else if (UNDERLINESTROKETYPE_Dashed.equals(getUnderlineStrokeType()) || UNDERLINESTROKETYPE_DoubleDashed.equals(getUnderlineStrokeType()))
				underline_Stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, getPatternDashed(width), 0.0f);
		}
		return underline_Stroke;
	} // getUnderine_Stroke

	/**
	 * Get Pattern Dotted . . . .
	 * 
	 * @param width - Width of line
	 * @return pattern
	 */
	private float[] getPatternDotted(float width)
	{
		return new float[] { 2 * width, 2 * width };
	} // getPatternDotted

	/**
	 * Get Pattern Dashed - - - -
	 * 
	 * @param width - Width of line
	 * @return pattern
	 */
	private float[] getPatternDashed(float width)
	{
		return new float[] { 10 * width, 4 * width };
	} // getPatternDashed

}	//	MReportLine
