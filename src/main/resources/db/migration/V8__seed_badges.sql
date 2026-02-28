-- V8: Seed badge verisi (7 rozet)
INSERT INTO badges (code, name, description, icon, condition_type, condition_value) VALUES
('FIRST_STEP',     'İlk Adım',         'İlk ilerleme kaydını yaptın!',              '🎯', 'ENTRY_COUNT',  1),
('WEEK_WARRIOR',   'Hafta Savaşçısı',  '7 gün üst üste hedefe ulaştın!',           '🔥', 'STREAK',       7),
('MONTH_CHAMPION', 'Ay Şampiyonu',     '30 gün üst üste kesintisiz çalıştın!',     '🏆', 'STREAK',      30),
('SPEED_DEMON',    'Süper Hızlı',      'Planının %150''sini tutturdun!',            '⚡', 'PACE_PCT',   150),
('GOAL_HUNTER',    'Hedef Avcısı',     'İlk hedefini tamamladın!',                 '✅', 'COMPLETIONS',  1),
('MULTI_TASKER',   'Çok Yönlü',        'Aynı anda 5 aktif hedefin var!',           '🌟', 'ACTIVE_GOALS', 5),
('CENTURY',        '100 Günlük',       '100 gün boyunca aktif kaldın!',            '💯', 'STREAK',     100);

