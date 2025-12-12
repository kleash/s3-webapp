package com.example.s3webapp.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.s3webapp.model.CopyMoveRequest;
import com.example.s3webapp.model.ObjectItem;
import com.example.s3webapp.model.ObjectListResponse;
import com.example.s3webapp.s3.StorageService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
        "security.ldap.embedded.enabled=false",
        "security.ldap.url=ldap://localhost:0",
        "security.ldap.bindDn=cn=placeholder",
        "security.ldap.bindPassword=placeholder",
        "security.ldap.userSearchBase=dc=example,dc=com"
})
class SecurityAuthorizationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StorageService storageService;

    @BeforeEach
    void setupMocks() {
        when(storageService.listBuckets()).thenReturn(List.of());
        when(storageService.listObjects(anyString(), anyString(), any())).thenReturn(
                new ObjectListResponse("", List.of(), List.of(), null));
        when(storageService.copy(anyString(), any(CopyMoveRequest.class))).thenReturn(
                new ObjectItem("a.txt", "a.txt", 10, Instant.now(), "text/plain"));
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/buckets")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/buckets/demo/objects/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceKey\":\"a\",\"targetKey\":\"b\",\"overwrite\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    void readOnlyCanReadButNotWrite() throws Exception {
        mockMvc.perform(get("/api/buckets")).andExpect(status().isOk());
        mockMvc.perform(get("/api/buckets/demo/objects")).andExpect(status().isOk());
        mockMvc.perform(post("/api/buckets/demo/objects/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceKey\":\"a\",\"targetKey\":\"b\",\"overwrite\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "READ_WRITE")
    void readWriteCanPerformWrites() throws Exception {
        mockMvc.perform(post("/api/buckets/demo/objects/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceKey\":\"a\",\"targetKey\":\"b\",\"overwrite\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/buckets/demo/objects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keys\":[\"a\"],\"prefixes\":[]}"))
                .andExpect(status().isOk());
    }
}
