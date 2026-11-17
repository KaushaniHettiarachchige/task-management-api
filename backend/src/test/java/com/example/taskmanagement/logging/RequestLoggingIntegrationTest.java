
package com.example.taskmanagement.logging;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // TEST 1: Unauthenticated requests receive
    // a request ID and retain their 401 response.
    @Test
    void unauthorizedRequest_shouldIncludeRequestId()
            throws Exception {

        mockMvc.perform(
                get("/api/tasks")
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
                header().exists("X-Request-ID")
        );
    }

    // TEST 2: Validation errors are not changed
    // by the logging filter.
    @Test
    void invalidRegistration_shouldReturn400WithRequestId()
            throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                header().exists("X-Request-ID")
        )
        .andExpect(
                jsonPath("$.status").value(400)
        );
    }

    // TEST 3: Each HTTP request receives a unique ID.
    @Test
    void separateRequests_shouldHaveDifferentRequestIds()
            throws Exception {

        MvcResult first = mockMvc.perform(
                get("/api/tasks")
        )
        .andExpect(status().isUnauthorized())
        .andReturn();

        MvcResult second = mockMvc.perform(
                get("/api/tasks")
        )
        .andExpect(status().isUnauthorized())
        .andReturn();

        String firstId = first.getResponse()
                .getHeader("X-Request-ID");

        String secondId = second.getResponse()
                .getHeader("X-Request-ID");

        assertNotNull(firstId);
        assertNotNull(secondId);

        assertNotEquals(firstId, secondId);
    }

    // TEST 4: Client-supplied request ID cannot
    // override the trusted server-generated ID.
    @Test
    void clientProvidedRequestId_shouldBeReplaced()
            throws Exception {

        MvcResult result = mockMvc.perform(
                get("/api/tasks")
                        .header(
                                "X-Request-ID",
                                "untrusted-client-value"
                        )
        )
        .andExpect(status().isUnauthorized())
        .andReturn();

        String returnedId = result.getResponse()
                .getHeader("X-Request-ID");

        assertNotNull(returnedId);
        assertNotEquals(
                "untrusted-client-value",
                returnedId
        );
    }
}
