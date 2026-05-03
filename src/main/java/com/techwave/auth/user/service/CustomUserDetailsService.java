package com.techwave.auth.user.service;

import com.techwave.auth.user.model.User;
import com.techwave.auth.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = Logger.getLogger(CustomUserDetailsService.class.getName());
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.info("Tentative de chargement de l'utilisateur : " + username);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    LOGGER.severe("Utilisateur non trouvé : " + username);
                    return new UsernameNotFoundException("Utilisateur non trouvé : " + username);
                });
        LOGGER.info("Utilisateur trouvé : " + user.getUsername() +
                " Nom : " + user.getNom() +
                " rôles : " + user.getRoles());

        return user;
    }


}