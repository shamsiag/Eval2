/******************************************************************************
 * Copyright (C) 2008 Elaine Tan                                              *
 * Copyright (C) 2008 Idalica Corporation                                     *
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
package org.adempiere.webui.dashboard;

import org.adempiere.webui.component.Window;
import org.adempiere.webui.util.ServerPushTemplate;

/**
 * Base class for dashboard gadget/widget
 * @author Elaine
 * @date November 20, 2008
 */
public abstract class DashboardPanel extends Window implements IDashboardPanel {

	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 7424244218250118823L;

	/**
	 * Default constructor
	 */
	public DashboardPanel()
	{
		super();
	}
	
	@Override
	public void refresh(ServerPushTemplate template) {
	}

	@Override
	public void updateUI() {
	}
	
	/**
	 * Is using polling for update
	 * @return true if this dashboard widget uses polling to update its content
	 */
	public boolean isPooling() {
		return false;
	}
	
	/**
	 * Override this together with refresh and updateUI to implement background loading of gadget
	 * @return true if panel created without loading of data
	 */
	public boolean isLazy() {
		return false;
	}
	
	/**
	 * Empty Dashboard Panel are not rendered on the Dashboard
	 * @return true if the panel is empty
	 */
	public boolean isEmpty() {
		return false;
	}
}
