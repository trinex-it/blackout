# Blackout

A Spring Boot starter that provides JWT-based authentication and authorization with minimal configuration.

## Features

**Base URL**: All Blackout endpoints are prefixed with `/api` by default. Configure the base path via `blackout.base-url` property.

### Authentication Endpoints

Blackout provides ready-to-use REST endpoints for authentication:

#### POST {base-url}/auth/login

Authenticates users with email/password and returns JWT tokens.

**Request Body (`LoginRequestDTO`)**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "rememberMe": true
}
```

**Response (`AuthResponseDTO`)**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "access_token_expiration": 3600000,
  "refresh_token_expiration": 2592000000
}
```

#### POST {base-url}/auth/refresh

Refreshes an expired access token using a valid refresh token with automatic token rotation.

**Request Body (`RefreshRequestDTO`)**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (`AuthResponseDTO`)**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "access_token_expiration": 3600000,
  "refresh_token_expiration": 2592000000
}
```

#### GET {base-url}/auth/status

Returns information about the currently authenticated user.

**Response (`AuthStatusResponseDTO`)**:
```json
{
  "id": 1,
  "username": "user@example.com",
  "role": "USER",
  "firstName": "John",
  "lastName": "Doe"
}
```

### User Registration

#### POST {base-url}/signup

Optional basic registration endpoint (configurable via `blackout.signup.enabled`). Recommended to implement custom registration logic in your application for production scenarios and cross-database features, implementation example are shown below.

**Request Body (`SignupRequestDTO`)**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "confirmPassword": "SecurePassword123!"
}
```

**Response**: HTTP 201 Created (empty body)

**Note**: After registration, use the login endpoint to obtain authentication tokens.

### Security Configuration

- **Flexible Filter Chain** - Configure allowed and authenticated endpoints via `blackout.filterchain.allowed` and `blackout.filterchain.authenticated` properties
- **CORS Support** - Easily configure CORS policies with `blackout.cors.*` properties (origins, methods, headers, credentials)

### Multi-Database Architecture

- **Dual DataSource** - Automatic separation between authentication data and business data
- **Auth Database** - Managed by Blackout, stores `AuthAccount` entities (configured via `blackout.datasource.*`)
- **Primary Database** - Your application's business entities (configured via `spring.datasource.*`)
- **Independent JPA Contexts** - Separate `EntityManagerFactory` and `PlatformTransactionManager` for each database

### Custom User Principals

- **Extensible User Principal** - Extend `BlackoutUserPrincipal` to add custom user data
- **JWT Claims Integration** - Custom fields automatically included in JWT tokens
- **Type-Safe Access** - Use typed `CurrentUserService<T>` to access authenticated users in controllers
- **Principal Factory Pattern** - Reconstruct custom principals from JWT claims on each request

### JWT Configuration

- **Configurable Expiration** - Set access token and refresh token expiration times via `blackout.jwt.*` properties
- **Token Rotation** - Automatic refresh token rotation on every refresh for enhanced security
- **Secret Key** - Configure JWT signing key with `blackout.jwt.secret`

### OpenAPI Integration

- **Swagger UI** - Auto-generated API documentation available at `/swagger-ui/index.html`
- **Configurable Metadata** - Customize title, description, version, and contact info via `blackout.openapi.*` properties

### Auto-Configuration

All components are automatically configured via Spring Boot's auto-configuration mechanism:
- Security filter chain with JWT authentication
- Authentication provider with BCrypt password encoding
- JPA repositories and entity manager for auth database
- CORS configuration source

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
        return Map.of(
                "codice_fiscale", codiceFiscale
        );
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
    protected void applyCustomFields(Claims claims,
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

#### Step 4: Implement UserDetailsService

To load your custom principal from the database during authentication, implement a `UserDetailsService` that queries both the auth database and your primary database:

```java
@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final UtenteRepo utenteRepo;
    private final AuthAccountRepo authAccountRepo;

    @Override
    public BlackoutUserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Load AuthAccount from auth database
        AuthAccount authAccount = authAccountRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        // 2. Check if account is active
        if (!authAccount.isActive()) {
            throw new AccountNotActiveException("Account not active: " + username);
        }

        // 3. Load your business entity from primary database
        MyUser user = utenteRepo.findByAuthAccountId(authAccount.getId())
                .orElseThrow(() -> new UsernameNotFoundException(username));

        // 4. Build authorities
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        // 5. Return your custom principal with all fields
        return MyUserPrincipal.builder()
                // Default Blackout fields
                .id(authAccount.getId())
                .userId(utente.getId())
                .authorities(authorities)
                .username(username)
                .password(authAccount.getPasswordHash())
                // Your custom fields
                .codiceFiscale(utente.getCodiceFiscale())
                .piattoPreferito(utente.getPiattoPreferito())
                .build();
    }
}
```

**Important**: This service is only used during **initial authentication** (login). After login, the JWT token contains all necessary data and no database queries are needed for subsequent requests.

**Why**: Allows you to enrich your authenticated user with business data from your primary database while keeping authentication data separate.

#### Step 5: Use in Your Controllers

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
public class MyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String role;

    private String nome;
    private String email;
}

@Repository
public interface UtenteRepo extends JpaRepository<Utente, Long> {
}
```

**Why**: Standard Spring Data JPA entities and repositories work seamlessly with your primary data source.

**Note**: All repositories in the package specified in `@EnableJpaRepositories` will use the primary data source. Blackout repositories (`it.trinex.blackout.AuthAccountRepo`) automatically use the auth data source.
