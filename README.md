# NNH - Spring Security JWT Starter

A Spring Boot starter library for JWT-based authentication with HTTP-only cookie token storage, multi-role support, and maximum flexibility through conditional auto-configuration.

## Features

- 🔐 **JWT-based Authentication** - Stateless authentication using JWT tokens
- 🍪 **HTTP-only Cookies** - Secure token storage (not in JSON responses)
- 👥 **Multi-role Support** - Different token expiration times per role
- 🔧 **Highly Customizable** - All components can be overridden
- 🎯 **Zero Configuration** - Works out-of-box with sensible defaults
- 🧪 **Testing Support** - Built-in in-memory authentication for testing
- 📝 **JPA Entities Included** - Optional entities for quick start
- 🚀 **Spring Boot Auto-Configuration** - Just add the dependency and go

## Quick Start

### 1. Add the Dependency

```xml
<dependency>
    <groupId>it.trinex</groupId>
    <artifactId>nnh</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure Minimum Required Properties

```yaml
# application.yml
nnh:
  security:
    jwt:
      secret: YOUR_BASE64_ENCODED_SECRET_KEY_HERE
```

That's it! The library will automatically configure authentication with sensible defaults.

### 3. Test It

```bash
# Create a new user
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecurePassword123!"}'

# Login with the created user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecurePassword123!"}'
```

## Configuration

### Minimal Configuration

```yaml
nnh:
  security:
    jwt:
      secret: CJwRxZA4Eh2Tx8ekR6FAieZceo2qiMpeuLtk1fb5V93vDlk7PQDnGm9E2KuLGfAr
```

### Full Configuration

```yaml
nnh:
  security:
    # Enable/disable entire security system
    enabled: true

    # Use JPA repositories (set false for custom UserDetailsService)
    use-jpa: true

    # Use in-memory authentication (for testing only!)
    use-in-memory-auth: false

    # Expose default AuthController (set false to use custom controller)
    expose-controller: true

    jwt:
      # Base64-encoded secret key (minimum 256 bits when decoded)
      # Generate with: openssl rand -base64 32
      secret: YOUR_BASE64_SECRET

      access-token:
        expiration:
          # Access token expiration per role (milliseconds)
          OWNER: 900000       # 15 minutes
          ADMIN: 900000       # 15 minutes

      refresh-token:
        expiration:
          # Refresh token expiration per role (milliseconds)
          OWNER: 7776000000   # 90 days
          ADMIN: 604800000    # 7 days

    cookie:
      # HTTP-only cookie (prevents XSS)
      http-only: true

      # Secure cookie (HTTPS only)
      secure: true

      # SameSite: Strict, Lax, None
      same-site: None

      # Cookie path
      path: /

      # Optional domain
      # domain: example.com

    cors:
      # Allowed origins
      allowed-origins:
        - http://localhost:4200
        - http://localhost:5173
        - https://yourdomain.com

      # Allowed methods
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
        - OPTIONS

      # Allowed headers
      allowed-headers:
        - "*"

      # Allow credentials
      allow-credentials: true

      # Preflight cache max age (seconds)
      max-age: 3600
```

## Database Setup

### Option 1: Use JPA Entities (Recommended for Production)

#### Step 1: Add Database Driver

```xml
<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- H2 (for testing) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

#### Step 2: Configure Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_database
    username: your_username
    password: your_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

nnh:
  security:
    use-jpa: true
```

#### Step 3: Tables Created Automatically

The library will create these tables:

- `auth_accounts` - Authentication credentials

**Note:** The `Owner` entity has been removed. Applications should create their own profile entities with a one-to-one relationship to `AuthAccount`.

#### Create Users

**Option A: Using the Signup API Endpoint (Recommended)**

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "accountType": "USER"
  }'
```

The user is automatically logged in and receives JWT cookies.

**Option B: Programmatically (for initial admin user)**

```java
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        // Create initial admin user
        if (!userService.userExists("admin@example.com")) {
            userService.createUser(
                "admin@example.com",
                "AdminPassword123!",
                AuthAccountType.ADMIN
            );
        }
    }
}
```

### Option 2: In-Memory Authentication (For Testing)

```yaml
nnh:
  security:
    use-in-memory-auth: true

spring:
  # No datasource needed!
```

Default test users:
- `test@example.com` / `password` (USER role)
- `admin@example.com` / `password` (ADMIN role)

### Option 3: Custom UserDetailsService

```yaml
nnh:
  security:
    use-jpa: false
```

```java
@Service
public class MyCustomUserDetailsService implements UserDetailsService {

    private final MyUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        MyUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Convert to JwtUserPrincipal
        return JwtUserPrincipal.builder()
            .id(user.getId())
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
    }
}
```

## API Endpoints

When `expose-controller: true` (default), the following endpoints are available:

### POST /api/auth/signup

Create a new user account. The user is automatically logged in after signup.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "accountType": "USER"
}
```

**Fields:**
- `email` (required): User's email address
- `password` (required): Password (min 8 characters)
- `accountType` (optional): Account type (USER, ADMIN, or custom). Defaults to USER

**Response (201 Created):**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "role": "USER",
  "expiresIn": 900000,
  "message": "User created successfully"
}
```

**Cookies Set:**
```
Set-Cookie: access_token=...; HttpOnly; Secure; SameSite=None; Path=/
Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=None; Path=/
```

**Error Responses:**
- `409 Conflict` - User already exists
- `400 Bad Request` - Validation error

### POST /api/auth/login

Authenticate with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password",
  "rememberMe": true
}
```

**Response:**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "role": "OWNER",
  "ownerId": 1,
  "firstName": "Mario",
  "lastName": "Rossi",
  "expiresIn": 900000
}
```

**Cookies Set:**
```
Set-Cookie: access_token=...; HttpOnly; Secure; SameSite=None; Path=/
Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=7776000
```

### POST /api/auth/refresh

Refresh access token using refresh token cookie.

**Request:**
- Requires `refresh_token` cookie

**Response:**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "role": "OWNER",
  "ownerId": 1,
  "firstName": "Mario",
  "lastName": "Rossi",
  "expiresIn": 900000
}
```

### POST /api/auth/logout

Clear authentication cookies.

**Response:** `200 OK`

**Cookies Cleared:**
```
Set-Cookie: access_token=; Max-Age=0
Set-Cookie: refresh_token=; Max-Age=0
```

### GET /api/auth/status

Get current authentication status.

**Response (authenticated):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "role": "OWNER",
  "ownerId": 1,
  "firstName": "Mario",
  "lastName": "Rossi"
}
```

**Response (not authenticated):** `401 Unauthorized`

## Using in Your Code

### Access Current User

**DO:** Use `CurrentUserService`

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final CurrentUserService currentUserService;

    public void doSomething() {
        Long authAccountId = currentUserService.getCurrentAuthAccountId();
        Optional<Long> ownerId = currentUserService.getCurrentOwnerId();
        String email = currentUserService.getCurrentEmail();
        String role = currentUserService.getCurrentRole();
    }
}
```

**DON'T:** Pass userId as parameter

```java
// ❌ DON'T DO THIS
public void doSomething(Long userId) { }

// ✅ DO THIS
public void doSomething() {
    Long userId = currentUserService.getCurrentAuthAccountId();
}
```

### Secure Your Endpoints

```java
@RestController
@RequestMapping("/api/my-endpoints")
public class MyController {

    // Public endpoint
    @GetMapping("/public")
    public String publicEndpoint() {
        return "Anyone can access";
    }

    // Requires authentication
    @GetMapping("/authenticated")
    public String authenticatedEndpoint() {
        return "Only authenticated users";
    }

    // Requires OWNER role
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/owner-only")
    public String ownerOnlyEndpoint() {
        return "Only owners";
    }

    // Requires ADMIN role
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin-only")
    public String adminOnlyEndpoint() {
        return "Only admins";
    }

    // Multiple roles
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @GetMapping("/multi-role")
    public String multiRoleEndpoint() {
        return "Owners or admins";
    }
}
```

## Customization & Overriding

Every component can be overridden! The library uses `@ConditionalOnMissingBean` to ensure your beans take precedence.

### Override Strategy Table

| Component | Condition to Load | How to Override |
|-----------|------------------|-----------------|
| `SecurityFilterChain` | `@ConditionalOnMissingBean` | Provide custom `SecurityFilterChain` bean |
| `JwtService` | `@ConditionalOnMissingBean` | Provide custom `JwtService` bean |
| `JwtAuthenticationFilter` | `@ConditionalOnMissingBean` | Provide custom filter bean |
| `UserDetailsService` | `@ConditionalOnMissingBean` | Provide custom `UserDetailsService` |
| `AuthController` | `expose-controller=true` | Set `expose-controller=false` or provide custom controller |
| JPA Repositories | `use-jpa=true` | Set `use-jpa=false` or provide custom repositories |
| `CurrentUserService` | `@ConditionalOnMissingBean` | Provide custom `CurrentUserService` bean |

### Override Examples

#### 1. Disable Default Controller and Use Custom

```yaml
nnh:
  security:
    expose-controller: false
```

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class MyAuthController {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CookieUtils cookieUtils;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        // Your custom login logic
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        // Set cookies
        response.addHeader(HttpHeaders.SET_COOKIE,
            cookieUtils.createAccessTokenCookie(accessToken, 900000).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
            cookieUtils.createRefreshTokenCookie(refreshToken, request.isRememberMe()).toString());

        return ResponseEntity.ok(AuthResponse.fromPrincipal(principal, 900000));
    }
}
```

#### 2. Override Security Configuration

```java
@Configuration
public class MySecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Your custom security configuration
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            // ... other configuration
        return http.build();
    }
}
```

#### 3. Use Custom Entities

```java
@Entity
@Table(name = "users")
@Data
public class MyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String fullName;
    private String role;
}

@Repository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {
    Optional<MyUser> findByEmail(String email);
}

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final MyUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) {
        MyUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Convert to JwtUserPrincipal
        return JwtUserPrincipal.builder()
            .id(user.getId())
            .username(user.getEmail())
            .password(user.getPassword())
            .firstName(user.getFullName())
            .lastName("")
            .role(user.getRole())
            .ownerId(Optional.empty())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
            .build();
    }
}
```

**Configure:**
```yaml
nnh:
  security:
    use-jpa: false  # Disable default JPA entities
```

#### 4. Override JWT Service

```java
@Service
public class MyJwtService {

    public String generateToken(MyUser user) {
        // Your custom JWT generation logic
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("customClaim", "customValue")
            .signWith(key())
            .compact();
    }

    // ... other methods
}
```

#### 5. Override Password Encoder

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use stronger encoding
        return new BCryptPasswordEncoder(12);
    }
}
```

#### 6. Add Custom Claims to JWT

```java
@Service
public class CustomJwtService extends JwtService {

    @Override
    public String generateAccessToken(JwtUserPrincipal userPrincipal) {
        return Jwts.builder()
            .subject(userPrincipal.getUsername())
            .claim("uid", userPrincipal.getId())
            .claim("role", userPrincipal.getRole())
            .claim("customData", "your-custom-data")  // Add custom claims
            .claim("permissions", getPermissions(userPrincipal))  // Example
            .signWith(getSigningKey())
            .compact();
    }
}
```

#### 7. Custom Token Validation

```java
@Component
public class CustomJwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            // Add custom validation logic
            if (isTokenBlacklisted(token)) {
                response.sendError(401, "Token is blacklisted");
                return;
            }

            // Standard validation
            if (jwtService.isTokenValid(token)) {
                JwtUserPrincipal principal = jwtService.extractUserPrincipal(token);

                // Add custom checks
                if (isUserLocked(principal.getId())) {
                    response.sendError(403, "User is locked");
                    return;
                }

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

#### 8. Override CORS Configuration

```java
@Configuration
public class MyCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://yourdomain.com"));
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

#### 9. Add Rate Limiting

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        String endpoint = request.getRequestURI();

        if (!rateLimitService.allowRequest(clientIp, endpoint)) {
            response.setStatus(429);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

#### 10. Custom Exception Handling

```java
@RestControllerAdvice
public class MyExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(MyCustomException.class)
    public ResponseEntity<ErrorResponse> handleMyCustomException(MyCustomException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(400)
            .category("CUSTOM_ERROR")
            .message(ex.getMessage())
            .build();
        return ResponseEntity.badRequest().body(error);
    }
}
```

## Testing

### Unit Testing with In-Memory Auth

```yaml
# src/test/resources/application-test.yml
nnh:
  security:
    use-in-memory-auth: true
    jwt:
      secret: TEST_SECRET_KEY_FOR_DEVELOPMENT
```

```java
@SpringBootTest
@AutoConfigureMockMvc
class MyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAuthenticatedEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected"))
            .andExpect(status().isOk());
    }
}
```

### Integration Testing with H2

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop

nnh:
  security:
    jwt:
      secret: TEST_SECRET_KEY
```

### Test Utilities

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestUtils {

    @Autowired
    protected JwtService jwtService;

    protected String generateTestToken(String email, String role) {
        JwtUserPrincipal principal = JwtUserPrincipal.builder()
            .id(1L)
            .username(email)
            .password("encoded-password")
            .firstName("Test")
            .lastName("User")
            .role(role)
            .ownerId(Optional.of(1L))
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
            .build();

        return jwtService.generateAccessToken(principal);
    }
}
```

## Common Patterns

### 1. Require Authentication for All Endpoints

```yaml
nnh:
  security:
    expose-controller: false
```

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

### 2. Custom Owner Profile

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "auth_account_id")
    private AuthAccount authAccount;

    private String companyName;
    private String vatNumber;
    // ... other fields
}
```

### 3. Add Social Login (OAuth2)

```java
@Configuration
public class OAuth2Config {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .authorizationEndpoint()
                    .baseUri("/oauth2/authorize")
                .and()
                .redirectionEndpoint()
                    .baseUri("/oauth2/callback/*")
            );
        return http.build();
    }
}
```

## FAQ

### Q: How do I disable the library?

```yaml
nnh:
  security:
    enabled: false
```

### Q: Can I use this with OAuth2?

Yes, but you'll need to override the SecurityFilterChain to add OAuth2 configuration.

### Q: How do I change the token expiration per role?

```yaml
nnh:
  security:
    jwt:
      access-token:
        expiration:
          OWNER: 1800000   # 30 minutes
          ADMIN: 3600000   # 1 hour
          CUSTOM_ROLE: 7200000  # 2 hours
```

### Q: How do I revoke tokens?

Implement a token blacklist (e.g., using Redis):

```java
@Service
public class TokenRevocationService {

    private final RedisTemplate<String, String> redisTemplate;

    public void revokeToken(String jti) {
        redisTemplate.opsForValue().set("revoked:" + jti, "true", Duration.ofDays(7));
    }

    public boolean isTokenRevoked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("revoked:" + jti));
    }
}
```

### Q: Can I use this with GraphQL?

Yes! The JWT filter works with any HTTP-based framework, including GraphQL.

## License

[Your License Here]

## Support

For issues and questions, please open an issue on GitHub.
