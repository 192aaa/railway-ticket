package com.example.demo.config;

import com.example.demo.dto.TrainTicketVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    void jsonSerializerRestoresCachedTicketList() {
        GenericJacksonJsonRedisSerializer serializer = RedisConfig.jsonSerializer();

        TrainTicketVO ticket = new TrainTicketVO();
        ticket.setTrainId(1L);
        ticket.setTrainNumber("G1");
        List<TrainTicketVO> source = new ArrayList<>(List.of(ticket));

        byte[] bytes = serializer.serialize(source);
        Object restored = serializer.deserialize(bytes);

        assertThat(restored).isInstanceOf(List.class);
        Object first = ((List<?>) restored).get(0);
        assertThat(first).isInstanceOf(TrainTicketVO.class);
        assertThat(((TrainTicketVO) first).getTrainNumber()).isEqualTo("G1");
    }
}
