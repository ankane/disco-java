package org.ankane.disco;

/**
 * Information about a training iteration.
 */
public class FitInfo {
    /** The iteration. */
    public int iteration;
    /** The training loss. */
    public float trainLoss;

    FitInfo(int iteration, float trainLoss) {
        this.iteration = iteration;
        this.trainLoss = trainLoss;
    }
}
