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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.adempiere.base.Generated;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.Adempiere;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

/**
 *  General Utility methods
 *
 *  @author     Jorg Janke
 *  @version    $Id: Util.java,v 1.3 2006/07/30 00:52:23 jjanke Exp $
 *  
 *  @author     Teo Sarca, SC ARHIPAC SERVICE SRL - BF [ 1748346 ]
 */
public class Util
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(Util.class.getName());

	/**
	 *	Replace String values.
	 *  @param value string to be processed
	 *  @param oldPart old part
	 *  @param newPart replacement - can be null or ""
	 *  @return String with replaced values
	 */
	public static String replace (String value, String oldPart, String newPart)
	{
		if (value == null || value.length() == 0
			|| oldPart == null || oldPart.length() == 0)
			return value;
		//
		int oldPartLength = oldPart.length();
		String oldValue = value;
		StringBuilder retValue = new StringBuilder();
		int pos = oldValue.indexOf(oldPart);
		while (pos != -1)
		{
			retValue.append (oldValue.substring(0, pos));
			if (newPart != null && newPart.length() > 0)
				retValue.append(newPart);
			oldValue = oldValue.substring(pos+oldPartLength);
			pos = oldValue.indexOf(oldPart);
		}
		retValue.append(oldValue);
		return retValue.toString();
	}	//	replace

	/**
	 * Remove CR / LF from String
	 * @param in input
	 * @return cleaned string
	 */
	public static String removeCRLF (String in)
	{
		char[] inArray = in.toCharArray();
		StringBuilder out = new StringBuilder (inArray.length);
		for (int i = 0; i < inArray.length; i++)
		{
			char c = inArray[i];
			if (c == '\n' || c == '\r')
				;
			else
				out.append(c);
		}
		return out.toString();
	}	//	removeCRLF

	/**
	 * Clean - Remove all white spaces
	 * @param in
	 * @return cleaned string
	 */
	public static String cleanWhitespace (String in)
	{
		char[] inArray = in.toCharArray();
		StringBuilder out = new StringBuilder(inArray.length);
		boolean lastWasSpace = false;
		for (int i = 0; i < inArray.length; i++)
		{
			char c = inArray[i];
			if (Character.isWhitespace(c))
			{
				if (!lastWasSpace)
					out.append (' ');
				lastWasSpace = true;
			}
			else
			{
				out.append (c);
				lastWasSpace = false;
			}
		}
		return out.toString();
	}	//	cleanWhitespace

	/**
	 * Mask HTML content.<br/>
	 * i.e. replace characters with &values;<br/>
	 * CR is not masked.
	 * @param content content
	 * @return masked content
	 * @see #maskHTML(String, boolean)
	 */
	public static String maskHTML (String content)
	{
		return maskHTML (content, false);
	}	//	maskHTML
	
	/**
	 * Mask HTML content.<br/>
	 * i.e. replace characters with &values;
	 * @param content content
	 * @param maskCR convert CR into <br>
	 * @return masked content or null if the <code>content</code> is null
	 */
	public static String maskHTML (String content, boolean maskCR)
	{
		// If the content is null, then return null - teo_sarca [ 1748346 ]
		if (content == null)
			return content;
		//
		StringBuilder out = new StringBuilder();
		char[] chars = content.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			switch (c)
			{
				case '<':
					out.append ("&lt;");
					break;
				case '>':
					out.append ("&gt;");
					break;
				case '&':
					out.append ("&amp;");
					break;
				case '"':
					out.append ("&quot;");
					break;
				case '\'':
					out.append ("&#039;");
					break;
				case '\n':
					if (maskCR)
						out.append ("<br>");
					break;
				//
				default:
					int ii =  (int)c;
					if (ii > 255)		//	Write Unicode
						out.append("&#").append(ii).append(";");
					else
						out.append(c);
					break;
			}
		}
		return out.toString();
	}	//	maskHTML

	/**
	 * Get the number of occurrences of countChar in string.
	 * @param string String to be searched
	 * @param countChar to be counted character
	 * @return number of occurrences
	 */
	public static int getCount (String string, char countChar)
	{
		if (string == null || string.length() == 0)
			return 0;
		int counter = 0;
		char[] array = string.toCharArray();
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == countChar)
				counter++;
		}
		return counter;
	}	//	getCount

	/**
	 * Is String Empty or null
	 * @param str string
	 * @return true if str is empty or null
	 */
	public static boolean isEmpty (String str)
	{
		return isEmpty(str, false);
	}	//	isEmpty
	
	/**
	 * Is String Empty or null
	 * @param str string
	 * @param trimWhitespaces trim whitespaces
	 * @return true if str is empty or null
	 */
	public static boolean isEmpty (String str, boolean trimWhitespaces)
	{
		if (str == null)
			return true;
		if (trimWhitespaces)
			return str.trim().length() == 0;
		else
			return str.length() == 0;
	}	//	isEmpty

	/**
	 * Remove accents from string
	 * @param text string
	 * @return Unaccented String
	 */
	public static String deleteAccents(String text) {
	    String nfdNormalizedString = Normalizer.normalize(text, Normalizer.Form.NFD); 
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    text = pattern.matcher(nfdNormalizedString).replaceAll("");
		return text;
	}

	/**
	 * Find index of search character in str.<br/>
	 * This ignores content in () and quoted text ('texts').
	 * @param str string
	 * @param search search character
	 * @return index or -1 if not found
	 */
	public static int findIndexOf (String str, char search)
	{
		return findIndexOf(str, search, search);
	}   //  findIndexOf

	/**
	 *  Find index of search characters in str.<br/>
	 *  This ignores content in () and quoted text ('texts').
	 *  @param str string
	 *  @param search1 first search character
	 *  @param search2 second search character (or)
	 *  @return index or -1 if not found
	 */
	public static int findIndexOf (String str, char search1, char search2)
	{
		if (str == null)
			return -1;
		//
		int endIndex = -1;
		int parCount = 0;
		boolean ignoringText = false;
		int size = str.length();
		while (++endIndex < size)
		{
			char c = str.charAt(endIndex);
			if (c == '\'')
				ignoringText = !ignoringText;
			else if (!ignoringText)
			{
				if (parCount == 0 && (c == search1 || c == search2))
					return endIndex;
				else if (c == ')')
						parCount--;
				else if (c == '(')
					parCount++;
			}
		}
		return -1;
	}   //  findIndexOf

	/**
	 *  Find index of search string in str.<br/>
	 *  This ignores content in () and quoted text ('texts')
	 *  @param str string
	 *  @param search search string
	 *  @return index or -1 if not found
	 */
	public static int findIndexOf (String str, String search)
	{
		if (str == null || search == null || search.length() == 0)
			return -1;
		//
		int endIndex = -1;
		int parCount = 0;
		boolean ignoringText = false;
		int size = str.length();
		while (++endIndex < size)
		{
			char c = str.charAt(endIndex);
			if (c == '\'')
				ignoringText = !ignoringText;
			else if (!ignoringText)
			{
				if (parCount == 0 && c == search.charAt(0))
				{
					if (str.substring(endIndex).startsWith(search))
						return endIndex;
				}
				else if (c == ')')
						parCount--;
				else if (c == '(')
					parCount++;
			}
		}
		return -1;
	}   //  findIndexOf

	/**
	 *  Return Hex String representation of byte b
	 *  @param b byte
	 *  @return Hex
	 */
	static public String toHex (byte b)
	{
		char hexDigit[] = {
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
		};
		char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
		return new String(array);
	}

	/**
	 *  Return Hex String representation of char c
	 *  @param c character
	 *  @return Hex
	 */
	static public String toHex (char c)
	{
		byte hi = (byte) (c >>> 8);
		byte lo = (byte) (c & 0xff);
		return toHex(hi) + toHex(lo);
	}   //  toHex
	
	/**
	 * Capitalize first character of a word
	 * @param in string
	 * @return Capitalize string
	 */
	public static String initCap (String in)
	{
		if (in == null || in.length() == 0)
			return in;
		//
		boolean capitalize = true;
		char[] data = in.toCharArray();
		for (int i = 0; i < data.length; i++)
		{
			if (data[i] == ' ' || Character.isWhitespace(data[i]))
				capitalize = true;
			else if (capitalize)
			{
				data[i] = Character.toUpperCase (data[i]);
				capitalize = false;
			}
			else
				data[i] = Character.toLowerCase (data[i]);
		}
		return new String (data);
	}	//	initCap
	
	/**
	 * Return a Iterator with only the relevant attributes.<br/>
	 * Fixes implementation in AttributedString, which returns everything.
	 * @param aString attributed string
	 * @param relevantAttributes relevant attributes
	 * @return iterator
	 */
	@Generated
	static public AttributedCharacterIterator getIterator (AttributedString aString, 
		AttributedCharacterIterator.Attribute[] relevantAttributes)
	{
		AttributedCharacterIterator iter = aString.getIterator();
		Set<?> set = iter.getAllAttributeKeys();
		if (set.size() == 0)
			return iter;
		//	Check, if there are unwanted attributes
		Set<AttributedCharacterIterator.Attribute> unwanted = new HashSet<AttributedCharacterIterator.Attribute>(iter.getAllAttributeKeys());
		for (int i = 0; i < relevantAttributes.length; i++)
			unwanted.remove(relevantAttributes[i]);
		if (unwanted.size() == 0)
			return iter;

		//	Create new String
		StringBuilder sb = new StringBuilder();
		for (char c = iter.first(); c != AttributedCharacterIterator.DONE; c = iter.next())
			sb.append(c);
		aString = new AttributedString(sb.toString());

		//	copy relevant attributes
		Iterator<AttributedCharacterIterator.Attribute> it = iter.getAllAttributeKeys().iterator();
		while (it.hasNext())
		{
			AttributedCharacterIterator.Attribute att = it.next();
			if (!unwanted.contains(att))
			{
				for (char c = iter.first(); c != AttributedCharacterIterator.DONE; c = iter.next())
				{
					Object value = iter.getAttribute(att);
					if (value != null)
					{
						int start = iter.getRunStart(att);
						int limit = iter.getRunLimit(att);
					//	System.out.println("Attribute=" + att + " Value=" + value + " Start=" + start + " Limit=" + limit);
						aString.addAttribute(att, value, start, limit);
						iter.setIndex(limit);
					}
				}
			}
		}
		return aString.getIterator();
	}	//	getIterator

	/**
	 * Dump a Map (key=value) to standard out
	 * @param map Map
	 */
	@Generated
	static public void dump (Map<Object,Object> map)
	{
		System.out.println("Dump Map - size=" + map.size());
		Iterator<Object> it = map.keySet().iterator();
		while (it.hasNext())
		{
			Object key = it.next();
			Object value = map.get(key);
			System.out.println(key + "=" + value);
		}
	}	//	dump (Map)

	/**
	 * Print Action and Input Map for component
	 * @param comp  Component with ActionMap
	 * @deprecated Swing client have been deprecated
	 */
	@Deprecated
	@Generated
	public static void printActionInputMap (JComponent comp)
	{
		//	Action Map
		ActionMap am = comp.getActionMap();
		Object[] amKeys = am.allKeys(); //  including Parents
		if (amKeys != null)
		{
			System.out.println("-------------------------");
			System.out.println("ActionMap for Component " + comp.toString());
			for (int i = 0; i < amKeys.length; i++)
			{
				Action a = am.get(amKeys[i]);

				StringBuilder sb = new StringBuilder("- ");
				sb.append(a.getValue(Action.NAME));
				if (a.getValue(Action.ACTION_COMMAND_KEY) != null)
					sb.append(", Cmd=").append(a.getValue(Action.ACTION_COMMAND_KEY));
				if (a.getValue(Action.SHORT_DESCRIPTION) != null)
					sb.append(" - ").append(a.getValue(Action.SHORT_DESCRIPTION));
				System.out.println(sb.toString() + " - " + a);
			}
		}
		/**	Same as below
		KeyStroke[] kStrokes = comp.getRegisteredKeyStrokes();
		if (kStrokes != null)
		{
		System.out.println("-------------------------");
			System.out.println("Registered Key Strokes - " + comp.toString());
			for (int i = 0; i < kStrokes.length; i++)
			{
				System.out.println("- " + kStrokes[i].toString());
			}
		}
		/** Focused				*/
		InputMap im = comp.getInputMap(JComponent.WHEN_FOCUSED);
		KeyStroke[] kStrokes = im.allKeys();
		if (kStrokes != null)
		{
			System.out.println("-------------------------");
			System.out.println("InputMap for Component When Focused - " + comp.toString());
			for (int i = 0; i < kStrokes.length; i++)
			{
				System.out.println("- " + kStrokes[i].toString() + " - "
					+ im.get(kStrokes[i]).toString());
			}
		}
		/** Focused in Window	*/
		im = comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		kStrokes = im.allKeys();
		if (kStrokes != null)
		{
			System.out.println("-------------------------");
			System.out.println("InputMap for Component When Focused in Window - " + comp.toString());
			for (int i = 0; i < kStrokes.length; i++)
			{
				System.out.println("- " + kStrokes[i].toString() + " - "
					+ im.get(kStrokes[i]).toString());
			}
		}
		/** Focused when Ancester	*/
		im = comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		kStrokes = im.allKeys();
		if (kStrokes != null)
		{
			System.out.println("-------------------------");
			System.out.println("InputMap for Component When Ancestor - " + comp.toString());
			for (int i = 0; i < kStrokes.length; i++)
			{
				System.out.println("- " + kStrokes[i].toString() + " - "
					+ im.get(kStrokes[i]).toString());
			}
		}
		System.out.println("-------------------------");
	}   //  printActionInputMap

	/**
	 * Is str a 8 Bit string
	 * @param str string
	 * @return true if str doesn't contains chars &gt; 255
	 */
	public static boolean is8Bit (String str)
	{
		if (str == null || str.length() == 0)
			return true;
		char[] cc = str.toCharArray();
		for (int i = 0; i < cc.length; i++)
		{
			if (cc[i] > 255)
			{
				return false;
			}
		}
		return true;
	}	//	is8Bit
	
	/**
	 * Remove all Ampersand character (used to indicate shortcut in Swing client) 
	 * @param in input
	 * @return cleaned string
	 */
	public static String cleanAmp (String in)
	{
		if (in == null || in.length() == 0)
			return in;
		int pos = in.indexOf('&');
		if (pos == -1)
			return in;
		//
		if (pos+1 < in.length() && in.charAt(pos+1) != ' ')
			in = in.substring(0, pos) + in.substring(pos+1);
		return in;
	}	//	cleanAmp
	
	/**
	 * Trim to max character length
	 * @param str string
	 * @param length max (inclusive) character length
	 * @return trim string
	 */
	public static String trimLength (String str, int length)
	{
		if (str == null)
			return str;
		if (length <= 0)
			throw new IllegalArgumentException("Trim length invalid: " + length);
		if (str.length() > length) 
			return str.substring(0, length);
		return str;
	}	//	trimLength
	
	/**
	 * Size of String in bytes
	 * @param str string
	 * @return size in bytes
	 */
	public static int size (String str)
	{
		if (str == null)
			return 0;
		int length = str.length();
		int size = length;
		try
		{
			size = str.getBytes("UTF-8").length;
		}
		catch (UnsupportedEncodingException e)
		{
			//should never happen
			log.log(Level.SEVERE, str, e);
		}
		return size;
	}	//	size

	/**
	 * Trim to max byte size
	 * @param str string
	 * @param size max size in bytes
	 * @return string
	 */
	public static String trimSize (String str, int size)
	{
		if (str == null)
			return str;
		if (size <= 0)
			throw new IllegalArgumentException("Trim size invalid: " + size);
		//	Assume two byte code
		int length = str.length();
		if (length < size/2)
			return str;
		try
		{
			byte[] bytes = str.getBytes("UTF-8");
			if (bytes.length <= size)
				return str;
			//	create new - may cut last character in half
			byte[] result = new byte[size];
			System.arraycopy(bytes, 0, result, 0, size);
			return new String(result, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			//should never happen
			log.log(Level.SEVERE, str, e);
		}
		return str;
	}	//	trimSize

	/**
	 * Strip diacritics from given string
	 * @param s	original string
	 * @return string without diacritics
	 * @deprecated dummy method, not doing anything
	 */
	@Deprecated(forRemoval = true, since = "12")
	@Generated
	public static String stripDiacritics(String s) {
		return s;
	}

	/**
	 * Set time portion to zero.
	 * @param ts
	 * @return truncated timestamp
	 */
	public static Timestamp removeTime(Timestamp ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(ts);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp(cal.getTimeInMillis());
    }
	
	/**
     * Merge pdf files
     * @param pdfList list of pdf file to merge
     * @param outFile merged output file
     * @throws IOException
     * @throws DocumentException
     * @throws FileNotFoundException
     */
	public static void mergePdf(List<File> pdfList, File outFile) throws IOException,
			DocumentException, FileNotFoundException {
		Document document = null;
		PdfWriter copy = null;
		
		List<PdfReader> pdfReaders = new ArrayList<PdfReader>();
		
		try
		{		
			for (File f : pdfList)
			{
				PdfReader reader = new PdfReader(f.getAbsolutePath());
				
				pdfReaders.add(reader);
				
				if (document == null)
				{
					document = new Document(reader.getPageSizeWithRotation(1));
					copy = PdfWriter.getInstance(document, new FileOutputStream(outFile));
					document.open();
				}
				int pages = reader.getNumberOfPages();
				PdfContentByte cb = copy.getDirectContent();
				for (int i = 1; i <= pages; i++) {
					document.newPage();
					copy.newPage();
					PdfImportedPage page = copy.getImportedPage(reader, i);
					cb.addTemplate(page, 0, 0);
					copy.releaseTemplate(page);
				}
			}			
		}
		finally
		{
			if(document != null)
			{
				document.close();
			}
			for(PdfReader reader:pdfReaders)
			{
				reader.close();
			}
		}
	}

	/**
	 * Make filename safe (replace all unauthorized characters with safe ones)
	 * @param input the filename to check
	 * @returns the corrected filename
	 */
	public static String setFilenameCorrect(String input) {
		String output = Normalizer.normalize(input, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		output = output.replace("/" , "-");
		output = output.replace(":" , "-");
		output = output.replace("*" , "-");
		output = output.replace("<" , "-");
		output = output.replace(">" , "-");
		output = output.replace("%" , "-");
		return output.trim();
	}

	private final static String UUID_REGEX="[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

	/**
	 * Is value a valid UUID string
	 * @param value
	 * @return true if value is a UUID identifier
	 */
	public static boolean isUUID(String value)
	{
		return value == null ? false : value.matches(UUID_REGEX);
	}

	/**
	 * Is running from Eclipse
	 * @return true if there is a directory org.adempiere.base within AdempiereHome or if there is a System property org.idempiere.developermode set to Y 
	 */
	@Generated
	public static boolean isDeveloperMode() {
		return Files.isDirectory(Paths.get(Adempiere.getAdempiereHome() + File.separator + "org.adempiere.base")) || "Y".equals(System.getProperty("org.idempiere.developermode"));
	}
	
	/**
	 * Returns a string with a formatted JSON object  
	 * @return string with a pretty JSON format 
	 */
	public static String prettifyJSONString(String value) {
		Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
		try {
			JsonElement jsonElement = JsonParser.parseString(value);
			return gson.toJson(jsonElement);
	    } catch (JsonSyntaxException e) {
	        throw new AdempiereException(Msg.getMsg(Env.getCtx(), "InvalidJSON"));
	    }
	}


}   //  Util
