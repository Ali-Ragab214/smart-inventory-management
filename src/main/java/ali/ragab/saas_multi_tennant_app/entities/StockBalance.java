package ali.ragab.saas_multi_tennant_app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "stock_balances")
public class StockBalance {

    @Id
    @Column(name = "product_id", nullable = false)
    private String productId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_stock_balances_product"))
    private Product product;

    @Builder.Default
    @Column(name = "on_hand", nullable = false)
    private Integer onHand = 0;

    @Builder.Default
    @Column(name = "reserved", nullable = false)
    private Integer reserved = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void setUpdatedTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
