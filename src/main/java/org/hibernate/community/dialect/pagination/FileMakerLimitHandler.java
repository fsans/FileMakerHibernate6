package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.query.spi.Limit;

public class FileMakerLimitHandler extends AbstractLimitHandler {
    /*
     * design notes
     * OFFSET clause should come first.
     * The OFFSET and FETCH FIRST clauses are not supported in subqueries
     * WITH TIES must be used with the ORDER BY clause
     *
     * Offset syntax:
     * OFFSET n {ROWS | ROW} ]
     * where n must be 0 or greater integer
     *
     * Fetch syntax
     * FETCH FIRST [ n [PERCENT] ] { ROWS | ROW } {ONLY | WITH TIES } ]
     * where n must be 1 or greater integer
     */

    public static final FileMakerLimitHandler INSTANCE = new FileMakerLimitHandler();

    @Override
    public String processSql(String sql, Limit selection) {
        StringBuilder stringBuilder = new StringBuilder(sql.length() + 50);
        stringBuilder.append(sql);

        // FileMaker requires OFFSET before FETCH
        if (hasFirstRow(selection)) {
            // FileMaker accepts OFFSET ≥ 0
            long offset = selection.getFirstRow() - 1;
            // Avoid negative offsets
            if (offset < 0) {
                offset = 0;
            }
            stringBuilder.append(" offset ").append(offset).append(" rows");
        }
        
        if (hasMaxRows(selection)) {
            int maxRows = selection.getMaxRows();
            // FileMaker requires FETCH FIRST n ROWS where n ≥ 1
            if (maxRows < 1) {
                // For zero rows, use FETCH FIRST 1 ROW and rely on OFFSET
                maxRows = 1;
            }
            stringBuilder.append(" fetch first ").append(maxRows).append(" rows only");
        }

        return stringBuilder.toString();
    }

    @Override
    public int bindLimitParametersAtEndOfQuery(
            Limit limit,
            java.sql.PreparedStatement statement,
            int index) throws java.sql.SQLException {
        // We're not using parameters, so nothing to bind
        return index;
    }

    @Override
    public boolean supportsLimit() {
        return true;
    }

    @Override
    public boolean supportsLimitOffset() {
        return true;
    }

    @Override
    public boolean supportsVariableLimit() {
        // We're not using variable limits since we're embedding values directly
        return false;
    }
}
