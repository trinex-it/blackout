package it.trinex.blackout.security;

import it.trinex.blackout.exception.PasskeyRequiredException;
import it.trinex.blackout.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Custom security expression root for Blackout-specific method security expressions.
 * Adds custom methods like {@link #passkeyRequired()} that can be used in @PreAuthorize annotations.
 *
 * <p>Usage in controllers:</p>
 * <pre>
 * {@code
 * @PreAuthorize("passkeyRequired()")
 * public String secureMethod() { ... }
 * }
 * </pre>
 */
public class BlackoutSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private Object filterObject;
    private Object returnObject;
    private Object target;
    private final JwtService jwtService;

    /**
     * Creates a new BlackoutSecurityExpressionRoot.
     *
     * @param authentication the current authentication object
     * @param jwtService the JWT service for token validation
     */
    public BlackoutSecurityExpressionRoot(Authentication authentication, JwtService jwtService) {
        super(authentication);
        this.jwtService = jwtService;
    }

    /**
     * Checks if passkey authentication is required for the current request.
     *
     * <p><strong>IMPORTANT:</strong> This method currently returns {@code false}.
     * Users should override this bean to implement actual passkey validation logic.</p>
     *
     * <p>Example override in application:</p>
     * <pre>
     * {@code
     * @Bean
     * @ConditionalOnMissingBean
     * public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
     *     return new BlackoutMethodSecurityExpressionHandler() {
     *         @Override
     *         protected BlackoutSecurityExpressionRoot createSecurityExpressionRoot(Authentication auth) {
     *             return new BlackoutSecurityExpressionRoot(auth) {
     *                 @Override
     *                 public boolean passkeyRequired() {
     *                     // Custom logic here
     *                     return myPasskeyService.isRequired(auth);
     *                 }
     *             };
     *         }
     *     };
     * }
     * </pre>
     *
     * @return {@code true} if passkey is present and valid, {@code false} otherwise
     */
    public boolean passkeyRequired() {
        // Ottieni la richiesta HTTP corrente
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }

        HttpServletRequest request = attributes.getRequest();

        // Esempio: leggere un cookie specifico (sostituisci "passkey_cookie" con il nome del tuo cookie)
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }

        String passkeyToken = "";

        for (Cookie cookie : cookies) {
            if ("passkey".equals(cookie.getName())) {
                passkeyToken = cookie.getValue();
            }
        }

        if (!passkeyToken.isBlank() && jwtService.isTokenValid(passkeyToken)) {
                return true;
            }

        throw new PasskeyRequiredException("Passkey authentication is required for this operation.");
    }

    public boolean bruttoCoglione() {
        return false;
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    /**
     * Sets the target object for the expression evaluation.
     * This is called by the expression handler.
     */
    public void setTargetObject(Object target) {
        this.target = target;
    }

    @Override
    public Object getThis() {
        return target;
    }
}
