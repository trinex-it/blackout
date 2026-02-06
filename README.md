# Blackout

A Spring Boot starter that provides JWT-based authentication and authorization with minimal configuration.

## Table of Contents

- [Features](#features)
  - [Authentication Endpoints](#authentication-endpoints)
  - [User Registration](#user-registration)
  - [Two-Factor Authentication](#two-factor-authentication)
  - [Security Configuration](#security-configuration)
  - [Multi-Database Architecture](#multi-database-architecture)
  - [Custom User Principals](#custom-user-principals)
  - [JWT Configuration](#jwt-configuration)
  - [OpenAPI Integration](#openapi-integration)
  - [Overrideable Beans](#overrideable-beans)
  - [Auto-Configuration](#auto-configuration)
- [Installation](#installation)
  - [Add the Maven Repository](#1-add-the-maven-repository)
  - [Add the Dependency](#2-add-the-dependency)
  - [Minimum Requirements](#3-minimum-requirements)
- [Configuration](#configuration)
  - [Defining and Using a Custom User Principal](#defining-and-using-a-custom-user-principal)
  - [Custom User Registration](#custom-user-registration)
  - [Minimal Configuration](#minimal-configuration)
  - [Complete Configuration Example](#complete-configuration-example)

## Features

**Base URL**: All Blackout endpoints are prefixed with `/api` by default. Configure the base path via `blackout.base-url` property.

### Authentication Endpoints

Blackout provides ready-to-use REST endpoints for authentication:

#### POST {base-url}/auth/login

Authenticates users with email/username and password, and returns JWT tokens.

**Request Body (`LoginRequestDTO`)**:
```json
{
  "subject": "user@example.com",
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

Optional basic registration endpoint (configurable via `blackout.signup.enabled`). Recommended to implement custom registration logic in your application for production scenarios and cross-database features, implementation example is shown at [Custom User Registration](#custom-user-registration)

**Request Body (`SignupRequestDTO`)**:
```json
{
  "email": "user@example.com",
  "username": "myUsername03",
  "password": "SecurePassword123!",
  "confirmPassword": "SecurePassword123!"
}
```

**Response**: HTTP 201 Created (empty body)

**Note**: After registration, use the login endpoint to obtain authentication tokens.

### Two-Factor Authentication

Blackout provides TOTP-based (Time-based One-Time Password) two-factor authentication using authenticator apps like Google Authenticator, Authy, or similar.

#### GET {base-url}/2fa

Generates a TOTP secret key and QR code for setting up two-factor authentication.

**Response (`TOTPRegistrationResponse`)**:
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrURI": "otpauth://totp/MyApp:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=MyApp"
}
```

**Flow**:
1. User calls this endpoint to get the secret and QR code
2. User scans the QR code with their authenticator app
3. User calls POST `/api/2fa` with the secret and a generated TOTP code to complete setup

**Response Codes**:
- `200` - TOTP secret generated successfully
- `401` - User not authenticated
- `409` - 2FA is already enabled for this user

#### POST {base-url}/2fa

Enables two-factor authentication for the current user by validating a TOTP code.

**Request Body (`Enable2FARequest`)**:
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "totp": "123456"
}
```

**Response (`TFAEnabledResponse`)**:
```json
{
  "recoveryCodes": [
    "ABCD-1234-EFGH-5678",
    "IJKL-9012-MNOP-3456",
    "QRST-7890-UVWX-1234",
    "YZAB-4567-CDEF-8901",
    "GHIJ-2345-KLMN-6789",
    "OPQR-0123-STUV-3456",
    "WXYZ-7890-ABCD-1234",
    "EFGH-5678-IJKL-9012"
  ]
}
```

**Important**: Save the recovery codes securely! They can be used to access your account if you lose access to your authenticator app. Each code can only be used once.

**Response Codes**:
- `200` - 2FA enabled successfully
- `400` - Invalid TOTP code or secret
- `401` - User not authenticated

#### POST {base-url}/2fa/disable

Disables two-factor authentication for the current user.

**Request Body (`Disable2FA`)**:
```json
{
  "code": "123456"
}
```

**Response**: HTTP 200 OK (empty body)

**Response Codes**:
- `200` - 2FA disabled successfully
- `400` - Invalid TOTP code
- `401` - User not authenticated

**Note**: The endpoint requires a valid TOTP code to ensure the user has access to their authenticator app before disabling 2FA. Once disabled, the user will only need email and password to log in.

#### POST {base-url}/2fa/disable-recovery

Disables two-factor authentication using a recovery code. Use this endpoint when you've lost access to your authenticator app.

**Request Body (`Disable2FAWithRecoveryRequest`)**:
```json
{
  "subject": "user@example.com",
  "password": "SecurePassword123!",
  "recoveryCode": "ABCD-1234-EFGH-5678"
}
```

**Response**: HTTP 200 OK (empty body)

**Response Codes**:
- `200` - 2FA disabled successfully
- `400` - Invalid credentials or recovery code
- `401` - Authentication failed

**Important**:
- The recovery code is invalidated after use and cannot be reused
- This endpoint validates both your credentials and the recovery code
- Keep remaining recovery codes safe after using this endpoint
- Consider re-enabling 2FA after regaining access to your authenticator app

**Integration with Login**:

When 2FA is enabled, the login endpoint behavior changes:

1. **First login attempt** (without TOTP code):
   ```json
   POST {base-url}/auth/login
   {
     "email": "user@example.com",
     "password": "SecurePassword123!"
   }
   ```
   **Response**: HTTP 202 Accepted
   ```json
   {
     "needOTP": true
   }
   ```

2. **Second login attempt** (with TOTP code):
   ```json
   POST {base-url}/auth/login
   {
     "email": "user@example.com",
     "password": "SecurePassword123!",
     "totpCode": "123456"
   }
   ```
   **Response**: HTTP 200 OK
   ```json
   {
     "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "access_token_expiration": 3600000,
     "refresh_token_expiration": 2592000000,
     "needOTP": false
   }
   ```


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

### Overrideable Beans

Blackout provides sensible defaults for all its core components through Spring's `@ConditionalOnMissingBean` annotation. This means you can override any bean by simply declaring your own in a configuration class.

#### How to Override Beans

Create a configuration class (similar to `BlackoutConfig.java` in the test application) and declare the beans you want to customize:

```java
@Configuration
public class MyCustomConfig {

    // Example: Override the default PasswordEncoder
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Custom strength
    }

    // Example: Override the default SecurityFilterChain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

#### Complete List of Overrideable Beans

All beans listed below can be overridden by declaring your own `@Bean` method with the same return type or bean name:

**Security & Authentication:**
- `SecurityFilterChain` - Configures HTTP security rules, filter chains, and endpoint permissions
- `AuthenticationProvider` - Custom authentication logic (e.g., integrate with external auth services)
- `AuthenticationManager` - Manages authentication requests
- `PasswordEncoder` - Password hashing algorithm (default: BCrypt)
- `CorsConfigurationSource` - CORS policies and allowed origins/methods/headers

**JWT & User Management:**
- `BlackoutPrincipalFactory` - Factory for reconstructing user principals from JWT claims
- `CurrentUserService<P>` - Type-safe service for accessing the current authenticated user in controllers

**Auth Database (`blackout.datasource.*`):**
- `blackoutDataSource` - DataSource for authentication database
- `blackoutEntityManager` - JPA EntityManagerFactory for auth entities
- `blackoutTransactionManager` - PlatformTransactionManager for auth database operations

**Primary Database (`spring.datasource.*` / `blackout.parent.datasource.*`):**
- `parentDataSource` - DataSource for your application's business database
- `entityManagerFactory` - JPA EntityManagerFactory for business entities
- `parentTransactionManager` - PlatformTransactionManager for primary database (marked as `@Primary`)

**Controllers:**
- `authController` - REST controller for `/api/auth/login`, `/api/auth/refresh`, `/api/auth/status`

**OpenAPI/Swagger:**
- `customOpenAPI` - OpenAPI metadata (title, description, version, license, contact info)
- `groupedOpenAPI` - GroupedOpenApi configuration for path matching and documentation

**Note:** When overriding beans that are used by other Blackout components, ensure your implementation maintains compatibility with the expected interfaces and contracts.

### Auto-Configuration

All components are automatically configured via Spring Boot's auto-configuration mechanism:
- Security filter chain with JWT authentication
- Authentication provider with BCrypt password encoding
- JPA repositories and entity manager for auth database
- CORS configuration source

## Installation

### 1. Add the Dependency

Add the Blackout dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>it.trinex</groupId>
    <artifactId>blackout</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 2. Minimum Requirements

- Java 21+
- Spring Boot 4.0.2+
- MySQL/PostgreSQL/H2 database

## Configuration

### Defining and Using a Custom User Principal

To add custom user data to your JWT tokens and access it in your controllers, follow these three steps:

#### Step 1: Create Your Custom Principal

Extend `BlackoutUserPrincipal` and add custom fields. Override `getExtraClaims()` to include these fields in JWT tokens:

```java

@Getter
@SuperBuilder
public class MyUserPrincipal extends BlackoutUserPrincipal {

  private String taxCode;

  @Override
  public Map<String, Object> getExtraClaims() {
    return Map.of(
            "tax_code", taxCode
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

    private final UserRepo userRepo;
    private final AuthAccountRepo authAccountRepo;

    @Override
    public BlackoutUserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Load AuthAccount from auth database
        AuthAccount authAccount = authAccountRepo.findByUsername(username).orElse(
                authAccountRepo.findByEmail(username)
                        .orElseThrow(() -> new UsernameNotFoundException(username))
        );

        // 2. Check if account is active
        if (!authAccount.isActive()) {
            throw new AccountNotActiveException("Account not active: " + username);
        }

        // 3. Load your business entity from primary database
        MyUser user = userRepo.findByAuthAccountId(authAccount.getId())
                .orElseThrow(() -> new UsernameNotFoundException(username));

        // 4. Build authorities
        List<SimpleGrantedAuthority> authorities = utente.getRuoli().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toString()))
                .toList();

        // 5. Return your custom principal with all fields
        return MyUserPrincipal.builder()
                // Default Blackout fields
                .id(authAccount.getId())
                .userId(user.getId())
                .authorities(authorities)
                .username(username)
                .password(authAccount.getPasswordHash())
                // Your custom fields
                .taxCode(user.getTaxCode())
                .piattoPreferito(user.getPiattoPreferito())
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
        return user.getTaxCode();
    }
}
```

**Why**: Provides type-safe, direct access to your custom authenticated user throughout your application.

### Custom User Registration

While Blackout provides a basic signup endpoint, production applications typically require custom registration logic that handles business-specific data and cross-database operations. This section explains how to implement a complete custom signup flow.

#### Why Custom Signup?

Blackout's default signup endpoint only creates an `AuthAccount` in the auth database. For real-world applications, you typically need to:

1. **Collect additional user data** (tax code, address, preferences, etc.)
2. **Create business entities** in your primary database
3. **Link authentication and business data** via foreign key relationships
4. **Handle cross-database transactions** manually (different databases = different transaction managers)

#### Architecture

The custom signup flow involves two databases:
- **Auth Database**: Stores `AuthAccount` (email, password hash)
- **Primary Database**: Stores your business entity (e.g., `User`, `Customer`) with an `authAccountId` foreign key

#### Step 1: Create Custom Request DTO

Define a DTO that collects all the data you need for registration:

```java
import jakarta.validation.constraints.NotBlank;

@Data
public class MySignupRequestDTO {

  @NotBlank
  @Email
  private String email;

  @NotBlank
  private String username;

  @NotBlank
  @Size(min = 8, max = 100)
  private String password;

  @NotBlank
  private String confirmPassword;

  @NotBlank
  private String firstName;

  @NotBlank
  private String lastName;

  @NotBlank
  private String taxCode; // Business-specific field
}
```

**Why**: Collects both authentication credentials (email/password) and business data (name, tax code) in a single request.

#### Step 2: Create the Signup Controller

Create a REST endpoint that handles the signup request:

```java
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid MySignupRequestDTO dto) {
        userService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
```

**Why**: Provides a clean REST API endpoint for user registration.

#### Step 3: Implement the Signup Service with Manual Transaction Management

The service must handle two separate database operations as a single unit of work:

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthService authService;
    private final AuthAccountRepo authAccountRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo; // Your business repository

    @Transactional
    public void signup(MySignupRequestDTO request) {
        AuthAccount authAccount = null;

        try {
            // 1. Create AuthAccount in auth database
            authAccount = authService.registerAuthAccount(
                AuthAccount.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .isActive(true)
                    .build()
            );

            // 2. Create business entity in primary database
            User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())    
                .email(request.getEmail())
                .taxCode(request.getTaxCode())
                .authAccountId(authAccount.getId()) // ← CRITICAL: Link to AuthAccount
                .build();

            userRepo.save(user);

        } catch (Exception e) {
            // 3. Manual rollback: delete AuthAccount if User creation fails
            if (authAccount != null && authAccount.getId() != null) {
                authAccountRepo.deleteById(authAccount.getId());
            }
            throw e; // Re-throw to let caller handle the error
        }
    }
}
```

**Key Points**:

1. **`authService.registerAuthAccount()`**: Blackout's helper method to create `AuthAccount` with validation
2. **`authAccountId` foreign key**: Links your business entity to the auth account - this is how you'll load user data during login
3. **Manual rollback**: Since we're dealing with two databases with separate transaction managers, we must manually clean up the `AuthAccount` if the business entity creation fails
4. **`@Transactional`**: Only applies to the primary database transaction. The auth database operation happens in its own transaction via `AuthService`.

#### Step 4: Update UserDetailsService to Use the Foreign Key

When implementing your custom `UserDetailsService` (see "Defining and Using a Custom User Principal" section), use the `authAccountId` to load the business entity:

```java
@Service
@RequiredArgsConstructor
@Primary // IMPORTANT: Override default implementation
public class MyUserDetailsService implements UserDetailsService {

    private final AuthAccountRepo authAccountRepo;
    private final UserRepo userRepo;

    @Override
    public BlackoutUserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Load AuthAccount from auth database
        AuthAccount authAccount = authAccountRepo.findByUsername(username).orElse(
                authAccountRepo.findByEmail(username)
                        .orElseThrow(() -> new UsernameNotFoundException(username))
        );
      
        // 2. Load business entity using authAccountId foreign key
        User user = userRepo.findByAuthAccountId(authAccount.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found in primary database"));

        // 3. Build custom principal with data from both databases
        return MyUserPrincipal.builder()
                .id(authAccount.getId())
                .userId(user.getId())
                .username(authAccount.getUsername())
                .password(authAccount.getPasswordHash())
                .authorities(user.getRolesAsAuthorities())
                .taxCode(user.getTaxCode()) // Custom field from business entity
                .build();
    }
}
```

**Why**: Loads complete user data from both databases during login, stores it in JWT, eliminates cross-database queries on subsequent requests.

#### Step 5: Configure Public Access for Signup Endpoint

Add your custom signup endpoint to the allowed endpoints list:

```yaml
blackout:
  filterchain:
    allowed:
      - "/user/**" # Allow access to custom signup endpoint
```

**Or** for more granular control:

```yaml
blackout:
  filterchain:
    allowed:
      - "/user/signup" # Allow only signup endpoint
```

#### Cross-Database Transaction Considerations

When working with separate databases:

1. **No distributed transactions**: Spring's `@Transactional` only works for a single database. Two databases = two separate transactions.

2. **Manual rollback pattern**: If the second operation fails, manually undo the first operation (as shown in the try-catch block above).

3. **Cleanup jobs**: Consider implementing a cleanup job that removes orphaned `AuthAccount` records (auth accounts without corresponding business entities).

#### Cross-Database Transaction Rules

When working with `authAccountRepo` and `AuthAccount` entities, always specify the transaction manager:

```java
// For auth database (REQUIRED)
@Transactional("blackoutTransactionManager")
public void authOperation() {
    authAccountRepo.save(...);
}

// For primary database (default)
@Transactional
public void businessOperation() {
    userRepository.save(...);
}
```

**Important**: A single `@Transactional` method cannot manage transactions across both databases. You must create separate methods:
- One method annotated with `@Transactional` for primary database operations
- One method annotated with `@Transactional("blackoutTransactionManager")` for auth database operations

Then call these methods from a non-transactional coordinator method that handles manual rollback if needed (as shown in the example above).


### Minimal Configuration

For a quick setup with sensible defaults, you only need to configure these required properties in your `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/app # Primary database URL
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

blackout:
  parent: # !!REQUIRED!! Config to access and configure primary datasource
    datasource:
      repository: com.my.app.repository # JPA repositories package
      model: com.my.app.model # JPA models package

  jwt: # !!REQUIRED!! JWT configuration
    secret: myverylongsecretthatshouldabsolutelybearandomgeneratedstring # JWT secret key (should be base64-encoded in production)

  openapi:
    paths-to-match:
      - "/api/**"
```

**Defaults applied**: Blackout will automatically use the primary application database for authentication if `blackout.datasource.*` is not configured, use `/api` as base URL, enable CORS with open access.

**Publicly accessible endpoints** (no authentication required):
- `{base-url}/auth/**` - Authentication endpoints (login, refresh, status)
- `/error` - Error page
- `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html` - Swagger/OpenAPI documentation
- `{base-url}/signup` - User registration (only if `blackout.signup.enabled=true`)

### Complete Configuration Example

Here's a complete `application.yml` example showing all Blackout configuration options:

```yaml
server:
  port: 8090 # Server port [8080]

spring:
  application:
    name: blackout-test # Application name
  datasource:
    url: jdbc:mysql://localhost:3306/app # Primary database URL
    username: root # Database username
    password: root # Database password
    driver-class-name: com.mysql.cj.jdbc.Driver # MySQL JDBC driver

blackout:
  base-url: /api # Base URL for all API endpoints [/api]

  # !!REQUIRED!! Config to access and configure primary datasource
  parent:
    datasource:
      repository: com.my.app.repository # JPA repositories package
      model: com.my.app.model # JPA models package
      jpaProperties:
        hibernate.hbm2ddl.auto: update # Hibernate DDL mode [update]
        hibernate.format_sql: true # Format SQL queries [true]

  # Config for alternative auth database
  datasource:
    url: jdbc:mysql://localhost:3306/auth # Auth database URL
    username: root # Auth database username
    password: root # Auth database password
    driver-class-name: com.mysql.cj.jdbc.Driver # MySQL JDBC driver for auth database

  # !!REQUIRED!! JWT configuration
  jwt:
    access-token-exp: 900000 # Expiration time for the access token (15 min) [900000 (15 min)]
    refresh-token-exp-no-remember: 3600000 # Refresh token expiration when "remember me" is false (1 hour) [3600000 (1 hour)]
    refresh-token-exp: 2592000000 # Refresh token expiration when "remember me" is true (30 days) [2592000000 (30 days)]
    secret: myverylongsecretthatshouldabsolutelybearandomgeneratedstring # JWT secret key (should be base64-encoded in production)

  # CORS configuration
  cors:
    allow-credentials: false # Allow credentials (cookies) in CORS requests [false]
    allowed-headers: # Allowed headers in CORS requests [*]
      - "*"
    allowed-methods: # Allowed HTTP methods in CORS requests [*]
      - "*"
    allowed-origins: # Allowed origins for CORS (use specific domains in production) [*]
      - "*"

  # Security filter chain rules
  filterchain:
    allowed: # Endpoints accessible without authentication []
      - "/everyone/**"
    authenticated: # Endpoints requiring authentication []
      - "/showidplease/**"

  # OpenAPI/Swagger configuration
  openapi:
    enabled: true # Enable Swagger/OpenAPI documentation [true]
    title: "Blackout Test API" # API title
    description: "Test application for Blackout Spring Boot starter" # API description
    version: "1.0.0" # API version
    contact-name: "Trinex Development Team" # Contact name
    contact-email: "hello@trinex.it" # Contact email
    license-name: "Apache 2.0" # License name
    license-url: "https://www.apache.org/licenses/LICENSE-2.0.html" # License URL
    paths-to-match: # !!REQUIRED!! Paths to include in OpenAPI documentation 
      - "/api/**"

  # User registration configuration
  signup:
    enabled: false # Enable default user registration endpoint [false]
    default-role: USER # Default role for new users [USER]
```

**Important Notes**:
- `blackout.parent.datasource.repository` and `blackout.parent.datasource.model` are **required** for Blackout to configure your primary data source
- `blackout.datasource.*` configures the separate authentication database
- `blackout.jwt.secret` should be a strong, randomly generated string in production (preferably base64-encoded)
- Use specific domains instead of `"*"` for `blackout.cors.allowed-origins` in production
