package it.trinex.blackout.security;

import it.trinex.blackout.exception.PasskeyRequiredException;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

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

    /**
     * Creates a new BlackoutSecurityExpressionRoot.
     *
     * @param authentication the current authentication object
     */
    public BlackoutSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
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
     * @return {@code false} by default; override to implement actual passkey check
     */
    public boolean passkeyRequired() {
        // TODO: Implement actual passkey validation logic
        // This is a placeholder that always returns false
        // Users can override the expression handler bean to provide custom logic
        throw new PasskeyRequiredException("Passkey required");
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
