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
import ru.netology.service.StorageFileService;

import java.util.List;

@RestController
@RequestMapping("/")
@AllArgsConstructor
public class StorageFileController {

    private StorageFileService cloudStorageService;

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(@RequestHeader("auth-token") String authToken, @RequestParam("filename") String filename, MultipartFile file) {
        cloudStorageService.uploadFile(authToken, filename, file);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(@RequestHeader("auth-token") String authToken, @RequestParam("filename") String filename) {
        cloudStorageService.deleteFile(authToken, filename);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(@RequestHeader("auth-token") String authToken, @RequestParam("filename") String filename) {
        byte[] file = cloudStorageService.downloadFile(authToken, filename);
        return ResponseEntity.ok().body(new ByteArrayResource(file));
    }

    @PutMapping(value = "/file")
    public ResponseEntity<?> editFileName(@RequestHeader("auth-token") String authToken, @RequestParam("filename") String filename, @RequestBody EditFileNameRQ editFileNameRQ) {
        cloudStorageService.editFileName(authToken, filename, editFileNameRQ);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/list")
    public List<FileRS> getAllFiles(@RequestHeader("auth-token") String authToken, @RequestParam("limit") Integer limit) {
        return cloudStorageService.getAllFiles(authToken, limit);
    }
}