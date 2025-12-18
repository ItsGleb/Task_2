package ru.education_services.stellarburgers.pojo_model.patch;


import ru.education_services.stellarburgers.pojo_model.register.RegisterRequest;

public class PatchRequest extends RegisterRequest {

    public PatchRequest(String parameterWhichWillChanged) {
        super(null, null, null);
        if (parameterWhichWillChanged == null || parameterWhichWillChanged.isEmpty()) {
            throw new IllegalArgumentException("Значение для обновления не может быть пустым");
        }
        if (parameterWhichWillChanged.contains("@") && parameterWhichWillChanged.contains(".")) {
            setEmail(parameterWhichWillChanged);
        } else if (parameterWhichWillChanged.contains("password")) {
            setPassword(parameterWhichWillChanged);
        } else if (parameterWhichWillChanged.matches("^[a-zA-Z\\s]+$")) {
            setName(parameterWhichWillChanged);
        } else throw new IllegalArgumentException("Не удалось определить тип данных для обновления");
        /*
         * ("^[a-zA-Z\\s]+$") - разрешает латинские символы и пробелы
         * contains("password") - делаем так чтобы точно не попасть в поле name
         * */

    }


}
