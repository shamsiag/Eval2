/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                        *
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
package org.compiere.util;

import java.sql.Timestamp;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.compiere.model.MCountry;

/**
 *	Time and Date Utilities
 *
 * 	@author 	Jorg Janke
 *  @author 	Teo Sarca, SC ARHIPAC SERVICE SRL
 * 	@version 	$Id: TimeUtil.java,v 1.3 2006/07/30 00:54:35 jjanke Exp $
 */
public class TimeUtil
{
	/**
	 * 	Get day only timestamp from time (setting all the time values to zero).
	 *  @param time timestamp in millisecond. 0 for current time.
	 *  @return day with 00:00
	 */
	static public Timestamp getDay (long time)
	{
		if (time == 0)
			time = System.currentTimeMillis();
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return new Timestamp (cal.getTimeInMillis());
	}	//	getDay

	/**
	 * 	Truncate timestamp to day only timestamp (setting all the time values to zero)
	 *  @param dayTime day and time
	 *  @return day with 00:00
	 */
	static public Timestamp getDay (Timestamp dayTime)
	{
		if (dayTime == null)
			return getDay(System.currentTimeMillis());
		return getDay(dayTime.getTime());
	}	//	getDay

	/**
	 * 	Create day only timestamp (setting all time values to zero) 
	 *	@param year year (if two digits: &lt; 50 is 2000; &gt; 50 is 1900)
	 *	@param month month 1..12
	 *	@param day day 1..31
	 *	@return timestamp ** not too reliable
	 */
	static public Timestamp getDay (int year, int month, int day)
	{
		if (year < 50)
			year += 2000;
		else if (year < 100)
			year += 1900;
		if (month < 1 || month > 12)
			throw new IllegalArgumentException("Invalid Month: " + month);
		if (day < 1 || day > 31)
			throw new IllegalArgumentException("Invalid Day: " + day);
		GregorianCalendar cal = new GregorianCalendar (year, month-1, day);
		return new Timestamp (cal.getTimeInMillis());
	}	//	getDay

	/**
	 * 	Get today (truncate the time portion)
	 *  @return day with 00:00
	 */
	static public Calendar getToday ()
	{
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}	//	getToday

	/**
	 * 	Get next day timestamp (truncate the time portion)
	 *  @param day day
	 *  @return next day with 00:00
	 */
	static public Timestamp getNextDay (Timestamp day)
	{
		if (day == null)
			day = new Timestamp(System.currentTimeMillis());
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		cal.setTimeInMillis(day.getTime());
		cal.add(Calendar.DAY_OF_YEAR, +1);	//	next
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return new Timestamp (cal.getTimeInMillis());
	}	//	getNextDay
	
	/**
	 * 	Get previous day timestamp (truncate the time portion)
	 *  @param day day
	 *  @return previous day with 00:00
	 */
	static public Timestamp getPreviousDay (Timestamp day)
	{
		if (day == null)
			day = new Timestamp(System.currentTimeMillis());
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		cal.setTimeInMillis(day.getTime());
		cal.add(Calendar.DAY_OF_YEAR, -1);	//	previous
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return new Timestamp (cal.getTimeInMillis());
	}	//	getPreviousDay

	/**
	 * 	Get last date in month (truncate the time portion)
	 *  @param day day
	 *  @return last day of month with 00:00
	 */
	static public Timestamp getMonthLastDay (Timestamp day)
	{
		if (day == null)
			day = new Timestamp(System.currentTimeMillis());
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		cal.setTimeInMillis(day.getTime());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		//
		cal.add(Calendar.MONTH, 1);			//	next
		cal.set(Calendar.DAY_OF_MONTH, 1);	//	first
		cal.add(Calendar.DAY_OF_YEAR, -1);	//	previous
		return new Timestamp (cal.getTimeInMillis());
	}	//	getMonthLastDay

	/**
	 * 	Create a new timestamp from the day and time part (millisecond is set to 0).
	 * 	@param day day part
	 * 	@param time time part
	 * 	@return new timestamp from day + time
	 */
	static public Timestamp getDayTime (Timestamp day, Timestamp time)
	{
		GregorianCalendar cal_1 = new GregorianCalendar();
		cal_1.setTimeInMillis(day.getTime());
		GregorianCalendar cal_2 = new GregorianCalendar();
		cal_2.setTimeInMillis(time.getTime());
		//
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		cal.set(cal_1.get(Calendar.YEAR),
			cal_1.get(Calendar.MONTH),
			cal_1.get(Calendar.DAY_OF_MONTH),
			cal_2.get(Calendar.HOUR_OF_DAY),
			cal_2.get(Calendar.MINUTE),
			cal_2.get(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, 0);
		Timestamp retValue = new Timestamp(cal.getTimeInMillis());
		return retValue;
	}	//	getDayTime

	/**
	 * 	Is the _1 in the Range of _2
	 *  <pre>
	 * 		Time_1         +--x--+
	 * 		Time_2   +a+      +---b---+   +c+
	 * 	</pre>
	 *  The function returns true for b and false for a/c.
	 *  @param start_1 start (1)
	 *  @param end_1 not included end (1)
	 *  @param start_2 start (2)
	 *  @param end_2 not included (2)
	 *  @return true if in range
	 */
	static public boolean inRange (Timestamp start_1, Timestamp end_1, Timestamp start_2, Timestamp end_2)
	{
		//	validity check
		if (end_1.before(start_1))
			throw new UnsupportedOperationException ("TimeUtil.inRange End_1=" + end_1 + " before Start_1=" + start_1);
		if (end_2.before(start_2))
			throw new UnsupportedOperationException ("TimeUtil.inRange End_2=" + end_2 + " before Start_2=" + start_2);
		//	case a
		if (!end_2.after(start_1))		//	end not including
		{
			return false;
		}
		//	case c
		if (!start_2.before(end_1))		//	 end not including
		{
			return false;
		}
		return true;
	}	//	inRange

	/**
	 * 	Is start..end include one of the days ?
	 *  @param start start day
	 *  @param end end day (not including)
	 *  @param OnMonday true if OK
	 *  @param OnTuesday true if OK
	 *  @param OnWednesday true if OK
	 *  @param OnThursday true if OK
	 *  @param OnFriday true if OK
	 *  @param OnSaturday true if OK
	 *  @param OnSunday true if OK
	 *  @return true if on one of the days
	 */
	static public boolean inRange (Timestamp start, Timestamp end,
		boolean OnMonday, boolean OnTuesday, boolean OnWednesday,
		boolean OnThursday, boolean OnFriday, boolean OnSaturday, boolean OnSunday)
	{
		//	are there restrictions?
		if (OnSaturday && OnSunday && OnMonday && OnTuesday && OnWednesday && OnThursday && OnFriday)
			return false;

		GregorianCalendar calStart = new GregorianCalendar();
		calStart.setTimeInMillis(start.getTime());
		int dayStart = calStart.get(Calendar.DAY_OF_WEEK);
		//
		GregorianCalendar calEnd = new GregorianCalendar();
		calEnd.setTimeInMillis(end.getTime());
		calEnd.add(Calendar.DAY_OF_YEAR, -1);	//	not including
		int dayEnd = calEnd.get(Calendar.DAY_OF_WEEK);

		//	On same day
		if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR)
			&& calStart.get(Calendar.MONTH) == calEnd.get(Calendar.MONTH)
			&& calStart.get(Calendar.DAY_OF_MONTH) == calEnd.get(Calendar.DAY_OF_MONTH))
		{
			if ((OnSaturday && dayStart == Calendar.SATURDAY)
				|| (OnSunday && dayStart == Calendar.SUNDAY)
				|| (OnMonday && dayStart == Calendar.MONDAY)
				|| (OnTuesday && dayStart == Calendar.TUESDAY)
				|| (OnWednesday && dayStart == Calendar.WEDNESDAY)
				|| (OnThursday && dayStart == Calendar.THURSDAY)
				|| (OnFriday && dayStart == Calendar.FRIDAY))
			{
				return true;
			}
			return false;
		}

		//	Calendar.SUNDAY=1 ... SATURDAY=7
		BitSet days = new BitSet (8);
		//	Set covered days in BitArray
		if (dayEnd <= dayStart)
			dayEnd += 7;
		for (int i = dayStart; i <= dayEnd; i++)
		{
			int index = i;
			if (index > 7)
				index -= 7;
			days.set(index);
		}

		//	Compare days to availability
		if ((OnSaturday && days.get(Calendar.SATURDAY))
			|| (OnSunday && days.get(Calendar.SUNDAY))
			|| (OnMonday && days.get(Calendar.MONDAY))
			|| (OnTuesday && days.get(Calendar.TUESDAY))
			|| (OnWednesday && days.get(Calendar.WEDNESDAY))
			|| (OnThursday && days.get(Calendar.THURSDAY))
			|| (OnFriday && days.get(Calendar.FRIDAY)))
		{
			return true;
		}

		return false;
	}	//	inRange

	/**
	 * 	Is it the same day
	 * 	@param one day
	 * 	@param two compared day
	 * 	@return true if one and two is same day
	 */
	static public boolean isSameDay (Timestamp one, Timestamp two)
	{
		GregorianCalendar calOne = new GregorianCalendar();
		if (one != null)
			calOne.setTimeInMillis(one.getTime());
		GregorianCalendar calTwo = new GregorianCalendar();
		if (two != null)
			calTwo.setTimeInMillis(two.getTime());
		if (calOne.get(Calendar.YEAR) == calTwo.get(Calendar.YEAR)
			&& calOne.get(Calendar.MONTH) == calTwo.get(Calendar.MONTH)
			&& calOne.get(Calendar.DAY_OF_MONTH) == calTwo.get(Calendar.DAY_OF_MONTH))
			return true;
		return false;
	}	//	isSameDay

	/**
	 * 	Is it the same day and same hour
	 * 	@param one day/time
	 * 	@param two compared day/time
	 * 	@return true if one and two is same day and same hour
	 */
	static public boolean isSameHour (Timestamp one, Timestamp two)
	{
		GregorianCalendar calOne = new GregorianCalendar();
		if (one != null)
			calOne.setTimeInMillis(one.getTime());
		GregorianCalendar calTwo = new GregorianCalendar();
		if (two != null)
			calTwo.setTimeInMillis(two.getTime());
		if (calOne.get(Calendar.YEAR) == calTwo.get(Calendar.YEAR)
			&& calOne.get(Calendar.MONTH) == calTwo.get(Calendar.MONTH)
			&& calOne.get(Calendar.DAY_OF_MONTH) == calTwo.get(Calendar.DAY_OF_MONTH)
			&& calOne.get(Calendar.HOUR_OF_DAY) == calTwo.get(Calendar.HOUR_OF_DAY))
			return true;
		return false;
	}	//	isSameHour

	/**
	 * 	Is all day
	 * 	@param start start date
	 * 	@param end end date
	 * 	@return true if all day (00:00-00:00 next day)
	 */
	static public boolean isAllDay (Timestamp start, Timestamp end)
	{
		GregorianCalendar calStart = new GregorianCalendar();
		calStart.setTimeInMillis(start.getTime());
		GregorianCalendar calEnd = new GregorianCalendar();
		calEnd.setTimeInMillis(end.getTime());
		if (calStart.get(Calendar.HOUR_OF_DAY) == calEnd.get(Calendar.HOUR_OF_DAY)
			&& calStart.get(Calendar.MINUTE) == calEnd.get(Calendar.MINUTE)
			&& calStart.get(Calendar.SECOND) == calEnd.get(Calendar.SECOND)
			&& calStart.get(Calendar.MILLISECOND) == calEnd.get(Calendar.MILLISECOND)
			&& calStart.get(Calendar.HOUR_OF_DAY) == 0
			&& calStart.get(Calendar.MINUTE) == 0
			&& calStart.get(Calendar.SECOND) == 0
			&& calStart.get(Calendar.MILLISECOND) == 0
			&& start.before(end))
			return true;
		//
		return false;
	}	//	isAllDay

	/**
	 * 	Calculate the number of days between start and end.
	 * 	@param start start date
	 * 	@param end end date
	 * 	@return number of days (0 = same)
	 */
	static public int getDaysBetween (Timestamp start, Timestamp end)
	{
		boolean negative = false;
		if (end.before(start))
		{
			negative = true;
			Timestamp temp = start;
			start = end;
			end = temp;
		}
		//
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(start);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		GregorianCalendar calEnd = new GregorianCalendar();
		calEnd.setTime(end);
		calEnd.set(Calendar.HOUR_OF_DAY, 0);
		calEnd.set(Calendar.MINUTE, 0);
		calEnd.set(Calendar.SECOND, 0);
		calEnd.set(Calendar.MILLISECOND, 0);

		//	in same year
		if (cal.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR))
		{
			if (negative)
				return (calEnd.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR)) * -1;
			return calEnd.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR);
		}

		//	not very efficient, but correct
		int counter = 0;
		while (calEnd.after(cal))
		{
			cal.add (Calendar.DAY_OF_YEAR, 1);
			counter++;
		}
		if (negative)
			return counter * -1;
		return counter;
	}	//	getDaysBetween

	/**
	 * 	Return Day + offset (truncate the time portion)
	 * 	@param day Day
	 * 	@param offset day offset
	 * 	@return Day + offset at 00:00
	 */
	static public Timestamp addDays (Timestamp day, int offset)
	{
		if (offset == 0)
		{
			return day;
		}
		if (day == null)
		{
			day = new Timestamp(System.currentTimeMillis());
		}
		//
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(day);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (offset == 0)
			return new Timestamp (cal.getTimeInMillis());
		cal.add(Calendar.DAY_OF_YEAR, offset);			//	may have a problem with negative (before 1/1)
		return new Timestamp (cal.getTimeInMillis());
	}	//	addDays

	/**
	 * 	Return DateTime + offset in minutes
	 * 	@param dateTime Date and Time
	 * 	@param offset minute offset
	 * 	@return dateTime + offset in minutes
	 */
	static public Timestamp addMinutess (Timestamp dateTime, int offset)
	{
		if (dateTime == null)
			dateTime = new Timestamp(System.currentTimeMillis());
		if (offset == 0)
			return dateTime;
		//
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(dateTime);
		cal.add(Calendar.MINUTE, offset);			//	may have a problem with negative
		return new Timestamp (cal.getTimeInMillis());
	}	//	addMinutes

	/**
	 * 	Format Elapsed Time
	 * 	@param start start time or null for now
	 * 	@param end end time or null for now
	 * 	@return formatted elapsed time string 1'23:59:59.999
	 */
	public static String formatElapsed (Timestamp start, Timestamp end)
	{
		long startTime = 0;
		if (start == null)
			startTime = System.currentTimeMillis();
		else
			startTime = start.getTime();
		//
		long endTime = 0;
		if (end == null)
			endTime = System.currentTimeMillis();
		else
			endTime = end.getTime();
		return formatElapsed(endTime-startTime);
	}	//	formatElapsed

	/**
	 * 	Format Elapsed Time until now
	 * 	@param start start time
	 *	@return formatted elapsed time string 1'23:59:59.999
	 */
	public static String formatElapsed (Timestamp start)
	{
		if (start == null)
			return "NoStartTime";
		long startTime = start.getTime();
		long endTime = System.currentTimeMillis();
		return formatElapsed(endTime-startTime);
	}	//	formatElapsed

	/**
	 * 	Format Elapsed Time
	 *	@param elapsedMS elapsed time in ms
	 *	@return formatted elapsed time string 1'23:59:59.999 - d'hh:mm:ss.xxx
	 */
	public static String formatElapsed (long elapsedMS)
	{
		if (elapsedMS == 0)
			return "0";
		StringBuilder sb = new StringBuilder();
		if (elapsedMS < 0)
		{
			elapsedMS = - elapsedMS;
			sb.append("-");
		}
		//
		long miliSeconds = elapsedMS%1000;
		elapsedMS = elapsedMS / 1000;
		long seconds = elapsedMS%60;
		elapsedMS = elapsedMS / 60;
		long minutes = elapsedMS%60;
		elapsedMS = elapsedMS / 60;
		long hours = elapsedMS%24;
		long days = elapsedMS / 24;
		//
		sb.append(days).append("'");
		//	hh
		if (hours != 0)
			sb.append(get2digits(hours)).append(":");
		else
			sb.append("00:");
		//	mm
		if (minutes != 0)
			sb.append(get2digits(minutes)).append(":");
		else
			sb.append("00:");
		//	ss
		sb.append(get2digits(seconds))
			.append(".").append(miliSeconds);
		return sb.toString();
	}	//	formatElapsed

	/**
	 * 	Get Minimum of 2 digits
	 *	@param no number
	 *	@return String
	 */
	private static String get2digits (long no)
	{
		String s = String.valueOf(no);
		if (s.length() > 1)
			return s;
		return "0" + s;
	}	//	get2digits

	/**
	 * 	Is today a valid date ?
	 *	@param validFrom valid from
	 *	@param validTo valid to
	 *	@return true if today is between validFrom and validTo
	 */
	public static boolean isValid (Timestamp validFrom, Timestamp validTo)
	{
		return isValid (validFrom, validTo, new Timestamp (System.currentTimeMillis()));
	}	//	isValid

	/**
	 * 	Is it valid on test date
	 *	@param validFrom valid from
	 *	@param validTo valid to
	 *	@param testDate Date
	 *  @return true if testDate is null or between validFrom and validTo
	 */
	public static boolean isValid (Timestamp validFrom, Timestamp validTo, Timestamp testDate)
	{
		if (testDate == null)
			return true;
		if (validFrom == null && validTo == null)
			return true;
		//	(validFrom)	ok
		if (validFrom != null && validFrom.after(testDate))
			return false;
		//	ok	(validTo)
		if (validTo != null && validTo.before(testDate))
			return false;
		return true;
	}	//	isValid
	
	/**
	 * 	Get the greater of ts1 and ts2
	 *	@param ts1 p1
	 *	@param ts2 p2
	 *	@return the greater of ts1 and ts2
	 */
	public static Timestamp max (Timestamp ts1, Timestamp ts2)
	{
		if (ts1 == null)
			return ts2;
		if (ts2 == null)
			return ts1;
		
		if (ts2.after(ts1))
			return ts2;
		return ts1;
	}	//	max

	/** Truncate Day - D			*/
	public static final String	TRUNC_DAY = "D";
	/** Truncate Week - W			*/
	public static final String	TRUNC_WEEK = "W";
	/** Truncate Month - MM			*/
	public static final String	TRUNC_MONTH = "MM";
	/** Truncate Quarter - Q		*/
	public static final String	TRUNC_QUARTER = "Q";
	/** Truncate Year - Y			*/
	public static final String	TRUNC_YEAR = "Y";
	
	/**
	 * 	Get truncated timestamp (without time)
	 *  @param dayTime day
	 *  @param trunc {@link #TRUNC_DAY}, {@link #TRUNC_WEEK}, {@link #TRUNC_MONTH}, {@link #TRUNC_QUARTER} or {@link #TRUNC_YEAR}
	 *  @return truncated timestamp (without time)
	 */
	static public Timestamp trunc (Timestamp dayTime, String trunc)
	{
		if (dayTime == null)
			dayTime = new Timestamp(System.currentTimeMillis());
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		cal.setTimeInMillis(dayTime.getTime());
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		//	D
		cal.set(Calendar.HOUR_OF_DAY, 0);
		if (trunc == null || trunc.equals(TRUNC_DAY))
			return new Timestamp (cal.getTimeInMillis());
		//	W
		if (trunc.equals(TRUNC_WEEK))
		{
			cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
			return new Timestamp (cal.getTimeInMillis());
		}
		// MM
		cal.set(Calendar.DAY_OF_MONTH, 1);
		if (trunc.equals(TRUNC_MONTH))
			return new Timestamp (cal.getTimeInMillis());
		//	Q
		if (trunc.equals(TRUNC_QUARTER))
		{
			int mm = cal.get(Calendar.MONTH);
			if (mm < Calendar.APRIL)
				mm = Calendar.JANUARY;
			else if (mm < Calendar.JULY)
				mm = Calendar.APRIL;
			else if (mm < Calendar.OCTOBER)
				mm = Calendar.JULY;
			else
				mm = Calendar.OCTOBER;
			cal.set(Calendar.MONTH, mm);
			return new Timestamp (cal.getTimeInMillis());
		}
		cal.set(Calendar.DAY_OF_YEAR, 1);
		return new Timestamp (cal.getTimeInMillis());
	}	//	trunc
	
	/**
	 * Returns timestamp by combining the date part from dateTime and time part form timeSlot.<br/>
	 * If timeSlot is null, then first millisecond of the day will be used (if end == false)
	 * or last millisecond of the day (if end == true).
	 * 
	 * @param dateTime
	 * @param timeSlot
	 * @param end
	 * @return {@link Timestamp}
	 */
	public static Timestamp getDayBorder(Timestamp dateTime, Timestamp timeSlot, boolean end)
	{
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(dateTime.getTime());
		dateTime.setNanos(0);

		if(timeSlot != null)
		{
			timeSlot.setNanos(0);
			GregorianCalendar gcTS = new GregorianCalendar();
			gcTS.setTimeInMillis(timeSlot.getTime());

			gc.set(Calendar.HOUR_OF_DAY, gcTS.get(Calendar.HOUR_OF_DAY));
			gc.set(Calendar.MINUTE, gcTS.get(Calendar.MINUTE));
			gc.set(Calendar.SECOND, gcTS.get(Calendar.SECOND));
			gc.set(Calendar.MILLISECOND, gcTS.get(Calendar.MILLISECOND));
		} 
		else if(end)
		{
			gc.set(Calendar.HOUR_OF_DAY, 23);
			gc.set(Calendar.MINUTE, 59);
			gc.set(Calendar.SECOND, 59);
			gc.set(Calendar.MILLISECOND, 999);
		}
		else
		{
			gc.set(Calendar.MILLISECOND, 0);
			gc.set(Calendar.SECOND, 0);
			gc.set(Calendar.MINUTE, 0);
			gc.set(Calendar.HOUR_OF_DAY, 0);
		}
		return new Timestamp(gc.getTimeInMillis());
	}

	/**
	 * [ ARHIPAC ] Gets calendar instance of given date
	 * @param date calendar initialization date; if null, the current date is used
	 * @return calendar
	 */
	static public Calendar getCalendar(Timestamp date)
	{
		GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		if (date != null) {
			cal.setTimeInMillis(date.getTime());
		}
		return cal;
	}
	
	/**
	 * [ ARHIPAC ] Get first date in month
	 * @param day day; if null current time will be used
	 * @return first day of the month (time will be 00:00)
	 */
	static public Timestamp getMonthFirstDay (Timestamp day)
	{
		if (day == null)
			day = new Timestamp(System.currentTimeMillis());
		Calendar cal = getCalendar(day);
		cal.setTimeInMillis(day.getTime());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		//
		cal.set(Calendar.DAY_OF_MONTH, 1);	//	first
		return new Timestamp (cal.getTimeInMillis());
	}	//	getMonthFirstDay
	
	/**
	 * [ ARHIPAC ] Return Day + offset (truncate the time portion)
	 * @param day Day; if null current time will be used
	 * @param offset months offset
	 * @return Day + offset (time will be 00:00)
	 */
	static public Timestamp addMonths (Timestamp day, int offset)
	{
		if (day == null)
			day = new Timestamp(System.currentTimeMillis());
		//
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(day);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (offset == 0)
			return new Timestamp (cal.getTimeInMillis());
		cal.add(Calendar.MONTH, offset);
		return new Timestamp (cal.getTimeInMillis());
	}	//	addMonths
	
	/**
	 * Get number of months between start and end
	 * @param start
	 * @param end
	 * @return number of months between start and end
	 */
	public static int getMonthsBetween (Timestamp start, Timestamp end)
	{
		Calendar startCal = getCalendar(start);
		Calendar endCal = getCalendar(end);
		//
		return endCal.get(Calendar.YEAR) * 12 + endCal.get(Calendar.MONTH)
				- (startCal.get(Calendar.YEAR) * 12 + startCal.get(Calendar.MONTH));
	}

	/**
	 * Add n days to startDate, skipping non business day.
	 * @param startDate
	 * @param nbDays number of days
	 * @param clientID AD_Client_ID
	 * @param trxName
	 * @return start date + nbDays which cannot be saturday or sunday or non business days
	 */
	public static Timestamp addOnlyBusinessDays(Timestamp startDate, int nbDays, int clientID, String trxName)
	{
		Timestamp retValue = startDate;
		while (nbDays > 0) {
			retValue = TimeUtil.addDays(retValue, 1);
			StringBuilder sql = new StringBuilder("SELECT nextBusinessDay(?,?) FROM DUAL");
			retValue = DB.getSQLValueTSEx(trxName, sql.toString(), retValue, clientID);
			nbDays--;
		}
		return retValue;
	}

	/**
	 * Get number of business day between startDate and endDate
	 * @param startDate
	 * @param endDate (not inclusive)
	 * @param clientID
	 * @param trxName
	 * @return number of business days between 2 dates for the country based on current default country
	 */
	public static int getBusinessDaysBetween(Timestamp startDate, Timestamp endDate, int clientID, String trxName)
	{
		return getBusinessDaysBetween(startDate, endDate, clientID, false, trxName);
	}

	/**
	 * Get number of business day between startDate and endDate
	 * @param startDate
	 * @param endDate
	 * @param clientID
	 * @param includeEndDate
	 * @param trxName
	 * @return number of business days between 2 dates for the country based on current default country
	 */
	public static int getBusinessDaysBetween(Timestamp startDate, Timestamp endDate, int clientID, boolean includeEndDate, String trxName)
	{
		return getBusinessDaysBetween(startDate, endDate, clientID, MCountry.getDefault().getC_Country_ID(), includeEndDate, trxName);
	}
	
	/**
	 * Get number of business day between startDate and endDate
	 * @param startDate
	 * @param endDate (not inclusive)
	 * @param clientID
	 * @param countryID
	 * @param trxName
	 * @return number of business days between 2 dates for a specified country
	 */
	public static int getBusinessDaysBetween(Timestamp startDate, Timestamp endDate, int clientID, int countryID, String trxName)
	{
		return getBusinessDaysBetween(startDate, endDate, clientID, countryID, false, trxName);
	}
	
	/**
	 * Get number of business day between startDate and endDate
	 * @param startDate
	 * @param endDate
	 * @param clientID
	 * @param countryID
	 * @param includeEndDate
	 * @param trxName
	 * @return number of business days between 2 dates for a specified country, with ability to include the end date in the count
	 */
	public static int getBusinessDaysBetween(Timestamp startDate, Timestamp endDate, int clientID, int countryID, boolean includeEndDate, String trxName)
	{
		int retValue = 0;

		if (startDate.equals(endDate))
			return 0;

		boolean negative = false;
		if (endDate.before(startDate)) {
			negative = true;
			Timestamp temp = startDate;
			startDate = endDate;
			endDate = temp;
		}

		final String sql = "SELECT Date1 FROM C_NonBusinessDay WHERE IsActive='Y' AND AD_Client_ID=? AND Date1 BETWEEN ? AND ? AND COALESCE(C_Country_ID,0) IN (0, ?)";
		List<Object> nbd = DB.getSQLValueObjectsEx(trxName, sql, clientID, startDate, endDate, countryID);

		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(startDate);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		GregorianCalendar calEnd = new GregorianCalendar();
		calEnd.setTime(endDate);
		calEnd.set(Calendar.HOUR_OF_DAY, 0);
		calEnd.set(Calendar.MINUTE, 0);
		calEnd.set(Calendar.SECOND, 0);
		calEnd.set(Calendar.MILLISECOND, 0);

		while (cal.before(calEnd) || (includeEndDate && cal.equals(calEnd))) {
			if (nbd == null || !nbd.contains(new Timestamp(cal.getTimeInMillis()))) {
				if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
					retValue++;
				}
			}
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}

		if (negative)
			retValue = retValue * -1;
		return retValue;
	}

}	//	TimeUtil
