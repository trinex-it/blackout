package it.trinex.nnh.service;

import it.trinex.nnh.exception.UserAlreadyExistsException;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.AuthAccountType;
import it.trinex.nnh.repository.AuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of UserService.
 */
@Service
@ConditionalOnBean(AuthAccountRepository.class)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthAccount createUser(String email, String password, AuthAccountType type) {
        // Check if user already exists
        if (authAccountRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        }

        // Create new auth account
        AuthAccount authAccount = AuthAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .type(type != null ? type : AuthAccountType.USER)
                .isActive(true)
                .build();

        return authAccountRepository.save(authAccount);
    }

    @Override
    @Transactional
    public AuthAccount createUser(String email, String password) {
        return createUser(email, password, AuthAccountType.USER);
    }

    @Override
    public boolean userExists(String email) {
        return authAccountRepository.existsByEmail(email);
    }

    @Override
    public Optional<AuthAccount> findByEmail(String email) {
        return authAccountRepository.findByEmail(email);
    }
}
