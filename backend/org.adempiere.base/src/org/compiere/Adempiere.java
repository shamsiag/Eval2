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
package org.compiere;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.event.EventListenerList;

import org.adempiere.base.Core;
import org.compiere.db.CConnection;
import org.compiere.model.MClient;
import org.compiere.model.MSysConfig;
import org.compiere.model.MSystem;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ServerStateChangeEvent;
import org.compiere.model.ServerStateChangeListener;
import org.compiere.model.SystemProperties;
import org.compiere.util.CLogFile;
import org.compiere.util.CLogMgt;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Login;
import org.compiere.util.SecureEngine;
import org.compiere.util.SecureInterface;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 *  Static methods for iDempiere startup, system info and global thread pool.
 *
 *  @author Jorg Janke
 */
public final class Adempiere
{
	/** Timestamp                   */
	@Deprecated
	static public final String	ID				= "$Id: Adempiere.java,v 1.8 2006/08/11 02:58:14 jjanke Exp $";
	/** Main Version String         */
	static public String	MAIN_VERSION	= "Release 13";
	/** Detail Version as date      Used for Client/Server		*/
	static public String	DATE_VERSION	= "2024-12-24";
	/** Database Version as date    Compared with AD_System		*/
	static public String	DB_VERSION		= "2024-12-24";

	/** Product Name            */
	static public final String	NAME 			= "iDempiere\u00AE";
	/** URL of Product          */
	static public final String	URL				= "www.idempiere.org";
	/** 16*16 Product Image. **/
	static private final String	s_File16x16		= "images/iD16.gif";
	/** 32*32 Product Image.   	*/
	static private final String	s_file32x32		= "images/iD32.gif";
	/** 100*30 Product Image.  	*/
	static private final String	s_file100x30	= "images/iD10030.png";
	/** 48*15 Product Image.   	*/
	static private final String	s_file48x15		= "images/iDempiere.png";
	static private final String	s_file48x15HR	= "images/iDempiereHR.png";
	/** Header Logo				*/
	static private final String	s_fileHeaderLogo= "images/header-logo.png";
	/** Support Email           */
	static private String		s_supportEmail	= "";

	/** Subtitle                */
	static public final String	SUB_TITLE		= "Smart Suite ERP, CRM and SCM";
	static public final String	ADEMPIERE_R		= "iDempiere\u00AE";
	static public final String	COPYRIGHT		= "\u00A9 1999-2025 iDempiere\u00AE";

	static private String		s_ImplementationVersion = null;
	static private String		s_ImplementationVendor = null;

	static private Image 		s_image16;
	static private Image 		s_image48x15;
	static private Image 		s_imageLogo;
	static private ImageIcon 	s_imageIcon32;
	static private ImageIcon 	s_imageIconLogo;
	static private Image		s_headerLogo;

	static private final String ONLINE_HELP_URL = "http://wiki.idempiere.org";

	/**	Logging								*/
	private static CLogger		log = null;
	
	/** Thread pool **/
	private final static ScheduledThreadPoolExecutor threadPoolExecutor = createThreadPool();
	static {
		Trx.startTrxMonitor();
	}
	
	 /** A list of event listeners for this component.	*/
    private static EventListenerList m_listenerList = new EventListenerList();

	static {
		ClassLoader loader = Adempiere.class.getClassLoader();
		InputStream inputStream = loader.getResourceAsStream("org/adempiere/version.properties");
		if (inputStream != null)
		{
			Properties properties = new Properties();
			try {
				properties.load(inputStream);
				if (properties.containsKey("MAIN_VERSION"))
					MAIN_VERSION = properties.getProperty("MAIN_VERSION");
				if (properties.containsKey("DATE_VERSION"))
					DATE_VERSION = properties.getProperty("DATE_VERSION");
				if (properties.containsKey("DB_VERSION"))
					DB_VERSION = properties.getProperty("DB_VERSION");
				if (properties.containsKey("IMPLEMENTATION_VERSION"))
					s_ImplementationVersion = properties.getProperty("IMPLEMENTATION_VERSION");
				if (properties.containsKey("IMPLEMENTATION_VENDOR"))
					s_ImplementationVendor = properties.getProperty("IMPLEMENTATION_VENDOR");
			} catch (IOException e) {
			}
		}
	}

	/**
	 *  Get Product Name
	 *  @return Application Name
	 */
	public static String getName()
	{
		return NAME;
	}   //  getName

	/**
	 *  Get Product Version
	 *  @return Application Version
	 */
	public static String getVersion()
	{
		String version = MSysConfig.getValue(MSysConfig.APPLICATION_MAIN_VERSION, null);
		if(version != null)
			return version;

		IProduct product = Platform.getProduct();
		if (product != null) {
			Bundle bundle = product.getDefiningBundle();
			if (bundle != null) {
				return bundle.getVersion().toString();
			}
		}
		else
		{
			Bundle bundle = Platform.getBundle("org.adempiere.base");
			if (bundle != null) {
				return bundle.getVersion().toString();
			}
		}
		return "Unknown";
	}   //  getVersion

	/**
	 * @return true if application version should be shown to user
	 */
	public static boolean isVersionShown(){ 
		boolean defaultVal = MSystem.SYSTEMSTATUS_Evaluation.equals(MSystem.get(Env.getCtx()).getSystemStatus());
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_MAIN_VERSION_SHOWN, defaultVal);
	}

	/**
	 * @return true if iDempiere AD version should be shown to user
	 */
	public static boolean isDBVersionShown(){
		boolean defaultVal = MSystem.SYSTEMSTATUS_Evaluation.equals(MSystem.get(Env.getCtx()).getSystemStatus());
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_DATABASE_VERSION_SHOWN, defaultVal);
	}

	/**
	 * @return true if implementation vendor name should be shown to user
	 */
	public static boolean isVendorShown(){
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_IMPLEMENTATION_VENDOR_SHOWN, true);
	}

	/**
	 * @return true if JVM info should be shown to user
	 */
	public static boolean isJVMShown(){
		boolean defaultVal = MSystem.SYSTEMSTATUS_Evaluation.equals(MSystem.get(Env.getCtx()).getSystemStatus());
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_JVM_VERSION_SHOWN, defaultVal);
	}

	/**
	 * @return true if OS information should be shown to user
	 */
	public static boolean isOSShown(){
		boolean defaultVal = MSystem.SYSTEMSTATUS_Evaluation.equals(MSystem.get(Env.getCtx()).getSystemStatus());
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_OS_INFO_SHOWN, defaultVal);
	}

	/**
	 * @return true if application host should be shown to user
	 */
	public static boolean isHostShown() 
	{
		boolean defaultVal = MSystem.SYSTEMSTATUS_Evaluation.equals(MSystem.get(Env.getCtx()).getSystemStatus());
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_HOST_SHOWN, defaultVal);
	}

	/**
	 * Defines if this server is used for demo purposes, to show the login information at the left panel and provide quick fill of User/Password
	 * @return
	 */
	public static boolean isLoginInfoShown() {
		boolean inEvaluation = MSystem.SYSTEMSTATUS_Evaluation.equals(MSystem.get(Env.getCtx()).getSystemStatus());
		return MSysConfig.getBooleanValue(MSysConfig.APPLICATION_LOGIN_INFO_SHOWN, false) && inEvaluation;
	}

	/**
	 * @return version of iDempiere AD
	 */
	public static String getDatabaseVersion() 
	{
		return MSysConfig.getValue(MSysConfig.APPLICATION_DATABASE_VERSION,
				DB.getSQLValueString(null, "select lastmigrationscriptapplied from ad_system"));
	}
	
	/**
	 *	Short Summary
	 *  @return short summary (name + main_version + sub_title)
	 */
	public static String getSum()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(NAME).append(" ").append(MAIN_VERSION).append(SUB_TITLE);
		return sb.toString();
	}	//	getSum

	/**
	 *	Summary
	 *  @return Summary (name + main_version + date_version + sub_title+copyright
	 *  + implementation_version + implementation_vendor)
	 */
	public static String getSummary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(NAME).append(" ")
			.append(MAIN_VERSION).append("_").append(DATE_VERSION)
			.append(" -").append(SUB_TITLE)
			.append("- ").append(COPYRIGHT)
			.append("; Implementation: ").append(getImplementationVersion())
			.append(" - ").append(getImplementationVendor());
		return sb.toString();
	}	//	getSummary

	/**
	 * Initialize implementation version and vendor text from Package Info.
	 */
	private static void setPackageInfo()
	{
		if (s_ImplementationVendor != null)
			return;

		Package adempierePackage = Adempiere.class.getClassLoader().getDefinedPackage("org.compiere");
		s_ImplementationVendor = adempierePackage.getImplementationVendor();
		s_ImplementationVersion = adempierePackage.getImplementationVersion();
		if (s_ImplementationVendor == null)
		{
			s_ImplementationVendor = "Supported by iDempiere community";
			s_ImplementationVersion = "iDempiere";
		}
	}	//	setPackageInfo

	/**
	 * 	Get Implementation Version
	 * 	@return Implementation-Version
	 */
	public static String getImplementationVersion()
	{
		if (s_ImplementationVersion == null)
			setPackageInfo();
		return s_ImplementationVersion;
	}	//	getImplementationVersion

	/**
	 * 	Get Implementation Vendor
	 * 	@return Implementation-Vendor
	 */
	public static String getImplementationVendor()
	{
		if(DB.isConnected()){
			String vendor = MSysConfig.getValue(MSysConfig.APPLICATION_IMPLEMENTATION_VENDOR, null);
			if(vendor != null)
				return vendor;
		}
		if (s_ImplementationVendor == null)
			setPackageInfo();
		return s_ImplementationVendor;
	}	//	getImplementationVendor

	/**
	 *  Get Checksum
	 *  @return checksum
	 */
	public static int getCheckSum()
	{
		return getSum().hashCode();
	}   //  getCheckSum

	/**
	 *	Summary in ASCII
	 *  @return Summary in ASCII
	 */
	public static String getSummaryAscii()
	{
		String retValue = getSummary();
		//  Registered Trademark
		retValue = Util.replace(retValue, "\u00AE", "(r)");
		//  Trademark
		retValue = Util.replace(retValue, "\u2122", "(tm)");
		//  Copyright
		retValue = Util.replace(retValue, "\u00A9", "(c)");
		//  Cr
		retValue = Util.replace(retValue, Env.NL, " ");
		retValue = Util.replace(retValue, "\n", " ");
		return retValue;
	}	//	getSummaryAscii

	/**
	 * 	Get Java VM Info
	 *	@return VM info
	 */
	public static String getJavaInfo()
	{
		return System.getProperty("java.vm.name")
			+ " " + System.getProperty("java.vm.version");
	}	//	getJavaInfo

	/**
	 * 	Get Operating System Info
	 *	@return OS info
	 */
	public static String getOSInfo()
	{
		return System.getProperty("os.name") + " "
			+ System.getProperty("os.version") + " "
			+ System.getProperty("sun.os.patch.level");
	}	//	getJavaInfo

	/**
	 *  Get URL of product
	 *  @return URL or product
	 */
	public static String getURL()
	{
		return "http://" + URL;
	}   //  getURL

	/**
	 * @return online help URL
	 */
	public static String getOnlineHelpURL()
	{
		return ONLINE_HELP_URL;
	}

	/**
	 *  Get Sub Title
	 *  @return Product Subtitle
	 */
	public static String getSubtitle()
	{
		return SUB_TITLE;
	}   //  getSubitle

	/**
	 *  Get 16x16 Image.
	 *	@return Image Icon
	 */
	public static Image getImage16()
	{
		if (s_image16 == null)
		{
			Toolkit tk = Toolkit.getDefaultToolkit();
			URL url = Core.getResourceFinder().getResource(s_File16x16);
			if (url == null)
				return null;
			s_image16 = tk.getImage(url);
		}
		return s_image16;
	}   //  getImage16

	/**
	 *  Get 28*15 Logo Image.
	 *  @param hr high resolution
	 *  @return Image Icon
	 */
	public static Image getImageLogoSmall(boolean hr)
	{
		if (s_image48x15 == null)
		{
			Toolkit tk = Toolkit.getDefaultToolkit();
			URL url = null;
			if (hr)
				url = Core.getResourceFinder().getResource(s_file48x15HR);
			else
				url = Core.getResourceFinder().getResource(s_file48x15);
			if (url == null)
				return null;
			s_image48x15 = tk.getImage(url);
		}
		return s_image48x15;
	}   //  getImageLogoSmall

	/**
	 * Get Header logo
	 * @return Image
	 */
	public static Image getHeaderLogo() {
		if (s_headerLogo == null) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			URL url = Core.getResourceFinder().getResource(s_fileHeaderLogo);
			if (url != null)
				s_headerLogo = tk.getImage(url);
		}
		return s_headerLogo;
	}

	/**
	 *  Get Logo Image.
	 *  @return Image Logo
	 */
	public static Image getImageLogo()
	{
		if (s_imageLogo == null)
		{
			Toolkit tk = Toolkit.getDefaultToolkit();
			URL url = Core.getResourceFinder().getResource(s_file100x30);
			if (url == null)
				return null;
			s_imageLogo = tk.getImage(url);
		}
		return s_imageLogo;
	}   //  getImageLogo

	/**
	 *  Get 32x32 ImageIcon.
	 *	@return Image Icon
	 */
	public static ImageIcon getImageIcon32()
	{
		if (s_imageIcon32 == null)
		{
			URL url = Core.getResourceFinder().getResource(s_file32x32);
			if (url == null)
				return null;
			s_imageIcon32 = new ImageIcon(url);
		}
		return s_imageIcon32;
	}   //  getImageIcon32

	/**
	 *  Get 100x30 ImageIcon.
	 *	@return Image Icon
	 */
	public static ImageIcon getImageIconLogo()
	{
		if (s_imageIconLogo == null)
		{
			URL url = Core.getResourceFinder().getResource(s_file100x30);
			if (url == null)
				return null;
			s_imageIconLogo = new ImageIcon(url);
		}
		return s_imageIconLogo;
	}   //  getImageIconLogo

	/**
	 *  Get instance home directory
	 *  @return Home directory
	 */
	public static String getAdempiereHome()
	{
		//  Try Environment
		String retValue = Ini.getAdempiereHome();
		if (retValue == null)
			retValue = File.separator + "idempiere";
		return retValue;
	}   //  getHome

	/**
	 *  Get Support Email
	 *  @return Support mail address
	 */
	public static String getSupportEMail()
	{
		return s_supportEmail;
	}   //  getSupportEMail

	/**
	 *  Set Support Email
	 *  @param email Support mail address
	 */
	public static void setSupportEMail(String email)
	{
		s_supportEmail = email;
	}   //  setSupportEMail

	/**
	 * @return true if started
	 */
	public static synchronized boolean isStarted()
	{
		return (log != null);
	}

	/**
	 *  Startup Client/Server.<br/>
	 *  <pre>
	 *  - Print greeting,
	 *  - Check Java version and
	 *  - load ini parameters
	 *  </pre>
	 *  If it is a client, load/set PLAF and exit if error.<br/>
	 *  If client, you need to call startupEnvironment explicitly!
	 *	@param isClient true for client
	 *  @return successful startup
	 */
	public static synchronized boolean startup (boolean isClient)
	{
		//	Already started
		if (log != null)
			return true;

		//	Check Version
		if (isClient && !Login.isJavaOK(isClient))
			System.exit(1);

		Ini.setClient (isClient);		//	init logging in Ini
		
		if (! isClient)  // Calling this on client is dropping the link with eclipse console
			CLogMgt.initialize(isClient);
		//	Init Log
		log = CLogger.getCLogger(Adempiere.class);
		//	Greeting
		if (log.isLoggable(Level.INFO)) log.info(getSummaryAscii());

		//  System properties
		Ini.loadProperties (false);

		//	Set up Log
		CLogMgt.setLevel(Ini.getProperty(Ini.P_TRACELEVEL));
		if (isClient && Ini.isPropertyBool(Ini.P_TRACEFILE))
			CLogMgt.addHandler(new CLogFile(Ini.findAdempiereHome(), true, isClient));

		//setup specific log level
		Properties properties = Ini.getProperties();
		for(Object key : properties.keySet())
		{
			if (key instanceof String)
			{
				String s = (String)key;
				if (s.endsWith("."+Ini.P_TRACELEVEL))
				{
					String level = properties.getProperty(s);
					s = s.substring(0, s.length() - ("."+Ini.P_TRACELEVEL).length());
					CLogMgt.setLevel(s, level);
				}
			}
		}
		
		//	Set UI
		if (isClient)
		{
			if (CLogMgt.isLevelAll())
				log.log(Level.FINEST, System.getProperties().toString());
		}

		loadDBProvider();
		
		//  Set Default Database Connection from Ini
		DB.setDBTarget(CConnection.get());

		createThreadPool();
		
		fireServerStateChanged(new ServerStateChangeEvent(new Object(), ServerStateChangeEvent.SERVER_START));
		
		if (isClient)		//	don't test connection
			return false;	//	need to call

		return startupEnvironment(isClient);
	}   //  startup

	private static void loadDBProvider() {
		try {
			Adempiere.class.getClassLoader().loadClass("org.adempiere.db.oracle.config.ConfigOracle");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Adempiere.class.getClassLoader().loadClass("org.adempiere.db.postgresql.config.ConfigPostgreSQL");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create thread pool
	 * @return ScheduledThreadPoolExecutor
	 */
	private static ScheduledThreadPoolExecutor createThreadPool() {
		int max = Runtime.getRuntime().availableProcessors() * 20;
		int defaultMax = max;
		Properties properties = Ini.getProperties();
		String maxSize = properties.getProperty("MaxThreadPoolSize");
		if (maxSize != null) {
			try {
				max = Integer.parseInt(maxSize);
			} catch (Exception e) {}
		}
		if (max <= 0) {
			max = defaultMax;
		}
		
		// start thread pool
		return new ScheduledThreadPoolExecutor(max);								
	}

	/**
	 * 	Startup Adempiere Environment.<br/>
	 * 	Automatically called for Server connections. <br/>
	 *	@param isClient true if client connection
	 *  @return successful startup
	 */
	public static boolean startupEnvironment (boolean isClient)
	{
		startup(isClient);		//	returns if already initiated
		if (!DB.isConnected())
		{
			log.severe ("No Database");
			return false;
		}
		
		//	Check Build
		if (!DB.isBuildOK(Env.getCtx()))
		{
			if (isClient)
				System.exit(1);
			log = null;
			return false;
		}
		
		MSystem system = MSystem.get(Env.getCtx());	//	Initializes Base Context too
		if (system == null)
			return false;
		
		//	Initialize main cached Singletons
		ModelValidationEngine.get();
		try
		{
			String className = system.getEncryptionKey();
			if (className == null || className.length() == 0)
			{
				className = SystemProperties.getAdempiereSecure();
				if (className != null && className.length() > 0
					&& !className.equals(SecureInterface.ADEMPIERE_SECURE_DEFAULT))
				{
					SecureEngine.init(className);	//	test it
					system.setEncryptionKey(className);
					system.saveEx();
				}
			}
			SecureEngine.init(className);

			//
			if (isClient)
				MClient.get(Env.getCtx(),0);			//	Login Client loaded later
			else
				MClient.getAll(Env.getCtx());
		}
		catch (Exception e)
		{
			log.warning("Environment problems: " + e.toString());
		}

		//	Start Workflow Document Manager (in other package) for PO
		String className = null;
		try
		{
			className = "org.compiere.wf.DocWorkflowManager";
			Class.forName(className);
			//	Initialize Archive Engine
			className = "org.compiere.print.ArchiveEngine";
			Class.forName(className);
		}
		catch (Exception e)
		{
			log.warning("Not started: " + className + " - " + e.getMessage());
		}
		
		if (!isClient)
			DB.updateMail();
				
		return true;
	}	//	startupEnvironment

	/**
	 * @param name
	 * @return URL for named resource
	 */
	public static URL getResource(String name) {
		return Core.getResourceFinder().getResource(name);
	}
	
	/**
	 * Stop instance
	 */
	public static synchronized void stop() {
		threadPoolExecutor.shutdown();
		log = null;
	}
	
	/**
	 * @return {@link ScheduledThreadPoolExecutor}
	 */
	public static ScheduledThreadPoolExecutor getThreadPoolExecutor() {
		return threadPoolExecutor;
	}
	
	/**
	 *  @param l listener
	 */
	public static synchronized void removeServerStateChangeListener(ServerStateChangeListener l)
	{
		m_listenerList.remove(ServerStateChangeListener.class, l);
	}
	
	/**
	 *  @param l listener
	 */
	public static synchronized void addServerStateChangeListener(ServerStateChangeListener l)
	{
		m_listenerList.add(ServerStateChangeListener.class, l);
	}
	
	/**
	 * Fire event
	 * @param e
	 */
	private static synchronized void fireServerStateChanged(ServerStateChangeEvent e)
	{
		ServerStateChangeListener[] listeners = m_listenerList.getListeners(ServerStateChangeListener.class);
		for (int i = 0; i < listeners.length; i++)
        	listeners[i].stateChange(e);
	}
}	//	Adempiere
