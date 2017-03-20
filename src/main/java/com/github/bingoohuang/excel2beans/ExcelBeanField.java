package com.github.bingoohuang.excel2beans;

import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import lombok.Data;
import org.apache.poi.ss.usermodel.CellStyle;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * @author bingoohuang [bingoohuang@gmail.com] Created on 2016/11/10.
 */
@Data
public class ExcelBeanField {
    private String name;
    private String setter;
    private String getter;
    private String title;
    private int columnIndex;
    private CellStyle cellStyle;

    public <T> void setFieldValue(
            FieldAccess fieldAccess,
            MethodAccess methodAccess,
            T o,
            Object cellValue) {

        try {
            methodAccess.invoke(o, setter, cellValue);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            fieldAccess.set(o, name, cellValue);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Object getFieldValue(
            FieldAccess fieldAccess,
            MethodAccess methodAccess,
            Object o) {

        try {
            return methodAccess.invoke(o, getter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            return fieldAccess.get(o, name);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public boolean hasTitle() {
        return isNotEmpty(title);
    }

    public boolean containTitle(String cellValue) {
        return cellValue != null && cellValue.toUpperCase().contains(title);
    }

    public void setTitle(String title) {
        this.title = title.toUpperCase();
    }
}