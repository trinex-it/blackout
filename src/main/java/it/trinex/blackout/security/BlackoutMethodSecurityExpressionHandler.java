package it.trinex.blackout.security;

import it.trinex.blackout.service.JwtService;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Custom method security expression handler for Blackout.
 *
 * <p>Replaces the default expression handler with a custom implementation that uses
 * {@link BlackoutSecurityExpressionRoot} as the SpEL root object, enabling custom
 * security expressions like {@code passkeyRequired()}.</p>
 *
 * <p>This handler delegates most operations to {@link DefaultMethodSecurityExpressionHandler}
 * but customizes the evaluation context creation to inject the custom root object.</p>
 *
 * <p><strong>Thread Safety:</strong> This handler is thread-safe. A new instance of
 * {@link BlackoutSecurityExpressionRoot} is created for each evaluation, ensuring
 * thread-safety without requiring synchronization.</p>
 *
 * <p><strong>Conditional Override:</strong> Users can provide their own bean to
 * override the default behavior:</p>
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
 *                     // Custom implementation
 *                     return true;
 *                 }
 *             };
 *         }
 *     };
 * }
 * }
 * </pre>
 */
@Component
public class BlackoutMethodSecurityExpressionHandler implements MethodSecurityExpressionHandler {

    private final DefaultMethodSecurityExpressionHandler delegate;
    private final AuthenticationTrustResolver trustResolver;
    private final JwtService jwtService;

    /**
     * Creates a new BlackoutMethodSecurityExpressionHandler.
     * Initializes the delegate handler and supporting components.
     */
    @Autowired
    public BlackoutMethodSecurityExpressionHandler(JwtService jwtService) {
        this.delegate = new DefaultMethodSecurityExpressionHandler();
        this.trustResolver = new AuthenticationTrustResolverImpl();
        this.jwtService = jwtService;

        // Configure the delegate
        this.delegate.setTrustResolver(this.trustResolver);
    }

    /**
     * Creates the evaluation context for method security expressions.
     *
     * <p>This method overrides the default behavior to inject a custom
     * {@link BlackoutSecurityExpressionRoot} as the root object, which provides
     * custom security expressions like {@code passkeyRequired()}.</p>
     *
     * <p>The root object is set directly (replacing the delegate root), ensuring
     * that custom methods are available without requiring a bean prefix.</p>
     *
     * @param authentication supplier for the current authentication
     * @param invocation the method invocation being secured
     * @return the configured evaluation context
     */
    public org.springframework.expression.EvaluationContext createEvaluationContext(
            Supplier<Authentication> authentication, MethodInvocation invocation) {

        // Create the standard evaluation context via delegate
        org.springframework.expression.EvaluationContext context =
                delegate.createEvaluationContext(authentication, invocation);

        // Create our custom root object
        Authentication auth = authentication.get();
        BlackoutSecurityExpressionRoot root = createSecurityExpressionRoot(auth);

        // Set the root object directly (not as a variable)
        // This allows expressions like "passkeyRequired()" without prefix
        if (context instanceof org.springframework.expression.spel.support.StandardEvaluationContext) {
            org.springframework.expression.spel.support.StandardEvaluationContext standardContext =
                    (org.springframework.expression.spel.support.StandardEvaluationContext) context;
            standardContext.setRootObject(root);
        }

        return context;
    }

    /**
     * Creates a custom security expression root.
     *
     * <p>Subclasses can override this method to provide a custom root object
     * with overridden expression methods.</p>
     *
     * @param authentication the current authentication
     * @return a new BlackoutSecurityExpressionRoot instance
     */
    protected BlackoutSecurityExpressionRoot createSecurityExpressionRoot(Authentication authentication) {
        return new BlackoutSecurityExpressionRoot(authentication, jwtService);
    }

    @Override
    public org.springframework.expression.ExpressionParser getExpressionParser() {
        return delegate.getExpressionParser();
    }

    @Override
    public Object filter(Object filterTarget, org.springframework.expression.Expression expression,
                        org.springframework.expression.EvaluationContext ctx) {
        return delegate.filter(filterTarget, expression, ctx);
    }

    @Override
    public void setReturnObject(Object returnObject, org.springframework.expression.EvaluationContext ctx) {
        delegate.setReturnObject(returnObject, ctx);
    }

    /**
     * Creates the evaluation context for method security expressions.
     * This method implements the SecurityExpressionHandler interface requirement.
     *
     * @param authentication the current authentication
     * @param invocation the method invocation being secured
     * @return the configured evaluation context
     */
    @Override
    public org.springframework.expression.EvaluationContext createEvaluationContext(
            Authentication authentication, MethodInvocation invocation) {
        return createEvaluationContext(() -> authentication, invocation);
    }

    // Setter methods for Spring's auto-configuration

    /**
     * Sets the trust resolver for determining anonymous/authenticated status.
     * @param trustResolver the trust resolver
     */
    public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
        this.delegate.setTrustResolver(trustResolver);
    }
}
