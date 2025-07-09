---
globs:
  - "**/HikariCPIntegrationTest.java"
  - "**/hibernate.cfg.xml"
description: Standard configuration rules for using HikariCP with the FileMaker
  JDBC driver in Hibernate projects.
---

# HikariCP Configuration for FileMaker JDBC

Use the fmjdbc.jar for FileMaker JDBC Driver with driver class 'com.filemaker.jdbc.Driver'. Use 'org.hibernate.community.dialect.FileMakerDialect' as the Hibernate dialect. Set the connection test query to 'SELECT p.* FROM FileMaker_Tables p'.