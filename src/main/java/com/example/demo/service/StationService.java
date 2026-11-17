package com.example.demo.service;

import com.example.demo.entity.Station;
import com.example.demo.repository.StationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Cacheable(value = "stations", key = "'all'")
    public List<Station> listAll() {
        return stationRepository.findAll();
    }

    @Cacheable(value = "stations", key = "#keyword == null || #keyword.isEmpty() ? 'all' : #keyword")
    public List<Station> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return stationRepository.findAll();
        }
        return stationRepository.findByNameContaining(keyword);
    }
}
