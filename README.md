# Blackout

A Spring Boot starter that provides JWT-based authentication and authorization with minimal configuration.

## Features

- 🔐 JWT-based stateless authentication
- 🚀 Auto-configuration for quick setup
- 📊 OpenAPI/Swagger UI integration
- 🔒 Configurable security rules
- 👥 User registration and login endpoints
- 🔄 Token refresh mechanism
- 🎯 Custom user principal support
- 🗄️ Separate auth database schema

## Installation

### 1. Add the Maven Repository

Add the Gitea Maven repository to your project's `pom.xml`:

```xml
<repositories>
    <repository>
        <id>gitea</id>
        <url>https://lab.0tb.it/api/packages/trinex/maven</url>
        <snapshots>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```

### 2. Add the Dependency

Add the Blackout dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>it.trinex</groupId>
    <artifactId>blackout</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3. Minimum Requirements

- Java 21+
- Spring Boot 4.0.2+
- MySQL/PostgreSQL/H2 database

## Configuration

### Defining and Using a Custom User Principal

To add custom user data to your JWT tokens and access it in your controllers, follow these three steps:

#### Step 1: Create Your Custom Principal

Extend `BlackoutUserPrincipal` and add custom fields. Override `getExtraClaims()` to include these fields in JWT tokens:

```java
@SuperBuilder
public class MyUserPrincipal extends BlackoutUserPrincipal {

    private String codiceFiscale;

    @Override
    public Map<String, Object> getExtraClaims() {
        return Map.of("codice_fiscale", codiceFiscale);
    }
}
```

**Why**: Custom fields are automatically included in JWT tokens during authentication and available on every request without database queries.

#### Step 2: Create a Principal Factory

Implement the factory that reconstructs your custom principal from JWT claims:

```java
@Component
public class MyPrincipalFactory extends AbstractBlackoutPrincipalFactory<MyUserPrincipal> {

    @Override
    protected BlackoutUserPrincipal.BlackoutUserPrincipalBuilder<?, ?> getBuilder() {
        return MyUserPrincipal.builder();
    }

    @Override
    protected void applyCustomFields(Claims claims, Collection<? extends GrantedAuthority> authorities,
                                      BlackoutUserPrincipal.BlackoutUserPrincipalBuilder<?, ?> builder) {
        MyUserPrincipalBuilder<?, ?> myBuilder = (MyUserPrincipalBuilder<?, ?>) builder;
        // Repeat this for all the extra fields
        myBuilder.codiceFiscale(claims.get("codice_fiscale", String.class));
    }
}
```

**Why**: Automatically extracts custom fields from JWT claims and rebuilds your principal on each authenticated request.

#### Step 3: Configure CurrentUserService Bean

Create a typed `CurrentUserService` bean to access the authenticated user:

```java
@Configuration
public class BlackoutConfig {

    @Bean
    public CurrentUserService<MyUserPrincipal> currentUserService() {
        return new CurrentUserService<>();
    }
}
```

#### Step 4: Use in Your Controllers

Inject and use the typed service to access the current user:

```java
@RestController
@RequiredArgsConstructor
public class MyController {

    private final CurrentUserService<MyUserPrincipal> currentUserService;

    @GetMapping("/me")
    public MyUserPrincipal getCurrentUser() {
        return currentUserService.getCurrentPrincipal();
    }

    @GetMapping("/my-tax-code")
    public String getTaxCode() {
        MyUserPrincipal user = currentUserService.getCurrentPrincipal();
        return user.getCodiceFiscale();
    }
}
```

**Why**: Provides type-safe, direct access to your custom authenticated user throughout your application.

### Configuring the Primary Data Source

Blackout manages its own authentication database separately. Your application needs its own primary data source for business entities.

#### Architecture

- **Blackout Auth Database**: Stores `AuthAccount` entities (configured via `blackout.datasource.*`)
- **Primary Database**: Your application's business entities (configured via `spring.datasource.*`)

#### Step 1: Configure Primary Data Source in application.yml

```yaml
spring:
  datasource:
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    jdbc-url: jdbc:mysql://localhost:3306/app  # Your business database
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

blackout:
  datasource:
    url: jdbc:mysql://localhost:3306/auth  # Blackout auth database (separate)
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    jpa:
      hibernate:
        ddl-auto: update
      dialect: org.hibernate.dialect.MySQLDialect
```

**Why**: Separates authentication data from business data for cleaner architecture and security isolation.

#### Step 2: Create Primary JPA Configuration

```java
@Configuration
@EnableJpaRepositories(
    basePackages = {"com.example.app"},  // Your repository package
    entityManagerFactoryRef = "primaryEntityManager",
    transactionManagerRef = "primaryTransactionManager"
)
public class PrimaryJpaConfig {

    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "primaryEntityManager")
    public LocalContainerEntityManagerFactoryBean primaryEntityManager(
            @Qualifier("primaryDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.example.app.model");  // Your entity package

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        // JPA properties
        emf.getJpaPropertyMap().put("hibernate.hbm2ddl.auto", "update");
        emf.getJpaPropertyMap().put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        emf.getJpaPropertyMap().put("hibernate.format_sql", "true");

        return emf;
    }

    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManager") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

**Why**: Creates a separate JPA context for your business entities, independent from Blackout's authentication context.

#### Step 3: Create Your Business Entities and Repositories

```java
@Entity
@Data
public class Utente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
}

@Repository
public interface UtenteRepo extends JpaRepository<Utente, Long> {
}
```

**Why**: Standard Spring Data JPA entities and repositories work seamlessly with your primary data source.

**Note**: All repositories in the package specified in `@EnableJpaRepositories` will use the primary data source. Blackout repositories (`it.trinex.blackout.AuthAccountRepo`) automatically use the auth data source.
