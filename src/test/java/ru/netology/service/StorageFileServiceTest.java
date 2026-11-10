package ru.netology.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import ru.netology.exception.InputDataException;
import ru.netology.model.StorageFile;
import ru.netology.model.User;
import ru.netology.repository.StorageFileRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageFileServiceTest {

    @Mock
    private StorageFileRepository storageFileRepository;

    @InjectMocks
    private StorageFileService storageFileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("user@test.ru")
                .password("encoded")
                .build();
    }

    @Test
    void uploadSavesFileForUser() {
        StorageFile file = new StorageFile(
                null, "hello.txt", null, null, "hello".getBytes(), null
        );

        storageFileService.uploadFile(user, file);

        ArgumentCaptor<StorageFile> captor =
                ArgumentCaptor.forClass(StorageFile.class);

        verify(storageFileRepository).save(captor.capture());

        StorageFile savedFile = captor.getValue();

        assertEquals("hello.txt", savedFile.getFilename());
        assertEquals(5L, savedFile.getSize());
        assertArrayEquals("hello".getBytes(), savedFile.getFileContent());
        assertSame(user, savedFile.getUser());
        assertNotNull(savedFile.getDate());
    }

    @Test
    void uploadRejectsDuplicateFilename() {
        StorageFile file = new StorageFile(
                null, "hello.txt", null, null, "hello".getBytes(), null
        );

        when(storageFileRepository.existsByUserAndFilename(user, "hello.txt"))
                .thenReturn(true);

        assertThrows(
                InputDataException.class,
                () -> storageFileService.uploadFile(user, file)
        );

        verify(storageFileRepository, never()).save(any());
    }

    @Test
    void uploadRejectsBlankFilename() {
        StorageFile file = new StorageFile(
                null, " ", null, null, new byte[]{1}, null
        );

        assertThrows(
                InputDataException.class,
                () -> storageFileService.uploadFile(user, file)
        );

        verify(storageFileRepository, never()).save(any());
    }

    @Test
    void uploadRejectsNullFileContent() {
        StorageFile file = new StorageFile(
                null, "a.txt", null, null, null, null
        );

        assertThrows(
                InputDataException.class,
                () -> storageFileService.uploadFile(user, file)
        );

        verify(storageFileRepository, never()).save(any());
    }

    @Test
    void deleteRemovesFileOwnedByUser() {
        when(storageFileRepository.deleteByUserAndFilename(user, "a.txt"))
                .thenReturn(1L);

        assertDoesNotThrow(
                () -> storageFileService.deleteFile(user, "a.txt")
        );

        verify(storageFileRepository).deleteByUserAndFilename(user, "a.txt");
    }

    @Test
    void deleteThrowsWhenFileDoesNotExist() {
        when(storageFileRepository.deleteByUserAndFilename(user, "missing.txt"))
                .thenReturn(0L);

        assertThrows(
                InputDataException.class,
                () -> storageFileService.deleteFile(user, "missing.txt")
        );
    }

    @Test
    void downloadReturnsStorageFile() {
        StorageFile file = new StorageFile(
                1L, "a.txt", null, 7L, "payload".getBytes(), user
        );

        when(storageFileRepository.findByUserAndFilename(user, "a.txt"))
                .thenReturn(file);

        StorageFile result = storageFileService.downloadFile(user, "a.txt");

        assertSame(file, result);
        assertArrayEquals("payload".getBytes(), result.getFileContent());
    }

    @Test
    void downloadThrowsWhenFileDoesNotExist() {
        when(storageFileRepository.findByUserAndFilename(user, "missing.txt"))
                .thenReturn(null);

        assertThrows(
                InputDataException.class,
                () -> storageFileService.downloadFile(user, "missing.txt")
        );
    }

    @Test
    void renameUpdatesFilename() {
        when(storageFileRepository.editFileNameByUser(
                user, "old.txt", "new.txt"
        )).thenReturn(1);

        assertDoesNotThrow(
                () -> storageFileService.editFileName(user, "old.txt", "new.txt")
        );

        verify(storageFileRepository)
                .editFileNameByUser(user, "old.txt", "new.txt");
    }

    @Test
    void renameThrowsWhenFileDoesNotExist() {
        when(storageFileRepository.editFileNameByUser(
                user, "missing.txt", "new.txt"
        )).thenReturn(0);

        assertThrows(
                InputDataException.class,
                () -> storageFileService.editFileName(
                        user, "missing.txt", "new.txt"
                )
        );
    }

    @Test
    void renameRejectsDuplicateFilename() {
        when(storageFileRepository.existsByUserAndFilename(user, "existing.txt"))
                .thenReturn(true);

        assertThrows(
                InputDataException.class,
                () -> storageFileService.editFileName(
                        user, "old.txt", "existing.txt"
                )
        );

        verify(storageFileRepository, never())
                .editFileNameByUser(any(), anyString(), anyString());
    }

    @Test
    void renameRejectsBlankFilename() {
        assertThrows(
                InputDataException.class,
                () -> storageFileService.editFileName(user, "old.txt", " ")
        );

        verify(storageFileRepository, never())
                .editFileNameByUser(any(), anyString(), anyString());
    }

    @Test
    void listReturnsFilesForUserAndUsesRequestedLimit() {
        StorageFile file = new StorageFile(
                1L, "a.txt", null, 10L, new byte[10], user
        );

        when(storageFileRepository.findAllByUser(eq(user), any(PageRequest.class)))
                .thenReturn(Collections.singletonList(file));

        List<StorageFile> result = storageFileService.getAllFiles(user, 10);

        assertEquals(1, result.size());
        assertSame(file, result.get(0));

        ArgumentCaptor<PageRequest> captor =
                ArgumentCaptor.forClass(PageRequest.class);

        verify(storageFileRepository)
                .findAllByUser(eq(user), captor.capture());

        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void listRejectsZeroLimit() {
        assertThrows(
                InputDataException.class,
                () -> storageFileService.getAllFiles(user, 0)
        );

        verify(storageFileRepository, never()).findAllByUser(any(), any());
    }

    @Test
    void listRejectsNullLimit() {
        assertThrows(
                InputDataException.class,
                () -> storageFileService.getAllFiles(user, null)
        );

        verify(storageFileRepository, never()).findAllByUser(any(), any());
    }
}
