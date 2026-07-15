package com.yatayat.backend.dto;

public class AdminLoginRequest {

    private String email;
    private String password;

    public AdminLoginRequest() {
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}