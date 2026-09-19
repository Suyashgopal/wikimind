package com.wikimind.dto;

import jakarta.validation.constraints.NotBlank;

public record AddRequest(
        @NotBlank(message = "title must not be blank") String title,
        @NotBlank(message = "content must not be blank") String content
) {
}
