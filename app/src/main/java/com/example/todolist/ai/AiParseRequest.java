package com.example.todolist.ai;

public class AiParseRequest {
    public String text;
    public String system;

    public AiParseRequest(String text, String system) {
        this.text = text;
        this.system = system;
    }
}
