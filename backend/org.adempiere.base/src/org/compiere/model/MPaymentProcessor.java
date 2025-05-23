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

import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 *  Payment Processor Model
 *
 *  @author Jorg Janke
 *  @version $Id: MPaymentProcessor.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MPaymentProcessor extends X_C_PaymentProcessor
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = 8514876566904723695L;
		
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_PaymentProcessor_UU  UUID key
     * @param trxName Transaction
     */
    public MPaymentProcessor(Properties ctx, String C_PaymentProcessor_UU, String trxName) {
        super(ctx, C_PaymentProcessor_UU, trxName);
		if (Util.isEmpty(C_PaymentProcessor_UU))
			setInitialDefaults();
    }

	/**
	 *	Payment Processor Model
	 * 	@param ctx context
	 * 	@param C_PaymentProcessor_ID payment processor
	 *	@param trxName transaction
	 */
	public MPaymentProcessor (Properties ctx, int C_PaymentProcessor_ID, String trxName)
	{
		super (ctx, C_PaymentProcessor_ID, trxName);
		if (C_PaymentProcessor_ID == 0)
			setInitialDefaults();
	}	//	MPaymentProcessor

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setCommission (Env.ZERO);
		setAcceptVisa (false);
		setAcceptMC (false);
		setAcceptAMEX (false);
		setAcceptDiners (false);
		setCostPerTrx (Env.ZERO);
		setAcceptCheck (false);
		setRequireVV (false);
		setAcceptCorporate (false);
		setAcceptDiscover (false);
		setAcceptATM (false);
		setAcceptDirectDeposit(false);
		setAcceptDirectDebit(false);
	}

	/**
	 *	Payment Processor Model
	 * 	@param ctx context
	 * 	@param rs result set
	 *	@param trxName transaction
	 */
	public MPaymentProcessor (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MPaymentProcessor

	/**
	 * 	String representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MPaymentProcessor[")
			.append(get_ID ()).append("-").append(getName())
			.append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * @deprecated Use C_BankAccount.C_PaymentProcessor_ID 
	 */
	@Override
	@Deprecated
	public I_C_BankAccount getC_BankAccount() throws RuntimeException {
		return super.getC_BankAccount();
	}

	/**
	 * @deprecated Use C_BankAccount.C_PaymentProcessor_ID
	 */
	@Override
	@Deprecated
	public void setC_BankAccount_ID(int C_BankAccount_ID) {
		super.setC_BankAccount_ID(C_BankAccount_ID);
	}

	/**
	 * @deprecated Use C_BankAccount.C_PaymentProcessor_ID
	 */
	@Override
	@Deprecated
	public int getC_BankAccount_ID() {
		return super.getC_BankAccount_ID();
	}
}	//	MPaymentProcessor
