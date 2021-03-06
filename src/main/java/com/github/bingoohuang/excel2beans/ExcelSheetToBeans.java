package com.github.bingoohuang.excel2beans;

import com.github.bingoohuang.instantiator.BeanInstantiator;
import com.github.bingoohuang.instantiator.BeanInstantiatorFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import lombok.Getter;
import lombok.val;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

public class ExcelSheetToBeans<T> {
    private final BeanInstantiator<T> instantiator;
    private final List<ExcelBeanField> beanFields;
    private @Getter final boolean hasTitle;
    private final DataFormatter cellFormatter = new DataFormatter();
    private final Sheet sheet;
    private final Table<Integer, Integer, ImageData> imageDataTable;
    private final boolean cellDataMapAttachable;

    public ExcelSheetToBeans(Workbook workbook, Class<T> beanClass) {
        this.instantiator = BeanInstantiatorFactory.newBeanInstantiator(beanClass);
        this.sheet = PoiUtil.findSheet(workbook, beanClass);
        ReflectAsmCache reflectAsmCache = new ReflectAsmCache();
        this.beanFields = new ExcelBeanFieldParser(beanClass, sheet).parseBeanFields(null, reflectAsmCache);
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
        for (int rowNum = startRowNum, ii = sheet.getLastRowNum(); rowNum <= ii; ++rowNum) {
            T object = new RowObjectCreator<T>(instantiator, beanFields, cellDataMapAttachable,
                    sheet, imageDataTable, cellFormatter, rowNum).createObject();
            if (object != null) {
                addToBeans(beans, rowNum, object);
            }
        }

        return beans;
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
        if (object instanceof ExcelRowIgnorable && ((ExcelRowIgnorable) object).ignoreRow())
            return;
        if (object instanceof ExcelRowReferable)
            ((ExcelRowReferable) object).setRowNum(i);

        beans.add(object);
    }


    private int jumpToStartDataRow() {
        int i = sheet.getFirstRowNum();
        if (!hasTitle) return i;

        // try to find the title row
        for (int ii = sheet.getLastRowNum(); i <= ii; ++i) {
            val row = sheet.getRow(i);

            parseContainsTitle(row);
            if (isTitleColumnsAllFound()) {
                resetNotFoundColumnIndex();
                return i + 1;
            }
        }

        checkTitleColumnsAllFound();
        throw new IllegalArgumentException("找不到标题行");
    }

    private void parseContainsTitle(Row row) {
        for (int j = 0, jj = beanFields.size(); j < jj; ++j) {
            val beanField = beanFields.get(j);
            if (!beanField.hasTitle()) {
                beanField.setColumnIndex(j + row.getFirstCellNum());
            } else {
                findColumn(row, beanField);
            }
        }

    }

    private void resetNotFoundColumnIndex() {
        beanFields.forEach(x -> {
            if (x.hasTitle() && !x.isTitleColumnFound()) x.setColumnIndex(-1);
        });
    }

    private boolean isTitleColumnsAllFound() {
        return !beanFields.stream().filter(ExcelBeanField::isTitleNotMatched).findAny().isPresent();
    }

    private void checkTitleColumnsAllFound() {
        beanFields.stream().filter(ExcelBeanField::isTitleNotMatched).findAny().ifPresent(x -> {
            throw new IllegalArgumentException("找不到[" + x.getTitle() + "]的列");
        });
    }

    private boolean findColumn(Row row, ExcelBeanField beanField) {
        for (int k = row.getFirstCellNum(), kk = row.getLastCellNum(); k < kk; ++k) {
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
        return beanFields.stream().anyMatch(ExcelBeanField::hasTitle);
    }

}
