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
 * - hengsin                         								   *
 **********************************************************************/
package org.adempiere.webui;

import static org.compiere.model.SystemIDs.FORM_ARCHIVEVIEWER;

import java.util.logging.Level;

import org.adempiere.webui.apps.form.WArchiveViewer;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.session.SessionManager;
import org.compiere.model.MArchive;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Popup;

/**
 *	Archive Button Action.<br/>
 *  Create and show popup menu.
 *	
 *  @author Jorg Janke
 */
public class WArchive implements EventListener<Event>
{
	/**
	 * 	Constructor
	 *	@param invoker button
	 *	@param AD_Table_ID table
	 *	@param Record_ID record ID
	 *  @deprecated - use {@link #WArchive(Component, int, int, String)} instead
	 */
	@Deprecated
	public WArchive (Component invoker, int AD_Table_ID, int Record_ID)
	{
		this(invoker, AD_Table_ID, Record_ID, null);
	}

	/**
	 * 	Constructor
	 *	@param invoker button
	 *	@param AD_Table_ID table
	 *	@param Record_ID record ID
	 *  @param Record_UU record UUID
	 */
	public WArchive (Component invoker, int AD_Table_ID, int Record_ID, String Record_UU)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("AD_Table_ID=" + AD_Table_ID + ", Record_ID=" + Record_ID + ", Record_UU=" + Record_UU);
		m_AD_Table_ID = AD_Table_ID;
		m_Record_ID = Record_ID;
		m_Record_UU = Record_UU;
		getArchives(invoker);
	}

	/**	The Table						*/
	private int			m_AD_Table_ID;
	/** The Record ID					*/
	private int			m_Record_ID;
	/** The Record UUID					*/
	private String		m_Record_UU;
	
	/**	The Popup						*/
	private Menupopup 	m_popup = new Menupopup();
	private Menuitem 	m_reports = null;
	private Menuitem 	m_reportsAll = null;
	private Menuitem 	m_documents = null;	

	/**	Logger	*/
	private static final CLogger	log	= CLogger.getCLogger (WArchive.class);
	
	/**
	 * 	Create and show the archive popup menu
	 * 	@param invoker button
	 */
	private void getArchives(Component invoker)
	{
		int[] counts = MArchive.getReportAndDocumentCountByRecordId(m_AD_Table_ID, m_Record_ID, m_Record_UU, null);
		int reportCount = counts[0];
		int documentCount = counts[1];
		//
		if (documentCount > 0)
		{
			m_documents = new Menuitem(Msg.getMsg(Env.getCtx(), "ArchivedDocuments") 
					+ " (" + documentCount + ")");
			m_documents.addEventListener(Events.ON_CLICK, this);
			m_popup.appendChild(m_documents);
		}
		if (reportCount > 0)
		{
			m_reports = new Menuitem(Msg.getMsg(Env.getCtx(), "ArchivedReports") 
					+ " (" + reportCount + ")");
			m_reports.addEventListener(Events.ON_CLICK, this);
			m_popup.appendChild(m_reports);
		}
		//	All Reports
		int allReports = MArchive.getReportCountByTableId(m_AD_Table_ID, null);
		if (allReports > 0)
		{
			m_reportsAll = new Menuitem(Msg.getMsg(Env.getCtx(), "ArchivedReportsAll") 
					+ " (" + allReports + ")");
			m_reportsAll.addEventListener(Events.ON_CLICK, this);
			m_popup.appendChild(m_reportsAll);
		}
		
		if (documentCount == 0 && reportCount == 0 && allReports == 0)
			m_popup.appendChild(new Menuitem(Msg.getMsg(Env.getCtx(), "ArchivedNone")));
			
		Popup popup = LayoutUtils.findPopup(invoker);
		if (popup != null)
		{
			popup.appendChild(m_popup);
		}
		else
		{
			m_popup.setPage(invoker.getPage());
			LayoutUtils.autoDetachOnClose(m_popup);
		}
		m_popup.open(invoker, "after_start");
	}	//	getArchives
	
	/**
	 * 	Listener
	 *	@param e event
	 */
	@Override
	public void onEvent(Event e) throws Exception 
	{
		if (e.getTarget() instanceof Menuitem) 
		{
			//open archive viewer
			int AD_Form_ID = FORM_ARCHIVEVIEWER;	//	ArchiveViewer
			ADForm form = ADForm.openForm(AD_Form_ID);
			
			WArchiveViewer av = (WArchiveViewer) form.getICustomForm();
			av.setShowQuery(false);
			if (e.getTarget() == m_documents)
				av.query(false, m_AD_Table_ID, m_Record_ID, m_Record_UU);
			else if (e.getTarget() == m_reports)
				av.query(true, m_AD_Table_ID, m_Record_ID, m_Record_UU);
			else	//	all Reports
				av.query(true, m_AD_Table_ID, 0);
			
			if (m_popup.getParent() instanceof Popup) {
				((Popup)m_popup.getParent()).close();
			}

			form.setAttribute(Window.MODE_KEY, form.getWindowMode());
			SessionManager.getAppDesktop().showWindow(form);
		}
	}
}
