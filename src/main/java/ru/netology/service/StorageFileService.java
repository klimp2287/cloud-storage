package ru.netology.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.netology.exception.InputDataException;
import ru.netology.model.StorageFile;
import ru.netology.model.User;
import ru.netology.repository.StorageFileRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class StorageFileService {

    private final StorageFileRepository storageFileRepository;

    public void uploadFile(User user, StorageFile file) {
        if (user == null) {
            throw new InputDataException("User must not be null");
        }

        if (file == null || file.getFilename() == null || file.getFilename().isBlank()) {
            throw new InputDataException("Filename must not be blank");
        }

        if (file.getFileContent() == null) {
            throw new InputDataException("File content must not be null");
        }

        if (storageFileRepository.existsByUserAndFilename(user, file.getFilename())) {
            log.error("Upload file: File already exists");
            throw new InputDataException("File already exists");
        }

        file.setId(null);
        file.setDate(LocalDateTime.now());
        file.setSize((long) file.getFileContent().length);
        file.setUser(user);

        storageFileRepository.save(file);

        log.info("Success upload file. User {}", user.getUsername());
    }

    @Transactional
    public void deleteFile(User user, String filename) {
        long deletedRows = storageFileRepository.deleteByUserAndFilename(user, filename);

        if (deletedRows == 0) {
            log.error("Delete file: File not found");
            throw new InputDataException("Delete file: File not found");
        }

        log.info("Success delete file. User {}", user.getUsername());
    }

    public StorageFile downloadFile(User user, String filename) {
        StorageFile file = storageFileRepository.findByUserAndFilename(user, filename);

        if (file == null) {
            log.error("Download file: File not found");
            throw new InputDataException("Download file: File not found");
        }

        log.info("Success download file. User {}", user.getUsername());
        return file;
    }

    @Transactional
    public void editFileName(User user, String filename, String newFilename) {
        if (newFilename == null || newFilename.isBlank()) {
            throw new InputDataException("Filename must not be blank");
        }

        if (!filename.equals(newFilename)
                && storageFileRepository.existsByUserAndFilename(user, newFilename)) {
            throw new InputDataException("File with this name already exists");
        }

        int updatedRows = storageFileRepository.editFileNameByUser(
                user,
                filename,
                newFilename
        );

        if (updatedRows == 0) {
            log.error("Edit file name: File not found");
            throw new InputDataException("Edit file name: File not found");
        }

        log.info("Success edit file name. User {}", user.getUsername());
    }

    public List<StorageFile> getAllFiles(User user, Integer limit) {
        if (limit == null || limit <= 0) {
            throw new InputDataException("Limit must be greater than zero");
        }

        List<StorageFile> files = storageFileRepository.findAllByUser(
                user,
                PageRequest.of(0, limit)
        );

        log.info("Success get all files. User {}", user.getUsername());
        return files;
    }
}