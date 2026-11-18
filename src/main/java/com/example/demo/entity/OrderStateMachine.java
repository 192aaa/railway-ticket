package com.example.demo.entity;

import com.example.demo.common.BusinessException;

/**
 * 订单状态机：集中定义合法的状态迁移，避免各处 if-else 散落。
 *
 * PENDING --确认/支付--> PAID --完结--> COMPLETED
 * PENDING --超时/取消--> CANCELLED
 * PAID / COMPLETED --退票--> REFUNDED
 */
public final class OrderStateMachine {

    private OrderStateMachine() {
    }

    /** 校验并迁移，返回目标状态。非法迁移直接抛业务异常。 */
    public static OrderStatus transition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            throw new BusinessException("订单状态异常");
        }
        boolean allowed = switch (from) {
            case PENDING -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
            case PAID -> to == OrderStatus.COMPLETED || to == OrderStatus.REFUNDED;
            case COMPLETED -> to == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };
        if (!allowed) {
            throw new BusinessException("订单状态不允许从 " + from + " 变更为 " + to);
        }
        return to;
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        try {
            transition(from, to);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
}
