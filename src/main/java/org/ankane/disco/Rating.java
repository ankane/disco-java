package org.ankane.disco;

class Rating<T, U> {
    T userId;
    U itemId;
    float value;

    Rating(T userId, U itemId, float value) {
        this.userId = userId;
        this.itemId = itemId;
        this.value = value;
    }
}
