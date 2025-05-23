/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
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
package org.adempiere.webui.component;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;

/**
 * Composite component with {@link Textbox} and {@link Button}
 * @author Low Heng Sin
 */
public class EditorBox extends Div {
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -3152111756471436612L;
	@Deprecated
	protected PropertyChangeSupport m_propertyChangeListeners = new PropertyChangeSupport(this);
	protected Textbox txt;
	protected Button btn;

	/**
	 * Default constructor
	 */
	public EditorBox() {
		initComponents();
	}

	/**
	 * @param text
	 */
	public EditorBox(String text) {
		initComponents();
		setText(text);
	}

	/**
	 * @param imageSrc
	 */
	public void setButtonImage(String imageSrc) {
		btn.setImage(imageSrc);
	}

	/**
	 * Layout component
	 */
	private void initComponents() {
		txt = new Textbox();
		txt.setSclass("editor-input");
		ZKUpdateUtil.setHflex(txt, "0");
		appendChild(txt);
		btn = new Button();
		btn.setTabindex(-1);
		ZKUpdateUtil.setHflex(btn, "0");
		btn.setSclass("editor-button");
		appendChild(btn);
		
		LayoutUtils.addSclass("editor-box", this);
		setTableEditorMode(false);
	}

	/**
	 * @return Textbox component
	 */
	public Textbox getTextbox() {
		return txt;
	}

	/**
	 * Set value to text box
	 * @param value
	 */
	public void setText(String value) {
		txt.setText(value);
	}

	/**
	 * @return text from text box
	 */
	public String getText() {
		return txt.getText();
	}

	/**
	 * Enable/disable component.<br/>
	 * Hide button when component is disabled/readonly. 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		txt.setReadonly(!enabled);
		btn.setEnabled(enabled);
		btn.setVisible(enabled);
		if (enabled) {
			if (btn.getParent() != txt.getParent())
				btn.setParent(txt.getParent());
		} else {
			if (btn.getParent() != null)
				btn.detach();
		}
		if (enabled) {
			LayoutUtils.removeSclass("editor-input-disd", txt);
		} else {
			LayoutUtils.addSclass("editor-input-disd", txt);
		}
	}

	/**
	 * @return true if enable, false otherwise
	 */
	public boolean isEnabled() {
		return btn.isEnabled();
	}

	/**
	 * If evtnm is ON_CLICK, add listener to {@link #btn}, else add listener to {@link #txt}
	 * @param evtnm Event name
	 * @param listener EventListener
	 */
	public boolean addEventListener(String evtnm, EventListener<?> listener) {
		if (Events.ON_CLICK.equals(evtnm)) {
			return btn.addEventListener(evtnm, listener);
		} else {
			return txt.addEventListener(evtnm, listener);
		}
	}

	/**
	 * @param l PropertyChangeListener
	 * @deprecated not implemented
	 */
	@Deprecated
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		m_propertyChangeListeners.addPropertyChangeListener(l);
	}

	/**
	 * Set tooltip text for text box component
	 * @param tooltiptext
	 */
	public void setToolTipText(String tooltiptext) {
		txt.setTooltiptext(tooltiptext);
	}
	
	/**
	 * @return Button
	 */
	public Button getButton() {
		return btn;
	}
	
	/**
	 * Set grid view mode.
	 * @param flag
	 */
	public void setTableEditorMode(boolean flag) {
		if (flag) {
			ZKUpdateUtil.setHflex(this, "0");
			LayoutUtils.addSclass("grid-editor-input", txt);
			LayoutUtils.addSclass("grid-editor-button", btn);
		} else {
			ZKUpdateUtil.setHflex(this, "1");
			LayoutUtils.removeSclass("grid-editor-input", txt);
			LayoutUtils.removeSclass("grid-editor-button", btn);
		}
			
	}

	/**
	 * Set focus to text box
	 */
	@Override
	public void focus() {
		txt.focus();
	}
}
