package com.mindflow.security.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "cleaned_records")
public class CleanedRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @Column(name = "source_ref", nullable = false, length = 255)
    private String sourceRef;

    @Column(name = "stop_name", length = 200)
    private String stopName;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "residential_area", length = 200)
    private String residentialArea;

    @Column(name = "apartment_type", length = 120)
    private String apartmentType;

    @Column(name = "area_standardized", length = 120)
    private String areaStandardized;

    @Column(name = "price_standardized", length = 120)
    private String priceStandardized;

    @Column(name = "raw_snapshot", nullable = false, length = 4000)
    private String rawSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getImportJobId() {
        return importJobId;
    }

    public void setImportJobId(Long importJobId) {
        this.importJobId = importJobId;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getApartmentType() {
        return apartmentType;
    }

    public void setApartmentType(String apartmentType) {
        this.apartmentType = apartmentType;
    }

    public String getResidentialArea() {
        return residentialArea;
    }

    public void setResidentialArea(String residentialArea) {
        this.residentialArea = residentialArea;
    }

    public String getAreaStandardized() {
        return areaStandardized;
    }

    public void setAreaStandardized(String areaStandardized) {
        this.areaStandardized = areaStandardized;
    }

    public String getPriceStandardized() {
        return priceStandardized;
    }

    public void setPriceStandardized(String priceStandardized) {
        this.priceStandardized = priceStandardized;
    }

    public String getRawSnapshot() {
        return rawSnapshot;
    }

    public void setRawSnapshot(String rawSnapshot) {
        this.rawSnapshot = rawSnapshot;
    }
}
