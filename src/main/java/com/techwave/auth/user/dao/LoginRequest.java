package com.techwave.auth.user.dao;

public class LoginRequest {
    private String email;
    private String password;
    private String nom;
    private String telephone; // Changé de Double à String
    private String pays;

    public String getUsername() { return email; }
    public void setEmail(String email ) { this.email = email ; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }
}