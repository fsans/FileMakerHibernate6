package org.hibernate.community.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.Limit;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * Custom SQL AST Translator for FileMaker that handles pagination
 * by embedding OFFSET/FETCH values directly in SQL instead of using parameters.
 * <p>
 * FileMaker JDBC driver does not support parameterized OFFSET/FETCH clauses.
 * This translator intercepts pagination rendering and embeds literal values
 * instead of using JDBC parameters.
 */
public class FileMakerSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

    public FileMakerSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
        super(sessionFactory, statement);
    }

    @Override
    protected void renderOffsetExpression(Expression offsetExpression) {
        // Render offset as literal value, not parameter
        renderExpressionAsLiteral(offsetExpression);
    }

    @Override
    protected void renderFetchExpression(Expression fetchExpression) {
        // Render fetch as literal value, not parameter
        renderExpressionAsLiteral(fetchExpression);
    }

    /**
     * Renders an expression as a literal value instead of a parameter.
     * This is required because FileMaker doesn't support parameterized OFFSET/FETCH.
     */
    private void renderExpressionAsLiteral(Expression expression) {
        if (expression instanceof Literal) {
            Object value = ((Literal) expression).getLiteralValue();
            appendSql(String.valueOf(value));
        } else if (expression instanceof QueryLiteral) {
            Object value = ((QueryLiteral<?>) expression).getLiteralValue();
            appendSql(String.valueOf(value));
        } else {
            // For JdbcParameter or other expressions, let parent handle it
            // but this may still produce parameters - FileMaker limitation
            expression.accept(this);
        }
    }

    @Override
    public void visitOffsetFetchClause(QueryPart queryPart) {
        // Check if we have a Limit set (from setFirstResult/setMaxResults)
        // This is the path Spring Data JPA uses
        final Limit limit = getLimit();
        
        if (limit != null && (limit.getFirstRow() != null || limit.getMaxRows() != null)) {
            // Render pagination from Limit object with literal values
            final int offset = limit.getFirstRow() != null ? limit.getFirstRow() : 0;
            final Integer maxRows = limit.getMaxRows();
            
            if (offset > 0) {
                appendSql(" offset ");
                appendSql(String.valueOf(offset));
                appendSql(" rows");
            }
            
            if (maxRows != null && maxRows > 0) {
                appendSql(" fetch first ");
                appendSql(String.valueOf(maxRows));
                appendSql(" rows only");
            }
        } else {
            // Fall back to query part expressions (HQL OFFSET/FETCH)
            final Expression offsetExpression = queryPart.getOffsetClauseExpression();
            final Expression fetchExpression = queryPart.getFetchClauseExpression();
            
            if (offsetExpression != null) {
                appendSql(" offset ");
                renderOffsetExpression(offsetExpression);
                appendSql(" rows");
            }
            
            if (fetchExpression != null) {
                appendSql(" fetch first ");
                renderFetchExpression(fetchExpression);
                appendSql(" rows only");
            }
        }
    }

    @Override
    protected void renderOffsetFetchClause(QueryPart queryPart, boolean renderOffsetRowsKeyword) {
        // Delegate to visitOffsetFetchClause which handles both Limit and expression-based pagination
        visitOffsetFetchClause(queryPart);
    }

    @Override
    public void visitQuerySpec(QuerySpec querySpec) {
        // Let parent render the query
        super.visitQuerySpec(querySpec);
    }

    @Override
    protected boolean useOffsetFetchClause(QueryPart queryPart) {
        // Always use offset/fetch clause rendering (not window functions emulation)
        return true;
    }

    @Override
    protected boolean isRowsOnlyFetchClauseType(QueryPart queryPart) {
        // FileMaker only supports ROWS ONLY, not WITH TIES
        return true;
    }
}
