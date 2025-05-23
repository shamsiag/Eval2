/**********************************************************************
* This file is part of Adempiere ERP Bazaar                           *
* http://www.adempiere.org                                            *
*                                                                     *
* Copyright (C) Carlos Ruiz - globalqss                               *
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
*                                                                     *
* Sponsors:                                                           *
* - Company (http://www.globalqss.com)                                *
***********************************************************************/

package org.adempiere.process;

import java.util.Enumeration;
import java.util.logging.Level;

import org.compiere.model.MClientInfo;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MForm;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTab;
import org.compiere.model.MTask;
import org.compiere.model.MToolBarButton;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.model.MWindow;
import org.compiere.model.X_ASP_Field;
import org.compiere.model.X_ASP_Form;
import org.compiere.model.X_ASP_Process;
import org.compiere.model.X_ASP_Process_Para;
import org.compiere.model.X_ASP_Tab;
import org.compiere.model.X_ASP_Task;
import org.compiere.model.X_ASP_Window;
import org.compiere.model.X_ASP_Workflow;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWorkflow;

/**
 * 	Generate ASP entries for a level
 *	
 *  @author Carlos Ruiz
 */
@org.adempiere.base.annotation.Process
public class ASPGenerateLevel extends SvrProcess
{
	private String  p_ASP_Status;
	private int p_AD_Menu_ID;
	private boolean p_IsGenerateFields;
	private int p_ASP_Level_ID;	
	private int noWindows = 0;
	private int noTabs = 0;
	private int noFields = 0;
	private int noProcesses = 0;
	private int noParameters = 0;
	private int noForms = 0;
	private int noTasks = 0;
	private int noWorkflows = 0;
	
	/**
	 * 	Prepare
	 */
	protected void prepare ()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.equals("ASP_Status"))
				p_ASP_Status = (String) para.getParameter();
			else if (name.equals("AD_Menu_ID"))
				p_AD_Menu_ID = para.getParameterAsInt();
			else if (name.equals("IsGenerateFields"))
				p_IsGenerateFields = para.getParameter().equals("Y");
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
		}
		p_ASP_Level_ID = getRecord_ID();
	}	//	prepare

	/**
	 * 	Process
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		StringBuilder msglog = new StringBuilder("ASP_Status=").append(p_ASP_Status) 
				.append(", AD_Menu_ID=").append(p_AD_Menu_ID)
				.append(", IsGenerateFields=").append(p_IsGenerateFields);
		if (log.isLoggable(Level.INFO)) log.info(msglog.toString());
		
		MClientInfo clientInfo = MClientInfo.get(getCtx(), getAD_Client_ID(), get_TrxName());
		int AD_Tree_ID = clientInfo.getAD_Tree_Menu_ID();
		MTree thisTree = new MTree (getCtx(), AD_Tree_ID, true, true, true, get_TrxName());
		MTreeNode node;
		if (p_AD_Menu_ID > 0)
			node = thisTree.getRoot().findNode(p_AD_Menu_ID);
		else
			node = thisTree.getRoot();
			
		// Navigate the menu and add every non-summary node
		if (node != null)
		{
			Enumeration<?> en = node.preorderEnumeration();
			while (en.hasMoreElements())
			{
				MTreeNode nn = (MTreeNode)en.nextElement();
				if (!nn.isSummary())
					addNodeToLevel(nn);
			}
		}
		
		if (noWindows > 0)
			addLog("Window " + noWindows);
		if (noTabs > 0)
			addLog("Tab " + noTabs);
		if (noFields > 0)
			addLog("Field " + noFields);
		if (noProcesses > 0)
			addLog("Process " + noProcesses);
		if (noParameters > 0)
			addLog("Process Parameter " + noParameters);
		if (noForms > 0)
			addLog("Form " + noForms);
		if (noTasks > 0)
			addLog("Task " + noTasks);
		if (noWorkflows > 0)
			addLog("Workflow " + noWorkflows);

		return "@OK@";
	}	//	doIt

	private void addNodeToLevel(MTreeNode nn) {
		// Add Menu
		MMenu menu = new MMenu(getCtx(), nn.getNode_ID(), get_TrxName());

		if (menu.getAction().equals(MMenu.ACTION_Window)) {
			generateWindow(menu.getAD_Window_ID());
		} else if (menu.getAction().equals(MMenu.ACTION_Process) || menu.getAction().equals(MMenu.ACTION_Report)) {
			generateProcess(menu.getAD_Process_ID());
		} else if (menu.getAction().equals(MMenu.ACTION_Form)) {
			generateForm(menu.getAD_Form_ID());
		} else if (menu.getAction().equals(MMenu.ACTION_Task)) {
			generateTask(menu.getAD_Task_ID());
		} else if (menu.getAction().equals(MMenu.ACTION_WorkFlow)) {
			generateWorkflow(menu.getAD_Workflow_ID());
		}		
	}

	private void generateProcess(int p_AD_Process_ID) {
		// Add Process and Parameters
		MProcess process = new MProcess(getCtx(), p_AD_Process_ID, get_TrxName());
		int asp_process_id = DB.getSQLValueEx(get_TrxName(),
				"SELECT ASP_Process_ID FROM ASP_Process WHERE ASP_Level_ID = ? AND AD_Process_ID = ?",
				p_ASP_Level_ID, process.getAD_Process_ID());
		X_ASP_Process aspProcess = null;
		if (asp_process_id < 1) {
			aspProcess = new X_ASP_Process(getCtx(), 0, get_TrxName());
			aspProcess.setASP_Level_ID(p_ASP_Level_ID);
			aspProcess.setAD_Process_ID(process.getAD_Process_ID());
			aspProcess.setASP_Status(p_ASP_Status);
			if (aspProcess.save()) {
				noProcesses++;
				asp_process_id = aspProcess.getASP_Process_ID();
			}
		} else {
			aspProcess = new X_ASP_Process(getCtx(), asp_process_id, get_TrxName());
		}
		// parameters
		for (MProcessPara processpara : process.getParameters()) {
			if (DB.getSQLValueEx(
					get_TrxName(),
					"SELECT COUNT(*) FROM ASP_Process_Para WHERE ASP_Process_ID = ? AND AD_Process_Para_ID = ?",
					asp_process_id, processpara.getAD_Process_Para_ID()) < 1) {
				X_ASP_Process_Para aspProcess_Para = new X_ASP_Process_Para(getCtx(), 0, get_TrxName());
				aspProcess_Para.setASP_Process_ID(asp_process_id);
				aspProcess_Para.setAD_Process_Para_ID(processpara.getAD_Process_Para_ID());
				aspProcess_Para.setASP_Status(p_ASP_Status);
				if (aspProcess_Para.save())
					noParameters++;
			}
		}
		if (process.getAD_Workflow_ID() > 0) {
			generateWorkflow(process.getAD_Workflow_ID());
		}
		if (process.getAD_Form_ID() > 0) {
			generateForm(process.getAD_Form_ID());
		}
	}

	private void generateWorkflow(int p_AD_Workflow_ID) {
		// Add Workflow and Nodes
		MWorkflow workflow = new MWorkflow(getCtx(), p_AD_Workflow_ID, get_TrxName());
		if (DB.getSQLValueEx(
				get_TrxName(),
				"SELECT COUNT(*) FROM ASP_Workflow WHERE ASP_Level_ID = ? AND AD_Workflow_ID = ?",
				p_ASP_Level_ID, workflow.getAD_Workflow_ID()) < 1) {
			X_ASP_Workflow aspWorkflow = new X_ASP_Workflow(getCtx(), 0, get_TrxName());
			aspWorkflow.setASP_Level_ID(p_ASP_Level_ID);
			aspWorkflow.setAD_Workflow_ID(workflow.getAD_Workflow_ID());
			aspWorkflow.setASP_Status(p_ASP_Status);
			if (aspWorkflow.save())
				noWorkflows++;
		}
		for (MWFNode node : workflow.getNodes(false, getAD_Client_ID())) {
			if ( ( MWFNode.ACTION_AppsProcess.equals(node.getAction()) || MWFNode.ACTION_AppsReport.equals(node.getAction()) ) && node.getAD_Process_ID() > 0) {
				generateProcess(node.getAD_Process_ID());
			} else if (MWFNode.ACTION_AppsTask.equals(node.getAction()) && node.getAD_Task_ID() > 0) {
				generateTask(node.getAD_Task_ID());
			} else if (MWFNode.ACTION_UserForm.equals(node.getAction()) && node.getAD_Form_ID() > 0) {
				generateForm(node.getAD_Form_ID());
			} else if (MWFNode.ACTION_UserWindow.equals(node.getAction()) && node.getAD_Window_ID() > 0) {
				generateWindow(node.getAD_Window_ID());
			}
		}
	}

	private void generateForm(int p_AD_Form_ID) {
		// Add Form
		MForm form = new MForm(getCtx(), p_AD_Form_ID, get_TrxName());
		if (DB.getSQLValueEx(
				get_TrxName(),
				"SELECT COUNT(*) FROM ASP_Form WHERE ASP_Level_ID = ? AND AD_Form_ID = ?",
				p_ASP_Level_ID, form.getAD_Form_ID()) < 1) {
			X_ASP_Form aspForm = new X_ASP_Form(getCtx(), 0, get_TrxName());
			aspForm.setASP_Level_ID(p_ASP_Level_ID);
			aspForm.setAD_Form_ID(form.getAD_Form_ID());
			aspForm.setASP_Status(p_ASP_Status);
			if (aspForm.save())
				noForms++;
		}
	}

	private void generateTask(int p_AD_Task_ID) {
		// Add Task
		MTask task = new MTask(getCtx(), p_AD_Task_ID, get_TrxName());
		if (DB.getSQLValueEx(
				get_TrxName(),
				"SELECT COUNT(*) FROM ASP_Task WHERE ASP_Level_ID = ? AND AD_Task_ID = ?",
				p_ASP_Level_ID, task.getAD_Task_ID()) < 1) {
			X_ASP_Task aspTask = new X_ASP_Task(getCtx(), 0, get_TrxName());
			aspTask.setASP_Level_ID(p_ASP_Level_ID);
			aspTask.setAD_Task_ID(task.getAD_Task_ID());
			aspTask.setASP_Status(p_ASP_Status);
			if (aspTask.save())
				noTasks++;
		}
	}

	private void generateWindow(int p_AD_Window_ID) {
		MWindow window = new MWindow(getCtx(), p_AD_Window_ID, get_TrxName());
		int asp_window_id = DB.getSQLValueEx(get_TrxName(),
				"SELECT ASP_Window_ID FROM ASP_Window WHERE ASP_Level_ID = ? AND AD_Window_ID = ?",
				p_ASP_Level_ID, window.getAD_Window_ID());
		X_ASP_Window aspWindow = null;
		if (asp_window_id < 1) {
			// Add Window, Tabs and Fields (if IsGenerateFields)
			aspWindow = new X_ASP_Window(getCtx(), 0, get_TrxName());
			aspWindow.setASP_Level_ID(p_ASP_Level_ID);
			aspWindow.setAD_Window_ID(window.getAD_Window_ID());
			aspWindow.setASP_Status(p_ASP_Status);
			if (aspWindow.save()) {
				noWindows++;
				asp_window_id = aspWindow.getASP_Window_ID();
			}
		} else {
			aspWindow = new X_ASP_Window(getCtx(), asp_window_id, get_TrxName());
		}
		// tabs
		for (MTab tab : window.getTabs(true, get_TrxName())) {
			int asp_tab_id = DB.getSQLValueEx(get_TrxName(),
					"SELECT ASP_Tab_ID FROM ASP_Tab WHERE ASP_Window_ID = ? AND AD_Tab_ID = ?",
					asp_window_id, tab.getAD_Tab_ID());
			X_ASP_Tab aspTab = null;
			if (asp_tab_id < 1) {
				aspTab = new X_ASP_Tab(getCtx(), 0, get_TrxName());
				aspTab.setASP_Window_ID(asp_window_id);
				aspTab.setAD_Tab_ID(tab.getAD_Tab_ID());
				aspTab.setASP_Status(p_ASP_Status);
				aspTab.setAllFields(! p_IsGenerateFields);
				if (aspTab.save()) {
					noTabs++;
					asp_tab_id = aspTab.getASP_Tab_ID();
				}
			} else {
				aspTab = new X_ASP_Tab(getCtx(), asp_tab_id, get_TrxName());
			}
			if (tab.getAD_Process_ID() > 0) {
				generateProcess(tab.getAD_Process_ID());
			}
			for (MToolBarButton tb : MToolBarButton.getProcessButtonOfTab(tab.getAD_Tab_ID(), get_TrxName())) {
				if (tb.getAD_Process_ID() > 0) {
					generateProcess(tb.getAD_Process_ID());
				}
			}
			// fields
			for (MField field : tab.getFields(true, get_TrxName())) {
				if (p_IsGenerateFields) {
					if (DB.getSQLValueEx(
							get_TrxName(),
							"SELECT COUNT(*) FROM ASP_Field WHERE ASP_Tab_ID = ? AND AD_Field_ID = ?",
							aspTab.getASP_Tab_ID(), field.getAD_Field_ID()) < 1) {
						X_ASP_Field aspField = new X_ASP_Field(getCtx(), 0, get_TrxName());
						aspField.setASP_Tab_ID(aspTab.getASP_Tab_ID());
						aspField.setAD_Field_ID(field.getAD_Field_ID());
						aspField.setASP_Status(p_ASP_Status);
						if (aspField.save())
							noFields++;
					}
				}
				// verify if a field is a button and assign permission to the corresponding process
				MColumn column = MColumn.get(getCtx(), field.getAD_Column_ID());
				if (column.getAD_Reference_ID() == DisplayType.Button) {
					if (column.getAD_Process_ID() > 0) {
						generateProcess(column.getAD_Process_ID());
					}
				}
			}
		}
	}

}	//	ASPGenerateLevel
