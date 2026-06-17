(function () {
    'use strict';

    var pieChart = null;
    var PIE_COLORS = ['#185FA5', '#BA7517', '#3B6D11'];

    // ── Flatpickr 初始化 ─────────────────────────────────────────
    var now = new Date();
    var defaultMonth = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');

    flatpickr('#monthPicker', {
        plugins: [new monthSelectPlugin({ shorthand: false, dateFormat: 'Y-m', altFormat: 'Y-m' })],
        defaultDate: defaultMonth,
        onChange: function (selectedDates, dateStr) {
            // 防止選到未來月份
            if (selectedDates[0] && selectedDates[0] > now) {
                this.setDate(defaultMonth, false);
            }
        }
    });

    // ── 事件綁定 ─────────────────────────────────────────────────
    document.addEventListener('click', function (e) {
        if (e.target && e.target.id === 'btnLoadReport') {
            loadReport();
        }
    });

    loadReport();

    // ── 主要載入 ─────────────────────────────────────────────────
    function loadReport() {
        var monthVal = document.getElementById('monthPicker').value;
        if (!monthVal) { alert('請選擇報表月份'); return; }

        showLoading(true);

        fetch('/api/report/monthly-summary?month=' + encodeURIComponent(monthVal), { credentials: 'same-origin' })
            .then(function (res) { return res.json(); })
            .then(function (json) {
                showLoading(false);
                if (!json.success || !json.data) { showNoData(); return; }
                render(json.data);
            })
            .catch(function (err) {
                showLoading(false);
                console.error('月報表載入失敗', err);
                showNoData();
            });
    }

    // ── 渲染 ─────────────────────────────────────────────────────
    function render(data) {
        hideNoData();
        renderSummary(data.summary);
        renderTypeGroups(data.typeGroups);
        renderPie(data.typeGroups);
        renderAccountTable(data.accountDetails);
        show('summaryCards');
        show('typeGroupCards');
        show('detailRow');
    }

    function renderSummary(s) {
        setText('totalAsset', '$' + fmt(s.totalAssetTwd));
        setText('totalAssetNote', '上月 $' + fmt(s.lastMonthTotalAssetTwd));

        var netEl = document.getElementById('netChange');
        var netPos = s.netChangeTwd >= 0;
        netEl.textContent = (netPos ? '+' : '') + '$' + fmt(s.netChangeTwd);
        netEl.className = 'mb-1 ' + (netPos ? 'text-success' : 'text-danger');

        var detailEl = document.getElementById('netChangeDetail');
        detailEl.textContent = (netPos ? '+' : '') + s.netChangePercent + '% 較上月';
        detailEl.className = 'fw-semibold small ' + (netPos ? 'text-success' : 'text-danger');

        setText('totalIncome', '+$' + fmt(s.totalIncomeTwd));
        setText('totalExpense', '-$' + fmt(s.totalExpenseTwd));

        var netIncomeEl = document.getElementById('netIncome');
        var incPos = s.netIncomeTwd >= 0;
        netIncomeEl.textContent = (incPos ? '+' : '') + '$' + fmt(s.netIncomeTwd);
        netIncomeEl.className = 'small fw-semibold ' + (incPos ? 'text-success' : 'text-danger');
    }

    function renderTypeGroups(groups) {
        var icons = { BANK: 'fas fa-university', INVESTMENT: 'fas fa-chart-line', CASH: 'fas fa-wallet' };
        var badgeColors = { BANK: 'bg-primary', INVESTMENT: 'bg-warning text-dark', CASH: 'bg-success' };
        var container = document.getElementById('typeGroupCards');
        container.innerHTML = '';
        groups.forEach(function (g) {
            var col = document.createElement('div');
            col.className = 'col-md-4';
            col.innerHTML =
                '<div class="card h-100"><div class="card-body">' +
                '<div class="d-flex align-items-center gap-2 mb-2">' +
                '<i class="' + (icons[g.type] || 'fas fa-circle') + ' text-secondary"></i>' +
                '<span class="small text-muted">' + g.displayName + '</span>' +
                '</div>' +
                '<h5 class="mb-1">$' + fmt(g.totalTwd) + '</h5>' +
                '<span class="badge ' + (badgeColors[g.type] || 'bg-secondary') + '">' + g.percent + '%</span>' +
                '</div></div>';
            container.appendChild(col);
        });
    }

    function renderPie(groups) {
        var canvas = document.getElementById('pieChart');
        if (pieChart) { pieChart.destroy(); }
        pieChart = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: groups.map(function (g) { return g.displayName; }),
                datasets: [{
                    data: groups.map(function (g) { return g.totalTwd; }),
                    backgroundColor: PIE_COLORS,
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                return ctx.label + ': $' + fmt(ctx.raw) + ' (' + groups[ctx.dataIndex].percent + '%)';
                            }
                        }
                    }
                }
            }
        });

        var legend = document.getElementById('pieLegend');
        legend.innerHTML = groups.map(function (g, i) {
            return '<div class="d-flex align-items-center gap-2 mb-1">' +
                '<span style="width:12px;height:12px;border-radius:2px;background:' + PIE_COLORS[i] + ';flex-shrink:0"></span>' +
                '<span class="small text-muted">' + g.displayName + ' ' + g.percent + '%</span>' +
                '</div>';
        }).join('');
    }

    function renderAccountTable(details) {
        var tbody = document.getElementById('accountTableBody');
        tbody.innerHTML = '';
        details.forEach(function (d) {
            var isNonTwd = d.currencyCode !== 'TWD';
            var changePos = d.changeTwd >= 0;
            var changeStr = (changePos ? '+' : '') + (isNonTwd
                ? fmt(d.changeOriginal) + ' ' + d.currencyCode
                : '$' + fmt(d.changeOriginal));
            var pctStr = (changePos ? '+' : '') + d.changePercent + '%';

            var tr = document.createElement('tr');
            tr.innerHTML =
                '<td>' +
                '<div class="fw-semibold small">' + esc(d.name) + '</div>' +
                '<div class="text-muted" style="font-size:11px">' + d.accountTypeDisplay + ' · ' + d.currencyCode + '</div>' +
                '</td>' +
                '<td class="text-end small">' +
                (isNonTwd ? fmt(d.balanceOriginal) + ' ' + d.currencyCode : '$' + fmt(d.balanceOriginal)) +
                '</td>' +
                '<td class="text-end small">$' + fmt(d.balanceTwd) + '</td>' +
                '<td class="text-end small ' + (changePos ? 'text-success' : 'text-danger') + '">' +
                changeStr + '<br><span style="font-size:11px">' + pctStr + '</span>' +
                '</td>';
            tbody.appendChild(tr);
        });
    }

    // ── 工具 ─────────────────────────────────────────────────────
    function fmt(num) {
        if (num == null) return '—';
        return Number(num).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
    }

    function setText(id, text) {
        document.getElementById(id).textContent = text;
    }

    function esc(str) {
        var d = document.createElement('div');
        d.textContent = str;
        return d.innerHTML;
    }

    function show(id) {
        document.getElementById(id).style.removeProperty('display');
    }

    function showLoading(on) {
        document.getElementById('loadingSpinner').classList.toggle('d-none', !on);
        document.getElementById('btnLoadReport').disabled = on;
    }

    function showNoData() {
        document.getElementById('noDataMsg').classList.remove('d-none');
        ['summaryCards', 'typeGroupCards', 'detailRow'].forEach(function (id) {
            document.getElementById(id).style.display = 'none';
        });
    }

    function hideNoData() {
        document.getElementById('noDataMsg').classList.add('d-none');
    }
})();
