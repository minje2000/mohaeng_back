package org.poolpool.mohaeng.event.review.service;

import java.util.Optional;

import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.review.dto.EventReviewTabItemDto;
import org.poolpool.mohaeng.event.review.dto.MyPageReviewItemDto;
import org.poolpool.mohaeng.event.review.dto.ReviewCreateRequestDto;
import org.poolpool.mohaeng.event.review.dto.ReviewEditRequestDto;

public interface ReviewService {
	PageResponse<MyPageReviewItemDto> selectMyList(long userId, org.springframework.data.domain.Pageable pageable);

	PageResponse<EventReviewTabItemDto> selectEventReviews(long userId, long eventId,
			org.springframework.data.domain.Pageable pageable);

	Optional<EventReviewTabItemDto> selectMyReviewForEvent(long userId, long eventId);

	long create(long userId, ReviewCreateRequestDto request);

	boolean edit(long userId, long reviewId, ReviewEditRequestDto request);

	boolean delete(long userId, long reviewId);
}
