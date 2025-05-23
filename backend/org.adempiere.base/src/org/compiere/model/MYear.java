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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.adempiere.exceptions.FillMandatoryException;
import org.adempiere.util.IProcessUI;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;

/**
 *	Year Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MYear.java,v 1.5 2006/10/11 04:12:39 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>BF [ 1761918 ] Error creating periods for a year with per. created partial
 * 			<li>BF [ 2430755 ] Year Create Periods display proper error message
 */
public class MYear extends X_C_Year
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = 2110541427179611810L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_Year_UU  UUID key
     * @param trxName Transaction
     */
    public MYear(Properties ctx, String C_Year_UU, String trxName) {
        super(ctx, C_Year_UU, trxName);
		if (Util.isEmpty(C_Year_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_Year_ID id
	 *	@param trxName transaction
	 */
	public MYear (Properties ctx, int C_Year_ID, String trxName)
	{
		super (ctx, C_Year_ID, trxName);
		if (C_Year_ID == 0)
			setInitialDefaults();
	}	//	MYear

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setProcessing (false);	// N
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MYear (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MYear
	
	/**
	 * 	Parent Constructor
	 *	@param calendar parent
	 */
	public MYear (MCalendar calendar)
	{
		this (calendar.getCtx(), 0, calendar.get_TrxName());
		setClientOrg(calendar);
		setC_Calendar_ID(calendar.getC_Calendar_ID());
		setYear();
	}	//	MYear
		
	/**
	 * 	Set current Year
	 */
	private void setYear ()
	{
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		String Year = String.valueOf(cal.get(Calendar.YEAR));
		super.setFiscalYear(Year);
	}	//	setYear
	
	/**
	 * 	Get Year As Int
	 *	@return year as int or 0
	 */
	public int getYearAsInt()
	{
		String year = getFiscalYear();
		try
		{
			return Integer.parseInt(year);
		}
		catch (Exception e)
		{
			StringTokenizer st = new StringTokenizer(year, "/-, \t\n\r\f");
			if (st.hasMoreTokens())
			{
				String year2 = st.nextToken();
				try
				{
					return Integer.parseInt(year2);
				}
				catch (Exception e2)
				{
					log.log(Level.WARNING, year + "->" + year2 + " - " + e2.toString());
				}
			}
			else
				log.log(Level.WARNING, year + " - " + e.toString());
		}
		return 0;
	}	//	getYearAsInt
	
	/**
	 * 	Get last two characters of year
	 *	@return last two characters of year, for e.g 01 for year 2001
	 */
	public String getYY()
	{
		int yy = getYearAsInt();
		String year = String.valueOf(yy);
		if (year.length() == 4)
			return year.substring(2, 4);
		return getFiscalYear();
	}	//	getYY
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MYear[");
		sb.append(get_ID()).append("-")
			.append(getFiscalYear())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
	
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		int yy = getYearAsInt();
		if (yy == 0)
		{
			throw new FillMandatoryException(COLUMNNAME_FiscalYear);
		}
		return true;
	}	//	beforeSave
	
	/**
	 * 	Create 12 Standard (Jan-Dec) Periods. <br/>
	 * 	Creates also Period Control from DocType.<br/>
	 * 	Cross Reference: org.compiere.process.DocumentTypeVerify.createPeriodControls(Properties, int, SvrProcess, String)
	 * 	@param locale locale 
	 */
	public void createStdPeriods(Locale locale)
	{
		createStdPeriods(locale, null, null);
		
	}	//	createStdPeriods
	
	/**
	 * 	Create 12 Standard Periods from the specified start date.<br/>
	 * 	Creates also Period Control from DocType.<br/>
	 * 	see DocumentTypeVerify#createPeriodControls(Properties, int, SvrProcess, String)
	 * 	@param locale locale
	 *	@param startDate first day of the calendar year
     *  @param dateFormat SimpleDateFormat pattern for generating the period names.
	 *	@return true if created
	 */
	public boolean createStdPeriods(Locale locale, Timestamp startDate, String dateFormat)
	{
		if (locale == null)
		{
			MClient client = MClient.get(getCtx());
			locale = client.getLocale();
		}
		
		if (locale == null && Language.getLoginLanguage() != null)
			locale = Language.getLoginLanguage().getLocale();
		if (locale == null)
			locale = Env.getLanguage(getCtx()).getLocale();
		//
		SimpleDateFormat formatter;
		if ( dateFormat == null || dateFormat.equals("") )
			dateFormat = "MMM-yy";
		formatter = new SimpleDateFormat(dateFormat, locale);
		
		//
		int year = getYearAsInt();
		GregorianCalendar cal = new GregorianCalendar(locale);
		if ( startDate != null )
		{
			cal.setTime(startDate);
			if ( cal.get(Calendar.YEAR) != year)     // specified start date takes precedence in setting year
				year = cal.get(Calendar.YEAR);
		
		}
		else 
		{
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, 0);
			cal.set(Calendar.DAY_OF_MONTH, 1);
		}
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		//
		IProcessUI processMonitor = Env.getProcessUI(getCtx());
		for (int month = 0; month < 12; month++)
		{
			
			Timestamp start = new Timestamp(cal.getTimeInMillis());
			String name = formatter.format(start);
			// get last day of same month
			cal.add(Calendar.MONTH, 1);
			cal.add(Calendar.DAY_OF_YEAR, -1);
			Timestamp end = new Timestamp(cal.getTimeInMillis());
			//
			MPeriod period = MPeriod.findByCalendar(getCtx(), start, getC_Calendar_ID(), get_TrxName());
			if (period == null)
			{
				period = new MPeriod (this, month+1, name, start, end);
			}
			else
			{
				period = MPeriod.getCopy(getCtx(), period.getC_Period_ID(), get_TrxName());
				period.setC_Year_ID(this.getC_Year_ID());
				period.setPeriodNo(month+1);
				period.setName(name);
				period.setStartDate(start);
				period.setEndDate(end);
			}
			if (processMonitor != null)
			{
				processMonitor.statusUpdate(period.toString());
			}
			period.saveEx(get_TrxName());	//	Creates Period Control
			// get first day of next month
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		return true;
		
	}	//	createStdPeriods
	
}	//	MYear
