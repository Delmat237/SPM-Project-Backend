package com.techwave.auth.user.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    private String nom;
    private String pays;
    private String telephone;
    private String password;
    private boolean enabled = false;

    // ---- Préférences de rappels (échéances de tâches) ----
    @ColumnDefault("true")
    @Column(name = "reminder_email_enabled", nullable = false)
    private boolean reminderEmailEnabled = true;

    @ColumnDefault("true")
    @Column(name = "reminder_in_app_enabled", nullable = false)
    private boolean reminderInAppEnabled = true;

    @ColumnDefault("false")
    @Column(name = "reminder_push_enabled", nullable = false)
    private boolean reminderPushEnabled = false;

    /** Nombre de jours avant l'échéance pour déclencher le rappel. */
    @ColumnDefault("1")
    @Column(name = "reminder_days_before", nullable = false)
    private int reminderDaysBefore = 1;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles; // ✅ Seule source des rôles



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ✅ Transforme ADMIN → ROLE_ADMIN
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return this.enabled; }
}
