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
package org.compiere.print.layout;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 *	HTML ELement.
 *  <p>
 *  Restrictions:
 *  <ul>- Label is not printed</ul>
 * 	<ul>- Alignment is ignored</ul>
 *  
 * 	@author 	Jorg Janke
 * 	@version 	$Id: HTMLElement.java,v 1.2 2006/07/30 00:53:02 jjanke Exp $
 */
public class HTMLElement extends PrintElement
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -7752468251586676726L;

	/**
	 * 	HTML String Constructor
	 * 	@param html html code
	 */
	public HTMLElement (String html)
	{
		if (html == null || html.equals(""))
			throw new IllegalArgumentException("HTMLElement is null");
		if (log.isLoggable(Level.FINE)) log.fine("Length=" + html.length()); 
		//	Create View
		m_renderer = HTMLRenderer.get(html);
	}	//	HTMLElement

	/**	View for Printing						*/
	private HTMLRenderer 	m_renderer;
		
	/**
	 * 	Layout and Calculate Size.
	 * 	Set p_width and p_height
	 * 	@return Size
	 */
	protected boolean calculateSize()
	{
		if (p_sizeCalculated)
			return true;
		//
		p_height = m_renderer.getHeight();
		p_width = m_renderer.getWidth();

		//	Limits
		if (p_maxWidth != 0f)
			p_width = p_maxWidth;
		if (p_maxHeight != 0f)
		{
			if (p_maxHeight == -1f)		//	one line only
				p_height = m_renderer.getHeightOneLine();
			else
				p_height = p_maxHeight;
		}
		//
		m_renderer.setAllocation((int)p_width, (int)p_height);
		return true;
	}	//	calculateSize

	/**
	 * 	Paint/Print.<br/>
	 *  Calculate actual Size.<br/>
	 *  The text is printed in the topmost left position - i.e. the leading is below the line.
	 * 	@param g2D Graphics
	 *  @param pageNo page number for multi page support (0 = header/footer) - ignored
	 *  @param pageStart top left Location of page
	 *  @param ctx print context
	 *  @param isView true if online view (IDs are links)
	 */
	public void paint (Graphics2D g2D, int pageNo, Point2D pageStart, Properties ctx, boolean isView)
	{
		Point2D.Double location = getAbsoluteLocation(pageStart);
		//
		Rectangle allocation = m_renderer.getAllocation();
		g2D.translate(location.x, location.y);
		m_renderer.paint(g2D, allocation);
		g2D.translate(-location.x, -location.y);
	}	//	paint

	/**
	 * 	String Representation
	 * 	@return info
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder("HTMLElement[");
		sb.append("Bounds=").append(getBounds())
			.append(",Height=").append(p_height).append("(").append(p_maxHeight)
			.append("),Width=").append(p_width).append("(").append(p_maxHeight)
			.append("),PageLocation=").append(p_pageLocation).append(" - ");
		sb.append("]");
		return sb.toString();
	}	//	toString

	/**
	 * 	Is content HTML
	 *	@param content content
	 *	@return true if HTML
	 */
	public static boolean isHTML (Object content)
	{
		if (content == null)
			return false;
		// code borrowed from https://denofdevelopers.com/how-to-detect-if-string-is-html-or-not-in-android/
	    final String TAG_START = "<\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)>";
	    final String TAG_END = "</\\w+>";
	    final String TAG_SELF_CLOSING = "<\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)/>";
	    final String HTML_ENTITY = "&[a-zA-Z][a-zA-Z0-9]+;";
	    final Pattern htmlPattern = Pattern
	                        .compile("(" + TAG_START + ".*" + TAG_END + ")|(" + TAG_SELF_CLOSING + ")|(" + HTML_ENTITY + ")", Pattern.DOTALL);
        boolean isHTML = false;
		String htmlString = content.toString();
        if (htmlString != null) {
            isHTML = htmlPattern.matcher(htmlString).find();
        }
        return isHTML;
	}	//	isHTML

}	//	HTMLElement
