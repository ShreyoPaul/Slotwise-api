# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.2.0/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.2.0/maven-plugin/build-image.html)
* [Validation](https://docs.spring.io/spring-boot/4.2.0/reference/io/validation.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.2.0/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Web](https://docs.spring.io/spring-boot/4.2.0/reference/web/servlet.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Validation](https://spring.io/guides/gs/validating-form-input/)
* [Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Local MySQL with Docker

From the project directory, create a local environment file and fill in unique values for
`MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_ROOT_PASSWORD`:

```powershell
Copy-Item .env.example .env
# Edit .env locally; do not commit it.
docker compose up -d
docker compose ps
```

The database is available at `localhost:3306` (database name: `slotwise`). Open
[http://localhost:8080](http://localhost:8080) to use Adminer. Log in with system
`MySQL`, server `mysql`, and the username and password you set in `.env`.

Docker Compose reads `.env` to configure the MySQL container. Spring Boot does not
automatically read that file, so set its database credentials in the process environment
before starting the app. In PowerShell, load just the app's credentials from `.env`:

```powershell
$env:DB_USERNAME = ((Get-Content .env | Where-Object { $_ -match '^MYSQL_USER=' } | Select-Object -First 1) -replace '^MYSQL_USER=', '')
$env:DB_PASSWORD = ((Get-Content .env | Where-Object { $_ -match '^MYSQL_PASSWORD=' } | Select-Object -First 1) -replace '^MYSQL_PASSWORD=', '')
.\mvnw.cmd spring-boot:run
```

The app requires `DB_USERNAME` and `DB_PASSWORD`; it has no checked-in credential
fallback. Keep `.env` private and use your deployment platform's secret store for
production. Environment variables keep credentials out of source control, but they are
still available to processes with access to the host or container runtime.

To stop MySQL while keeping its data, run `docker compose down`. To remove the database and its persisted data, run `docker compose down -v`.

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.
