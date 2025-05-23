/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
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
 * Copyright (C) 2003-2010 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): victor.perez@e-evolution.com, www.e-evolution.com          *
 *****************************************************************************/
package org.adempiere.webui.apps.form;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.function.Function;
import java.util.logging.Level;

import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.SimpleTreeModel;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.TreeUtils;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.apps.form.TreeBOM;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MProduct;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Center;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.West;

/**
 * Form to view BOM tree.
 */
@org.idempiere.ui.zk.annotation.Form(name = "org.compiere.apps.form.VTreeBOM")
public class WTreeBOM extends TreeBOM<MySimpleTreeNode> implements IFormController, EventListener<Event> {
	
	private int         	m_WindowNo = 0;
	/** Custom form/window UI instance */
	private CustomForm		m_frame = new CustomForm();
	/** BOM Tree. Child of {@link #treePane}. */
	private Tree			m_tree = new Tree();
	/** Main layout of {@link #m_frame} */
	private Borderlayout 	mainLayout = new Borderlayout();
	/** North of {@link #mainLayout}. Parameter panel. */
	private Panel			northPanel = new Panel();
	private Panel			southPanel = new Panel();
	private Label			labelProduct = new Label();
	/** Product parameter */
	private WSearchEditor   fieldProduct;
	/** West of {@link #mainLayout} */
	private West 			west = new West();
	/** BOM Implosion Y/N parameter */
	private Checkbox		implosion	= new Checkbox ();
	/** Show M_Product.Value of {@link #fieldProduct} */
	private Label			treeInfo	= new Label ();
	
	/** Center of {@link #mainLayout} */
	private Panel dataPane = new Panel();
	/** Tree panel. Child of {@link #west} */
	private Panel treePane = new Panel();

	private MySimpleTreeNode   m_selectedNode;	//	the selected model node
	/** Action buttons panel. Child of {@link #southLayout} */
	private ConfirmPanel confirmPanel = new ConfirmPanel(true);
	/** List of BOM components. Child of {@link #dataPane} */
	private WListbox tableBOM = ListboxFactory.newDataTable();
	/** Layout of {@link #northPanel} */
	private Hlayout northLayout = new Hlayout();
	/** Layout of {@link #southPanel} */
	private Hlayout southLayout = new Hlayout();
	private MySimpleTreeNode  	m_root = null;
	/** Expand or collapse all tree nodes. Child of {@link #southLayout}. */
	private Checkbox treeExpand = new Checkbox();
	
	/**
	 * Default constructor
	 */
	public WTreeBOM(){
		try{
			m_WindowNo = m_frame.getWindowNo();
			preInit();
			layoutForm ();
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "VTreeBOM.init", e);
		}
	}
	
	/**
	 * Load data into {@link #tableBOM}.
	 */
	private void loadTableBOM()
	{
		Vector<String> columnNames = getColumnNames();
		
		tableBOM.clear();

		//  Set Model
		ListModelTable model = new ListModelTable(dataBOM);
		tableBOM.setData(model, columnNames);

		setColumnClass(tableBOM);
		
	}   //  dynInit

	/**
	 * Initialize fields and listeners
	 * @throws Exception
	 */
	private void preInit() throws Exception
	{
		Properties ctx = Env.getCtx();
		Language language = Language.getLoginLanguage(); // Base Language
		MLookup m_fieldProduct = MLookupFactory.get(ctx, m_WindowNo,
				MColumn.getColumn_ID(MProduct.Table_Name, "M_Product_ID"),
				DisplayType.Search, language, MProduct.COLUMNNAME_M_Product_ID, 0, false,
				" M_Product.IsSummary = 'N'");
		
		fieldProduct = new WSearchEditor("M_Product_ID", true, false, true, m_fieldProduct)
		{
			public void setValue(Object value) {
				super.setValue(value);
				this.fireValueChange(new ValueChangeEvent(this, this.getColumnName(), getValue(), value));
				confirmPanel.getButton(ConfirmPanel.A_REFRESH).setFocus(true);
			}
		};
		
		implosion.addActionListener(this);
		treeExpand.addActionListener(this);		
	}
	
	/**
	 * Layout {@link #m_frame}
	 */
	private void layoutForm()
	{	
		ZKUpdateUtil.setWidth(m_frame, "99%");
		ZKUpdateUtil.setHeight(m_frame, "100%");
		m_frame.setStyle("position: absolute; padding: 0; margin: 0");
		m_frame.appendChild (mainLayout);
		ZKUpdateUtil.setHflex(mainLayout, "1");
		ZKUpdateUtil.setHeight(mainLayout, "100%");
		northPanel.appendChild(northLayout);
		southPanel.appendChild(southLayout);
		ZKUpdateUtil.setVflex(southPanel, "min");
				
		labelProduct.setText (Msg.getElement(Env.getCtx(), "M_Product_ID"));
		implosion.setText (Msg.getElement(Env.getCtx(), "Implosion"));
		treeInfo.setText (Msg.getElement(Env.getCtx(), "Sel_Product_ID")+": ");
		
		North north = new North();
		north.appendChild(northPanel);
		ZKUpdateUtil.setVflex(north, "min");
		ZKUpdateUtil.setWidth(northPanel, "100%");
		mainLayout.appendChild(north);

		northLayout.setValign("middle");
		northLayout.setStyle("padding: 4px;");
		northLayout.appendChild(labelProduct.rightAlign());
		if (ClientInfo.maxWidth(ClientInfo.EXTRA_SMALL_WIDTH-1))
			ZKUpdateUtil.setWidth(fieldProduct.getComponent(), "150px");
		else if (ClientInfo.maxWidth(ClientInfo.SMALL_WIDTH-1))
			ZKUpdateUtil.setWidth(fieldProduct.getComponent(), "200px");
		else if (ClientInfo.minWidth(ClientInfo.MEDIUM_WIDTH))
			ZKUpdateUtil.setWidth(fieldProduct.getComponent(), "400px");
		else
			ZKUpdateUtil.setWidth(fieldProduct.getComponent(), "300px");
		northLayout.appendChild(fieldProduct.getComponent());
		northLayout.appendChild(new Space());
		northLayout.appendChild(implosion);
		northLayout.appendChild(new Space());
		northLayout.appendChild(treeInfo);
		if (ClientInfo.maxWidth(ClientInfo.SMALL_WIDTH-1))
			treeInfo.setVisible(false);
		
		treeExpand.setText(Msg.getMsg(Env.getCtx(), "ExpandTree"));

		South south = new South();
		south.appendChild(southPanel);
		ZKUpdateUtil.setVflex(south, "min");
		ZKUpdateUtil.setWidth(southPanel, "100%");
		mainLayout.appendChild(south);
		
		southLayout.setValign("middle");
		southLayout.setStyle("padding: 4px");
		ZKUpdateUtil.setHflex(southLayout, "1");
		ZKUpdateUtil.setVflex(southLayout, "min");
		southLayout.appendChild(treeExpand);
		ZKUpdateUtil.setHflex(treeExpand, "1");
		treeExpand.setStyle("float: left;");
		southLayout.appendChild(confirmPanel);
		ZKUpdateUtil.setHflex(confirmPanel, "1");
		confirmPanel.setStyle("float: right;");
		confirmPanel.getOKButton().setVisible(false);
		confirmPanel.addComponentsBeforeRight(confirmPanel.createButton(ConfirmPanel.A_REFRESH));
		confirmPanel.addActionListener(this);
		
		mainLayout.appendChild(west);
		west.setSplittable(true);
		west.appendChild(treePane);
		treePane.appendChild(m_tree);
		m_tree.setStyle("border: none;");
		ZKUpdateUtil.setWidth(west, "33%");
		west.setAutoscroll(true);
		m_tree.addEventListener(Events.ON_SELECT, this);
		
		Center center = new Center();
		mainLayout.appendChild(center);
		center.appendChild(dataPane);
		dataPane.appendChild(tableBOM);
		ZKUpdateUtil.setHflex(dataPane, "1");
		ZKUpdateUtil.setVflex(dataPane, "1");
		center.setAutoscroll(true);				
	}
	
	/**
	 * Close form.
	 */
	public void dispose()
	{
		SessionManager.getAppDesktop().closeActiveWindow();
	}	//	dispose
	
	@Override
	public void onEvent(Event event) throws Exception {
		
		if (event.getTarget().getId().equals(ConfirmPanel.A_REFRESH))
		{
			if(getM_Product_ID() > 0)
				action_loadBOM();
		}
		if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL)) 
		{
			dispose();
		}
		if (event.getTarget().equals(treeExpand)) 
		{
			expandOrCollapse();
		}
		//  *** Tree ***
		if (event.getTarget() instanceof Tree )	
		{
			Treeitem ti = m_tree.getSelectedItem(); 
			if (ti == null) {
				log.log(Level.WARNING, "WTreeBOM.onEvent treeItem=null");
			}
			else
			{
				MySimpleTreeNode tn = (MySimpleTreeNode)ti.getValue();
				setSelectedNode(tn);
			}
		}

	}

	/**
	 * Expand of collapse all nodes of {@link #m_tree}.
	 */
	private void expandOrCollapse() {
		if (treeExpand.isChecked())
		{
			if (m_tree.getTreechildren() != null)
				TreeUtils.expandAll(m_tree);
		}
		else
		{
			if (m_tree.getTreechildren() != null)
				TreeUtils.collapseAll(m_tree);				
		}
	}
	
	/**
	 * Set selected node & load BOM.
	 * @param nd node
	 * @throws Exception 
	 */
	private void setSelectedNode (MySimpleTreeNode nd) throws Exception
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Node = " + nd);
		m_selectedNode = nd;
		if(m_selectedNode == null)
			return;

		Vector <?> nodeInfo = (Vector <?>)(m_selectedNode.getData());
        m_selectedId =  ((KeyNamePair)nodeInfo.elementAt(2)).getKey() ;

        if(m_selectedId > 0)
        	action_reloadBOM();        
	}   //  setSelectedNode
	
	private Function<NewNodeArguments<MySimpleTreeNode>, MySimpleTreeNode> createNewNodeFunction = a -> {
		MySimpleTreeNode child = a.isLeafNode() ? new MySimpleTreeNode(a.dataLine()) : new MySimpleTreeNode(a.dataLine(), new ArrayList<TreeNode<Object>>());
		a.parentNode().add(child);
		return child;
	};
	
	/**
	 * Load BOM of selected product from {@link #fieldProduct}.
	 * @throws Exception
	 */
	private void action_loadBOM() throws Exception
	{
		int M_Product_ID = getM_Product_ID(); 
		if (M_Product_ID == 0)
			return;
		MProduct product = MProduct.get(Env.getCtx(), M_Product_ID);
		treeInfo.setText (Msg.getElement(Env.getCtx(), "Sel_Product_ID")+": "+product.getValue());
		
		Vector<Object> line = newProductLine(product, 0, Env.ONE);
		
		// dummy root node, as first node is not displayed in tree  
		MySimpleTreeNode parent = new MySimpleTreeNode("Root", new ArrayList<TreeNode<Object>>());
		m_root = new MySimpleTreeNode(line, new ArrayList<TreeNode<Object>>());
		parent.add(m_root);

		if (isImplosion())
		{
			//let selected product as BOM component and show BOM parent products as tree node.
			try{
				m_tree.setModel(null);
			}catch(Exception e)
			{}
			
			if (m_tree.getTreecols() != null)
				m_tree.getTreecols().detach();
			if (m_tree.getTreefoot() != null)
				m_tree.getTreefoot().detach();
			if (m_tree.getTreechildren() != null)
				m_tree.getTreechildren().detach();

			loadBOM(product, m_root, createNewNodeFunction, true, false);
			
			Treecols treeCols = new Treecols();
			m_tree.appendChild(treeCols);
			Treecol treeCol = new Treecol();
			treeCols.appendChild(treeCol);
			
			SimpleTreeModel model = new SimpleTreeModel(parent);
			m_tree.setPageSize(-1);
			m_tree.setItemRenderer(model);
			m_tree.setModel(model);
		}
		else
		{
			//let selected product as BOM parent and show BOM components as tree node.
			try{
				m_tree.setModel(null);
			}catch(Exception e)
			{}
			
			if (m_tree.getTreecols() != null)
				m_tree.getTreecols().detach();
			if (m_tree.getTreefoot() != null)
				m_tree.getTreefoot().detach();
			if (m_tree.getTreechildren() != null)
				m_tree.getTreechildren().detach();
			
			loadBOM(product, m_root, createNewNodeFunction, false, false);
			
			Treecols treeCols = new Treecols();
			m_tree.appendChild(treeCols);
			Treecol treeCol = new Treecol();
			treeCols.appendChild(treeCol);
			
			SimpleTreeModel model = new SimpleTreeModel(parent);
			
			m_tree.setPageSize(-1);
			m_tree.setItemRenderer(model);
			m_tree.setModel(model);
		}
		
		loadTableBOM();

		treeExpand.setChecked(false);
	}

	/**
	 * Load BOM of selected tree node.
	 * @throws Exception
	 */
	private void action_reloadBOM() throws Exception
	{
		int M_Product_ID = m_selectedId;

		if (M_Product_ID == 0)
			return;
		
		MProduct product = MProduct.get(Env.getCtx(), M_Product_ID);
		treeInfo.setText (Msg.getElement(Env.getCtx(), "Sel_Product_ID")+": "+product.getValue());
		
		if (isImplosion())
		{
			loadBOM(product, m_selectedNode, createNewNodeFunction, true, true);			
		}
		else
		{
			loadBOM(product, m_selectedNode, createNewNodeFunction, false, true);
		}

		loadTableBOM();
	}

	/**
	 * @return M_Product_ID from {@link #fieldProduct}
	 */
	private int getM_Product_ID() {
		Integer Product = (Integer)fieldProduct.getValue();
		if (Product == null)
			return 0;
		return Product.intValue(); 
	}
	
	/**
	 * @return true for implosion, false for explosion.
	 */
	private boolean isImplosion() {
		return implosion.isSelected();
	}
	
	@Override
	public ADForm getForm() {
		return m_frame;
	}

}

/**
 * mySimpleTreeNode
 * - Override toString method for display.
 *  
 */
class MySimpleTreeNode extends DefaultTreeNode<Object>
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = -7430786399068849936L;

	/**
	 * @param data
	 * @param children
	 */
	public MySimpleTreeNode(Object data, List<TreeNode<Object>> children) {		
		super(data, children);		
	}
	
	public MySimpleTreeNode(Object data) {
		super(data);
	}
	
	@Override 
	public String toString(){		
		Vector <?> userObject = (Vector <?>)getData();
		// Product
		StringBuilder sb = new StringBuilder(((KeyNamePair)userObject.elementAt(2)).getName());
		// UOM
		sb.append(" ["+((KeyNamePair) userObject.elementAt(3)).getName().trim()+"]");
		// BOMQty
		BigDecimal BOMQty = (BigDecimal)(userObject.elementAt(4));
		sb.append("x"+BOMQty.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros());
		
		return sb.toString();
	}
}