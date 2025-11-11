package com.example.tableaudemov2.service;

import com.example.tableaudemov2.dto.ParseResult;
import com.example.tableaudemov2.dto.StatementItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.Rectangle;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 只解析「表格」：
 * - 不讀上方說明文字
 * - 不啟動 OCR
 * - 只在頁面下 30%~95% 區域抽表格
 */
@Service
public class OcrPdfService {

    /** 與現有 Controller 相容的入口 */
    public ParseResult parseCreditCardStatement(InputStream pdfIn) throws Exception {
        return parseStatementTableOnly(pdfIn);
    }

    /** 外部呼叫：只回傳表格資料 */
    public ParseResult parseStatementTableOnly(InputStream pdfIn) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfIn)) {
            List<StatementItem> items = extractTablesOnly(doc);
            return new ParseResult("玉山銀行", "", items, "", "");
        }
    }

    /* ===================== 表格抽取 ===================== */

    private List<StatementItem> extractTablesOnly(PDDocument doc) {
        List<StatementItem> out = new ArrayList<>();
        ObjectExtractor oe = new ObjectExtractor(doc);

        for (int p = 1; p <= doc.getNumberOfPages(); p++) {
            Page page = oe.extract(p);

            // 只鎖定表格常見位置（頁面下 30% ~ 95%）
            float w = (float) page.getWidth();
            float h = (float) page.getHeight();
            Rectangle areaRect = new Rectangle(0f, h * 0.30f, w, h * 0.65f);
            Page area = page.getArea(areaRect);

            // 先 lattice（Spreadsheet），再 stream（Basic）
            List<Table> tables = new SpreadsheetExtractionAlgorithm().extract(area);
            if (tables.isEmpty()) {
                tables = new BasicExtractionAlgorithm().extract(area);
            }

            for (Table tb : tables) {
                out.addAll(toItemsByHeader(tb));
            }
        }
        return out;
    }

    /** 依表頭關鍵字定位欄位，只輸出真正的資料列 */
    private List<StatementItem> toItemsByHeader(Table tb) {
        List<StatementItem> list = new ArrayList<>();

        // 注意：Tabula 1.0.5 這裡是原始類型，沒有泛型參數
        List<List<RectangularTextContainer>> rows = tb.getRows();
        if (rows.isEmpty()) return list;

        // 1) 找表頭
        int headerIdx = -1;
        for (int i = 0; i < Math.min(rows.size(), 8); i++) {
            String line = joinRow(rows.get(i)).replaceAll("\\s+", "");
            if (line.contains("交易日期") && line.contains("入帳日期")
                    && (line.contains("交易項目") || line.contains("交易國家") || line.contains("交易說明"))
                    && line.contains("幣別")
                    && (line.contains("交易金額") || line.contains("金額"))
                    && (line.contains("新臺幣") || line.toUpperCase().contains("TWD"))) {
                headerIdx = i; break;
            }
        }
        if (headerIdx == -1) return list;

        // 2) 欄位索引
        List<RectangularTextContainer> header = rows.get(headerIdx);
        int idxTxn  = findCol(header, "交易日期");
        int idxPost = findCol(header, "入帳日期");
        int idxDesc = findFirstCol(header, "交易項目", "交易說明", "交易項目/交易國家或地區");
        int idxCurr = findCol(header, "幣別");
        int idxAmt  = findFirstCol(header, "交易金額", "金額");
        int idxTwd  = findFirstCol(header, "新臺幣", "TWD");

        // 3) 逐列轉模型
        for (int r = headerIdx + 1; r < rows.size(); r++) {
            List<RectangularTextContainer> row = rows.get(r);
            String txn  = cell(row, idxTxn);
            String post = cell(row, idxPost);
            String desc = cell(row, idxDesc);
            String cur  = cell(row, idxCurr);
            String amt  = cell(row, idxAmt);
            String twd  = cell(row, idxTwd);

            String merged = (txn + post + desc).replaceAll("\\s+", "");
            if (merged.isEmpty()) continue;
            if (desc.contains("本期費用明細") || desc.contains("卡號") || desc.contains("上期應繳金額")) continue;

            list.add(new StatementItem(
                    txn, post, desc, cur,
                    toAmount(amt), toAmount(twd)
            ));
        }
        return list;
    }

    private int findFirstCol(List<RectangularTextContainer> header, String... keys) {
        for (String k : keys) {
            int idx = findCol(header, k);
            if (idx >= 0) return idx;
        }
        return -1;
    }

    private int findCol(List<RectangularTextContainer> header, String key) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).getText().contains(key)) return i;
        }
        return -1;
    }

    private String cell(List<RectangularTextContainer> row, int idx) {
        if (idx < 0 || idx >= row.size()) return "";
        return row.get(idx).getText().trim();
    }

    private String joinRow(List<RectangularTextContainer> row) {
        StringBuilder sb = new StringBuilder();
        for (RectangularTextContainer c : row) sb.append(c.getText()).append('\t');
        return sb.toString();
    }

    private BigDecimal toAmount(String s) {
        if (s == null) return null;
        String t = s.replaceAll("[,\\s]", "");
        if (t.isEmpty() || "-".equals(t)) return null;
        try { return new BigDecimal(t); } catch (Exception e) { return null; }
    }
}
