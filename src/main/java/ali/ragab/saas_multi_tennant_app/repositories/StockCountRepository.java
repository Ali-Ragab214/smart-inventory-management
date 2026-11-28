package ali.ragab.saas_multi_tennant_app.repositories;

import ali.ragab.saas_multi_tennant_app.entities.StockCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCountRepository extends JpaRepository<StockCount, String> {

    Page<StockCount> findByProductId(String productId, Pageable pageable);
}
