package it.trinex.blackout.model.webauthn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "webauthn_user_entity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnUserEntity {

    @Id
    @Column(columnDefinition = "VARBINARY(255)", updatable = false, nullable = false)
    private byte[] id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String displayName;
}
