package com.ai.cs.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Component
public class ExcelParser implements DocumentParser {

    @Override
    public String[] supportedTypes() {
        return new String[]{"XLSX", "XLS"};
    }

    @Override
    public String parse(InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        StringBuilder text = new StringBuilder();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                text.append("【工作表").append(sheet.getSheetName()).append("】\n");

                for (Row row : sheet) {
                    StringBuilder rowText = new StringBuilder();
                    for (Cell cell : row) {
                        String cellValue = getCellValue(cell);
                        if (!cellValue.isEmpty()) {
                            if (rowText.length() > 0) {
                                rowText.append(" | ");
                            }
                            rowText.append(cellValue);
                        }
                    }
                    if (rowText.length() > 0) {
                        text.append(rowText).append("\n");
                    }
                }
                text.append("\n");
            }

            log.info("Excel解析完成: sheets={}", sheetCount);
        }

        return text.toString();
    }

    private String getCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    yield cell.getCellFormula();
                }
            }
            default -> "";
        };
    }
}
