package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.exceptions.GlobalExceptionHandler;
import ali.ragab.saas_multi_tennant_app.responses.StockBalanceResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockCountResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMovementResponse;
import ali.ragab.saas_multi_tennant_app.services.StockCountService;
import ali.ragab.saas_multi_tennant_app.services.StockLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockControllerTest {

    private StockLedgerService stockLedgerService;
    private StockCountService stockCountService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        stockLedgerService = mock(StockLedgerService.class);
        stockCountService = mock(StockCountService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StockController(stockLedgerService, stockCountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /stock/movements returns 201 with movement response")
    void postMovementReturns201() throws Exception {
        when(stockLedgerService.postMovement(any())).thenReturn(
                StockMovementResponse.builder()
                        .id("mvt-1")
                        .type(TypeMvt.IN)
                        .quantity(10)
                        .reason(MovementReason.PURCHASE_RECEIPT)
                        .balanceAfter(60)
                        .build());

        mockMvc.perform(post("/api/v1/stock/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": "prod-1",
                                    "type": "IN",
                                    "quantity": 10,
                                    "reason": "PURCHASE_RECEIPT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("mvt-1"))
                .andExpect(jsonPath("$.balanceAfter").value(60));
    }

    @Test
    @DisplayName("POST /stock/movements with missing productId returns 400")
    void postMovementValidation() throws Exception {
        mockMvc.perform(post("/api/v1/stock/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "type": "IN",
                                    "quantity": 10,
                                    "reason": "PURCHASE_RECEIPT"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /stock/receipts returns 201")
    void receiveStockReturns201() throws Exception {
        when(stockLedgerService.receiveStock(any())).thenReturn(
                StockMovementResponse.builder()
                        .id("mvt-r1")
                        .type(TypeMvt.IN)
                        .quantity(20)
                        .reason(MovementReason.PURCHASE_RECEIPT)
                        .build());

        mockMvc.perform(post("/api/v1/stock/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": "prod-1",
                                    "quantity": 20
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("mvt-r1"));
    }

    @Test
    @DisplayName("POST /stock/counts returns 201 with count response")
    void recordCountReturns201() throws Exception {
        when(stockCountService.recordCount(any())).thenReturn(
                StockCountResponse.builder()
                        .id("count-1")
                        .systemQty(50)
                        .countedQty(55)
                        .variance(5)
                        .build());

        mockMvc.perform(post("/api/v1/stock/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": "prod-1",
                                    "countedQty": 55
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variance").value(5));
    }

    @Test
    @DisplayName("GET /stock/products/{id}/balance returns balance")
    void getBalanceReturnsOk() throws Exception {
        when(stockLedgerService.getBalance("prod-1")).thenReturn(
                StockBalanceResponse.builder()
                        .productId("prod-1")
                        .onHand(50)
                        .reserved(0)
                        .available(50)
                        .build());

        mockMvc.perform(get("/api/v1/stock/products/prod-1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onHand").value(50))
                .andExpect(jsonPath("$.available").value(50));
    }

    @Test
    @DisplayName("GET /stock/products/{id}/movements returns paginated history")
    void getMovementHistoryReturns200() throws Exception {
        when(stockLedgerService.getMovementHistory(anyString(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(ali.ragab.saas_multi_tennant_app.common.PageResponse.<StockMovementResponse>builder()
                        .content(java.util.List.of())
                        .page(0)
                        .size(20)
                        .totalElements(0)
                        .totalPages(0)
                        .build());

        mockMvc.perform(get("/api/v1/stock/products/prod-1/movements")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("POST /stock/movements with zero quantity returns 400")
    void postMovementZeroQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/stock/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": "prod-1",
                                    "type": "IN",
                                    "quantity": 0,
                                    "reason": "OTHER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
