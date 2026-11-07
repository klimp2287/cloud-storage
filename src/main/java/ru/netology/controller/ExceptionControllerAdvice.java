package ru.netology.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.netology.exception.*;
import ru.netology.dto.response.ExceptionRS;

@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionRS> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionRS(e.getMessage(), 400));
    }

    @ExceptionHandler(InputDataException.class)
    public ResponseEntity<ExceptionRS> handleInputData(InputDataException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionRS(e.getMessage(), 400));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ExceptionRS> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ExceptionRS(e.getMessage(), 401));
    }

    @ExceptionHandler(DeleteFileException.class)
    public ResponseEntity<ExceptionRS> handleDeleteFile(DeleteFileException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionRS(e.getMessage(), 500));
    }

    @ExceptionHandler(UploadFileException.class)
    public ResponseEntity<ExceptionRS> handleUploadFile(UploadFileException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionRS(e.getMessage(), 500));
    }

    @ExceptionHandler(GettingFileListException.class)
    public ResponseEntity<ExceptionRS> handleGettingFileList(GettingFileListException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionRS(e.getMessage(), 500));
    }
}