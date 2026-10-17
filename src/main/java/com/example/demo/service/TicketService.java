package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.TrainTicketVO;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final TicketRepository ticketRepository;
    private final TrainStationRepository trainStationRepository;

    public TicketService(StationRepository stationRepository,
                         TrainRepository trainRepository,
                         TicketRepository ticketRepository,
                         TrainStationRepository trainStationRepository) {
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
        this.ticketRepository = ticketRepository;
        this.trainStationRepository = trainStationRepository;
    }

    @Cacheable(value = "tickets", key = "#fromStationName + '_' + #toStationName + '_' + #dateStr")
    public List<TrainTicketVO> queryTickets(String fromStationName, String toStationName, String dateStr) {
        Station fromStation = stationRepository.findByNameContaining(fromStationName).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到出发站: " + fromStationName));

        Station toStation = stationRepository.findByNameContaining(toStationName).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到到达站: " + toStationName));

        List<Train> trains = trainRepository.findByStation(fromStation.getId(), toStation.getId());
        LocalDate date = LocalDate.parse(dateStr);

        List<Long> trainIds = trains.stream().map(Train::getId).collect(Collectors.toList());
        Map<Long, Ticket> ticketMap = ticketRepository.findByTrainIdInAndTrainDate(trainIds, date).stream()
                .collect(Collectors.toMap(Ticket::getTrainId, t -> t, (a, b) -> a));

        List<TrainTicketVO> result = new ArrayList<>();
        for (Train train : trains) {
            TrainTicketVO vo = new TrainTicketVO();
            vo.setTrainId(train.getId());
            vo.setTrainNumber(train.getTrainNumber());
            vo.setTrainType(train.getTrainType());
            vo.setFromStation(fromStation.getName());
            vo.setToStation(toStation.getName());
            vo.setStartTime(train.getStartTime());
            vo.setEndTime(train.getEndTime());
            vo.setDuration(train.getDuration());

            Ticket ticket = ticketMap.get(train.getId());
            if (ticket != null) {
                vo.setBusinessSeatCount(ticket.getBusinessSeatCount());
                vo.setFirstSeatCount(ticket.getFirstSeatCount());
                vo.setSecondSeatCount(ticket.getSecondSeatCount());
                vo.setSleeperCount(ticket.getSleeperCount());
            } else {
                vo.setBusinessSeatCount(0);
                vo.setFirstSeatCount(0);
                vo.setSecondSeatCount(0);
                vo.setSleeperCount(0);
            }

            vo.setBusinessSeatPrice(train.getBusinessSeatPrice());
            vo.setFirstSeatPrice(train.getFirstSeatPrice());
            vo.setSecondSeatPrice(train.getSecondSeatPrice());
            vo.setSleeperPrice(train.getSleeperPrice());

            result.add(vo);
        }
        return result;
    }
}