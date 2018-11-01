package com.github.bingoohuang.beans2excel;

import com.github.bingoohuang.excel2beans.BeansToExcelOnTemplate;
import com.github.bingoohuang.excel2beans.ExcelToBeansUtils;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

public class BeansToExcelOnTemplateTest {
    @Test @SneakyThrows
    public void test1() {
        @Cleanup val wb = ExcelToBeansUtils.getClassPathWorkbook("hanergy-ceping.xlsx");
        val beansToExcel = new BeansToExcelOnTemplate(wb.getSheet("有评语-模板"));
        val bean = HanergyCepingResult.builder()
                .interviewCode("20181101.001")
                .name("东方不败")
                .gender("不男不女")
                .age("36")
                .position("高级HR主管")
                .level("16级")
                .annualSalary("18")
                .matchScore(3.8)
                .matchComment("观自在菩萨，行深般若波罗蜜多时，照见五蕴皆空，度一切苦厄。舍利子，色不异空，空不异色，色即是空，空即是色，受想行识，亦复如是。" +
                        "舍利子，是诸法空相，不生不灭，不垢不净，不增不减。" +
                        "是故空中无色，无受想行识，无眼耳鼻舌身意，无色声香味触法，无眼界，乃至无意识界，无无明，亦无无明尽，乃至无老死，亦无老死尽。" +
                        "无苦集灭道，无智亦无得。以无所得故。菩提萨埵，依般若波罗蜜多故，心无挂碍。无挂碍故，无有恐怖，远离颠倒梦想，究竟涅槃。" +
                        "三世诸佛，依般若波罗蜜多故，得阿耨多罗三藐三菩提。故知般若波罗蜜多，是大神咒，是大明咒，是无上咒，是无等等咒，能除一切苦，真实不虚。" +
                        "故说般若波罗蜜多咒，即说咒曰：揭谛揭谛，波罗揭谛，波罗僧揭谛，菩提萨婆诃。")

                .itemComment(HanergyCepingResult.ItemComment.builder().item("心理健康").comment("心理承受能力极强，非常乐观").build())
                .itemComment(HanergyCepingResult.ItemComment.builder().item("优势").comment("巴拉巴拉优势1").build())
                .itemComment(HanergyCepingResult.ItemComment.builder().item("优势").comment("巴拉巴拉优势2").build())
                .build();

        @Cleanup val newWb = beansToExcel.create(bean);

        ExcelToBeansUtils.writeExcel(newWb, "东方不败.xlsx");
    }
}
