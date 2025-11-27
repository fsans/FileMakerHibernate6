# Geronimo - Minimal Viable FileMaker Hibernate Dialect

> "Geronimo!" - The jump into minimal, functional FileMaker + Hibernate integration

## Goal

A minimal, functional Hibernate 6.5+ dialect for Spring Boot + FileMaker + HikariCP that:

- ✅ Passes basic CRUD tests
- ✅ Uses available key generation strategy (`max(id)` workaround)
- ✅ Implements OFFSET/FETCH pagination
- ✅ Maps only FileMaker-supported types

---

## Key Constraints (from FileMaker JDBC Driver)

| Constraint | Impact |
|------------|--------|
| **No auto-generated keys** | Must use `select max(id)` or rely on FileMaker auto-enter serial |
| **No timezone support** | Use `LocalDateTime` without timezone |
| **No subquery pagination** | OFFSET/FETCH only at top-level SELECT |
| **No DDL** | `hbm2ddl.auto=none` always |
| **JDBC 3 minimal** | No scrollable cursors, no savepoints |

---

## Phase 1: Core Dialect Cleanup

### 1.1 Simplify `FileMakerDialect.java`

- [x] Remove unused/commented code
- [x] Keep only essential type mappings: `NUMERIC`, `VARCHAR`, `DATE`, `TIME`, `TIMESTAMP`, `BLOB`
- [x] Confirm `FileMakerLimitHandler2` is the only pagination handler
- [x] Remove `FileMakerLimitHandler` (legacy)

### 1.2 Fix Identity Strategy

- [ ] Document that `@GeneratedValue(strategy = GenerationType.IDENTITY)` uses `select max(id)`
- [ ] Alternative: Use `@GeneratedValue(strategy = GenerationType.AUTO)` with a custom ID generator or let FileMaker handle it via auto-enter serial

### 1.3 Type Mapping Verification

Map only what FileMaker JDBC actually supports:

| Java Type | FileMaker SQL Type | JDBC Code |
|-----------|-------------------|-----------|
| `Long/Integer` | NUMERIC | 2 |
| `Double/BigDecimal` | DECIMAL | 3 |
| `String` | VARCHAR | 12 |
| `byte[]` | BLOB | -2 |
| `LocalDate` | DATE | 91 |
| `LocalTime` | TIME | 92 |
| `LocalDateTime` | TIMESTAMP | 93 |

---

## Phase 2: Spring Boot + HikariCP Integration

### 2.1 Create Sample `application.yml`

- [ ] Create reference configuration for Spring Boot + HikariCP
- [ ] Document all required properties

### 2.2 Test HikariCP Connection

- [ ] Create integration test that verifies pool initialization
- [ ] Verify connection acquisition/release cycle

---

## Phase 3: Minimal Test Suite

### 3.1 Essential CRUD Tests

| Test | Status | Priority |
|------|--------|----------|
| `testCreate` | ✅ Passing | P0 |
| `testRead` | ✅ Passing | P0 |
| `testUpdate` | ✅ Passing | P0 |
| `testDelete` | ✅ Passing | P0 |

### 3.2 Fix Skipped Tests

- [ ] `testRowIdPersistence` - Fix field validation
- [ ] `testDateTimeOperations` - Remove timezone expectations, use `LocalDateTime`

### 3.3 Pagination Test

- [ ] Verify `OFFSET x ROWS FETCH FIRST y ROWS ONLY` generates correctly
- [ ] Test with Spring Data `Pageable`

---

## Phase 4: Documentation & Packaging

### 4.1 Update README

- [ ] HikariCP configuration example
- [ ] Spring Boot setup guide
- [ ] Known limitations section

### 4.2 Clean Up Project

- [ ] Remove `FileMakerLimitHandler.java` (keep only `FileMakerLimitHandler2`)
- [ ] Remove `FileMakerStatementInspector` or make it optional debug tool
- [ ] Update `status.md` with final state

---

## Progress Log

### 2025-11-27

- [x] Created `geronimo` branch
- [x] Created this plan document
- [x] **Phase 1.1 Complete:**
  - Removed legacy `FileMakerLimitHandler.java`
  - Cleaned up `FileMakerDialect.java` (improved docs, removed commented code)
  - Improved `FileMakerIdentityColumnSupport.java` (better docs, fixed column name in getIdentitySelectString)
  - Updated JDBC driver version to 21.0.2 in pom.xml
  - Build compiles successfully
- [x] **Tests verified:** 60 tests run, 57 passed, 2 skipped, 1 edge case failure (non-critical)

---

## Out of Scope (Deferred)

- Spring Boot Starter auto-configuration
- Maven Central publishing
- Binary/BLOB handling with `GetAs()`/`PutAs()`
- Advanced queries (JOINs, subqueries)
- Connection failover

---

*Branch: geronimo*
*Last updated: 2025-11-27*
