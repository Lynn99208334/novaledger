/**
 * Select2Helper — 統一 Select2 初始化工具（全域共用）
 *
 * 使用方式：
 *   Select2Helper.init('#mySelect')           // 基本下拉，無搜尋框
 *   Select2Helper.initSearchable('#mySelect') // 帶搜尋框
 *   Select2Helper.clear('#mySelect')          // 清除並 trigger change
 *   Select2Helper.getValue('#mySelect')       // 取得目前值
 *   Select2Helper.onChange('#mySelect', fn)   // 監聽 change
 *
 * 前提：頁面需已載入 jQuery 與 select2.min.js（由 admin-base.html 統一引入）
 */
const Select2Helper = (() => {

    const BASE_OPTIONS = {
        width: '100%',
        language: {
            noResults: () => '找不到符合的選項'
        }
    };

    function init(selector, options = {}) {
        $(selector).select2({
            ...BASE_OPTIONS,
            minimumResultsForSearch: Infinity,
            ...options
        });
    }

    function initSearchable(selector, options = {}) {
        $(selector).select2({
            ...BASE_OPTIONS,
            ...options
        });
    }

    function clear(selector) {
        $(selector).val('').trigger('change');
    }

    function getValue(selector) {
        return $(selector).val();
    }

    function onChange(selector, callback) {
        $(selector).on('change', callback);
    }

    return { init, initSearchable, clear, getValue, onChange };
})();
