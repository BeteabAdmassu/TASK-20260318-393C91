package com.mindflow.security.search;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TransitStopBootstrapService {

    private final TransitStopRepository repository;

    public TransitStopBootstrapService(TransitStopRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        if (repository.count() > 0) {
            return;
        }
        save("1A", "Beijing Central", "central,hub,main station", "beijing", "bj", 95, 980);
        save("2B", "Airport Express", "airport,terminal,fast", "jichang kuaixian", "jc", 90, 940);
        save("3C", "West Lake Stop", "lake,tourism,west", "xihu", "xh", 80, 760);
        save("8K", "South Tech Park", "technology,park,campus", "nan keji yuan", "nkjy", 88, 890);
        save("11", "East Market", "market,shopping,east", "dong shichang", "dsc", 75, 700);
        save("BJ7", "Beijing North Gate", "beijing,north,gate", "beijing", "bj", 85, 820);
        save("D5", "Dispatcher Operations", "dispatch,operations,control", "diaodu", "dd", 70, 600);
        save("A9", "University Town", "university,town,student", "daxuecheng", "dxc", 78, 720);
        save("M3", "City Museum", "museum,history,city", "bowuguan", "bwg", 65, 640);
    }

    private void save(String routeNumber, String stopName, String keywords, String pinyin, String initials,
                      int frequencyPriority, int stopPopularity) {
        TransitStopEntity row = new TransitStopEntity();
        row.setRouteNumber(routeNumber);
        row.setStopName(stopName);
        row.setKeywords(keywords);
        row.setPinyin(pinyin);
        row.setInitials(initials);
        row.setFrequencyPriority(frequencyPriority);
        row.setStopPopularity(stopPopularity);
        repository.save(row);
    }
}
