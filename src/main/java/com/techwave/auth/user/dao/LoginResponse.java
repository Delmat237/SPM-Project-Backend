package com.techwave.auth.user.dao;

import java.util.List;

public class LoginResponse {
    private String token;
    private UserInfo user;

    public LoginResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    public static class UserInfo {
        private Long id;
        private String email;
        private String nom;
        private List<String> roles;

        public UserInfo(Long id, String email , String nom, List<String> roles) {
            this.id = id;
            this.email = email ;
            this.nom = nom;
            this.roles = roles;
        }

        public Long getId() { return id; }
        public String getUsername() { return email; }
        public String getNom() { return nom; }
        public List<String> getRoles() { return roles; }
    }

    @Override
    public String toString() {
        return "LoginResponse{token='" + token + "', user=" + user + "}";
    }
}