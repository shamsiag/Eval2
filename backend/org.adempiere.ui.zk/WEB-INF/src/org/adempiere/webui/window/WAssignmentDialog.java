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
package org.adempiere.webui.window;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.logging.Level;

import org.adempiere.util.Callback;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Datebox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.NumberBox;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Timebox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUOMConversion;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Listitem;

/**
 *	Resource Assignment Dialog
 *
 * 	@author 	Jorg Janke
 * 
 *  Zk Port
 *  @author Low Heng Sin
 */
public class WAssignmentDialog extends Window implements EventListener<Event>
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -1762339564864115852L;
	
	/* SysConfig USE_ESC_FOR_TAB_CLOSING */
	private boolean isUseEscForTabClosing = MSysConfig.getBooleanValue(MSysConfig.USE_ESC_FOR_TAB_CLOSING, false, Env.getAD_Client_ID(Env.getCtx()));

	/**
	 * 	Assignment Dialog.
	 * 	<pre>
	 * 		Creates a new assignment or displays an assignment
	 * 		Create new:	(ID == 0)
	 * 			check availability and create assignment
	 * 			(confirmed when order/invoice/timeExpense is processed)
	 * 			alternatively let InfoResource do the assignment
	 * 			return ID
	 * 		Existing assignment: (ID != 0)
	 * 			if confirmed - no change.
	 * 			ability to delete or change assignment
	 * 			return ID
	 * 	</pre>
	 *  @param mAssignment Assignment
	 *  @param allowZoom allow to zoom to schedule
	 *  @param allowDelete allow to delete record
	 */
	public WAssignmentDialog (MResourceAssignment mAssignment, 
		boolean allowZoom, boolean allowDelete)
	{
		super ();
		this.setTitle(Msg.getMsg(Env.getCtx(), "VAssignmentDialog"));
		this.setBorder("normal");
		this.setAttribute(Window.MODE_KEY, Window.MODE_HIGHLIGHTED);
		if (log.isLoggable(Level.CONFIG)) log.config(mAssignment.toString());
		m_mAssignment = mAssignment;
		try
		{
			init();
			if (!allowZoom)
				confirmPanel.getButton("Zoom").setVisible(false);
			delete.setVisible(allowDelete);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
		}
		setDisplay();	//	from mAssignment
		if (!ThemeManager.isUseCSSForWindowSize())
			ZKUpdateUtil.setWindowWidthX(this, 600);
		setSizable(true);
		setMaximizable(true);
		setSclass("assignment-dialog");
		//		
	}	//	VAssignmentDialog

	/**	Assignment						*/
	private MResourceAssignment	m_mAssignment;
	/**	True if setting Value			*/
	private boolean		m_setting = false;
	/**	Logger							*/
	private static final CLogger log = CLogger.getCLogger(WAssignmentDialog.class);
	/**	Lookup with Resource and UOM		*/
	private HashMap<KeyNamePair,KeyNamePair>	m_lookup = new HashMap<KeyNamePair,KeyNamePair>();	
	//
	private Grid mainPanel = new Grid();
	private Label lResource = new Label(Msg.translate(Env.getCtx(), "S_Resource_ID"));
	private Listbox fResource = new Listbox(getResources());
	private Label lDate = new Label(Msg.translate(Env.getCtx(), "DateFrom"));
	private Datebox fDateFrom = new Datebox();
	private Timebox fTimeFrom = new Timebox();
	private Label lQty = new Label(Msg.translate(Env.getCtx(), "Qty"));
	private NumberBox fQty = new NumberBox(false);
	private Label lUOM = new Label();
	private Label lName = new Label(Msg.translate(Env.getCtx(), "Name"));
	private Label lDescription = new Label(Msg.translate(Env.getCtx(), "Description"));
	private Textbox fName = new Textbox();
	private Textbox fDescription = new Textbox();
	private ConfirmPanel confirmPanel = new ConfirmPanel(true, false, false, false, false, true /*, true*/);
	private Button delete = confirmPanel.createButton("Delete");
	private boolean m_cancel = true;
	private boolean m_zoom;

	/**
	 * 	Layout dialog
	 * 	@throws Exception
	 */
	private void init() throws Exception
	{
		fResource.setMold("select");
		fResource.addEventListener(Events.ON_SELECT, this);
		delete.addEventListener(Events.ON_CLICK, this);
		confirmPanel.addComponentsLeft(delete);
		confirmPanel.addActionListener(Events.ON_CLICK, this);
		//
		this.appendChild(mainPanel);
		mainPanel.makeNoStrip();
		mainPanel.setStyle("background-color: transparent");
		
		Rows rows = new Rows();
		mainPanel.appendChild(rows);
		Row row = new Row();
		row.appendChild(LayoutUtils.makeRightAlign(lResource));
		row.appendChild(fResource);
		row.appendChild(new Label(" "));
		rows.appendChild(row);
		
		row = new Row();
		row.appendCellChild(LayoutUtils.makeRightAlign(lDate), 1);
		Div div = new Div();
		ZKUpdateUtil.setHflex(div, "1");
		div.appendChild(fDateFrom);
		fDateFrom.setCols(10);
		div.appendChild(fTimeFrom);
		fTimeFrom.setCols(10);
		fTimeFrom.setStyle("margin-left: 1px");
		row.appendCellChild(div, 2);
		rows.appendChild(row);
		
		row = new Row();
		row.appendChild(LayoutUtils.makeRightAlign(lQty));		
		row.appendChild(fQty);
		row.appendChild(lUOM);
		rows.appendChild(row);
		
		row = new Row();
		row.appendCellChild(LayoutUtils.makeRightAlign(lName), 1);
		row.appendCellChild(fName, 2);
		fName.setStyle("width: 100%");
		rows.appendChild(row);
		
		row = new Row();
		row.appendCellChild(LayoutUtils.makeRightAlign(lDescription), 1);
		row.appendCellChild(fDescription, 2);
		fDescription.setMultiline(true);
		fDescription.setRows(3);
		ZKUpdateUtil.setWidth(fDescription, "100%");
		rows.appendChild(row);
		
		row = new Row();
		row.appendCellChild(new Label(" "), 3);
		rows.appendChild(row);

		row = new Row();
		row.appendCellChild(confirmPanel, 3);
		rows.appendChild(row);
		
		addEventListener(Events.ON_CANCEL, e -> onCancel());
		//
	}	//	init

	/**
	 * 	Initialize component & values from m_mAssignment
	 */
	private void setDisplay()
	{
		m_setting = true;

		//	Set Resource
		int S_Resource_ID = m_mAssignment.getS_Resource_ID();
		
		for (Listitem item : fResource.getItems()) {
			if ( (Integer)item.getValue() == S_Resource_ID) {
				fResource.setSelectedItem(item);
				break;
			}
		}
		
		ListItem listItem = fResource.getSelectedItem();
		KeyNamePair check = new KeyNamePair((Integer)listItem.getValue(), listItem.getLabel());
		if (check == null || check.getKey() != S_Resource_ID)
		{
			if (m_mAssignment.getS_ResourceAssignment_ID() == 0)	//	new record select first
				fResource.setSelectedItem(fResource.getSelectedItem());		//	initiates UOM display
			else
				log.log(Level.SEVERE, "Resource not found ID=" + S_Resource_ID);
		}

		//	Set Date, Qty
		fDateFrom.setValue(m_mAssignment.getAssignDateFrom());
		fTimeFrom.setValue(m_mAssignment.getAssignDateFrom());
		fQty.setValue(m_mAssignment.getQty());

		//	Name, Description
		fName.setValue(m_mAssignment.getName());
		fDescription.setValue(m_mAssignment.getDescription());

		//	Set Editor to R/O if confirmed
		boolean readWrite = true;
		if (m_mAssignment.isConfirmed())
			readWrite = false;
		confirmPanel.getButton("Cancel").setVisible(readWrite);
		fResource.setEnabled(readWrite);
		fDateFrom.setReadonly(!readWrite);
		fQty.setEnabled(readWrite);

		m_setting = false;
	}	//	setDisplay

	/**
	 * 	Get Assignment
	 * 	@return Assignment
	 */
	public MResourceAssignment getMResourceAssignment()
	{
		return m_mAssignment;
	}	//	getMResourceAssignment

	/**
	 * 	Check availability and insert record
	 *  @return true if saved/updated
	 */
	private boolean cmd_save()
	{
		//	Set AssignDateTo
		Calendar date = new GregorianCalendar();
		getDateAndTimeFrom(date);
		Timestamp assignDateFrom = new Timestamp(date.getTimeInMillis());
		BigDecimal qty = fQty.getValue();
		ListItem listItem = fResource.getSelectedItem();
		KeyNamePair resource = listItem != null ? new KeyNamePair((Integer)listItem.getValue(), listItem.getLabel()) : null;
		KeyNamePair uom = (KeyNamePair)m_lookup.get(resource);
		int minutes = MUOMConversion.convertToMinutes(Env.getCtx(), uom.getKey(), qty);
		Timestamp assignDateTo = TimeUtil.addMinutess(assignDateFrom, minutes);
		m_mAssignment.setAssignDateTo (assignDateTo);
		//
		return m_mAssignment.save();
	}	//	cmdSave

	
	/**
	 * 	Load Resources.
	 * 	@return Array with resources
	 */
	private KeyNamePair[] getResources()
	{
		if (m_lookup.size() == 0)
		{
			String sql = MRole.getDefault().addAccessSQL(
				"SELECT r.S_Resource_ID, r.Name, r.IsActive,"	//	1..3
				+ "uom.C_UOM_ID,COALESCE(uom.UOMSymbol, uom.Name) "					//	4..5
				+ "FROM S_Resource r, S_ResourceType rt, C_UOM uom "
				+ "WHERE r.S_ResourceType_ID=rt.S_ResourceType_ID AND rt.C_UOM_ID=uom.C_UOM_ID",
				"r", MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql, null);
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					StringBuilder sb = new StringBuilder (rs.getString(2));
					if (!"Y".equals(rs.getString(3)))
						sb.insert(0,'~').append('~');	//	inactive marker
					//	Key		S_Resource_ID/Name
					KeyNamePair key = new KeyNamePair (rs.getInt(1), sb.toString());
					//	Value	C_UOM_ID/Name
					KeyNamePair value = new KeyNamePair (rs.getInt(4), rs.getString(5).trim());
					m_lookup.put(key, value);
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, sql, e);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}
		}
		//	Convert to Array
		KeyNamePair[] retValue = new KeyNamePair[m_lookup.size()];
		m_lookup.keySet().toArray(retValue);
		Arrays.sort(retValue);
		return retValue;
	}	//	getResources

	@Override
	public void onEvent(Event e) throws Exception {
		if (m_setting)
			return;
		//	Update Assignment
		ListItem listItem = fResource.getSelectedItem();
		KeyNamePair resource = listItem != null ? new KeyNamePair((Integer)listItem.getValue(), listItem.getLabel()) : null;
		if (resource != null)
		{
			int S_Resource_ID = resource.getKey();
			m_mAssignment.setS_Resource_ID (S_Resource_ID);
		}
		
		Calendar date = new GregorianCalendar();
		getDateAndTimeFrom(date);
		
		Timestamp assignDateFrom = new Timestamp(date.getTimeInMillis());
		if (assignDateFrom != null)
			m_mAssignment.setAssignDateFrom (assignDateFrom);
		if (fQty.getValue() != null) {
			BigDecimal qty = fQty.getValue();
			m_mAssignment.setQty(qty);
		}
		m_mAssignment.setName((String)fName.getValue());
		m_mAssignment.setDescription((String)fDescription.getValue());

		//	Resource - Look up UOM
		if (e.getTarget() == fResource)
		{
			Object o = m_lookup.get(resource);
			if (o == null)
				lUOM.setValue(" ? ");
			else
				lUOM.setValue(o.toString());
		}

		//	Zoom - InfoResource
		else if (e.getTarget().getId().equals("Zoom"))
		{
			m_zoom = true;
			setVisible(false);
			Events.echoEvent("onShowSchedule", this, null);
		}

		//	cancel - return
		else if (e.getTarget().getId().equals("Cancel"))
		{
			onCancel();
		}

		//	delete - delete and return
		else if (e.getTarget().getId().equals("Delete"))
		{
			if (m_mAssignment.delete(true))
			{
				m_mAssignment = null;
				detach();
			}
			else
				Dialog.error(0, "ResourceAssignmentNotDeleted");
		}

		//	OK - Save
		else if (e.getTarget().getId().equals("Ok"))
		{
			m_cancel = false;
			if (cmd_save())
				detach();
		}		
	}

	/**
	 * onCancel event
	 */
	private void onCancel() {
		// do not allow to close tab for Events.ON_CTRL_KEY event
		if(isUseEscForTabClosing)
			SessionManager.getAppDesktop().setCloseTabWithShortcut(false);

		m_cancel = true;
		detach();
	}

	/**
	 * Open info schedule window
	 */
	public void onShowSchedule() 
	{
		InfoSchedule is = new InfoSchedule (m_mAssignment, true, this, new Callback<MResourceAssignment>() {			
			@Override
			public void onCallback(MResourceAssignment result) {
				m_zoom = false;
				if (result != null)
				{
					m_mAssignment = result;
					setDisplay();
				}	
				setVisible(true);
				WAssignmentDialog.this.focus();
			}
		});
		AEnv.showWindow(is);
		is.focus();
	}
	
	/**
	 * Set date and time from to date calendar
	 * @param date
	 */
	private void getDateAndTimeFrom(Calendar date) {
		Date dateFrom = fDateFrom.getValue();
		Date timeFrom = fTimeFrom.getValue();		
		date.setTime(dateFrom);
		Calendar time = new GregorianCalendar();
		time.setTime(timeFrom);
		date.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
		date.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
	}

	/**
	 * @return true if cancel by user
	 */
	public boolean isCancelled() {
		return m_cancel;
	}
	
	/**
	 * @return from Datebox
	 */
	public Datebox getDateFrom() {
		return fDateFrom;
	}

	@Override
	public boolean setVisible(boolean visible) {		
		boolean b = super.setVisible(visible);
		if (!m_zoom && b && !visible) {
			if (getModeType() == Mode.POPUP) {
				this.detach();
			}
		}
		return b;
	}		
}	//	WAssignmentDialog
