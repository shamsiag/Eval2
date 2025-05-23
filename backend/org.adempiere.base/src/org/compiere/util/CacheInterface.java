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
package org.compiere.util;

/**
 *  iDempiere Cache interaction Interface
 *
 *  @author Jorg Janke
 *  @version $Id: CacheInterface.java,v 1.2 2006/07/30 00:54:35 jjanke Exp $
 */
public interface CacheInterface
{
	/**
	 *	Reset Cache
	 *	@return number of items reset
	 */
	public int reset();
	
	/**
	 *	Reset Cache by record id
	 *  @param recordId
	 *	@return number of items reset
	 */
	public int reset(int recordId);

	/**
	 * Reset Cache by String key
	 * @param key
	 * @return number of items reset
	 */
	default int resetByStringKey(String key) {
		return 0;
	}
	
	/**
	 * 	Get Size of Cache
	 *	@return number of items
	 */
	public int size();

	/**
	 * New record created notification 
	 * @param record_ID
	 */
	public void newRecord(int record_ID);
		
}	//	CacheInterface
