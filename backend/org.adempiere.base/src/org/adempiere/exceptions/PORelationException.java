/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2009 www.metas.de                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.adempiere.exceptions;

import java.util.logging.Level;

import org.adempiere.model.MRelationType;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Msg;

/**
 * Exception related to invalid {@link MRelationType} configuration.
 * @author Tobias Schoeneberg, www.metas.de - FR [ 2897194 ] Advanced Zoom and
 *         RelationTypes
 */
public class PORelationException extends AdempiereException {

	private static final CLogger logger = CLogger
			.getCLogger(PORelationException.class);

	/**
	 * Message indicates that a po has more or less than one key columns.
	 * <ul>
	 * <li>Param 1: the po (toString)</li>
	 * <li>Param 2: the number of key columns</li>
	 * </ul>
	 */
	public static final String MSG_ERR_KEY_COLUMNS_2P = "MRelationType_Err_KeyColumns_2P";

	/**
	 * Message indicates that neither the reference nor the table have an
	 * AD_Window_ID set.
	 * <ul>
	 * <li>Param 1: The AD_Reference's name</li>
	 * <li>Param 2: The Table name</li>
	 * <li>Param 3: Whether we are in the ctx of a SO (Y or N)</li>
	 * </ul>
	 */
	public static final String MSG_ERR_WINDOW_3P = "MRelationType_Err_Window_3P";

	public final String adMsg;

	public final Object[] msgParams;

	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -906400765022362887L;

	/**
	 * @param msg
	 * @param adMsg
	 * @param msgParams
	 */
	private PORelationException(final String msg, final String adMsg,
			final Object... msgParams) {

		super(msg);

		this.adMsg = adMsg;
		this.msgParams = msgParams;
	}

	/**
	 * Throw wrong number of key column count exception 
	 * @param po
	 */
	public static void throwWrongKeyColumnCount(final PO po) {

		if (logger.isLoggable(Level.FINE)) logger.fine("Invoked with po " + po);

		final Object[] msgParams = new Object[] { po.toString(),
				po.get_KeyColumns().length };

		final String msg = Msg.getMsg(po.getCtx(), MSG_ERR_KEY_COLUMNS_2P,
				msgParams);

		final StringBuilder sb = new StringBuilder(msg);

		for (final String keyCol : po.get_KeyColumns()) {
			sb.append("\n");
			sb.append(keyCol);
		}

		throw new PORelationException(sb.toString(), MSG_ERR_KEY_COLUMNS_2P,
				msgParams);
	}

	/**
	 * throw missing AD_Window_ID exception 
	 * @param po
	 * @param referenceName
	 * @param tableName
	 * @param isSOTrx
	 */
	public static void throwMissingWindowId(final PO po,
			final String referenceName, final String tableName,
			final boolean isSOTrx) {

		final Object[] msgParams = { referenceName, tableName,
				isSOTrx ? "Y" : "N" };

		final String msg = Msg
				.getMsg(po.getCtx(), MSG_ERR_WINDOW_3P, msgParams);

		throw new PORelationException(msg, MSG_ERR_WINDOW_3P, msgParams);
	}
}
