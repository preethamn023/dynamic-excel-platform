package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "formula_dependencies", uniqueConstraints = {
    @UniqueConstraint(name = "uk_source_target", columnNames = {"source_cell_id", "dependent_cell_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormulaDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_cell_id", nullable = false)
    private Cell sourceCell;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_cell_id", nullable = false)
    private Cell dependentCell;
}
