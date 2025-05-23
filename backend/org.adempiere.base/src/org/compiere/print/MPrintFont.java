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
package org.compiere.print;

import java.awt.Font;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.X_AD_PrintFont;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Print Font Model for AD_PrintFont
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: MPrintFont.java,v 1.3 2006/07/30 00:53:02 jjanke Exp $
 */
public class MPrintFont extends X_AD_PrintFont implements ImmutablePOSupport
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -613305916546183810L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_PrintFont_UU  UUID key
     * @param trxName Transaction
     */
    public MPrintFont(Properties ctx, String AD_PrintFont_UU, String trxName) {
        super(ctx, AD_PrintFont_UU, trxName);
		if (Util.isEmpty(AD_PrintFont_UU))
			setInitialDefaults();
    }

	/**
	 *	Constructor
	 *  @param ctx context
	 *  @param AD_PrintFont_ID ID
	 *  @param trxName transaction
	 */
	public MPrintFont(Properties ctx, int AD_PrintFont_ID, String trxName)
	{
		super (ctx, AD_PrintFont_ID, trxName);
		if (AD_PrintFont_ID == 0)
			setInitialDefaults();
	}	//	MPrintFont
	
	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setIsDefault(false);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MPrintFont(Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}

	/**
	 * Copy constructor 
	 * @param copy
	 */
	public MPrintFont(MPrintFont copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MPrintFont(Properties ctx, MPrintFont copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MPrintFont(Properties ctx, MPrintFont copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
		this.m_cacheFont = copy.m_cacheFont;
	}
	
	/** Font cached					*/
	private Font 	m_cacheFont = null;

	/**
	 * 	Get Font
	 * 	@return Font
	 */
	public Font getFont()
	{
		if (m_cacheFont != null)
			return m_cacheFont;
		String code = getCode();
		if (code == null || code.equals("."))
			m_cacheFont = new Font (null);
		try
		{
			if (code != null && !code.equals("."))
			//	fontfamilyname-style-pointsize
				m_cacheFont = Font.decode(code);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "MPrintFont.getFont", e);
		}
		if (code == null)
			m_cacheFont = new Font (null);		//	family=dialog,name=Dialog,style=plain,size=12
		return m_cacheFont;
	}	//	getFont

	/**
	 * 	Set Font
	 * 	@param font Font
	 */
	public void setFont (Font font)
	{
		//	fontfamilyname-style-pointsize
		StringBuilder sb = new StringBuilder();
		sb.append(font.getFamily()).append("-");
		int style = font.getStyle();
		if (style == Font.PLAIN)
			sb.append("PLAIN");
		else if (style == Font.BOLD)
			sb.append("BOLD");
		else if (style == Font.ITALIC)
			sb.append("ITALIC");
		else if (style == (Font.BOLD + Font.ITALIC))
			sb.append("BOLDITALIC");
		sb.append("-").append(font.getSize());
		setCode(sb.toString());
	}	//	setFont

	/**
	 * 	Create Font in Database and save
	 * 	@param font font
	 * 	@return PrintFont
	 */
	static MPrintFont create (Font font)
	{
		MPrintFont pf = new MPrintFont(Env.getCtx(), 0, null);
		StringBuilder name = new StringBuilder (font.getName());
		if (font.isBold())
			name.append(" bold");
		if (font.isItalic())
			name.append(" italic");
		name.append(" ").append(font.getSize());
		pf.setName(name.toString());
		pf.setFont(font);
		pf.saveEx();
		return pf;
	}	//	create

	/**
	 * 	String Representation
	 * 	@return info
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("MPrintFont[");
		sb.append("ID=").append(get_ID())
			.append(",Name=").append(getName())
			.append("PSName=").append(getFont().getPSName())
			.append(getFont())
			.append("]");
		return sb.toString();
	}	//	toString

	/**
	 * 	Get PostScript Level 2 definition.
	 *  e.g. /dialog 12 selectfont
	 * 	@return PostScript command
	 */
	public String toPS()
	{
		StringBuilder sb = new StringBuilder("/");
		sb.append(getFont().getPSName());
		if (getFont().isBold())
			sb.append(" Bold");
		if (getFont().isItalic())
			sb.append(" Italic");
		sb.append(" ").append(getFont().getSize())
			.append(" selectfont");
		return sb.toString();
	}	//	toPS

	/**
	 * 	Dump Font
	 * 	@param font font
	 */
	static void dump (Font font)
	{
		System.out.println("Family=" + font.getFamily());
		System.out.println("FontName=" + font.getFontName());
		System.out.println("Name=" + font.getName());
		System.out.println("PSName=" + font.getPSName());
		System.out.println("Style=" + font.getStyle());
		System.out.println("Size=" + font.getSize());
		System.out.println("Attributes:");
		Map<?,?> map = font.getAttributes();
		Iterator<?> keys = map.keySet().iterator();
		while (keys.hasNext())
		{
			Object key = keys.next();
			Object value = map.get(key);
			System.out.println(" - " + key + "=" + value);
		}
		System.out.println(font);
	}	//	dump

	/** Cached Fonts						*/
	static private ImmutableIntPOCache<Integer,MPrintFont> s_fonts = new ImmutableIntPOCache<Integer,MPrintFont>(Table_Name, 20);

	/**
	 * 	Get Font
	 * 	@param AD_PrintFont_ID id
	 * 	@return Font
	 */
	static public MPrintFont get (int AD_PrintFont_ID)
	{
		Integer key = Integer.valueOf(AD_PrintFont_ID);
		MPrintFont pf = s_fonts.get(key);
		if (pf == null)
		{
			pf = new MPrintFont (Env.getCtx(), AD_PrintFont_ID, (String)null);
			if (pf.get_ID() == AD_PrintFont_ID)
			{
				s_fonts.put(key, pf);
				return pf;
			}
			return null;
		}
		return pf;
	}	//	get

	@Override
	public MPrintFont markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MPrintFont
