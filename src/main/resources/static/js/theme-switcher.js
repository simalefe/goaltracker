/**
 * GoalTracker Pro — Theme Switcher
 *
 * Modern çoklu tema desteği.
 * Tema seçimi localStorage'da saklanır ve sayfa yüklenirken anında uygulanır.
 *
 * Desteklenen temalar:
 *  - obsidian   (varsayılan — koyu siyah)
 *  - midnight   (koyu mavi)
 *  - aurora     (koyu yeşil / orman)
 *  - nebula     (koyu mor)
 *  - ember      (koyu kırmızı / volkanik)
 *  - arctic     (koyu gri-mavi / buz)
 */

(function () {
    'use strict';

    var STORAGE_KEY = 'goaltracker-theme';
    var DEFAULT_THEME = 'obsidian';
    var THEME_ATTR = 'data-theme';

    var THEMES = {
        obsidian: {
            label: 'Obsidian',
            description: 'Varsayılan koyu tema',
            icon: 'bi-moon-stars-fill',
            preview: {
                bg: '#0A0A0F',
                card: '#12121A',
                accent: '#00D4FF'
            },
            vars: {}  // varsayılan — override gerekmez
        },
        midnight: {
            label: 'Midnight Blue',
            description: 'Derin gece mavisi',
            icon: 'bi-cloud-moon-fill',
            preview: {
                bg: '#0B1120',
                card: '#111B2E',
                accent: '#3B82F6'
            },
            vars: {
                '--bg-primary':       '#0B1120',
                '--bg-secondary':     '#111B2E',
                '--bg-card':          'rgba(59, 130, 246, 0.04)',
                '--bg-card-hover':    'rgba(59, 130, 246, 0.08)',
                '--bg-glass':         'rgba(59, 130, 246, 0.04)',
                '--border':           'rgba(59, 130, 246, 0.12)',
                '--border-hover':     'rgba(59, 130, 246, 0.22)',
                '--accent-cyan':      '#3B82F6',
                '--accent-indigo':    '#6366F1',
                '--glow-cyan':        '0 0 20px rgba(59, 130, 246, 0.15)',
                '--glow-indigo':      '0 0 20px rgba(99, 102, 241, 0.15)'
            }
        },
        aurora: {
            label: 'Aurora',
            description: 'Kuzey ışıkları orman yeşili',
            icon: 'bi-tree-fill',
            preview: {
                bg: '#0A1510',
                card: '#0F1F18',
                accent: '#34D399'
            },
            vars: {
                '--bg-primary':       '#0A1510',
                '--bg-secondary':     '#0F1F18',
                '--bg-card':          'rgba(16, 185, 129, 0.04)',
                '--bg-card-hover':    'rgba(16, 185, 129, 0.08)',
                '--bg-glass':         'rgba(16, 185, 129, 0.04)',
                '--border':           'rgba(16, 185, 129, 0.12)',
                '--border-hover':     'rgba(16, 185, 129, 0.22)',
                '--accent-cyan':      '#34D399',
                '--accent-indigo':    '#10B981',
                '--glow-cyan':        '0 0 20px rgba(52, 211, 153, 0.15)',
                '--glow-indigo':      '0 0 20px rgba(16, 185, 129, 0.15)'
            }
        },
        nebula: {
            label: 'Nebula',
            description: 'Kozmik mor galaksi',
            icon: 'bi-stars',
            preview: {
                bg: '#0F0A1A',
                card: '#16102A',
                accent: '#A78BFA'
            },
            vars: {
                '--bg-primary':       '#0F0A1A',
                '--bg-secondary':     '#16102A',
                '--bg-card':          'rgba(139, 92, 246, 0.04)',
                '--bg-card-hover':    'rgba(139, 92, 246, 0.08)',
                '--bg-glass':         'rgba(139, 92, 246, 0.04)',
                '--border':           'rgba(139, 92, 246, 0.12)',
                '--border-hover':     'rgba(139, 92, 246, 0.22)',
                '--accent-cyan':      '#A78BFA',
                '--accent-indigo':    '#8B5CF6',
                '--glow-cyan':        '0 0 20px rgba(167, 139, 250, 0.15)',
                '--glow-indigo':      '0 0 20px rgba(139, 92, 246, 0.15)'
            }
        },
        ember: {
            label: 'Ember',
            description: 'Volkanik kırmızı sıcaklık',
            icon: 'bi-fire',
            preview: {
                bg: '#150A0A',
                card: '#1F1010',
                accent: '#F87171'
            },
            vars: {
                '--bg-primary':       '#150A0A',
                '--bg-secondary':     '#1F1010',
                '--bg-card':          'rgba(239, 68, 68, 0.04)',
                '--bg-card-hover':    'rgba(239, 68, 68, 0.08)',
                '--bg-glass':         'rgba(239, 68, 68, 0.04)',
                '--border':           'rgba(239, 68, 68, 0.12)',
                '--border-hover':     'rgba(239, 68, 68, 0.22)',
                '--accent-cyan':      '#F87171',
                '--accent-indigo':    '#EF4444',
                '--glow-cyan':        '0 0 20px rgba(248, 113, 113, 0.15)',
                '--glow-indigo':      '0 0 20px rgba(239, 68, 68, 0.15)'
            }
        },
        arctic: {
            label: 'Arctic',
            description: 'Buz mavisi soğuk tonlar',
            icon: 'bi-snow2',
            preview: {
                bg: '#0A1015',
                card: '#101820',
                accent: '#67E8F9'
            },
            vars: {
                '--bg-primary':       '#0A1015',
                '--bg-secondary':     '#101820',
                '--bg-card':          'rgba(103, 232, 249, 0.04)',
                '--bg-card-hover':    'rgba(103, 232, 249, 0.08)',
                '--bg-glass':         'rgba(103, 232, 249, 0.04)',
                '--border':           'rgba(103, 232, 249, 0.10)',
                '--border-hover':     'rgba(103, 232, 249, 0.20)',
                '--accent-cyan':      '#67E8F9',
                '--accent-indigo':    '#22D3EE',
                '--glow-cyan':        '0 0 20px rgba(103, 232, 249, 0.15)',
                '--glow-indigo':      '0 0 20px rgba(34, 211, 238, 0.15)'
            }
        }
    };

    /**
     * Kaydedilmiş temayı al veya varsayılana dön.
     */
    function getSavedTheme() {
        try {
            var saved = localStorage.getItem(STORAGE_KEY);
            return (saved && THEMES[saved]) ? saved : DEFAULT_THEME;
        } catch (e) {
            return DEFAULT_THEME;
        }
    }

    /**
     * CSS custom property'lerini document root'una uygula.
     */
    function applyTheme(themeName) {
        var theme = THEMES[themeName];
        if (!theme) {
            themeName = DEFAULT_THEME;
            theme = THEMES[DEFAULT_THEME];
        }

        var root = document.documentElement;

        // Önce varsayılan temadaki tüm override'ları temizle
        Object.keys(THEMES).forEach(function (key) {
            var t = THEMES[key];
            if (t.vars) {
                Object.keys(t.vars).forEach(function (prop) {
                    root.style.removeProperty(prop);
                });
            }
        });

        // Seçilen temanın değişkenlerini uygula
        if (theme.vars) {
            Object.keys(theme.vars).forEach(function (prop) {
                root.style.setProperty(prop, theme.vars[prop]);
            });
        }

        // data-theme attribute'unu güncelle
        root.setAttribute(THEME_ATTR, themeName);

        // localStorage'a kaydet
        try {
            localStorage.setItem(STORAGE_KEY, themeName);
        } catch (e) {
            // localStorage kullanılamıyor — sessizce devam et
        }

        // Aktif tema kartını güncelle (eğer ayar sayfasındaysa)
        updateThemeCards(themeName);
    }

    /**
     * Tema kartlarının aktif durumunu güncelle.
     */
    function updateThemeCards(activeTheme) {
        var cards = document.querySelectorAll('[data-theme-select]');
        cards.forEach(function (card) {
            var cardTheme = card.getAttribute('data-theme-select');
            if (cardTheme === activeTheme) {
                card.classList.add('theme-card--active');
                card.setAttribute('aria-pressed', 'true');
            } else {
                card.classList.remove('theme-card--active');
                card.setAttribute('aria-pressed', 'false');
            }
        });
    }

    /**
     * Tema kartı tıklama olaylarını bağla.
     */
    function bindThemeCards() {
        var cards = document.querySelectorAll('[data-theme-select]');
        cards.forEach(function (card) {
            card.addEventListener('click', function () {
                var themeName = card.getAttribute('data-theme-select');
                applyTheme(themeName);
            });

            // Klavye erişilebilirliği
            card.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    var themeName = card.getAttribute('data-theme-select');
                    applyTheme(themeName);
                }
            });
        });
    }

    // ── Sayfa yüklenmeden önce temayı hemen uygula (FOUC önleme) ──
    // Bu script base.html'de <head> içinde veya body başında yüklenir
    var initialTheme = getSavedTheme();
    applyTheme(initialTheme);

    // ── DOM hazır olunca kartları bağla ──
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bindThemeCards);
    } else {
        bindThemeCards();
    }

    // ── Global API ──
    window.GoalTrackerTheme = {
        apply: applyTheme,
        getCurrent: getSavedTheme,
        getThemes: function () { return THEMES; }
    };
})();

