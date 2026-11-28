package ali.ragab.saas_multi_tennant_app.entities;

import ali.ragab.saas_multi_tennant_app.common.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "stock_counts")
public class StockCount extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_counts_product"))
    private Product product;

    @Column(name = "counted_qty", nullable = false)
    private Integer countedQty;

    @Column(name = "system_qty", nullable = false)
    private Integer systemQty;

    @Column(name = "variance", nullable = false)
    private Integer variance;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "counted_by", nullable = false)
    private String countedBy;

    @Builder.Default
    @Column(name = "counted_at", nullable = false)
    private Instant countedAt = Instant.now();


    // This field is used to link the stock count to the adjustment movement that mean we create
    // a stock movement to adjust the stock balance based on the variance found during the stock count.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_movement_id",
            foreignKey = @ForeignKey(name = "fk_stock_counts_adjustment"))
    private StockMvt adjustmentMovement;
}
