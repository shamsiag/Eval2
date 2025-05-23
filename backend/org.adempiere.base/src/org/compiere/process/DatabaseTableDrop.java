/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Carlos Ruiz - globalqss                                           *
 **********************************************************************/
package org.compiere.process;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * Process to drop a DB table.
 */
@org.adempiere.base.annotation.Process
public class DatabaseTableDrop extends SvrProcess {

	private int p_AD_Table_ID = 0;
	// User Confirmation
	private boolean p_AreYouSure;
	// Drop the table even if it has data
	private boolean p_IsEvenWithData;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if ("AreYouSure".equals(name)) {
				p_AreYouSure = para.getParameterAsBoolean();
			} else if ("IsEvenWithData".equals(name)) {
				p_IsEvenWithData = para.getParameterAsBoolean();
			} else {
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
			}
		}
		p_AD_Table_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		MTable table = new MTable(getCtx(), p_AD_Table_ID, get_TrxName());
		if (log.isLoggable(Level.INFO)) log.info(table.toString());
		if (!p_AreYouSure) {
			throw new AdempiereException(Util.cleanAmp(Msg.getMsg(getCtx(), "Cancel")));
		}
		if (!p_IsEvenWithData) {
			int cnt = DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM " + table.getTableName());
			if (cnt > 0) {
				throw new AdempiereException(Util.cleanAmp(Msg.parseTranslation(getCtx(), "@Cancel@.  @Records@ = " + cnt)));
			}
		}

		String sql = "DROP TABLE " + table.getTableName();
		int rvalue = DB.executeUpdateEx(sql, get_TrxName());

		return rvalue + " - " + sql;
	}
} // DatabaseTableDrop
