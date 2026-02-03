package it.trinex.blackout.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Abstract base class for BlackoutPrincipalFactory that handles all common field mappings.
 * Subclasses only need to implement custom fields by overriding applyCustomFields.
 *
 * @param <T> The type of UserDetails (must extend BlackoutUserPrincipal)
 */
public abstract class AbstractBlackoutPrincipalFactory<T extends UserDetails> implements BlackoutPrincipalFactory<T> {

    @Override
    public final T fromClaims(Claims claims, Collection<? extends GrantedAuthority> authorities) {
        BlackoutUserPrincipal.BlackoutUserPrincipalBuilder<?, ?> builder = (BlackoutUserPrincipal.BlackoutUserPrincipalBuilder<?, ?>) getBuilder();

        // Handle all common fields
        builder.id(claims.get("uid", Long.class))
                .firstName(claims.get("first_name", String.class))
                .lastName(claims.get("last_name", String.class))
                .authorities(authorities)
                .userId(claims.get("user_id", Long.class))
                .username(claims.getSubject())
                .password(null);

        // Let subclasses apply custom fields
        applyCustomFields(claims, builder);

        return (T) builder.build();
    }

    /**
     * Returns the appropriate builder for the target type.
     * Subclasses must override this to return their custom builder.
     */
    protected abstract BlackoutUserPrincipal.BlackoutUserPrincipalBuilder<?, ?> getBuilder();

    /**
     * Apply custom fields to the builder.
     * Subclasses override this to add their specific fields.
     *
     * @param claims the JWT claims
     * @param builder the builder with common fields already applied
     */
    protected void applyCustomFields(Claims claims,
                                      BlackoutUserPrincipal.BlackoutUserPrincipalBuilder<?, ?> builder) {
        // Default: no custom fields
    }
}
