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

/**
 *  Product Attribute Value
 *
 *	@author Jorg Janke
 *	@version $Id: MAttributeValue.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class MAttributeValue extends X_M_AttributeValue
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 4105427429027399512L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_AttributeValue_UU  UUID key
     * @param trxName Transaction
     */
    public MAttributeValue(Properties ctx, String M_AttributeValue_UU, String trxName) {
        super(ctx, M_AttributeValue_UU, trxName);
    }

	/**
	 * 	Constructor
	 *	@param ctx context
	 *	@param M_AttributeValue_ID id
	 *	@param trxName transaction
	 */
	public MAttributeValue (Properties ctx, int M_AttributeValue_ID, String trxName)
	{
		super (ctx, M_AttributeValue_ID, trxName);
	}	//	MAttributeValue

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MAttributeValue (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MAttributeValue

	/**
	 * Copy constructor 
	 * @param copy
	 */
	public MAttributeValue(MAttributeValue copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MAttributeValue(Properties ctx, MAttributeValue copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MAttributeValue(Properties ctx, MAttributeValue copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 *	String Representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		return getName();
	}	//	toString

}	//	MAttributeValue
