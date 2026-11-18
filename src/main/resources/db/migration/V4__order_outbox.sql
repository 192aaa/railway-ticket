-- 下单异步化：事务发件箱。订单与事件在同一事务落库，worker 可靠投递。
CREATE TABLE IF NOT EXISTS order_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(40) NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    payload TEXT,
    status VARCHAR(16) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 8,
    next_retry_at DATETIME(6) NOT NULL,
    last_error VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_status (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
