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
package org.compiere.wf;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MProcessPara;
import org.compiere.model.Query;
import org.compiere.model.X_AD_WF_Node_Para;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutablePOSupport;

/**
 *	Extended Workflow Node Process Parameter Model for AD_WF_Node_Para
 *	
 *  @author Jorg Janke
 *  @version $Id: MWFNodePara.java,v 1.2 2006/07/30 00:51:05 jjanke Exp $
 */
public class MWFNodePara extends X_AD_WF_Node_Para implements ImmutablePOSupport
{
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = -7304684637362248174L;

	/**
	 * 	Get Parameters for a node
	 *	@param ctx context
	 *	@param AD_WF_Node_ID node
	 *	@return array of parameters
	 */
	public static MWFNodePara[] getParameters (Properties ctx, int AD_WF_Node_ID)
	{
		
		List<MWFNodePara> list = new Query(ctx, Table_Name, "AD_WF_Node_ID=?", null)
			.setParameters(new Object[]{AD_WF_Node_ID})
			.setOnlyActiveRecords(true)
			.list();
		MWFNodePara[] retValue = new MWFNodePara[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getParameters
		
    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_WF_Node_Para_UU  UUID key
     * @param trxName Transaction
     */
    public MWFNodePara(Properties ctx, String AD_WF_Node_Para_UU, String trxName) {
        super(ctx, AD_WF_Node_Para_UU, trxName);
    }

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param id id
	 *	@param trxName transaction
	 */
	public MWFNodePara (Properties ctx, int id, String trxName)
	{
		super (ctx, id, trxName);
	}	//	MWFNodePara

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MWFNodePara (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MWFNodePara
	
	/**
	 * Copy constructor
	 * @param copy
	 */
	public MWFNodePara(MWFNodePara copy) 
	{
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 */
	public MWFNodePara(Properties ctx, MWFNodePara copy) 
	{
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MWFNodePara(Properties ctx, MWFNodePara copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
		this.m_processPara = copy.m_processPara != null ? new MProcessPara(ctx, copy.m_processPara, trxName) : null;
	}
	
	/** Linked Process Parameter			*/
	private MProcessPara 	m_processPara = null;
	
	/**
	 * 	Get Process Parameter
	 *	@return process parameter
	 */
	public MProcessPara getProcessPara()
	{
		if (m_processPara == null)
		{
			m_processPara = new MProcessPara (getCtx(), getAD_Process_Para_ID(), get_TrxName());
			if (is_Immutable())
				m_processPara.markImmutable();
		}
		return m_processPara;
	}	//	getProcessPara
	
	/**
	 * 	Get Attribute Name.
	 * 	If not set - retrieve it
	 *	@return attribute name
	 */
	public String getAttributeName ()
	{
		String an = super.getAttributeName ();
		if (an == null || an.length() == 0 && getAD_Process_Para_ID() != 0)
		{
			an = getProcessPara().getColumnName();
			setAttributeName(an);
			saveEx();
		}
		return an;
	}	//	getAttributeName
	
	/**
	 * 	Get Display Type
	 *	@return display type
	 */
	public int getDisplayType()
	{
		return getProcessPara().getAD_Reference_ID();
	}	//	getDisplayType

	/**
	 * 	Is Mandatory
	 *	@return true if mandatory
	 */
	public boolean isMandatory()
	{
		return getProcessPara().isMandatory();
	}	//	isMandatory
	
	/**
	 * 	Set AD_Process_Para_ID
	 *	@param AD_Process_Para_ID id
	 */
	public void setAD_Process_Para_ID (int AD_Process_Para_ID)
	{
		super.setAD_Process_Para_ID (AD_Process_Para_ID);
		setAttributeName(null);
	}
	
	@Override
	public MWFNodePara markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		if (m_processPara != null)
			m_processPara.markImmutable();
		return this;
	}

}	//	MWFNodePara
