package ali.ragab.saas_multi_tennant_app.repositories;

import ali.ragab.saas_multi_tennant_app.entities.StockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockBalanceRepository extends JpaRepository<StockBalance, String> {

    // This method is used to find a stock balance by its product ID and lock it for update.
    // This is useful when we need to ensure that the stock balance is not modified by another transaction
    // while we are updating it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sb FROM StockBalance sb WHERE sb.productId = :productId")
    Optional<StockBalance> findByProductIdForUpdate(@Param("productId") String productId);

    @Query("""
            SELECT sb FROM StockBalance sb
            JOIN Product p ON sb.productId = p.id
            WHERE p.deleted = false
              AND sb.onHand <= p.alertThreshold
            ORDER BY (p.alertThreshold - sb.onHand) DESC
            """)
    List<StockBalance> findLowStockBalances();
}
