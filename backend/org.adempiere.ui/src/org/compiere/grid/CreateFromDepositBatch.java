/******************************************************************************
 * Copyright (C) 2013 Elaine Tan                                              *
 * Copyright (C) 2013 Trek Global
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.compiere.grid;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Vector;
import java.util.logging.Level;

import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.MDepositBatch;
import org.compiere.model.MDepositBatchLine;
import org.compiere.model.MOrg;
import org.compiere.model.MPayment;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;

/**
 * Create C_DepositBatchLine for C_DepositBatch from C_Payment
 * @author Elaine
 *
 */
public abstract class CreateFromDepositBatch extends CreateFromBatch 
{
	
	// AD_Org_ID
	protected int AD_Org_ID = 0;
	/** Window No */
	protected int p_WindowNo;
	
	/**
	 * 
	 * @param mTab
	 */
	public CreateFromDepositBatch(GridTab mTab) 
	{
		super(mTab);
		if (log.isLoggable(Level.INFO)) log.info(mTab.toString());
	}

	@Override
	protected boolean dynInit() throws Exception
	{
		if (log.isLoggable(Level.CONFIG)) log.config("");
		setTitle(Msg.getElement(Env.getCtx(), "C_DepositBatch_ID") + " .. " + Msg.translate(Env.getCtx(), "CreateFrom"));

		//  Set AD_Org_ID
		AD_Org_ID = Env.getContextAsInt(Env.getCtx(), p_WindowNo, MOrg.COLUMNNAME_AD_Org_ID);
			
		return true;
	}
	
	/**
	 * @return transaction records (selection,datetrx,[c_payment_id,documentno],[c_currency_id,iso_code],payamt,converted amt,bp name)
	 */
	@Override
	protected Vector<Vector<Object>> getBankAccountData(Integer BankAccount, Integer BPartner, String DocumentNo, 
			Timestamp DateFrom, Timestamp DateTo, BigDecimal AmtFrom, BigDecimal AmtTo, Integer DocType, String TenderType, String AuthCode, Integer Currency)
	{
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT p.DateTrx,p.C_Payment_ID,p.DocumentNo, p.C_Currency_ID,c.ISO_Code, p.PayAmt,");
		sql.append("currencyConvertPayment(p.C_Payment_ID,ba.C_Currency_ID), bp.Name ");
		sql.append("FROM C_BankAccount ba");
		sql.append(" INNER JOIN C_Payment_v p ON (p.C_BankAccount_ID=ba.C_BankAccount_ID)");
		sql.append(" INNER JOIN C_Currency c ON (p.C_Currency_ID=c.C_Currency_ID)");
		sql.append(" INNER JOIN C_Payment py ON (py.C_Payment_ID=p.C_Payment_ID)");
		sql.append(" LEFT OUTER JOIN C_BPartner bp ON (p.C_BPartner_ID=bp.C_BPartner_ID) ");
		sql.append(getSQLWhere(BPartner, DocumentNo, DateFrom, DateTo, AmtFrom, AmtTo, DocType, TenderType, AuthCode, Currency, AD_Org_ID));
		
		sql.append(" AND py.IsReconciled = 'N'");
		sql.append(" AND p.DocStatus IN ('CO','CL') AND p.PayAmt<>0");
		sql.append(" AND py.TrxType <> 'X'");
		sql.append(" AND (py.C_DepositBatch_ID = 0 OR py.C_DepositBatch_ID IS NULL)");
		sql.append(" AND NOT EXISTS (SELECT 1 FROM C_BankStatementLine l WHERE p.C_Payment_ID=l.C_Payment_ID AND l.StmtAmt <> 0)");
		
		sql.append(" ORDER BY p.DateTrx");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), getTrxName());
			setParameters(pstmt, BankAccount, BPartner, DocumentNo, DateFrom, DateTo, AmtFrom, AmtTo, DocType, TenderType, AuthCode, Currency, AD_Org_ID);
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				Vector<Object> line = new Vector<Object>(6);
				line.add(Boolean.FALSE);       //  0-Selection
				line.add(rs.getTimestamp(1));       //  1-DateTrx
				KeyNamePair pp = new KeyNamePair(rs.getInt(2), rs.getString(3));
				line.add(pp);                       //  2-C_Payment_ID
				pp = new KeyNamePair(rs.getInt(4), rs.getString(5));
				line.add(pp);                       //  3-Currency
				line.add(rs.getBigDecimal(6));      //  4-PayAmt
				line.add(rs.getBigDecimal(7));      //  5-Conv Amt
				line.add(rs.getString(8));      	//  6-BParner
				data.add(line);
			}
		}
		catch(SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		
		return data;
	}
	
	/**
	 * set class/type of columns
	 * @param miniTable
	 */
	protected void configureMiniTable(IMiniTable miniTable)
	{
		miniTable.setColumnClass(0, Boolean.class, false);      //  0-Selection
		miniTable.setColumnClass(1, Timestamp.class, true);     //  1-TrxDate
		miniTable.setColumnClass(2, String.class, true);        //  2-Payment
		miniTable.setColumnClass(3, String.class, true);        //  3-Currency
		miniTable.setColumnClass(4, BigDecimal.class, true);    //  4-Amount
		miniTable.setColumnClass(5, BigDecimal.class, true);    //  5-ConvAmount
		miniTable.setColumnClass(6, String.class, true);    	//  6-BPartner
		//  Table UI
		miniTable.autoSize();
	}
	
	/**
	 * Create C_DepositBatchLine
	 */
	@Override	
	public boolean save(IMiniTable miniTable, String trxName)
	{
		//  fixed values
		int C_DepositBatch_ID = ((Integer) getGridTab().getValue("C_DepositBatch_ID")).intValue();
		MDepositBatch db = new MDepositBatch(Env.getCtx(), C_DepositBatch_ID, trxName);
		if (log.isLoggable(Level.CONFIG)) log.config(db.toString());

		//  Lines
		for(int i = 0; i < miniTable.getRowCount(); i++)
		{
			if(((Boolean) miniTable.getValueAt(i, 0)).booleanValue())
			{
				Timestamp trxDate = (Timestamp) miniTable.getValueAt(i, 1);  //  1-DateTrx
				KeyNamePair pp = (KeyNamePair) miniTable.getValueAt(i, 2);   //  2-C_Payment_ID
				int C_Payment_ID = pp.getKey();
				pp = (KeyNamePair) miniTable.getValueAt(i, 3);               //  3-Currency
				int C_Currency_ID = pp.getKey();
				BigDecimal TrxAmt = (BigDecimal) miniTable.getValueAt(i, 4); //  4-PayAmt
				//
				if (log.isLoggable(Level.FINE)) log.fine("Line Date=" + trxDate + ", Payment=" + C_Payment_ID + ", Currency=" + C_Currency_ID + ", Amt=" + TrxAmt);
				//	
				MDepositBatchLine dbl = new MDepositBatchLine(db);
				dbl.setPayment(new MPayment(Env.getCtx(), C_Payment_ID, trxName));
				dbl.saveEx();
			}   //   if selected
		}   //  for all rows
		return true;
	}

	/**
	 * 
	 * @return column header names (select,date,c_payment_id,c_currency_id,amount,converted amount,c_bpartner_id)
	 */
	protected Vector<String> getOISColumnNames()
	{
		//  Header Info
	    Vector<String> columnNames = new Vector<String>(7);
	    columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
		columnNames.add(Msg.translate(Env.getCtx(), "Date"));
		columnNames.add(Msg.getElement(Env.getCtx(), "C_Payment_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "C_Currency_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "Amount"));
		columnNames.add(Msg.translate(Env.getCtx(), "ConvertedAmount"));
		columnNames.add(Msg.translate(Env.getCtx(), "C_BPartner_ID"));		

	    return columnNames;
	}
}