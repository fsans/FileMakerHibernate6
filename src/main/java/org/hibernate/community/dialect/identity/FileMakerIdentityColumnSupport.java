package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * Identity column support for FileMaker databases.
 * <p>
 * FileMaker JDBC driver does NOT support auto-generated keys retrieval via
 * {@code Statement.getGeneratedKeys()}. This implementation uses a workaround
 * based on FileMaker's ROWID system column.
 * <p>
 * <b>Strategy:</b> After INSERT, query the record with the highest ROWID to get
 * the user-defined ID column value:
 * <pre>
 * SELECT id FROM table WHERE ROWID = (SELECT MAX(ROWID) FROM table)
 * </pre>
 * <p>
 * <b>Why ROWID instead of MAX(id)?</b>
 * <ul>
 *   <li>ROWID is system-managed and cannot be altered by users</li>
 *   <li>ROWID always increases and never resets (even after record deletion)</li>
 *   <li>ROWID is guaranteed unique within the table</li>
 *   <li>User-defined serial fields (id) can be manually changed or reset</li>
 * </ul>
 * <p>
 * <b>Concurrency Note:</b> There is still a theoretical race condition in high-concurrency
 * scenarios where multiple inserts happen simultaneously. For mission-critical applications,
 * consider using application-generated UUIDs instead.
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
     * <p>
     * Uses FileMaker's ROWID system column to reliably identify the last inserted record,
     * then retrieves the user-defined ID column value. This is safer than {@code MAX(id)}
     * because ROWID:
     * <ul>
     *   <li>Is system-managed and cannot be altered by users</li>
     *   <li>Always increases and never resets</li>
     *   <li>Is guaranteed unique within the table</li>
     * </ul>
     * <p>
     * The query pattern is: {@code SELECT column FROM table WHERE ROWID = (SELECT MAX(ROWID) FROM table)}
     *
     * @param table  the table name
     * @param column the identity column name
     * @param type   the JDBC type code
     * @return SQL string to retrieve the identity value
     */
    @Override
    public String getIdentitySelectString(String table, String column, int type) {
        // Use ROWID to reliably find the last inserted record, then get its id column value
        return "select " + column + " from " + table + " where ROWID = (select max(ROWID) from " + table + ")";
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

