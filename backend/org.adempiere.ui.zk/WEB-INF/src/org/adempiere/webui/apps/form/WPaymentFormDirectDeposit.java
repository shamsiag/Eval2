/******************************************************************************
 * Copyright (C) 2012 Elaine Tan                                              *
 * Copyright (C) 2012 Trek Global
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
package org.adempiere.webui.apps.form;

import org.compiere.model.GridTab;
import org.compiere.model.X_C_Order;


/**
 * Form for direct deposit payment rule ({@link X_C_Order#PAYMENTRULE_DirectDeposit}).
 * @author Elaine
 */
public class WPaymentFormDirectDeposit extends WPaymentFormDirect {
	
	/**
	 * @param windowNo
	 * @param mTab
	 */
	public WPaymentFormDirectDeposit(int windowNo, GridTab mTab) {
		super(windowNo, mTab, false);
	}
}
