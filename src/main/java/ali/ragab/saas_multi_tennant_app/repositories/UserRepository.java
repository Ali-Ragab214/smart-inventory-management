package ali.ragab.saas_multi_tennant_app.repositories;

import ali.ragab.saas_multi_tennant_app.entities.User;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {


    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deleted = false")
    Optional<User> findByIdAndNotDeleted(@Param("id") String id);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.tenant.id = :tenantId AND u.deleted = false")
    Optional<User> findByIdAndTenantIdAndDeletedFalse(@Param("id") String id, @Param("tenantId") String tenantId);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.deleted = false")
    Page<User> findAllByTenantId(String tenantId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId AND u.role = :role AND u.enabled = true AND u.deleted = false")
    long countActiveByTenantIdAndRole(@Param("tenantId") String tenantId, @Param("role") UserRole role);

}