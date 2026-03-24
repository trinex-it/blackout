package it.trinex.blackout.repository.webauthn;

import it.trinex.blackout.model.webauthn.WebAuthnUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;

@RequiredArgsConstructor
public class JpaPublicKeyCredentialUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private final WebAuthnUserEntityJpaRepo repo;

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        return repo.findById(id.getBytes())
                .map(this::toEntity)
                .orElse(null);
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        return repo.findByUsername(username)
                .map(this::toEntity)
                .orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
        WebAuthnUserEntity entity = WebAuthnUserEntity.builder()
                .id(userEntity.getId().getBytes())
                .username(userEntity.getName())
                .displayName(userEntity.getDisplayName())
                .build();
        repo.save(entity);
    }

    @Override
    public void delete(Bytes id) {
        repo.deleteById(id.getBytes());
    }

    private PublicKeyCredentialUserEntity toEntity(WebAuthnUserEntity entity) {
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(new Bytes(entity.getId()))
                .name(entity.getUsername())
                .displayName(entity.getDisplayName())
                .build();
    }
}
