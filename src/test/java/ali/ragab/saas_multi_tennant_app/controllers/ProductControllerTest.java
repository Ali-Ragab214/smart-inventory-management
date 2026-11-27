package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.exceptions.GlobalExceptionHandler;
import ali.ragab.saas_multi_tennant_app.responses.ProductResponse;
import ali.ragab.saas_multi_tennant_app.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.productService = mock(ProductService.class);
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(this.productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsProductAndReturnsItsSafeResponse() throws Exception {
        when(this.productService.create(any())).thenReturn(ProductResponse.builder()
                .id("product-id")
                .reference("SKU-1")
                .name("Rice")
                .categoryId("category-id")
                .build());

        this.mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rice","reference":"SKU-1","price":10.00,
                                 "alertThreshold":0,"categoryId":"category-id","unitOfMeasure":"KG"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("product-id"))
                .andExpect(jsonPath("$.categoryId").value("category-id"));
    }

    @Test
    void rejectsInvalidProductRequest() throws Exception {
        this.mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"reference\":\"\",\"price\":0,\"alertThreshold\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("name")))
                .andExpect(jsonPath("$.message").value(containsString("categoryId")));
    }

    @Test
    void findsProductBySku() throws Exception {
        when(this.productService.findByReference(eq("SKU-1"))).thenReturn(ProductResponse.builder()
                .id("product-id")
                .reference("SKU-1")
                .build());

        this.mockMvc.perform(get("/api/v1/products/by-sku/SKU-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("SKU-1"));
    }
}
