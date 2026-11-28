package ali.ragab.saas_multi_tennant_app.repositories;

import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMvtRepository extends JpaRepository<StockMvt, String>, JpaSpecificationExecutor<StockMvt> {

    Page<StockMvt> findAllByProductId(String productId, Pageable pageable);

    // P1-T2: Sum of all IN movements for a product (for balance recompute)
    @Query("""
            SELECT COALESCE(SUM(m.quantity), 0) FROM StockMvt m
            WHERE m.product.id = :productId
              AND m.deleted = false
              AND m.typeMvt = ali.ragab.saas_multi_tennant_app.entities.TypeMvt.IN
            """)
    int sumInQuantityByProductId(@Param("productId") String productId);

    // P1-T2: Sum of all OUT movements for a product (for balance recompute)
    @Query("""
            SELECT COALESCE(SUM(m.quantity), 0) FROM StockMvt m
            WHERE m.product.id = :productId
              AND m.deleted = false
              AND m.typeMvt = ali.ragab.saas_multi_tennant_app.entities.TypeMvt.OUT
            """)
    int sumOutQuantityByProductId(@Param("productId") String productId);

    // P1-T2: Get all movements for a product ordered by occurred_at (for recompute with balance_after)
    @Query("""
            SELECT m FROM StockMvt m
            WHERE m.product.id = :productId
              AND m.deleted = false
            ORDER BY m.occurredAt ASC
            """)
    List<StockMvt> findAllByProductIdOrderByOccurredAtAsc(@Param("productId") String productId);
}
