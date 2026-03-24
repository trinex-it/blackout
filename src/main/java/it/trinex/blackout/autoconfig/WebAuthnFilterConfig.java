package it.trinex.blackout.autoconfig;

import it.trinex.blackout.security.WebAuthnJwtSuccessHandler;
import it.trinex.blackout.service.WebAuthnHelperService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "blackout.webauthn", name = "enabled", havingValue = "true")
public class WebAuthnFilterConfig {

    private final WebAuthnHelperService webAuthnHelperService;
    private final ObjectMapper objectMapper;

    @Bean
    public BeanPostProcessor webAuthnFilterPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebAuthnAuthenticationFilter) {
                    WebAuthnAuthenticationFilter filter = (WebAuthnAuthenticationFilter) bean;
                    filter.setAuthenticationSuccessHandler(new WebAuthnJwtSuccessHandler(webAuthnHelperService, objectMapper));
                }
                // WebAuthnRegistrationFilter doesn't implement AuthenticationSuccessHandler,
                // it just returns a JSON response: {"success":true}. That's fine as we don't need tokens here.
                return bean;
            }
        };
    }
}
