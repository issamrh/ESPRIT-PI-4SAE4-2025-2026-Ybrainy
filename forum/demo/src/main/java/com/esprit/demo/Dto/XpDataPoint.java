package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpDataPoint {
    private LocalDate date;
    private int xpGained;
    private long newTotal;
    private String source;
}
