package com.example.novaledger.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")            // 首頁
    public String index() { return "index"; }      // 對應 templates/index.html

    @GetMapping("/charts")      // 圖表頁
    public String charts() { return "charts"; }    // 對應 templates/charts.html

    @GetMapping("/tables")      // 表格頁
    public String tables() { return "tables"; }    // 對應 templates/tables.html

    @GetMapping("/ccAnalysis")
    public String ccAnalysis() { return "ccAnalysis"; }    // 對應 templates/tables.html

    @GetMapping("/ccTest")
    public String ccTest() { return "ccTest"; }    // 對應 templates/tables.html

    //開發
    @GetMapping("/bankSetting")
    public String bankSetting() { return "bankSetting"; }

    @GetMapping("/ccSettingTest")
    public String ccSettingTest() { return "ccSettingTest"; }
}