package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.exceptions.GlobalExceptionHandler;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;
import ali.ragab.saas_multi_tennant_app.security.JwtAuthenticationFilter;
import ali.ragab.saas_multi_tennant_app.services.StockMvtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockMvtController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@ContextConfiguration(classes = {StockMvtController.class, GlobalExceptionHandler.class,
        StockMvtControllerTest.MethodSecurityConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class StockMvtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockMvtService stockMvtService;

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void postStockMvtIsRejectedWith405MethodNotAllowed() throws Exception {
        this.mockMvc.perform(post("/api/v1/stock-mvts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"typeMvt":"IN","quantity":5,"productId":"prod-1"}
                                """))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(this.stockMvtService);
    }

    @Test
    @WithMockUser(roles = "SALES_OPERATOR")
    void getAllStockMvtsReturns200() throws Exception {
        when(this.stockMvtService.findAll(0, 10)).thenReturn(
                PageResponse.<StockMvtResponse>builder()
                        .content(List.of(sampleResponse()))
                        .page(0).size(10).totalElements(1).totalPages(1)
                        .build());

        this.mockMvc.perform(get("/api/v1/stock-mvts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("mvt-1"));
    }

    @Test
    @WithMockUser(roles = "SALES_OPERATOR")
    void getStockMvtByIdReturns200() throws Exception {
        when(this.stockMvtService.findById("mvt-1")).thenReturn(sampleResponse());

        this.mockMvc.perform(get("/api/v1/stock-mvts/mvt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mvt-1"));
    }

    @Test
    @WithMockUser(roles = "SALES_OPERATOR")
    void getStockMvtsByProductIdReturns200() throws Exception {
        when(this.stockMvtService.findAllByProductId("prod-1", 0, 10)).thenReturn(
                PageResponse.<StockMvtResponse>builder()
                        .content(List.of(sampleResponse()))
                        .page(0).size(10).totalElements(1).totalPages(1)
                        .build());

        this.mockMvc.perform(get("/api/v1/stock-mvts/product/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("mvt-1"));
    }

    private static StockMvtResponse sampleResponse() {
        return StockMvtResponse.builder()
                .id("mvt-1")
                .typeMvt(TypeMvt.IN)
                .quantity(5)
                .dateMvt(LocalDate.of(2026, 11, 28))
                .build();
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}
