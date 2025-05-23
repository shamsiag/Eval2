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
 * Contributor(s) : Layda Salas - globalqss                                   *
 *****************************************************************************/
package org.compiere.model;

import org.adempiere.base.event.IEventTopics;

/**
 *	Model Validator
 *
 *  @author Jorg Janke
 *  @version $Id: ModelValidator.java,v 1.2 2006/07/30 00:58:18 jjanke Exp $
 *
 *  2007/02/26 laydasalasc - globalqss - Add new timings for all before/after events on documents
 */
public interface ModelValidator
{
	/** Model Change Type New		*/
	public static final int TYPE_BEFORE_NEW = 1;			// teo_sarca [ 1675490 ]
	public static final int	TYPE_NEW = 1;
	@Deprecated
	public static final int	CHANGETYPE_NEW = 1;				// Compatibility with Compiere 260c
	public static final int TYPE_AFTER_NEW = 4;			// teo_sarca [ 1675490 ]
	public static final int TYPE_AFTER_NEW_REPLICATION = 7;	// @Trifon
	/** Model Change Type Change	*/
	public static final int	TYPE_BEFORE_CHANGE = 2;		// teo_sarca [ 1675490 ]
	public static final int	TYPE_CHANGE = 2;
	@Deprecated
	public static final int	CHANGETYPE_CHANGE = 2;			// Compatibility with Compiere 260c
	public static final int	TYPE_AFTER_CHANGE = 5;			// teo_sarca [ 1675490 ]
	public static final int	TYPE_AFTER_CHANGE_REPLICATION = 8; // @Trifon
	/** Model Change Type Delete	*/
	public static final int	TYPE_BEFORE_DELETE = 3;		// teo_sarca [ 1675490 ]
	public static final int	TYPE_DELETE = 3;
	@Deprecated
	public static final int	CHANGETYPE_DELETE = 3;			// Compatibility with Compiere 260c
	public static final int	TYPE_AFTER_DELETE = 6;			// teo_sarca [ 1675490 ]
	public static final int	TYPE_BEFORE_DELETE_REPLICATION = 9; // @Trifon

	// Correlation between AD_Table_ScriptValidator.EventModelValidator constants and model change TYPE_ constants.
	public static String[] tableEventValidators = new String[] {
		"", // 0
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableBeforeNew,    // TYPE_BEFORE_NEW = 1
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableBeforeChange, // TYPE_BEFORE_CHANGE = 2
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableBeforeDelete, // TYPE_BEFORE_DELETE = 3
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableAfterNew,     // TYPE_AFTER_NEW = 4
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableAfterChange,  // TYPE_AFTER_CHANGE = 5
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableAfterDelete,   // TYPE_AFTER_DELETE = 6
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableAfterNewReplication,     // TYPE_AFTER_NEW_REPLICATION = 7
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableAfterChangeReplication,  // TYPE_AFTER_CHANGE_REPLICATION = 8
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_TableBeforeDeleteReplication   // TYPE_BEFORE_DELETE_REPLICATION = 9
	};

	// Correlation between OSGi event topic and model change TYPE_ constants
	public static String[] tableEventTopics = new String[] {
		"", // 0
		IEventTopics.PO_BEFORE_NEW,    // TYPE_BEFORE_NEW = 1
		IEventTopics.PO_BEFORE_CHANGE, // TYPE_BEFORE_CHANGE = 2
		IEventTopics.PO_BEFORE_DELETE, // TYPE_BEFORE_DELETE = 3
		IEventTopics.PO_AFTER_NEW,     // TYPE_AFTER_NEW = 4
		IEventTopics.PO_AFTER_CHANGE,  // TYPE_AFTER_CHANGE = 5
		IEventTopics.PO_AFTER_DELETE,   // TYPE_AFTER_DELETE = 6
		IEventTopics.PO_AFTER_NEW_REPLICATION,     // TYPE_AFTER_NEW_REPLICATION = 7
		IEventTopics.PO_AFTER_CHANGE_REPLICATION,  // TYPE_AFTER_CHANGE_REPLICATION = 8
		IEventTopics.PO_BEFORE_DELETE_REPLICATION// TYPE_BEFORE_DELETE_REPLICATION = 9
	};

	/** Called before document is prepared */
	public static final int TIMING_BEFORE_PREPARE = 1;
	@Deprecated
	public static final int DOCTIMING_BEFORE_PREPARE = 1; // Compatibility with Compiere 260c
	/** Called before document is void */
	public static final int TIMING_BEFORE_VOID = 2;
	/** Called before document is close */
	public static final int TIMING_BEFORE_CLOSE = 3;
	/** Called before document is reactivate */
	public static final int TIMING_BEFORE_REACTIVATE = 4;
	/** Called before document is reversecorrect */
	public static final int TIMING_BEFORE_REVERSECORRECT = 5;
	/** Called before document is reverseaccrual */
	public static final int TIMING_BEFORE_REVERSEACCRUAL = 6;
	/** Called before document is completed */
	public static final int TIMING_BEFORE_COMPLETE = 7;
	/** Called after document is prepared */
	public static final int TIMING_AFTER_PREPARE = 8;
	/** Called after document is completed */
	public static final int TIMING_AFTER_COMPLETE = 9;
	@Deprecated
	public static final int DOCTIMING_AFTER_COMPLETE = 9; // Compatibility with Compiere 260c
	/** Called after document is void */
	public static final int TIMING_AFTER_VOID = 10;
	/** Called after document is closed */
	public static final int TIMING_AFTER_CLOSE = 11;
	/** Called after document is reactivated */
	public static final int TIMING_AFTER_REACTIVATE = 12;
	/** Called after document is reversecorrect */
	public static final int TIMING_AFTER_REVERSECORRECT = 13;
	/** Called after document is reverseaccrual */
	public static final int TIMING_AFTER_REVERSEACCRUAL = 14;
	/** Called before document is posted */
	public static final int TIMING_BEFORE_POST = 15;
	/** Called after document is posted */
	public static final int TIMING_AFTER_POST = 16;

	// Correlation between AD_Table_ScriptValidator.EventModelValidator constants and document change TIMING_ constants.
	public static String[] documentEventValidators = new String[] {
		"", // 0
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentBeforePrepare,        // TIMING_BEFORE_PREPARE = 1
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentBeforeVoid,           // TIMING_BEFORE_VOID = 2
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentBeforeClose,          // TIMING_BEFORE_CLOSE = 3
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentBeforeReactivate,     // TIMING_BEFORE_REACTIVATE = 4
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentBeforeReverseCorrect, // TIMING_BEFORE_REVERSECORRECT = 5
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentBeforeReverseAccrual, // TIMING_BEFORE_REVERSEACCRUAL = 6
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentBeforeComplete,       // TIMING_BEFORE_COMPLETE = 7
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentAfterPrepare,         // TIMING_AFTER_PREPARE = 8
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentAfterComplete,        // TIMING_AFTER_COMPLETE = 9
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentAfterVoid,            // TIMING_AFTER_VOID = 10
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentAfterClose,           // TIMING_AFTER_CLOSE = 11
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentAfterReactivate,      // TIMING_AFTER_REACTIVATE = 12
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentAfterReverseCorrect,  // TIMING_AFTER_REVERSECORRECT = 13
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentAfterReverseAccrual,  // TIMING_AFTER_REVERSEACCRUAL = 14
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentBeforePost,           // TIMING_BEFORE_POST = 15
		X_AD_Table_ScriptValidator.EVENTMODELVALIDATOR_DocumentAfterPost             // TIMING_AFTER_POST = 16
	};

	// Correlation between OSGi event topics and document change TIMING_ constants.
	public static String[] documentEventTopics = new String[] {
		"", // 0
		IEventTopics.DOC_BEFORE_PREPARE,        // TIMING_BEFORE_PREPARE = 1
		IEventTopics.DOC_BEFORE_VOID,           // TIMING_BEFORE_VOID = 2
		IEventTopics.DOC_BEFORE_CLOSE,          // TIMING_BEFORE_CLOSE = 3
		IEventTopics.DOC_BEFORE_REACTIVATE,     // TIMING_BEFORE_REACTIVATE = 4
		IEventTopics.DOC_BEFORE_REVERSECORRECT, // TIMING_BEFORE_REVERSECORRECT = 5
		IEventTopics.DOC_BEFORE_REVERSEACCRUAL, // TIMING_BEFORE_REVERSEACCRUAL = 6
		IEventTopics.DOC_BEFORE_COMPLETE,       // TIMING_BEFORE_COMPLETE = 7
		IEventTopics.DOC_AFTER_PREPARE,         // TIMING_AFTER_PREPARE = 8
		IEventTopics.DOC_AFTER_COMPLETE,        // TIMING_AFTER_COMPLETE = 9
		IEventTopics.DOC_AFTER_VOID,            // TIMING_AFTER_VOID = 10
		IEventTopics.DOC_AFTER_CLOSE,           // TIMING_AFTER_CLOSE = 11
		IEventTopics.DOC_AFTER_REACTIVATE,      // TIMING_AFTER_REACTIVATE = 12
		IEventTopics.DOC_AFTER_REVERSECORRECT,  // TIMING_AFTER_REVERSECORRECT = 13
		IEventTopics.DOC_AFTER_REVERSEACCRUAL,  // TIMING_AFTER_REVERSEACCRUAL = 14
		IEventTopics.DOC_BEFORE_POST,           // TIMING_BEFORE_POST = 15
		IEventTopics.DOC_AFTER_POST 			// TIMING_AFTER_POST = 16
	};

	/**
	 * 	Initialize validator.
	 * 	@param engine validation engine
	 *	@param client optional client. null for validator that's applicable to all client.
	 */
	public void initialize (ModelValidationEngine engine, MClient client);

	/**
	 * 	Get Client to be monitored
	 *	@return AD_Client_ID or 0
	 */
	public int getAD_Client_ID();

	/**
	 * 	User logged in.
	 * 	Called before preferences are set.
	 *	@param AD_Org_ID org
	 *	@param AD_Role_ID role
	 *	@param AD_User_ID user
	 *	@return error message or null
	 */
	public String login (int AD_Org_ID, int AD_Role_ID, int AD_User_ID);

    /**
     * 	Model change event of a Table.
     * 	@param po persistent object
     * 	@param type TYPE_ event
     *	@return error message or null
     *	@exception Exception if the recipient wishes the change to be not accept.
     */
	public String modelChange (PO po, int type) throws Exception;

	/**
	 * 	Validate Document Action.<br/>
	 * 	Called as first step of DocAction.prepareIt
	 * 	or at the end of DocAction.completeIt.<br/>
     * 	Note that totals, etc. may not be correct before the prepare stage.
	 *	@param po persistent object
	 *	@param timing see TIMING_ constants
     *	@return error message or null -
     *	if not null, the document will be marked as Invalid.
	 */
	public String docValidate (PO po, int timing);

}	//	ModelValidator
