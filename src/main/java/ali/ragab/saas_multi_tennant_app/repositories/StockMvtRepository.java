package ali.ragab.saas_multi_tennant_app.repositories;

import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMvtRepository extends JpaRepository<StockMvt, String> {

    //here i soft-delete the stock movements of a product before the product itself is soft-deleted
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockMvt s set s.deleted = true where s.product.id = :productId")
    int softDeleteByProductId(@Param("productId") String productId);
    Page<StockMvt> findAllByProductId(String productId, Pageable pageable);

}
