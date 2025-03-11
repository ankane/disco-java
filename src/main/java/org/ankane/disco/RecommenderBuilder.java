package org.ankane.disco;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A recommender builder.
 */
public class RecommenderBuilder {
    int factors;
    int iterations;
    Optional<Float> regularization;
    float learningRate;
    float alpha;
    Optional<Consumer<FitInfo>> callback;
    Optional<Long> seed;

    /**
     * Starts a new recommender.
     */
    public RecommenderBuilder() {
        this.factors = 8;
        this.iterations = 20;
        this.regularization = Optional.empty();
        this.learningRate = 0.1f;
        this.alpha = 40.0f;
        this.callback = Optional.empty();
        this.seed = Optional.empty();
    }

    /**
     * Sets the number of factors.
     */
    public RecommenderBuilder factors(int value) {
        this.factors = value;
        return this;
    }

    /**
     * Sets the number of iterations.
     */
    public RecommenderBuilder iterations(int value) {
        this.iterations = value;
        return this;
    }

    /**
     * Sets the regularization.
     */
    public RecommenderBuilder regularization(float value) {
        this.regularization = Optional.of(value);
        return this;
    }

    /**
     * Sets the learning rate.
     */
    public RecommenderBuilder learningRate(float value) {
        this.learningRate = value;
        return this;
    }

    /**
     * Sets alpha.
     */
    public RecommenderBuilder alpha(float value) {
        this.alpha = value;
        return this;
    }

    /**
     * Sets the callback for each iteration.
     */
    public RecommenderBuilder callback(Consumer<FitInfo> value) {
        this.callback = Optional.of(value);
        return this;
    }

    /**
     * Sets the random seed.
     */
    public RecommenderBuilder seed(long value) {
        this.seed = Optional.of(value);
        return this;
    }

    /**
     * Creates a recommender with explicit feedback.
     */
    public <T, U> Recommender<T, U> fitExplicit(Dataset<T, U> trainSet) {
        return Recommender.fit(trainSet, this, false);
    }

    /**
     * Creates a recommender with implicit feedback.
     */
    public <T, U> Recommender<T, U> fitImplicit(Dataset<T, U> trainSet) {
        return Recommender.fit(trainSet, this, true);
    }
}
