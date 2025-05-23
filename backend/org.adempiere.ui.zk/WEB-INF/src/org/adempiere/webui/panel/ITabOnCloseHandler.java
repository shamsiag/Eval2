/******************************************************************************
 * Copyright (C) 2010 Low Heng Sin                                            *
 * Copyright (C) 2010 Idalica Corporation                                     *
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
package org.adempiere.webui.panel;

import org.adempiere.webui.component.Tabpanel;

/**
 * Interface to handle the closing of a Tab.
 * @author hengsin
 *
 */
public interface ITabOnCloseHandler {
	
	/**
	 * NOTE onClose method must implement the actual closing of the tab
	 * @param tabPanel 
	 */
	public void onClose(Tabpanel tabPanel);
}
