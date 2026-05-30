-- Drop tables if they exist to allow clean recreation
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS formula_dependencies;
DROP TABLE IF EXISTS cells;
DROP TABLE IF EXISTS cell_styles;
DROP TABLE IF EXISTS merged_regions;
DROP TABLE IF EXISTS named_ranges;
DROP TABLE IF EXISTS sheets;
DROP TABLE IF EXISTS workbook_versions;
DROP TABLE IF EXISTS workbooks;

CREATE TABLE workbooks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    upload_date DATETIME NOT NULL,
    current_version INT NOT NULL,
    status VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workbook_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workbook_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_date DATETIME NOT NULL,
    created_by VARCHAR(255),
    file_data LONGBLOB,
    CONSTRAINT fk_version_workbook FOREIGN KEY (workbook_id) REFERENCES workbooks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE named_ranges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workbook_version_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    reference_expression TEXT NOT NULL,
    CONSTRAINT fk_namedrange_version FOREIGN KEY (workbook_version_id) REFERENCES workbook_versions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sheets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workbook_version_id BIGINT NOT NULL,
    sheet_name VARCHAR(255) NOT NULL,
    sheet_index INT NOT NULL,
    visibility_state VARCHAR(50),
    CONSTRAINT fk_sheet_version FOREIGN KEY (workbook_version_id) REFERENCES workbook_versions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE merged_regions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id BIGINT NOT NULL,
    first_row INT NOT NULL,
    last_row INT NOT NULL,
    first_col INT NOT NULL,
    last_col INT NOT NULL,
    CONSTRAINT fk_mergedregion_sheet FOREIGN KEY (sheet_id) REFERENCES sheets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cell_styles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workbook_version_id BIGINT NOT NULL,
    style_hash VARCHAR(255) NOT NULL,
    font_name VARCHAR(255),
    font_size INT,
    is_bold BOOLEAN,
    is_italic BOOLEAN,
    background_color VARCHAR(50),
    border_top VARCHAR(50),
    border_bottom VARCHAR(50),
    border_left VARCHAR(50),
    border_right VARCHAR(50),
    number_format VARCHAR(255),
    CONSTRAINT fk_cellstyle_version FOREIGN KEY (workbook_version_id) REFERENCES workbook_versions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cells (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id BIGINT NOT NULL,
    style_id BIGINT,
    row_idx INT NOT NULL,
    col_idx INT NOT NULL,
    cell_ref VARCHAR(20) NOT NULL,
    raw_value TEXT,
    calculated_value TEXT,
    formula_expression TEXT,
    formula_status VARCHAR(50),
    data_type VARCHAR(50),
    CONSTRAINT fk_cell_sheet FOREIGN KEY (sheet_id) REFERENCES sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cell_style FOREIGN KEY (style_id) REFERENCES cell_styles(id) ON DELETE SET NULL,
    UNIQUE KEY uk_sheet_row_col (sheet_id, row_idx, col_idx)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sheet_cellref ON cells(sheet_id, cell_ref);

CREATE TABLE formula_dependencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_cell_id BIGINT NOT NULL,
    dependent_cell_id BIGINT NOT NULL,
    CONSTRAINT fk_dep_source FOREIGN KEY (source_cell_id) REFERENCES cells(id) ON DELETE CASCADE,
    CONSTRAINT fk_dep_target FOREIGN KEY (dependent_cell_id) REFERENCES cells(id) ON DELETE CASCADE,
    UNIQUE KEY uk_source_target (source_cell_id, dependent_cell_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_dep_source ON formula_dependencies(source_cell_id);
CREATE INDEX idx_dep_target ON formula_dependencies(dependent_cell_id);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workbook_id BIGINT NOT NULL,
    sheet_id BIGINT NOT NULL,
    cell_ref VARCHAR(20) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    modified_date DATETIME NOT NULL,
    modified_by VARCHAR(255),
    CONSTRAINT fk_audit_workbook FOREIGN KEY (workbook_id) REFERENCES workbooks(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_sheet FOREIGN KEY (sheet_id) REFERENCES sheets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
