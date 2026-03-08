package org.poolpool.mohaeng.event.wishlist.exception;

public class WishlistAlreadyExistsException extends RuntimeException {
    public WishlistAlreadyExistsException(long userId, long eventId) {
        super("이미 관심 등록된 행사입니다. userId=" + userId + ", eventId=" + eventId);
    }
}
