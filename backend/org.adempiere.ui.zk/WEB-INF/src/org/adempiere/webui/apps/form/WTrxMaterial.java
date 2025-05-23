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
package org.adempiere.webui.apps.form;

import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.adwindow.ADTabpanel;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WLocatorEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.panel.StatusBarPanel;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.DateRangeButton;
import org.compiere.apps.form.TrxMaterial;
import org.compiere.model.MLocatorLookup;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.South;

/**
 * Form to view Material Transaction History.
 */
@org.idempiere.ui.zk.annotation.Form(name = "org.compiere.apps.form.VTrxMaterial")
public class WTrxMaterial extends TrxMaterial
	implements IFormController, EventListener<Event>, ValueChangeListener
{
	/** Custom form/window UI instance */
	private CustomForm form = new CustomForm();	

	/** Center of {@link #mainLayout} */
	private ADTabpanel  m_gridController = null;

	/** Main panel of {@link #form} */
	private Panel mainPanel = new Panel();
	/** Layout of {@link #mainPanel} */
	private Borderlayout mainLayout = new Borderlayout();
	
	/** North of {@link #mainLayout}. Form parameters panel */
	private Panel parameterPanel = new Panel();
	/** Layout of {@link #parameterPanel} */
	private Grid parameterLayout = GridFactory.newGridLayout();
	private Label orgLabel = new Label();
	private WTableDirEditor orgField;
	private Label locatorLabel = new Label();
	private WLocatorEditor locatorField;
	private Label productLabel = new Label();
	private WSearchEditor productField;
	private Label dateFLabel = new Label();
	private WDateEditor dateFField;
	private Label dateTLabel = new Label();
	private WDateEditor dateTField;
	private Label mtypeLabel = new Label();
	private WTableDirEditor mtypeField;
	
	/** South of {@link #mainLayout} */
	private Panel southPanel = new Panel();
	/** Action buttons panel. Child of {@link #southPanel} */
	private ConfirmPanel confirmPanel = new ConfirmPanel(true, true, false, false, false, true, false);
	/** Status bar */
	private StatusBarPanel statusBar = new StatusBarPanel();

	/** Number of columns for {@link #parameterLayout} */
	private int noOfColumns;

	/**
	 * Default constructor
	 */
	public WTrxMaterial()
	{
		if (log.isLoggable(Level.INFO)) log.info("");
		try
		{
			m_WindowNo = form.getWindowNo();
			dynParameter();
			zkInit();
			dynInit();		
			if (ClientInfo.isMobile())
				ClientInfo.onClientInfo(form, this::onClientInfo);
		}
		catch(Exception ex)
		{
			log.log(Level.SEVERE, "", ex);
		}
	}
	
	/**
	 *  Layout {@link #form}
	 *  @throws Exception
	 */
	protected void zkInit() throws Exception
	{
		form.appendChild(mainPanel);
		mainPanel.setStyle("width: 100%; height: 100%; border: none; padding: 0; margin: 0");
		mainPanel.appendChild(mainLayout);
		ZKUpdateUtil.setWidth(mainLayout, "100%");
		ZKUpdateUtil.setHeight(mainLayout, "100%");
		parameterPanel.appendChild(parameterLayout);
		//
		orgLabel.setText(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		locatorLabel.setText(Msg.translate(Env.getCtx(), "M_Locator_ID"));
		productLabel.setText(Msg.translate(Env.getCtx(), "Product"));
		dateFLabel.setText(Msg.translate(Env.getCtx(), "DateFrom"));
		dateTLabel.setText(Msg.translate(Env.getCtx(), "DateTo"));
		mtypeLabel.setText(Msg.translate(Env.getCtx(), "MovementType"));
		//
		North north = new North();
		mainLayout.appendChild(north);
		north.appendChild(parameterPanel);
		north.setSplittable(true);
		north.setCollapsible(true);
		north.setAutoscroll(true);
		LayoutUtils.addSlideSclass(north);
		
		layoutParameters();
		//
		southPanel.appendChild(confirmPanel);
		southPanel.appendChild(statusBar);
		South south = new South();
		south.setStyle("border: none");
		mainLayout.appendChild(south);
		south.appendChild(southPanel);
		ZKUpdateUtil.setHeight(southPanel, "64px");
		ZKUpdateUtil.setHeight(south, "64px");
		ZKUpdateUtil.setHeight(confirmPanel, "32px");
		ZKUpdateUtil.setHeight(statusBar, "32px");
		
		ZKUpdateUtil.setWidth(southPanel, "100%");
		ZKUpdateUtil.setWidth(confirmPanel, "100%");
		ZKUpdateUtil.setWidth(statusBar, "100%");
		
		LayoutUtils.addSclass("status-border", statusBar);
	}

	/**
	 * Layout {@link #parameterLayout}
	 */
	protected void layoutParameters() {
		noOfColumns = 6;
		if (ClientInfo.maxWidth(639))
			noOfColumns = 2;
		else if (ClientInfo.maxWidth(ClientInfo.MEDIUM_WIDTH-1))
			noOfColumns = 4;
		
		int childCnt = 0;
		Rows rows = parameterLayout.newRows();
		Row row = rows.newRow();
		row.appendCellChild(orgLabel.rightAlign());
		ZKUpdateUtil.setHflex(orgField.getComponent(), "true");
		row.appendCellChild(orgField.getComponent());
		childCnt += 2;
		if ((childCnt % noOfColumns) ==0 )
			row = rows.newRow();
		row.appendCellChild(mtypeLabel.rightAlign());
		ZKUpdateUtil.setHflex(mtypeField.getComponent(), "true");
		row.appendCellChild(mtypeField.getComponent());
		childCnt += 2;
		if ((childCnt % noOfColumns) ==0 )
			row = rows.newRow();
		if (noOfColumns == 6)
		{
			row.appendCellChild(dateFLabel.rightAlign());
			row.appendCellChild(dateFField.getComponent());
			childCnt += 2;
			if ((childCnt % noOfColumns) ==0 )
				row = rows.newRow();
		}
		row.appendCellChild(locatorLabel.rightAlign());
		ZKUpdateUtil.setHflex(locatorField.getComponent(), "true");		
		row.appendCellChild(locatorField.getComponent());
		childCnt += 2;
		if ((childCnt % noOfColumns) ==0 )
			row = rows.newRow();
		row.appendCellChild(productLabel.rightAlign());
		ZKUpdateUtil.setHflex(productField.getComponent(), "true");
		row.appendCellChild(productField.getComponent());
		childCnt +=2;
		if ((childCnt % noOfColumns) ==0 )
			row = rows.newRow();
		if (noOfColumns < 6)
		{
			row.appendCellChild(dateFLabel.rightAlign());
			row.appendCellChild(dateFField.getComponent());
			childCnt += 2;
			if ((childCnt % noOfColumns) ==0 )
				row = rows.newRow();
		}
		row.appendCellChild(dateTLabel.rightAlign());
		Hbox boxTo = new Hbox();
		boxTo.appendChild(dateTField.getComponent());
		DateRangeButton drb = (new DateRangeButton(dateFField, dateTField));
		boxTo.appendChild(drb);
		row.appendCellChild(boxTo);
	}

	/**
	 *  Initialize Parameter fields
	 *  @throws Exception if Lookups cannot be initialized
	 */
	private void dynParameter() throws Exception
	{
		Properties ctx = Env.getCtx();
		//  Organization
		MLookup orgLookup = MLookupFactory.get (ctx, m_WindowNo, 0, 3660, DisplayType.TableDir);
		orgField = new WTableDirEditor("AD_Org_ID", false, false, true, orgLookup);
		//  Locator
		MLocatorLookup locatorLookup = new MLocatorLookup(ctx, m_WindowNo, null);
		locatorField = new WLocatorEditor ("M_Locator_ID", false, false, true, locatorLookup, m_WindowNo);
		//  Product
		MLookup productLookup = MLookupFactory.get (ctx, m_WindowNo, 0, 3668, DisplayType.Search);
		productField = new WSearchEditor("M_Product_ID", false, false, true, productLookup);
		productField.addValueChangeListener(this);
		//  Movement Type
		MLookup mtypeLookup = MLookupFactory.get (ctx, m_WindowNo, 0, 3666, DisplayType.List);
		mtypeField = new WTableDirEditor("MovementType", false, false, true, mtypeLookup);
		//  Dates
		dateFField = new WDateEditor("DateFrom", false, false, true, Msg.getMsg(Env.getCtx(), "DateFrom"));
		dateTField = new WDateEditor("DateTo", false, false, true, Msg.getMsg(Env.getCtx(), "DateTo"));
		//
		confirmPanel.addActionListener(this);
		statusBar.setStatusLine("");
	}   //  dynParameter

	/**
	 *  Initialize {@link #m_gridController}.
	 * 	Based on AD_Window: Material Transactions (indirect use).
	 */
	private void dynInit()
	{
		super.dynInit(statusBar);
		//		
		m_gridController = new ADTabpanel();
		// m_mTab is level 0 GridTab of Material Transactions (indirect use) 
		m_gridController.init(null, m_mTab);
		if (!m_gridController.isGridView())
			m_gridController.switchRowPresentation();
		Center center = new Center();
		mainLayout.appendChild(center);
		center.appendChild(m_gridController);
		ZKUpdateUtil.setVflex(m_gridController, "1");
		ZKUpdateUtil.setHflex(m_gridController, "1");
	}   //  dynInit

	/**
	 * Close form.
	 */
	public void dispose()
	{
		SessionManager.getAppDesktop().closeActiveWindow();
	}	//	dispose
	
	/**
	 * Event Listener
	 * @param e event
	 */
	public void onEvent (Event e)
	{
		if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
			dispose();
		else if (e.getTarget().getId().equals(ConfirmPanel.A_REFRESH)
				|| e.getTarget().getId().equals(ConfirmPanel.A_OK))
			refresh();
		else if (e.getTarget().getId().equals(ConfirmPanel.A_ZOOM))
			zoom();
	}
	
	/**
	 * Value change listener
	 * @param e event
	 */
	public void valueChange (ValueChangeEvent e)
	{
		if (e.getPropertyName().equals("M_Product_ID"))
			productField.setValue(e.getNewValue());
	}

	/**
	 * Refresh - Create Query and refresh {@link #m_gridController}.
	 */
	private void refresh()
	{
		Object organization = orgField.getValue();
		Object locator = locatorField.getValue();
		Object product = productField.getValue();
		Object movementType = mtypeField.getValue();
		Timestamp movementDateFrom = (Timestamp)dateFField.getValue();
		Timestamp movementDateTo = (Timestamp)dateTField.getValue();
		
		refresh(organization, locator, product, movementType, movementDateFrom, movementDateTo, statusBar);
		if (ClientInfo.maxHeight(ClientInfo.MEDIUM_HEIGHT-1))
			mainLayout.getNorth().setOpen(false);
	}   //  refresh

	/**
	 * Zoom to AD_Table_ID + Record_ID of current {@link #m_gridController} row.
	 */
	public void zoom()
	{
		super.zoom();

		//  Zoom
		AEnv.zoom(AD_Table_ID, Record_ID);
	}   //  zoom
	
	@Override
	public ADForm getForm() 
	{
		return form;
	}

	/**
	 * Handle onClientInfo event from browser
	 */
	protected void onClientInfo() 
	{
		if (noOfColumns > 0 && parameterLayout.getRows() != null)
		{
			int n = 6;
			if (ClientInfo.maxWidth(639))
				n = 2;
			else if (ClientInfo.maxWidth(ClientInfo.MEDIUM_WIDTH-1))
				n = 4;
			if (n != noOfColumns)
			{
				parameterLayout.getRows().detach();
				layoutParameters();
				form.invalidate();
			}
		}
	}
}
