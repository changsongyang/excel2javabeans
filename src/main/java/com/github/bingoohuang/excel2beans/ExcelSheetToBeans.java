package com.github.bingoohuang.excel2beans;

import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.github.bingoohuang.excel2beans.CellData.CellDataBuilder;
import com.github.bingoohuang.util.instantiator.BeanInstantiator;
import com.github.bingoohuang.util.instantiator.BeanInstantiatorFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class ExcelSheetToBeans<T> {
    private final Workbook workbook;
    private final FieldAccess fieldAccess;
    private final MethodAccess methodAccess;
    private final BeanInstantiator<T> instantiator;
    private final List<ExcelBeanField> beanFields;
    private @Getter final boolean hasTitle;
    private final DataFormatter cellFormatter = new DataFormatter();
    private final Sheet sheet;
    private final Table<Integer, Integer, ImageData> imageDataTable;
    private final boolean cellDataMapAttachable;

    public ExcelSheetToBeans(Workbook workbook, Class<T> beanClass) {
        this.workbook = workbook;
        this.fieldAccess = FieldAccess.get(beanClass);
        this.methodAccess = MethodAccess.get(beanClass);
        this.instantiator = BeanInstantiatorFactory.newBeanInstantiator(beanClass);
        this.sheet = ExcelToBeansUtils.findSheet(workbook, beanClass);
        this.beanFields = new ExcelBeanFieldParser(beanClass, null).parseBeanFields();
        this.imageDataTable = hasImageDatas() ? ExcelImages.readAllCellImages(sheet) : null;
        this.hasTitle = hasTitle();
        this.cellDataMapAttachable = CellDataMapAttachable.class.isAssignableFrom(beanClass);
    }

    public int findTitleRowNum() {
        int i = sheet.getFirstRowNum();
        if (!hasTitle) return i;

        // try to find the title row
        for (int ii = sheet.getLastRowNum(); i <= ii; ++i) {
            val row = sheet.getRow(i);

            for (int j = 0, jj = beanFields.size(); j < jj; ++j) {
                val beanField = beanFields.get(j);
                if (beanField.hasTitle() && findColumn(row, beanField)) {
                    return i;
                }
            }
        }

        throw new IllegalArgumentException("Unable to find title row.");
    }

    public List<T> convert() {
        val beans = Lists.<T>newArrayList();

        val startRowNum = jumpToStartDataRow();
        for (int i = startRowNum, ii = sheet.getLastRowNum(); i <= ii; ++i) {
            T object = createObject(sheet, i);
            if (object != null) {
                addToBeans(beans, i, object);
            }
        }

        return beans;
    }

    private T createObject(Sheet sheet, int i) {
        val row = sheet.getRow(i);
        if (row == null) return null;

        T object = (T) instantiator.newInstance();

        Map<String, CellData> cellDataMap = null;
        if (cellDataMapAttachable) cellDataMap = Maps.newHashMap();

        val emptyNum = processRow(object, row, cellDataMap);
        if (emptyNum == beanFields.size()) {
            object = null;
        } else if (cellDataMapAttachable) {
            ((CellDataMapAttachable) object).attachCellDataMap(cellDataMap);
        }

        return object;
    }

    private boolean hasImageDatas() {
        for (val beanField : beanFields) {
            int columnIndex = beanField.getColumnIndex();
            if (columnIndex < 0) continue;

            if (beanField.isImageDataField()) return true;
        }
        return false;
    }

    private void addToBeans(List<T> beans, int i, T object) {
        if (object instanceof ExcelRowIgnorable) {
            val ignore = (ExcelRowIgnorable) object;
            if (ignore.ignoreRow()) return;
        }

        if (object instanceof ExcelRowRef) {
            val ref = (ExcelRowRef) object;
            ref.setRowNum(i);
        }

        beans.add(object);
    }

    private int processRow(T object, Row row, Map<String, CellData> cellDataMap) {
        int emptyNum = 0;
        for (val beanField : beanFields) {
            val fieldValue = parseFieldValue(row, beanField, cellDataMap);

            if (fieldValue == null) {
                ++emptyNum;
            } else {
                beanField.setFieldValue(fieldAccess, methodAccess, object, fieldValue);
            }
        }

        return emptyNum;
    }

    private Object parseFieldValue(Row row, ExcelBeanField beanField, Map<String, CellData> cellDataMap) {
        if (beanField.isMultipleColumns()) {
            return parseMultipleFieldValue(row, beanField, cellDataMap);
        } else {
            return processSingleColumn(beanField.getColumnIndex(), beanField, row, -1, cellDataMap);
        }
    }

    private Object parseMultipleFieldValue(Row row, ExcelBeanField beanField, Map<String, CellData> cellDataMap) {
        int nonEmptyFieldValues = 0;
        val fieldValues = Lists.<Object>newArrayList();
        for (int columnIndex : beanField.getMultipleColumnIndexes()) {
            val value = processSingleColumn(columnIndex, beanField, row, fieldValues.size(), cellDataMap);
            fieldValues.add(value);

            if (value != null) ++nonEmptyFieldValues;
        }

        return nonEmptyFieldValues > 0 ? fieldValues : null;
    }

    private Object processSingleColumn(int columnIndex, ExcelBeanField beanField, Row row,
                                       int fieldName_index, Map<String, CellData> cellDataMap) {
        if (columnIndex < 0) return null;

        val cell = row.getCell(columnIndex);

        if (beanField.isImageDataField()) {
            attachCellDataMap(beanField, row, columnIndex, fieldName_index, cellDataMap, cell);
            return imageDataTable.get(row.getRowNum(), columnIndex);
        } else {
            val cellValue = getCellValue(cell);

            return convertCellValue(beanField, cell, cellValue, row.getRowNum(), columnIndex, fieldName_index, cellDataMap);
        }
    }

    private void attachCellDataMap(ExcelBeanField beanField, Row row, int columnIndex,
                                   int fieldName_index, Map<String, CellData> cellDataMap, Cell cell) {
        if (!cellDataMapAttachable) return;

        val attachFieldName = createAttachFieldName(beanField, fieldName_index);
        val cellData = createCellData(cell, null, row.getRowNum(), columnIndex);
        cellDataMap.put(attachFieldName, cellData);
    }

    private String createAttachFieldName(ExcelBeanField beanField, int fieldName_index) {
        val fieldName = beanField.getFieldName();
        return fieldName_index < 0 ? fieldName : fieldName + "_" + fieldName_index;
    }

    private Object convertCellValue(ExcelBeanField beanField, Cell cell, String cellValue, int rowNum,
                                    int columnIndex,
                                    int fieldName_index, Map<String, CellData> cellDataMap) {

        CellData cellData = null;
        if (beanField.isCellDataType() || cellDataMapAttachable) {
            cellData = createCellData(cell, cellValue, rowNum, columnIndex);
        }

        if (cellDataMapAttachable) {
            val attachFieldName = createAttachFieldName(beanField, fieldName_index);
            cellDataMap.put(attachFieldName, cellData);
        }

        if (StringUtils.isEmpty(cellValue)) return null;

        return beanField.isCellDataType() ? cellData : beanField.convert(cellValue);
    }

    private CellData createCellData(Cell cell, String cellValue, int rowNum, int colNum) {
        val cellData = CellData.builder()
                .value(cellValue).row(rowNum).col(colNum)
                .sheetIndex(workbook.getSheetIndex(sheet));
        applyComment(cell, cellData);
        return cellData.build();
    }

    private void applyComment(Cell cell, CellDataBuilder cellData) {
        if (cell == null) return;

        val comment = cell.getCellComment();
        if (comment == null) return;

        cellData.comment(comment.getString().getString())
                .commentAuthor(comment.getAuthor());
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        val cellType = cell.getCellTypeEnum();
        if (cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            val dateCellValue = cell.getDateCellValue();
            val sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(dateCellValue);
        }

        val cellValue = cellFormatter.formatCellValue(cell);
        return StringUtils.trim(cellValue);
    }


    private int jumpToStartDataRow() {
        int i = sheet.getFirstRowNum();
        if (!hasTitle) return i;

        // try to find the title row
        for (int ii = sheet.getLastRowNum(); i <= ii; ++i) {
            val row = sheet.getRow(i);

            val containsTitle = parseContainsTitle(row);
            if (containsTitle) {
                resetNotFoundColumnIndex();
                checkTitleColumnsAllFound();
                return i + 1;
            }
        }

        throw new IllegalArgumentException("找不到标题行");
    }

    private boolean parseContainsTitle(Row row) {
        boolean containsTitle = false;
        for (int j = 0, jj = beanFields.size(); j < jj; ++j) {
            val beanField = beanFields.get(j);
            if (!beanField.hasTitle()) {
                beanField.setColumnIndex(j + row.getFirstCellNum());
            } else {
                if (findColumn(row, beanField) && !containsTitle) {
                    containsTitle = true;
                }
            }
        }

        return containsTitle;
    }

    private void resetNotFoundColumnIndex() {
        for (val beanField : beanFields) {
            if (beanField.hasTitle() && !beanField.isTitleColumnFound()) {
                beanField.setColumnIndex(-1);
            }
        }
    }

    private void checkTitleColumnsAllFound() {
        for (val beanField : beanFields) {
            if (beanField.isTitleNotMatched()) {
                throw new IllegalArgumentException("找不到[" + beanField.getTitle() + "]的列");
            }
        }
    }

    private boolean findColumn(Row row, ExcelBeanField beanField) {
        for (int k = row.getFirstCellNum(), kk = row.getLastCellNum(); k <= kk; ++k) {
            val cell = row.getCell(k);
            if (cell == null) continue;

            val cellValue = cell.getStringCellValue();
            if (beanField.containTitle(cellValue)) {
                beanField.setColumnIndex(cell.getColumnIndex());
                beanField.setTitleColumnFound(true);

                if (!beanField.isMultipleColumns()) return true;

                beanField.addMultipleColumnIndex(cell.getColumnIndex());
            }
        }

        return !beanField.getMultipleColumnIndexes().isEmpty();
    }

    private boolean hasTitle() {
        for (val beanField : beanFields) {
            if (beanField.hasTitle()) return true;
        }

        return false;
    }
}
