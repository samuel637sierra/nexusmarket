package application.api;

import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueObjects.SystemRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "nexusmarket.bootstrap.admin.username=bootstrap-test-admin",
        "nexusmarket.bootstrap.admin.password=bootstrap-test-password-123"
})
@AutoConfigureMockMvc
class AdminBootstrapTests {
    @Autowired
    private UserRepositoryPort users;
    @Autowired
    private PasswordServicePort passwords;
    @Autowired
    private MockMvc mvc;

    @Test
    void bootstrapsAdminOnceWithHashedPasswordAndGrantsAdminAccess() throws Exception {
        var administrator = users.findByUsername("bootstrap-test-admin").orElseThrow();
        assertEquals(SystemRole.ADMINISTRATOR, administrator.getRole());
        assertTrue(passwords.matches("bootstrap-test-password-123", administrator.getPassword()));

        mvc.perform(post("/api/warehouses")
                        .with(httpBasic("bootstrap-test-admin", "bootstrap-test-password-123"))
                        .contentType("application/json")
                        .content("{\"warehouseCode\":\"WH-BOOTSTRAP\",\"name\":\"Central\","
                                + "\"address\":\"Calle 1\",\"capacity\":100}"))
                .andExpect(status().isOk());
    }
}
