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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.net.ssl.HttpsURLConnection;

import org.adempiere.base.Core;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 *  Abstract Payment Processor base class 
 *
 *  @author Jorg Janke
 *  @version $Id: PaymentProcessor.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public abstract class PaymentProcessor
{
	/**
	 *  Public Constructor
	 */
	public PaymentProcessor()
	{
	}   //  PaymentProcessor

	/**	Logger							*/
	protected CLogger			log = CLogger.getCLogger (getClass());
	/** Payment Processor Logger		*/
	@SuppressWarnings("unused")
	static private CLogger		s_log = CLogger.getCLogger (PaymentProcessor.class);
	/** Encoding (ISO-8859-1 - UTF-8) 		*/
	public static final String	ENCODING = "UTF-8";
	/** Encode Parameters		*/
	private boolean 			m_encoded = false;
	/** Ampersand				*/
	public static final char	AMP = '&';
	/** Equals					*/
	public static final char	EQ = '=';

	/**
	 * @param mbap
	 * @param mp
	 */
	public void initialize(MBankAccountProcessor mbap, PaymentInterface mp)
	{
		p_mbap = mbap;
		p_mp = mp;
	}

	/**
	 *  Static Factory method
	 *  @param mbap
	 *  @param mp
	 *  @return PaymentProcessor instance or null
	 */
	public static PaymentProcessor create (MBankAccountProcessor mbap, PaymentInterface mp)
	{
		return Core.getPaymentProcessor(mbap, mp);
	}   //  create

	protected MBankAccountProcessor p_mbap = null;
	protected PaymentInterface		p_mp = null;
	//
	private int     m_timeout = 30;

	/**
	 *  Process CreditCard (no date check)
	 *  @return true if processed successfully
	 *  @throws IllegalArgumentException
	 */
	public abstract boolean processCC () throws IllegalArgumentException;

	/**
	 *  Is payment processed successfully
	 *  @return true if OK
	 */
	public abstract boolean isProcessedOK();

	// Validation methods. Override if you have specific needs.

	/**
	 * Validate payment before process.
	 *  @return  "" or Error AD_Message.
	 *  @throws IllegalArgumentException
	 */
	public String validate() throws IllegalArgumentException {
		String msg = null;
		if (MPayment.TENDERTYPE_CreditCard.equals(p_mp.getTenderType())) {
			msg = validateCreditCard();
		} else if (MPayment.TENDERTYPE_Check.equals(p_mp.getTenderType())) {
			msg = validateCheckNo();
		} else if (MPayment.TENDERTYPE_Account.equals(p_mp.getTenderType())) {
			msg = validateAccountNo();
		}
		return(msg);
	}

	/**
	 * Standard account validation.
	 * @return "" or Error AD_Message.
	 */
	public String validateAccountNo() {
		return MPaymentValidate.validateAccountNo(p_mp.getAccountNo());
	}

	/**
	 * Validate check no
	 * @return "" or Error AD_Message.
	 */
	public String validateCheckNo() {
		return MPaymentValidate.validateCheckNo(p_mp.getCheckNo());
	}

	/**
	 * Validate credit card
	 * @return "" or Error AD_Message.
	 * @throws IllegalArgumentException
	 */
	public String validateCreditCard() throws IllegalArgumentException {
		String msg = null;
		if (p_mp.getC_BP_BankAccount_ID() != 0 || (p_mp.getCustomerPaymentProfileID() != null && p_mp.getCustomerPaymentProfileID().length() > 0))
			return msg;
		msg = MPaymentValidate.validateCreditCardNumber(p_mp.getCreditCardNumber(), p_mp.getCreditCardType());
		if (msg != null && msg.length() > 0)
			throw new IllegalArgumentException(Msg.getMsg(Env.getCtx(), msg));
		msg = MPaymentValidate.validateCreditCardExp(p_mp.getCreditCardExpMM(), p_mp.getCreditCardExpYY());
		if (msg != null && msg.length() > 0)
			throw new IllegalArgumentException(Msg.getMsg(Env.getCtx(), msg));
		if (p_mp.getCreditCardVV() != null && p_mp.getCreditCardVV().length() > 0)
		{
			msg = MPaymentValidate.validateCreditCardVV(p_mp.getCreditCardVV(), p_mp.getCreditCardType());
			if (msg != null && msg.length() > 0)
				throw new IllegalArgumentException(Msg.getMsg(Env.getCtx(), msg));
		}
		return(msg);
	}

	/**
	 * 	Set Timeout
	 * 	@param newTimeout timeout
	 */
	public void setTimeout(int newTimeout)
	{
		m_timeout = newTimeout;
	}
	/**
	 * 	Get Timeout
	 *	@return timeout
	 */
	public int getTimeout()
	{
		return m_timeout;
	}

	/**
	 *  Check for delimiter fields &amp;= and add length of not encoded
	 *  @param name name
	 *  @param value value
	 *  @param maxLength maximum length
	 *  @return name[5]=value or name=value
	 */
	protected String createPair(String name, BigDecimal value, int maxLength)
	{
		if (value == null)
			return createPair (name, "0", maxLength);
		else
		{
			if (value.scale() < 2)
				value = value.setScale(2, RoundingMode.HALF_UP);
			return createPair (name, String.valueOf(value), maxLength);
		}
	}	//	createPair

	/**
	 *  Check for delimiter fields &amp;= and add length of not encoded
	 *  @param name name
	 *  @param value value
	 *  @param maxLength maximum length
	 *  @return name[5]=value or name=value
	 */
	protected String createPair(String name, int value, int maxLength)
	{
		if (value == 0)
			return "";
		else
			return createPair (name, String.valueOf(value), maxLength);
	}	//	createPair

	/**
	 *  Check for delimiter fields &amp;= and add length of not encoded
	 *  @param name name
	 *  @param value value
	 *  @param maxLength maximum length
	 *  @return name[5]=value or name=value
	 */
	protected String createPair(String name, String value, int maxLength)
	{
		//  Nothing to say
		if (name == null || name.length() == 0
			|| value == null || value.length() == 0)
			return "";

		if (value.length() > maxLength)
			value = value.substring(0, maxLength);

		StringBuilder retValue = new StringBuilder(name);
		if (m_encoded)
			try
			{
				value = URLEncoder.encode(value, ENCODING);
			}
			catch (UnsupportedEncodingException e)
			{
				log.log(Level.SEVERE, value + " - " + e.toString());
			}
		else if (value.indexOf(AMP) != -1 || value.indexOf(EQ) != -1)
			retValue.append("[").append(value.length()).append("]");
		//
		retValue.append(EQ);
		retValue.append(value);
		return retValue.toString();
	}   // createPair

	/**
	 * 	Set Encoded
	 *	@param doEncode true if encode
	 */
	public void setEncoded (boolean doEncode)
	{
		m_encoded = doEncode;
	}	//	setEncode
	
	/**
	 * 	Is Encoded
	 *	@return true if encoded
	 */
	public boolean isEncoded()
	{
		return m_encoded;
	}	//	setEncode

	/**
	 * 	Get Properties from URL
	 *	@param urlString POST url string
	 *	@param parameter parameter
	 *	@return result as properties
	 */
	protected Properties getConnectPostProperties (String urlString, String parameter)
	{
		long start = System.currentTimeMillis();
		String result = connectPost(urlString, parameter);
		if (result == null)
			return null;
		Properties prop = new Properties();
		try
		{
			String info = URLDecoder.decode(result, ENCODING);
			StringTokenizer st = new StringTokenizer(info, "&");	//	AMP
			while (st.hasMoreTokens())
			{
				String token = st.nextToken();
				int index = token.indexOf('=');
				if (index == -1)
					prop.put(token, "");
				else
				{
					String key = token.substring(0, index);
					String value = token.substring(index+1);
					prop.put(key, value);
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, result, e);
		}
		long ms = System.currentTimeMillis() - start;
		if (log.isLoggable(Level.FINE)) log.fine(ms + "ms - " + prop.toString());
		return prop;
	}	//	getConnectPostProperties

	/**
	 * 	Connect via Post
	 *	@param urlString url destination (assuming https)
	 *	@param parameter parameter
	 *	@return response or null if failure
	 */
	protected String connectPost (String urlString, String parameter)
	{
		String response = null;
		try
		{
			// open secure connection
			URL url = new URL(urlString);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			if (log.isLoggable(Level.FINE)) log.fine(connection.getURL().toString());

			// POST the parameter
			DataOutputStream out = new DataOutputStream (connection.getOutputStream());
			out.write(parameter.getBytes());
			out.flush();
			out.close();

			// process and read the gateway response
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			response = in.readLine();
			in.close();	                     // no more data
			log.finest(response);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, urlString, e);
		}
		//
	    return response;
	}	//	connectPost

}   //  PaymentProcessor
