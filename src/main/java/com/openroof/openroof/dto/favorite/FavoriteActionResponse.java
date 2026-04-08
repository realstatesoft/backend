package com.openroof.openroof.dto.favorite;

public record FavoriteActionResponse(
        Long propertyId,
        boolean favorited,
        Integer favoriteCount) {
}
