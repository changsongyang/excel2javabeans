package com.github.bingoohuang.excel2beans;

import lombok.SneakyThrows;
import lombok.val;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Mapping excel cell values to java beans.
 *
 * @author bingoohuang [bingoohuang@gmail.com] Created on 2016/11/10.
 */
public class ExcelToBeans implements Closeable {
    private final Workbook workbook;
    private final boolean shouldBeClosedByMe;

    @SneakyThrows
    public ExcelToBeans(InputStream excelInputStream) {
        this.workbook = WorkbookFactory.create(excelInputStream);
        this.shouldBeClosedByMe = true;
    }

    @SneakyThrows
    public ExcelToBeans(Workbook workbook) {
        this.workbook = workbook;
        this.shouldBeClosedByMe = false;
    }


    @SneakyThrows
    public <T> List<T> convert(Class<T> beanClass) {
        val converter = new ExcelSheetToBeans(workbook, beanClass);
        return converter.convert();
    }

    @Override public void close() throws IOException {
        if (shouldBeClosedByMe) {
            workbook.close();
        }
    }
}
