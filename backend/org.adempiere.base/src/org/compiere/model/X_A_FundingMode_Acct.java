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

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for A_FundingMode_Acct
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="A_FundingMode_Acct")
public class X_A_FundingMode_Acct extends PO implements I_A_FundingMode_Acct, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241222L;

    /** Standard Constructor */
    public X_A_FundingMode_Acct (Properties ctx, int A_FundingMode_Acct_ID, String trxName)
    {
      super (ctx, A_FundingMode_Acct_ID, trxName);
      /** if (A_FundingMode_Acct_ID == 0)
        {
			setA_FundingMode_Acct (0);
			setA_FundingMode_ID (0);
			setC_AcctSchema_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_A_FundingMode_Acct (Properties ctx, int A_FundingMode_Acct_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, A_FundingMode_Acct_ID, trxName, virtualColumns);
      /** if (A_FundingMode_Acct_ID == 0)
        {
			setA_FundingMode_Acct (0);
			setA_FundingMode_ID (0);
			setC_AcctSchema_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_A_FundingMode_Acct (Properties ctx, String A_FundingMode_Acct_UU, String trxName)
    {
      super (ctx, A_FundingMode_Acct_UU, trxName);
      /** if (A_FundingMode_Acct_UU == null)
        {
			setA_FundingMode_Acct (0);
			setA_FundingMode_ID (0);
			setC_AcctSchema_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_A_FundingMode_Acct (Properties ctx, String A_FundingMode_Acct_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, A_FundingMode_Acct_UU, trxName, virtualColumns);
      /** if (A_FundingMode_Acct_UU == null)
        {
			setA_FundingMode_Acct (0);
			setA_FundingMode_ID (0);
			setC_AcctSchema_ID (0);
        } */
    }

    /** Load Constructor */
    public X_A_FundingMode_Acct (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
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
      StringBuilder sb = new StringBuilder ("X_A_FundingMode_Acct[")
        .append(get_UUID()).append("]");
      return sb.toString();
    }

	public I_C_ValidCombination getA_FundingMode_A() throws RuntimeException
	{
		return (I_C_ValidCombination)MTable.get(getCtx(), I_C_ValidCombination.Table_ID)
			.getPO(getA_FundingMode_Acct(), get_TrxName());
	}

	/** Set Funding Mode Account.
		@param A_FundingMode_Acct Funding Mode Account
	*/
	public void setA_FundingMode_Acct (int A_FundingMode_Acct)
	{
		set_Value (COLUMNNAME_A_FundingMode_Acct, Integer.valueOf(A_FundingMode_Acct));
	}

	/** Get Funding Mode Account.
		@return Funding Mode Account	  */
	public int getA_FundingMode_Acct()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_A_FundingMode_Acct);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set A_FundingMode_Acct_UU.
		@param A_FundingMode_Acct_UU A_FundingMode_Acct_UU
	*/
	public void setA_FundingMode_Acct_UU (String A_FundingMode_Acct_UU)
	{
		set_Value (COLUMNNAME_A_FundingMode_Acct_UU, A_FundingMode_Acct_UU);
	}

	/** Get A_FundingMode_Acct_UU.
		@return A_FundingMode_Acct_UU	  */
	public String getA_FundingMode_Acct_UU()
	{
		return (String)get_Value(COLUMNNAME_A_FundingMode_Acct_UU);
	}

	public org.compiere.model.I_A_FundingMode getA_FundingMode() throws RuntimeException
	{
		return (org.compiere.model.I_A_FundingMode)MTable.get(getCtx(), org.compiere.model.I_A_FundingMode.Table_ID)
			.getPO(getA_FundingMode_ID(), get_TrxName());
	}

	/** Set Asset Funding Mode.
		@param A_FundingMode_ID Asset Funding Mode
	*/
	public void setA_FundingMode_ID (int A_FundingMode_ID)
	{
		if (A_FundingMode_ID < 1)
			set_ValueNoCheck (COLUMNNAME_A_FundingMode_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_A_FundingMode_ID, Integer.valueOf(A_FundingMode_ID));
	}

	/** Get Asset Funding Mode.
		@return Asset Funding Mode	  */
	public int getA_FundingMode_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_A_FundingMode_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_AcctSchema getC_AcctSchema() throws RuntimeException
	{
		return (org.compiere.model.I_C_AcctSchema)MTable.get(getCtx(), org.compiere.model.I_C_AcctSchema.Table_ID)
			.getPO(getC_AcctSchema_ID(), get_TrxName());
	}

	/** Set Accounting Schema.
		@param C_AcctSchema_ID Rules for accounting
	*/
	public void setC_AcctSchema_ID (int C_AcctSchema_ID)
	{
		if (C_AcctSchema_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_AcctSchema_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_AcctSchema_ID, Integer.valueOf(C_AcctSchema_ID));
	}

	/** Get Accounting Schema.
		@return Rules for accounting
	  */
	public int getC_AcctSchema_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_AcctSchema_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}