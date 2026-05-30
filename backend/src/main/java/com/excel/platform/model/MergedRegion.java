package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "merged_regions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergedRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private Sheet sheet;

    @Column(nullable = false)
    private Integer firstRow;

    @Column(nullable = false)
    private Integer lastRow;

    @Column(nullable = false)
    private Integer firstCol;

    @Column(nullable = false)
    private Integer lastCol;
}
