/**
 * GoalTracker Pro — Chart.js Grafik Entegrasyonu (Faz 4)
 *
 * Bileşenler:
 *  - Dashboard Overview Doughnut
 *  - PlannedVsActualChart (Line)
 *  - DailyBarChart (Bar)
 *  - CompletionDonut (Doughnut)
 *  - ActivityHeatmap (CSS Grid)
 */

/* ========== Renk Sabitleri (Dark Theme) ========== */
const COLORS = {
    planned:  '#6366F1',   // indigo — accent-indigo
    actual:   '#10B981',   // emerald — accent-emerald
    behind:   '#EF4444',   // red — accent-red
    ahead:    '#10B981',   // emerald
    onTrack:  '#00D4FF',   // cyan — accent-cyan
    streak:   '#F59E0B',   // amber — accent-amber
    neutral:  '#475569',   // text-muted
    gold:     '#F59E0B',   // amber
};

const HEATMAP_COLORS = {
    empty:    '#1E293B',   // slate-800 (dark mode)
    level1:   '#064E3B',   // emerald-900
    level2:   '#059669',   // emerald-600
    level3:   '#10B981',   // emerald-500
    level4:   '#34D399',   // emerald-400
};

/* ========== Yardımcı Fonksiyonlar ========== */

function getDonutColor(pct) {
    if (pct >= 100) return COLORS.gold;
    if (pct >= 75)  return COLORS.actual;
    if (pct >= 50)  return COLORS.planned;
    if (pct >= 25)  return '#F97316'; // turuncu
    return COLORS.behind;
}

function getBarColor(dailyActual, dailyTarget) {
    return dailyActual >= dailyTarget ? COLORS.actual : COLORS.behind;
}

function getHeatmapIntensity(dailyActual, dailyTarget) {
    if (dailyActual <= 0) return 0;
    var ratio = dailyActual / dailyTarget;
    if (ratio < 0.5)  return 1;
    if (ratio < 1.0)  return 2;
    if (ratio <= 1.5) return 3;
    return 4;
}

function getHeatmapColor(level) {
    switch (level) {
        case 1: return HEATMAP_COLORS.level1;
        case 2: return HEATMAP_COLORS.level2;
        case 3: return HEATMAP_COLORS.level3;
        case 4: return HEATMAP_COLORS.level4;
        default: return HEATMAP_COLORS.empty;
    }
}

function formatChartDate(dateStr, index, total) {
    if (total <= 14) return dateStr;
    if (total <= 60) return index % 7 === 0 ? dateStr : '';
    return index % 30 === 0 ? dateStr : '';
}

/* ========== Dashboard Overview Doughnut ========== */

function initDashboardOverviewChart() {
    var canvas = document.getElementById('dashboardOverviewChart');
    if (!canvas) return;

    var onTrack = parseInt(canvas.dataset.onTrack) || 0;
    var behind = parseInt(canvas.dataset.behind) || 0;

    if (onTrack === 0 && behind === 0) {
        // Boş durum — gri halka
        onTrack = 0;
        behind = 0;
    }

    var total = onTrack + behind;
    var data = total > 0 ? [onTrack, behind] : [1];
    var bgColors = total > 0 ? [COLORS.actual, COLORS.behind] : ['#1E293B'];
    var labels = total > 0 ? ['Yolunda', 'Geride'] : ['Hedef yok'];

    new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: bgColors,
                borderWidth: 2,
                borderColor: '#0A0A0F'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '70%',
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(ctx) {
                            if (total === 0) return 'Henüz hedef yok';
                            return ctx.label + ': ' + ctx.parsed + ' hedef';
                        }
                    }
                }
            }
        },
        plugins: [{
            id: 'centerText',
            afterDraw: function(chart) {
                var ctx = chart.ctx;
                var width = chart.width;
                var height = chart.height;
                ctx.save();
                ctx.font = 'bold 24px "JetBrains Mono", monospace';
                ctx.fillStyle = '#F8FAFC';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                if (total > 0) {
                    var pct = Math.round((onTrack / total) * 100);
                    ctx.fillText(pct + '%', width / 2, height / 2 - 8);
                    ctx.font = '12px "DM Sans", sans-serif';
                    ctx.fillStyle = '#94A3B8';
                    ctx.fillText('Yolunda', width / 2, height / 2 + 14);
                } else {
                    ctx.font = '14px "DM Sans", sans-serif';
                    ctx.fillStyle = '#475569';
                    ctx.fillText('—', width / 2, height / 2);
                }
                ctx.restore();
            }
        }]
    });
}

/* ========== Planned vs Actual Line Chart ========== */

function initPlannedVsActualChart() {
    var canvas = document.getElementById('plannedVsActualChart');
    if (!canvas) return;

    var goalId = canvas.dataset.goalId;
    if (!goalId) return;

    // Show loading state
    var container = canvas.parentElement;
    var loadingEl = document.createElement('div');
    loadingEl.className = 'text-center py-4 chart-loading';
    loadingEl.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Yükleniyor...</span></div>';
    container.insertBefore(loadingEl, canvas);
    canvas.style.display = 'none';

    fetch('/api/goals/' + goalId + '/chart-data', {
        headers: { 'Accept': 'application/json' }
    })
    .then(function(res) { return res.json(); })
    .then(function(response) {
        var loadingEls = container.querySelectorAll('.chart-loading');
        loadingEls.forEach(function(el) { el.remove(); });
        canvas.style.display = '';

        if (!response.success || !response.data || !response.data.dataPoints || response.data.dataPoints.length === 0) {
            canvas.style.display = 'none';
            var emptyEl = document.createElement('div');
            emptyEl.className = 'text-center py-4 text-muted';
            emptyEl.innerHTML = '<i class="bi bi-bar-chart display-4"></i><p class="mt-2">Henüz grafik verisi yok.</p>';
            container.appendChild(emptyEl);
            return;
        }

        var dataPoints = response.data.dataPoints;
        var dailyTarget = response.data.dailyTarget;
        var total = dataPoints.length;

        var labels = dataPoints.map(function(dp, i) { return formatChartDate(dp.date, i, total); });
        var plannedData = dataPoints.map(function(dp) { return dp.planned; });
        var actualData = dataPoints.map(function(dp) { return dp.actual; });

        new Chart(canvas, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Planlanan',
                        data: plannedData,
                        borderColor: COLORS.planned,
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        borderDash: [6, 3],
                        borderWidth: 2,
                        pointRadius: total > 60 ? 0 : 3,
                        fill: false,
                        tension: 0.1
                    },
                    {
                        label: 'Gerçekleşen',
                        data: actualData,
                        borderColor: COLORS.actual,
                        backgroundColor: 'rgba(16, 185, 129, 0.1)',
                        borderWidth: 2,
                        pointRadius: total > 60 ? 0 : 3,
                        fill: false,
                        tension: 0.1,
                        spanGaps: false
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { intersect: false, mode: 'index' },
                plugins: {
                    legend: { position: 'top' },
                    tooltip: {
                        callbacks: {
                            title: function(items) {
                                var idx = items[0].dataIndex;
                                return dataPoints[idx].date;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        ticks: {
                            maxTicksLimit: 15,
                            maxRotation: 45,
                            callback: function(value, index) {
                                return formatChartDate(dataPoints[index].date, index, total);
                            }
                        }
                    },
                    y: {
                        beginAtZero: true,
                        title: { display: true, text: 'Kümülatif Değer' }
                    }
                }
            }
        });
    })
    .catch(function(err) {
        var loadingEls = container.querySelectorAll('.chart-loading');
        loadingEls.forEach(function(el) { el.remove(); });
        canvas.style.display = 'none';
        var errorEl = document.createElement('div');
        errorEl.className = 'text-center py-4';
        errorEl.innerHTML = '<p class="text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Grafik yüklenemedi.</p>' +
            '<button class="btn btn-outline-primary btn-sm" onclick="location.reload()"><i class="bi bi-arrow-clockwise me-1"></i>Tekrar Dene</button>';
        container.appendChild(errorEl);
    });
}

/* ========== Daily Bar Chart ========== */

function initDailyBarChart() {
    var canvas = document.getElementById('dailyBarChart');
    if (!canvas) return;

    var goalId = canvas.dataset.goalId;
    if (!goalId) return;

    var container = canvas.parentElement;
    var loadingEl = document.createElement('div');
    loadingEl.className = 'text-center py-4 chart-loading';
    loadingEl.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Yükleniyor...</span></div>';
    container.insertBefore(loadingEl, canvas);
    canvas.style.display = 'none';

    fetch('/api/goals/' + goalId + '/chart-data', {
        headers: { 'Accept': 'application/json' }
    })
    .then(function(res) { return res.json(); })
    .then(function(response) {
        var loadingEls = container.querySelectorAll('.chart-loading');
        loadingEls.forEach(function(el) { el.remove(); });
        canvas.style.display = '';

        if (!response.success || !response.data || !response.data.dataPoints || response.data.dataPoints.length === 0) {
            canvas.style.display = 'none';
            var emptyEl = document.createElement('div');
            emptyEl.className = 'text-center py-4 text-muted';
            emptyEl.innerHTML = '<i class="bi bi-bar-chart display-4"></i><p class="mt-2">Henüz günlük veri yok.</p>';
            container.appendChild(emptyEl);
            return;
        }

        var dataPoints = response.data.dataPoints;
        var dailyTarget = parseFloat(response.data.dailyTarget) || 0;
        var total = dataPoints.length;

        // Filter to only days that have entries
        var filteredPoints = dataPoints.filter(function(dp) { return dp.dailyActual !== null; });
        if (filteredPoints.length === 0) {
            canvas.style.display = 'none';
            var emptyEl = document.createElement('div');
            emptyEl.className = 'text-center py-4 text-muted';
            emptyEl.innerHTML = '<i class="bi bi-bar-chart display-4"></i><p class="mt-2">Henüz günlük veri yok.</p>';
            container.appendChild(emptyEl);
            return;
        }

        var labels = filteredPoints.map(function(dp) { return dp.date; });
        var barData = filteredPoints.map(function(dp) { return dp.dailyActual || 0; });
        var barColors = filteredPoints.map(function(dp) {
            return getBarColor(dp.dailyActual || 0, dailyTarget);
        });

        new Chart(canvas, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Günlük Değer',
                    data: barData,
                    backgroundColor: barColors,
                    borderRadius: 4,
                    borderSkipped: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            afterLabel: function(ctx) {
                                return 'Hedef: ' + dailyTarget.toFixed(2);
                            }
                        }
                    },
                    annotation: dailyTarget > 0 ? {
                        annotations: {
                            targetLine: {
                                type: 'line',
                                yMin: dailyTarget,
                                yMax: dailyTarget,
                                borderColor: COLORS.planned,
                                borderWidth: 2,
                                borderDash: [4, 4],
                                label: {
                                    display: true,
                                    content: 'Hedef: ' + dailyTarget.toFixed(2),
                                    position: 'end'
                                }
                            }
                        }
                    } : {}
                },
                scales: {
                    x: {
                        ticks: { maxTicksLimit: 15, maxRotation: 45 }
                    },
                    y: {
                        beginAtZero: true,
                        title: { display: true, text: 'Günlük Değer' }
                    }
                }
            }
        });

        // Draw reference line manually if no annotation plugin
        // The annotation plugin is separate, so we draw the line in afterDraw
    })
    .catch(function(err) {
        var loadingEls = container.querySelectorAll('.chart-loading');
        loadingEls.forEach(function(el) { el.remove(); });
        canvas.style.display = 'none';
        var errorEl = document.createElement('div');
        errorEl.className = 'text-center py-4';
        errorEl.innerHTML = '<p class="text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Grafik yüklenemedi.</p>' +
            '<button class="btn btn-outline-primary btn-sm" onclick="location.reload()"><i class="bi bi-arrow-clockwise me-1"></i>Tekrar Dene</button>';
        container.appendChild(errorEl);
    });
}

/* ========== Completion Donut ========== */

function initCompletionDonut() {
    var canvas = document.getElementById('completionDonutChart');
    if (!canvas) return;

    var pct = parseFloat(canvas.dataset.completionPct) || 0;
    var remaining = Math.max(0, 100 - pct);
    var donutColor = getDonutColor(pct);

    new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: ['Tamamlanan', 'Kalan'],
            datasets: [{
                data: [pct, remaining],
                backgroundColor: [donutColor, '#1E293B'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '75%',
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(ctx) {
                            return ctx.label + ': ' + ctx.parsed.toFixed(1) + '%';
                        }
                    }
                }
            }
        },
        plugins: [{
            id: 'centerTextDonut',
            afterDraw: function(chart) {
                var ctx = chart.ctx;
                var width = chart.width;
                var height = chart.height;
                ctx.save();
                ctx.font = 'bold 20px "JetBrains Mono", monospace';
                ctx.fillStyle = '#F8FAFC';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillText(pct.toFixed(1) + '%', width / 2, height / 2);
                ctx.restore();
            }
        }]
    });
}

/* ========== Activity Heatmap (CSS Grid) ========== */

function initActivityHeatmap() {
    var container = document.getElementById('activityHeatmap');
    if (!container) return;

    var goalId = container.dataset.goalId;
    var dailyTarget = parseFloat(container.dataset.dailyTarget) || 1;
    if (!goalId) return;

    container.innerHTML = '<div class="text-center py-3"><div class="spinner-border spinner-border-sm text-primary" role="status"></div></div>';

    fetch('/api/goals/' + goalId + '/chart-data', {
        headers: { 'Accept': 'application/json' }
    })
    .then(function(res) { return res.json(); })
    .then(function(response) {
        if (!response.success || !response.data || !response.data.dataPoints || response.data.dataPoints.length === 0) {
            container.innerHTML = '<p class="text-muted text-center py-3 mb-0">Henüz heatmap verisi yok.</p>';
            return;
        }

        var dataPoints = response.data.dataPoints;
        dailyTarget = parseFloat(response.data.dailyTarget) || dailyTarget;

        // Build date → value map
        var dateMap = {};
        dataPoints.forEach(function(dp) {
            dateMap[dp.date] = dp.dailyActual || 0;
        });

        // Determine range: last 52 weeks (or 26 on mobile)
        var isMobile = window.innerWidth < 640;
        var weeks = isMobile ? 26 : 52;
        var today = new Date();
        var startDate = new Date(today);
        startDate.setDate(startDate.getDate() - (weeks * 7 - 1));

        // Adjust start to Monday
        var dayOfWeek = startDate.getDay();
        var mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
        startDate.setDate(startDate.getDate() + mondayOffset);

        // Build grid
        var html = '<div class="heatmap-wrapper" style="overflow-x:auto;">';
        html += '<div class="heatmap-grid" style="display:inline-grid; grid-template-columns: 30px repeat(' + weeks + ', 14px); grid-template-rows: repeat(7, 14px); gap:2px; font-size:10px;">';

        // Day labels (column 0)
        var dayLabels = ['Pzt', '', 'Çrş', '', 'Cmt', '', 'Paz'];
        for (var d = 0; d < 7; d++) {
            html += '<div style="grid-column:1; grid-row:' + (d + 1) + '; display:flex; align-items:center; color:#94A3B8;">' + dayLabels[d] + '</div>';
        }

        // Cells
        var currentDate = new Date(startDate);
        for (var w = 0; w < weeks; w++) {
            for (var dow = 0; dow < 7; dow++) {
                var dateStr = currentDate.toISOString().split('T')[0];
                var value = dateMap[dateStr] || 0;
                var level = getHeatmapIntensity(value, dailyTarget);
                var color = getHeatmapColor(level);
                var col = w + 2; // offset for label column
                var row = dow + 1;

                var pctText = dailyTarget > 0 ? Math.round((value / dailyTarget) * 100) + '%' : '0%';
                var ariaLabel = dateStr + ': ' + value + ' (' + pctText + ')';

                html += '<div style="grid-column:' + col + '; grid-row:' + row + '; width:12px; height:12px; border-radius:2px; background:' + color + ';" ' +
                    'title="' + ariaLabel + '" ' +
                    'aria-label="' + ariaLabel + '" ' +
                    'tabindex="0" role="gridcell"></div>';

                currentDate.setDate(currentDate.getDate() + 1);
            }
        }

        html += '</div></div>';
        container.innerHTML = html;

        // Keyboard navigation for heatmap
        var cells = container.querySelectorAll('[role="gridcell"]');
        cells.forEach(function(cell, index) {
            cell.addEventListener('keydown', function(e) {
                var newIndex = index;
                if (e.key === 'ArrowRight') newIndex = Math.min(index + 7, cells.length - 1);
                else if (e.key === 'ArrowLeft') newIndex = Math.max(index - 7, 0);
                else if (e.key === 'ArrowDown') newIndex = Math.min(index + 1, cells.length - 1);
                else if (e.key === 'ArrowUp') newIndex = Math.max(index - 1, 0);
                else return;
                e.preventDefault();
                cells[newIndex].focus();
            });
        });
    })
    .catch(function(err) {
        container.innerHTML = '<p class="text-danger text-center py-3 mb-0"><i class="bi bi-exclamation-triangle me-1"></i>Heatmap yüklenemedi.</p>';
    });
}

/* ========== Initialization ========== */

document.addEventListener('DOMContentLoaded', function() {
    // Dashboard page charts
    initDashboardOverviewChart();

    // Goal detail page charts
    initPlannedVsActualChart();
    initDailyBarChart();
    initCompletionDonut();
    initActivityHeatmap();

    // Tab switching: re-init charts when tab is shown
    var tabEls = document.querySelectorAll('button[data-bs-toggle="tab"], a[data-bs-toggle="tab"]');
    tabEls.forEach(function(tabEl) {
        tabEl.addEventListener('shown.bs.tab', function(event) {
            // Update URL with tab
            var tabId = event.target.getAttribute('data-bs-target') || event.target.getAttribute('href');
            if (tabId) {
                var tabName = tabId.replace('#', '');
                var url = new URL(window.location);
                url.searchParams.set('tab', tabName);
                history.replaceState(null, '', url);
            }

            // Charts may need resize after tab switch
            setTimeout(function() {
                window.dispatchEvent(new Event('resize'));
            }, 100);
        });
    });

    // Restore tab from URL
    var urlParams = new URLSearchParams(window.location.search);
    var activeTab = urlParams.get('tab');
    if (activeTab) {
        var tabTrigger = document.querySelector('[data-bs-target="#' + activeTab + '"], [href="#' + activeTab + '"]');
        if (tabTrigger) {
            var tab = new bootstrap.Tab(tabTrigger);
            tab.show();
        }
    }
});

