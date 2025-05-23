/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
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
package org.compiere.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.sql.RowSet;

import org.adempiere.exceptions.DBException;
import org.compiere.util.CCachedRowSet;
import org.compiere.util.CStatementVO;
import org.compiere.util.DB;
import org.compiere.util.Trx;
import org.idempiere.db.util.AutoCommitConnectionBroker;

/**
 * Dynamic proxy for the CPreparedStatement interface 
 * @author Low Heng Sin
 */
public class PreparedStatementProxy extends StatementProxy {

	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param sql0
	 * @param trxName
	 */
	public PreparedStatementProxy(int resultSetType, int resultSetConcurrency,
			String sql0, String trxName) {
		if (sql0 == null || sql0.length() == 0)
			throw new IllegalArgumentException("sql required");

		p_vo = new CStatementVO(resultSetType, resultSetConcurrency, DB
				.getDatabase().convertStatement(sql0));

		p_vo.setTrxName(trxName);

		init();
	} // PreparedStatementProxy
	
	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param sql0
	 * @param connection
	 */
	public PreparedStatementProxy(int resultSetType, int resultSetConcurrency,
			String sql0, Connection connection) {
		if (sql0 == null || sql0.length() == 0)
			throw new IllegalArgumentException("sql required");

		p_vo = new CStatementVO(resultSetType, resultSetConcurrency, DB
				.getDatabase().convertStatement(sql0));

		init(connection);
	} // PreparedStatementProxy
	
	/**
	 * @param vo
	 */
	public PreparedStatementProxy(CStatementVO vo)
	{
		super(vo);
	}	//	PreparedStatementProxy

	/**
	 * Initialize the prepared statement wrapper object
	 */
	protected void init() {
		try {
			Connection conn = null;
			Trx trx = p_vo.getTrxName() == null ? null : Trx.get(p_vo
					.getTrxName(), false);
			if (trx != null) {
				conn = trx.getConnection();
			} else {
				m_conn = AutoCommitConnectionBroker.getConnection();
				conn = m_conn;
			}
			if (conn == null)
				throw new DBException("No Connection");
			p_stmt = conn.prepareStatement(p_vo.getSql(), p_vo
					.getResultSetType(), p_vo.getResultSetConcurrency());
		} catch (Exception e) {
			try {
				this.close();
			} catch (SQLException e1) {
				// ignore
			}
			log.log(Level.SEVERE, p_vo.getSql(), e);
			throw new DBException(e);
		}
	}

	/**
	 * Initialize the prepared statement wrapper object
	 */
	protected void init(Connection connection) {
		try {
			p_stmt = connection.prepareStatement(p_vo.getSql(), p_vo
					.getResultSetType(), p_vo.getResultSetConcurrency());
		} catch (Exception e) {
			log.log(Level.SEVERE, p_vo.getSql(), e);
			throw new DBException(e);
		}
	}
	
	@Override
	protected RowSet getRowSet() 
	{
		if (log.isLoggable(Level.FINE))
			log.finest("local_getRowSet");
		
		RowSet rowSet = null;
		ResultSet rs = null;
		PreparedStatement pstmt = (PreparedStatement)p_stmt;
		try
		{
			rs = pstmt.executeQuery();
			rowSet = CCachedRowSet.getRowSet(rs);						
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, p_vo.toString(), ex);
			throw new RuntimeException (ex);
		}		
		finally
		{
			DB.close(rs);
		}
		return rowSet;
	}	//	local_getRowSet

}
