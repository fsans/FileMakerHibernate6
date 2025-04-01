package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.community.dialect.identity.FileMakerIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FileMakerLimitHandler2;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;

import java.sql.Types;

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
        registerDefaultKeywords();
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
        // Use FileMakerLimitHandler2 which extends OffsetFetchLimitHandler
        return new FileMakerLimitHandler2();
    }

     @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
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
        return super.getSqlAstTranslatorFactory();
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

        /* 
     * Reserved FileMaker specific keywords
     * According to the ANSI SQL:2003 standard, SQL keywords are case-insensitive.
     * However, it's a best practice to use uppercase when registering keywords to
     * maintain consistency 
     * 
     */

     @Override
     protected void registerDefaultKeywords() {
 
         // default ansi keywords as in org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords
         super.registerDefaultKeywords();
 
         // added for FileMaker - all keywords must be lowercase as Dialect.java checks tokens in lowercase
         String[] keywords = {
                 // FileMaker-specific keywords
                 "rowid", "modid", "getas", "putas", "recid",
                
                 // Common SQL keywords
                 "select", "insert", "update", "delete", "from", "where",
                
                 // Data type keywords
                 "varchar", "integer", "timestamp", "blob",
                
                 // Function keywords
                 "count", "max", "min", "avg", "sum",
                
                 // FileMaker date/time functions
                 "curdate", "curtime", "today", "now",
                
                 // FileMaker type conversion
                 "strval", "numval", "dateval", "timestampval",

                 // Additional FileMaker keywords
                 "absolute", "action", "add", "asc", "assertion", "begin", "bit", "bit_length", "boolean",
                 "by", "cascade", "catalog", "char_length", "character_length", "chr", "close", "coalesce",
                 "collation", "connect", "connection", "constraints", "convert", "create",
                 "cursor", "curtimestamp", "day", "dayname", "dayofweek", "dec",
                 "deferrable", "deferred", "desc", "descriptor", "diagnostics", "disconnect", "domain", "double",
                 "end_exec", "every", "except", "exception", "exists", "extract", "fetch", "first", "float",
                 "found", "get", "go", "goto", "grant", "group", "having", "hour", "identity", "immediate",
                 "index", "indicator", "initially", "inner", "input", "insensitive", "int", "intersect",
                 "interval", "is", "isolation", "join", "key", "language", "last", "leading", "left", "length",
                 "level", "like", "local", "longvarbinary", "lower", "ltrim", "match", "minute",
                 "module", "month", "monthname", "names", "national", "natural", "nchar", "next", "no", "not",
                 "null", "nullif", "numeric", "octet_length", "of", "offset", "on", "only", "open",
                 "option", "or", "order", "outer", "output", "overlaps", "pad", "part", "partial", "percent",
                 "position", "precision", "prepare", "preserve", "primary", "prior", "privileges", "procedure",
                 "public", "read", "real", "references", "relative", "restrict", "revoke", "right", "rollback",
                 "round", "row", "rows", "rtrim", "schema", "scroll", "second", "section",
                 "session", "session_user", "set", "size", "smallint", "some", "space", "sql", "sqlcode",
                 "sqlerror", "sqlstate", "substring", "system_user", "table", "temporary",
                 "then", "ties", "time", "timeval", "timezone_hour",
                 "timezone_minute", "to", "trailing", "transaction", "translate", "translation",
                 "trim", "true", "union", "unique", "unknown", "upper", "usage", "user", "using",
                 "value", "values", "varying", "view", "when", "whenever", "with",
                 "work", "write", "year", "zone"
         };

         for (String keyword : keywords) {
             registerKeyword(keyword);
         }
     }

}
