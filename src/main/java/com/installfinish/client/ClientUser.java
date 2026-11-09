package com.installfinish.client;

import com.installfinish.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A person on the client side (approver or viewer), bound to exactly one client account. */
@Entity
@Table(name = "client_user")
public class ClientUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_account_id", nullable = false, updatable = false)
    private UUID clientAccountId;

    @Column(name = "identity_subject", nullable = false, updatable = false)
    private String identitySubject;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ClientUser() {
    }

    public ClientUser(UUID clientAccountId, String identitySubject, String email, String fullName, Role role) {
        if (role.isStaff()) {
            throw new IllegalArgumentException("Client users can only hold customer roles");
        }
        this.clientAccountId = clientAccountId;
        this.identitySubject = identitySubject;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientAccountId() {
        return clientAccountId;
    }

    public String getIdentitySubject() {
        return identitySubject;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
