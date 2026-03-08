package org.poolpool.mohaeng.event.wishlist.service;

import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistCreateRequestDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistItemDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistToggleRequestDto;
import org.poolpool.mohaeng.event.wishlist.repository.*;
import org.springframework.data.domain.Pageable;

public interface EventWishlistService {
    PageResponse<WishlistItemDto> getList(long userId, Pageable pageable);
    long add(Long userId, WishlistCreateRequestDto request);
    void remove(long userId, long wishId);
    WishlistItemDto toggleNotification(long userId, long wishId, WishlistToggleRequestDto request);
}
