package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workbook_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkbookVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workbook_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Workbook workbook;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status;

    private String createdBy;
    
    // Deprecated: No longer written to. Will be removed in a future migration.
    @Deprecated
    @Lob
    @Column(columnDefinition="LONGBLOB")
    private byte[] fileData;
}
