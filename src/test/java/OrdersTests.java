
import io.restassured.RestAssured;
import io.restassured.response.Response;
import jdk.jfr.Description;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.education_services.stellarburgers.pojo_model.orders.OrdersRequest;
import ru.education_services.stellarburgers.steps.Steps;

import java.net.HttpURLConnection;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static ru.education_services.stellarburgers.config.EnvConfig.*;


public class OrdersTests {

    private Steps steps;
    private String authToken;

    @BeforeEach
    public void setUp() {
        // Базовая часть URL
        RestAssured.baseURI = BASE_URL;
        steps = new Steps();
    }

    @Test
    @DisplayName("Создание заказа с авторизацией и с ингредиентами")
    @Description("Проверка успешного создания заказа авторизованном пользователем")
    public void createOrderWithAuthorazedTest() {
        // Регистрация, логин и получение токена после логина
        authToken = steps.registerAndLogin().jsonPath().getString("accessToken");
        // Запрашиваем список ингредиентов
        Response getIngredientResponse = steps.getIngredients(authToken);
        List<String> ingredientIdList = getIngredientResponse.jsonPath().getList("data._id");
        OrdersRequest ordersRequest = new OrdersRequest(ingredientIdList, 2);
        Response orderResponse = steps.postOrders(ordersRequest, ORDERS_URL, authToken);
        // Проверяем наличие флага success
        assertTrue(orderResponse.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertTrue(orderResponse.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение true");
        // Проверяем объект order
        assertTrue(orderResponse.jsonPath().getMap("$").containsKey("order"), "Отсутствует" +
                "объект order");
        // Проверяем что вернулся номер заказа
        assertTrue(orderResponse.then().extract().path("order.number") != null);
        // Проверяем наличие флага success
        assertTrue(orderResponse.jsonPath().getMap("$").containsKey("name"));
        // Добавляем для некритических проверок
        SoftAssertions softly = new SoftAssertions();
        // Проверяем что статус-код = 200
        softly.assertThat(orderResponse.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
        // Нужно для того чтобы вывелась ошибка
        softly.assertAll();
        /* Для проверки что получаю {"data":["61c0c5a71d1f82001bdaaa6d","61c0c5a71d1f82001bdaaa6f"]}
        Gson gson = new Gson();
        String olo = gson.toJson(ordersRequest);
        System.out.println(olo);
        */
    }

    @Test
    @DisplayName("Создание заказа с авторизацией без ингредиентов")
    @Description("Проверка что нельзя создать заказ без ингредиентов")
    public void createOrderWithAuthorazedButWithoutIngredientsTest() {
        // Регистрация, логин и получение токена после логина
        authToken = steps.registerAndLogin().jsonPath().getString("accessToken");
        String expectedMessage = "Ingredient ids must be provided";
        List<String> emptyList = new ArrayList<>();
        OrdersRequest ordersRequest = new OrdersRequest(emptyList);
        Response orderResponse = steps.postOrders(ordersRequest, ORDERS_URL, authToken);
        // Проверяем наличие флага success
        assertTrue(orderResponse.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertFalse(orderResponse.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение false");
        // Проверяем значение параметра message
        assertEquals(expectedMessage, orderResponse.jsonPath().getString("message"));
        // Проверяем статус-код = 400
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, orderResponse.statusCode(), "Статус-код" +
                "должен быть 400");
    }

    @Test
    @DisplayName("Создание заказа с авторизацией и неверным хешем ингредиентов")
    @Description("Проверка корректной работы при невалидных хешах")
    public void createOrderWithInvalidHashTest() {
        // Регистрация, логин и получение токена после логина
        authToken = steps.registerAndLogin().jsonPath().getString("accessToken");
        // Запрашиваем список ингредиентов
        Response getIngredientResponse = steps.getIngredients(authToken);
        List<String> ingredientIdList = getIngredientResponse.jsonPath().getList("data._id");
        OrdersRequest ordersRequest = new OrdersRequest(ingredientIdList);
        // Ломаем список id ингредиентов
        ordersRequest.makeInvalidIngredients();
        // Делаем запрос с невалидными ингредиентами
        Response orderResponse = steps.postOrders(ordersRequest, ORDERS_URL, authToken);
        // Проверяем статус-код
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, orderResponse.statusCode(), "Статус-код" +
                "должен быть 500");
    }

    @Test
    @DisplayName("Создание заказа без авторизации")
    @Description("Проверка что невозможно создать заказ без авторизации")
    public void createOrderWithoutAuthorizationTest() {
        String expectedMessage = "You should be authorised";
        Response getIngredientResponse = steps.getIngredients("");
        List<String> ingredientIdList = getIngredientResponse.jsonPath().getList("data._id");
        OrdersRequest ordersRequest = new OrdersRequest(ingredientIdList, 2);
        Response orderResponse = steps.postOrders(ordersRequest, ORDERS_URL, "");
        // Проверяем наличие флага success
        assertTrue(orderResponse.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertFalse(orderResponse.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение false");
        // Проверяем значение параметра message
        assertEquals(expectedMessage, orderResponse.jsonPath().getString("message"));
        // Проверяем статус-код = 401
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, orderResponse.statusCode(), "Статус-код" +
                "должен быть 401");
    }

    @Test
    @DisplayName("Получение заказов конкретного пользователя")
    @Description("Проверка корректности тела ответа")
    public void getUserOrdersTest() {
        // Ожидаемая длина списка orders
        int expectedOrdersSize = 50;
        // Для сравнения дат в массиве ордерс
        ZonedDateTime updatedDateLater;
        ZonedDateTime updatedDateEarlier;

        // Регистрация, логин и получение токена после логина
        authToken = steps.registerAndLogin().jsonPath().getString("accessToken");
        // Запрашиваем список ингредиентов
        Response getIngredientResponse = steps.getIngredients(authToken);
        List<String> ingredientIdList = getIngredientResponse.jsonPath().getList("data._id");
        // Подготавливаем и отправляем первый заказ
        OrdersRequest ordersRequestFirst = new OrdersRequest(ingredientIdList, 3);
        steps.postOrders(ordersRequestFirst, ORDERS_URL, authToken);
        // Подготавливаем и отправляем второй заказ
        OrdersRequest ordersRequestSecond = new OrdersRequest(ingredientIdList, 2);
        steps.postOrders(ordersRequestSecond, ORDERS_URL, authToken);
        // Получение списка заказов
        Response getUserOrders = steps.getUserOrder(authToken);

        // Забираем список заказов. Признаюсь, нагуглил ---> List<Map<String, Object>>
        List<Map<String, Object>> orders = getUserOrders.jsonPath().getList("orders");
        // Проверяем наличие флага success
        assertTrue(getUserOrders.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertTrue(getUserOrders.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение true");
        // Проверяем статус-код = 200
        assertEquals(HttpURLConnection.HTTP_OK, getUserOrders.statusCode(), "Статус-код" +
                "должен быть 200");
        // Проверяем длину списка заказов
        assertTrue(orders.size() <= expectedOrdersSize, "Список заказов больше 50");
        /* Перебираем список и сравниваем даты. Минус 1 нам нужен чтобы сравнение закончилось на предпоследнем и последнем элементе.
         *  С циклом сам придумал. Сложность была в том как вытащить orders в список без десериализации в POJO объект
         * */
        for (int i = 0; i < orders.size() - 1; i++) {
            updatedDateEarlier = ZonedDateTime.parse(orders.get(i).get("updatedAt").toString());
            updatedDateLater = ZonedDateTime.parse(orders.get(i + 1).get("updatedAt").toString());
            // Проверяем что заказы отсортированы по параметру updateAt
            assertTrue(updatedDateLater.isAfter(updatedDateEarlier), "Сортировка заказов по дате обновления " +
                    "должна быть по убыванию");
        }
        // Проверяем наличие параметров total и totalToday
        assertTrue(getUserOrders.jsonPath().getMap("$").containsKey("total"), "Отсутствует " +
                "параметр total");
        assertTrue(getUserOrders.jsonPath().getMap("$").containsKey("totalToday"), "Отсутствует " +
                "параметр totalToday");
    }
    @Test
    @DisplayName("Получение заказов неавторизованным пользователем")
    @Description("Проверка корректности ответа")
    public void getUnauthorazedUserOrdersTest(){
        String expectedMessage = "You should be authorised";
        // Получение списка заказов
        Response getUserOrders = steps.getUserOrder("");
        // Проверяем наличие флага success
        assertTrue(getUserOrders.jsonPath().getMap("$").containsKey("success"));
        // Проверяем положение флага success
        assertFalse(getUserOrders.jsonPath().getBoolean("success"), "Флаг success должен " +
                "иметь значение false");
        // Проверяем статус-код = 401
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, getUserOrders.statusCode(), "Статус-код" +
                "должен быть 401");
        // Проверяем значение параметра message
        assertEquals(expectedMessage, getUserOrders.jsonPath().getString("message"));
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
