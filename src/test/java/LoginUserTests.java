import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.education_services.stellarburgers.pojo_model.login.LoginRequest;
import ru.education_services.stellarburgers.pojo_model.register.RegisterRequest;
import ru.education_services.stellarburgers.steps.Steps;

import java.net.HttpURLConnection;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.education_services.stellarburgers.config.EnvConfig.*;

public class LoginUserTests {

    private LoginRequest loginRequest;
    private Steps steps;

    private String authToken;

    private RegisterRequest registerRequest;


    @BeforeEach
    public void setUp() {
        // Базовая часть URL
        RestAssured.baseURI = BASE_URL;
        steps = new Steps();
    }

    @Test
    @DisplayName("Успешная авторизация")
    @Description("Проверка авторизации с уже существующим пользователем")
    public void successAuthorizationTest(){
        // Создаем пользователя
        registerRequest = RegisterRequest.generateRandomRequest();
        // Регистрируем пользователя
        steps.registerUser(registerRequest,REGISTER_URL);
        // Создаем объект для логина
        loginRequest = new LoginRequest(registerRequest.getEmail(), registerRequest.getPassword());
        // Авторизуемся созданным пользователем
        Response response = steps.loginUser(loginRequest,LOGIN_URL);
        // Проверяем 2 важных параметра
        assertNotNull(response.jsonPath().get("accessToken"));
        // Вытаскиваем токен для того чтобы могли удалить пользователя потом
        authToken = response.jsonPath().get("accessToken");
        // Проверяем что accessToken имеет правильный формат
        assertTrue(authToken.startsWith("Bearer "), "accessToken должен начинаться с 'Bearer '");
        // Проверяем что refreshToken не пустой
        assertNotNull(response.jsonPath().get("refreshToken"), "refreshToken обязателен");
        // Добавляем для некритических проверок
        SoftAssertions softly = new SoftAssertions();
        // Проверяем статус-код
        softly.assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
        // Проверяем флаг success
        softly.assertThat(response.jsonPath().getBoolean("success")).isTrue();
        // Проверяем что user не пустой
        softly.assertThat(response.jsonPath().getMap("user")).as("Поле 'user' должно быть объектом")
                .isNotNull()
                .isNotEmpty();
        // Тут используем loginRequest, т.к. передали его в запросе на авторизацию
        softly.assertThat(response.jsonPath().getMap("user").get("email")).isEqualTo(loginRequest.getEmail());
        // Проверяем что name == name с которым пользователя создали
        softly.assertThat(response.jsonPath().getMap("user").get("name")).isEqualTo(registerRequest.getName());
        // Нужно для того чтобы вывелась ошибка
        softly.assertAll();
    }
    @Test
    @DisplayName("Авторизация с несуществующим пользователем")
    @Description("Проверка невозможности авторизации с несуществующим пользователем")
    public void unsuccessAuthorizationWithUnExistUser(){
        loginRequest = new LoginRequest("unexist@yandex.ru","Unexistpassword");
        String expectedMessage = "email or password are incorrect";
        Response response = steps.loginUser(loginRequest,LOGIN_URL);
        // Забираем токен для удаления созданного пользователя(на всякий случай)
        authToken = response.jsonPath().get("accessToken");
        // Проверяем наличие флага success
        assertTrue(response.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertFalse(response.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение false");
        // Проверяем наличие параметра message
        assertTrue(response.jsonPath().getMap("$").containsKey("message"));
        // Проверяем значение параметра message
        assertEquals(expectedMessage, response.jsonPath().getString("message"));
        // Добавляем для некритических проверок
        SoftAssertions softly = new SoftAssertions();
        // Проверяем что статус-код = 401
        softly.assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_UNAUTHORIZED);
        // Нужно для того чтобы вывелась ошибка
        softly.assertAll();
    }


    @AfterEach
    public void deletingTheCreatedData() {
        // Пробуем удалить, если токен кривой или пустая строка, то увидим исключение по идее
        // Не удаляем если токен не пришел в ответе
        if (authToken != null) {
            try {
                given().log().all()
                        .header("Authorization", authToken)
                        .header("Content-Type", "application/json")
                        .when()
                        .delete(DELETE_OR_PATCH_URL);
            } catch (Exception e) {
                System.out.println("Удаление созданного пользователя завершилось с ошибкой: " + e.getMessage());
            }

        }
    }

}
