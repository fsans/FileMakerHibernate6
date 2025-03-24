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
     *
     * Fetch syntax
     * FETCH FIRST [ n [ PERCENT ] ] { ROWS | ROW } {ONLY | WITH TIES } ]
     *
     */

    public static final FileMakerLimitHandler INSTANCE = new FileMakerLimitHandler();

    private static final String OFFSET_TEMPLATE = " offset %d rows /*?*/";
    private static final String FETCH_TEMPLATE = " fetch first %d rows only /*?*/";

    @Override
    public String processSql(String sql, Limit selection) {
        StringBuilder stringBuilder = new StringBuilder(sql.length() + OFFSET_TEMPLATE.length() + FETCH_TEMPLATE.length());
        stringBuilder.append(sql);

        // Append offset and fetch if applicable
        appendOffset(stringBuilder, selection);
        appendFetch(stringBuilder, selection);

        return stringBuilder.toString();
    }

    private void appendOffset(StringBuilder stringBuilder, Limit selection) {
        if (hasFirstRow(selection)) {
            stringBuilder.append(String.format(OFFSET_TEMPLATE, selection.getFirstRow()));
        }
    }

    private void appendFetch(StringBuilder stringBuilder, Limit selection) {
        if (hasMaxRows(selection)) {
            stringBuilder.append(String.format(FETCH_TEMPLATE, selection.getMaxRows()));
        }
    }

    @Override
    public boolean supportsLimit() {
        return true;
    }

    @Override
    public boolean supportsLimitOffset() {
        return supportsLimit();
    }
}
