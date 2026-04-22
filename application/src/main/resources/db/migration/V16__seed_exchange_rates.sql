-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V16
-- Exchange Rates：初始匯率 seed data
-- ========================================

SET NAMES utf8mb4;

-- USD/TWD
INSERT INTO exchange_rates (BASE_CURRENCY, QUOTE_CURRENCY, RATE, RATE_DATE, SOURCE, CREATED_AT)
VALUES ('USD', 'TWD', 31.500000, '2026-04-22', 'MANUAL', NOW());

-- EUR/TWD
INSERT INTO exchange_rates (BASE_CURRENCY, QUOTE_CURRENCY, RATE, RATE_DATE, SOURCE, CREATED_AT)
VALUES ('EUR', 'TWD', 35.800000, '2026-04-22', 'MANUAL', NOW());

-- JPY/TWD
INSERT INTO exchange_rates (BASE_CURRENCY, QUOTE_CURRENCY, RATE, RATE_DATE, SOURCE, CREATED_AT)
VALUES ('JPY', 'TWD', 0.215000, '2026-04-22', 'MANUAL', NOW());

-- CNY/TWD
INSERT INTO exchange_rates (BASE_CURRENCY, QUOTE_CURRENCY, RATE, RATE_DATE, SOURCE, CREATED_AT)
VALUES ('CNY', 'TWD', 4.320000, '2026-04-22', 'MANUAL', NOW());