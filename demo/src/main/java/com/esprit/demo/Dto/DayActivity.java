package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayActivity {
    private String weekLabel;
    private long postsCount;
    private long threadsCount;
    private long commentsCount;
}
