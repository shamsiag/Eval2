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

import java.awt.Dimension;
import java.awt.Image;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.Icon;

import org.apache.ecs.xhtml.a;
import org.apache.ecs.xhtml.h2;
import org.apache.ecs.xhtml.h3;
import org.apache.ecs.xhtml.h4;
import org.apache.ecs.xhtml.i;
import org.apache.ecs.xhtml.p;
import org.apache.ecs.xhtml.strong;
import org.apache.ecs.xhtml.table;
import org.apache.ecs.xhtml.td;
import org.apache.ecs.xhtml.th;
import org.apache.ecs.xhtml.tr;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.WebDoc;

/**
 *	Window Model
 *
 *  @author 	Jorg Janke
 *  @version 	$Id: GridWindow.java,v 1.4 2006/07/30 00:51:02 jjanke Exp $
 */
public class GridWindow implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4533195514781938417L;

	/**
	 * 	Get Grid Window
	 *  @param ctx context
	 *  @param WindowNo window no for ctx
	 *  @param AD_Window_ID window id
	 *	@return window or null if not found
	 */
	public static GridWindow get (Properties ctx, int WindowNo, int AD_Window_ID)
	{
		return get(ctx, WindowNo, AD_Window_ID, false);
	}
	
	/**
	 * 	Get Grid Window
	 *  @param ctx context
	 *  @param WindowNo window no for ctx
	 *  @param AD_Window_ID window id
	 *  @param virtual
	 *	@return window or null if not found
	 */
	public static GridWindow get (Properties ctx, int WindowNo, int AD_Window_ID, boolean virtual)
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Window=" + WindowNo + ", AD_Window_ID=" + AD_Window_ID);
		GridWindowVO mWindowVO = GridWindowVO.get (AD_Window_ID, WindowNo);
		if (mWindowVO == null)
			return null;
		return new GridWindow(mWindowVO, virtual);
	}	//	get

	/**
	 *	Constructor
	 *  @param vo value object
	 */
	public GridWindow (GridWindowVO vo)
	{
		this(vo, false);
	}
	
	/**
	 *	Constructor
	 *  @param vo value object
	 *  @param virtual true to use virtual buffer
	 */
	public GridWindow (GridWindowVO vo, boolean virtual)
	{
		m_vo = vo;
		m_virtual = virtual;
		if (loadTabData())
			enableEvents();
	}	//	MWindow

	/** Value Object                */
	private GridWindowVO   	m_vo;
	/** use virtual table			*/
	private boolean m_virtual;
	/**	Tabs						*/
	private ArrayList<GridTab>	m_tabs = new ArrayList<GridTab>();
	/** Model last updated			*/
	private Timestamp		m_modelUpdated = null;

	private Set<GridTab> initTabs = new HashSet<GridTab>();
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(GridWindow.class);
	
	/**
	 *	Dispose
	 */
	public void dispose()
	{
		if (log.isLoggable(Level.INFO)) log.info("AD_Window_ID=" + m_vo.AD_Window_ID);
		for (int i = 0; i < getTabCount(); i++)
			getTab(i).dispose();
		m_tabs.clear();
		m_tabs = null;
	}	//	dispose

	/**
	 * Wait until asynchronous loading of all tab is complete.
	 */
	public void loadCompete ()
	{
		//  for all tabs
		for (int i = 0; i < getTabCount(); i++)
			getTab(i).getMTable().loadComplete();
	}   //  loadComplete

	/**
	 *	Get Tab data and create GridTab(s)
	 *  @return true if tab loaded
	 */
	private boolean loadTabData()
	{
		if (m_vo.Tabs == null)
			return false;

		for (int t = 0; t < m_vo.Tabs.size(); t++)
		{
			GridTabVO mTabVO = (GridTabVO)m_vo.Tabs.get(t);
			if (mTabVO != null)
			{
				GridTab mTab = new GridTab(mTabVO, this, m_virtual);
				Env.setContext(m_vo.ctx, m_vo.WindowNo, t, GridTab.CTX_TabLevel, Integer.toString(mTab.getTabLevel()));
				m_tabs.add(mTab);
			}
		}	//  for all tabs
		return true;
	}	//	loadTabData

	/**
	 * Is tab initialize
	 * @param index tab index
	 * @return true if tab at index have been initialized
	 */
	public boolean isTabInitialized(int index)
	{
		GridTab mTab = m_tabs.get(index);
		return initTabs.contains(mTab);
	}
	
	/**
	 * Initialize tab
	 * @param index tab index
	 */
	public void initTab(int index)
	{
		GridTab mTab = m_tabs.get(index);
		if (initTabs.contains(mTab)) return;		
		mTab.initTab(false);		
		//		Set Link Column
		if (mTab.getLinkColumnName().length() == 0)
		{
			ArrayList<String> parents = mTab.getParentColumnNames();
			//	No Parent - no link
			if (parents.size() == 0)
				;
			//	Standard case
			else if (parents.size() == 1)
				mTab.setLinkColumnName((String)parents.get(0));
			else
			{
				GridTab parentTab = null;
				//	More than one parent.
				//	Search prior tabs for the "right parent"
				//	for all previous tabs
				for (int i = 0; i < index; i++)
				{
					if (m_tabs.get(i).getTabLevel() + 1 == mTab.getTabLevel()) {
						parentTab = m_tabs.get(i);
						break;						
					}
				}
				
				if (parentTab != null)
				{
					//	we have a tab
					String tabKey = parentTab.getKeyColumnName();		//	may be ""
					//	look, if one of our parents is the key of that tab
					for (int j = 0; j < parents.size(); j++)
					{
						String parent = (String)parents.get(j);
						if (parent.equals(tabKey))
						{
							mTab.setLinkColumnName(parent);
							break;
						}
						//	The tab could have more than one key, look into their parents
						if (tabKey.equals(""))
							for (int k = 0; k < parentTab.getParentColumnNames().size(); k++)
								if (parent.equals(parentTab.getParentColumnNames().get(k)))
								{
									mTab.setLinkColumnName(parent);
									break;
								}
					}	//	for all parents
				}	//	for all previous tabs
			}	//	parents.size > 1
		}	//	set Link column
		mTab.setLinkColumnName(null);	//	overwrites, if AD_Column_ID exists
		//
		initTabs.add(mTab);
	}
	
	/**
	 *  Get Window Icon
	 *  @return Icon for Window
	 */
	public Image getImage()
	{
		if (m_vo.AD_Image_ID == 0)
			return null;
		//
		MImage mImage = MImage.get(Env.getCtx(), m_vo.AD_Image_ID);
		return mImage.getImage();
	}   //  getImage
	
	/**
	 * @return MImage
	 */
	public MImage getMImage()
	{
		if (m_vo.AD_Image_ID == 0)
			return null;
		//
		MImage mImage = MImage.get(Env.getCtx(), m_vo.AD_Image_ID);
		return mImage;
	}

	/**
	 *  Get Window Icon
	 *  @return Icon for Window
	 */
	public Icon getIcon()
	{
		if (m_vo.AD_Image_ID == 0)
			return null;
		//
		MImage mImage = MImage.get(Env.getCtx(), m_vo.AD_Image_ID);
		return mImage.getIcon();
	}   //  getIcon

	/**
	 *  Get Color
	 *  @return MColor or null
	 */
	public MColor getColor()
	{
		if (m_vo.AD_Color_ID == 0)
			return null;
		MColor mc = new MColor(m_vo.ctx,  m_vo.AD_Color_ID, null);
		return mc;
	}   //  getColor

	/**
	 *	@return true if window is for SO Trx
	 */
	public boolean isSOTrx()
	{
		return m_vo.IsSOTrx;
	}	//	isSOTrx
		
	/**
	 *  Open and query first Tab (events should be enabled) and navigate to first row.
	 */
	public void query()
	{
		if (log.isLoggable(Level.INFO))
			log.info("");
		GridTab tab = getTab(0);
		tab.query(false, 0, 0);
		if (tab.getRowCount() > 0)
			tab.navigate(0);
	}   //  open

	/**
	 *  Enable events for all tabs (listen for GridTable events)
	 */
	private void enableEvents()
	{
		for (int i = 0; i < getTabCount(); i++)
			getTab(i).enableEvents();
	}   //  enableEvents

	/**
	 *	Get number of Tabs
	 *  @return number of tabs
	 */
	public int getTabCount()
	{
		return m_tabs.size();
	}	//	getTabCount

	/**
	 *  @param i tab index
	 *  @return GridTab at tab index or null if tab index is invalid
	 */
	public GridTab getTab (int i)
	{
		if (i < 0 || i+1 > m_tabs.size())
			return null;
		return (GridTab)m_tabs.get(i);
	}	//	getTab
	
	/**
	 * @param tab
	 * @return tab index
	 */
	public int getTabIndex(GridTab tab)
	{
		return m_tabs.indexOf(tab);
	}

	/**
	 *	Get Window_ID
	 *  @return AD_Window_ID
	 */
	public int getAD_Window_ID()
	{
		return m_vo.AD_Window_ID;
	}	//	getAD_Window_ID

	/**
	 *	Get WindowNo
	 *  @return WindowNo
	 */
	public int getWindowNo()
	{
		return m_vo.WindowNo;
	}	//	getWindowNo

	/**
	 *	Get Name
	 *  @return name
	 */
	public String getName()
	{
		return m_vo.Name;
	}	//	getName

	/**
	 *	Get Description
	 *  @return Description
	 */
	public String getDescription()
	{
		return m_vo.Description;
	}	//	getDescription

	/**
	 *	Get Help
	 *  @return Help
	 */
	public String getHelp()
	{
		return m_vo.Help;
	}	//	getHelp

	/**
	 *	Get Window Type
	 *  @return Window Type see WindowType_*
	 */
	public String getWindowType()
	{
		return m_vo.WindowType;
	}	//	getWindowType

	/**
	 * Get EntityType
	 * @return Window Entity Type
	 */
	public String getEntityType()
	{
		return m_vo.EntityType;
	}

	/**
	 *	Is Transaction Window
	 *  @return true if this is transaction window
	 */
	public boolean isTransaction()
	{
		return m_vo.WindowType.equals(GridWindowVO.WINDOWTYPE_TRX);
	}   //	isTransaction

	/**
	 * 	Get Window Size
	 *	@return window size or null if not set
	 */
	public Dimension getWindowSize()
	{
		if (m_vo.WinWidth != 0 && m_vo.WinHeight != 0)
			return new Dimension (m_vo.WinWidth, m_vo.WinHeight);
		return null;
	}	//	getWindowSize

	/**
	 *  To String
	 *  @return String representation
	 */
	public String toString()
	{
		StringBuilder msgreturn = new StringBuilder("MWindow[").append(m_vo.WindowNo).append(",").append(m_vo.Name).append(" (").append(m_vo.AD_Window_ID).append(")]");
		return msgreturn.toString();
	}   //  toString

	/**
	 * 	Get Help HTML Document
	 * 	@param javaClient true if java client, false for browser
	 *	@return help document
	 */
	public WebDoc getHelpDoc (boolean javaClient)
	{
		StringBuilder title = new StringBuilder(Msg.getMsg(Env.getCtx(), "Window")).append(": ").append(getName());
		WebDoc doc = null;
		doc = WebDoc.create (false, title.toString(), javaClient);
		
		td center  = doc.addPopupCenter(false);
		//	Window
		if (getDescription().length() != 0)
			center.addElement(new p().addElement(new i(getDescription())));
		if (getHelp().length() != 0)
			center.addElement(new p().addElement(getHelp()));

		center.addElement(new a().setName("Tabs")).addElement(new h3(Msg.getMsg(Env.getCtx(), "Tabs")).addAttribute("ALIGN", "left"));
		//	List of all Tabs in current window
		int size = getTabCount();
		p p = new p();
		for (int i = 0; i < size; i++)
		{
			GridTab tab = getTab(i);
			if (i > 0)
				p.addElement(" | ");
			p.addElement(new a("#Tab"+i).addElement(tab.getName()));
		}
		center.addElement(p)
			.addElement(new p().addElement(WebDoc.NBSP));

		//	For all Tabs
		for (int i = 0; i < size; i++)
		{
			table table = new table("1", "5", "5", "100%", null);
			table.setBorder("1px").setCellSpacing(0);
			GridTab tab = getTab(i);
			table tabHeader = new table();
			tabHeader.setBorder("0").setCellPadding(0).setCellSpacing(0);
			tabHeader.addElement(new tr()
				.addElement(new td().addElement(new a().setName("Tab" + i))
						.addElement(new h2(Msg.getMsg(Env.getCtx(), "Tab") + ": " + tab.getName())))
				.addElement(new td().addElement(WebDoc.NBSP).addElement(WebDoc.NBSP)
						.addElement(new a("#Tabs").addElement("..").addAttribute("title", "Up one level"))));
			tr tr = new tr()
				.addElement(new th()
				.addElement(tabHeader));
					
			if (tab.getDescription().length() != 0)
				tr.addElement(new th()
					.addElement(new i(tab.getDescription())));
			else
				tr.addElement(new th()
					.addElement(WebDoc.NBSP));
			table.addElement(tr);
			//	Description
			td td = new td().setColSpan(2);
			if (tab.getHelp().length() != 0)
				td.addElement(new p().addElement(tab.getHelp()));
			//	Links to Fields
			td.addElement(new a().setName("Fields"+i));
			td.addElement(new h4(Msg.getMsg(Env.getCtx(), "Fields")).addAttribute("ALIGN", "left"));
			p = new p();
			if (!tab.isLoadComplete())
				this.initTab(i);
			for (int j = 0; j < tab.getFieldCount(); j++)
			{
				GridField field = tab.getField(j);
				// hidden fields should not be displayed - teo_sarca, [ 1667073 ] 
				if (!field.isDisplayed(false)) {
					continue;
				}
				String hdr = field.getHeader();
				if (hdr != null && hdr.length() > 0)
				{
					if (j > 0)
						p.addElement(" | ");
					p.addElement(new a("#Field" + i + "-" + j, hdr));
				}
			}
			td.addElement(p);
			table.addElement(new tr().addElement(td));

			//	For all Fields
			for (int j = 0; j < tab.getFieldCount(); j++)
			{
				GridField field = tab.getField(j);
				// hidden fields should not be displayed - teo_sarca, [ 1667073 ] 
				if (!field.isDisplayed(false)) {
					continue;
				}
				String hdr = field.getHeader();
				if (hdr != null && hdr.length() > 0)
				{
					table fieldHeader = new table();
					fieldHeader.setBorder("0").setCellPadding(0).setCellSpacing(0);
					fieldHeader.addElement(new tr()
					.addElement(new td().addElement(new a().setName("Field" + i + "-" + j))
						.addElement(new h3(Msg.getMsg(Env.getCtx(), "Field") + ": " + hdr)))
					.addElement(new td().addElement(WebDoc.NBSP).addElement(WebDoc.NBSP)
						.addElement(new strong().addElement(new a("#Fields"+i).addElement("..").addAttribute("title", "Up one level")))));
					
					td = new td().setColSpan(2).addElement(fieldHeader);
						
					if (field.getDescription().length() != 0)
						td.addElement(new i(field.getDescription()));
					//
					if (field.getHelp().length() != 0)
						td.addElement(new p().addElement(field.getHelp()));
					table.addElement(new tr().addElement(td));
				}
			}	//	for all Fields
			
			center.addElement(table);
			center.addElement(new p().addElement(WebDoc.NBSP));
		}	//	for all Tabs
		
	   	//System.out.println(doc.toString());
		return doc;
	}	//	getHelpDoc

	/**
	 * 	Get Model last Updated
	 * 	@param recalc true to always re-query from DB
	 *	@return last updated timestamp
	 */
	public Timestamp getModelUpdated (boolean recalc)
	{
		if (recalc || m_modelUpdated == null)
		{
			String sql = "SELECT MAX(w.Updated), MAX(t.Updated), MAX(tt.Updated), MAX(f.Updated), MAX(c.Updated) "
					+ "FROM AD_Window w"
					+ " INNER JOIN AD_Tab t ON (w.AD_Window_ID=t.AD_Window_ID)"
					+ " INNER JOIN AD_Table tt ON (t.AD_Table_ID=tt.AD_Table_ID)"
					+ " INNER JOIN AD_Field f ON (t.AD_Tab_ID=f.AD_Tab_ID)"
					+ " INNER JOIN AD_Column c ON (f.AD_Column_ID=c.AD_Column_ID) "
					+ "WHERE w.AD_Window_ID=?";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql, null);
				pstmt.setInt (1, getAD_Window_ID());
				rs = pstmt.executeQuery ();
				if (rs.next ())
				{
					m_modelUpdated = rs.getTimestamp(1);	//	Window
					Timestamp ts = rs.getTimestamp(2);		//	Tab
					if (ts.after(m_modelUpdated))
						m_modelUpdated = ts;
					ts = rs.getTimestamp(3);				//	Table
					if (ts.after(m_modelUpdated))
						m_modelUpdated = ts;
					ts = rs.getTimestamp(4);				//	Field
					if (ts.after(m_modelUpdated))
						m_modelUpdated = ts;
					ts = rs.getTimestamp(5);				//	Column
					if (ts.after(m_modelUpdated))
						m_modelUpdated = ts;
				}
			}
			catch (Exception e)
			{
				log.log (Level.SEVERE, sql, e);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
			}
		}
		return m_modelUpdated;
	}	//	getModelUpdated

	/**
	 * @return AD_Window_UU
	 */
	public String getAD_Window_UU() {
		return m_vo.AD_Window_UU;
	}

	/**
	 * Get {@link GridTab} by Tab ID
	 * @param ad_tab_id
	 * @return {@link GridTab}
	 */
	public GridTab getGridTab (int ad_tab_id)
	{
		for (int i = 0; i < m_tabs.size(); i++)
		{
			GridTab tab = getTab(i);
			if (tab.getAD_Tab_ID()==ad_tab_id)
				return tab;
		}

		return null;

	}	//	getTab

	/**
	 * Get {@link GridTab} by Tab UUID
	 * @param ad_tab_uu
	 * @return {@link GridTab}
	 */
	public GridTab getGridTab (String ad_tab_uu)
	{
		for (int i = 0; i < m_tabs.size(); i++)
		{
			GridTab tab = getTab(i);
			if (tab.getAD_Tab_UU() != null && tab.getAD_Tab_UU().equals(ad_tab_uu))
				return tab;
		}

		return null;

	}	//	getTab
}	//	MWindow
