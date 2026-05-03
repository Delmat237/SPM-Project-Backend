package com.techwave.auth.user.service;

import com.techwave.auth.user.model.User;
import com.techwave.auth.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("userEmailService")
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public User createUser(User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email obligatoire");
        }
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }


    public User updateUser(Long id, User user) {
        return userRepository.findById(id)
                .map(existing -> {
                    if(user.getEmail() != null) existing.setEmail(user.getEmail());
                    if(user.getNom() != null) existing.setNom(user.getNom());
                    if(user.getPays() != null) existing.setPays(user.getPays());
                    if(user.getTelephone() != null) existing.setTelephone(user.getTelephone());
                    return userRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    public void deactivateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setEnabled(false);
            userRepository.save(user);
        });
    }

}
