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
 * Hibernate dialect for FileMaker databases via JDBC.
 * <p>
 * Supports FileMaker Server 20+ with the FileMaker JDBC driver.
 * <p>
 * Key features:
 * <ul>
 *   <li>OFFSET/FETCH pagination (ANSI SQL standard)</li>
 *   <li>Type mappings for NUMERIC, VARCHAR, DATE, TIME, TIMESTAMP, BLOB</li>
 *   <li>Identity column support via ROWID-based retrieval</li>
 * </ul>
 * <p>
 * Limitations (FileMaker JDBC driver constraints):
 * <ul>
 *   <li>No DDL support (CREATE/ALTER/DROP)</li>
 *   <li>No auto-generated keys retrieval</li>
 *   <li>No scrollable result sets</li>
 *   <li>No savepoints</li>
 *   <li>No subquery pagination</li>
 * </ul>
 *
 * @author Francesc Sans
 * @see <a href="https://github.com/fsans/FileMakerHibernate6">GitHub Repository</a>
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

            // FileMaker JDBC driver supported types:
            // NUMERIC(2), DECIMAL(3), INTEGER(4), VARCHAR(12), BLOB(-2), DATE(91), TIME(92), TIMESTAMP(93)
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

    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        // Use custom translator that embeds OFFSET/FETCH values directly
        // FileMaker doesn't support parameterized pagination
        return new FileMakerSqlAstTranslatorFactory();
    }

    /**
     * FileMaker doesn't support parameterized OFFSET/FETCH in SQL AST.
     * Return false to force use of LimitHandler.processSql() which embeds values directly.
     */
    @Override
    public boolean supportsOffsetInSubquery() {
        return false;
    }

    /**
     * FileMaker supports FETCH FIRST n ROWS ONLY syntax.
     * Return false to force use of LimitHandler instead of SQL AST rendering.
     */
    @Override
    public boolean supportsFetchClause(org.hibernate.query.sqm.FetchClauseType fetchClauseType) {
        // Return false to force LimitHandler usage instead of SQL AST
        return false;
    }


    @Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No create schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "No drop schema syntax supported by " + getClass().getName() );
	}

    /**
     * Registers FileMaker-specific SQL keywords.
     * Keywords are registered in lowercase as Hibernate's Dialect.java checks tokens in lowercase.
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
