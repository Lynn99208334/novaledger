package com.example.tableaudemov2.service;

import com.example.tableaudemov2.dto.ParseResult;
import com.example.tableaudemov2.dto.StatementItem;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import technology.tabula.*;

import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OcrPdfService {

    // ===== OCR 參數（依你的環境調整）=====
    private static final String TESSDATA_PATH = "/opt/tesseract/tessdata";
    private static final String TESS_LANG = "chi_tra+eng";
    private static final int OCR_DPI = 300;

    // 版面重建容差
    private static final int LINE_MERGE_TOLERANCE_PX = 10;   // 同一行的 y 容差
    private static final int WORD_GAP_IS_COLUMN_BOUND_PX = 40; // 相鄰字超過此距視為斷欄

    /** 與既有 Controller 相容的入口 */
    public ParseResult parseCreditCardStatement(InputStream pdfIn) throws Exception {
        return parseStatementTableOnly(pdfIn);
    }

    /** 僅回表格資料；同時回傳 rawTableText 供前端預覽（一定不會空） */
    public ParseResult parseStatementTableOnly(InputStream pdfIn) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfIn)) {
            ExtractOut out = extractTablesWithPreview(doc);

            // 若完全抽不到資料，啟動保底與 OCR
            String plain = safeExtractText(doc).replaceAll("\\s+", "");
            boolean probablyImageOnly = plain.length() < 30;

            if (out.items.isEmpty()) {
                if (probablyImageOnly) {
                    out.previewLines.add("[diagnostic] 幾乎沒有文字層，啟動 OCR（Tesseract）…");
                    ocrAndRebuildTables(doc, out); // 進階：以 OCR 重建「行/欄」
                } else {
                    out.previewLines.add("[fallback] 下列為 PDF 文字層抽取（非表格）：");
                    out.previewLines.add(safeExtractText(doc));
                }
            }

            if (out.previewLines.isEmpty()) {
                out.previewLines.add("[diagnostic] 無法抽取任何內容，可能是掃描影像且 OCR 失敗。");
            }

            return new ParseResult(
                    "玉山銀行",
                    "",
                    out.items,
                    "",
                    String.join("\n", out.previewLines)
            );
        }
    }

    /* ===================== Tabula 表格抽取（含預覽） ===================== */

    private static class ExtractOut {
        final List<StatementItem> items = new ArrayList<>();
        final List<String> previewLines = new ArrayList<>();
        void appendTablePreview(Table t) {
            for (List<RectangularTextContainer> row : t.getRows()) {
                previewLines.add(joinRow(row));
            }
        }
    }

    private ExtractOut extractTablesWithPreview(PDDocument doc) {
        ExtractOut out = new ExtractOut();
        ObjectExtractor oe = new ObjectExtractor(doc);

        for (int p = 1; p <= doc.getNumberOfPages(); p++) {
            Page page = oe.extract(p);

            List<Rectangle> areas = new ArrayList<>();
            float w = (float) page.getWidth();
            float h = (float) page.getHeight();
            areas.add(new Rectangle(0f, h * 0.30f, w, h * 0.65f));
            areas.add(new Rectangle(0f, h * 0.15f, w, h * 0.80f));
            areas.add(new Rectangle(0f, 0f, w, h));

            boolean gotAny = false;
            for (Rectangle rect : areas) {
                Page area = page.getArea(rect);

                List<Table> tables = new SpreadsheetExtractionAlgorithm().extract(area);
                if (tables.isEmpty()) tables = new BasicExtractionAlgorithm().extract(area);

                if (tables.isEmpty()) {
                    Page scaled = scalePage(area, 1.6f);
                    List<Table> t2 = new SpreadsheetExtractionAlgorithm().extract(scaled);
                    if (t2.isEmpty()) t2 = new BasicExtractionAlgorithm().extract(scaled);
                    tables = t2;
                }

                if (!tables.isEmpty()) {
                    gotAny = true;
                    out.previewLines.add("[Page " + p + "] 表格偵測到 " + tables.size() + " 個區塊");
                    for (Table t : tables) {
                        out.appendTablePreview(t);
                        out.items.addAll(toItemsByHeader_Tabula(t));
                    }
                    break;
                }
            }
            if (!gotAny) out.previewLines.add("[Page " + p + "] 無偵測到表格");
        }
        return out;
    }

    private Page scalePage(Page src, float scale) {
        float w = (float) (src.getWidth() * scale);
        float h = (float) (src.getHeight() * scale);
        return src.getArea(new Rectangle(0f, 0f, w, h));
    }

    /** Tabula 路徑：用你原本的表頭比對邏輯 */
    private List<StatementItem> toItemsByHeader_Tabula(Table tb) {
        List<StatementItem> list = new ArrayList<>();
        List<List<RectangularTextContainer>> rows = tb.getRows();
        if (rows.isEmpty()) return list;

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

        List<RectangularTextContainer> header = rows.get(headerIdx);
        int idxTxn  = findCol(header, "交易日期");
        int idxPost = findCol(header, "入帳日期");
        int idxDesc = findFirstCol(header, "交易項目", "交易說明", "交易項目/交易國家或地區");
        int idxCurr = findCol(header, "幣別");
        int idxAmt  = findFirstCol(header, "交易金額", "金額");
        int idxTwd  = findFirstCol(header, "新臺幣", "TWD");

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
                    txn, post, desc, cur, toAmount(amt), toAmount(twd)
            ));
        }
        return list;
    }

    /* ===================== OCR 路徑：以 Tess4J 重建行/欄 ===================== */

    private void ocrAndRebuildTables(PDDocument doc, ExtractOut out) throws Exception {
        PDFRenderer renderer = new PDFRenderer(doc);
        Tesseract t = new Tesseract();
        t.setDatapath(TESSDATA_PATH);
        t.setLanguage(TESS_LANG);
        t.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);

        for (int p = 0; p < doc.getNumberOfPages(); p++) {
            BufferedImage img = renderer.renderImageWithDPI(p, OCR_DPI);
            // 1) 預覽文字
            String pageText = t.doOCR(img);
            out.previewLines.add("[OCR Page " + (p + 1) + "]\n" + pageText.trim());

            // 2) 取字詞與座標
            List<Word> words = t.getWords(img, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            if (words == null || words.isEmpty()) continue;

            // 3) 依 y 分群為「行」
            List<List<Word>> lines = groupByLine(words, LINE_MERGE_TOLERANCE_PX);

            // 4) 將每行依 x 切欄 → 形成「近似表格」
            List<List<String>> tableRows = lines.stream()
                    .map(this::splitColumnsByGap)
                    .collect(Collectors.toList());

            // 5) 套用「表頭偵測 + 映射」產出 StatementItem
            out.items.addAll(toItemsByHeader_OCR(tableRows));
        }
    }

    // 依行聚類
    private List<List<Word>> groupByLine(List<Word> words, int tolerancePx) {
        List<Word> sorted = new ArrayList<>(words);
        sorted.removeIf(w -> w.getText() == null || w.getText().trim().isEmpty());
        sorted.sort(Comparator.comparingInt(w -> w.getBoundingBox().y));

        List<List<Word>> lines = new ArrayList<>();
        for (Word w : sorted) {
            boolean placed = false;
            for (List<Word> line : lines) {
                int y = line.get(0).getBoundingBox().y;
                if (Math.abs(w.getBoundingBox().y - y) <= tolerancePx) {
                    line.add(w); placed = true; break;
                }
            }
            if (!placed) {
                List<Word> nl = new ArrayList<>();
                nl.add(w);
                lines.add(nl);
            }
        }
        // 行內按 x 排序
        for (List<Word> line : lines) {
            line.sort(Comparator.comparingInt(a -> a.getBoundingBox().x));
        }
        // 去噪：太短的行丟掉（避免頁眉頁腳干擾）
        return lines.stream()
                .filter(l -> l.stream().map(Word::getText).collect(Collectors.joining()).trim().length() >= 2)
                .collect(Collectors.toList());
    }

    // 以相鄰字距突變切欄位
    private List<String> splitColumnsByGap(List<Word> line) {
        if (line.isEmpty()) return List.of();

        List<Integer> gaps = new ArrayList<>();
        for (int i = 1; i < line.size(); i++) {
            int prevRight = line.get(i - 1).getBoundingBox().x + line.get(i - 1).getBoundingBox().width;
            int curLeft = line.get(i).getBoundingBox().x;
            gaps.add(curLeft - prevRight);
        }

        List<List<Word>> columns = new ArrayList<>();
        List<Word> cur = new ArrayList<>();
        cur.add(line.get(0));
        for (int i = 1; i < line.size(); i++) {
            int gap = gaps.get(i - 1);
            if (gap >= WORD_GAP_IS_COLUMN_BOUND_PX) {
                columns.add(cur);
                cur = new ArrayList<>();
            }
            cur.add(line.get(i));
        }
        columns.add(cur);

        return columns.stream()
                .map(words -> words.stream().map(Word::getText).collect(Collectors.joining(" ")).trim())
                .collect(Collectors.toList());
    }

    // OCR 重建 rows → 套用表頭規則
    private List<StatementItem> toItemsByHeader_OCR(List<List<String>> rows) {
        List<StatementItem> list = new ArrayList<>();
        if (rows.isEmpty()) return list;

        // 將每行合併為 tab 分隔，沿用舊邏輯的偵測方式
        int headerIdx = -1;
        for (int i = 0; i < Math.min(rows.size(), 10); i++) {
            String line = String.join("\t", rows.get(i)).replaceAll("\\s+", "");
            if (line.contains("交易日期") && line.contains("入帳日期")
                    && (line.contains("交易項目") || line.contains("交易國家") || line.contains("交易說明"))
                    && line.contains("幣別")
                    && (line.contains("交易金額") || line.contains("金額"))
                    && (line.contains("新臺幣") || line.toUpperCase().contains("TWD"))) {
                headerIdx = i; break;
            }
        }
        if (headerIdx == -1) return list;

        List<String> header = rows.get(headerIdx);
        int idxTxn  = indexOfCol(header, "交易日期");
        int idxPost = indexOfCol(header, "入帳日期");
        int idxDesc = indexOfFirstCol(header, "交易項目", "交易說明", "交易項目/交易國家或地區");
        int idxCurr = indexOfCol(header, "幣別");
        int idxAmt  = indexOfFirstCol(header, "交易金額", "金額");
        int idxTwd  = indexOfFirstCol(header, "新臺幣", "TWD");

        for (int r = headerIdx + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            String txn  = safeCell(row, idxTxn);
            String post = safeCell(row, idxPost);
            String desc = safeCell(row, idxDesc);
            String cur  = safeCell(row, idxCurr);
            String amt  = safeCell(row, idxAmt);
            String twd  = safeCell(row, idxTwd);

            String merged = (txn + post + desc).replaceAll("\\s+", "");
            if (merged.isEmpty()) continue;
            if (desc.contains("本期費用明細") || desc.contains("卡號") || desc.contains("上期應繳金額")) continue;

            list.add(new StatementItem(
                    txn, post, desc, cur, toAmount(amt), toAmount(twd)
            ));
        }
        return list;
    }

    private int indexOfFirstCol(List<String> header, String... keys) {
        for (String k : keys) {
            int i = indexOfCol(header, k);
            if (i >= 0) return i;
        }
        return -1;
    }
    private int indexOfCol(List<String> header, String key) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i) != null && header.get(i).contains(key)) return i;
        }
        return -1;
    }
    private String safeCell(List<String> row, int idx) {
        if (idx < 0 || idx >= row.size()) return "";
        String s = row.get(idx);
        return s == null ? "" : s.trim();
    }

    /* ===================== 共用工具 ===================== */

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
    private static String joinRow(List<RectangularTextContainer> row) {
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
    private String safeExtractText(PDDocument doc) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(true);
            return stripper.getText(doc);
        } catch (Exception e) {
            return "";
        }
    }
}
