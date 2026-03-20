package com.example.novaledger.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UserRequest {
    @NotBlank
    private String name;

}
