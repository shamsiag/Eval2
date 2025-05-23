/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2009 www.metas.de                                            *
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
package org.adempiere.model;

import static org.compiere.model.I_AD_Ref_Table.COLUMNNAME_AD_Reference_ID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.PORelationException;
import org.adempiere.model.ZoomInfoFactory.ZoomInfo;
import org.compiere.model.I_AD_Ref_Table;
import org.compiere.model.Lookup;
import org.compiere.model.MColumn;
import org.compiere.model.MQuery;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_AD_RelationType;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Extended model class for AD_RelationType.<br/>
 * Formal definition for a set of data record pairs.
 * 
 * @author Tobias Schoeneberg, www.metas.de - FR [ 2897194 ] Advanced Zoom and
 *         RelationTypes
 */
public class MRelationType extends X_AD_RelationType implements IZoomProvider {

	private static final CLogger logger = CLogger
			.getCLogger(MRelationType.class);

	/**
	 * Selection for those relation types whose AD_Reference(s) might match a
	 * given PO. Only evaluates the table and key column of the reference's
	 * AD_Ref_Table entries.
	 * <p>
	 * <b>Warning:</b> Doesn't support POs with more or less than one key
	 * column.
	 */
	protected final static String SQL =
	    "  SELECT "
			+ "    rt.AD_RelationType_ID"
			+ ",   rt.Name"
			+ ",   rt.IsDirected"
			+ ",   ref.AD_Reference_ID"
			+ ",   tab.WhereClause"
			+ ",   tab.OrderByClause"
			+ "  FROM"
			+ "    AD_RelationType rt, AD_Reference ref, AD_Ref_Table tab"
			+ "  WHERE "
			+ "    rt.IsActive='Y'"
			+ "    AND ref.IsActive='Y'"
			+ "    AND ref.ValidationType='T'" // must have table validation
			+ "    AND (" // join the source AD_Reference
			+ "      rt.AD_Reference_Source_ID=ref.AD_Reference_ID" //
			+ "      OR (" // not directed? -> also join the target AD_Reference
			+ "        rt.IsDirected='N' "
			+ "        AND rt.AD_Reference_Target_ID=ref.AD_Reference_ID"
			+ "      )"
			+ "    )"
			+ "    AND tab.IsActive='Y'" // Join the AD_Reference's AD_Ref_Table
			+ "    AND tab.AD_Reference_ID=ref.AD_Reference_ID"
			+ "    AND tab.AD_Table_ID=?"
			+ "    AND tab.AD_Key=?"
			+ "  ORDER BY rt.Name";

	protected final static String SQL_WINDOW_NAME = "SELECT Name FROM AD_Window WHERE AD_WINDOW_ID=?";

	protected final static String SQL_WINDOW_NAME_TRL = "SELECT Name FROM AD_Window_Trl WHERE AD_WINDOW_ID=? AND AD_LANGUAGE=?";

	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = 5486148151201672913L;

	public int destinationRefId;

    /**
     * UUID based Constructor
     * @param ctx  Context
     * @param AD_RelationType_UU  UUID key
     * @param trxName Transaction
     */
    public MRelationType(Properties ctx, String AD_RelationType_UU, String trxName) {
        super(ctx, AD_RelationType_UU, trxName);
    }

    /**
     * @param ctx
     * @param AD_RelationType_ID
     * @param trxName
     */
	public MRelationType(Properties ctx, int AD_RelationType_ID, String trxName) {
		super(ctx, AD_RelationType_ID, trxName);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MRelationType(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * @param ctx
	 * @param AD_RelationType_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MRelationType(Properties ctx, int AD_RelationType_ID, String trxName, String... virtualColumns) {
		super(ctx, AD_RelationType_ID, trxName, virtualColumns);
	}

	/**
	 * Returns the types that define a relation which contains the given PO.<br/>
	 * Explicit types are returned even if they don't actually contain the given
	 * PO.
	 * 
	 * @param po
	 * @return matching relation types
	 */
	public static List<MRelationType> retrieveTypes(final PO po,
			final int windowId) {

		if (po.get_KeyColumns().length != 1) {

			logger.severe(po + " has " + po.get_KeyColumns().length
					+ " key column(s). Should have one.");
			PORelationException.throwWrongKeyColumnCount(po);
		}

		final String keyColumn = po.get_KeyColumns()[0];

		final int colId = MColumn.getColumn_ID(po.get_TableName(), keyColumn);

		final PreparedStatement pstmt = DB.prepareStatement(SQL, po
				.get_TrxName());

		ResultSet rs = null;
		try {
			pstmt.setInt(1, po.get_Table_ID());
			pstmt.setInt(2, colId);

			rs = pstmt.executeQuery();

			final List<MRelationType> result = evalResultSet(po, windowId, rs);

			if (logger.isLoggable(Level.INFO)) logger.info("There are " + result.size() + " matching types for "
					+ po);

			return result;

		} catch (SQLException e) {
			logger.severe(e.getMessage());
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**
	 * @param po
	 * @param windowID
	 * @return zoom info records from matching relation types
	 */
	public static List<ZoomInfo> retrieveZoomInfos(final PO po,
			final int windowID) {

		final List<MRelationType> matchingTypes = MRelationType.retrieveTypes(
				po, windowID);

		final List<ZoomInfo> result = new ArrayList<ZoomInfo>();

		for (final MRelationType currentType : matchingTypes) {

			result.addAll(currentType.retrieveZoomInfos(po));

		}
		return result;
	}

	/**
	 * @return display text from lookup of destination reference
	 */
	private String getDestinationRoleDisplay() {

		checkDestinationRefId();

		final Integer colIdx;
		final String keyValue;

		if (destinationRefId == getAD_Reference_Source_ID()) {

			colIdx = this.get_ColumnIndex(COLUMNNAME_Role_Source);
			keyValue = getRole_Source();
		} else {
			colIdx = this.get_ColumnIndex(COLUMNNAME_Role_Target);
			keyValue = getRole_Target();
		}

		if (Util.isEmpty(keyValue)) {
			return "";
		}
		final Lookup lookup = this.p_info.getColumnLookup(colIdx);
		return lookup.getDisplay(keyValue);
	}

	/**
	 * @param windowId
	 * @return window name
	 */
	private String retrieveWindowName(final int windowId) {

		final boolean baseLanguage = Env.isBaseLanguage(Env.getCtx(),
				"AD_Window");
		final String sql = baseLanguage ? SQL_WINDOW_NAME : SQL_WINDOW_NAME_TRL;

		final PreparedStatement pstmt = DB.prepareStatement(sql, null);
		ResultSet rs = null;
		try {
			pstmt.setInt(1, windowId);
			if(!baseLanguage)
			{    
			pstmt.setString(2, Env.getAD_Language(Env.getCtx()));
			}
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
		}
	}

	/**
	 * @param po
	 * @param windowId
	 * @param rs
	 * @return relation types
	 * @throws SQLException
	 */
	private static List<MRelationType> evalResultSet(final PO po,
			final int windowId, final ResultSet rs) throws SQLException {

		final List<MRelationType> result = new ArrayList<MRelationType>();

		final Set<Integer> alreadySeen = new HashSet<Integer>();

		while (rs.next()) {

			final int relTypeId = rs.getInt(COLUMNNAME_AD_RelationType_ID);
			if (!alreadySeen.add(relTypeId)) {
				continue;
			}

			final MRelationType newType = new MRelationType(po.getCtx(),
					relTypeId, po.get_TrxName());

			// figure out which AD_reference is the destination relative to the
			// given po and windowID

			if (newType.isDirected()) {
				// the type is directed, so the target reference is always the
				// destination.
				newType.destinationRefId = newType.getAD_Reference_Target_ID();
			}

			else if (newType.retrieveSourceTableName().equals(
					newType.retrieveTargetTableName())) {

				final MRefTable sourceRefTable = retrieveRefTable(po.getCtx(),
						newType.getAD_Reference_Source_ID(), po.get_TrxName());

				if (windowId == newType.retrieveWindowID(po, sourceRefTable)) {

					newType.destinationRefId = newType
							.getAD_Reference_Target_ID();
				} else {
					newType.destinationRefId = newType
							.getAD_Reference_Source_ID();
				}
			} else {

				if (po.get_TableName()
						.equals(newType.retrieveSourceTableName())) {

					newType.destinationRefId = newType
							.getAD_Reference_Target_ID();
				} else {
					newType.destinationRefId = newType
							.getAD_Reference_Source_ID();
				}
			}
			result.add(newType);
		}
		return result;
	}

	/**
	 * @param po
	 * @param where
	 * @return true if where is empty or match found in DB
	 */
	protected static boolean whereClauseMatches(PO po, String where) {

		if (Util.isEmpty(where, true)) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("whereClause is empty. Returning true");
			return true;
		}

		final String parsedWhere = parseWhereClause(po, where);
		if (Util.isEmpty(parsedWhere)) {
			return false;
		}

		final PO result = new Query(po.getCtx(), po.get_TableName(),
				parsedWhere, po.get_TrxName()).first();
		final boolean match = result != null;

		if (logger.isLoggable(Level.FINE)) logger.fine("whereClause='" + parsedWhere + "' matches po='" + po
				+ "':" + match);
		return match;
	}

	/**
	 * @param po
	 * @param where
	 * @return parsed where clause
	 */
	public static String parseWhereClause(final PO po, final String where) {

		if (logger.isLoggable(Level.FINE))
			logger.fine("building private ctx instance containing the PO's String and int values");
		
		final Properties privateCtx = new Properties();
		privateCtx.putAll(po.getCtx());
		for (int i = 0; i < po.get_ColumnCount(); i++) {
			final Object val = po.get_Value(i);

			if (val == null) {
				continue;
			}

			if (val instanceof Integer) {

				Env.setContext(privateCtx, "#" + po.get_ColumnName(i),
						(Integer) val);
				Env.setContext(privateCtx, po.get_ColumnName(i),
						(Integer) val);

			} else if (val instanceof String) {

				Env.setContext(privateCtx, "#" + po.get_ColumnName(i),
						(String) val);
				Env.setContext(privateCtx, po.get_ColumnName(i),
						(String) val);
			}
		}

		final String parsedWhere = Env.parseContext(privateCtx, -1, where,
				false);

		if (logger.isLoggable(Level.FINE)) logger.fine("whereClause='" + where + "'; parsedWhere='" + parsedWhere
				+ "'");

		return parsedWhere;
	}

	/**
	 * throw exception if destination reference is not valid
	 */
	public void checkDestinationRefId() {

		if (destinationRefId == 0) {
			throw new IllegalStateException(
					"Can't create a destination query when I don't know which one of the two AD_Reference_ID is the destination.");
		}
	}

	/**
	 * 
	 * @param po
	 * @return zoom info records from destination reference
	 */
	public List<ZoomInfoFactory.ZoomInfo> retrieveZoomInfos(final PO po) {

		checkDestinationRefId();

		final MRefTable refTable = retrieveRefTable(getCtx(), destinationRefId,
				get_TrxName());

		final MQuery query = new MQuery();
		query.addRestriction(parseWhereClause(po, refTable.getWhereClause()));
		query.setZoomTableName(retrieveDestinationTableName());
		query.setZoomColumnName(retrieveDestinationKeyColName());

		evaluateQuery(query);

		final int windowId = retrieveWindowID(po, refTable);

		String display = getDestinationRoleDisplay();
		if (Util.isEmpty(display)) {
			display = retrieveWindowName(windowId);
		}
		assert !Util.isEmpty(display);
		
		return Collections.singletonList(new ZoomInfoFactory.ZoomInfo(windowId,
				query, display));
	}

	/**
	 * @param po
	 * @param refTable
	 * @return AD_Window_ID
	 */
	public int retrieveWindowID(final PO po, final MRefTable refTable) {

		MTable table = null;

		int windowId = refTable.getAD_Window_ID();
		if (windowId == 0) {

			final int tableId = refTable.getAD_Table_ID();
			table = MTable.get(po.getCtx(), tableId);

			if (Env.isSOTrx(po.getCtx())) {
				windowId = table.getAD_Window_ID();
			} else {
				windowId = table.getPO_Window_ID();
			}
		}

		if (windowId == 0) {

			PORelationException.throwMissingWindowId(po,
					getAD_Reference_Target().getName(), table.getName(), Env
							.isSOTrx(po.getCtx()));
		}
		return windowId;

	}

	/**
	 * @param ctx
	 * @param referenceId
	 * @param trxName
	 * @return MRefTable
	 */
	public static MRefTable retrieveRefTable(final Properties ctx,
			final int referenceId, final String trxName) {

		final Object[] params = { referenceId };

		final MRefTable refTable = new Query(ctx, I_AD_Ref_Table.Table_Name,
				COLUMNNAME_AD_Reference_ID + "=?", trxName).setParameters(
				params).firstOnly();

		return refTable;
	}

	/**
	 * Update record count and zoom value of query
	 * @param query
	 */
	private static void evaluateQuery(final MQuery query) {

		StringBuilder sqlCommon = new StringBuilder(" FROM ").append(query.getZoomTableName())
				.append(" WHERE ").append(query.getWhereClause(false));

		final String sqlCount = "SELECT COUNT(*) " + sqlCommon;

		final int count = DB.getSQLValueEx(null, sqlCount);
		query.setRecordCount(count);

		if (count > 0) {

			StringBuilder sqlFirstKey = new StringBuilder("SELECT ").append(query.getZoomColumnName())
					.append(sqlCommon);

			final int firstKey = DB.getSQLValueEx(null, sqlFirstKey.toString());
			query.setZoomValue(firstKey);
		}
	}

	/**
	 * @return table name of source reference
	 */
	private String retrieveSourceTableName() {

		return retrieveTableName(getAD_Reference_Source_ID());
	}

	/**
	 * @return table name of target reference
	 */
	private String retrieveTargetTableName() {

		return retrieveTableName(getAD_Reference_Target_ID());
	}

	/**
	 * @return table name of destination reference
	 */
	public String retrieveDestinationTableName() {

		return retrieveTableName(destinationRefId);
	}

	/**
	 * @param refId
	 * @return table name
	 */
	private String retrieveTableName(final int refId) {

		return retrieveRefTable(getCtx(), refId, get_TrxName()).getAD_Table()
				.getTableName();
	}

	/**
	 * @return key column name of destination reference
	 */
	public String retrieveDestinationKeyColName() {

		final int keyColumnId = retrieveRefTable(getCtx(), destinationRefId,
				get_TrxName()).getAD_Key();
		return MColumn.getColumnName(getCtx(), keyColumnId);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("MRelationType[");
		//
		sb.append(get_ID());
		sb.append(", Name=").append(getName());
		sb.append(", Type=").append(getType());

		sb.append(", AD_Reference_Destination_RefId=").append(destinationRefId);

		sb.append(", AD_Reference_Source_ID=").append(
				getAD_Reference_Source_ID());
		sb.append(", Role_Source=").append(getRole_Source());

		sb.append(", AD_Reference_Target_ID=").append(
				getAD_Reference_Target_ID());
		sb.append(", Role_Target=").append(getRole_Target()); //

		sb.append("]");

		return sb.toString();
	}
}