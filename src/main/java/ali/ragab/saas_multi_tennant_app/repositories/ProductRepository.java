package ali.ragab.saas_multi_tennant_app.repositories;

import ali.ragab.saas_multi_tennant_app.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findByReferenceIgnoreCaseAndDeletedFalse(String reference);
    Optional<Product> findByIdAndDeletedFalse(String id);
    Page<Product> findAllByDeletedFalse(Pageable pageable);
    @Query("""
            select p from Product p
            where p.deleted = false
              and (lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.reference) like lower(concat('%', :search, '%')))
            """)
    Page<Product> searchActiveProducts(@Param("search") String search, Pageable pageable);
    // Detach the products before soft-deleting the category so the FK is not violated.
    // Native SQL is required: the HQL association assignment renders as "set id = null".
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE products SET category_id = NULL WHERE category_id = :categoryId", nativeQuery = true)
    int detachFromCategory(@Param("categoryId") String categoryId);
}
