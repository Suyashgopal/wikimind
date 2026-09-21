package com.wikimind.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    public AppUser() {
    }

    public AppUser(String username, String passwordHash, Role role, String orgId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.orgId = orgId;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getOrgId() {
        return orgId;
    }
}
