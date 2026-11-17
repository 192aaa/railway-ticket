-- 座位唯一约束：同一车次 + 日期 + 席别下，一个座位号只能被一个订单占用。
-- 冲突时由 OrderService 在独立事务中重新分配座位并重试；本脚本负责让历史数据满足约束。

-- 1) 历史重复座位号：按 (车次, 日期, 席别, 座位号) 分组，仅保留最早一条，
--    其余加释放后缀，避免建唯一索引失败。
UPDATE orders o
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY train_id, train_date, seat_type, seat_no
               ORDER BY id
           ) AS rn
    FROM orders
    WHERE seat_no IS NOT NULL AND seat_no <> ''
) d ON o.id = d.id
SET o.seat_no = CONCAT(LEFT(o.seat_no, 12), '#D', o.id)
WHERE d.rn > 1;

-- 2) 非有效占用状态（退票 / 取消 / 待支付）让出座位号，便于座位复用。
--    保留原座位信息作为审计痕迹，追加释放标记后不再与有效座位冲突。
UPDATE orders
SET seat_no = CONCAT(LEFT(seat_no, 12), '#F', id)
WHERE seat_no IS NOT NULL AND seat_no <> ''
  AND status IN ('REFUNDED', 'CANCELLED', 'PENDING');

-- 3) 建立唯一约束。
ALTER TABLE orders ADD CONSTRAINT uk_orders_seat
    UNIQUE KEY (train_id, train_date, seat_type, seat_no);
