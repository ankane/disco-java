package org.ankane.disco;

/**
 * A recommendation.
 */
public class Rec<T> {
    /** The id. */
    public T id;
    /** The score. */
    public float score;

    Rec(T id, float score) {
        this.id = id;
        this.score = score;
    }
}
