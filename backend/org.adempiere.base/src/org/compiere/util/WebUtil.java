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
package org.compiere.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ecs.AlignType;
import org.apache.ecs.xhtml.a;
import org.apache.ecs.xhtml.body;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.label;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.script;
import org.apache.ecs.xhtml.td;
import org.apache.ecs.xhtml.tr;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;

/**
 *  Servlet Utilities
 *
 *  @author Jorg Janke
 *  @version  $Id: WebUtil.java,v 1.7 2006/09/24 12:11:54 comdivision Exp $
 */
public final class WebUtil
{
	/**	Static Logger	*/
	private static CLogger		log	= CLogger.getCLogger (WebUtil.class);
	
	/**
	 *  Create Exit Page "Log-off".
	 *  <p>
	 *  - End Session
	 *  - Go to start page (e.g. /adempiere/index.html)
	 *
	 *  @param request request
	 *  @param response response
	 *  @param servlet servlet
	 *  @param ctx context
	 *  @param AD_Message messahe
	 *  @throws ServletException
	 *  @throws IOException
	 *  @deprecated
	 */
	@Deprecated
	public static void createLoginPage (HttpServletRequest request, HttpServletResponse response,
		HttpServlet servlet, Properties ctx, String AD_Message) throws ServletException, IOException
	{
		request.getSession().invalidate();
		String url = WebEnv.getBaseDirectory("index.html");
		//
		WebDoc doc = null;
		if (ctx != null && AD_Message != null && !AD_Message.equals(""))
			doc = WebDoc.create (Msg.getMsg(ctx, AD_Message));
		else if (AD_Message != null)
			doc = WebDoc.create (AD_Message);
		else
			doc = WebDoc.create (false);
		script script = new script("window.top.location.replace('" + url + "');");
		doc.getBody().addElement(script);
		//
		createResponse (request, response, servlet, null, doc, false);
	}   //  createLoginPage

	/**
	 *  Create Login Button - replace Window
	 *
	 *  @param ctx context
	 *  @return Button
	 *  @deprecated
	 */
	@Deprecated
	public static input getLoginButton (Properties ctx)
	{
		String text = "Login";
		if (ctx != null)
			text = Msg.getMsg (ctx, "Login");
		
		input button = new input("button", text, "  "+text);		
		button.setID(text);
		button.setClass("loginbtn");		
		StringBuilder cmd = new StringBuilder ("window.top.location.replace('");
		cmd.append(WebEnv.getBaseDirectory("index.html"));
		cmd.append("');");
		button.setOnClick(cmd.toString());
		return button;
	}   //  getLoginButton
	
	/**
	 *  Get Cookie Properties
	 *
	 *  @param request request
	 *  @return Properties
	 *  @deprecated
	 */
	@Deprecated
	public static Properties getCookieProprties(HttpServletRequest request)
	{
		//  Get Properties
		Cookie[] cookies = request.getCookies();
		if (cookies != null)
		{
			for (int i = 0; i < cookies.length; i++)
			{
				if (cookies[i].getName().equals(WebEnv.COOKIE_INFO))
					return propertiesDecode(cookies[i].getValue());
			}
		}
		return new Properties();
	}   //  getProperties

	
	/**
	 *  Get String Parameter.
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return string or null
	 */
	public static String getParameter (HttpServletRequest request, String parameter)
	{
		if (request == null || parameter == null)
			return null;
		String enc = request.getCharacterEncoding();
		try
		{
			if (enc == null)
			{
				request.setCharacterEncoding(WebEnv.ENCODING);
				enc = request.getCharacterEncoding();
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Set CharacterEncoding=" + WebEnv.ENCODING, e);
			enc = request.getCharacterEncoding();
		}
		String data = request.getParameter(parameter);
		if (data == null || data.length() == 0)
			return data;
		
		//	Convert
		if (enc != null && !WebEnv.ENCODING.equals(enc))
		{
			try
			{
				String dataEnc = new String(data.getBytes(enc), WebEnv.ENCODING);
				if (log.isLoggable(Level.FINER))log.log(Level.FINER, "Convert " + data + " (" + enc + ")-> " 
						+ dataEnc + " (" + WebEnv.ENCODING + ")");
				data = dataEnc;
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Convert " + data + " (" + enc + ")->" + WebEnv.ENCODING);
			}
		}
		
		//	Convert &#000; to character (JSTL input)
		String inStr = data;
		StringBuilder outStr = new StringBuilder();
		int i = inStr.indexOf("&#");
		while (i != -1)
		{
			outStr.append(inStr.substring(0, i));			// up to &#
			inStr = inStr.substring(i+2, inStr.length());	// from &#

			int j = inStr.indexOf(';');						// next ;
			if (j < 0)										// no second tag
			{
				inStr = "&#" + inStr;
				break;
			}

			String token = inStr.substring(0, j);
			try
			{
				int intToken = Integer.parseInt(token);
				outStr.append((char)intToken);				// replace context
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Token=" + token, e);
				outStr.append("&#").append(token).append(";");
			}
			inStr = inStr.substring(j+1, inStr.length());	// from ;
			i = inStr.indexOf("&#");
		}

		outStr.append(inStr);           					//	add remainder
		String retValue = outStr.toString();
		if (log.isLoggable(Level.FINEST)) log.finest(parameter + "=" + data + " -> " + retValue);
		return retValue;
	}   //  getParameter

	/**
	 *  Get integer Parameter - 0 if not defined.
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return int result or 0
	 */
	public static int getParameterAsInt (HttpServletRequest request, String parameter)
	{
		if (request == null || parameter == null)
			return 0;
		String data = getParameter(request, parameter);
		if (data == null || data.length() == 0)
			return 0;
		try
		{
			return Integer.parseInt(data);
		}
		catch (Exception e)
		{
			log.warning (parameter + "=" + data + " - " + e);
		}
		return 0;
	}   //  getParameterAsInt

	/**
	 *  Get numeric Parameter - 0 if not defined
	 *
	 *  @param request request
	 *  @param parameter parameter
	 *  @return big decimal result or 0
	 */
	public static BigDecimal getParameterAsBD (HttpServletRequest request, String parameter)
	{
		if (request == null || parameter == null)
			return Env.ZERO;
		String data = getParameter(request, parameter);
		if (data == null || data.length() == 0)
			return Env.ZERO;
		try
		{
			return new BigDecimal (data);
		}
		catch (Exception e)
		{
		}
		try
		{
			DecimalFormat format = DisplayType.getNumberFormat(DisplayType.Number);
			Object oo = format.parseObject(data);
			if (oo instanceof BigDecimal)
				return (BigDecimal)oo;
			else if (oo instanceof Number)
				return BigDecimal.valueOf(((Number)oo).doubleValue());
			return new BigDecimal (oo.toString());
		}
		catch (Exception e)
		{
			if (log.isLoggable(Level.FINE)) log.fine(parameter + "=" + data + " - " + e);
		}
		return Env.ZERO;
	}   //  getParameterAsBD

	/**
	 *  Get date Parameter - null if not defined.
	 *	Date portion only
	 *  @param request request
	 *  @param parameter parameter
	 *  @return timestamp result or null
	 */
	public static Timestamp getParameterAsDate (HttpServletRequest request, 
		String parameter)
	{
		return getParameterAsDate (request, parameter, null);
	}	//	getParameterAsDate
	
	/**
	 *  Get date Parameter - null if not defined.
	 *	Date portion only
	 *  @param request request
	 *  @param parameter parameter
	 *  @param language optional language
	 *  @return timestamp result or null
	 */
	public static Timestamp getParameterAsDate (HttpServletRequest request, 
		String parameter, Language language)
	{
		if (request == null || parameter == null)
			return null;
		String data = getParameter(request, parameter);
		if (data == null || data.length() == 0)
			return null;
		
		//	Language Date Format
		if (language != null)
		{
			try
			{
				DateFormat format = DisplayType.getDateFormat(DisplayType.Date, language);
				java.util.Date date = format.parse(data);
				if (date != null)
					return new Timestamp (date.getTime());
			}
			catch (Exception e)
			{
			}
		}
		
		//	Default Simple Date Format
		try
		{
			SimpleDateFormat format = DisplayType.getDateFormat(DisplayType.Date);
			java.util.Date date = format.parse(data);
			if (date != null)
				return new Timestamp (date.getTime());
		}
		catch (Exception e)
		{
		}
		
		//	JDBC Format
		try 
		{
			return Timestamp.valueOf(data);
		}
		catch (Exception e) 
		{
		}
		
		log.warning(parameter + " - cannot parse: " + data);
		return null;
	}   //  getParameterAsDate

	/**
	 *  Get boolean Parameter.
	 *  @param request request
	 *  @param parameter parameter
	 *  @return true if found
	 */
	public static boolean getParameterAsBoolean (HttpServletRequest request, 
		String parameter)
	{
		return getParameterAsBoolean(request, parameter, null);
	}	//	getParameterAsBoolean
	
	/**
	 *  Get boolean Parameter.
	 *  @param request request
	 *  @param parameter parameter
	 *  @param expected optional expected value
	 *  @return true if found and if optional value matches
	 */
	public static boolean getParameterAsBoolean (HttpServletRequest request, 
		String parameter, String expected)
	{
		if (request == null || parameter == null)
			return false;
		String data = getParameter(request, parameter);
		if (data == null || data.length() == 0)
			return false;
		//	Ignore actual value
		if (expected == null)
			return true;
		//
		return expected.equalsIgnoreCase(data);
	}   //  getParameterAsBoolean
	
    /**
     * 	Get Parameter or Null if empty
     *	@param request request
     *	@param parameter parameter
     *	@return Request Value or null
     */
    public static String getParamOrNull (HttpServletRequest request, String parameter)
    {
        String value = WebUtil.getParameter(request, parameter);
        if(value == null) 
        	return value;
        if (value.length() == 0) 
        	return null;
        return value;
    }	//	getParamOrNull
    	
	/**
	 *  Create Standard Response Header with optional Cookie and print document.
	 *  D:\j2sdk1.4.0\docs\guide\intl\encoding.doc.html
	 *
	 *  @param request request
	 *  @param response response
	 *  @param servlet servlet
	 *  @param cookieProperties cookie properties
	 *  @param doc doc
	 *  @param debug debug
	 *  @throws IOException
	 */
	public static void createResponse (HttpServletRequest request, HttpServletResponse response,
		HttpServlet servlet, Properties cookieProperties, WebDoc doc, boolean debug) throws IOException
	{
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("text/html; charset=UTF-8");
		//
		//  Update Cookie - overwrite
		if (cookieProperties != null)
		{
			Cookie cookie = new Cookie (WebEnv.COOKIE_INFO, propertiesEncode(cookieProperties));
			cookie.setComment("(c) iDempiere, Inc - Jorg Janke");
			cookie.setSecure(true);
			cookie.setPath("/");
			if (cookieProperties.size() == 0)
				cookie.setMaxAge(0);            //  delete cookie
			else
				cookie.setMaxAge(2592000);      //  30 days in seconds   60*60*24*30
			response.addCookie(cookie);
		}

		//  print document
		PrintWriter out = response.getWriter();     //  with character encoding support
		doc.output(out);
		out.flush();
		if (out.checkError())
			log.log(Level.SEVERE, "error writing");
		out.close();
	}   //  createResponse

	/**
	 *  Create Java Script to clear Target frame
	 *
	 *  @param targetFrame target frame
	 *  @return Clear Frame Script
	 *  @deprecated
	 */
	@Deprecated
	public static script getClearFrame (String targetFrame)
	{
		StringBuilder cmd = new StringBuilder();
		cmd.append("// <!-- clear frame\n")
			.append("var d = parent.").append(targetFrame).append(".document;\n")
			.append("d.open();\n")
			.append("d.write('<link href=\"").append(WebEnv.getStylesheetURL()).append("\" type=\"text/css\" rel=\"stylesheet\">');\n")
			.append("d.write('<link href=\"/adempiere/css/window.css\" type=\"text/css\" rel=\"stylesheet\">');\n")
			.append("d.write('<br><br><br><br><br><br><br>');")
			.append("d.write('<div style=\"text-align: center;\"><img class=\"CenterImage\" style=\"vertical-align: middle; filter:alpha(opacity=50); -moz-opacity:0.5;\" src=\"Logo.gif\" /></div>');\n")
			.append("d.close();\n")
			.append("// -- clear frame -->");
		//
		return new script(cmd.toString());
	}   //  getClearFrame

	/**
	 * 	Return a link and script with new location.
	 * 	@param url forward url
	 * 	@param delaySec delay in seconds (default 3)
	 * 	@return html
	 *  @deprecated
	 */
	@Deprecated
	public static HtmlCode getForward (String url, int delaySec)
	{
		if (delaySec <= 0)
			delaySec = 3;
		HtmlCode retValue = new HtmlCode();
		//	Link
		a a = new a(url);
		a.addElement(url);
		retValue.addElement(a);
		//	Java Script	- document.location - 
		script script = new script("setTimeout(\"window.top.location.replace('" + url 
			+ "')\"," + (delaySec+1000) + ");");
		retValue.addElement(script);
		//
		return retValue;
	}	//	getForward

	/**
	 * 	Create Forward Page
	 * 	@param response response
	 * 	@param title page title
	 * 	@param forwardURL url
	 * 	@param delaySec delay in seconds (default 3)
	 * 	@throws ServletException
	 * 	@throws IOException
	 *  @deprecated
	 */
	@Deprecated
	public static void createForwardPage (HttpServletResponse response,
		String title, String forwardURL, int delaySec) throws ServletException, IOException
	{
		response.setContentType("text/html; charset=UTF-8");
		WebDoc doc = WebDoc.create(title);
		body b = doc.getBody();
		b.addElement(getForward(forwardURL, delaySec));
		PrintWriter out = response.getWriter();
		doc.output(out);
		out.flush();
		if (out.checkError())
			log.log(Level.SEVERE, "Error writing");
		out.close();
		if (log.isLoggable(Level.FINE)) log.fine(forwardURL + " - " + title);
	}	//	createForwardPage

	/**
	 * 	Does Test exist
	 *	@param test string
	 *	@return true if String with data
	 */
	public static boolean exists (String test)
	{
		if (test == null)
			return false;
		return test.length() > 0;
	}	//	exists

	/**
	 * 	Does Parameter exist
	 * 	@param request request
	 *	@param parameter string
	 *	@return true if String with data
	 */
	public static boolean exists (HttpServletRequest request, String parameter)
	{
		if (request == null || parameter == null)
			return false;
		try
		{
			String enc = request.getCharacterEncoding();
			if (enc == null)
				request.setCharacterEncoding(WebEnv.ENCODING);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Set CharacterEncoding=" + WebEnv.ENCODING, e);
		}
		return exists (request.getParameter(parameter));
	}	//	exists

	/**
	 *	Is EMail address valid
	 * 	@param email mail address
	 * 	@return true if valid
	 */
	public static boolean isEmailValid (String email)
	{
		if (email == null || email.length () == 0)
			return false;
		try
		{
			InternetAddress ia = new InternetAddress (email, true);
			if (ia != null)
				return true;
		}
		catch (AddressException ex)
		{
			log.warning (email + " - "
				+ ex.getLocalizedMessage ());
		}
		return false;
	}	//	isEmailValid

	/**
	 *  Decode Properties into String (URL encoded)
	 *
	 *  @param pp properties
	 *  @return Encoded String
	 */
	public static String propertiesEncode (Properties pp)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try
		{
			pp.store(bos, "adempiere");   //  Header
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "store", e);
		}
		String result = new String (bos.toByteArray());
		try
		{
			result = URLEncoder.encode(result, WebEnv.ENCODING);
		}
		catch (UnsupportedEncodingException e)
		{
			log.log(Level.SEVERE, "encode" + WebEnv.ENCODING, e);
			String enc = System.getProperty("file.encoding");      //  Windows default is Cp1252
			try
			{
				result = URLEncoder.encode(result, enc);
				if (log.isLoggable(Level.INFO)) log.info("encode: " + enc);
			}
			catch (Exception ex)
			{
				log.log(Level.SEVERE, "encode", ex);
			}
		}
		return result;
	}   //  propertiesEncode

	/**
	 *  Decode data String (URL encoded) into Properties
	 *
	 *  @param data data
	 *  @return Properties
	 */
	public static Properties propertiesDecode (String data)
	{
		String result = null;
		try
		{
			result = URLDecoder.decode(data, WebEnv.ENCODING);
		}
		catch (UnsupportedEncodingException e)
		{
			log.log(Level.SEVERE, "decode" + WebEnv.ENCODING, e);
			String enc = System.getProperty("file.encoding");      //  Windows default is Cp1252
			try
			{
				result = URLEncoder.encode(data, enc);
				log.log(Level.SEVERE, "decode: " + enc);
			}
			catch (Exception ex)
			{
				log.log(Level.SEVERE, "decode", ex);
			}
		}

		ByteArrayInputStream bis = new ByteArrayInputStream(result.getBytes());
		Properties pp = new Properties();
		try
		{
			pp.load(bis);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "load", e);
		}
		return pp;
	}   //  propertiesDecode
	
	/**
	 *  Convert Array of NamePair to HTTP Option Array.
	 *  <p>
	 *  If the ArrayList does not contain NamePairs, the String value is used
	 *  @see org.compiere.util.NamePair
	 *  @param  list    ArrayList containing NamePair values
	 *  @param  default_ID  Sets the default if the key/ID value is found.
	 *      If the value is null or empty, the first value is selected
	 *  @return Option Array
	 */
	public static option[] convertToOption (NamePair[] list, String default_ID)
	{		
		int size = list.length;
		option[] retValue = new option[size];	
		for (int i = 0; i < size; i++)
		{			
			boolean selected = false;
			//  select first entry
			if (i == 0 && (default_ID == null || default_ID.length() == 0))
				selected = true;

			//  Create option
			String name = Util.maskHTML(list[i].getName());
			retValue[i] = new option(list[i].getID()).addElement(name);

			//  Select if ID/Key is same as default ID
			if (default_ID != null && default_ID.equals(list[i].getID()))
				selected = true;
			retValue[i].setSelected(selected);
		}		
		return retValue;
	}   //  convertToOption
	
	/**
	 *  Create label/field table row
	 *
	 *  @param line - null for new line (table row)
	 *  @param FORMNAME form name
	 *  @param PARAMETER parameter name
	 *  @param labelText label
	 *  @param inputType HTML input type
	 *  @param value data value
	 *  @param sizeDisplay display size
	 *  @param size data size
	 *  @param longField field spanning two columns
	 *  @param mandatory mark as mandatory
	 *  @param onChange onChange call
	 *  @param script script
	 *  @return tr table row
	 *  @deprecated
	 */
	@Deprecated
	static public tr createField (tr line, String FORMNAME, String PARAMETER,
		String labelText, String inputType, Object value,
		int sizeDisplay, int size, boolean longField, 
		boolean mandatory, String onChange, StringBuffer script)
	{
		if (line == null)
			line = new tr();
		String labelInfo = labelText;
		if (mandatory)
		{
			labelInfo += "&nbsp;<font color=\"red\">*</font>";
			String fName = "document." + FORMNAME + "." + PARAMETER;
			script.append(fName).append(".required=true; ");
		}

		label llabel = new label().setFor(PARAMETER).addElement(labelInfo);
		llabel.setID("ID_" + PARAMETER + "_Label");
	//	label.setTitle(description);
		line.addElement(new td().addElement(llabel).setAlign(AlignType.RIGHT));
		input iinput = new input(inputType, PARAMETER, value == null ? "" : value.toString());
		iinput.setSize(sizeDisplay).setMaxlength(size);
		iinput.setID("ID_" + PARAMETER);
		if (onChange != null && onChange.length() > 0)
			iinput.setOnChange(onChange);
		iinput.setTitle(labelText);
		td field = new td().addElement(iinput).setAlign(AlignType.LEFT);
		if (longField)
			field.setColSpan(3);
		line.addElement(field);
		return line;
	}   //  addField

	/**
	 * 	Get Close PopUp Buton
	 *	@return button
	 *  @deprecated
	 */
	@Deprecated
	public static input createClosePopupButton(Properties ctx)
	{
		String text = "Close";
		if (ctx != null)
			text = Msg.getMsg (ctx, "Close");
		
		input close = new input("button", text, "  "+text);		
		close.setID(text);
		close.setClass("closebtn");		
		close.setTitle ("Close PopUp");	//	Help
		//close.setOnClick ("closePopup();return false;");
		close.setOnClick ("self.close();return false;");
		return close;
	}	//	getClosePopupButton
	
	
	/**
	 * 	Stream Attachment Entry
	 *	@param response response
	 *	@param attachment attachment
	 *	@param attachmentIndex logical index
	 *	@return error message or null
	 */
	public static String streamAttachment (HttpServletResponse response, 
		MAttachment attachment, int attachmentIndex)
	{
		if (attachment == null)
			return "No Attachment";
		
		int realIndex = -1;
		MAttachmentEntry[] entries = attachment.getEntries();
		for (int i = 0; i < entries.length; i++)
		{
			MAttachmentEntry entry = entries[i];
			if (entry.getIndex() == attachmentIndex)
			{
				realIndex = i;
				break;
			}
		}
		if (realIndex < 0)
		{
			if (log.isLoggable(Level.FINE)) log.fine("No Attachment Entry for Index=" 
				+ attachmentIndex + " - " + attachment);
			return "Attachment Entry not found";
		}
		
		MAttachmentEntry entry = entries[realIndex];
		if (entry.getData() == null)
		{
			if (log.isLoggable(Level.FINE)) log.fine("Empty Attachment Entry for Index=" 
				+ attachmentIndex + " - " + attachment);
			return "Attachment Entry empty";
		}
		
		//	Stream Attachment Entry
		try
		{
			int bufferSize = 2048; //	2k Buffer
			int fileLength = entry.getData().length;
			//
			response.setContentType(entry.getContentType());
			response.setBufferSize(bufferSize);
			response.setContentLength(fileLength);
			//
			if (log.isLoggable(Level.FINE)) log.fine(entry.toString());
			long time = System.currentTimeMillis();		//	timer start
			//
			ServletOutputStream out = response.getOutputStream ();
			out.write (entry.getData());
			out.flush();
			out.close();
			//
			time = System.currentTimeMillis() - time;
			double speed = (fileLength/(double)1024) / (time/(double)1000);
			if (log.isLoggable(Level.INFO)) log.info("Length=" 
				+ fileLength + " - " 
				+ time + " ms - " 
				+ speed + " kB/sec - " + entry.getContentType());
		}
		catch (IOException ex)
		{
			log.log(Level.SEVERE, ex.toString());
			return "Streaming error - " + ex;
		}
		return null;
	}	//	streamAttachment
	
	/**
	 * 	Stream File
	 *	@param response response
	 *	@param file file to stream
	 *	@return error message or null
	 */
	public static String streamFile (HttpServletResponse response, File file)
	{
		if (file == null)
			return "No File";
		if (!file.exists())
			return "File not found: " + file.getAbsolutePath();
		
		MimeType mimeType = MimeType.get(file.getAbsolutePath());
		//	Stream File
		try (FileInputStream in = new FileInputStream(file))
		{
			int bufferSize = 2048; //	2k Buffer
			int fileLength = (int)file.length();
			//
			response.setContentType(mimeType.getMimeType());
			response.setBufferSize(bufferSize);
			response.setContentLength(fileLength);
			//
			if (log.isLoggable(Level.FINE)) log.fine(file.toString());
			long time = System.currentTimeMillis();		//	timer start
			//	Get Data
			try (ServletOutputStream out = response.getOutputStream ();) {
			int c = 0;
			while ((c = in.read()) != -1)
				out.write(c);
			//
			out.flush();}			
			//
			time = System.currentTimeMillis() - time;
			double speed = (fileLength/(double)1024) / (time/(double)1000);
			if (log.isLoggable(Level.INFO)) log.info("Length=" 
				+ fileLength + " - " 
				+ time + " ms - " 
				+ speed + " kB/sec - " + mimeType);
		}
		catch (IOException ex)
		{
			log.log(Level.SEVERE, ex.toString());
			return "Streaming error - " + ex;
		}
		return null;
	}	//	streamFile
		
	/**
	 * 	Remove Cookie with web user by setting user to _
	 * 	@param request request (for context path)
	 * 	@param response response to add cookie
	 *  @deprecated
	 */
	@Deprecated
	public static void deleteCookieWebUser (HttpServletRequest request, HttpServletResponse response, String COOKIE_NAME)
	{
		Cookie cookie = new Cookie(COOKIE_NAME, " ");
		cookie.setComment("adempiere Web User");
		cookie.setPath(request.getContextPath());
		cookie.setMaxAge(1);      //  second
		cookie.setSecure(true);
		response.addCookie(cookie);
	}	//	deleteCookieWebUser
	
	/**
	 * 	Get Remote From info
	 * 	@param request request
	 * 	@return remore info
	 */
	public static String getFrom (HttpServletRequest request)
	{
		String host = request.getRemoteHost();
		if (!host.equals(request.getRemoteAddr()))
			host += " (" + request.getRemoteAddr() + ")";
		return host;
	}	//	getFrom

	/**
	 * 	Add Cookie with web user
	 * 	@param request request (for context path)
	 * 	@param response response to add cookie
	 * 	@param webUser email address
	 *  @deprecated
	 */
	@Deprecated
	public static void addCookieWebUser (HttpServletRequest request, HttpServletResponse response, String webUser, String COOKIE_NAME)
	{
	  try {
		Cookie cookie = new Cookie(COOKIE_NAME, URLEncoder.encode(webUser, "utf-8"));
		cookie.setComment("adempiere Web User");
		cookie.setPath(request.getContextPath());
		cookie.setMaxAge(2592000);      //  30 days in seconds   60*60*24*30
		cookie.setSecure(true);
		response.addCookie(cookie);
	  } catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	  }
	}	//	setCookieWebUser

	/**
	 * 	Update Web User
	 * 	@param request request
	 * 	@param wu user
	 * 	@param updateEMailPwd if true, change email/password
	 * 	@return true if saved
	 *  @deprecated
	 */
	@Deprecated
	public static boolean updateFields (HttpServletRequest request, WebUser wu, boolean updateEMailPwd)
	{
		if (updateEMailPwd)
		{
			String s = WebUtil.getParameter (request, "PasswordNew");
			wu.setPasswordMessage (null);
			wu.setPassword (s);
			if (wu.getPasswordMessage () != null)
            {
                return false;
            }
			//
			s = WebUtil.getParameter (request, "EMail");
			if (!WebUtil.isEmailValid (s))
			{
				wu.setPasswordMessage ("EMail Invalid");
				return false;
			}
			wu.setEmail (s.trim());
		}
		//
		StringBuilder mandatory = new StringBuilder();
		String s = WebUtil.getParameter (request, "Name");
		if (s != null && s.length() != 0)
			wu.setName(s.trim());
		else
			mandatory.append(" - Name");
		s = WebUtil.getParameter (request, "Company");
		if (s != null && s.length() != 0)
			wu.setCompany(s);
		s = WebUtil.getParameter (request, "Title");
		if (s != null && s.length() != 0)
			wu.setTitle(s);
		//
		s = WebUtil.getParameter (request, "Address");
		if (s != null && s.length() != 0)
			wu.setAddress(s);
		else
			mandatory.append(" - Address");
		s = WebUtil.getParameter (request, "Address2");
		if (s != null && s.length() != 0)
			wu.setAddress2(s);
		//
		s = WebUtil.getParameter (request, "City");
		if (s != null && s.length() != 0)
			wu.setCity(s);
		else
			mandatory.append(" - City");
		s = WebUtil.getParameter (request, "Postal");
		if (s != null && s.length() != 0)
			wu.setPostal(s);
		else
			mandatory.append(" - Postal");
		//	Set Country before Region for validation
		s = WebUtil.getParameter (request, "C_Country_ID");
		if (s != null && s.length() != 0)
			wu.setC_Country_ID(s);
		s = WebUtil.getParameter (request, "C_Region_ID");
		if (s != null && s.length() != 0)
			wu.setC_Region_ID(s);
		s = WebUtil.getParameter (request, "RegionName");
		if (s != null && s.length() != 0)
			wu.setRegionName(s);
		//
		s = WebUtil.getParameter (request, "Phone");
		if (s != null && s.length() != 0)
			wu.setPhone(s);
		s = WebUtil.getParameter (request, "Phone2");
		if (s != null && s.length() != 0)
			wu.setPhone2(s);
		s = WebUtil.getParameter (request, "C_BP_Group_ID");
		if (s != null && s.length() != 0)
			wu.setC_BP_Group_ID (s);
		s = WebUtil.getParameter (request, "Fax");
		if (s != null && s.length() != 0)
			wu.setFax(s);
		//
		if (mandatory.length() > 0)
		{
			mandatory.insert(0, "Enter Mandatory");
			wu.setSaveErrorMessage(mandatory.toString());
			return false;
		}
		return wu.save();
	}	//	updateFields
	
	/**
	 * Get server name
	 * @return Server name including host name: IP : instance name
	 */
	public static String getServerName(){
		StringBuilder strBuilder = new StringBuilder();
		
		try {
			strBuilder.append(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			log.log(Level.WARNING, "Local host or IP not found", e);
		}
		strBuilder.append(":").append(getHostIP());
					
		return strBuilder.toString();
	}
	
	/**
	 * Get server ip
	 * @return server ip
	 */
	public static String getHostIP() {
		String retVal = null;
		try {
			InetAddress localAddress= InetAddress.getLocalHost();
			if (!localAddress.isLinkLocalAddress() && !localAddress.isLoopbackAddress() && localAddress.isSiteLocalAddress())
				return localAddress.getHostAddress();
		} catch (UnknownHostException e) {
			log.log(Level.WARNING,
					"UnknownHostException while retrieving host ip");
		}
		
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isLinkLocalAddress()
							&& inetAddress.isSiteLocalAddress()) {
						retVal = inetAddress.getHostAddress().toString();
						break;
					}
				}
			}
		} catch (SocketException e) {
			log.log(Level.WARNING, "Socket Exeception while retrieving host ip");
		}

		if (retVal == null) {
			try {
				retVal = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				log.log(Level.WARNING,
						"UnknownHostException while retrieving host ip");
			}
		}
		return retVal;
	}

	/**
	 * returns true if the URL exists and answer with a 200 code 
	 * @param urlString
	 * @return boolean
	 */
	public static boolean isUrlOk(String urlString) {
		int responseCode = 0;
		URL url;
		try {
			url = new URL(urlString);
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			huc.setRequestMethod("HEAD");
			responseCode = huc.getResponseCode();
		} catch (IOException e) {
			responseCode = -1;
		} 
		return responseCode == HttpURLConnection.HTTP_OK;
	}

}   //  WebUtil
