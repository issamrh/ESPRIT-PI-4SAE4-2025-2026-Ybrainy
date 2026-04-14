package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceInsight {
    /** POSITIVE | NEGATIVE | SUGGESTION */
    private String type;
    private String message;
    private String icon;
}
