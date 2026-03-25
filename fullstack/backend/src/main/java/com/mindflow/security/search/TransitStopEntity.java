package com.mindflow.security.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transit_stops")
public class TransitStopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_number", nullable = false, length = 40)
    private String routeNumber;

    @Column(name = "stop_name", nullable = false, length = 200)
    private String stopName;

    @Column(name = "keywords", nullable = false, length = 500)
    private String keywords;

    @Column(name = "pinyin", nullable = false, length = 200)
    private String pinyin;

    @Column(name = "initials", nullable = false, length = 80)
    private String initials;

    @Column(name = "frequency_priority", nullable = false)
    private int frequencyPriority;

    @Column(name = "stop_popularity", nullable = false)
    private int stopPopularity;

    public Long getId() {
        return id;
    }

    public String getRouteNumber() {
        return routeNumber;
    }

    public void setRouteNumber(String routeNumber) {
        this.routeNumber = routeNumber;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public int getFrequencyPriority() {
        return frequencyPriority;
    }

    public void setFrequencyPriority(int frequencyPriority) {
        this.frequencyPriority = frequencyPriority;
    }

    public int getStopPopularity() {
        return stopPopularity;
    }

    public void setStopPopularity(int stopPopularity) {
        this.stopPopularity = stopPopularity;
    }
}
