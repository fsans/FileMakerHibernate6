package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

public class FileMakerIdentityColumnSupport extends IdentityColumnSupportImpl {
 
  
    public static final FileMakerIdentityColumnSupport INSTANCE = new FileMakerIdentityColumnSupport();


    @Override
    public boolean supportsIdentityColumns() {
        return false;
    }


    @Override
    public String getIdentityColumnString(int type) {
        // The keyword used to specify an identity column, if identity column key generation is supported.
        return "SERIAL"; // Representing the concept, adjust as per actual requirement
    }


    @Override
    public String getIdentitySelectString(String table, String column, int type) {
        // Use Hibernate's syntax for retrieving the last inserted identity value
        // use the internal (unique, serialized integer) rowid as a secure identity
        return "select max(id) from " + table;
        //return "select ROWID from " + table;
    }


    @Override
    public boolean supportsInsertSelectIdentity(){
        return false;
    }


    @Override
    public boolean hasDataTypeInIdentityColumn() {
        return false; // FileMaker support a native identity column type ??
    }

  

}

