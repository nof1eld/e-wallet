package com.example.e_wallet.controller;

import com.example.e_wallet.entity.User;
import com.example.e_wallet.repository.UserRepository;
import com.example.e_wallet.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// test the full security chain: JwtAuthFilter -> @PreAuthorize -> AccountSecurity
@SpringBootTest
@ActiveProfiles("test")
class AccountAuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    // the full app context (needed for auth)

    private MockMvc mockMvc;

    @BeforeEach
    // we'll run this before each test in this class
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ownershipAndWriteAccessAreEnforcedAcrossAccounts() throws Exception {
        String userA = "userA-auth" + System.nanoTime();
        String userB = "userB-auth" + System.nanoTime();
        // nanoTime() is used to get a unique username each run to avoid collisions (because old usernames could be stored in-memory)

        String tokenA = registerAndGetToken(userA, "password123");
        String tokenB = registerAndGetToken(userB, "password123");

        Long accountAId = createAccount(tokenA);
        Long accountBId = createAccount(tokenB);


        mockMvc.perform(get("/accounts/{id}", accountBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isForbidden());
        // user A trying to READ user B's account: must be 403

        mockMvc.perform(post("/accounts/{id}/deposit", accountBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.00}"))
                .andExpect(status().isForbidden());
        // user A trying to deposit into user B's account: must be 403


        mockMvc.perform(get("/accounts/{id}", accountAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                // must have same username with that account
                .andExpect(jsonPath("$.ownerUsername").value(userA));
        // user A reading their own account -> 200 (success)

        mockMvc.perform(post("/accounts/{id}/deposit", accountAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":15.00}"))
                .andExpect(status().isOk());
        // user A depositing into their own account -> success


        String adminToken = jwtService.generateToken("admin-user", "ADMIN");

        User adminUser = new User();
        adminUser.setUsername("admin-user");
        adminUser.setPasswordHash("admin-password");
        adminUser.setRole(User.Role.ADMIN);
        userRepository.save(adminUser);

        mockMvc.perform(get("/accounts/{id}", accountBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
        // admin reading any account (even not their own) is allowed

        mockMvc.perform(post("/accounts/{id}/deposit", accountBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isForbidden());
        // Admin trying to deposit into someone else's account: 403

        assertThat(tokenB).isNotBlank();
    }

    private String registerAndGetToken(String username, String password) throws Exception {
        String request = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private Long createAccount(String token) throws Exception {
        String response = mockMvc.perform(post("/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}