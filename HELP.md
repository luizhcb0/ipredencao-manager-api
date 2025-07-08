# Read Me First
The following was discovered as part of building this project:

* The original package name 'org.ipredencao.ipredencao-manager' is invalid and this project uses 'org.ipredencao.ipredencao_manager' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.3/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.3/gradle-plugin/packaging-oci-image.html)
* [JOOQ Access Layer](https://docs.spring.io/spring-boot/3.5.3/reference/data/sql.html#data.sql.jooq)
* [Liquibase Migration](https://docs.spring.io/spring-boot/3.5.3/how-to/data-initialization.html#howto.data-initialization.migration-tool.liquibase)
* [Docker Compose Support](https://docs.spring.io/spring-boot/3.5.3/reference/features/dev-services.html#features.dev-services.docker-compose)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

### Docker Compose support
This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* postgres: [`postgres:latest`](https://hub.docker.com/_/postgres)

Please review the tags of the used images and set them to the same as you're running in production.

