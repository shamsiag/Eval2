/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 **********************************************************************/
package org.compiere.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProjectClose;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.idempiere.fa.exceptions.AssetAlreadyDepreciatedException;
import org.idempiere.fa.exceptions.AssetException;
import org.idempiere.fa.exceptions.AssetNotImplementedException;
import org.idempiere.fa.exceptions.AssetNotSupportedException;
import org.idempiere.fa.feature.UseLifeImpl;
import org.idempiere.fa.util.POCacheLocal;

/**
 *  Asset Addition Model
 *	@author Teo Sarca, SC ARHIPAC SERVICE SRL
 *
 * TODO: BUG: REG in depexp creates a zero if they have more sites Addition during 0?!
 */
public class MAssetAddition extends X_A_Asset_Addition
	implements DocAction
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 5977180589101094202L;

	/** Static Logger */
	private static CLogger s_log = CLogger.getCLogger(MAssetAddition.class);

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param A_Asset_Addition_UU  UUID key
     * @param trxName Transaction
     */
    public MAssetAddition(Properties ctx, String A_Asset_Addition_UU, String trxName) {
        super(ctx, A_Asset_Addition_UU, trxName);
		if (Util.isEmpty(A_Asset_Addition_UU))
			setInitialDefaults();
    }

    /**
     * @param ctx
     * @param A_Asset_Addition_ID
     * @param trxName
     */
	public MAssetAddition (Properties ctx, int A_Asset_Addition_ID, String trxName)
	{
		super (ctx, A_Asset_Addition_ID, trxName);
		if (A_Asset_Addition_ID == 0)
			setInitialDefaults();
	}	//	MAssetAddition

	/**
	 * Set the initial defaults for a new record
	 */
	private void setInitialDefaults() {
		setDocStatus(DOCSTATUS_Drafted);
		setDocAction(DOCACTION_Complete);
		setProcessed(false);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MAssetAddition (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MAAssetAddition

	@Override
	protected boolean beforeSave (boolean newRecord)
	{
		setA_CreateAsset();
		if (isA_CreateAsset() && getA_QTY_Current().signum() == 0)
		{
			setA_QTY_Current(Env.ONE);
		}
		if (getC_Currency_ID() <= 0)
		{
			setC_Currency_ID(MClient.get(getCtx()).getAcctSchema().getC_Currency_ID());
		}
		if (getC_ConversionType_ID() <= 0)
		{
			setC_ConversionType_ID(MConversionType.getDefault(getAD_Client_ID()));
		}
		getDateAcct();
		setAssetValueAmt();
		if (isA_CreateAsset())
		{
			setA_CapvsExp(A_CAPVSEXP_Capital);
		}
		
		// set approved
		setIsApproved();
		
		return true;
	}	//	beforeSave
	
	/**
	 * Create Asset and asset Addition from MMatchInv.
	 * MAssetAddition is saved.
	 * @param match match invoice
	 * @return asset addition
	 */
	public static MAssetAddition createAsset(MMatchInv match)
	{
		MAssetAddition assetAdd = new MAssetAddition(match);
		assetAdd.dump();
		//@win add condition to prevent asset creation when expense addition or second addition
		if (MAssetAddition.A_CAPVSEXP_Capital.equals(assetAdd.getA_CapvsExp())
				&& match.getC_InvoiceLine().getA_Asset_ID() == 0 && assetAdd.isA_CreateAsset()) { 
		//end @win add condition to prevent asset creation when expense addition or second addition
		MAsset asset = assetAdd.createAsset();
		asset.dump();
		//@win add
		
		} else {
			assetAdd.setA_Asset_ID(match.getC_InvoiceLine().getA_Asset_ID());
			assetAdd.setA_CreateAsset(false);
		}
		assetAdd.saveEx();
		return assetAdd;
	}
	
	/**
	 * Create Asset and asset Addition from MIFixedAsset. MAssetAddition is saved. 
	 * (@win note, not referenced from anywhere. incomplete feature)
	 * @param	ifa
	 * @return asset addition
	 */
	public static MAssetAddition createAsset(MIFixedAsset ifa)
	{
		MAssetAddition assetAdd = new MAssetAddition(ifa);
		assetAdd.dump();
		//@win add condition to prevent asset creation when expense addition or second addition
		if (MAssetAddition.A_CAPVSEXP_Capital.equals(assetAdd.getA_CapvsExp())
				&& ifa.getA_Asset_ID() == 0) { 
		//end @win add condition to prevent asset creation when expense addition or second addition
		MAsset asset = assetAdd.createAsset();
		asset.dump();	
		}
		assetAdd.saveEx();
		return assetAdd;
	}
	
	/**
	 * Create Asset and asset Addition from MProject. MAssetAddition is saved. 
	 * Addition from Project only allows initial addition (will definitely create new asset)
	 * @param	project
	 * @return asset addition
	 */
	public static MAssetAddition createAsset(MProject project, MProduct product)
	{
		MAssetAddition assetAdd = new MAssetAddition(project);
		assetAdd.dump();
		
		MAsset asset = assetAdd.createAsset();
		
		if (product != null) {
			asset.setM_Product_ID(product.getM_Product_ID());
			asset.setA_Asset_Group_ID(product.getA_Asset_Group_ID());
			MAttributeSetInstance asi = MAttributeSetInstance.create(Env.getCtx(), product, null);
			asset.setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
			asset.setName(product.getName().concat(project.getName()));
			asset.setValue(product.getName().concat(project.getName()));
		}	
		asset.saveEx();
		asset.dump();
		
		assetAdd.setA_Asset(asset);
		assetAdd.saveEx();
		//@win add
		
		return assetAdd;
	}

	/**	
	 * Create Asset
	 * @return MAsset
	 */
	private MAsset createAsset()
	{
		MAsset asset = null;
		if (getA_Asset_ID() <= 0)
		{
			String sourceType = getA_SourceType();
			if (A_SOURCETYPE_Invoice.equals(sourceType))
			{
				asset = new MAsset(getMatchInv(false));
				asset.saveEx();
				setA_Asset(asset);
			}
			else if (A_SOURCETYPE_Imported.equals(sourceType))
			{
				asset = new MAsset(getI_FixedAsset(false));
				asset.saveEx();
				setA_Asset(asset);
			}
			else if (A_SOURCETYPE_Project.equals(sourceType))
			{
				asset = new MAsset(getC_Project(false));
			}
			else
			{
				throw new AssetNotSupportedException(COLUMNNAME_A_SourceType, sourceType);
			}
		}
		else
		{
			asset = getA_Asset(false);
		}
		//
		return asset;
	}
	
	/**
	 * Construct addition from match invoice 
	 * @param match	match invoice model
	 */
	private MAssetAddition (MMatchInv match)
	{
		this(match.getCtx(), 0, match.get_TrxName());
		setM_MatchInv(match);
		setC_DocType_ID();
	}
	
	/**
	 * Construct addition from Project
	 * @param project 
	 */
	private MAssetAddition (MProject project)
	{
		this(project.getCtx(), 0, project.get_TrxName());
		if (log.isLoggable(Level.FINEST)) log.finest("Entering: Project=" + project);
		setAD_Org_ID(project.getAD_Org_ID());
		setPostingType(POSTINGTYPE_Actual);
		setA_SourceType(A_SOURCETYPE_Project);
		//
		setC_Currency_ID(project.getC_Currency_ID());
		if (project.get_ValueAsInt("C_ConversionType_ID")>0) {
			setC_ConversionType_ID(project.get_ValueAsInt("C_ConversionType_ID"));
		}
		setSourceAmt(project.getProjectBalanceAmt());
		setDateDoc(new Timestamp (System.currentTimeMillis()));
		setA_CreateAsset(true); //added by @win as create from project will certainly for createnew
	
		setC_DocType_ID();
		
		Timestamp dateAcct = new Timestamp (System.currentTimeMillis());
		if (dateAcct != null)
		{
			dateAcct = UseLifeImpl.getDateAcct(dateAcct, 1);
			if (log.isLoggable(Level.FINE)) log.fine("DateAcct=" + dateAcct);
			setDateAcct(dateAcct);
		}
		setC_Project(project);
	}
	
	private final POCacheLocal<MProject> m_cacheCProject = POCacheLocal.newInstance(this, MProject.class);
	
	/**
	 * @param requery
	 * @return MProject
	 */
	public MProject getC_Project(boolean requery)
	{
		return m_cacheCProject.get(requery);
	}
	
	/**
	 * @param project
	 */
	private void setC_Project(MProject project)
	{
		set_Value("C_Project_ID", project.get_ID());
		m_cacheCProject.set(project);
	}
	
	/**
	 * Construct addition from import
	 * @param ifa	fixed asset import
	 */
	private MAssetAddition (MIFixedAsset ifa)
	{
		this(ifa.getCtx(), 0, ifa.get_TrxName());
		if (log.isLoggable(Level.FINEST)) log.finest("Entering: ifa=" + ifa);
		setAD_Org_ID(ifa.getAD_Org_ID());
		setPostingType(POSTINGTYPE_Actual);
		setA_SourceType(A_SOURCETYPE_Imported);
		//
		setM_Product_ID(ifa.getM_Product_ID());
		setAssetValueAmt(ifa.getA_Asset_Cost());
		setSourceAmt(ifa.getA_Asset_Cost());
		setDateDoc(ifa.getAssetServiceDate());
		setM_Locator_ID(ifa.getM_Locator_ID());
		
	
		
		setA_CapvsExp(MAssetAddition.A_CAPVSEXP_Capital); //added by zuhri, import must be in Capital
		setA_CreateAsset(true); //added by zuhri, import must be create asset
		setA_Salvage_Value(ifa.getA_Salvage_Value());
		setC_DocType_ID();
		
		Timestamp dateAcct = ifa.getDateAcct();
		if (dateAcct != null)
		{
			if (log.isLoggable(Level.FINE)) log.fine("DateAcct=" + dateAcct);
			setDateAcct(dateAcct);
		}
		if (ifa.getA_Asset_ID() > 0)
			setA_Asset_ID(ifa.getA_Asset_ID());
		if (ifa.getC_Currency_ID() > 0)
			setC_Currency_ID(ifa.getC_Currency_ID());
		setAssetAmtEntered(ifa.getAssetAmtEntered());
		setAssetSourceAmt(ifa.getAssetSourceAmt());
		
		setI_FixedAsset(ifa);
	}
	
	/** Match Invoice Cache */
	private final POCacheLocal<MMatchInv> m_cacheMatchInv = POCacheLocal.newInstance(this, MMatchInv.class);

	/**
	 * @param requery
	 * @return MMatchInv
	 */
	private MMatchInv getMatchInv(boolean requery)
	{
		return m_cacheMatchInv.get(requery);
	}
	
	/**
	 * @param mi
	 */
	private void setM_MatchInv(MMatchInv mi)
	{
		mi.load(get_TrxName());
		setAD_Org_ID(mi.getAD_Org_ID());
		setPostingType(POSTINGTYPE_Actual);
		setA_SourceType(A_SOURCETYPE_Invoice);
		setM_MatchInv_ID(mi.get_ID());
		
		if (MAssetAddition.A_CAPVSEXP_Capital.equals(mi.getC_InvoiceLine().getA_CapvsExp())
					&& mi.getC_InvoiceLine().getA_Asset_ID() == 0) {
			setA_CreateAsset(true);
		}
		 
		setC_Invoice_ID(mi.getC_InvoiceLine().getC_Invoice_ID());
		setC_InvoiceLine_ID(mi.getC_InvoiceLine_ID());
		setM_InOutLine_ID(mi.getM_InOutLine_ID());
		setM_Product_ID(mi.getM_Product_ID());
		setM_AttributeSetInstance_ID(mi.getM_AttributeSetInstance_ID());
		setA_QTY_Current(mi.getQty());
		setLine(mi.getC_InvoiceLine().getLine());
		setM_Locator_ID(mi.getM_InOutLine().getM_Locator_ID());
		setA_CapvsExp(mi.getC_InvoiceLine().getA_CapvsExp());
		setAssetAmtEntered(mi.getC_InvoiceLine().getLineNetAmt());
		setAssetSourceAmt(mi.getC_InvoiceLine().getLineNetAmt());
		setC_Currency_ID(mi.getC_InvoiceLine().getC_Invoice().getC_Currency_ID());
		setC_ConversionType_ID(mi.getC_InvoiceLine().getC_Invoice().getC_ConversionType_ID());
		setDateDoc(mi.getM_InOutLine().getM_InOut().getMovementDate());
		setDateAcct(mi.getDateAcct());
		setAD_Org_ID(mi.getAD_Org_ID());
		m_cacheMatchInv.set(mi);
	}
	
	/**
	 * Copy fields from MatchInv+InvoiceLine+InOutLine
	 * @param model - to copy from
	 * @param M_MatchInv_ID - matching invoice id
	 */
	public static boolean setM_MatchInv(SetGetModel model, int M_MatchInv_ID)
	{
		boolean newRecord = false;
		String trxName = null;
		if (model instanceof PO)
		{
			PO po = (PO)model;
			newRecord = po.is_new();
			trxName = po.get_TrxName();
			
		}
		
		if (s_log.isLoggable(Level.FINE)) s_log.fine("Entering: model=" + model + ", M_MatchInv_ID=" + M_MatchInv_ID + ", newRecord=" + newRecord + ", trxName=" + trxName);
		
		final String qMatchInv_select = "SELECT"
				+ "  C_Invoice_ID"
				+ ", C_InvoiceLine_ID"
				+ ", M_InOutLine_ID"
				+ ", M_Product_ID"
				+ ", M_AttributeSetInstance_ID"
				+ ", Qty AS "+COLUMNNAME_A_QTY_Current
				+ ", InvoiceLine AS "+COLUMNNAME_Line
				+ ", M_Locator_ID"
				+ ", A_CapVsExp"
				+ ", MatchNetAmt AS "+COLUMNNAME_AssetAmtEntered
				+ ", MatchNetAmt AS "+COLUMNNAME_AssetSourceAmt
				+ ", C_Currency_ID"
				+ ", C_ConversionType_ID"
				+ ", MovementDate AS "+COLUMNNAME_DateDoc
		;
		final String qMatchInv_from = " FROM mb_matchinv WHERE M_MatchInv_ID="; //@win change M_MatchInv_ARH to M_MatchInv
		
		String query = qMatchInv_select;
		if (newRecord) {
			query += ", A_Asset_ID, A_CreateAsset";
		}
		query += qMatchInv_from + M_MatchInv_ID;
		
		SetGetUtil.updateColumns(model, null, query, trxName);
		
		return true;
	}
	
	private final POCacheLocal<MIFixedAsset> m_cacheIFixedAsset = POCacheLocal.newInstance(this, MIFixedAsset.class);
	
	/**
	 * @param requery
	 * @return MIFixedAsset
	 */
	public MIFixedAsset getI_FixedAsset(boolean requery)
	{
		return m_cacheIFixedAsset.get(requery);
	}
	
	/**
	 * Set fixed asset import model
	 * @param ifa
	 */
	private void setI_FixedAsset(MIFixedAsset ifa)
	{
		setI_FixedAsset_ID(ifa.get_ID());
		m_cacheIFixedAsset.set(ifa);
	}
	
	/**
	 *	Set AssetValueAmt from AssetSourceAmt using C_Currency_ID and C_ConversionRate_ID
	 */
	private void setAssetValueAmt()
	{
		if (A_SOURCETYPE_Imported.equals(getA_SourceType()))
			return;
		getDateAcct();
		MConversionRateUtil.convertBase(SetGetUtil.wrap(this),
				COLUMNNAME_DateAcct,
				COLUMNNAME_AssetSourceAmt,
				COLUMNNAME_AssetValueAmt,
				null);
	}
	
	/**
	 * Set source amount
	 * @param amt
	 */
	public void setSourceAmt(BigDecimal amt)
	{
		setAssetAmtEntered(amt);
		setAssetSourceAmt(amt);
	}
	
	/**
	 * Set IsApproved to true if current role can approved it, false otherwise.
	 */
	public void setIsApproved()
	{
		if(!isProcessed())
		{
			boolean isApproved = MRole.getDefault().isCanApproveOwnDoc();
			if (log.isLoggable(Level.FINE)) log.fine("IsCanApproveOwnDoc=" + isApproved);
			setIsApproved(isApproved);
		}
	}
	
	/**
	 * Set DateAcct to DateDoc if is still null
	 */
	@Override
	public Timestamp getDateAcct()
	{
		Timestamp dateAcct = super.getDateAcct();
		if (dateAcct == null) {
			dateAcct = getDateDoc();
			setDateAcct(dateAcct);
		}
		return dateAcct;
	}
	
	@Override
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;

	@Override
	public boolean unlockIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("unlockIt - " + toString());
		return true;
	}	//	unlockIt
	
	@Override
	public boolean invalidateIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("invalidateIt - " + toString());
		return false;
	}	//	invalidateIt
	
	@Override
	public String prepareIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		
		// Call model validators
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}
		
		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), MDocType.DOCBASETYPE_GLJournal, getAD_Org_ID());
		
		// Check AssetValueAmt != 0
		if (getAssetValueAmt().signum() == 0) {
			m_processMsg="@Invalid@ @AssetValueAmt@=0";
			return DocAction.STATUS_Invalid;
		}
		
		MAsset asset = getA_Asset(true);
		
		// If new assets (not renewals) must have nonzero values
		if (isA_CreateAsset() && hasZeroValues())
		{
			throw new AssetException("New document has nulls");
		}

		// Only New assets can be activated
		if (isA_CreateAsset() && !MAsset.A_ASSET_STATUS_New.equals(asset.getA_Asset_Status()))
		{
			if (!A_SOURCETYPE_Imported.equals(getA_SourceType()))
				throw new AssetException("Only new assets can be activated");
		}
		//
		// Validate Source - Project
		if (A_SOURCETYPE_Project.equals(getA_SourceType()))
		{
			if (getC_Project_ID() <= 0)
			{
				throw new FillMandatoryException(COLUMNNAME_C_Project_ID);
			}
			final String whereClause = COLUMNNAME_C_Project_ID+"=?"
								+" AND DocStatus IN ('IP','CO','CL')"
								+" AND "+COLUMNNAME_A_Asset_Addition_ID+"<>?";
			List<MAssetAddition> list = new Query(getCtx(), Table_Name, whereClause, get_TrxName())
					.setParameters(new Object[]{getC_Project_ID(), get_ID()})
					.list();
			if (list.size() > 0)
			{
				StringBuilder sb = new StringBuilder("You can not create project for this asset,"
									+" Project already has assets. View: ");
				for (MAssetAddition aa : list)
				{
					sb.append(aa.getDocumentInfo()).append("; ");
				}
				throw new AssetException(sb.toString());
			}
		}

		// Validate Source - Invoice
		if (A_SOURCETYPE_Invoice.equals(getA_SourceType()))
		{
			if (getC_Invoice_ID() <= 0)
			{
				throw new FillMandatoryException(COLUMNNAME_C_Invoice_ID);
			}
		}

		// Call model validators
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}

		
		//	Done
		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	@Override
	public boolean approveIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("approveIt - " + toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	@Override
	public boolean rejectIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("rejectIt - " + toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt
	
	@Override
	public String completeIt() 
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			m_justPrepared = false;
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
		//	User Validation
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null) {
			return DocAction.STATUS_Invalid;
		}
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		if (log.isLoggable(Level.INFO)) log.info(toString());
		//
		
		// Check/Create ASI:
		checkCreateASI();
		
		//loading asset
		MAsset asset = getA_Asset(!m_justPrepared); // requery if not just prepared
		if (log.isLoggable(Level.FINE)) log.fine("asset=" + asset);
		

		// Setting locator if is CreateAsset
		if (isA_CreateAsset() && getM_Locator_ID() > 0)
		{
			asset.setM_Locator_ID(getM_Locator_ID());
		}
		
		// Creating/Updating asset product
		updateA_Asset_Product(false);
		//
		// Changing asset status to Activated or Depreciated
		if (isA_CreateAsset())
		{
			asset.setAssetServiceDate(getDateDoc());
		}
		asset.changeStatus(MAsset.A_ASSET_STATUS_Activated, getDateAcct());
		asset.saveEx();

		//
		// Get/Create Asset Workfile:
		// If there Worksheet creates a new file in this asset
		MDepreciationWorkfile assetwk = MDepreciationWorkfile.get(getCtx(), getA_Asset_ID(), getPostingType(), get_TrxName());
		if (assetwk == null)
		{
			for (MAssetGroupAcct assetgrpacct :  MAssetGroupAcct.forA_Asset_Group_ID(getCtx(), asset.getA_Asset_Group_ID(), getPostingType(), get_TrxName()))
			{
				if (A_SOURCETYPE_Imported.equals(getA_SourceType()) && assetgrpacct.getC_AcctSchema_ID() != getI_FixedAsset().getC_AcctSchema_ID())
					continue;
				assetwk = new MDepreciationWorkfile(asset, getPostingType(), assetgrpacct);
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine("workfile: " + assetwk);

		for (MDepreciationWorkfile assetworkFile :  MDepreciationWorkfile.forA_Asset_ID(getCtx(), getA_Asset_ID(), get_TrxName()))
		{
			if (A_SOURCETYPE_Imported.equals(getA_SourceType()) && assetworkFile.getC_AcctSchema_ID() != getI_FixedAsset().getC_AcctSchema_ID())
				continue;
			
			assetworkFile.setDateAcct(getDateAcct());
			if (A_SOURCETYPE_Imported.equals(getA_SourceType())) {
				assetworkFile.adjustCost(getI_FixedAsset().getA_Asset_Cost(), getA_QTY_Current(), isA_CreateAsset());
			} else {
				if (assetworkFile.getC_AcctSchema().getC_Currency_ID() != getC_Currency_ID()) 
				{				
					BigDecimal convertedAssetCost  =  MConversionRate.convert(getCtx(), getAssetSourceAmt(),
							getC_Currency_ID(), assetworkFile.getC_AcctSchema().getC_Currency_ID() ,
							getDateAcct(), getC_ConversionType_ID(),
							getAD_Client_ID(), getAD_Org_ID());
					assetworkFile.adjustCost(convertedAssetCost, getA_QTY_Current(), isA_CreateAsset()); // reset if isA_CreateAsset
				} else {
					assetworkFile.adjustCost(getAssetSourceAmt(), getA_QTY_Current(), isA_CreateAsset()); // reset if isA_CreateAsset
				}				
			}
			// Do we have entries that are not processed and before this date:
			if (this.getA_CapvsExp().equals(A_CAPVSEXP_Capital)) { 
			//@win modification to asset value and use life should be restricted to Capital
			MDepreciationExp.checkExistsNotProcessedEntries(assetworkFile.getCtx(), assetworkFile.getA_Asset_ID(), getDateAcct(), assetworkFile.getPostingType(), assetworkFile.get_TrxName());
			//
			if (this.getA_Salvage_Value().signum() > 0) {
				if (A_SOURCETYPE_Imported.equals(getA_SourceType())) {
					assetworkFile.setA_Salvage_Value(this.getA_Salvage_Value());
				} else {
					if (assetworkFile.getC_AcctSchema().getC_Currency_ID() != getC_Currency_ID()) 
					{
						BigDecimal salvageValue = MConversionRate.convert(getCtx(), this.getA_Salvage_Value(),
								getC_Currency_ID(), assetworkFile.getC_AcctSchema().getC_Currency_ID() ,
								getDateAcct(), getC_ConversionType_ID(),
								getAD_Client_ID(), getAD_Org_ID());
						assetworkFile.setA_Salvage_Value(salvageValue);
					} else{
						assetworkFile.setA_Salvage_Value(this.getA_Salvage_Value());
					}
				}
			}
			assetworkFile.setDateAcct(getDateAcct());
			assetworkFile.setProcessed(true);
			assetworkFile.saveEx();
			}
			//@win set initial depreciation period = 1 
			if (isA_CreateAsset())
			{
				if (assetworkFile.getA_Current_Period() == 0)
				{
					assetworkFile.setA_Current_Period(1);
					assetworkFile.saveEx();
				}
			}
			//
			// Rebuild depreciation:
			assetworkFile.buildDepreciation();
		}		
		
		MAssetChange.createAddition(this, assetwk);
		
		//
		updateSourceDocument(false);
		
		// finish
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		//
		//	User Validation
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (m_processMsg != null) {
			return DocAction.STATUS_Invalid;
		}
		//
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	@Override
	public boolean voidIt()
	{
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;
		
		reverseIt(false);

		//	User Validation
		String errmsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);
		if (errmsg != null)
		{
			m_processMsg = errmsg;
			return false;
		}
		
		// finish
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}	//	voidIt
	
	/**
	 * @param isReActivate
	 */
	private void reverseIt(boolean isReActivate)
	{
		if (DOCSTATUS_Closed.equals(getDocStatus())
				|| DOCSTATUS_Reversed.equals(getDocStatus())
				|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			setDocAction(DOCACTION_None);
			throw new AssetException("Document Closed: " + getDocStatus());
		}

		// Handling Workfile
		MDepreciationWorkfile assetwk = MDepreciationWorkfile.get(getCtx(), getA_Asset_ID(), getPostingType(), get_TrxName());
		if (assetwk == null)
		{
			throw new AssetException("@NotFound@ @A_DepreciationWorkfile_ID");
		}
		
		// TODO: Check if there are Additions after this one
		
		if (assetwk.isFullyDepreciated())
		{
			throw new AssetNotImplementedException("Unable to verify if it is fully depreciated");
		}
		
		// cannot update a previous period
		if (!isA_CreateAsset() && assetwk.isDepreciated(getDateAcct()))
		{
			throw new AssetAlreadyDepreciatedException();
		}
		
		// adjust the asset value
		assetwk.adjustCost(getAssetValueAmt().negate(), getA_QTY_Current().negate(), false);
		assetwk.saveEx();
		
		//
		// Delete Expense Entries that were created by this addition
		{
			final String whereClause = MDepreciationExp.COLUMNNAME_A_Asset_Addition_ID+"=?"
									+" AND "+MDepreciationExp.COLUMNNAME_PostingType+"=?";
			List<MDepreciationExp>
			list = new Query(getCtx(), MDepreciationExp.Table_Name, whereClause, get_TrxName())
						.setParameters(new Object[]{get_ID(), assetwk.getPostingType()})
						.setOrderBy(MDepreciationExp.COLUMNNAME_DateAcct+" DESC, "+MDepreciationExp.COLUMNNAME_A_Depreciation_Exp_ID+" DESC")
						.list();
			for (MDepreciationExp depexp: list)
			{
				depexp.deleteEx(true);
			}
		}
		//
		// Update/Delete working file (after all entries were deleted)
		if (isA_CreateAsset())
		{
			assetwk.deleteEx(true);
		}
		else
		{
			assetwk.setA_Current_Period();
			assetwk.saveEx();
			assetwk.buildDepreciation();
		}
		
		// Creating/Updating asset product
		updateA_Asset_Product(true);
		
		// Change Asset Status
		if (isA_CreateAsset())
		{
			MAsset asset = getA_Asset(true);
			asset.changeStatus(MAsset.A_ASSET_STATUS_New, getDateAcct());
			asset.saveEx();
			
			
			if (!isReActivate)
			{
				setA_CreateAsset(false); // reset flag
			}
		}
		
		MFactAcct.deleteEx(get_Table_ID(), get_ID(), get_TrxName());
    
		updateSourceDocument(true);
	}
	
	@Override
	public boolean closeIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("closeIt - " + toString());
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt
	
	@Override
	public boolean reverseCorrectIt()
	{
		throw new AssetNotImplementedException("reverseCorrectIt");
	}	//	reverseCorrectionIt
	
	@Override
	public boolean reverseAccrualIt()
	{
		throw new AssetNotImplementedException("reverseAccrualIt");
	}	//	reverseAccrualIt
	
	@Override
	public boolean reActivateIt()
	{
		// Before
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;
		
		reverseIt(true);

		//	User Validation
		String errmsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);
		if (errmsg != null) {
			m_processMsg = errmsg;
			return false;
		}
		
		// finish
		setProcessed(false);
		setDocAction(DOCACTION_Complete);
		return true;
	}	//	reActivateIt
	
	@Override
	public String getSummary()
	{
		MAsset asset = getA_Asset(false);
		StringBuilder sb = new StringBuilder();
		sb.append("@DocumentNo@ #").append(getDocumentNo())
			.append(": @A_CreateAsset@=@").append(isA_CreateAsset() ? "Y" : "N").append("@")
		;
		if (asset != null)
		{
			sb.append(", @A_Asset_ID@=").append(asset.getName());
		}
		
		return Msg.parseTranslation(getCtx(), sb.toString());
	}	//	getSummary

	@Override
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	@Override
	public int getDoc_User_ID()
	{
		return getCreatedBy();
	}	//	getDoc_User_ID

	@Override
	public BigDecimal getApprovalAmt()
	{
		return getAssetValueAmt();
	}	//	getApprovalAmt
	
	
	/** Asset Cache */
	private final POCacheLocal<MAsset> m_cacheAsset = POCacheLocal.newInstance(this, MAsset.class);
	
	/**
	 * Get Asset 
	 * @param requery
	 * @return asset
	 */
	public MAsset getA_Asset(boolean requery)
	{
		return m_cacheAsset.get(requery);
	}
	
	/**
	 * Set Asset 
	 * @return asset
	 */
	private void setA_Asset(MAsset asset)
	{
		setA_Asset_ID(asset.getA_Asset_ID());
		m_cacheAsset.set(asset);
	} // setAsset
	
	@Override
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if(!success)
		{
			return false;
		}
		updateSourceDocument(false);
		return true;
	}	//	afterSave

	/**
	 * Update Source Document (Invoice, Project etc) Status
	 * @param isReversal is called from a reversal action (like Void, Reverse-Correct).
	 * 					We need this flag because that when we call the method from voidIt()
	 * 					the document is not marked as voided yet. Same thing applies for reverseCorrectIt too. 
	 */
	private void updateSourceDocument(final boolean isReversalParam)
	{
		boolean isReversal = isReversalParam;
		// Check if this document is reversed/voided
		String docStatus = getDocStatus();
		if (!isReversal && (DOCSTATUS_Reversed.equals(docStatus) || DOCSTATUS_Voided.equals(docStatus)))
		{
			isReversal = true;
		}
		final String sourceType = getA_SourceType();
		//
		// Invoice: mark C_InvoiceLine.A_Processed='Y' and set C_InvoiceLine.A_Asset_ID
		if (A_SOURCETYPE_Invoice.equals(sourceType) && isProcessed())
		{
			int C_InvoiceLine_ID = getC_InvoiceLine_ID();
			if (C_InvoiceLine_ID == 0)
				throw new AdempiereException("No Invoice Line");
			MInvoiceLine invoiceLine = new MInvoiceLine(getCtx(), C_InvoiceLine_ID, get_TrxName());
			invoiceLine.setA_Processed(!isReversal);
			invoiceLine.setA_Asset_ID(isReversal ? 0 : getA_Asset_ID());
			invoiceLine.saveEx();
		}
		//
		// Project
		else if (A_SOURCETYPE_Project.equals(sourceType) && isProcessed())
		{
			if (isReversal)
			{
				// Project remains closed. We just void/reverse/reactivate the Addition
			}
			else
			{
				//TODO decide whether to close project first or later
				
				int project_id = getC_Project_ID();
				ProcessInfo pi = new ProcessInfo("", 0, MProject.Table_ID, project_id);
				pi.setAD_Client_ID(getAD_Client_ID());
				pi.setAD_User_ID(Env.getAD_User_ID(getCtx()));
				//
				ProjectClose proc = new ProjectClose();
				proc.startProcess(getCtx(), pi, Trx.get(get_TrxName(), false));
				if (pi.isError())
				{
					throw new AssetException(pi.getSummary());
				}
				
			}
		}
		//
		// Import
		else if (A_SOURCETYPE_Imported.equals(sourceType) && !isProcessed())
		{
			if (is_new() && getI_FixedAsset_ID() > 0)
			{
				MIFixedAsset ifa = getI_FixedAsset(false);
				if (ifa != null)
				{
					ifa.setI_IsImported(true);
					ifa.setA_Asset_ID(getA_Asset_ID());
					ifa.saveEx(get_TrxName());
				}
			}
		}
		//
		// Manual
		else if (A_SOURCETYPE_Manual.equals(sourceType) && isProcessed())
		{
			// nothing to do
			if (log.isLoggable(Level.FINE))
				log.fine("Nothing to do");
		}
	}
	
	/**
	 * Check/Create ASI for Product (if any). If there is no product, no ASI will be created
	 */
	private void checkCreateASI() 
	{
		MProduct product = MProduct.get(getCtx(), getM_Product_ID());
		// Check/Create ASI:
		MAttributeSetInstance asi = null;
		if (product != null && getM_AttributeSetInstance_ID() == 0)
		{
			asi = new MAttributeSetInstance(getCtx(), 0, get_TrxName());
			asi.setAD_Org_ID(0);
			asi.setM_AttributeSet_ID(product.getM_AttributeSet_ID());
			asi.saveEx();
			setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
		}
	}
	
	/**
	 * Creating/Updating asset product (MAssetProduct)
	 * @param isReversal
	 */
	private void updateA_Asset_Product(boolean isReversal)
	{
		// Skip if no product
		if (getM_Product_ID() <= 0)
		{
			return;
		}
		//
		MAssetProduct assetProduct = MAssetProduct.getCreate(getCtx(),
										getA_Asset_ID(), getM_Product_ID(), getM_AttributeSetInstance_ID(),
										get_TrxName());
		//
		if (assetProduct.get_ID() <= 0 && isReversal)
		{
			log.warning("No Product found "+this+" [IGNORE]");
			return;
		}
		//
		BigDecimal adjQty = getA_QTY_Current();
		
		if (isReversal)
		{
			adjQty = adjQty.negate();
		}
		//
		assetProduct.addA_Qty_Current(getA_QTY_Current());
		assetProduct.setAD_Org_ID(getA_Asset().getAD_Org_ID()); 
		assetProduct.saveEx();
		if (isA_CreateAsset())
		{
			MAsset asset = getA_Asset(false);
			assetProduct.updateAsset(asset);
			asset.saveEx();
		}
	}
	
	/**
	 * @return true if assert value &lt;= 0
	 */
	public boolean hasZeroValues()
	{
		return				
				 getAssetValueAmt().signum() <= 0
		;
	}

	@Override
	public File createPDF ()
	{
		return null;
	}	//	createPDF
	
	@Override
	public String getDocumentInfo()
	{
		return getDocumentNo() + " / " + getDateDoc();
	}	//	getDocumentInfo

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("@DocumentNo@: " + getDocumentNo());
		MAsset asset = getA_Asset(false);
		if(asset != null && asset.get_ID() > 0)
		{
			sb.append(", @A_Asset_ID@: ").append(asset.getName());
		}
		return sb.toString();
	}	// toString
	
	/**
	 * Update A_CreateAsset flag
	 */
	private void setA_CreateAsset()
	{
		if (DOCSTATUS_Voided.equals(getDocStatus()))
		{
			setA_CreateAsset(false);
		}
		else if (A_SOURCETYPE_Imported.equals(getA_SourceType()))
		{
			setA_CreateAsset(true);
		}
		else
		{
			final String sql = "SELECT COUNT(*) FROM A_Asset_Addition WHERE A_Asset_ID=? AND A_CreateAsset='Y'"
							+" AND DocStatus<>'VO' AND IsActive='Y'"
							+" AND A_Asset_Addition_ID<>?";
			int cnt = DB.getSQLValueEx(null, sql, getA_Asset_ID(), getA_Asset_Addition_ID());
			if(isA_CreateAsset())
			{
				// A_CreateAsset='Y' must be unique
				if (cnt >= 1)
				{
					setA_CreateAsset(false);
				}
			}
			else
			{
				// Successful creation of Asset
				if (cnt == 0)
				{
					setA_CreateAsset(true);
				}
			}
		}
	}
	
	/**
	 * Set C_DocType_ID value by DocBaseType (FAA)
	 */
	private void setC_DocType_ID() 
	{
		StringBuilder sql = new StringBuilder ("SELECT C_DocType_ID FROM C_DocType ")
			.append( "WHERE AD_Client_ID=? AND AD_Org_ID IN (0,").append( getAD_Org_ID())
			.append( ") AND DocBaseType='FAA' ")
			.append( "ORDER BY AD_Org_ID DESC, IsDefault DESC");
		int C_DocType_ID = DB.getSQLValue(null, sql.toString(), getAD_Client_ID());
		if (C_DocType_ID <= 0)
			log.severe ("No FAA found for AD_Client_ID=" + getAD_Client_ID ());
		else
		{
			if (log.isLoggable(Level.FINE)) log.fine("(PO) - " + C_DocType_ID);
			setC_DocType_ID (C_DocType_ID);
		}
	
	}	
}	//	MAssetAddition
