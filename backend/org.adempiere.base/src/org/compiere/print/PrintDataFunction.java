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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

import org.compiere.util.DisplayType;
import org.compiere.util.Env;

/**
 * Print Data Function (Sum, Count, Average, etc) Node
 *
 * @author 	Jorg Janke
 * @version $Id: PrintDataFunction.java,v 1.3 2006/07/30 00:53:02 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 				<li>BF [ 1789279 ] DisplayType for group columns are incorect
 */
public class PrintDataFunction
{
	/**
	 *	Constructor
	 */
	public PrintDataFunction ()
	{
	}	//	PrintDataFunction

	/** The Sum				*/
	private BigDecimal	m_sum = Env.ZERO;
	/** The Count			*/
	private int			m_count = 0;
	/** Total Count			*/
	private int			m_totalCount = 0;
	/** Minimum				*/
	private BigDecimal	m_min = null;
	/** Maximum				*/
	private BigDecimal	m_max = null;
	/** Minimum Date		*/
	private Timestamp	m_minDate = null;
	/** Maximum	Date		*/
	private Timestamp	m_maxDate = null;
	/** Sum of Squares		*/
	private BigDecimal	m_sumSquare = Env.ZERO;

	/** Sum			*/
	static public final char		F_SUM = 'S';
	/** Mean		*/
	static public final char		F_MEAN = 'A';		//	Average mu
	/** Count		*/
	static public final char		F_COUNT = 'C';
	/** Min			*/
	static public final char		F_MIN = 'm';
	/** Max			*/
	static public final char		F_MAX = 'M';
	/** Variance	*/
	static public final char		F_VARIANCE = 'V';	//	sigma square
	/** Deviation	*/
	static public final char		F_DEVIATION = 'D';	//	sigma

	/** Function Keys							*/
	static private final char[]		FUNCTIONS = new char[]
		{F_SUM,     F_MEAN,    F_COUNT,   F_MIN,     F_MAX,     F_VARIANCE, F_DEVIATION};
	/** Symbols									*/
	static private final String[]	FUNCTION_SYMBOLS = new String[]
		{" \u03A3", " \u03BC", " \u2116", " \u2193", " \u2191", " \u03C3\u00B2", " \u03C3"};
	/**	AD_Message Names of Functions			*/
	static private final String[]	FUNCTION_NAMES = new String[]
		{"Sum",     "Mean",    "Count",   "Min",     "Max",     "Variance", "Deviation"};

	/**
	 * 	Add Value to Counter
	 * 	@param s data
	 */
	public void addValue (Serializable s)
	{
		if (s != null)
		{
			//	Count
			m_count++;
			if(s instanceof BigDecimal) {
				BigDecimal bdVaue =(BigDecimal)s;
				//	Sum
				m_sum = m_sum.add(bdVaue);
				//	Min
				if (m_min == null)
					m_min = bdVaue;
				m_min = m_min.min(bdVaue);
				//	Max
				if (m_max == null)
					m_max = bdVaue;
				m_max = m_max.max(bdVaue);
				//	Sum of Squares
				m_sumSquare = m_sumSquare.add (bdVaue.multiply(bdVaue));
			}
			else if(s instanceof Timestamp) {
				Timestamp t = (Timestamp) s;
				//	Min Timestamp
				if ((m_minDate == null) || (m_minDate.after(t)))
					m_minDate = t;
				//	Max Timestamp
				if ((m_maxDate == null) || (m_maxDate.before(t)))
					m_maxDate = t;
			}
		}
		m_totalCount++;
	}	//	addValue

	/**
	 * 	Get Function Value
	 *  @param function function constant (F_*)
	 *  @return function value
	 */
	public Serializable getValue(char function)
	{
		//	Sum
		if (function == F_SUM)
			return m_sum;
		//	Min/Max
		if (function == F_MIN) {
			if(m_minDate != null)
				return m_minDate;
			else
				return m_min;
		}
		if (function == F_MAX) {
			if(m_maxDate != null)
				return m_maxDate;
			else
				return m_max;
		}
		//	Count
		BigDecimal count = new BigDecimal(m_count);
		if (function == F_COUNT)
			return count;

		//	All other functions require count > 0
		if (m_count == 0)
			return Env.ZERO;

		//	Mean = sum/count - round to 4 digits
		if (function == F_MEAN)
		{
			BigDecimal mean = m_sum.divide(count, 4, RoundingMode.HALF_UP);
			if (mean.scale() > 4)
				mean = mean.setScale(4, RoundingMode.HALF_UP);
			return mean;
		}
		//	Variance = ( sum of squares - (square of sum / count) ) / count
		BigDecimal ss = m_sum.multiply(m_sum);
		ss = ss.divide(count, 10, RoundingMode.HALF_UP);
		BigDecimal variance = m_sumSquare.subtract(ss).divide(count, 10, RoundingMode.HALF_UP);
		if (function == F_VARIANCE)
		{
			if (variance.scale() > 4)
				variance = variance.setScale(4, RoundingMode.HALF_UP);
			return variance;
		}
		//	Standard Deviation
		BigDecimal deviation = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
		if (deviation.scale() > 4)
			deviation = deviation.setScale(4, RoundingMode.HALF_UP);
		return deviation;
	}	//	getValue

	/**
	 * 	Reset Value
	 */
	public void reset()
	{
		m_count = 0;
		m_totalCount = 0;
		m_sum = Env.ZERO;
		m_sumSquare = Env.ZERO;
		m_min = null;
		m_max = null;
		m_minDate = null;
		m_maxDate = null;
	}	//	reset

	/**
	 * 	String Representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("[")
			.append("Count=").append(m_count).append(",").append(m_totalCount)
			.append(",Sum=").append(m_sum)
			.append(",SumSquare=").append(m_sumSquare)
			.append(",Min=").append(m_min)
			.append(",Max=").append(m_max);
		sb.append("]");
		return sb.toString();
	}	//	toString

	/**
	 * 	Get Symbol of function
	 * 	@param function function constant (F_*)
	 * 	@return function symbol
	 */
	static public String getFunctionSymbol (char function)
	{
		for (int i = 0; i < FUNCTIONS.length; i++)
		{
			if (FUNCTIONS[i] == function)
				return FUNCTION_SYMBOLS[i];
		}
		return "UnknownFunction=" + function;
	}	//	getFunctionSymbol

	/**
	 * 	Get Name of function
	 * 	@param function function constant (F_*)
	 * 	@return function name
	 */
	static public String getFunctionName (char function)
	{
		for (int i = 0; i < FUNCTIONS.length; i++)
		{
			if (FUNCTIONS[i] == function)
				return FUNCTION_NAMES[i];
		}
		return "UnknownFunction=" + function;
	}	//	getFunctionName

	/**
	 * Get DisplayType of function
	 * @param function function constant (F_*)
	 * @param displayType columns display type
	 * @return display type for function
	 */
	static public int getFunctionDisplayType (char function, int displayType)
	{
		if (function == F_SUM || function == F_MIN || function == F_MAX)
			return displayType;
		if (function == F_COUNT)
			return DisplayType.Integer;
		//	Mean, Variance, Std. Deviation 
		return DisplayType.Number;
	}

}	//	PrintDataFunction
