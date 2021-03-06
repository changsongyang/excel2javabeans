package com.github.bingoohuang.beans2excel;

import com.github.bingoohuang.excel2beans.BeansToExcel;
import com.github.bingoohuang.excel2beans.PoiUtil;
import com.github.bingoohuang.excel2beans.annotations.ExcelColStyle;
import com.github.bingoohuang.excel2beans.annotations.ExcelColTitle;
import com.github.bingoohuang.excel2beans.annotations.ExcelSheet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.github.bingoohuang.excel2beans.annotations.ExcelColAlign.*;
import static com.google.common.truth.Truth.assertThat;

public class SimpleTest {
    @Test
    public void testHead() {
        val beansToExcel = new BeansToExcel();
        String name = "test-workbook-head.xlsx";

        List<Member> members = Lists.newArrayList();
        members.add(new Member(1000, 100, 80));

        List<Schedule> schedules = Lists.newArrayList();
        schedules.add(new Schedule(Timestamp.valueOf("2007-11-11 12:13:14"), 100, 200, 90, 10));
        schedules.add(new Schedule(Timestamp.valueOf("2007-01-11 12:13:14"), 101, 201, 91, 11));


        Map<String, Object> props = Maps.newHashMap();
        // 增加头行信息
        props.put("memberHead", "会员信息" + DateTime.now().toString("yyyy-MM-dd"));
        val workbook = beansToExcel.create(props, members, schedules);

        PoiUtil.writeExcel(workbook, name);
        new File(name).delete();
    }

    @Test @SneakyThrows
    public void testEmpty() {
        val beansToExcel = new BeansToExcel();
        String name = "test-empty.xlsx";

        val workbook = beansToExcel.create();

        Path path = Files.createTempFile("temp", ".xlsx");
        File file = path.toFile();
        file.deleteOnExit();

        @Cleanup val fileOut = new FileOutputStream(file);
        workbook.write(fileOut);

        @Cleanup Workbook wb = WorkbookFactory.create(file);
        assertThat(wb.getNumberOfSheets()).isEqualTo(1);
    }

    @Test
    public void test() {
        val beansToExcel = new BeansToExcel();
        createExcel(beansToExcel, "test-workbook.xlsx");
    }

    @Test
    public void testIncludeColumns() {
        val beansToExcel = new BeansToExcel();
        beansToExcel.includes(Member.class, "total", "effective");
        createExcel(beansToExcel, "test-workbook-total-effective.xlsx");
    }

    @Test @SneakyThrows
    public void testTemplate() {
        @Cleanup val workbook = PoiUtil.getClassPathWorkbook("template.xlsx");
        val beansToExcel = new BeansToExcel(workbook);
        createExcel(beansToExcel, "test-workbook-templ.xlsx");
    }

    @Test @SneakyThrows
    public void testTemplateIncludeColumns() {
        @Cleanup val workbook = PoiUtil.getClassPathWorkbook("template.xlsx");
        val beansToExcel = new BeansToExcel(workbook);
        beansToExcel.includes(Member.class, "total", "effective");
        createExcel(beansToExcel, "test-workbook-templ-total-effective.xlsx");
    }


    private void createExcel(BeansToExcel beansToExcel, String name) {

        List<Member> members = Lists.newArrayList();
        members.add(new Member(1000, 100, 80));

        List<Schedule> schedules = Lists.newArrayList();
        schedules.add(new Schedule(Timestamp.valueOf("2007-11-11 12:13:14"), 100, 200, 90, 10));
        schedules.add(new Schedule(Timestamp.valueOf("2007-01-11 12:13:14"), 101, 201, 91, 11));

        List<Subscribe> subscribes = Lists.newArrayList();
        subscribes.add(new Subscribe(Timestamp.valueOf("2007-01-11 12:13:14"), 100, 10));
        subscribes.add(new Subscribe(Timestamp.valueOf("2007-02-11 12:13:14"), 101, 11));
        subscribes.add(new Subscribe(Timestamp.valueOf("2007-03-11 12:13:14"), 102, 12));

        val workbook = beansToExcel.create(members, schedules, subscribes);

        PoiUtil.writeExcel(workbook, name);
        new File(name).delete();
    }

    @Data @AllArgsConstructor @ExcelSheet(name = "会员", headKey = "memberHead")
    public static class Member {
        @ExcelColTitle("会员总数")
        @ExcelColStyle(align = LEFT)
        private int total;
        @ExcelColTitle("其中：新增")
        @ExcelColStyle(align = CENTER)
        private int fresh;
        @ExcelColTitle("其中：有效")
        @ExcelColStyle(align = RIGHT)
        private int effective;
    }

    @Data @AllArgsConstructor @ExcelSheet(name = "排期")
    public static class Schedule {
        @ExcelColTitle("日期")
        private Timestamp time;
        @ExcelColTitle("排期数")
        private int schedules;
        @ExcelColTitle("定课数")
        private int subscribes;
        @ExcelColTitle("其中：小班课")
        private int publics;
        @ExcelColTitle("其中：私教课")
        private int privates;
    }

    @Data @AllArgsConstructor @ExcelSheet(name = "订课情况")
    public static class Subscribe {
        @ExcelColTitle("订单日期")
        private Timestamp day;
        @ExcelColTitle("人次")
        private int times;
        @ExcelColTitle("人数")
        private int heads;
    }
}
