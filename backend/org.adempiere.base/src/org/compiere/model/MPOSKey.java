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
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	POS Function Key Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MPOSKey.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MPOSKey extends X_C_POSKey implements ImmutablePOSupport
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = -5138032789563975514L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param C_POSKey_UU  UUID key
     * @param trxName Transaction
     */
    public MPOSKey(Properties ctx, String C_POSKey_UU, String trxName) {
        super(ctx, C_POSKey_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_POSKey_ID id
	 *	@param trxName transaction
	 */
	public MPOSKey (Properties ctx, int C_POSKey_ID, String trxName)
	{
		super (ctx, C_POSKey_ID, trxName);
	}	//	MPOSKey

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MPOSKey (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MPOSKey

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MPOSKey(MPOSKey copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MPOSKey(Properties ctx, MPOSKey copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MPOSKey(Properties ctx, MPOSKey copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	@Override
	protected boolean postDelete() {
		if (getAD_Image_ID() > 0) {
			MImage img = new MImage(getCtx(), getAD_Image_ID(), get_TrxName());
			if (!img.delete(true)) {
				log.warning("Associated image could not be deleted for POS Key - AD_Image_ID=" + getAD_Image_ID());
				return false;
			}
		}
		return true;
	}

	@Override
	public MPOSKey markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MPOSKey
