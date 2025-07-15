# FileMakerHibernate6

FileMaker Hibernate v6.5+ dialect

## Description

FileMakerHibernate6 is a custom Hibernate dialect designed to facilitate seamless integration between Java applications and FileMaker databases. This project provides a robust framework for developers to leverage the power of Hibernate ORM while working with FileMaker's unique data structures and functionalities. With support for the latest FileMaker v6.5+ features, this dialect aims to simplify database interactions, enhance performance, and streamline the development process for Java developers using FileMaker as their backend.

## Features

- Compatibility tested with FileMaker 20 and 21 (probably running with older versions, but not checked to date)
- Support for most standard Hibernate features, FileMaker's jdbc driver offers minimal JDBC3 standard support
- Optimized for performance with FileMaker databases
- Limit handling support
- Custom identity support (limited by FileMaker's JDBC driver capabilities) 

## Installation

To install the FileMaker Hibernate 6 dialect, include the necessary dependencies in your project and configure your Hibernate settings to use this dialect.

Use the [maven_deploy_dialect.sh](./maven_deploy_dialect.sh) script to create a local repository for the dialect:

```bash
cd project_root
./maven_deploy_dialect.sh 21.0.2
```

Then configure your `pom.xml` by adding the dialect dependency:
```xml
    <dependency>
        <groupId>com.filemaker.hibernate.dialect</groupId>
        <artifactId>FileMakerDialect</artifactId>
        <version>21.0.2</version>
    </dependency>
```

To add the FileMaker JDBC driver to your project, use the [maven_deploy_driver.sh](./maven_deploy_driver.sh) script provided:
```bash
cd project_root
./maven_deploy_driver.sh 21.0.2
```

Then configure your `pom.xml` by adding the driver dependency:
```xml
    <!-- fmjdbc driver -->
    <dependency>
        <groupId>com.filemaker.jdbc.Driver</groupId>
        <artifactId>fmjdbc</artifactId>
        <version>21.0.1</version>
    </dependency>
```

## Usage (Spring Boot Example)

Set up your `application.yml` configuration as follows:

```yml
spring
  datasource:
    driver-class-name: com.filemaker.jdbc.Driver
    type: com.zaxxer.hikari.HikariDataSource
    url: jdbc:filemaker://filemaker_host/database_name
    username: your_username
    password: your_password
  jpa:
    database-platform: org.hibernate.community.dialect.FileMakerDialect
    show-sql: true # overwrite or set false in production
    properties:
      hibernate:
        javax:
          cache:
            missing_cache_strategy: create
        dialect: org.hibernate.community.dialect.FileMakerDialect
        ddl-auto: none
        id:
          new_generator_mappings: true
        connection:
          provider_disables_autocommit: false
        cache:
          use_second_level_cache: false
          use_query_cache: false
        generate_statistics: false
        format_sql: true
        use_sql_comments: false
        naming:
          physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
          implicit-strategy: org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy

```

I recommend using a connection pool, preferably HikariCP for optimal performance.

## Documentation

For more detailed information about FileMaker SQL features and driver capabilities, see the documentation in the [docs](./docs/) directory:

- [Driver Information](./docs/DriverInfo.md)
- [FileMaker SQL Data Types](./docs/FileMakerSQL-dataTypes.md)
- [FileMaker SQL Functions](./docs/FileMakerSQL-functions.md)
- [FileMaker SQL Pagination](./docs/FileMakerSQL-pagination.md)
- [FileMaker SQL Binary Data](./docs/FileMakerSQL-binaryData.md)