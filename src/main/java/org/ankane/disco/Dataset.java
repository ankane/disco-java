package org.ankane.disco;

import java.util.ArrayList;
import java.util.List;

/**
 * A dataset.
 */
public class Dataset<T, U> {
    List<Rating<T, U>> data;

    /**
     * Creates a new dataset.
     */
    public Dataset() {
        this.data = new ArrayList<>();
    }

    /**
     * Creates a new dataset with an initial capacity.
     *
     * @param initialCapacity the initial capacity of the dataset
     */
    public Dataset(int initialCapacity) {
        this.data = new ArrayList<>(initialCapacity);
    }

    /**
     * Adds a rating to the dataset.
     *
     * @param userId - the user id
     * @param itemId - the item id
     * @param value - the value
     */
    public void add(T userId, U itemId, float value) {
        data.add(new Rating<T, U>(userId, itemId, value));
    }

    /**
     * Returns the number of ratings in the dataset.
     *
     * @return the number of ratings in the dataset
     */
    public int size() {
        return this.data.size();
    }
}
