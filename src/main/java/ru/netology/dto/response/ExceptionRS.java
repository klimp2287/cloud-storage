package ru.netology.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExceptionRS {

    private String message;
    private Integer id;
}