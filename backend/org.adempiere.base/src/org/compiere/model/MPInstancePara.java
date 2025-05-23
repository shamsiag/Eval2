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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Properties;

/**
 *  Process Instance Parameter Model
 *
 *  @author Jorg Janke
 *  @version $Id: MPInstancePara.java,v 1.3 2006/07/30 00:58:05 jjanke Exp $
 */
public class MPInstancePara extends X_AD_PInstance_Para
{	
	/**
	 * generated serial id 
	 */
	private static final long serialVersionUID = -8407658637240252680L;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_PInstance_Para_UU  UUID key
     * @param trxName Transaction
     */
    public MPInstancePara(Properties ctx, String AD_PInstance_Para_UU, String trxName) {
        super(ctx, AD_PInstance_Para_UU, trxName);
    }

	/**
	 *	@param ctx context
	 *	@param ignored ignored
	 *	@param trxName transaction
	 */
	public MPInstancePara (Properties ctx, int ignored, String trxName)
	{
		super(ctx, 0, trxName);
		if (ignored != 0)
			throw new IllegalArgumentException("Multi-Key");
	}	//	MPInstance_Para
	
	/**
	 * 	Parent Constructor
	 *	@param ctx
	 *	@param AD_PInstance_ID id
	 *	@param SeqNo sequence
	 */
	public MPInstancePara (Properties ctx, int AD_PInstance_ID, int SeqNo)
	{
		super(ctx, 0, null);
		setAD_PInstance_ID (AD_PInstance_ID);
		setSeqNo (SeqNo);
	}	//	MPInstance_Para

	/**
	 * 	Parent Constructor
	 *	@param instance instance
	 *	@param SeqNo sequence
	 */
	public MPInstancePara (MPInstance instance, int SeqNo)
	{
		super (instance.getCtx(), 0, instance.get_TrxName());
		setAD_PInstance_ID (instance.getAD_PInstance_ID());
		setSeqNo (SeqNo);
	}	//	MPInstance_Para

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MPInstancePara (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MPInstance_Para

	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ("MPInstancePara[")
			.append (get_ID ()).append("-").append(getParameterName());
		if (getP_String() != null)
		{
			sb.append("(s)=").append(getP_String());
			if (getP_String_To() != null)
				sb.append(" - ").append(getP_String_To());
		}
		BigDecimal bd = (BigDecimal)get_Value("P_Number");
		if (bd != null)
		{
			sb.append("(n)=").append(bd);
			BigDecimal bd2 = (BigDecimal)get_Value("P_Number_To");
			if (bd2 != null)
				sb.append(" - ").append(bd2);
		}
		if (getP_Date() != null)
		{
			sb.append("(d)=").append(getP_Date());
			if (getP_Date_To() != null)
				sb.append(" - ").append(getP_Date_To());
		}
		sb.append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	Set P_Number
	 *	@param P_Number no
	 */
	public void setP_Number (int P_Number)
	{
		setP_Number (new BigDecimal(P_Number));
	}	//	setP_Number

	/**
	 * 	Set P_Number
	 *	@param P_Number no
	 */
	public void setP_Number (Integer P_Number)
	{
		if (P_Number == null)
			setP_Number(0);
		else
			setP_Number (((Integer)P_Number).intValue());
	}	//	setP_Number
	
	/**
	 * 	Set P_Number To
	 *	@param P_Number_To no to
	 */
	public void setP_Number_To (int P_Number_To)
	{
		setP_Number_To (new BigDecimal(P_Number_To));
	}	//	setP_Number_To

	/**
	 * 	Set P_Number To
	 *	@param P_Number_To no to
	 */
	public void setP_Number_To (Integer P_Number_To)
	{
		if (P_Number_To == null)
			setP_Number_To(0);
		else
			setP_Number_To (((Integer)P_Number_To).intValue());
	}	//	setP_Number_To

	
	/**
	 * 	Set String Parameter
	 *	@param parameterName name
	 *	@param stringParameter value
	 */
	public void setParameter (String parameterName, String stringParameter)
	{
		setParameterName(parameterName);
		setP_String(stringParameter);
	}	//	setParameter
	
	/**
	 * 	Set Number Parameter
	 *	@param parameterName name
	 *	@param bdParameter value
	 */
	public void setParameter (String parameterName, BigDecimal bdParameter)
	{
		setParameterName(parameterName);
		setP_Number(bdParameter);
	}	//	setParameter
	
	/**
	 * 	Set Number Parameter
	 *	@param parameterName name
	 *	@param iParameter value
	 */
	public void setParameter (String parameterName, int iParameter)
	{
		setParameterName(parameterName);
		setP_Number(new BigDecimal(iParameter));
	}	//	setParameter
	
	/**
	 * 	Set Date Parameter
	 *	@param parameterName name
	 *	@param tsParameter value
	 */
	public void setParameter (String parameterName, Timestamp tsParameter)
	{
		setParameterName(parameterName);
		setP_Date(tsParameter);
	}	//	setParameter
	
	/**
	 * 	Set Boolean Parameter
	 *	@param parameterName name
	 *	@param boolParameter value
	 */
	public void setParameter (String parameterName, boolean boolParameter)
	{
		setParameterName(parameterName);
		setP_String(boolParameter ? "Y" : "N");
	}	//	setParameter

	/**
	 * @return Display type
	 */
	public int getDisplayType() 
	{
		MProcess process = (MProcess) getAD_PInstance().getAD_Process();
		MProcessPara[] params = process.getParameters();
		for(MProcessPara param : params)
		{
			if (param.getColumnName().equals(getParameterName()))
			{
				return param.getAD_Reference_ID();
			}
		}
		return -1;
	}
	
	/**
	 * @param param
	 * @return true if param is equal to this instance parameter record
	 */
	public boolean equalParameter(MPInstancePara param) {
		
		if (param == null)
			return false;

		String str = getP_String() == null ? "" : getP_String();
		String strParam = param.getP_String() == null ? "" : param.getP_String();
		String str2 = getP_String_To() == null ? "" : getP_String_To();
		String str2Param = param.getP_String_To() == null ? "" : param.getP_String_To();
		if (!Objects.equals(str, strParam) || !Objects.equals(str2, str2Param))
			return false;

		BigDecimal bd = getP_Number();
		BigDecimal bdParam = param.getP_Number();
		BigDecimal bd2 = getP_Number_To();
		BigDecimal bd2Param = param.getP_Number_To();
		if (!Objects.equals(bd, bdParam) || !Objects.equals(bd2, bd2Param))
			return false;

		Timestamp ts = getP_Date();
		Timestamp tsParam = param.getP_Date();
		Timestamp ts2 = getP_Date_To();
		Timestamp ts2Param = param.getP_Date_To();
		if (!Objects.equals(ts, tsParam) || !Objects.equals(ts2, ts2Param))
			return false;

		return true;
	}
	
	/**
	 * Get existing AD_PInstance_Para record or create a new one if not found
	 * @param ctx
	 * @param AD_PInstance_ID
	 * @param SeqNo
	 * @return MPInstancePara
	 */
	public static MPInstancePara getOrCreate(Properties ctx, int AD_PInstance_ID, int SeqNo)
	{
		Query query = new Query(ctx, Table_Name, "AD_PInstance_ID=? AND SeqNo=?", null);
		MPInstancePara para = query.setParameters(AD_PInstance_ID, SeqNo).first();
		if (para == null)
			para = new MPInstancePara(ctx, AD_PInstance_ID, SeqNo);
		return para;
	}
}	//	MPInstance_Para
