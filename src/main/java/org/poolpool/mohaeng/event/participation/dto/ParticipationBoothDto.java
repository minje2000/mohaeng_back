package org.poolpool.mohaeng.event.participation.dto;

import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParticipationBoothDto {
    private Long pctBoothId;
    private Long hostBoothId;
    private Long userId;

    private String homepageUrl;
    private String boothTitle;
    private String boothTopic;
    private String mainItems;
    private String description;

    private Integer boothCount;
    private Integer boothPrice;
    private Integer facilityPrice;
    private Integer totalPrice;

    private String status;
    private LocalDateTime approvedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 시설 신청 목록
    private List<ParticipationBoothFacilityDto> facilities = new ArrayList<>();

    public static ParticipationBoothDto fromEntity(ParticipationBoothEntity e) {
        ParticipationBoothDto d = new ParticipationBoothDto();
        d.pctBoothId = e.getPctBoothId();
        d.hostBoothId = e.getHostBoothId();
        d.userId = e.getUserId();
        d.homepageUrl = e.getHomepageUrl();
        d.boothTitle = e.getBoothTitle();
        d.boothTopic = e.getBoothTopic();
        d.mainItems = e.getMainItems();
        d.description = e.getDescription();
        d.boothCount = e.getBoothCount();
        d.boothPrice = e.getBoothPrice();
        d.facilityPrice = e.getFacilityPrice();
        d.totalPrice = e.getTotalPrice();
        d.status = e.getStatus();
        d.approvedDate = e.getApprovedDate();
        d.createdAt = e.getCreatedAt();
        d.updatedAt = e.getUpdatedAt();
        return d;
    }

    public ParticipationBoothEntity toEntity() {
        ParticipationBoothEntity e = new ParticipationBoothEntity();
        e.setPctBoothId(this.pctBoothId);
        e.setHostBoothId(this.hostBoothId);
        e.setUserId(this.userId);
        e.setHomepageUrl(this.homepageUrl);
        e.setBoothTitle(this.boothTitle);
        e.setBoothTopic(this.boothTopic);
        e.setMainItems(this.mainItems);
        e.setDescription(this.description);
        e.setBoothCount(this.boothCount);
        e.setBoothPrice(this.boothPrice);
        e.setFacilityPrice(this.facilityPrice);
        e.setTotalPrice(this.totalPrice);
        e.setStatus(this.status);
        return e;
    }

    // getter/setter (필요한 것만이라도 OK)
    public Long getPctBoothId() { return pctBoothId; }
    public void setPctBoothId(Long pctBoothId) { this.pctBoothId = pctBoothId; }
    public Long getHostBoothId() { return hostBoothId; }
    public void setHostBoothId(Long hostBoothId) { this.hostBoothId = hostBoothId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getHomepageUrl() { return homepageUrl; }
    public void setHomepageUrl(String homepageUrl) { this.homepageUrl = homepageUrl; }
    public String getBoothTitle() { return boothTitle; }
    public void setBoothTitle(String boothTitle) { this.boothTitle = boothTitle; }
    public String getBoothTopic() { return boothTopic; }
    public void setBoothTopic(String boothTopic) { this.boothTopic = boothTopic; }
    public String getMainItems() { return mainItems; }
    public void setMainItems(String mainItems) { this.mainItems = mainItems; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getBoothCount() { return boothCount; }
    public void setBoothCount(Integer boothCount) { this.boothCount = boothCount; }
    public Integer getBoothPrice() { return boothPrice; }
    public void setBoothPrice(Integer boothPrice) { this.boothPrice = boothPrice; }
    public Integer getFacilityPrice() { return facilityPrice; }
    public void setFacilityPrice(Integer facilityPrice) { this.facilityPrice = facilityPrice; }
    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<ParticipationBoothFacilityDto> getFacilities() { return facilities; }
    public void setFacilities(List<ParticipationBoothFacilityDto> facilities) { this.facilities = facilities; }
}
