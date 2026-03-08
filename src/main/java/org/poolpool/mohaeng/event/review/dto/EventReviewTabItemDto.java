package org.poolpool.mohaeng.event.review.dto;

import java.time.LocalDateTime;

import org.poolpool.mohaeng.event.review.entity.ReviewEntity;

public class EventReviewTabItemDto {
  private Long reviewId;
  private String userName;
  private int ratingContent;
  private int ratingProgress;
  private int ratingMood;
  private String content;
  private LocalDateTime createdAt;

  public static EventReviewTabItemDto fromEntity(ReviewEntity e) {
    EventReviewTabItemDto dto = new EventReviewTabItemDto();
    dto.reviewId = e.getReviewId();
    dto.userName = e.getUser().getName();          //  UserEntity 필드명 맞게 수정
    dto.ratingContent = e.getRatingContent();
    dto.ratingProgress = e.getRatingProgress();
    dto.ratingMood = e.getRatingMood();
    dto.content = e.getContent();
    dto.createdAt = e.getCreatedAt();
    return dto;
  }

  public Long getReviewId() { return reviewId; }
  public String getUserName() { return userName; }
  public int getRatingContent() { return ratingContent; }
  public int getRatingProgress() { return ratingProgress; }
  public int getRatingMood() { return ratingMood; }
  public String getContent() { return content; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
