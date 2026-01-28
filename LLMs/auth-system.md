---
trigger: model_decision
description: Use this document whenever working on authentication-related features or when asked about the authentication system
---

# Authentication System Documentation

This document provides a complete reference for the JWT-based authentication system implemented in this Spring Boot application. It is designed to be replicable as a standalone Maven package.

## Table of Contents

1. [Technology Stack](#1-technology-stack)
2. [Architecture Overview](#2-architecture-overview)
3. [Core Components](#3-core-components)
4. [Authentication Flow](#4-authentication-flow)
5. [Token Structure](#5-token-structure)
6. [Security Configuration](#6-security-configuration)
7. [Data Models](#7-data-models)
8. [API Endpoints](#8-api-endpoints)
9. [Exception Handling](#9-exception-handling)
10. [Maven Dependencies](#10-maven-dependencies)
11. [Configuration](#11-configuration)
12. [LLM Guidelines](#12-llm-guidelines)

---

## 1. Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Java | JDK | 21 |
| Spring Boot | Starter | 3.5.7 |
| Spring Security | Starter | 3.5.7 |
| JWT Library | JJWT | 0.12.5 |
| Password Encoder | BCrypt | (Spring Security default) |
| Database | PostgreSQL | - |
| ORM | Spring Data JPA / Hibernate | - |
| Cache | Redis | - |
| Rate Limiting | Bucket4j | 8.15.0 |
| Validation | Jakarta Validation | - |

---

## 2. Architecture Overview

The authentication system follows a **stateless JWT-based architecture**:

```
┌─────────────┐      Login/Refresh       ┌──────────────────┐
│   Client    │ ──────────────────────>  │  AuthController  │
│ (Frontend)  │ <──────────────────────  │                  │
└─────────────┘      Tokens (Cookies)     └────────┬─────────┘
       │                                         │
       │                                         │
       │          ┌──────────────────────────────┘
       │          │
       ▼          ▼
┌──────────────────────────────────────────────────┐
│              Security Filter Chain               │
│  ┌─────────────────────────────────────────┐    │
│  │  1. JwtAuthenticationFilter             │    │
│  │     (extracts & validates JWT token)    │    │
│  └─────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────┐    │
│  │  2. Authorization Rules                  │    │
│  │     (permitAll/authenticated)           │    │
│  └─────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
       │
       │ Valid Token
       ▼
┌──────────────────────────────────────────────────┐
│         SecurityContext Authentication           │
│  ┌─────────────────────────────────────────┐    │
│  │  JWTUserPrincipal                       │    │
│  │  - id, username, ownerId                │    │
│  │  - authorities (ROLE_OWNER, ROLE_ADMIN) │    │
│  └─────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
       │
       │ Controllers/Services can access
       ▼
┌──────────────────────────────────────────────────┐
│         CurrentUserService                       │
│  - getCurrentAuthAccountId()                    │
│  - getCurrentOwnerId()                          │
│  - getCurrentAuthAccount()                      │
│  - getCurrentOwner()                            │
└──────────────────────────────────────────────────┘
```

---

## 3. Core Components

### 3.1 Security Configuration

**File:** `config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/auth/status").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/**").authenticated())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 10 rounds
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**Key Points:**
- Stateless session management (`SessionCreationPolicy.STATELESS`)
- JWT filter added before `UsernamePasswordAuthenticationFilter`
- Custom `UserDetailsService` implementation
- BCrypt password encoder
- CORS configured for specific origins

### 3.2 JWT Service

**File:** `service/JWTService.java`

**Responsibilities:**
- Generate access tokens
- Generate refresh tokens
- Validate access and refresh tokens
- Extract user principal from tokens
- Calculate token expiration times

**Key Methods:**

| Method | Description |
|--------|-------------|
| `generateAccessToken(JWTUserPrincipal)` | Creates a new access token |
| `generateRefreshToken(JWTUserPrincipal)` | Creates a new refresh token |
| `isTokenValid(String)` | Validates an access token |
| `isRefreshTokenValid(String)` | Validates a refresh token |
| `extractUserPrincipal(String)` | Reconstructs user principal from token |
| `getCurrentUser()` | Gets authenticated user from SecurityContext |
| `extractUsername(String)` | Extracts email from token |
| `extractUserId(String)` | Extracts user ID from token |
| `extractRole(String)` | Extracts role from token |
| `extractExpiration(String)` | Extracts expiration date |
| `extractJti(String)` | Extracts JWT ID for revocation |

**Token Signing:**
- Algorithm: HS256
- Secret key: Base64-decoded from configuration
- Signature: `Keys.hmacShaKeyFor(keyBytes)`

### 3.3 JWT Authentication Filter

**File:** `security/JwtAuthenticationFilter.java`

**Responsibilities:**
- Extract JWT from `access_token` cookie
- Validate token using `JWTService`
- Create `UsernamePasswordAuthenticationToken`
- Set authentication in `SecurityContextHolder`

**Execution Order:** `OncePerRequestFilter` before `UsernamePasswordAuthenticationFilter`

### 3.4 User Details Service

**File:** `service/CustomUserDetailsService.java`

**Responsibilities:**
- Load user by email (not username!)
- Check account active status
- Create `JWTUserPrincipal` with authorities
- Fetch owner profile for OWNER type accounts

**Note:** Although the method is `loadUserByUsername`, it actually loads by **email**.

### 3.5 JWT User Principal

**File:** `security/JWTUserPrincipal.java`

**Type:** Java Record implementing `UserDetails`

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | AuthAccount ID |
| `username` | String | Email address |
| `password` | String | Password hash (for validation) |
| `ownerId` | Optional\<Long\> | Owner profile ID (if applicable) |
| `firstName` | String | User's first name |
| `lastName` | String | User's last name |
| `authorities` | Collection\<? extends GrantedAuthority\> | Spring Security authorities |

**All UserDetails methods return `true` except:**
- `getPassword()` - returns the password hash
- `isEnabled()` - implicitly checked before principal creation

### 3.6 Current User Service

**File:** `service/CurrentUserService.java`

**Purpose:** Service layer access to current authenticated user

**Methods:**
```java
public Long getCurrentAuthAccountId()         // Returns AuthAccount ID
public Optional<Long> getCurrentOwnerId()      // Returns Owner ID (if any)
public AuthAccount getCurrentAuthAccount()     // Returns AuthAccount entity
public Optional<Owner> getCurrentOwner()       // Returns Owner entity
```

**Important:** Do NOT pass `ownerId` or `authAccountId` as parameters when referring to the currently logged-in user. Always use this service.

---

## 4. Authentication Flow

### 4.1 Login Flow

```
┌─────────┐                        ┌──────────────────────┐
│ Client  │                        │ AuthenticationController│
└────┬────┘                        └──────┬───────────────┘
     │                                   │
     │ POST /api/auth/login              │
     │ {email, password, rememberMe}     │
     ├──────────────────────────────────>│
     │                                   │
     │                                   │ 1. AuthenticationManager.authenticate()
     │                                   │    - Uses CustomUserDetailsService
     │                                   │    - Validates with BCrypt
     │                                   │
     │                                   │ 2. Extract JWTUserPrincipal
     │                                   │
     │                                   │ 3. JWTService.generateTokens()
     │                                   │
     │<──────────────────────────────────┤
     │ Set-Cookie: access_token=...       │
     │ Set-Cookie: refresh_token=...      │
     │ {expiresIn: 3600000}               │
     │                                   │
```

### 4.2 Authentication Request Flow

```
┌─────────┐                        ┌──────────────────────────┐
│ Client  │                        │ JwtAuthenticationFilter  │
└────┬────┘                        └──────┬───────────────────┘
     │                                   │
     │ Request with access_token cookie  │
     ├──────────────────────────────────>│
     │                                   │
     │                                   │ 1. Extract token from cookie
     │                                   │ 2. JWTService.isTokenValid()
     │                                   │ 3. JWTService.extractUserPrincipal()
     │                                   │ 4. Create Authentication object
     │                                   │ 5. SecurityContext.setAuthentication()
     │                                   │
     │<──────────────────────────────────┤
     │ Proceed to Controller             │
     │                                   │
```

### 4.3 Token Refresh Flow

```
┌─────────┐                        ┌──────────────────────┐
│ Client  │                        │ AuthenticationController│
└────┬────┘                        └──────┬───────────────┘
     │                                   │
     │ POST /api/auth/refresh            │
     │ Cookie: refresh_token=...         │
     ├──────────────────────────────────>│
     │                                   │
     │                                   │ 1. JWTService.isRefreshTokenValid()
     │                                   │ 2. JWTService.extractUserPrincipal()
     │                                   │    (no DB call!)
     │                                   │ 3. Generate new access token
     │                                   │ 4. Update cookies
     │                                   │
     │<──────────────────────────────────┤
     │ Set-Cookie: access_token=...       │
     │ {expiresIn: 3600000}               │
     │                                   │
```

### 4.4 Logout Flow

```
┌─────────┐                        ┌──────────────────────┐
│ Client  │                        │ AuthenticationController│
└────┬────┘                        └──────┬───────────────┘
     │                                   │
     │ POST /api/auth/logout             │
     ├──────────────────────────────────>│
     │                                   │
     │                                   │ 1. Create cookies with maxAge=0
     │                                   │ 2. Return Set-Cookie headers
     │                                   │
     │<──────────────────────────────────┤
     │ Set-Cookie: access_token=; Max-Age=0
     │ Set-Cookie: refresh_token=; Max-Age=0
     │                                   │
```

---

## 5. Token Structure

### 5.1 JWT Claims

All JWT tokens contain the following claims:

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | String | Username (email) |
| `jti` | String | JWT ID (UUID for revocation) |
| `uid` | Long | AuthAccount ID |
| `role` | String | AuthAccountType (OWNER/ADMIN) |
| `ownerId` | Long (nullable) | Owner profile ID |
| `firstName` | String | User's first name |
| `lastName` | String | User's last name |
| `tokenType` | String | "ACCESS" or "REFRESH" |
| `iat` | Date | Issued at |
| `exp` | Date | Expiration |

### 5.2 Token Expiration (Configurable)

**Access Tokens:**
| Role | Expiration | Example Value |
|------|------------|---------------|
| OWNER | Configurable | 15 minutes (3600000 ms) |
| ADMIN | Configurable | 15 minutes (3600000 ms) |

**Refresh Tokens:**
| Role | Expiration | Example Value |
|------|------------|---------------|
| OWNER | Configurable | 90 days (7776000000 ms) |
| ADMIN | Configurable | 7 days (604800000 ms) |

### 5.3 Token Storage

Tokens are stored in **HTTP-only, Secure cookies**:

| Cookie | Attributes | Purpose |
|--------|------------|---------|
| `access_token` | HttpOnly, Secure, SameSite=None, Path=/ | Short-lived access |
| `refresh_token` | HttpOnly, Secure, SameSite=None, Path=/, Session-based | Long-lived refresh |

**Remember Me:** When `rememberMe=true`, refresh token cookie includes `Max-Age` header for persistent storage.

---

## 6. Security Configuration

### 6.1 Public Endpoints

The following endpoints are public (no authentication required):

| Pattern | Purpose |
|---------|---------|
| `/api/public/**` | Public API endpoints |
| `/api/auth/**` | Authentication endpoints |
| `/api/auth/status` | Auth status check |
| `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI documentation |
| `/swagger-ui.html` | Swagger UI entry point |

### 6.2 Protected Endpoints

All `/api/**` endpoints (except those listed above) require authentication.

### 6.3 CORS Configuration

Allowed origins:
- `http://localhost:4200`
- `http://localhost:5173`
- `https://dash.queuer.app`
- `https://queuer.app`

Allowed methods: GET, POST, PUT, DELETE, PATCH, OPTIONS

Allowed headers: `*` (all headers)

Allow credentials: `true`

---

## 7. Data Models

### 7.1 AuthAccount Entity

**File:** `model/AuthAccount.java`

```java
@Entity
public class AuthAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private AuthAccountType type;  // OWNER, ADMIN

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String totpSecret;  // For 2FA (future)

    @Column(nullable = false)
    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 7.2 AuthAccountType Enum

**File:** `model/AuthAccountType.java`

```java
public enum AuthAccountType {
    OWNER,
    ADMIN
}
```

**Authority Mapping:** The enum value is prefixed with `ROLE_` for Spring Security:
- `OWNER` → `ROLE_OWNER`
- `ADMIN` → `ROLE_ADMIN`

### 7.3 Owner Entity

**File:** `model/Owner.java`

```java
@Entity
public class Owner {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "auth_account_id", nullable = false, unique = true)
    private AuthAccount authAccount;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Column(unique = true, nullable = false)
    @Pattern(regexp = "^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$")
    private String fiscalCode;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @ManyToMany(mappedBy = "owners")
    private Set<Organization> managedOrganizations = new HashSet<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 7.4 Repository Interfaces

**AuthAccountRepository:**
```java
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {
    Optional<AuthAccount> findByEmail(String email);
    Optional<AuthAccount> findFirstByType(AuthAccountType type);
}
```

**OwnerRepository:**
```java
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Owner findByAuthAccount(AuthAccount authAccount);
    Optional<Owner> findByAuthAccount_Email(String email);
}
```

---

## 8. API Endpoints

### 8.1 Authentication Controller

**Base Path:** `/api/auth`

#### POST /api/auth/signup

Creates a new Owner account (inactive by default).

**Request:**
```json
{
  "email": "owner@example.com",
  "password": "SecurePassword123!",
  "firstName": "Mario",
  "lastName": "Rossi",
  "fiscalCode": "RSSMRA80A01H501U",
  "phoneNumber": "+39123456789"
}
```

**Response (201 Created):**
```json
{
  "ownerId": 1,
  "authAccountId": 10,
  "email": "owner@example.com",
  "firstName": "Mario",
  "lastName": "Rossi",
  "active": false,
  "message": "Owner account created. Awaiting activation by admin."
}
```

**Rate Limiting:** 5 requests per 60 seconds

#### POST /api/auth/login

Authenticates user with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "rememberMe": true
}
```

**Response (200 OK):**
```json
{
  "expiresIn": 3600000
}
```

**Cookies Set:**
- `access_token` - HTTP-only, secure
- `refresh_token` - HTTP-only, secure (persistent if rememberMe=true)

**Rate Limiting:** 5 requests per 30 seconds

#### POST /api/auth/refresh

Refreshes access token using valid refresh token.

**Request:**
- Requires `refresh_token` cookie

**Response (200 OK):**
```json
{
  "expiresIn": 3600000
}
```

**Rate Limiting:** 10 requests per 60 seconds

#### GET /api/auth/status

Returns current authentication status.

**Response (200 OK - Authenticated):**
```json
{
  "authenticated": true,
  "userId": 1,
  "email": "user@example.com",
  "role": "ROLE_OWNER",
  "ownerId": 1,
  "firstName": "Mario",
  "lastName": "Rossi"
}
```

**Response (401 Unauthorized - Not Authenticated):**
```json
{
  "authenticated": false
}
```

#### POST /api/auth/logout

Logs out user by clearing authentication cookies.

**Response (200 OK)**

**Cookies Cleared:**
- `access_token` - Max-Age=0
- `refresh_token` - Max-Age=0

---

## 9. Exception Handling

### 9.1 Exception Hierarchy

```
ApiException (base exception)
├── AccountNotActiveException
├── InvalidTokenException
└── (other custom exceptions)
```

### 9.2 Global Exception Handler

**File:** `exception/ApiExceptionHandler.java`

**Handled Exceptions:**

| Exception | HTTP Status | Category | Message |
|-----------|-------------|----------|---------|
| `BadCredentialsException` | 401 | AUTHENTICATION | Invalid email or password |
| `UsernameNotFoundException` | 401 | AUTHENTICATION | Invalid email or password |
| `AccountNotActiveException` | 401 | ACCOUNT_NOT_ACTIVE | Account is not active |
| `InvalidTokenException` | 401 | INVALID_TOKEN | Token is invalid or expired |
| `MethodArgumentNotValidException` | 400 | VALIDATION | Validation failed |
| `MissingRequestCookieException` | 401 | AUTHENTICATION | Missing required cookie |
| `RateLimitExceededException` | 429 | RATE_LIMIT_EXCEEDED | Too many requests |

### 9.3 Standard Error Response Format

```json
{
  "timestamp": "2025-11-23T10:00:00Z",
  "status": 401,
  "category": "AUTHENTICATION",
  "message": "Invalid email or password",
  "path": "/api/auth/login",
  "details": {}
}
```

---

## 10. Maven Dependencies

### 10.1 Core Authentication Dependencies

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JJWT (JWT library) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Rate Limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j_jdk17-core</artifactId>
    <version>8.15.0</version>
</dependency>

<!-- Redis (for token revocation support) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 10.2 Annotation Processors

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.6.3</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## 11. Configuration

### 11.1 JWT Configuration Properties

**File:** `application.yaml`

```yaml
security:
  jwt:
    # Base64-encoded secret key (use at least 256 bits)
    secret: CJwRxZA4Eh2Tx8ekR6FAieZceo2qiMpeuLtk1fb5V93vDlk7PQDnGm9E2KuLGfAr

    access-token:
      expiration:
        OWNER: 3600000      # 15 minutes (in milliseconds)
        ADMIN: 3600000      # 15 minutes

    refresh-token:
      expiration:
        OWNER: 7776000000   # 90 days (in milliseconds)
        ADMIN: 604800000    # 7 days
```

**Configuration Class:** `config/JwtProperties.java`

```java
@Component
@ConfigurationProperties(prefix = "security.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private TokenExpiration accessToken;
    private TokenExpiration refreshToken;

    @Getter
    @Setter
    public static class TokenExpiration {
        private Map<String, Long> expiration;
    }
}
```

### 11.2 CORS Configuration

**File:** `config/SecurityConfig.java`

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(
        "http://localhost:4200",
        "https://dash.queuer.app",
        "https://queuer.app",
        "http://localhost:5173"
    ));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
}
```

---

## 12. LLM Guidelines

### 12.1 When Creating Authentication Features

1. **Create DTOs** first (request and response)
2. **Create/update repositories** for data access
3. **Create/update services** to access repositories
4. **Create/update controllers** to access services
5. **Use MapStruct** for entity-DTO mapping
6. **Throw specific exceptions** (don't catch just to return ResponseEntity)

### 12.2 Accessing Current User

**DO:**
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final CurrentUserService currentUserService;

    public void doSomething() {
        Long authAccountId = currentUserService.getCurrentAuthAccountId();
        Optional<Long> ownerId = currentUserService.getCurrentOwnerId();
    }
}
```

**DON'T:**
```java
// DO NOT pass userId as a parameter
public void doSomething(Long userId) { ... }

// DO NOT extract from SecurityContext directly in services
public void doSomething() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    // ...
}
```

### 12.3 Authorization Checks

Use `@PreAuthorize` on service methods:

```java
@PreAuthorize("hasRole('OWNER')")
public void ownerOnlyMethod() { ... }

@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() { ... }

@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public void multiRoleMethod() { ... }
```

### 12.4 Password Handling

**Hashing:**
```java
String encodedPassword = passwordEncoder.encode(password);
```

**Validation:**
- Handled automatically by `DaoAuthenticationProvider`
- Never compare passwords directly
- BCrypt automatically handles salt

### 12.5 Token Generation

```java
// In service or controller
JWTUserPrincipal userPrincipal = jwtService.getCurrentUser();
String accessToken = jwtService.generateAccessToken(userPrincipal);
String refreshToken = jwtService.generateRefreshToken(userPrincipal);
```

### 12.6 Token Validation

```java
if (jwtService.isTokenValid(token)) {
    JWTUserPrincipal principal = jwtService.extractUserPrincipal(token);
}
```

### 12.7 Cookie Management

**Create cookie:**
```java
ResponseCookie cookie = ResponseCookie.from("access_token", token)
    .httpOnly(true)
    .secure(true)
    .path("/")
    .maxAge(Duration.ofMillis(expirationMs))
    .sameSite("None")
    .build();

return ResponseEntity.ok()
    .header(HttpHeaders.SET_COOKIE, cookie.toString())
    .body(response);
```

**Clear cookie:**
```java
ResponseCookie cookie = ResponseCookie.from("access_token", "")
    .httpOnly(true)
    .secure(true)
    .path("/")
    .maxAge(0)
    .sameSite("None")
    .build();
```

### 12.8 Common Patterns

**Login:**
```java
Authentication authentication = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
);
JWTUserPrincipal userPrincipal = (JWTUserPrincipal) authentication.getPrincipal();
```

**Get current user in controller:**
```java
@GetMapping("/me")
public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
    JWTUserPrincipal principal = (JWTUserPrincipal) authentication.getPrincipal();
    return ResponseEntity.ok(userService.getUserResponse(principal));
}
```

**Rate limiting annotation:**
```java
@RateLimit(
    capacity = 5,
    refillTokens = 1,
    refillPeriod = 30,
    refillUnit = ChronoUnit.SECONDS,
    keyPrefix = "login"
)
```

### 12.9 Important Reminders

1. **Email is username** - The system uses email as the username identifier
2. **Account active check** - `CustomUserDetailsService` throws `AccountNotActiveException` if inactive
3. **Authorities are ROLE_ prefixed** - `ROLE_OWNER`, `ROLE_ADMIN`
4. **Stateless** - No server-side sessions, all state in JWT
5. **Cookie-based tokens** - Tokens stored in HTTP-only cookies
6. **Filter order matters** - JWT filter must be before `UsernamePasswordAuthenticationFilter`
7. **Never return entities** - Always use DTOs for API responses
8. **Use `@Valid`** - Always validate request DTOs

---

## 13. File Structure Reference

```
src/main/java/it/trinex/queuerbe/
├── config/
│   ├── JwtProperties.java              # JWT configuration properties
│   └── SecurityConfig.java             # Spring Security configuration
├── controller/
│   └── AuthenticationController.java   # Auth endpoints
├── dto/
│   ├── request/
│   │   └── LoginRequestDTO.java        # Login request DTO
│   └── response/
│       ├── AuthResponseDTO.java        # Auth response DTO
│       └── AuthStatusResponseDTO.java  # Auth status DTO
├── exception/
│   ├── ApiException.java               # Base exception
│   ├── AccountNotActiveException.java  # Account inactive exception
│   ├── InvalidTokenException.java      # Invalid token exception
│   └── ApiExceptionHandler.java        # Global exception handler
├── model/
│   ├── AuthAccount.java                # Authentication entity
│   ├── AuthAccountType.java            # Account type enum
│   └── Owner.java                      # Owner profile entity
├── repository/
│   ├── AuthAccountRepository.java      # Auth account repo
│   └── OwnerRepository.java            # Owner repo
├── security/
│   ├── JwtAuthenticationFilter.java    # JWT filter
│   └── JWTUserPrincipal.java           # User principal
└── service/
    ├── CurrentUserService.java         # Current user access
    ├── CustomUserDetailsService.java   # User details service
    ├── JWTService.java                 # JWT operations
    └── OrganizationService.java        # Organization service (used by JWT)
```

---

## 14. Creating a Maven Package

To create a reusable Maven package from this auth system:

1. **Extract the following components:**
   - `config/SecurityConfig.java`
   - `config/JwtProperties.java`
   - `security/JwtAuthenticationFilter.java`
   - `security/JWTUserPrincipal.java`
   - `service/JWTService.java`
   - `service/CustomUserDetailsService.java`
   - `service/CurrentUserService.java`
   - `exception/ApiException.java`
   - `exception/InvalidTokenException.java`
   - `exception/AccountNotActiveException.java`
   - `exception/ApiExceptionHandler.java`
   - `model/AuthAccount.java`
   - `model/AuthAccountType.java`
   - `controller/AuthenticationController.java`
   - All DTOs in `dto/request/` and `dto/response/`
   - Repository interfaces

2. **Make entities abstract or configurable:**
   - Keep `AuthAccount` as base entity
   - Make `Owner` an example profile entity
   - Allow custom profile entities to extend the pattern

3. **Configuration requirements:**
   - JWT secret key (Base64-encoded)
   - Token expiration times per role
   - CORS origins
   - Database connection
   - Redis connection (for token revocation)

4. **Required dependencies:**
   - See section 10.1

5. **Expose extension points:**
   - Custom `UserDetailsService` implementation
   - Custom profile entities
   - Custom authorities/granted authorities
   - Custom token claims

---

*This document is a comprehensive reference. When in doubt, refer to the actual source code for the most up-to-date implementation.*
