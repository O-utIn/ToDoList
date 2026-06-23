package com.example.todolist.ai;

import java.util.List;

public class AiParseResponse {
    public String title;
    // epoch millis for due date, optional
    public Long due;
    public String note;
    public List<String> tags;
}
