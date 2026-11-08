package ru.netology.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import ru.netology.dto.request.EditFileNameRQ;
import ru.netology.dto.response.FileRS;
import ru.netology.exception.InputDataException;
import ru.netology.exception.UnauthorizedException;
import ru.netology.model.StorageFile;
import ru.netology.model.User;
import ru.netology.repository.AuthenticationRepository;
import ru.netology.repository.StorageFileRepository;
import ru.netology.repository.UserRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageFileServiceTest {
    private static final String TOKEN = "Bearer test-token";
    private static final String USERNAME = "user@test.ru";

    @Mock private StorageFileRepository storageFileRepository;
    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private StorageFileService storageFileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().username(USERNAME).password("encoded").build();
    }

    private void authenticateUser() {
        when(authenticationRepository.getUsernameByToken("test-token")).thenReturn(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(user);
    }

    @Test
    void uploadSavesFileForAuthenticatedUser() throws Exception {
        authenticateUser();
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());

        assertTrue(storageFileService.uploadFile(TOKEN, "hello.txt", multipartFile));

        ArgumentCaptor<StorageFile> captor = ArgumentCaptor.forClass(StorageFile.class);
        verify(storageFileRepository).save(captor.capture());
        assertEquals("hello.txt", captor.getValue().getFilename());
        assertEquals(5L, captor.getValue().getSize());
        assertArrayEquals("hello".getBytes(), captor.getValue().getFileContent());
        assertSame(user, captor.getValue().getUser());
    }

    @Test
    void uploadRejectsUnknownToken() {
        when(authenticationRepository.getUsernameByToken("test-token")).thenReturn(null);
        when(userRepository.findByUsername(null)).thenReturn(null);
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});

        assertThrows(UnauthorizedException.class,
                () -> storageFileService.uploadFile(TOKEN, "a.txt", file));
        verify(storageFileRepository, never()).save(any());
    }

    @Test
    void deleteRemovesFileOwnedByCurrentUser() {
        authenticateUser();
        when(storageFileRepository.deleteByUserAndFilename(user, "a.txt")).thenReturn(1L);

        assertDoesNotThrow(() -> storageFileService.deleteFile(TOKEN, "a.txt"));

        verify(storageFileRepository).deleteByUserAndFilename(user, "a.txt");
    }

    @Test
    void deleteThrowsWhenFileDoesNotExist() {
        authenticateUser();
        when(storageFileRepository.deleteByUserAndFilename(user, "missing.txt")).thenReturn(0L);

        assertThrows(InputDataException.class,
                () -> storageFileService.deleteFile(TOKEN, "missing.txt"));
    }

    @Test
    void downloadReturnsFileContent() {
        authenticateUser();
        byte[] content = "payload".getBytes();
        StorageFile file = new StorageFile(1L, "a.txt", null, 7L, content, user);
        when(storageFileRepository.findByUserAndFilename(user, "a.txt")).thenReturn(file);

        assertArrayEquals(content, storageFileService.downloadFile(TOKEN, "a.txt"));
    }

    @Test
    void downloadThrowsWhenFileDoesNotExist() {
        authenticateUser();
        when(storageFileRepository.findByUserAndFilename(user, "missing.txt")).thenReturn(null);

        assertThrows(InputDataException.class,
                () -> storageFileService.downloadFile(TOKEN, "missing.txt"));
    }

    @Test
    void renameUpdatesFileName() {
        authenticateUser();
        EditFileNameRQ request = new EditFileNameRQ();
        request.setFilename("new.txt");
        when(storageFileRepository.editFileNameByUser(user, "old.txt", "new.txt")).thenReturn(1);

        assertDoesNotThrow(() -> storageFileService.editFileName(TOKEN, "old.txt", request));

        verify(storageFileRepository).editFileNameByUser(user, "old.txt", "new.txt");
    }

    @Test
    void renameThrowsWhenFileDoesNotExist() {
        authenticateUser();
        EditFileNameRQ request = new EditFileNameRQ();
        request.setFilename("new.txt");
        when(storageFileRepository.editFileNameByUser(user, "missing.txt", "new.txt")).thenReturn(0);

        assertThrows(InputDataException.class,
                () -> storageFileService.editFileName(TOKEN, "missing.txt", request));
    }

    @Test
    void listReturnsFilesForUserAndUsesRequestedLimit() {
        authenticateUser();
        StorageFile file = new StorageFile(1L, "a.txt", null, 10L, new byte[10], user);
        when(storageFileRepository.findAllByUser(eq(user), any(PageRequest.class)))
                .thenReturn(Collections.singletonList(file));

        List<FileRS> result = storageFileService.getAllFiles(TOKEN, 10);

        assertEquals(1, result.size());
        assertEquals("a.txt", result.get(0).getFilename());
        assertEquals(10L, result.get(0).getSize());
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(storageFileRepository).findAllByUser(eq(user), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void listRejectsZeroLimit() {
        authenticateUser();
        assertThrows(InputDataException.class, () -> storageFileService.getAllFiles(TOKEN, 0));
        verify(storageFileRepository, never()).findAllByUser(any(), any());
    }

    @Test
    void listRejectsNullLimit() {
        authenticateUser();
        assertThrows(InputDataException.class, () -> storageFileService.getAllFiles(TOKEN, null));
        verify(storageFileRepository, never()).findAllByUser(any(), any());
    }

    @Test
    void operationsRejectMalformedAuthorizationHeader() {
        assertThrows(UnauthorizedException.class,
                () -> storageFileService.getAllFiles("invalid-token", 10));
    }
}
