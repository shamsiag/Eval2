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
package org.compiere.impexp;

import java.io.FileInputStream;
import java.io.IOException;

import org.compiere.model.MBankStatementLoader;
import org.xml.sax.SAXException;

/**
 *	Loader for OFX bank statements (file based)
 *
 *  @author Eldir Tomassen
 *  @version $Id:
 */
public final class OFXFileBankStatementLoader extends OFXBankStatementHandler implements BankStatementLoaderInterface
{

	/**
	 * Method init
	 * @param controller MBankStatementLoader
	 * @return boolean
	 * @see org.compiere.impexp.BankStatementLoaderInterface#init(MBankStatementLoader)
	 */
	@Override
	public boolean init(MBankStatementLoader controller)
	{
		boolean result = false;
		FileInputStream m_stream = null;
		try
		{
			//	Try to open the file specified as a process parameter
			if (controller.getLocalFileName() != null)
			{
				m_stream = new FileInputStream(controller.getLocalFileName());
			}
			//	Try to open the file specified as part of the loader configuration
			else if (controller.getFileName() != null)
			{
				m_stream = new FileInputStream(controller.getFileName());
			}
			else 
			{
				return result;
			}
			if (!super.init(controller))
			{
				return result;
			}
			//m_stream is not closed because the BufferedReader in the parent class is closed and according to the Java docs: 
			//Calling close() on the BufferedReader closes the underlying stream by default implementation
			result = attachInput(m_stream);
		}
		catch(Exception e)
		{
			m_errorMessage = new StringBuffer("ErrorReadingData");
			m_errorDescription = new StringBuffer();
		}
		finally
		{
			if (m_stream != null)
				try {
					m_stream.close();
				} catch (IOException e) {}
		}

		return result;
	}	//	init
	
	/**
	 * @param ch char[]
	 * @param start int
	 * @param length int
	 * @throws SAXException
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	@Override
	public void characters (char ch[], int start, int length)
		throws SAXException
	{
		/*
		 * There are no additional things to do when importing from file.
		 * All data is handled by OFXBankStatementHandler
		 */
		super.characters(ch, start, length);
	}	//	characterS
	
}	//	OFXFileBankStatementLoader
