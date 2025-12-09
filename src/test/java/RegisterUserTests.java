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
import ru.education_services.stellarburgers.pojo_model.register.RegisterRequest;
import ru.education_services.stellarburgers.steps.Steps;

import java.net.HttpURLConnection;

import static io.restassured.RestAssured.given;

import static org.junit.jupiter.api.Assertions.*;
import static ru.education_services.stellarburgers.config.EnvConfig.*;
import static ru.education_services.stellarburgers.pojo_model.register.RegisterRequest.generateRandomRequest;


public class RegisterUserTests {

    private RegisterRequest registerRequest;
    private Steps steps;
    private String authToken;

    @BeforeEach
    public void setUp() {
        // Базовая часть URL
        RestAssured.baseURI = BASE_URL;
        steps = new Steps();
    }

    @Test
    @DisplayName("Создание нового пользователя")
    @Description("Проверка создания нового пользователя с заполнением всех полей")
    public void createUserShouldSuccess() {
        // Создаем тело запроса
        registerRequest = generateRandomRequest();
        // Отправляем запрос
        Response response = steps.registerUser(registerRequest, REGISTER_URL);
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
        softly.assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_CREATED);
        // Проверяем флаг success
        softly.assertThat(response.jsonPath().getBoolean("success")).isTrue();
        // Проверяем что user не пустой
        softly.assertThat(response.jsonPath().getMap("user")).as("Поле 'user' должно быть объектом")
                .isNotNull()
                .isNotEmpty();
        softly.assertThat(response.jsonPath().getMap("user").get("email")).isEqualTo(registerRequest.getEmail());
        softly.assertThat(response.jsonPath().getMap("user").get("name")).isEqualTo(registerRequest.getName());
        // Нужно для того чтобы вывелась ошибка
        softly.assertAll();
    }

    @Test
    @DisplayName("Создание пользователя который уже зарегистрирован")
    @Description("Проверка что невозможно создать двух одинаковых пользователей")
    public void createTheSameUserShouldReturnErrorTest() {
        // Создаем тело запроса
        registerRequest = generateRandomRequest();
        String expectedMessage = "User already exists";
        // Отправляем запрос
        Response response = steps.registerUser(registerRequest, REGISTER_URL);
        // Забираем токен для удаления созданного пользователя
        authToken = response.jsonPath().get("accessToken");
        Response responseWithTheSameUser = steps.registerUser(registerRequest, REGISTER_URL);
        // Проверяем наличие флага success
        assertTrue(responseWithTheSameUser.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertFalse(responseWithTheSameUser.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение false");
        // Проверяем наличие параметра message
        assertTrue(responseWithTheSameUser.jsonPath().getMap("$").containsKey("message"));
        // Проверяем значение параметра message
        assertEquals(expectedMessage, responseWithTheSameUser.jsonPath().getString("message"));
        // Добавляем для некритических проверок
        SoftAssertions softly = new SoftAssertions();
        // Проверяем что статус-код = 409, т.к. сущность пользователя была создана ранее
        softly.assertThat(responseWithTheSameUser.statusCode()).isEqualTo(HttpURLConnection.HTTP_CONFLICT);
        // Нужно для того чтобы вывелась ошибка
        softly.assertAll();
    }



    @ParameterizedTest(name = "email={0}, password={1}, name={2}")
    @CsvSource({
            "test5-email@yandex.ru, password, ''",
            "test5-email@yandex.ru, '', IvanTest",
            "'', password, IvanTest"
    })
    @DisplayName("Создание пользователя без одного из обязательных полей")
    @Description("Проверка что невозможно создать пользователя если не заполнены обязательные поля")
    public void createUserWithoutRequredFieldTest(String email, String password, String name) {
        registerRequest = new RegisterRequest(email, password, name);
        Response response = steps.registerUser(registerRequest, REGISTER_URL);
        String expectedMessage = "Email, password and name are required fields";
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
        // Проверяем что статус-код = 400, т.к. сущность пользователя была создана ранее
        softly.assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST);
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
