package cl.duoc.andesstay.report.voucher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:reportapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class ReportApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminVeElResumen() throws Exception {
        mockMvc.perform(get("/api/reports/summary").with(role("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVouchers").isNumber())
                .andExpect(jsonPath("$.lastSevenDays.length()").value(7));
    }

    @Test
    void operadorNoTieneAcceso() throws Exception {
        mockMvc.perform(get("/api/reports/vouchers").with(role("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void voucherInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/reports/vouchers/VCH-NO-EXISTE").with(role("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtroConValorInvalidoResponde400() throws Exception {
        mockMvc.perform(get("/api/reports/vouchers").param("reservationId", "abc").with(role("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    private JwtRequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
