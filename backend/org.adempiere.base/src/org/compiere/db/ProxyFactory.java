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

import java.lang.reflect.Proxy;
import java.sql.Connection;

import org.compiere.util.CCallableStatement;
import org.compiere.util.CPreparedStatement;
import org.compiere.util.CStatement;
import org.compiere.util.CStatementVO;

/**
 * Factory class to instantiate dynamic proxy for CStatement, CPreparedStatement and CCallableStatement
 * @author Low Heng Sin
 *
 */
public class ProxyFactory {
	
	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param trxName
	 * @return new CStatement proxy instance
	 */
	public static CStatement newCStatement(int resultSetType,
			int resultSetConcurrency, String trxName) {
		return (CStatement)Proxy.newProxyInstance(CStatement.class.getClassLoader(), 
				new Class[]{CStatement.class}, 
				new StatementProxy(resultSetType, resultSetConcurrency, trxName));
	}
	
	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param sql
	 * @param trxName
	 * @return new CPreparedStatement proxy instance
	 */
	public static CPreparedStatement newCPreparedStatement(int resultSetType,
			int resultSetConcurrency, String sql, String trxName) {
		return (CPreparedStatement)Proxy.newProxyInstance(CPreparedStatement.class.getClassLoader(), 
				new Class[]{CPreparedStatement.class}, 
				new PreparedStatementProxy(resultSetType, resultSetConcurrency, sql, trxName));
	}

	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param sql
	 * @param connection
	 * @return new CPreparedStatement proxy instance
	 */
	public static CPreparedStatement newCPreparedStatement(int resultSetType,
			int resultSetConcurrency, String sql, Connection connection) {
		return (CPreparedStatement)Proxy.newProxyInstance(CPreparedStatement.class.getClassLoader(), 
				new Class[]{CPreparedStatement.class}, 
				new PreparedStatementProxy(resultSetType, resultSetConcurrency, sql, connection));
	}
	
	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param sql
	 * @param trxName
	 * @return new CCallableStatement proxy instance
	 */
	public static CCallableStatement newCCallableStatement(int resultSetType,
			int resultSetConcurrency, String sql, String trxName) {
		return (CCallableStatement)Proxy.newProxyInstance(CCallableStatement.class.getClassLoader(), 
				new Class[]{CCallableStatement.class}, 
				new CallableStatementProxy(resultSetType, resultSetConcurrency, sql, trxName));
	}

	/**
	 * @param info
	 * @return new CStatement proxy instance
	 */
	public static CStatement newCStatement(CStatementVO info) {
		return (CStatement)Proxy.newProxyInstance(CStatement.class.getClassLoader(), 
				new Class[]{CStatement.class}, 
				new StatementProxy(info));
	}
	
	/**
	 * @param info
	 * @return new CPreparedStatement proxy instance
	 */
	public static CPreparedStatement newCPreparedStatement(CStatementVO info) {
		return (CPreparedStatement)Proxy.newProxyInstance(CPreparedStatement.class.getClassLoader(), 
				new Class[]{CPreparedStatement.class}, 
				new PreparedStatementProxy(info));
	}
	
	/**
	 * @param info
	 * @return new CCallableStatement proxy instance
	 */
	public static CCallableStatement newCCallableStatement(CStatementVO info) {
		return (CCallableStatement)Proxy.newProxyInstance(CCallableStatement.class.getClassLoader(), 
				new Class[]{CCallableStatement.class}, 
				new CallableStatementProxy(info));
	}
	
	/**
	 * new proxy instance for read replica prepared statement
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param sql
	 * @return new {@link CPreparedStatement} proxy instance
	 */
	public static CPreparedStatement newReadReplicaPreparedStatement(int resultSetType, int resultSetConcurrency, String sql) {
		ReadReplicaPreparedStatementProxy handler = new ReadReplicaPreparedStatementProxy(resultSetType, resultSetConcurrency, sql);
		if (handler.isValid())
			return (CPreparedStatement)Proxy.newProxyInstance(CPreparedStatement.class.getClassLoader(), new Class[]{CPreparedStatement.class}, handler);
		else
			return null;
	}
}
