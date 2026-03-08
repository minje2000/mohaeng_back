package org.poolpool.mohaeng.event.wishlist.exception;

public class WishlistNotFoundOrForbiddenException extends RuntimeException {
    public WishlistNotFoundOrForbiddenException(long wishId) {
        super("관심행사가 없거나 본인 소유가 아닙니다. wishId=" + wishId);
    }
}
