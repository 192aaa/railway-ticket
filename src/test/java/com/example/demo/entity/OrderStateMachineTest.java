package com.example.demo.entity;

import com.example.demo.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    @Test
    void allowsLegalTransitions() {
        assertThat(OrderStateMachine.transition(OrderStatus.PENDING, OrderStatus.PAID)).isEqualTo(OrderStatus.PAID);
        assertThat(OrderStateMachine.transition(OrderStatus.PENDING, OrderStatus.CANCELLED)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(OrderStateMachine.transition(OrderStatus.PAID, OrderStatus.COMPLETED)).isEqualTo(OrderStatus.COMPLETED);
        assertThat(OrderStateMachine.transition(OrderStatus.PAID, OrderStatus.REFUNDED)).isEqualTo(OrderStatus.REFUNDED);
        assertThat(OrderStateMachine.transition(OrderStatus.COMPLETED, OrderStatus.REFUNDED)).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    void rejectsIllegalTransitions() {
        assertThatThrownBy(() -> OrderStateMachine.transition(OrderStatus.PAID, OrderStatus.PENDING))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许");
        assertThatThrownBy(() -> OrderStateMachine.transition(OrderStatus.REFUNDED, OrderStatus.PAID))
                .isInstanceOf(BusinessException.class);
        assertThat(OrderStateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.PAID)).isFalse();
    }

    @Test
    void rejectsNullStates() {
        assertThatThrownBy(() -> OrderStateMachine.transition(null, OrderStatus.PAID))
                .isInstanceOf(BusinessException.class);
    }
}
