package com.esprit.demo.Dto;

import com.esprit.demo.Models.XpSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpEventResponse {
    private Long id;
    private XpSource sourceType;
    private int amount;
    private long newTotal;
    private int newLevel;
    private String description;
    private boolean levelUp;
    private LocalDateTime createdAt;
}
