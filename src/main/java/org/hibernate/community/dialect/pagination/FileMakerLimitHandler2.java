package org.hibernate.community.dialect.pagination;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.query.spi.Limit;

/**
 * A {@link LimitHandler} for FileMaker, which supports ANSI SQL standard syntax
 * for pagination using OFFSET and FETCH clauses.
 * <p>
 * FileMaker requires direct embedding of values in SQL strings rather than using
 * parameterized queries for OFFSET and FETCH clauses.
 * <p>
 * Syntax: OFFSET n ROWS FETCH FIRST m ROWS ONLY
 * <p>
 * OFFSET value must be ≥ 0
 * FETCH FIRST value must be ≥ 1
 * <p>
 * FileMaker also supports FETCH FIRST PERCENT syntax:
 * FETCH FIRST n PERCENT ROWS ONLY
 */
public class FileMakerLimitHandler2 extends OffsetFetchLimitHandler {
    // FileMaker supports:
    // [OFFSET n {ROW|ROWS}]
    // [FETCH FIRST m {ROW|ROWS} {ONLY|WITH TIES}]
    // [FETCH FIRST n PERCENT {ROW|ROWS} {ONLY|WITH TIES}]

    // Pattern to find FOR UPDATE at the end of the query
    private static final Pattern FOR_UPDATE_PATTERN = Pattern.compile("\\s+for\\s+update\\b|\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);
    
    // Pattern to find FETCH FIRST PERCENT syntax
    private static final Pattern FETCH_PERCENT_PATTERN = Pattern.compile("\\s+fetch\\s+first\\s+\\d+\\s+percent\\s+rows\\s+(only|with\\s+ties)", Pattern.CASE_INSENSITIVE);

    public FileMakerLimitHandler2() {
        // FileMaker doesn't support parameterized values for OFFSET/FETCH
        super(false);
    }

    @Override
    public String processSql(String sql, Limit limit) {
        // If the SQL already contains FETCH FIRST PERCENT syntax, return it as is
        if (FETCH_PERCENT_PATTERN.matcher(sql).find()) {
            return sql;
        }
        
        // For FileMaker, we need to directly embed the values in the SQL string
        // instead of using parameters, as FileMaker has issues with parameterized
        // values for OFFSET and FETCH clauses
        
        final int firstRow = hasFirstRow(limit) ? limit.getFirstRow() : 0;
        final int maxRows = hasMaxRows(limit) ? limit.getMaxRows() : Integer.MAX_VALUE;
        
        final StringBuilder sb = new StringBuilder(sql.length() + 50);
        
        // Check for FOR UPDATE clause
        String forUpdateClause = null;
        Matcher matcher = FOR_UPDATE_PATTERN.matcher(sql);
        if (matcher.find()) {
            forUpdateClause = matcher.group();
            sb.append(sql, 0, sql.length() - forUpdateClause.length());
        }
        else {
            sb.append(sql);
        }
        
        // Add OFFSET clause (must be ≥ 0)
        sb.append(" offset ").append(firstRow).append(" rows");
        
        // Add FETCH FIRST clause (must be ≥ 1)
        sb.append(" fetch first ").append(maxRows).append(" rows only");
        
        // Append the FOR UPDATE clause if it was present
        if (forUpdateClause != null) {
            sb.append(forUpdateClause);
        }
        
        return sb.toString();
    }

    /**
     * FileMaker supports FOR UPDATE at the end of the query.
     * The offset/fetch clauses must come before FOR UPDATE.
     */
    @Override
    protected Pattern getForUpdatePattern() {
        return FOR_UPDATE_PATTERN;
    }
    
    @Override
    public int bindLimitParametersAtStartOfQuery(
            Limit limit,
            java.sql.PreparedStatement statement,
            int index) throws java.sql.SQLException {
        // Since we're using embedded values (variableLimit=false), 
        // there's nothing to bind
        return index;
    }

    @Override
    public int bindLimitParametersAtEndOfQuery(
            Limit limit,
            java.sql.PreparedStatement statement,
            int index) throws java.sql.SQLException {
        // Since we're using embedded values (variableLimit=false), 
        // there's nothing to bind
        return index;
    }
}
