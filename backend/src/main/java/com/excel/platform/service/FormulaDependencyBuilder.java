package com.excel.platform.service;

import com.excel.platform.model.Cell;
import com.excel.platform.model.FormulaDependency;
import com.excel.platform.repository.CellRepository;
import com.excel.platform.repository.FormulaDependencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormulaDependencyBuilder {

    private final FormulaDependencyRepository dependencyRepository;
    private final CellRepository cellRepository;

    // Simple regex to find cell references like A1, Z100, AA5
    private static final Pattern CELL_REF_PATTERN = Pattern.compile("\\b([A-Z]+[0-9]+)\\b");

    public void buildDependenciesForSheet(Long sheetId) {
        List<Cell> allCells = cellRepository.findBySheetId(sheetId);
        List<FormulaDependency> dependenciesToSave = new ArrayList<>();

        for (Cell dependentCell : allCells) {
            if (dependentCell.getFormulaExpression() != null) {
                Matcher matcher = CELL_REF_PATTERN.matcher(dependentCell.getFormulaExpression());
                while (matcher.find()) {
                    String sourceRef = matcher.group(1);
                    // Find the source cell in the same sheet
                    allCells.stream()
                            .filter(c -> c.getCellRef().equals(sourceRef))
                            .findFirst()
                            .ifPresent(sourceCell -> {
                                dependenciesToSave.add(FormulaDependency.builder()
                                        .sourceCell(sourceCell)
                                        .dependentCell(dependentCell)
                                        .build());
                            });
                }
            }
        }
        
        // Deduplicate and save
        dependencyRepository.saveAll(dependenciesToSave);
    }

    @Transactional
    public void rebuildDependenciesForSheet(Long sheetId) {
        // First delete existing dependencies for cells in this sheet
        dependencyRepository.deleteBySourceCellSheetId(sheetId);
        // Then rebuild
        buildDependenciesForSheet(sheetId);
        log.debug("Rebuilt formula dependencies for sheet {}", sheetId);
    }
}

