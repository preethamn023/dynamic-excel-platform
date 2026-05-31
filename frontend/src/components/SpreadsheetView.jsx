import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Box, Tabs, Tab, TextField, Paper, IconButton,
    Tooltip, CircularProgress, Typography, Alert
} from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';

import { getWorkbook, getSheets, getCells, updateCell, downloadWorkbookUrl } from '../api';
import useWebSocket from '../hooks/useWebSocket';

// Convert column index (0-based) to Excel-style column name (A, B, ..., Z, AA, AB, ...)
const getColName = (n) => {
    let s = '';
    while (n >= 0) {
        s = String.fromCharCode((n % 26) + 65) + s;
        n = Math.floor(n / 26) - 1;
    }
    return s;
};

const SpreadsheetView = () => {
    const { id: workbookId } = useParams();
    const navigate = useNavigate();

    const [workbook, setWorkbook] = useState(null);
    const [sheets, setSheets] = useState([]);
    const [currentSheetIdx, setCurrentSheetIdx] = useState(0);
    const [currentSheetId, setCurrentSheetId] = useState(null);
    const [gridData, setGridData] = useState([]);
    const [colDefs, setColDefs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedCellInfo, setSelectedCellInfo] = useState({ ref: '', value: '' });
    const lastLocalEditRef = useRef(null);

    const loadSheetData = useCallback(async (sheetId) => {
        setLoading(true);
        setError(null);
        try {
            const cellsRes = await getCells(sheetId);
            const cells = Array.isArray(cellsRes.data) ? cellsRes.data : [];

            // Calculate bounds
            let maxRow = 30;
            let maxCol = 10;
            cells.forEach(c => {
                if (c.rowIdx > maxRow) maxRow = c.rowIdx;
                if (c.colIdx > maxCol) maxCol = c.colIdx;
            });

            // Safety cap
            if (maxRow > 3000) maxRow = 3000;
            if (maxCol > 80) maxCol = 80;

            // Build column definitions
            const cols = [
                {
                    headerName: '#',
                    valueGetter: (params) => params.node.rowIndex + 1,
                    width: 55,
                    pinned: 'left',
                    suppressSizeToFit: true,
                    cellStyle: { background: '#f0f0f0', color: '#666', textAlign: 'center', fontWeight: 'bold' },
                    headerClass: 'row-number-header',
                }
            ];
            for (let c = 0; c <= maxCol; c++) {
                const colField = getColName(c);
                cols.push({
                    headerName: colField,
                    field: colField,
                    editable: true,
                    minWidth: 90,
                    valueFormatter: (params) => {
                        if (params.value === null || params.value === undefined) return '';
                        if (typeof params.value !== 'object') return String(params.value);
                        const { status, calculatedValue, rawValue, formulaExpression } = params.value;
                        if (status === 'UNSUPPORTED' || status === 'ERROR') {
                            return formulaExpression || rawValue || '';
                        }
                        if (calculatedValue !== null && calculatedValue !== undefined) return String(calculatedValue);
                        return rawValue !== null && rawValue !== undefined ? String(rawValue) : '';
                    }
                });
            }
            setColDefs(cols);

            // Build sparse row map
            const rowMap = {};
            cells.forEach(c => {
                if (c.rowIdx > maxRow || c.colIdx > maxCol) return;
                if (!rowMap[c.rowIdx]) rowMap[c.rowIdx] = {};
                const colName = getColName(c.colIdx);
                rowMap[c.rowIdx][colName] = {
                    rawValue: c.rawValue,
                    calculatedValue: c.calculatedValue,
                    formulaExpression: c.formulaExpression,
                    status: c.formulaStatus
                };
            });

            const rowData = [];
            for (let r = 0; r <= maxRow; r++) {
                rowData.push(rowMap[r] || {});
            }
            setGridData(rowData);
        } catch (e) {
            console.error('Failed to load sheet data', e);
            setError('Failed to load sheet data. Please check if the backend is running.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        const init = async () => {
            setLoading(true);
            setError(null);
            try {
                const wbRes = await getWorkbook(workbookId);
                setWorkbook(wbRes.data);

                const sheetsRes = await getSheets(workbookId);
                const sheetsList = Array.isArray(sheetsRes.data) ? sheetsRes.data : [];
                setSheets(sheetsList);

                if (sheetsList.length > 0) {
                    setCurrentSheetId(sheetsList[0].id);
                    await loadSheetData(sheetsList[0].id);
                } else {
                    setLoading(false);
                }
            } catch (e) {
                console.error('Failed to initialize workbook', e);
                setError('Failed to load workbook. Please go back and try again.');
                setLoading(false);
            }
        };
        init();
    }, [workbookId, loadSheetData]);

    const handleTabChange = (event, newValue) => {
        setCurrentSheetIdx(newValue);
        setCurrentSheetId(sheets[newValue].id);
        loadSheetData(sheets[newValue].id);
    };

    const handleWebSocketUpdate = useCallback((updatedCells) => {
        if (!Array.isArray(updatedCells) || updatedCells.length === 0) return;

        // Check if this is an echo of our own edit
        const lastEdit = lastLocalEditRef.current;
        if (lastEdit && Date.now() - lastEdit.timestamp < 2000) {
            const isEcho = updatedCells.some(uc => {
                const colName = getColName(uc.colIdx);
                const ref = `${colName}${uc.rowIdx + 1}`;
                return ref === lastEdit.cellRef;
            });
            if (isEcho) {
                lastLocalEditRef.current = null;
                return;
            }
        }

        setGridData(prevData => {
            const newGridData = [...prevData];
            updatedCells.forEach(uc => {
                const cName = getColName(uc.colIdx);
                if (newGridData[uc.rowIdx]) {
                    newGridData[uc.rowIdx] = { ...newGridData[uc.rowIdx] };
                    newGridData[uc.rowIdx][cName] = {
                        rawValue: uc.rawValue,
                        calculatedValue: uc.calculatedValue,
                        formulaExpression: uc.formulaExpression,
                        status: uc.formulaStatus
                    };
                }
            });
            return newGridData;
        });
    }, []);

    const { connected } = useWebSocket(workbookId, currentSheetId, handleWebSocketUpdate);

    const handleCellClicked = (e) => {
        if (!e.colDef.field) return;
        const colField = e.colDef.field;
        const rowNum = e.rowIndex + 1;
        const cellData = e.value;
        let displayValue = '';
        if (cellData && typeof cellData === 'object') {
            displayValue = cellData.formulaExpression
                ? cellData.formulaExpression
                : (cellData.rawValue || '');
        }
        setSelectedCellInfo({ ref: `${colField}${rowNum}`, value: displayValue });
    };

    const onCellValueChanged = async (e) => {
        if (!e.colDef.field) return;
        const colField = e.colDef.field;
        const typedValue = e.newValue;
        const oldValueObj = e.oldValue;
        const cellRef = `${colField}${e.rowIndex + 1}`;
        const isFormula = typedValue && String(typedValue).startsWith('=');

        lastLocalEditRef.current = { cellRef, timestamp: Date.now() };

        try {
            const activeSheetId = sheets[currentSheetIdx].id;
            const res = await updateCell(workbookId, activeSheetId, cellRef, typedValue, isFormula);
            const updatedCells = Array.isArray(res.data) ? res.data : [];

            const newGridData = [...gridData];
            updatedCells.forEach(uc => {
                if (uc.sheet && uc.sheet.id === activeSheetId) {
                    const cName = getColName(uc.colIdx);
                    if (newGridData[uc.rowIdx]) {
                        newGridData[uc.rowIdx][cName] = {
                            rawValue: uc.rawValue,
                            calculatedValue: uc.calculatedValue,
                            formulaExpression: uc.formulaExpression,
                            status: uc.formulaStatus
                        };
                    }
                }
            });
            setGridData(newGridData);
        } catch (err) {
            console.error('Cell update failed', err);
            // Revert cell
            const newGridData = [...gridData];
            if (newGridData[e.rowIndex]) {
                newGridData[e.rowIndex][colField] = oldValueObj;
            }
            setGridData(newGridData);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100%', overflow: 'hidden' }}>
            {/* Top toolbar */}
            <Paper elevation={1} sx={{ display: 'flex', alignItems: 'center', p: 1, gap: 1, flexShrink: 0, borderRadius: 0 }}>
                <Tooltip title="Back to Dashboard">
                    <IconButton onClick={() => navigate('/')} size="small">
                        <ArrowBackIcon />
                    </IconButton>
                </Tooltip>

                <Typography variant="subtitle1" fontWeight="bold" sx={{ mr: 1, color: 'primary.main', minWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {workbook?.fileName || 'Loading...'}
                </Typography>

                <Tooltip title={connected ? 'Connected - Real-time updates active' : 'Disconnected - Changes may not sync'}>
                    <Box
                        sx={{
                            width: 12,
                            height: 12,
                            borderRadius: '50%',
                            backgroundColor: connected ? '#4caf50' : '#f44336',
                            mr: 1,
                            flexShrink: 0,
                        }}
                    />
                </Tooltip>

                <Tooltip title="Download Excel">
                    <IconButton color="primary" component="a" href={downloadWorkbookUrl(workbookId)} target="_blank" size="small">
                        <DownloadIcon />
                    </IconButton>
                </Tooltip>

                <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1, ml: 1, gap: 1 }}>
                    <Typography variant="body2" sx={{ minWidth: 50, fontWeight: 'bold', color: '#555', bgcolor: '#f0f0f0', p: '2px 6px', borderRadius: 1, border: '1px solid #ddd', textAlign: 'center' }}>
                        {selectedCellInfo.ref || 'A1'}
                    </Typography>
                    <TextField
                        fullWidth
                        size="small"
                        value={selectedCellInfo.value}
                        onChange={() => {}}
                        InputProps={{ readOnly: true }}
                        placeholder="Click a cell to see its value or formula"
                        sx={{ bgcolor: 'white' }}
                    />
                </Box>
            </Paper>

            {/* Error alert */}
            {error && (
                <Alert severity="error" sx={{ borderRadius: 0 }}>{error}</Alert>
            )}

            {/* Grid area */}
            <Box sx={{ flexGrow: 1, overflow: 'hidden', position: 'relative' }}>
                {loading ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', flexDirection: 'column', gap: 2 }}>
                        <CircularProgress />
                        <Typography color="text.secondary">Loading spreadsheet data...</Typography>
                    </Box>
                ) : (
                    <div className="ag-theme-alpine" style={{ width: '100%', height: '100%' }}>
                        <AgGridReact
                            rowData={gridData}
                            columnDefs={colDefs}
                            defaultColDef={{ resizable: true, sortable: false, filter: false }}
                            onCellClicked={handleCellClicked}
                            onCellValueChanged={onCellValueChanged}
                            suppressMovableColumns={true}
                            rowSelection="single"
                            enableCellTextSelection={true}
                        />
                    </div>
                )}
            </Box>

            {/* Sheet tabs */}
            <Paper elevation={1} sx={{ flexShrink: 0, borderTop: '1px solid #e0e0e0', borderRadius: 0 }}>
                {sheets.length > 0 ? (
                    <Tabs
                        value={currentSheetIdx}
                        onChange={handleTabChange}
                        variant="scrollable"
                        scrollButtons="auto"
                        sx={{ minHeight: 36, '& .MuiTab-root': { minHeight: 36, py: 0.5 } }}
                    >
                        {sheets.map((s, idx) => (
                            <Tab key={s.id} label={s.sheetName} id={`sheet-tab-${idx}`} />
                        ))}
                    </Tabs>
                ) : (
                    <Box sx={{ p: 1, color: 'text.secondary', fontSize: 14 }}>
                        No sheets found in this workbook.
                    </Box>
                )}
            </Paper>
        </Box>
    );
};

export default SpreadsheetView;
