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

/** Generated Model for AD_UserBPAccess
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="AD_UserBPAccess")
public class X_AD_UserBPAccess extends PO implements I_AD_UserBPAccess, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241222L;

    /** Standard Constructor */
    public X_AD_UserBPAccess (Properties ctx, int AD_UserBPAccess_ID, String trxName)
    {
      super (ctx, AD_UserBPAccess_ID, trxName);
      /** if (AD_UserBPAccess_ID == 0)
        {
			setAD_UserBPAccess_ID (0);
			setAD_User_ID (0);
			setBPAccessType (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_UserBPAccess (Properties ctx, int AD_UserBPAccess_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_UserBPAccess_ID, trxName, virtualColumns);
      /** if (AD_UserBPAccess_ID == 0)
        {
			setAD_UserBPAccess_ID (0);
			setAD_User_ID (0);
			setBPAccessType (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_UserBPAccess (Properties ctx, String AD_UserBPAccess_UU, String trxName)
    {
      super (ctx, AD_UserBPAccess_UU, trxName);
      /** if (AD_UserBPAccess_UU == null)
        {
			setAD_UserBPAccess_ID (0);
			setAD_User_ID (0);
			setBPAccessType (null);
        } */
    }

    /** Standard Constructor */
    public X_AD_UserBPAccess (Properties ctx, String AD_UserBPAccess_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, AD_UserBPAccess_UU, trxName, virtualColumns);
      /** if (AD_UserBPAccess_UU == null)
        {
			setAD_UserBPAccess_ID (0);
			setAD_User_ID (0);
			setBPAccessType (null);
        } */
    }

    /** Load Constructor */
    public X_AD_UserBPAccess (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 2 - Client
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
      StringBuilder sb = new StringBuilder ("X_AD_UserBPAccess[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set User BP Access.
		@param AD_UserBPAccess_ID User/contact access to Business Partner information and resources
	*/
	public void setAD_UserBPAccess_ID (int AD_UserBPAccess_ID)
	{
		if (AD_UserBPAccess_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_UserBPAccess_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_UserBPAccess_ID, Integer.valueOf(AD_UserBPAccess_ID));
	}

	/** Get User BP Access.
		@return User/contact access to Business Partner information and resources
	  */
	public int getAD_UserBPAccess_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_UserBPAccess_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set AD_UserBPAccess_UU.
		@param AD_UserBPAccess_UU AD_UserBPAccess_UU
	*/
	public void setAD_UserBPAccess_UU (String AD_UserBPAccess_UU)
	{
		set_Value (COLUMNNAME_AD_UserBPAccess_UU, AD_UserBPAccess_UU);
	}

	/** Get AD_UserBPAccess_UU.
		@return AD_UserBPAccess_UU	  */
	public String getAD_UserBPAccess_UU()
	{
		return (String)get_Value(COLUMNNAME_AD_UserBPAccess_UU);
	}

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_Value (COLUMNNAME_AD_User_ID, null);
		else
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** BPAccessType AD_Reference_ID=358 */
	public static final int BPACCESSTYPE_AD_Reference_ID=358;
	/** Assets, Download = A */
	public static final String BPACCESSTYPE_AssetsDownload = "A";
	/** Business Documents = B */
	public static final String BPACCESSTYPE_BusinessDocuments = "B";
	/** Requests = R */
	public static final String BPACCESSTYPE_Requests = "R";
	/** Set Access Type.
		@param BPAccessType Type of Access of the user/contact to Business Partner information and resources
	*/
	public void setBPAccessType (String BPAccessType)
	{

		set_Value (COLUMNNAME_BPAccessType, BPAccessType);
	}

	/** Get Access Type.
		@return Type of Access of the user/contact to Business Partner information and resources
	  */
	public String getBPAccessType()
	{
		return (String)get_Value(COLUMNNAME_BPAccessType);
	}

	/** DocBaseType AD_Reference_ID=183 */
	public static final int DOCBASETYPE_AD_Reference_ID=183;
	/** AP Credit Memo = APC */
	public static final String DOCBASETYPE_APCreditMemo = "APC";
	/** AP Invoice = API */
	public static final String DOCBASETYPE_APInvoice = "API";
	/** AP Payment = APP */
	public static final String DOCBASETYPE_APPayment = "APP";
	/** AR Credit Memo = ARC */
	public static final String DOCBASETYPE_ARCreditMemo = "ARC";
	/** AR Pro Forma Invoice = ARF */
	public static final String DOCBASETYPE_ARProFormaInvoice = "ARF";
	/** AR Invoice = ARI */
	public static final String DOCBASETYPE_ARInvoice = "ARI";
	/** AR Receipt = ARR */
	public static final String DOCBASETYPE_ARReceipt = "ARR";
	/** Payment Allocation = CMA */
	public static final String DOCBASETYPE_PaymentAllocation = "CMA";
	/** Bank Statement = CMB */
	public static final String DOCBASETYPE_BankStatement = "CMB";
	/** Cash Journal = CMC */
	public static final String DOCBASETYPE_CashJournal = "CMC";
	/** Distribution Order = DOO */
	public static final String DOCBASETYPE_DistributionOrder = "DOO";
	/** Fixed Assets Addition = FAA */
	public static final String DOCBASETYPE_FixedAssetsAddition = "FAA";
	/** Fixed Assets Disposal = FAD */
	public static final String DOCBASETYPE_FixedAssetsDisposal = "FAD";
	/** Fixed Assets Depreciation = FDP */
	public static final String DOCBASETYPE_FixedAssetsDepreciation = "FDP";
	/** GL Document = GLD */
	public static final String DOCBASETYPE_GLDocument = "GLD";
	/** GL Journal = GLJ */
	public static final String DOCBASETYPE_GLJournal = "GLJ";
	/** Payroll = HRP */
	public static final String DOCBASETYPE_Payroll = "HRP";
	/** Manufacturing Cost Collector = MCC */
	public static final String DOCBASETYPE_ManufacturingCostCollector = "MCC";
	/** Material Physical Inventory = MMI */
	public static final String DOCBASETYPE_MaterialPhysicalInventory = "MMI";
	/** Material Movement = MMM */
	public static final String DOCBASETYPE_MaterialMovement = "MMM";
	/** Material Production = MMP */
	public static final String DOCBASETYPE_MaterialProduction = "MMP";
	/** Material Receipt = MMR */
	public static final String DOCBASETYPE_MaterialReceipt = "MMR";
	/** Material Delivery = MMS */
	public static final String DOCBASETYPE_MaterialDelivery = "MMS";
	/** Maintenance Order = MOF */
	public static final String DOCBASETYPE_MaintenanceOrder = "MOF";
	/** Manufacturing Order = MOP */
	public static final String DOCBASETYPE_ManufacturingOrder = "MOP";
	/** Quality Order = MQO */
	public static final String DOCBASETYPE_QualityOrder = "MQO";
	/** Match Invoice = MXI */
	public static final String DOCBASETYPE_MatchInvoice = "MXI";
	/** Match PO = MXP */
	public static final String DOCBASETYPE_MatchPO = "MXP";
	/** Project Issue = PJI */
	public static final String DOCBASETYPE_ProjectIssue = "PJI";
	/** Purchase Order = POO */
	public static final String DOCBASETYPE_PurchaseOrder = "POO";
	/** Purchase Requisition = POR */
	public static final String DOCBASETYPE_PurchaseRequisition = "POR";
	/** Sales Order = SOO */
	public static final String DOCBASETYPE_SalesOrder = "SOO";
	/** Set Document Base Type.
		@param DocBaseType Logical type of document
	*/
	public void setDocBaseType (String DocBaseType)
	{

		set_Value (COLUMNNAME_DocBaseType, DocBaseType);
	}

	/** Get Document Base Type.
		@return Logical type of document
	  */
	public String getDocBaseType()
	{
		return (String)get_Value(COLUMNNAME_DocBaseType);
	}

	public org.compiere.model.I_R_RequestType getR_RequestType() throws RuntimeException
	{
		return (org.compiere.model.I_R_RequestType)MTable.get(getCtx(), org.compiere.model.I_R_RequestType.Table_ID)
			.getPO(getR_RequestType_ID(), get_TrxName());
	}

	/** Set Request Type.
		@param R_RequestType_ID Type of request (e.g. Inquiry, Complaint, ..)
	*/
	public void setR_RequestType_ID (int R_RequestType_ID)
	{
		if (R_RequestType_ID < 1)
			set_Value (COLUMNNAME_R_RequestType_ID, null);
		else
			set_Value (COLUMNNAME_R_RequestType_ID, Integer.valueOf(R_RequestType_ID));
	}

	/** Get Request Type.
		@return Type of request (e.g. Inquiry, Complaint, ..)
	  */
	public int getR_RequestType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_RequestType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}