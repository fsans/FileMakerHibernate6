package org.hibernate.community.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * Factory for creating FileMaker-specific SQL AST translators.
 * <p>
 * This factory creates translators that embed pagination values directly
 * in SQL instead of using parameters, as required by FileMaker JDBC.
 */
public class FileMakerSqlAstTranslatorFactory extends StandardSqlAstTranslatorFactory {

    @Override
    protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
            SessionFactoryImplementor sessionFactory, 
            Statement statement) {
        return new FileMakerSqlAstTranslator<>(sessionFactory, statement);
    }
}
