package com.github.bingoohuang.excel2maps;

import com.github.bingoohuang.excel2maps.impl.ColumnRef;
import com.github.bingoohuang.excel2maps.impl.Ignored;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * @author bingoohuang [bingoohuang@gmail.com] Created on 2016/11/15.
 */
public class Excel2Maps {
    final DataFormatter cellFormatter = new DataFormatter();
    private final Excel2MapsConfig excel2MapsConfig;
    private final List<ColumnRef> columnRefs;

    public Excel2Maps(Excel2MapsConfig excel2MapsConfig) {
        this.excel2MapsConfig = excel2MapsConfig;
        this.columnRefs = Lists.newArrayList();
    }

    @SneakyThrows
    public List<Map<String, String>> convert(InputStream excelInputStream) {
        val workbook = WorkbookFactory.create(excelInputStream);
        return convert(workbook);
    }

    public List<Map<String, String>> convert(Workbook workbook) {
        List<Map<String, String>> beans = Lists.newArrayList();

        val sheet = workbook.getSheetAt(0);
        val startRowNum = jumpToStartDataRow(sheet);

        for (int i = startRowNum, ii = sheet.getLastRowNum(); i <= ii; ++i) {
            Map<String, String> map = createRowMap(sheet, i);
            if (map != null) {
                map.put("_rowNum", Integer.toString(i));
                beans.add(map);
            }
        }

        return beans;
    }

    private Map<String, String> createRowMap(Sheet sheet, int i) {
        val row = sheet.getRow(i);
        if (row != null) {
            Map<String, String> map = Maps.newHashMap();
            val ignore = processOrIgnoreRow(row, map);
            if (ignore != Ignored.YES) {
                return map;
            }
        }

        return null;
    }

    private Ignored processOrIgnoreRow(Row row, Map<String, String> map) {
        int emptyNum = 0;
        for (ColumnRef columnRef : columnRefs) {
            val cell = row.getCell(columnRef.getColumnIndex());
            val cellValue = getCellValue(cell);
            if (isEmpty(cellValue)) {
                emptyNum++;
            } else {
                val ignore = columnRef.putMapOrIgnored(map, cellValue);
                if (ignore == Ignored.YES) {
                    return Ignored.YES;
                }
            }
        }

        return emptyNum == columnRefs.size() ? Ignored.YES : Ignored.NO;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        val cellValue = cellFormatter.formatCellValue(cell);
        return trim(cellValue);
    }


    private int jumpToStartDataRow(Sheet sheet) {
        int i = sheet.getFirstRowNum();

        // try to find the title row
        for (int ii = sheet.getLastRowNum(); i <= ii; ++i) {
            val row = sheet.getRow(i);

            boolean containsTitle = false;
            for (val columnDef : excel2MapsConfig.getColumnDefs()) {
                if (findColumn(row, columnDef)) containsTitle = true;
            }

            if (containsTitle) return i + 1;
        }

        return i;
    }

    private boolean findColumn(Row row, ColumnDef columnDef) {
        for (int k = row.getFirstCellNum(), kk = row.getLastCellNum(); k <= kk; ++k) {
            Cell cell = row.getCell(k);
            if (cell == null) continue;

            val cellValue = cell.getStringCellValue();
            val upperCellValue = StringUtils.upperCase(cellValue);
            if (StringUtils.contains(upperCellValue, columnDef.getTitle())) {
                columnRefs.add(new ColumnRef(columnDef, k));
                return true;
            }
        }

        return false;
    }
}
