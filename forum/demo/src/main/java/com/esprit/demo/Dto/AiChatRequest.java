package com.esprit.demo.Dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiChatRequest {
    private String message;
    private List<Map<String, String>> history;
}
