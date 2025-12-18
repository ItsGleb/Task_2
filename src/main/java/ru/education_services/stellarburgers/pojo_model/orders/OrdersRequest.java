package ru.education_services.stellarburgers.pojo_model.orders;


import java.util.ArrayList;
import java.util.List;

public class OrdersRequest {

    private List<String> ingredients;

    public OrdersRequest(List<String> data) {
        this.ingredients = new ArrayList<>();
        for (String item : data) {
            this.ingredients.add(item);
        }

    }

    public OrdersRequest(List<String> data, int numberOfIngredients) {
        this.ingredients = new ArrayList<>(); // Создаем новый пустой список
        for (int i = 0; i < numberOfIngredients; i++) {
            this.ingredients.add(data.get(i)); // кладем только нужное количество ингредиентов
        }
    }

    public List<String> getIngredients() {
        return this.ingredients;
    }

    public void setIngredients(List<String> data) {
        this.ingredients = new ArrayList<>(data);
    }

    public void makeInvalidIngredients() {
        List<String> invalidIngredients = new ArrayList<>();
        for (String item : this.ingredients) {
            invalidIngredients.add(item + "Invalid");
        }
        this.ingredients = invalidIngredients;
    }

}
