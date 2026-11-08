package ru.netology;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudStorageIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:13-alpine")
                    .withDatabaseName("cloud_storage_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("jwt.token.secret", () -> "integration-test-secret-key-which-is-long-enough");
        registry.add("jwt.token.expiration", () -> "18000");
        registry.add("cors.origins", () -> "http://localhost:8082");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String body = "{\"login\":\"user@one.user\",\"password\":\"user_one\"}";
        String response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        token = json.get("auth-token").asText();
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @Test
    void completeFileLifecycle() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "integration.txt", MediaType.TEXT_PLAIN_VALUE, "integration-data".getBytes());

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "integration.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/list")
                        .param("limit", "20")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.filename == 'integration.txt')]", hasSize(1)));

        mockMvc.perform(get("/file")
                        .param("filename", "integration.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("integration-data".getBytes()));

        mockMvc.perform(put("/file")
                        .param("filename", "integration.txt")
                        .header("auth-token", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"renamed-integration.txt\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/list")
                        .param("limit", "20")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.filename == 'renamed-integration.txt')]", hasSize(1)));

        mockMvc.perform(delete("/file")
                        .param("filename", "renamed-integration.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/list")
                        .param("limit", "20")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.filename == 'renamed-integration.txt')]", hasSize(0)));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user@one.user\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/list").param("limit", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesToken() throws Exception {
        mockMvc.perform(post("/logout").header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/list")
                        .param("limit", "10")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersCannotAccessEachOthersFiles() throws Exception {
        String secondUserResponse = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user@two.user\",\"password\":\"user_two\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondUserToken = objectMapper
                .readTree(secondUserResponse)
                .get("auth-token")
                .asText();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "private-to-one.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "private-data".getBytes()
        );

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "private-to-one.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/file")
                        .param("filename", "private-to-one.txt")
                        .header("auth-token", "Bearer " + secondUserToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/file")
                        .param("filename", "private-to-one.txt")
                        .header("auth-token", "Bearer " + secondUserToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/file")
                        .param("filename", "private-to-one.txt")
                        .header("auth-token", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"stolen.txt\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/file")
                        .param("filename", "private-to-one.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("private-data".getBytes()));
    }

    @Test
    void differentUsersCanUploadFilesWithSameName() throws Exception {
        String secondUserResponse = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user@two.user\",\"password\":\"user_two\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondUserToken = objectMapper
                .readTree(secondUserResponse)
                .get("auth-token")
                .asText();

        MockMultipartFile firstFile = new MockMultipartFile(
                "file", "document.txt", MediaType.TEXT_PLAIN_VALUE, "user-one".getBytes());

        MockMultipartFile secondFile = new MockMultipartFile(
                "file", "document.txt", MediaType.TEXT_PLAIN_VALUE, "user-two".getBytes());

        mockMvc.perform(multipart("/file")
                        .file(firstFile)
                        .param("filename", "document.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/file")
                        .file(secondFile)
                        .param("filename", "document.txt")
                        .header("auth-token", "Bearer " + secondUserToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/file")
                        .param("filename", "document.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("user-one".getBytes()));

        mockMvc.perform(get("/file")
                        .param("filename", "document.txt")
                        .header("auth-token", "Bearer " + secondUserToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("user-two".getBytes()));
    }

    @Test
    void userCannotUploadFileWithDuplicateName() throws Exception {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file", "duplicate.txt", MediaType.TEXT_PLAIN_VALUE, "original-data".getBytes());

        MockMultipartFile secondFile = new MockMultipartFile(
                "file", "duplicate.txt", MediaType.TEXT_PLAIN_VALUE, "new-data".getBytes());

        mockMvc.perform(multipart("/file")
                        .file(firstFile)
                        .param("filename", "duplicate.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/file")
                        .file(secondFile)
                        .param("filename", "duplicate.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/file")
                        .param("filename", "duplicate.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("original-data".getBytes()));
    }

    @Test
    void listRejectsZeroLimit() throws Exception {
        mockMvc.perform(get("/list")
                        .param("limit", "0")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listRejectsNegativeLimit() throws Exception {
        mockMvc.perform(get("/list")
                        .param("limit", "-1")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/list")
                        .param("limit", "10")
                        .header("auth-token", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotRenameFileToExistingFilename() throws Exception {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file", "first.txt", MediaType.TEXT_PLAIN_VALUE, "first-data".getBytes());

        MockMultipartFile secondFile = new MockMultipartFile(
                "file", "second.txt", MediaType.TEXT_PLAIN_VALUE, "second-data".getBytes());

        mockMvc.perform(multipart("/file")
                        .file(firstFile)
                        .param("filename", "first.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/file")
                        .file(secondFile)
                        .param("filename", "second.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/file")
                        .param("filename", "first.txt")
                        .header("auth-token", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"second.txt\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/file")
                        .param("filename", "first.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("first-data".getBytes()));

        mockMvc.perform(get("/file")
                        .param("filename", "second.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("second-data".getBytes()));
    }

    @Test
    void userCanRenameFileToSameFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "same.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "same-data".getBytes()
        );

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "same.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/file")
                        .param("filename", "same.txt")
                        .header("auth-token", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"same.txt\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/file")
                        .param("filename", "same.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("same-data".getBytes()));
    }

    @Test
    void loginRejectsBlankUsername() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"\",\"password\":\"user_one\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsBlankPassword() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user@one.user\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadRejectsBlankFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "test content".getBytes()
        );

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadRejectsMissingFile() throws Exception {
        mockMvc.perform(multipart("/file")
                        .param("filename", "document.txt")
                        .header("auth-token", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
