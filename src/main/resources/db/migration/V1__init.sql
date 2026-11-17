-- 初始 schema：与 JPA 实体一一对应。
-- prod 由 Flyway 建表，Hibernate 使用 ddl-auto=none；dev/test 关闭 Flyway，仍由 Hibernate 自动建表。
-- 对已有库（此前由 ddl-auto=update 建表）启用 baseline-on-migrate，不会重复执行本脚本。

CREATE TABLE IF NOT EXISTS station (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    city VARCHAR(100),
    code VARCHAR(10),
    PRIMARY KEY (id),
    KEY idx_station_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    id_card VARCHAR(20),
    phone VARCHAR(20),
    seat_type VARCHAR(20),
    create_time DATETIME(6),
    update_time DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS train (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_number VARCHAR(20) NOT NULL,
    train_type VARCHAR(20) NOT NULL,
    from_station_id BIGINT NOT NULL,
    to_station_id BIGINT NOT NULL,
    start_time VARCHAR(20) NOT NULL,
    end_time VARCHAR(20) NOT NULL,
    duration INT,
    business_seat_price DOUBLE,
    first_seat_price DOUBLE,
    second_seat_price DOUBLE,
    sleeper_price DOUBLE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_number (train_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS train_station (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    sort INT NOT NULL,
    arrive_time VARCHAR(20),
    depart_time VARCHAR(20),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ticket (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_id BIGINT NOT NULL,
    train_date DATE NOT NULL,
    business_seat_count INT,
    first_seat_count INT,
    second_seat_count INT,
    sleeper_count INT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_train_date (train_id, train_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(30) NOT NULL,
    user_id BIGINT NOT NULL,
    train_id BIGINT NOT NULL,
    train_number VARCHAR(20) NOT NULL,
    from_station VARCHAR(50),
    to_station VARCHAR(50),
    train_date VARCHAR(10),
    seat_type VARCHAR(20),
    seat_no VARCHAR(20),
    passenger_name VARCHAR(30),
    passenger_idcard VARCHAR(20),
    price DOUBLE,
    status VARCHAR(20),
    create_time DATETIME(6),
    pay_time DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    is_read BOOLEAN NOT NULL,
    related_order_id BIGINT,
    create_time DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
