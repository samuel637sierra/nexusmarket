package application.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceApiTests {
    @Autowired
    private MockMvc mvc;

    @Test
    void exposesCatalogAndProtectsWriteOperations() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mvc.perform(post("/api/warehouses")
                        .contentType("application/json")
                        .content("{\"warehouseCode\":\"WH-1\",\"name\":\"Central\","
                                + "\"address\":\"Calle 1\",\"capacity\":100}"))
                .andExpect(status().isUnauthorized());
    }

            @Test
            void registersCustomerAndEnforcesRoleAuthorization() throws Exception {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            String username = "api-" + suffix;
            String password = "secure-password-123";
            String firstRegistration = mvc.perform(post("/api/auth/register/customer")
                    .contentType("application/json")
                    .content("{\"fullName\":\"API Customer\",\"email\":\"api-" + suffix
                        + "@example.com\",\"phoneNumber\":\"3001234567\","
                        + "\"address\":\"Calle 1\",\"username\":\"" + username
                        + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            String personId = firstRegistration.replaceAll("(?s).*\\\"personId\\\":\\\"([^\\\"]+)\\\".*", "$1");
            String authorization = "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));

            String otherUsername = "other-" + suffix;
            mvc.perform(post("/api/auth/register/customer")
                    .contentType("application/json")
                    .content("{\"fullName\":\"Other Customer\",\"email\":\"other-" + suffix
                        + "@example.com\",\"phoneNumber\":\"3007654321\","
                        + "\"address\":\"Carrera 2\",\"username\":\"" + otherUsername
                        + "\",\"password\":\"other-secure-password-123\"}"))
                .andExpect(status().isOk());
            String otherAuthorization = "Basic " + Base64.getEncoder().encodeToString(
                (otherUsername + ":other-secure-password-123").getBytes(StandardCharsets.UTF_8));

            mvc.perform(get("/api/customers/" + personId + "/cart").header("Authorization", authorization))
                .andExpect(status().isOk());

            mvc.perform(post("/api/warehouses")
                    .header("Authorization", authorization)
                    .contentType("application/json")
                    .content("{\"warehouseCode\":\"WH-" + suffix + "\",\"name\":\"Central\","
                        + "\"address\":\"Calle 1\",\"capacity\":100}"))
                .andExpect(status().isForbidden());

            mvc.perform(get("/api/customers/" + personId + "/cart").header("Authorization", otherAuthorization))
                .andExpect(status().isForbidden());
            }
}