package org.hibernate.community.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * Custom SQL AST Translator for FileMaker that handles pagination
 * by embedding OFFSET/FETCH values directly in SQL instead of using parameters.
 * <p>
 * FileMaker JDBC driver does not support parameterized OFFSET/FETCH clauses.
 */
public class FileMakerSqlAstTranslator<T extends JdbcOperation> extends StandardSqlAstTranslator<T> {

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
    protected void renderOffsetFetchClause(QueryPart queryPart, boolean renderOffsetRowsKeyword) {
        // FileMaker uses ANSI SQL syntax: OFFSET n ROWS FETCH FIRST m ROWS ONLY
        // We override to ensure literal values are used instead of parameters
        
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
