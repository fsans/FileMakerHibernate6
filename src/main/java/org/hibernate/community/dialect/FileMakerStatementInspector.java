/* 
 * THIS IS A DEBUG CLASS TO INTERCEPT ALL HIBERNATE SQL QUERIES
 * TO ENABLE OR DISABLE PLEASE ADD/REMOVE THE FOLLOWING PROPERTY FROMN HIBERNATE.CFS.XML
 * <property name="hibernate.session_factory.statement_inspector">org.hibernate.community.dialect.FileMakerStatementInspector</property>
 */

package org.hibernate.community.dialect;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class FileMakerStatementInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        // Custom logic to handle statement inspection
        return sql;
    }
}
