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
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * 	Natural Account
 *
 *  @author Jorg Janke
 *  @version $Id: MElementValue.java,v 1.3 2006/07/30 00:58:37 jjanke Exp $
 *  
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			BF [ 1883533 ] Change to summary - valid combination issue
 * 			BF [ 2320411 ] Translate "Already posted to" message
 */
public class MElementValue extends X_C_ElementValue implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 6352667759697380460L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_ElementValue_UU  UUID key
     * @param trxName Transaction
     */
    public MElementValue(Properties ctx, String C_ElementValue_UU, String trxName) {
        super(ctx, C_ElementValue_UU, trxName);
		if (Util.isEmpty(C_ElementValue_UU))
			setInitialDefaults();
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_ElementValue_ID ID or 0 for new
	 *	@param trxName transaction
	 */
	public MElementValue(Properties ctx, int C_ElementValue_ID, String trxName)
	{
		super(ctx, C_ElementValue_ID, trxName);
		if (C_ElementValue_ID == 0)
			setInitialDefaults();
	}	//	MElementValue

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setIsSummary (false);
		setAccountSign (ACCOUNTSIGN_Natural);
		setAccountType (ACCOUNTTYPE_Expense);
		setIsDocControlled(false);
		setIsForeignCurrency(false);
		setIsBankAccount(false);
		//
		setPostActual (true);
		setPostBudget (true);
		setPostEncumbrance (true);
		setPostStatistical (true);
	}

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MElementValue(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MElementValue

	/**
	 *	@param ctx context
	 *	@param Value value
	 *	@param Name name
	 *	@param Description description
	 *	@param AccountType account type
	 *	@param AccountSign account sign
	 *	@param IsDocControlled doc controlled
	 *	@param IsSummary summary
	 *	@param trxName transaction
	 */
	public MElementValue (Properties ctx, String Value, String Name, String Description,
		String AccountType, String AccountSign,
		boolean IsDocControlled, boolean IsSummary, String trxName)
	{
		this (ctx, 0, trxName);
		setValue(Value);
		setName(Name);
		setDescription(Description);
		setAccountType(AccountType);
		setAccountSign(AccountSign);
		setIsDocControlled(IsDocControlled);
		setIsSummary(IsSummary);
	}	//	MElementValue
	
	/**
	 * 	Import Constructor
	 *	@param imp import
	 */
	public MElementValue (X_I_ElementValue imp)
	{
		this (imp.getCtx(), 0, imp.get_TrxName());
		setClientOrg(imp);
		set(imp);
	}	//	MElementValue

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MElementValue(MElementValue copy)
	{
		this(Env.getCtx(), copy);
	}
	
	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MElementValue(Properties ctx, MElementValue copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MElementValue(Properties ctx, MElementValue copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 * 	Set/Update values from import
	 *	@param imp import
	 */
	public void set (X_I_ElementValue imp)
	{
		setValue(imp.getValue());
		setName(imp.getName());
		setDescription(imp.getDescription());
		setAccountType(imp.getAccountType());
		setAccountSign(imp.getAccountSign());
		setIsSummary(imp.isSummary());
		setIsDocControlled(imp.isDocControlled());
		setC_Element_ID(imp.getC_Element_ID());
		//
		setPostActual(imp.isPostActual());
		setPostBudget(imp.isPostBudget());
		setPostEncumbrance(imp.isPostEncumbrance());
		setPostStatistical(imp.isPostStatistical());
	}	//	set
	
	
	
	/**
	 * Is this a Balance Sheet Account
	 * @return true if this is a balance sheet account (i.e asset, liability or owner equity)
	 */
	public boolean isBalanceSheet()
	{
		String accountType = getAccountType();
		return (ACCOUNTTYPE_Asset.equals(accountType)
			|| ACCOUNTTYPE_Liability.equals(accountType)
			|| ACCOUNTTYPE_OwnerSEquity.equals(accountType));
	}	//	isBalanceSheet

	/**
	 * Is this an asset Account
	 * @return true if this is an asset account
	 */
	public boolean isActiva()
	{
		return ACCOUNTTYPE_Asset.equals(getAccountType());
	}	//	isActive

	/**
	 * Is this a Liability Account
	 * @return true this is liability or owners equity account
	 */
	public boolean isPassiva()
	{
		String accountType = getAccountType();
		return (ACCOUNTTYPE_Liability.equals(accountType)
			|| ACCOUNTTYPE_OwnerSEquity.equals(accountType));
	}	//	isPassiva

	/**
	 * 	User String Representation
	 *	@return info value - name
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getValue()).append(" - ").append(getName());
		return sb.toString ();
	}	//	toString

	/**
	 * 	Extended String Representation
	 *	@return info
	 */
	public String toStringX ()
	{
		StringBuilder sb = new StringBuilder ("MElementValue[");
		sb.append(get_ID()).append(",").append(getValue()).append(" - ").append(getName())
			.append ("]");
		return sb.toString ();
	}	//	toStringX
			
	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		if (getAD_Org_ID() != 0)
			setAD_Org_ID(0);
		//
		// Transform to summary level account
		if (!newRecord && isSummary() && is_ValueChanged(COLUMNNAME_IsSummary))
		{
			// Disallow change to summary level if there are existing posting records
			boolean match = new Query(getCtx(), I_Fact_Acct.Table_Name, I_Fact_Acct.COLUMNNAME_Account_ID+"=?", get_TrxName())
								.setParameters(getC_ElementValue_ID())
								.match();
			if (match)
			{
				throw new AdempiereException("@AlreadyPostedTo@");
			}
			//
			// Delete existing Valid Combination records 
			String whereClause = MAccount.COLUMNNAME_Account_ID+"=?";
			POResultSet<MAccount> rs = null;
			try {
				rs = new Query(getCtx(), I_C_ValidCombination.Table_Name, whereClause, get_TrxName())
				.setParameters(get_ID())
				.scroll();
				while(rs.hasNext()) {
					rs.next().deleteEx(true);
				}
			}
			finally {
				DB.close(rs);
				rs = null;
			}
		}
		return true;
	}	//	beforeSave
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		if (newRecord || is_ValueChanged(COLUMNNAME_Value))
		{
			int ad_Tree_ID= (new MElement(getCtx(), getC_Element_ID(), get_TrxName())).getAD_Tree_ID();
			String treeType= (new MTree(getCtx(),ad_Tree_ID,get_TrxName())).getTreeType();
			// Create tree record
			if (newRecord)
				insert_Tree(treeType, getC_Element_ID());

			// Update driven by value tree
			update_Tree(treeType);
		}
		
		//	Value/Name change, update Combination and Description of C_ValidCombination
		if (!newRecord && (is_ValueChanged(COLUMNNAME_Value) || is_ValueChanged(COLUMNNAME_Name)))
		{
			MAccount.updateValueDescription(getCtx(), "Account_ID=" + getC_ElementValue_ID(),get_TrxName());
			if ("Y".equals(Env.getContext(getCtx(), "$Element_U1"))) 
				MAccount.updateValueDescription(getCtx(), "User1_ID=" + getC_ElementValue_ID(),get_TrxName());
			if ("Y".equals(Env.getContext(getCtx(), "$Element_U2"))) 
				MAccount.updateValueDescription(getCtx(), "User2_ID=" + getC_ElementValue_ID(),get_TrxName());
		}

		return success;
	}	//	afterSave
	
	@Override
	protected boolean afterDelete (boolean success)
	{
		// Delete tree record
		if (success)
			delete_Tree(MTree_Base.TREETYPE_ElementValue);
		return success;
	}	//	afterDelete

	@Override
	public MElementValue markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MElementValue
