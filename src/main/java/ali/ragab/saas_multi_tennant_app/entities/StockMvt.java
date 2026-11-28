package ali.ragab.saas_multi_tennant_app.entities;

import ali.ragab.saas_multi_tennant_app.common.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static jakarta.persistence.EnumType.STRING;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "stock_mvts")
@SQLDelete(sql = "UPDATE stock_mvts SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class StockMvt extends AbstractEntity {

    @Column(name = "type_mvt", nullable = false)
    @Enumerated(STRING)
    private TypeMvt typeMvt;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "date_mvt", nullable = false)
    private LocalDate dateMvt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_id"))
    private Product product;

    @Builder.Default
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "reason", nullable = false, length = 40)
    @Enumerated(STRING)
    private MovementReason reason;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;
}