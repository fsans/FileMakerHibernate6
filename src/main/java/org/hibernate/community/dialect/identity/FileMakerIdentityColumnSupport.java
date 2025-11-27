package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * Identity column support for FileMaker databases.
 * <p>
 * FileMaker JDBC driver does NOT support auto-generated keys retrieval.
 * This implementation uses a workaround: after INSERT, it queries {@code select max(id) from table}
 * to retrieve the generated ID.
 * <p>
 * <b>Important:</b> This approach has a race condition risk in high-concurrency scenarios.
 * For production use, consider:
 * <ul>
 *   <li>Using FileMaker's auto-enter serial field and accepting the max(id) limitation</li>
 *   <li>Using application-generated UUIDs instead of database-generated IDs</li>
 *   <li>Using FileMaker's ROWID system column as an alternative identifier</li>
 * </ul>
 *
 * @author Francesc Sans
 */
public class FileMakerIdentityColumnSupport extends IdentityColumnSupportImpl {

    public static final FileMakerIdentityColumnSupport INSTANCE = new FileMakerIdentityColumnSupport();

    /**
     * FileMaker does not support native identity columns in the JDBC sense.
     * IDs are typically auto-enter serial fields configured in FileMaker Pro.
     */
    @Override
    public boolean supportsIdentityColumns() {
        return false;
    }

    @Override
    public String getIdentityColumnString(int type) {
        // Not used since supportsIdentityColumns() returns false
        return "";
    }

    /**
     * Returns SQL to retrieve the last inserted ID.
     * Uses {@code select max(column) from table} as a workaround since
     * FileMaker JDBC does not support getGeneratedKeys().
     *
     * @param table  the table name
     * @param column the identity column name
     * @param type   the JDBC type code
     * @return SQL string to retrieve the identity value
     */
    @Override
    public String getIdentitySelectString(String table, String column, int type) {
        return "select max(" + column + ") from " + table;
    }

    @Override
    public boolean supportsInsertSelectIdentity() {
        return false;
    }

    @Override
    public boolean hasDataTypeInIdentityColumn() {
        return false;
    }
}

