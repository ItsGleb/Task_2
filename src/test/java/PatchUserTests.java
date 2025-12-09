import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.education_services.stellarburgers.pojo_model.login.LoginRequest;
import ru.education_services.stellarburgers.pojo_model.patch.PatchRequest;
import ru.education_services.stellarburgers.pojo_model.register.RegisterRequest;
import ru.education_services.stellarburgers.steps.Steps;

import java.net.HttpURLConnection;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static ru.education_services.stellarburgers.config.EnvConfig.*;

public class PatchUserTests {
    private RegisterRequest registerRequest;
    private Steps steps;
    private String authToken;
    private PatchRequest patchRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    public void setUp() {
        // Базовая часть URL
        RestAssured.baseURI = BASE_URL;
        steps = new Steps();
    }

    @ParameterizedTest
    @DisplayName("Изменение данных пользователя с авторизацией")
    @Description("Проверка что можно изменить каждое поле созданного пользователя")
    @CsvSource({
            "changed_email@yandex.ru", "changedPassword", "changedName"
    })
    public void patchUserFieldTestWithAuthorizationShouldBeSuccess(String parameterWhichWillChanged) {
        // Создаем объект класса
        registerRequest = new RegisterRequest("test6-email@yandex.ru", "password", "Ivan");
        // Регистрируемся
        steps.registerUser(registerRequest, REGISTER_URL);
        // Логинимся
        loginRequest = new LoginRequest(registerRequest.getEmail(), registerRequest.getPassword());
        Response loginResponse = steps.loginUser(loginRequest, LOGIN_URL);
        // Забираем токен при логине
        authToken = loginResponse.jsonPath().getString("accessToken");
        // Создаем запрос на обновление данных
        patchRequest = new PatchRequest(parameterWhichWillChanged);
        Response patchResponse = steps.patchUser(patchRequest, DELETE_OR_PATCH_URL, authToken);
        // Проверяем наличие флага success
        assertTrue(patchResponse.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertTrue(patchResponse.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение true");
        // Проверяем объект user
        assertTrue(patchResponse.jsonPath().getMap("$").containsKey("user"), "Отсутствует" +
                "объект user");
        /* Проверяем что вернулось поле name. Пришлось делать так ,т.к. метод has() идея не видела.
         *  Я так и не понял в чем дело. */
        assertTrue(patchResponse.then().extract().path("user.name") != null);
        // Проверяем что вернулось поле email
        assertTrue(patchResponse.then().extract().path("user.email") != null);

        // Проверки в зависимости от параметра
        if (parameterWhichWillChanged.contains("@") && parameterWhichWillChanged.contains(".")) {
            // Проверяем поле email'a
            assertEquals(parameterWhichWillChanged, patchResponse.jsonPath().getString("user.email"),
                    "Значение email не было изменено");
            // Проверяем поле name
            assertEquals(registerRequest.getName(), patchResponse.jsonPath().getString("user.name"),
                    "Значение name было изменено");

        } else if (parameterWhichWillChanged.contains("password")) {
            // Проверяем поля которые не должны измениться
            assertEquals(registerRequest.getName(), patchResponse.jsonPath().getString("user.name"),
                    "Значение name было изменено");
            assertEquals(registerRequest.getEmail(), patchResponse.jsonPath().getString("user.email"),
                    "Значение email было изменено");
            // Создаем тело логина с новым паролем
            LoginRequest loginRequestAfterPasswordChanged = new LoginRequest(registerRequest.getEmail(),
                    parameterWhichWillChanged);
            // Делаем логин с новым паролем
            Response responseAfterPasswordChanged = steps.loginUser(loginRequestAfterPasswordChanged, LOGIN_URL);
            // Берем свежий токен при логине с новым паролем
            authToken = responseAfterPasswordChanged.jsonPath().getString("accessToken");
            // Проверяем успех авторизации
            assertTrue(responseAfterPasswordChanged.jsonPath().getBoolean("success"),
                    "Флаг success должен иметь значение true");

        } else if (parameterWhichWillChanged.matches("^[a-zA-Z\\s]+$")) {
            // Проверяем поле email'a
            assertEquals(registerRequest.getEmail(), patchResponse.jsonPath().getString("user.email"),
                    "Значение email было изменено");
            // Проверяем поле name
            assertEquals(parameterWhichWillChanged, patchResponse.jsonPath().getString("user.name"),
                    "Значение name не было изменено");
        }
        // Добавляем для некритических проверок
        SoftAssertions softly = new SoftAssertions();
        // Проверяем что статус-код = 200
        softly.assertThat(patchResponse.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
        // Нужно для того чтобы вывелась ошибка
        softly.assertAll();
    }

    @Test
    @DisplayName("Изменение данных пользователя без авторизации")
    @Description("Проверка что нельзя изменить поле созданного пользователя без авторизации")
    public void patchUserFieldTestWithoutAuthorizationShouldBeUnsuccess() {
        // Создаем объект класса
        registerRequest = new RegisterRequest("test6-email@yandex.ru", "password", "Ivan");
        String expectedMessage = "You should be authorised";
        // Регистрируемся
        Response registerResponse = steps.registerUser(registerRequest, REGISTER_URL);
        // Забираем токен при регистрации
        authToken = registerResponse.jsonPath().getString("accessToken");
        // Создаем запрос на обновление данных
        patchRequest = new PatchRequest("changed_email@yandex.ru");
        Response patchResponse = steps.patchUser(patchRequest, DELETE_OR_PATCH_URL, "");
        // Проверяем наличие флага success
        assertTrue(patchResponse.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertFalse(patchResponse.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение false");
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED,patchResponse.statusCode(),"Статус-код" +
                "должен быть 401");
        // Проверяем наличие параметра message
        assertTrue(patchResponse.jsonPath().getMap("$").containsKey("message"));
        // Проверяем значение параметра message
        assertEquals(expectedMessage, patchResponse.jsonPath().getString("message"));
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

