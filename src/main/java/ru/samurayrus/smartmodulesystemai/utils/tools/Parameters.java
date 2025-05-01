package ru.samurayrus.smartmodulesystemai.utils.tools;

import java.util.List;

public class Parameters {
    private String type;
    private Properties properties;
    private List<String> required;
    private boolean additionalProperties;

    // Геттеры и сеттеры
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public boolean isAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(boolean additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}
