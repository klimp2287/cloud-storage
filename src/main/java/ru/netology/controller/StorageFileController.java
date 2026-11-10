package ru.netology.controller;

import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.dto.request.EditFileNameRQ;
import ru.netology.dto.response.FileRS;
import ru.netology.exception.InputDataException;
import ru.netology.exception.UnauthorizedException;
import ru.netology.model.StorageFile;
import ru.netology.model.User;
import ru.netology.repository.AuthenticationRepository;
import ru.netology.repository.UserRepository;
import ru.netology.service.StorageFileService;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@AllArgsConstructor
public class StorageFileController {

    private final StorageFileService cloudStorageService;
    private final AuthenticationRepository authenticationRepository;
    private final UserRepository userRepository;

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String filename,
            MultipartFile file) {

        User user = getUserByAuthToken(authToken);

        if (file == null || file.isEmpty()) {
            throw new InputDataException("File must not be empty");
        }

        try {
            StorageFile storageFile = new StorageFile(
                    null,
                    filename,
                    null,
                    null,
                    file.getBytes(),
                    user
            );

            cloudStorageService.uploadFile(user, storageFile);
        } catch (IOException e) {
            throw new InputDataException("Upload file: Input data exception", e);
        }

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String filename) {

        User user = getUserByAuthToken(authToken);
        cloudStorageService.deleteFile(user, filename);

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String filename) {

        User user = getUserByAuthToken(authToken);
        StorageFile file = cloudStorageService.downloadFile(user, filename);

        return ResponseEntity.ok()
                .body(new ByteArrayResource(file.getFileContent()));
    }

    @PutMapping("/file")
    public ResponseEntity<?> editFileName(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String filename,
            @Valid @RequestBody EditFileNameRQ editFileNameRQ) {

        User user = getUserByAuthToken(authToken);

        cloudStorageService.editFileName(
                user,
                filename,
                editFileNameRQ.getFilename()
        );

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/list")
    public List<FileRS> getAllFiles(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("limit") Integer limit) {

        User user = getUserByAuthToken(authToken);

        return cloudStorageService.getAllFiles(user, limit)
                .stream()
                .map(file -> new FileRS(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
    }

    private User getUserByAuthToken(String authToken) {
        if (authToken == null || !authToken.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid authentication token");
        }

        String token = authToken.substring(7);

        if (token.isBlank()) {
            throw new UnauthorizedException("Invalid authentication token");
        }

        String username = authenticationRepository.getUsernameByToken(token);

        if (username == null) {
            throw new UnauthorizedException("Invalid authentication token");
        }

        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UnauthorizedException("Invalid authentication token");
        }

        return user;
    }
}
