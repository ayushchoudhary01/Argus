CREATE TABLE IF NOT EXISTS events
(
    id            VARCHAR(64) PRIMARY KEY,
    date          DATE         NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   TEXT         NOT NULL,
    what_happened TEXT         NOT NULL,
    assets        TEXT[]       NOT NULL,
    created_at    TIMESTAMPTZ DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS event_outcomes
(
    id          SERIAL PRIMARY KEY,
    event_id    VARCHAR(64)    NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    ticker      VARCHAR(32)    NOT NULL,
    window_days INTEGER        NOT NULL,
    return_pct  NUMERIC(8,4),
    quality     VARCHAR(32)    NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (event_id, ticker, window_days)
    );

CREATE TABLE IF NOT EXISTS divergence_reports
(
    id               SERIAL PRIMARY KEY,
    correlation_id   VARCHAR(64)  NOT NULL UNIQUE,
    divergence_type  VARCHAR(64)  NOT NULL,
    report_json      JSONB        NOT NULL,
    created_at       TIMESTAMPTZ DEFAULT NOW()
    );
