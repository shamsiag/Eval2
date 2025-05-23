/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.compiere.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/** Generated Model for PA_BenchmarkData
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="PA_BenchmarkData")
public class X_PA_BenchmarkData extends PO implements I_PA_BenchmarkData, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241222L;

    /** Standard Constructor */
    public X_PA_BenchmarkData (Properties ctx, int PA_BenchmarkData_ID, String trxName)
    {
      super (ctx, PA_BenchmarkData_ID, trxName);
      /** if (PA_BenchmarkData_ID == 0)
        {
			setBenchmarkDate (new Timestamp( System.currentTimeMillis() ));
			setBenchmarkValue (Env.ZERO);
			setName (null);
			setPA_BenchmarkData_ID (0);
			setPA_Benchmark_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_PA_BenchmarkData (Properties ctx, int PA_BenchmarkData_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, PA_BenchmarkData_ID, trxName, virtualColumns);
      /** if (PA_BenchmarkData_ID == 0)
        {
			setBenchmarkDate (new Timestamp( System.currentTimeMillis() ));
			setBenchmarkValue (Env.ZERO);
			setName (null);
			setPA_BenchmarkData_ID (0);
			setPA_Benchmark_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_PA_BenchmarkData (Properties ctx, String PA_BenchmarkData_UU, String trxName)
    {
      super (ctx, PA_BenchmarkData_UU, trxName);
      /** if (PA_BenchmarkData_UU == null)
        {
			setBenchmarkDate (new Timestamp( System.currentTimeMillis() ));
			setBenchmarkValue (Env.ZERO);
			setName (null);
			setPA_BenchmarkData_ID (0);
			setPA_Benchmark_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_PA_BenchmarkData (Properties ctx, String PA_BenchmarkData_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, PA_BenchmarkData_UU, trxName, virtualColumns);
      /** if (PA_BenchmarkData_UU == null)
        {
			setBenchmarkDate (new Timestamp( System.currentTimeMillis() ));
			setBenchmarkValue (Env.ZERO);
			setName (null);
			setPA_BenchmarkData_ID (0);
			setPA_Benchmark_ID (0);
        } */
    }

    /** Load Constructor */
    public X_PA_BenchmarkData (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_PA_BenchmarkData[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Date.
		@param BenchmarkDate Benchmark Date
	*/
	public void setBenchmarkDate (Timestamp BenchmarkDate)
	{
		set_Value (COLUMNNAME_BenchmarkDate, BenchmarkDate);
	}

	/** Get Date.
		@return Benchmark Date
	  */
	public Timestamp getBenchmarkDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_BenchmarkDate);
	}

	/** Set Value.
		@param BenchmarkValue Benchmark Value
	*/
	public void setBenchmarkValue (BigDecimal BenchmarkValue)
	{
		set_Value (COLUMNNAME_BenchmarkValue, BenchmarkValue);
	}

	/** Get Value.
		@return Benchmark Value
	  */
	public BigDecimal getBenchmarkValue()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_BenchmarkValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair()
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set Benchmark Data.
		@param PA_BenchmarkData_ID Performance Benchmark Data Point
	*/
	public void setPA_BenchmarkData_ID (int PA_BenchmarkData_ID)
	{
		if (PA_BenchmarkData_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PA_BenchmarkData_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PA_BenchmarkData_ID, Integer.valueOf(PA_BenchmarkData_ID));
	}

	/** Get Benchmark Data.
		@return Performance Benchmark Data Point
	  */
	public int getPA_BenchmarkData_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PA_BenchmarkData_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set PA_BenchmarkData_UU.
		@param PA_BenchmarkData_UU PA_BenchmarkData_UU
	*/
	public void setPA_BenchmarkData_UU (String PA_BenchmarkData_UU)
	{
		set_Value (COLUMNNAME_PA_BenchmarkData_UU, PA_BenchmarkData_UU);
	}

	/** Get PA_BenchmarkData_UU.
		@return PA_BenchmarkData_UU	  */
	public String getPA_BenchmarkData_UU()
	{
		return (String)get_Value(COLUMNNAME_PA_BenchmarkData_UU);
	}

	public org.compiere.model.I_PA_Benchmark getPA_Benchmark() throws RuntimeException
	{
		return (org.compiere.model.I_PA_Benchmark)MTable.get(getCtx(), org.compiere.model.I_PA_Benchmark.Table_ID)
			.getPO(getPA_Benchmark_ID(), get_TrxName());
	}

	/** Set Benchmark.
		@param PA_Benchmark_ID Performance Benchmark
	*/
	public void setPA_Benchmark_ID (int PA_Benchmark_ID)
	{
		if (PA_Benchmark_ID < 1)
			set_ValueNoCheck (COLUMNNAME_PA_Benchmark_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_PA_Benchmark_ID, Integer.valueOf(PA_Benchmark_ID));
	}

	/** Get Benchmark.
		@return Performance Benchmark
	  */
	public int getPA_Benchmark_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PA_Benchmark_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}