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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * 	Product Download Model
 *  @author Jorg Janke
 *  @version $Id: MProductDownload.java,v 1.2 2006/07/30 00:51:03 jjanke Exp $
 *	@author	Michael Judd BF [ 2736995 ] - toURL() in java.io.File has been deprecated
 */
public class MProductDownload extends X_M_ProductDownload implements ImmutablePOSupport
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = 6930118119436114158L;

	/**
	 * 	Migrate Download URLs (2.5.2c)
	 *	@param ctx context
	 *  @deprecated
	 */
	@Deprecated
	public static void migrateDownloads (Properties ctx)
	{
		String sql = "SELECT COUNT(*) FROM M_ProductDownload";
		int no = DB.getSQLValue(null, sql);
		if (no > 0)
			return;
		//
		int count = 0;
		sql = "SELECT AD_Client_ID, AD_Org_ID, M_Product_ID, Name, DownloadURL "
			+ "FROM M_Product "
			+ "WHERE DownloadURL IS NOT NULL";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				int AD_Client_ID = rs.getInt(1);
				int AD_Org_ID = rs.getInt(2);
				int M_Product_ID = rs.getInt(3);
				String Name = rs.getString(4);
				String DownloadURL = rs.getString(5);
				//
				MProductDownload pdl = new MProductDownload(ctx, 0, null);
				pdl.setClientOrg(AD_Client_ID, AD_Org_ID);
				pdl.setM_Product_ID(M_Product_ID);
				pdl.setName(Name);
				pdl.setDownloadURL(DownloadURL);
				if (pdl.save())
				{
					count++;
					String sqlUpdate = "UPDATE M_Product SET DownloadURL = NULL WHERE M_Product_ID=" + M_Product_ID;
					int updated = DB.executeUpdate(sqlUpdate, null);
					if (updated != 1)
						s_log.warning("Product not updated");
				}
				else
					s_log.warning("Product Download not created M_Product_ID=" + M_Product_ID);
			}
		}
		catch (Exception e)
		{
			s_log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		if (s_log.isLoggable(Level.INFO)) s_log.info("#" + count);
	}	//	migrateDownloads
	
	/**	Logger	*/
	private static CLogger	s_log	= CLogger.getCLogger (MProductDownload.class);
	
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param M_ProductDownload_UU  UUID key
     * @param trxName Transaction
     */
    public MProductDownload(Properties ctx, String M_ProductDownload_UU, String trxName) {
        super(ctx, M_ProductDownload_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_ProductDownload_ID id
	 *	@param trxName trx
	 */
	public MProductDownload (Properties ctx, int M_ProductDownload_ID, String trxName)
	{
		super (ctx, M_ProductDownload_ID, trxName);
	}	//	MProductDownload

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName trx
	 */
	public MProductDownload (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MProductDownload
	
	/**
	 * Copy constructor
	 * @param copy
	 */
	public MProductDownload(MProductDownload copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MProductDownload(Properties ctx, MProductDownload copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MProductDownload(Properties ctx, MProductDownload copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MProductDownload[")
			.append(get_ID())
			.append(",M_Product_ID=").append(getM_Product_ID())
			.append(",").append(getDownloadURL())
			.append ("]");
		return sb.toString ();
	}	//	toString
		
	/**
	 * 	Get Download Name
	 *	@return download name (last part of name)
	 */
	public String getDownloadName()
	{
		String url = getDownloadURL();
		if (url == null || !isActive())
			return null;
		int pos = Math.max(url.lastIndexOf('/'), url.lastIndexOf('\\'));
		if (pos != -1)
			return url.substring(pos+1);
		return url;
	}	//	getDownloadName

	/**
	 * 	Get Download URL
	 * 	@param directory optional directory
	 *	@return url
	 */
	public URI getDownloadURL (String directory)
	{
		String dl_url = getDownloadURL();
		if (dl_url == null || !isActive())
			return null;

		URI url = null;
		try
		{
			if (dl_url.indexOf ("://") != -1)
				url = new URI (dl_url);
			else
			{
				File f = getDownloadFile (directory);
				if (f != null)
					url = f.toURI ();
			}
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, dl_url, ex);
			return null;
		}
		return url;
	}	//	getDownloadURL
	
	/**
	 * 	Find download url
	 *	@param directory optional directory
	 *	@return file or null
	 */
	public File getDownloadFile (String directory)
	{
		File file = new File (getDownloadURL());	//	absolute file
		if (file.exists())
			return file;
		if (directory == null || directory.length() == 0)
		{
			log.log(Level.SEVERE, "Not found " + getDownloadURL());
			return null;
		}
		String downloadURL2 = directory;
		if (!downloadURL2.endsWith(File.separator))
			downloadURL2 += File.separator;
		downloadURL2 += getDownloadURL();
		file = new File (downloadURL2);
		if (file.exists())
			return file;

		log.log(Level.SEVERE, "Not found " + getDownloadURL() + " + " + downloadURL2);
		return null;
	}	//	getDownloadFile

	/**
	 * 	Get Download Stream
	 * 	@param directory optional directory
	 *	@return input stream or null
	 */
	public InputStream getDownloadStream (String directory)
	{
		String dl_url = getDownloadURL();
		if (dl_url == null || !isActive())
			return null;

		InputStream in = null;
		try
		{
			if (dl_url.indexOf ("://") != -1)
			{
				URI url = new URI (dl_url);
				in = url.toURL().openStream();
			}
			else //	file
			{
				File file = getDownloadFile(directory);
				if (file == null)
					return null;
				in = new FileInputStream (file);
			}
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, dl_url, ex);
			return null;
		}
		return in;
	}	//	getDownloadStream

	@Override
	public MProductDownload markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		return this;
	}

}	//	MProductDownload
