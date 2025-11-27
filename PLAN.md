# Geronimo - FileMaker Hibernate Dialect

> "Geronimo!" - The jump into functional FileMaker + Hibernate integration

## Current Status: ✅ STABLE

The FileMaker Hibernate Dialect is now functional and tested with:

- **FileMaker Server 2025 (22.0.3)**
- **JDBC Driver fmjdbc 22.0.1**
- **Hibernate ORM 6.5.x / 6.6.x**
- **Java 17+**

---

## Completed Features

### Core Dialect ✅

| Feature | Status | Notes |
|---------|--------|-------|
| Type Mappings | ✅ Complete | VARCHAR, DOUBLE, DATE, TIME, TIMESTAMP, BLOB |
| Pagination | ✅ Complete | ANSI SQL `OFFSET/FETCH FIRST` via `FileMakerSqlAstTranslator` |
| Identity Support | ✅ Complete | ROWID-based strategy |
| Reserved Keywords | ✅ Complete | ~100+ FileMaker-specific keywords |
| DDL Disabled | ✅ Complete | Schema managed in FileMaker Pro |

### Testing ✅

| Test Suite | Tests | Status |
|------------|-------|--------|
| IntegrationTest | CRUD operations | ✅ Passing |
| PaginationTest | OFFSET/FETCH | ✅ Passing (2 skipped - driver bugs) |
| DateTimeOperationsTest | LocalDate, LocalTime, LocalDateTime, Timezones | ✅ Passing (9 tests) |
| TypeMappingTest | All supported types | ✅ Passing |
| IdentitySupportTest | ID generation | ✅ Passing |
| LargeTextFieldsTest | Large VARCHAR | ✅ Passing |
| ReservedWordsTest | Keyword escaping | ✅ Passing |
| GenerateRandomDataTest | Bulk insert | ✅ Passing |

Total: 74 tests, 0 failures, 2 skipped

### Documentation ✅

| Document | Status |
|----------|--------|
| README.md | ✅ Complete - Installation, configuration, examples |
| IMPLEMENTATION.md | ✅ Complete - Limitations, driver bugs, workarounds |
| DriverInfo.md | ✅ Complete - JDBC driver reference |
| FileMakerSQL-pagination.md | ✅ Complete - Pagination syntax |

### Developer Tools ✅

| Tool | Status |
|------|--------|
| `FileMakerBaseEntity` | ✅ Abstract base class for entities |
| `maven_deploy_driver.sh` | ✅ JDBC driver installation script |
| `maven_deploy_dialect.sh` | ✅ Dialect installation script |
| Allure Test Reports | ✅ Configured (optional) |

---

## Known Limitations (Documented)

| Limitation | Workaround |
|------------|------------|
| No HikariCP | Use Apache DBCP2 |
| No `Connection.isValid()` | Use `validationQuery` |
| No `SELECT 1` | Use `SELECT * FROM FileMaker_Tables` |
| No `getGeneratedKeys()` | Use ROWID-based identity retrieval |
| No scrollable ResultSets | Forward-only cursors |
| No DDL generation | Manage schema in FileMaker Pro |

---

## Future Enhancements (Backlog)

### Priority 1 - Near Term

- [x] Binary/BLOB handling with `GetAs()`/`PutAs()` ✅ Complete
- [x] Container field support ✅ Complete

### Priority 2 - Medium Term

- [x] Test with new JDBC driver from FMS 2025 (driver 22+) ✅ Tested with 22.0.1
- [ ] Advanced queries (JOINs with limitations)
- [ ] FileMaker ODATA/Data API alternative connector

### Priority 3 - Long Term

- [ ] Maven Central publishing
- [ ] Spring Boot Starter auto-configuration
- [ ] Connection failover support (LOW)
- [ ] Connection pool health metrics (LOW)

---

## Project Structure

```text
FileMakerHibernate6/
├── src/main/java/org/hibernate/community/dialect/
│   ├── FileMakerDialect.java           # Main dialect
│   ├── FileMakerSqlAstTranslator.java  # Pagination SQL generation
│   ├── entity/
│   │   └── FileMakerBaseEntity.java    # Base entity class
│   ├── identity/
│   │   └── FileMakerIdentityColumnSupport.java
│   └── pagination/
│       └── FileMakerLimitHandler2.java
├── src/test/java/                       # Test suite (74 tests)
├── docs/
│   ├── IMPLEMENTATION.md               # Technical documentation
│   ├── DriverInfo.md                   # JDBC driver reference
│   └── FileMakerSQL-pagination.md      # Pagination syntax
├── README.md                           # User guide
├── maven_deploy_driver.sh              # Driver installation
└── maven_deploy_dialect.sh             # Dialect installation
```

---

## Companion Project

**filemaker-demo-api** - Spring Boot REST API demonstrating the dialect:

- CRUD endpoints for Contact entity
- Swagger UI for testing
- Apache DBCP2 connection pool configuration

---

*Branch: geronimo (merged to main)*  
*Last updated: 2025-11-27*
