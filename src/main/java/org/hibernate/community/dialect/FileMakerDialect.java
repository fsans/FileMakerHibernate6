package org.hibernate.community.dialect;

import java.sql.Types;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.community.dialect.identity.FileMakerIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FileMakerLimitHandler;

/**
 * An SQL dialect for FileMaker.
 *
 * @author Francesc Sans
 */


public class FileMakerDialect extends Dialect {

    private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 21, 0 );

    public FileMakerDialect() {
        this( DEFAULT_VERSION );
    }

    public FileMakerDialect(DatabaseVersion version) {
        super(version);
    }

    public FileMakerDialect(DialectResolutionInfo info) {
        //super(info);
        this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
		registerKeywords( info );
    }

    @Override
    public JdbcType resolveSqlTypeDescriptor(
            String columnTypeName,
            int jdbcTypeCode,
            int precision,
            int scale,

             /* driver supported data types: 
                "numeric", "decimal", "int", "varchar", "character varying", "blob", "varbinary", "longvarbinary", "binary varying", "date", "time", "timestamp" 
                 2, 3, 4, 12, 12, -2, -2, -2, -2, 91, 92, 93
            */
            JdbcTypeRegistry jdbcTypeRegistry) {

        switch (jdbcTypeCode) {

            case Types.NUMERIC: // 2 (fm native "Number")
            case Types.DECIMAL: // 3
            case Types.INTEGER: // 4
                jdbcTypeCode = Types.NUMERIC;
                break;

            case Types.VARCHAR: // 12
            case Types.LONGVARCHAR: // -1 (must be a character varying !!!)
                jdbcTypeCode = Types.VARCHAR;
                break;

            case Types.BLOB: // 2004
            case Types.VARBINARY: // -1 
            case Types.LONGVARBINARY: // -4
            //case Types.BINARY-VARYING: (must be a binary varying !!!)
                jdbcTypeCode = Types.BINARY;
                break;

            case Types.DATE: // 91
                jdbcTypeCode = Types.DATE;
                break;

            case Types.TIME: // 92
                jdbcTypeCode = Types.TIME; 
                break;

            case Types.TIMESTAMP: // 93
                jdbcTypeCode = Types.TIMESTAMP; 
                break;

            default:
                jdbcTypeCode = Types.VARCHAR;
        }
        return super.resolveSqlTypeDescriptor( 
            columnTypeName, 
            jdbcTypeCode, 
            precision, 
            scale, 
            jdbcTypeRegistry 
            );
    }


    @Override
    public LimitHandler getLimitHandler() {
        return FileMakerLimitHandler.INSTANCE;
    }

     @Override
    public FileMakerIdentityColumnSupport getIdentityColumnSupport() {
        return FileMakerIdentityColumnSupport.INSTANCE;
    }

    
    @Override
    public boolean dropConstraints() {
        return false;
    }

    @Override
    public boolean hasAlterTable() {
        return false;
    }

    @Override
    public boolean supportsColumnCheck() {
        return false;
    }

    @Override
    public boolean supportsCascadeDelete() {
        return false;
    }

    @Override
    public boolean supportsLockTimeouts() {
        return false;
    }

    @Override
    public boolean canCreateSchema() {
        return false;
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }

    @Override
    public boolean supportsTableCheck() {
        return false;
    }



    @Override
    public boolean supportsUnionAll() {
        return false;
    }

    // New method required for Hibernate 6
    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        //return super.getSqlAstTranslatorFactory();
         return new StandardSqlAstTranslatorFactory();
    }

    // New method required for Hibernate 6
    /*
    @Override
    public void initializeFunctionRegistry(QueryEngine queryEngine) {
        super.initializeFunctionRegistry(queryEngine);
    }
    */


    @Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No create schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No drop schema syntax supported by " + getClass().getName() );
	}


}
