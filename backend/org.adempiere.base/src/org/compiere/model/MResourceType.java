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
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Resource Type Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MResourceType.java,v 1.2 2006/07/30 00:51:03 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 				<li>FR [ 2051056 ] MResource[Type] should be cached
 * 				<li>added manufacturing related methods (getDayStart, getDayEnd etc)
 * 				<li>BF [ 2431049 ] If Time Slot then Time Slot Start/End should be mandatory
 */
public class MResourceType extends X_S_ResourceType implements ImmutablePOSupport
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = 1738229111191830237L;
	/** Cache */
	private static ImmutableIntPOCache<Integer, MResourceType> s_cache = new ImmutableIntPOCache<Integer, MResourceType>(Table_Name, 20);
	
	/**
	 * Get from Cache (immutable)
	 * @param S_ResourceType_ID
	 * @return MResourceType
	 */
	public static MResourceType get(int S_ResourceType_ID) 
	{
		return get(Env.getCtx(), S_ResourceType_ID);
	}
	
	/**
	 * Get from Cache (immutable)
	 * @param ctx
	 * @param S_ResourceType_ID
	 * @return MResourceType
	 */
	public static MResourceType get(Properties ctx, int S_ResourceType_ID) 
	{
		if (S_ResourceType_ID <= 0)
			return null;
		
		MResourceType type = s_cache.get(ctx, S_ResourceType_ID, e -> new MResourceType(ctx, e));
		if (type == null) {
			type = new MResourceType(ctx, S_ResourceType_ID, (String)null);
			if (type.get_ID() == S_ResourceType_ID) {
				s_cache.put(S_ResourceType_ID, type, e -> new MResourceType(Env.getCtx(), e));
				return type;
			}
			return null;
		}
		return type;
	}
	
	/**
	 * Get updateable copy of MResourceType from cache
	 * @param ctx
	 * @param S_ResourceType_ID
	 * @param trxName
	 * @return MResourceType 
	 */
	public static MResourceType getCopy(Properties ctx, int S_ResourceType_ID, String trxName)
	{
		MResourceType rt = get(S_ResourceType_ID);
		if (rt != null)
			rt = new MResourceType(ctx, rt, trxName);
		return rt;
	}
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param S_ResourceType_UU  UUID key
     * @param trxName Transaction
     */
    public MResourceType(Properties ctx, String S_ResourceType_UU, String trxName) {
        super(ctx, S_ResourceType_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param S_ResourceType_ID id
	 */
	public MResourceType (Properties ctx, int S_ResourceType_ID, String trxName)
	{
		super (ctx, S_ResourceType_ID, trxName);
	}	//	MResourceType

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 */
	public MResourceType (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MResourceType
	
	/**
	 * Copy constructor
	 * @param copy
	 */
	public MResourceType(MResourceType copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MResourceType(Properties ctx, MResourceType copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MResourceType(Properties ctx, MResourceType copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		// Validate TimeSlotEnd > TimeSlotStart and both is fill
		if (isTimeSlot())
		{
			Timestamp start = getTimeSlotStart();
			if (start == null)
				throw new FillMandatoryException(COLUMNNAME_TimeSlotStart);
			Timestamp end = getTimeSlotEnd();
			if (end == null)
				throw new FillMandatoryException(COLUMNNAME_TimeSlotEnd);
			if (start.compareTo(end) >= 0)
			{
				throw new AdempiereException("@TimeSlotStart@ > @TimeSlotEnd@"); 
			}
		}
		return true;
	}

	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return false;
		
		//	Update Resource Products
		if (!newRecord)
		{
			MProduct[] products = MProduct.get(getCtx(), "S_Resource_ID IN "
				+ "(SELECT S_Resource_ID FROM S_Resource WHERE S_ResourceType_ID=" 
				+ getS_ResourceType_ID() + ")", get_TrxName());
			for (int i = 0; i < products.length; i++)
			{
				MProduct product = products[i];
				if (product.setResource(this))
				{
					product.saveEx(get_TrxName());
				}
			}
		}
		
		return success;
	}	//	afterSave
	
	/**
	 * @param date
	 * @return day start time stamp
	 */
	public Timestamp getDayStart(Timestamp date)
	{
		if(isTimeSlot())
		{
			return TimeUtil.getDayBorder(date, getTimeSlotStart(), false);
		}
		else
		{
			return TimeUtil.getDayBorder(date, null, false);
		}
	}
	
	/**
	 * @param date
	 * @return day end time stamp
	 */
	public Timestamp getDayEnd(Timestamp date)
	{
		if(isTimeSlot())
		{
			return TimeUtil.getDayBorder(date, getTimeSlotEnd(), true);
		}
		else
		{
			return TimeUtil.getDayBorder(date, null, true);
		}
	}
	
	/**
	 * @return duration of a day in millisecond
	 */
	public long getDayDurationMillis()
	{
		if (isTimeSlot())
		{
			return getTimeSlotEnd().getTime() - getTimeSlotStart().getTime();
		}
		else
		{
			return 24*60*60*1000; // 24 hours
		}
	}

	/**
	 * Get how many hours/day is available.
	 * Minutes, seconds and millisecond are discarded.  
	 * @return available hours
	 */
	public int getTimeSlotHours()
	{
		long hours;
		if (isTimeSlot())                			
			hours = (getTimeSlotEnd().getTime() - getTimeSlotStart().getTime()) / (60 * 60 * 1000);
		else 
			hours  = 24;
		return (int) hours;
	}
	
	/**
	 * Get available days / week.
	 * @return available days / week
	 */
	public int getAvailableDaysWeek()
	{
		int availableDays = 0;
		if (isDateSlot())
		{
			if (isOnMonday())
				availableDays += 1; 
			if (isOnTuesday())
				availableDays += 1;
			if (isOnThursday())
				availableDays += 1;
			if (isOnWednesday())	
				availableDays += 1;
			if (isOnFriday())	 
				availableDays += 1;
			if (isOnSaturday())	
				availableDays += 1;
			if (isOnSunday())
				availableDays += 1;
		}
		else
		{
			availableDays = 7;
		}
		return availableDays;
	}
	
	/**
	 * @param dateTime
	 * @return true if given day is available
	 */
	public boolean isDayAvailable(Timestamp dateTime)
	{
		if (!isActive())
		{
			return false;
		}
		if(!isDateSlot())
		{
			return true;
		}

		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(dateTime.getTime());

		boolean retValue = false;
		switch(gc.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.SUNDAY:
			retValue = isOnSunday();
			break;

		case Calendar.MONDAY:
			retValue = isOnMonday();
			break;

		case Calendar.TUESDAY:
			retValue = isOnTuesday();
			break;

		case Calendar.WEDNESDAY:
			retValue = isOnWednesday();
			break;

		case Calendar.THURSDAY:
			retValue = isOnThursday();
			break;

		case Calendar.FRIDAY:
			retValue = isOnFriday();
			break;

		case Calendar.SATURDAY:
			retValue = isOnSaturday();	
			break;
		} 

		return retValue;
	}

	/**
	 * @return true if a resource of this type is generally available
	 * 			(active, at least 1 day available and at least 1 hour available) 
	 */
	public boolean isAvailable()
	{
		if (!isActive())
		{
			return false;
		}
		return getAvailableDaysWeek() > 0
				&& getTimeSlotHours() > 0;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("MResourceType[")
			.append(get_ID())
			.append(",Value=").append(getValue())
			.append(",Name=").append(getName());
		if (isTimeSlot())
		{
			SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
			sb.append(",TimeSlot=");
			Timestamp start = getTimeSlotStart();
			Timestamp end = getTimeSlotEnd();
			sb.append(start != null ? df.format(start) : " - ");
			sb.append("-");
			sb.append(end != null ? df.format(end) : " - ");
		}
		if (isDateSlot())
		{
			sb.append(",DaySlot=")
				.append(isOnMonday()	?	"M" : "-")
				.append(isOnTuesday()	?	"T" : "-")
				.append(isOnWednesday()	?	"W" : "-")
				.append(isOnThursday()	?	"T" : "-")
				.append(isOnFriday()	?	"F" : "-")
				.append(isOnSaturday()	?	"S" : "-")
				.append(isOnSunday()	?	"S" : "-");
		}
		return sb.append("]").toString();
	}
	
	@Override
	public MResourceType markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MResourceType
