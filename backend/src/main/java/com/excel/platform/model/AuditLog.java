package com.excel.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workbook_id", nullable = false)
    private Workbook workbook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private Sheet sheet;

    @Column(nullable = false, length = 20)
    private String cellRef;

    @Column(columnDefinition="TEXT")
    private String oldValue;

    @Column(columnDefinition="TEXT")
    private String newValue;

    @Column(nullable = false)
    private LocalDateTime modifiedDate;

    private String modifiedBy;
}
