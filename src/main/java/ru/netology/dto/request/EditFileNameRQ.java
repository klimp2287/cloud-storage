package ru.netology.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class EditFileNameRQ {

    @NotBlank(message = "filename must not be blank")
    private String filename;
}