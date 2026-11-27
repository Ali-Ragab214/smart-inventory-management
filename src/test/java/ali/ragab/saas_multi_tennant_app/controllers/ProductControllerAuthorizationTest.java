package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.exceptions.GlobalExceptionHandler;
import ali.ragab.saas_multi_tennant_app.responses.ProductResponse;
import ali.ragab.saas_multi_tennant_app.security.JwtAuthenticationFilter;
import ali.ragab.saas_multi_tennant_app.services.ProductService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@ContextConfiguration(classes = {ProductController.class, GlobalExceptionHandler.class,
        ProductControllerAuthorizationTest.MethodSecurityConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerAuthorizationTest {

    private static final String PRODUCT_JSON = """
            {"name":"Rice","reference":"SKU-1","price":10.00,
             "alertThreshold":0,"categoryId":"category-id","unitOfMeasure":"KG"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void managerCanCreateProduct() throws Exception {
        when(this.productService.create(any())).thenReturn(ProductResponse.builder().id("product-id").build());

        this.mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SALES_OPERATOR")
    void staffCannotCreateProduct() throws Exception {
        this.mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(this.productService);
    }

    @Test
    @WithMockUser(roles = "SALES_OPERATOR")
    void staffCannotUpdateOrDeleteProduct() throws Exception {
        this.mockMvc.perform(put("/api/v1/products/product-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(delete("/api/v1/products/product-id"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(this.productService);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void managerCanReadProducts() throws Exception {
        when(this.productService.findAll(0, 10, null)).thenReturn(PageResponse.<ProductResponse>builder()
                .content(List.of())
                .build());

        this.mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void staffCanReadProductsAndLookupBySku() throws Exception {
        when(this.productService.findAll(0, 10, null)).thenReturn(PageResponse.<ProductResponse>builder()
                .content(List.of())
                .build());
        when(this.productService.findByReference("SKU-1"))
                .thenReturn(ProductResponse.builder().id("product-id").reference("SKU-1").build());

        this.mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
        this.mockMvc.perform(get("/api/v1/products/by-sku/SKU-1"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}
