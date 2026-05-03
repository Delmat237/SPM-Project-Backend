package com.techwave.auth.user.controller;

import com.techwave.auth.user.dao.LoginRequest;
import com.techwave.auth.user.dao.LoginResponse;
import com.techwave.auth.user.dao.RegisterRequest;
import com.techwave.auth.user.model.PasswordResetToken;
import com.techwave.auth.user.model.User;
import com.techwave.auth.user.model.VerificationToken;
import com.techwave.auth.user.repository.PasswordResetTokenRepository;
import com.techwave.auth.user.repository.UserRepository;
import com.techwave.auth.user.repository.VerificationTokenRepository;
import com.techwave.auth.user.security.JwtUtil;
import com.techwave.auth.user.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.backend.url}")
    private String backendUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${google.client.id:YOUR_GOOGLE_CLIENT_ID}")
    private String googleClientId;

    private static final Logger LOGGER = Logger.getLogger(AuthController.class.getName());

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private EmailService emailService;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    // =============================================
    // 🔹 LOGIN (connexion)
    // =============================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<User> optionalUser = userRepository.findByEmail(loginRequest.getUsername());
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(new ErrorResponse("Aucun compte associé à cet email. Veuillez créer un compte."));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = (User) authentication.getPrincipal();
            List<String> roles = user.getRoles().stream()
                    .map(r -> "ROLE_" + r)
                    .collect(Collectors.toList());
            String token = jwtUtil.generateToken(user.getUsername(), roles);

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                    user.getId(), user.getUsername(), user.getNom(), roles
            );

            return ResponseEntity.ok(new LoginResponse(token, userInfo));

        } catch (DisabledException e) {
            return ResponseEntity.status(403)
                    .body(new ErrorResponse("Veuillez activer votre compte avant de vous connecter. Vérifiez votre email."));
        } catch (Exception e) {
            // Ici, on renvoie toujours le même message clair pour les mauvais identifiants
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("Échec de l'authentification : Vérifiez votre email ou votre mot de passe"));
        }
    }

    // =============================================
    // 🔹 GOOGLE SSO LOGIN
    // =============================================
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        try {
            String tokenString = payload.get("token");
            if (tokenString == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Token Google manquant"));
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(tokenString);
            if (idToken != null) {
                GoogleIdToken.Payload googlePayload = idToken.getPayload();
                String email = googlePayload.getEmail();
                String name = (String) googlePayload.get("name");

                Optional<User> optionalUser = userRepository.findByEmail(email);
                User user;
                if (optionalUser.isPresent()) {
                    user = optionalUser.get();
                    if (!user.isEnabled()) {
                        user.setEnabled(true);
                        userRepository.save(user);
                    }
                } else {
                    user = new User();
                    user.setEmail(email);
                    user.setNom(name);
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Mot de passe aléatoire
                    user.setRoles(Collections.singletonList("USER"));
                    user.setEnabled(true); // Activé directement
                    user = userRepository.save(user);
                }

                List<String> roles = user.getRoles().stream()
                        .map(r -> "ROLE_" + r)
                        .collect(Collectors.toList());
                String jwt = jwtUtil.generateToken(user.getUsername(), roles);

                LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                        user.getId(), user.getUsername(), user.getNom(), roles
                );

                return ResponseEntity.ok(new LoginResponse(jwt, userInfo));
            } else {
                return ResponseEntity.status(401).body(new ErrorResponse("Token Google invalide"));
            }
        } catch (Exception e) {
            LOGGER.severe("Erreur SSO Google : " + e.getMessage());
            return ResponseEntity.status(500).body(new ErrorResponse("Erreur lors de la connexion Google"));
        }
    }



    // =============================================
    // 🔹 REGISTER (inscription)
    // =============================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        LOGGER.info("Tentative d'inscription pour : " + registerRequest.getUsername());

        try {
            if (userRepository.findByEmail(registerRequest.getUsername()).isPresent()) {
                LOGGER.warning("L'utilisateur existe déjà : " + registerRequest.getUsername());
                return ResponseEntity.status(400).body(new ErrorResponse("L'utilisateur existe déjà."));
            }

            // Création du nouvel utilisateur
            User user = new User();
            user.setEmail(registerRequest.getUsername());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setNom(registerRequest.getNom());
            user.setPays(registerRequest.getPays());
            user.setTelephone(registerRequest.getTelephone());
            user.setRoles(Collections.singletonList("USER"));
            user.setEnabled(false); // Inactif tant qu’il n’a pas cliqué sur le lien

            User savedUser = userRepository.save(user);

            // Génération du code OTP (6 chiffres)
            String token = String.format("%06d", new java.util.Random().nextInt(999999));
            VerificationToken verificationToken = new VerificationToken();
            verificationToken.setToken(token);
            verificationToken.setUser(savedUser);
            verificationToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // Réduction à 1h pour sécurité
            verificationTokenRepository.save(verificationToken);

            // Envoi du mail d’activation (envoie juste le code)
            emailService.sendActivationEmail(savedUser.getEmail(), savedUser.getNom(), token);

            return ResponseEntity.ok(new ErrorResponse(
                    "Inscription réussie ✅. Un code a été envoyé à votre adresse email."
            ));
        } catch (Exception e) {
            LOGGER.severe("Erreur d'inscription : " + e.getMessage());
            return ResponseEntity.status(400).body(new ErrorResponse("Échec de l'inscription : " + e.getMessage()));
        }
    }

    // =============================================
    // 🔹 ACTIVATE (activation du compte)
    // =============================================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam("email") String email, @RequestParam("code") String code) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Utilisateur non trouvé."));
        }

        User user = optionalUser.get();

        if (user.isEnabled()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Le compte est déjà activé."));
        }

        // On cherche le token associé à cet utilisateur
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByUser(user);

        if (verificationToken.isEmpty() || !verificationToken.get().getToken().equals(code)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Code invalide."));
        }

        VerificationToken tokenEntity = verificationToken.get();

        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Le code est expiré. Veuillez en demander un nouveau."));
        }

        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenRepository.delete(tokenEntity);

        return ResponseEntity.ok(new ErrorResponse("Compte activé avec succès !"));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Email non trouvé."));
        }

        User user = optionalUser.get();
        String token = UUID.randomUUID().toString();

        // Vérifie si un token existe déjà pour cet utilisateur
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            // Met à jour le token existant
            PasswordResetToken resetToken = existingToken.get();
            resetToken.setToken(token);
            resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(resetToken);
        } else {
            // Crée un nouveau token
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(resetToken);
        }

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        emailService.sendResetPasswordEmail(user.getEmail(), user.getNom(), resetLink);

        return ResponseEntity.ok(new ErrorResponse("Lien de réinitialisation envoyé par email."));
    }


    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam("token") String token, @RequestParam("newPassword") String newPassword) {
        Optional<PasswordResetToken> optionalToken = passwordResetTokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Token invalide."));
        }

        PasswordResetToken resetToken = optionalToken.get();
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Token expiré."));
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken); // supprimer le token après usage

        return ResponseEntity.ok(new ErrorResponse("Mot de passe réinitialisé avec succès !"));
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(@RequestParam("email") String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Email non trouvé."));
        }

        User user = optionalUser.get();
        if (user.isEnabled()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Ce compte est déjà actif."));
        }

        // Supprimer les anciens tokens s'ils existent
        verificationTokenRepository.deleteByUser(user);

        // Génération d'un nouveau code OTP (6 chiffres)
        String token = String.format("%06d", new java.util.Random().nextInt(999999));
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // 1h
        verificationTokenRepository.save(verificationToken);

        emailService.sendActivationEmail(user.getEmail(), user.getNom(), token);

        return ResponseEntity.ok(new ErrorResponse("Un nouveau code a été envoyé par email."));
    }

}

