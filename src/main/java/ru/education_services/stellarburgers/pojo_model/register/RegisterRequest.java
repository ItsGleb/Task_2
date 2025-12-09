package ru.education_services.stellarburgers.pojo_model.register;

import java.util.Random;

public class RegisterRequest {

    private String email;
    private String password;
    private String name;

    public RegisterRequest(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static RegisterRequest generateRandomRequest(){
        Random random = new Random();
        String email = "testik"+random.nextInt(100)+"email@yandex.ru";
        String password = "password"+random.nextInt(100);
        String name = "Testovich"+random.nextInt(100);
        RegisterRequest registerRequest = new RegisterRequest(email,password,name);
        return registerRequest;
    }
}
