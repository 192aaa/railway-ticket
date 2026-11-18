package com.example.demo.mq;

/** 订单事件投递通道；默认本地 no-op（由 worker 定时轮询兜底）。 */
@FunctionalInterface
public interface OrderEventDispatcher {

    void dispatch(String eventId);
}
