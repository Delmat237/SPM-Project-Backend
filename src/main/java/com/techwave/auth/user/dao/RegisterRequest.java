package com.techwave.auth.user.dao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "ne doit pas être vide")
    private String email;
    @NotBlank @Size(min = 8) private String password;
    @NotBlank private String nom;
    private String role; // "USER" ou "ADMIN"

    private String telephone;
    private String pays;

    // Getters et Setters
    public String getUsername() { return email; }
    public void setEmail(String email) { this.email = email ; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }
}