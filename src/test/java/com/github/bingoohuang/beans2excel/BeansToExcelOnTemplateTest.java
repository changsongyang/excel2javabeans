package com.github.bingoohuang.beans2excel;

import com.github.bingoohuang.beans2excel.CepingResult.ItemComment;
import com.github.bingoohuang.excel2beans.BeansToExcelOnTemplate;
import com.github.bingoohuang.excel2beans.ExcelToBeansUtils;
import com.github.bingoohuang.excel2beans.PoiUtil;
import com.github.bingoohuang.utils.lang.Collects;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static com.github.bingoohuang.beans2excel.CepingResult.Item;

public class BeansToExcelOnTemplateTest {
    @Test
    public void test1() {
        val builder = CepingResult.builder();
        builder
                .sheetName("东方不败测评结论表")
                .interviewCode("20181101.001")
                .name("东方不败")
                .gender("不男不女")
                .age("36")
                .position("高级HR主管")
                .level("16级")
                .annualSalary("18")
                .matchScore(3.8)

                .item(Item.builder().category("基本素质").quality("心理健康").dimension("焦虑不安,抑郁消沉,偏执多疑,冷漠孤僻,特立独行,冲动暴躁,喜怒无常,社交回避,僵化固执,依赖顺从,夸张做作,狂妄自恋").score("无高风险").remark("").build())
                .item(Item.builder().category("基本素质").quality("诚信").dimension("称许性").score("正常").remark("").build())
                .item(Item.builder().category("核心素质").quality("使命认同").dimension("文化价值观匹配").score("6.8").remark("工作场景偏好(一票否决)").build())
                .item(Item.builder().category("核心素质").quality("责任心").dimension("尽责").score("1^2").remark("Hogan").build())
                .item(Item.builder().category("核心素质").quality("责任心").dimension("精通掌握").score("1^2").remark("Hogan").build())
                .item(Item.builder().category("核心素质").quality("责任心").dimension("避免麻烦").score("1^2").remark("Hogan").build())
                .item(Item.builder().category("核心素质").quality("责任心").dimension("胆怯").score("1^2").remark("Hogan").build())
                .item(Item.builder().category("核心素质").quality("责任心").dimension("循规蹈矩").score("1^2").remark("Hogan").build())
                .item(Item.builder().category("核心素质").quality("学习能力").dimension("科学能力").score("2^2").remark("Hogan").build());


        export(builder);


        builder.matchComment("观自在菩萨，行深般若波罗蜜多时，照见五蕴皆空，度一切苦厄。舍利子，色不异空，空不异色，色即是空，空即是色，受想行识，亦复如是。" +
                "舍利子，是诸法空相，不生不灭，不垢不净，不增不减。" +
                "是故空中无色，无受想行识，无眼耳鼻舌身意，无色声香味触法，无眼界，乃至无意识界，无无明，亦无无明尽，乃至无老死，亦无老死尽。" +
                "无苦集灭道，无智亦无得。以无所得故。菩提萨埵，依般若波罗蜜多故，心无挂碍。无挂碍故，无有恐怖，远离颠倒梦想，究竟涅槃。" +
                "三世诸佛，依般若波罗蜜多故，得阿耨多罗三藐三菩提。故知般若波罗蜜多，是大神咒，是大明咒，是无上咒，是无等等咒，能除一切苦，真实不虚。" +
                "故说般若波罗蜜多咒，即说咒曰：揭谛揭谛，波罗揭谛，波罗僧揭谛，菩提萨婆诃。");

        export(builder);

        builder
                .itemComment(ItemComment.builder().item("心理健康").comment("心理承受能力极强，非常乐观").build())
                .itemComment(ItemComment.builder().item("优势").comment("巴拉巴拉优势1").build())
                .itemComment(ItemComment.builder().item("优势").comment("巴拉巴拉优势2").build());

        export(builder);
    }

    @SneakyThrows
    private void export(CepingResult.CepingResultBuilder builder) {
        @Cleanup val wb = ExcelToBeansUtils.getClassPathWorkbook("ceping.xlsx");
        val bean = builder.build();

        val templateName = Collects.isEmpty(bean.getItemComments())
                ? StringUtils.isEmpty(bean.getMatchComment()) ? "无总评语-模板" : "无评语-模板" : "有评语-模板";

        System.out.println(templateName);

        val beansToExcel = new BeansToExcelOnTemplate(wb.getSheet(templateName));

        @Cleanup val newWb = beansToExcel.create(bean);

        PoiUtil.writeExcel(newWb, templateName + "-result.xlsx");
    }
}
