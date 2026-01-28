---
trigger: model_decision
description: Use this document whenever working on authentication-related features or when asked about the authentication system
---

# Authentication System Documentation

This document provides a complete reference for the JWT-based authentication library implemented in this Spring Boot application. It is designed to be replicable as a standalone Maven package.

**Core Principle:** This library provides **authentication only**. It handles `AuthAccount` (credentials, roles, tokens). The application developer creates their own profile entities (e.g., Owner, Customer, Employee) that reference `AuthAccount`.

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
13. [Creating Custom Profile Entities](#13-creating-custom-profile-entities)

---

## 1. Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Java | JDK | 21 |
| Spring Boot | Starter | 3.5.7 |
| Spring Security | Starter | 3.5.7 |
| JWT Library | JJWT | 0.12.5 |
| Password Encoder | BCrypt | (Spring Security default) |
| Validation | Jakarta Validation | - |
| Rate Limiting | Bucket4j | 8.15.0 |

---

## 2. Architecture Overview

The authentication library follows a **stateless JWT-based architecture**:

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
│  │  JwtUserPrincipal                       │    │
│  │  - id, username, password               │    │
│  │  - role, authorities                    │    │
│  └─────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
       │
       │ Controllers/Services can access
       ▼
┌──────────────────────────────────────────────────┐
│         CurrentUserService                       │
│  - getCurrentAuthAccountId()                    │
│  - getCurrentUserPrincipal()                    │
│  - getCurrentEmail()                            │
│  - getCurrentRole()                             │
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

**File:** `security/jwt/JwtService.java`

**Responsibilities:**
- Generate access tokens
- Generate refresh tokens
- Validate access and refresh tokens
- Extract user principal from tokens
- Calculate token expiration times

**Key Methods:**

| Method | Description |
|--------|-------------|
| `generateAccessToken(JwtUserPrincipal)` | Creates a new access token |
| `generateRefreshToken(JwtUserPrincipal)` | Creates a new refresh token |
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

**File:** `security/jwt/JwtAuthenticationFilter.java`

**Responsibilities:**
- Extract JWT from `access_token` cookie
- Validate token using `JwtService`
- Create `UsernamePasswordAuthenticationToken`
- Set authentication in `SecurityContextHolder`

**Execution Order:** `OncePerRequestFilter` before `UsernamePasswordAuthenticationFilter`

### 3.4 User Details Service

**File:** `security/CustomUserDetailsService.java`

**Responsibilities:**
- Load user by email (not username!)
- Check account active status
- Create `JwtUserPrincipal` with authorities

**Note:** Although the method is `loadUserByUsername`, it actually loads by **email**.

**Extension Point:** This is where you load your custom profile entities if needed.

### 3.5 JWT User Principal

**File:** `security/jwt/JwtUserPrincipal.java`

**Type:** Java class implementing `UserDetails`

**Core Properties Only:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | AuthAccount ID |
| `username` | String | Email address |
| `password` | String | Password hash (for validation) |
| `role` | String | AuthAccountType (from enum or custom) |
| `authorities` | Collection\<? extends GrantedAuthority\> | Spring Security authorities |

**Design Note:** This class contains only fundamental authentication properties. Application-specific data (firstName, lastName, profileId, etc.) should be accessed through your custom profile entities.

**Constructor:**
```java
public JwtUserPrincipal(
    Long id,
    String username,
    String password,
    String role,
    Collection<? extends GrantedAuthority> authorities
)
```

**Factory Method from AuthAccount:**
```java
public static JwtUserPrincipal create(AuthAccount authAccount) {
    List<GrantedAuthority> authorities = List.of(
        new SimpleGrantedAuthority("ROLE_" + authAccount.getType().name())
    );

    return new JwtUserPrincipal(
        authAccount.getId(),
        authAccount.getEmail(),
        authAccount.getPasswordHash(),
        authAccount.getType().name(),
        authorities
    );
}
```

**All UserDetails methods return `true` except:**
- `getPassword()` - returns the password hash
- `isEnabled()` - implicitly checked before principal creation

### 3.6 Current User Service

**File:** `service/CurrentUserService.java`

**Purpose:** Service layer access to current authenticated user

**Methods:**
```java
public Long getCurrentAuthAccountId()         // Returns AuthAccount ID
public JwtUserPrincipal getCurrentUserPrincipal() // Returns full principal
public String getCurrentEmail()               // Returns email
public String getCurrentRole()                // Returns role
public boolean hasRole(String role)           // Check if user has role
```

**Important:** Do NOT pass `authAccountId` as parameters when referring to the currently logged-in user. Always use this service.

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
     │                                   │ 2. Extract JwtUserPrincipal
     │                                   │
     │                                   │ 3. JwtService.generateTokens()
     │                                   │
     │<──────────────────────────────────┤
     │ Set-Cookie: access_token=...       │
     │ Set-Cookie: refresh_token=...      │
     │ {expiresIn: 900000}                │
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
     │                                   │ 2. JwtService.isTokenValid()
     │                                   │ 3. JwtService.extractUserPrincipal()
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
     │                                   │ 1. JwtService.isRefreshTokenValid()
     │                                   │ 2. JwtService.extractUserPrincipal()
     │                                   │    (no DB call!)
     │                                   │ 3. Generate new access token
     │                                   │ 4. Update cookies
     │                                   │
     │<──────────────────────────────────┤
     │ Set-Cookie: access_token=...       │
     │ {expiresIn: 900000}                │
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
| `role` | String | AuthAccountType |
| `tokenType` | String | "ACCESS" or "REFRESH" |
| `iat` | Date | Issued at |
| `exp` | Date | Expiration |

**Note:** Tokens only contain authentication data. Application-specific data (firstName, lastName, etc.) is accessed via your custom profile entities using the `uid` claim.

### 5.2 Token Expiration (Configurable)

**Access Tokens:**
| Role | Expiration | Example Value |
|------|------------|---------------|
| Any | Configurable | 15 minutes (900000 ms) |

**Refresh Tokens:**
| Role | Expiration | Example Value |
|------|------------|---------------|
| Any | Configurable | 7-90 days |

Configure per role in `application.yaml`:
```yaml
nnh:
  security:
    jwt:
      access-token:
        expiration:
          USER: 900000        # 15 minutes
          ADMIN: 1800000      # 30 minutes
      refresh-token:
        expiration:
          USER: 604800000     # 7 days
          ADMIN: 7776000000   # 90 days
```

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

Configured via `SecurityProperties`:

```yaml
nnh:
  security:
    cors:
      allowed-origins:
        - http://localhost:4200
        - http://localhost:5173
        - https://yourdomain.com
      allow-credentials: true
```

Allowed methods: GET, POST, PUT, DELETE, PATCH, OPTIONS

Allowed headers: `*` (all headers)

---

## 7. Data Models

### 7.1 AuthAccount Entity (Core)

**File:** `model/AuthAccount.java`

This is the **only core entity** provided by the library. It handles authentication credentials.

```java
@Entity
@Table(name = "auth_accounts")
public class AuthAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthAccountType type;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "totp_secret")
    private String totpSecret;  // For 2FA (future)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
```

**Key Features:**
- Unique email constraint
- Account type (extensible enum)
- Active status for account activation/deactivation
- Audit timestamps
- Password change tracking
- Last login tracking

### 7.2 AuthAccountType Enum

**File:** `model/AuthAccountType.java`

```java
public enum AuthAccountType {
    USER,
    ADMIN
}
```

**Extension:** You can add custom types to this enum for your application's needs.

**Authority Mapping:** The enum value is prefixed with `ROLE_` for Spring Security:
- `USER` → `ROLE_USER`
- `ADMIN` → `ROLE_ADMIN`
- Custom → `ROLE_CUSTOM`

**Example Custom Types:**
```java
public enum AuthAccountType {
    USER,
    ADMIN,
    CUSTOMER,
    VENDOR,
    EMPLOYEE,
    MODERATOR
}
```

### 7.3 Repository Interfaces

**AuthAccountRepository (provided by library):**

```java
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {
    Optional<AuthAccount> findByEmail(String email);
    Optional<AuthAccount> findFirstByType(AuthAccountType type);
}
```

---

## 8. API Endpoints

### 8.1 Authentication Controller

**Base Path:** `/api/auth`

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
  "expiresIn": 900000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "role": "ROLE_USER"
  }
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
  "expiresIn": 900000
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
  "role": "ROLE_USER"
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
AuthenticationException (base exception)
├── TokenExpiredException
├── UserNotFoundException
├── UserAlreadyExistsException
└── (other custom exceptions)
```

### 9.2 Global Exception Handler

**File:** `exception/GlobalExceptionHandler.java`

**Handled Exceptions:**

| Exception | HTTP Status | Category | Message |
|-----------|-------------|----------|---------|
| `BadCredentialsException` | 401 | AUTHENTICATION | Invalid email or password |
| `UsernameNotFoundException` | 401 | AUTHENTICATION | Invalid email or password |
| `AuthenticationException` | 401 | AUTHENTICATION | Account is not active |
| `TokenExpiredException` | 401 | TOKEN_EXPIRED | Token is expired |
| `MethodArgumentNotValidException` | 400 | VALIDATION | Validation failed |
| `MissingRequestCookieException` | 401 | AUTHENTICATION | Missing required cookie |

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

<!-- Rate Limiting (optional) -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j_jdk17-core</artifactId>
    <version>8.15.0</version>
</dependency>
```

---

## 11. Configuration

### 11.1 Application Configuration

**File:** `application.yaml`

```yaml
nnh:
  security:
    enabled: true
    use-jpa: true
    use-in-memory-auth: false
    expose-controller: true
    jwt:
      secret: YOUR_BASE64_SECRET_HERE_MINIMUM_256_BITS
      access-token:
        expiration:
          USER: 900000        # 15 minutes (in milliseconds)
          ADMIN: 1800000      # 30 minutes
      refresh-token:
        expiration:
          USER: 604800000     # 7 days (in milliseconds)
          ADMIN: 7776000000   # 90 days
    cookie:
      http-only: true
      secure: true
      same-site: None
      path: /
    cors:
      allowed-origins:
        - http://localhost:4200
        - http://localhost:5173
      allow-credentials: true
```

**Configuration Class:** `config/SecurityProperties.java`

```java
@Getter
@Setter
@ConfigurationProperties(prefix = "nnh.security")
public class SecurityProperties {
    private boolean enabled = true;
    private boolean useJpa = true;
    private boolean useInMemoryAuth = false;
    private boolean exposeController = true;

    private Jwt jwt = new Jwt();
    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private TokenExpiration accessToken = new TokenExpiration();
        private TokenExpiration refreshToken = new TokenExpiration();
    }

    @Getter
    @Setter
    public static class TokenExpiration {
        private Map<String, Long> expiration = new HashMap<String, Long>() {{
            put("USER", 900000L);      // 15 minutes
            put("ADMIN", 900000L);     // 15 minutes
        }};

        public Long getExpirationForRole(String role) {
            return expiration.getOrDefault(role, 900000L);
        }
    }
}
```

### 11.2 Secret Key Generation

Generate a secure Base64-encoded secret key (minimum 256 bits):

```bash
# Using openssl
openssl rand -base64 32

# Or using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## 12. LLM Guidelines

### 12.1 When Creating Authentication Features

1. **Create DTOs** first (request and response)
2. **Create/update repositories** for data access
3. **Create/update services** to access repositories
4. **Create/update controllers** to access services
5. **Throw specific exceptions** (don't catch just to return ResponseEntity)

### 12.2 Accessing Current User

**DO:**
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final CurrentUserService currentUserService;

    public void doSomething() {
        Long authAccountId = currentUserService.getCurrentAuthAccountId();
        String email = currentUserService.getCurrentEmail();
        String role = currentUserService.getCurrentRole();
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
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() { ... }

@PreAuthorize("hasRole('USER')")
public void userOnlyMethod() { ... }

@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
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
JwtUserPrincipal userPrincipal = jwtService.getCurrentUser();
String accessToken = jwtService.generateAccessToken(userPrincipal);
String refreshToken = jwtService.generateRefreshToken(userPrincipal);
```

### 12.6 Token Validation

```java
if (jwtService.isTokenValid(token)) {
    JwtUserPrincipal principal = jwtService.extractUserPrincipal(token);
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
JwtUserPrincipal userPrincipal = (JwtUserPrincipal) authentication.getPrincipal();
```

**Get current user in controller:**
```java
@GetMapping("/me")
public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
    JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
    return ResponseEntity.ok(userService.getUserResponse(principal));
}
```

### 12.9 Important Reminders

1. **Email is username** - The system uses email as the username identifier
2. **Account active check** - `CustomUserDetailsService` throws exception if inactive
3. **Authorities are ROLE_ prefixed** - `ROLE_USER`, `ROLE_ADMIN`
4. **Stateless** - No server-side sessions, all state in JWT
5. **Cookie-based tokens** - Tokens stored in HTTP-only cookies
6. **Filter order matters** - JWT filter must be before `UsernamePasswordAuthenticationFilter`
7. **Never return entities** - Always use DTOs for API responses
8. **Use `@Valid`** - Always validate request DTOs
9. **Library provides AuthAccount only** - Profile entities are your responsibility

---

## 13. Creating Custom Profile Entities

This library provides authentication (`AuthAccount`). Application-specific profiles are your responsibility.

### 13.1 Example: Creating a Customer Profile

**Step 1: Create your profile entity**

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "auth_account_id", nullable = false, unique = true)
    private AuthAccount authAccount;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return authAccount != null ? authAccount.getEmail() : null;
    }
}
```

**Step 2: Create repository**

```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByAuthAccount_Email(String email);
    Customer findByAuthAccount(AuthAccount authAccount);
}
```

**Step 3: Access profile data in your services**

```java
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CurrentUserService currentUserService;
    private final CustomerRepository customerRepository;

    public CustomerDto getCurrentCustomerProfile() {
        Long authAccountId = currentUserService.getCurrentAuthAccountId();
        AuthAccount authAccount = authAccountRepository.findById(authAccountId)
            .orElseThrow();

        Customer customer = customerRepository.findByAuthAccount(authAccount);
        return mapToDto(customer);
    }
}
```

**Step 4: Or extend CustomUserDetailsService to load profile**

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthAccountRepository authAccountRepository;
    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        AuthAccount authAccount = authAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!authAccount.getIsActive()) {
            throw new AuthenticationException("Account is not active");
        }

        // Create principal with auth data only
        // Profile data is accessed separately via repository
        return JwtUserPrincipal.create(authAccount);
    }
}
```

### 13.2 Key Architecture Points

1. **Separation of Concerns:**
   - `AuthAccount` = Authentication (library provides)
   - `Customer/Owner/Employee` = Profile data (you create)

2. **Relationship:**
   - Your profile entity has `@OneToOne` with `AuthAccount`
   - `AuthAccount` does NOT reference your profile

3. **Access Pattern:**
   - JWT contains only auth data (id, email, role)
   - Use auth account id to fetch your profile entity when needed

4. **Benefits:**
   - Library stays simple and focused
   - You control your profile schema
   - Multiple profile types can coexist (Customer, Vendor, etc.)

---

## 14. File Structure Reference

**Library files (provided):**
```
src/main/java/it/trinex/nnh/
├── config/
│   ├── SecurityProperties.java       # Configuration properties
│   ├── JwtSecurityConfiguration.java  # Security config
│   └── SecurityAutoConfiguration.java # Auto-config
├── controller/
│   └── AuthController.java            # Auth endpoints
├── dto/
│   ├── request/
│   │   └── LoginRequest.java
│   └── response/
│       └── AuthResponse.java
├── exception/
│   ├── AuthenticationException.java
│   ├── TokenExpiredException.java
│   ├── UserNotFoundException.java
│   └── GlobalExceptionHandler.java
├── model/
│   ├── AuthAccount.java               # Core auth entity (ONLY)
│   └── AuthAccountType.java           # Account type enum
├── repository/
│   └── AuthAccountRepository.java     # Core auth repository (ONLY)
├── security/
│   ├── CustomUserDetailsService.java  # Extension point
│   ├── InMemoryUserDetailsService.java
│   └── jwt/
│       ├── JwtService.java
│       ├── JwtProperties.java
│       ├── JwtAuthenticationFilter.java
│       └── JwtUserPrincipal.java      # Core principal (basic properties)
└── service/
    ├── AuthenticationService.java
    ├── AuthenticationServiceImpl.java
    └── CurrentUserService.java
```

**Application-specific files (you create):**
```
src/main/java/your/application/
├── model/
│   ├── Customer.java       # Your profile entity
│   ├── Vendor.java         # Your profile entity
│   └── Employee.java       # Your profile entity
├── repository/
│   ├── CustomerRepository.java
│   ├── VendorRepository.java
│   └── EmployeeRepository.java
└── service/
    ├── CustomerService.java
    └── VendorService.java
```

---

*This document is a comprehensive reference. When in doubt, refer to the actual source code for the most up-to-date implementation.*
