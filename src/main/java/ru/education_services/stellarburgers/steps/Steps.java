package ru.education_services.stellarburgers.steps;

import io.restassured.response.Response;
import io.qameta.allure.Step;
import ru.education_services.stellarburgers.pojo_model.login.LoginRequest;
import ru.education_services.stellarburgers.pojo_model.orders.OrdersRequest;
import ru.education_services.stellarburgers.pojo_model.patch.PatchRequest;
import ru.education_services.stellarburgers.pojo_model.register.RegisterRequest;


import static io.restassured.RestAssured.given;
import static ru.education_services.stellarburgers.config.EnvConfig.*;


public class Steps {
    @Step("Создание пользователя")
    public Response registerUser(RegisterRequest registerRequest, String urlPath) {
        Response response =
                given().log().method().log().uri().log().body()
                        .header("Content-Type", "application/json")
                        .body(registerRequest)
                        .when()
                        .post(urlPath)
                        .then()
                        .log().status()
                        .log().body()
                        .extract().response();
        return response;
    }

    @Step("Удаление пользователя")
    public Response deleteUser(String authToken, String urlPath) {
        Response response =
                given().log().method().log().uri()
                        .header("Authorization", "Bearer " + authToken)
                        .header("Content-Type", "application/json")
                        .when()
                        .delete(urlPath)
                        .then()
                        .log().status()
                        .log().body()
                        .extract().response();
        return response;
    }

    @Step("Логин пользователя")
    public Response loginUser(LoginRequest loginRequest, String urlPath) {
        Response response = given().log().body()
                .header("Content-Type", "application/json")
                .body(loginRequest)
                .when()
                .post(urlPath)
                .then()
                .log().status()
                .log().body()
                .extract().response();
        return response;
    }

    @Step("Обновление данных пользователя")
    public Response patchUser(PatchRequest patchRequest, String urlPath, String authToken) {
        Response response = given().log().method()
                .log().uri()
                .log().body()
                .header("Content-Type", "application/json")
                .header("Authorization", authToken)
                .body(patchRequest)
                .when()
                .patch(urlPath)
                .then()
                .log().status()
                .log().body()
                .extract().response();
        return response;
    }

    @Step("Получение ингредиентов")
    public Response getIngredients(String authToken) {
        Response response = given().log().uri()
                .when()
                .get(INGREDIENT_URL);
        return response;
    }

    @Step("Создание заказа")
    public Response postOrders(OrdersRequest ordersRequest, String urlPath, String authToken) {
        Response response = given().log().method()
                .log().uri()
                .log().body()
                .header("Content-Type", "application/json")
                .header("Authorization", authToken)
                .body(ordersRequest)
                .when()
                .post(urlPath)
                .then()
                .log().status()
                .log().body()
                .extract().response();
        return response;
    }

    @Step("Регистрация и логин пользователя без передачи объектов")
    public Response registerAndLogin() {
        // Создаем объект класса
        RegisterRequest registerRequest = RegisterRequest.generateRandomRequest();
        // Регистрируемся
        registerUser(registerRequest, REGISTER_URL);
        // Логинимся
        LoginRequest loginRequest = new LoginRequest(registerRequest.getEmail(), registerRequest.getPassword());
        Response response = loginUser(loginRequest, LOGIN_URL);
        return response;
    }

    @Step("Получение всех заказов")
    public Response getAllOrders() {
        Response response = given().log().uri()
                .when()
                .get(ALL_ORDERS_URL);
        return response;
    }
    @Step("Получение списка заказов пользователя")
    public Response getUserOrder(String authToken){
        Response response = given().log().uri()
                .header("Authorization", authToken)
                .when()
                .get(ORDERS_URL);
        return response;
    }
}
