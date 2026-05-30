package com.excel.platform.model;

public enum FormulaStatus {
    NONE,           // Not a formula
    SUPPORTED,      // Successfully evaluated by POI
    UNSUPPORTED,    // POI doesn't support this function natively
    ERROR           // Evaluation resulted in an error (e.g. #DIV/0!)
}
