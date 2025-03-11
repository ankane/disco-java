package org.ankane.disco;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * A recommender.
 */
public class Recommender<T, U> {
    private IdMap<T> userMap;
    private IdMap<U> itemMap;
    private Map<Integer, Set<Integer>> rated;
    private float globalMean;
    private float[][] userFactors;
    private float[][] itemFactors;
    private float[] userNorms;
    private float[] itemNorms;

    private Recommender(IdMap<T> userMap, IdMap<U> itemMap, Map<Integer, Set<Integer>> rated, float globalMean, float[][] userFactors, float[][] itemFactors) {
        this.userMap = userMap;
        this.itemMap = itemMap;
        this.rated = rated;
        this.globalMean = globalMean;
        this.userFactors = userFactors;
        this.itemFactors = itemFactors;
    }

    /**
     * Creates a recommender builder.
     */
    public static RecommenderBuilder builder() {
        return new RecommenderBuilder();
    }

    /**
     * Creates a recommender with explicit feedback.
     */
    public static <T, U> Recommender<T, U> fitExplicit(Dataset<T, U> trainSet) {
        return fit(trainSet, builder(), false);
    }

    /**
     * Creates a recommender with implicit feedback.
     */
    public static <T, U> Recommender<T, U> fitImplicit(Dataset<T, U> trainSet) {
        return fit(trainSet, builder(), true);
    }

    static <T, U> Recommender<T, U> fit(Dataset<T, U> trainSet, RecommenderBuilder options, boolean implicit) {
        int factors = options.factors;

        IdMap<T> userMap = new IdMap<>();
        IdMap<U> itemMap = new IdMap<>();
        Map<Integer, Set<Integer>> rated = new HashMap<>();

        int[] rowInds = new int[trainSet.size()];
        int[] colInds = new int[trainSet.size()];
        float[] values = new float[trainSet.size()];

        List<List<SparseRow>> cui = new ArrayList<>();
        List<List<SparseRow>> ciu = new ArrayList<>();

        for (int j = 0; j < trainSet.data.size(); j++) {
            Rating<T, U> rating = trainSet.data.get(j);

            int u = userMap.add(rating.userId);
            int i = itemMap.add(rating.itemId);

            if (implicit) {
                if (u == cui.size()) {
                    cui.add(new ArrayList<>());
                }

                if (i == ciu.size()) {
                    ciu.add(new ArrayList<>());
                }

                float confidence = 1.0f + options.alpha * rating.value;
                cui.get(u).add(new SparseRow(i, confidence));
                ciu.get(i).add(new SparseRow(u, confidence));
            } else {
                rowInds[j] = u;
                colInds[j] = i;
                values[j] = rating.value;
            }

            rated.computeIfAbsent(u, k -> new HashSet<>()).add(i);
        }

        int users = userMap.size();
        int items = itemMap.size();

        float globalMean = 0.0f;
        if (!implicit) {
            for (int i = 0; i < values.length; i++) {
                globalMean += values[i];
            }
            globalMean /= values.length;
        }

        float endRange = implicit ? 0.01f : 0.1f;

        Random prng = options.seed.map(s -> new Random(s)).orElseGet(() -> new Random());

        float[][] userFactors = createFactors(users, factors, prng, endRange);
        float[][] itemFactors = createFactors(items, factors, prng, endRange);

        Recommender<T, U> recommender = new Recommender<T, U>(userMap, itemMap, rated, globalMean, userFactors, itemFactors);

        if (implicit) {
            // conjugate gradient method
            // https://www.benfrederickson.com/fast-implicit-matrix-factorization/

            float regularization = options.regularization.orElse(0.01f);

            for (int iteration = 0; iteration < options.iterations; iteration++) {
                leastSquaresCg(cui, userFactors, itemFactors, regularization, factors);
                leastSquaresCg(ciu, itemFactors, userFactors, regularization, factors);

                if (options.callback.isPresent()) {
                    FitInfo info = new FitInfo(iteration + 1, Float.NaN);
                    options.callback.get().accept(info);
                }
            }
        } else {
            // stochastic gradient method with twin learners
            // https://www.csie.ntu.edu.tw/~cjlin/papers/libmf/mf_adaptive_pakdd.pdf
            // algorithm 2

            float learningRate = options.learningRate;
            float lambda = options.regularization.orElse(0.1f);
            int k = factors;
            int ks = Math.max((int) Math.round(k * 0.08), 1);

            float[] gSlow = new float[users];
            float[] gFast = new float[users];
            Arrays.fill(gSlow, 1.0f);
            Arrays.fill(gFast, 1.0f);

            float[] hSlow = new float[items];
            float[] hFast = new float[items];
            Arrays.fill(hSlow, 1.0f);
            Arrays.fill(hFast, 1.0f);

            for (int iteration = 0; iteration < options.iterations; iteration++) {
                double trainLoss = 0.0;

                // shuffle for each iteration
                for (int j : sample(prng, trainSet.size())) {
                    int u = rowInds[j];
                    int v = colInds[j];

                    float[] pu = userFactors[u];
                    float[] qv = itemFactors[v];
                    float e = values[j] - dot(pu, qv);

                    // slow learner
                    float gHat = 0.0f;
                    float hHat = 0.0f;

                    float nu = learningRate / (float) Math.sqrt(gSlow[u]);
                    float nv = learningRate / (float) Math.sqrt(hSlow[v]);

                    for (int d = 0; d < ks; d++) {
                        float gud = -e * qv[d] + lambda * pu[d];
                        float hvd = -e * pu[d] + lambda * qv[d];

                        gHat += gud * gud;
                        hHat += hvd * hvd;

                        pu[d] -= nu * gud;
                        qv[d] -= nv * hvd;
                    }

                    gSlow[u] += gHat / (float) ks;
                    hSlow[v] += hHat / (float) ks;

                    // fast learner
                    // don't update on first outer iteration
                    if (iteration > 0) {
                        gHat = 0.0f;
                        hHat = 0.0f;

                        nu = learningRate / (float) Math.sqrt(gFast[u]);
                        nv = learningRate / (float) Math.sqrt(hFast[v]);

                        for (int d = ks; d < k; d++) {
                            float gud = -e * qv[d] + lambda * pu[d];
                            float hvd = -e * pu[d] + lambda * qv[d];

                            gHat += gud * gud;
                            hHat += hvd * hvd;

                            pu[d] -= nu * gud;
                            qv[d] -= nv * hvd;
                        }

                        gFast[u] += gHat / (float) (k - ks);
                        hFast[v] += hHat / (float) (k - ks);
                    }

                    trainLoss += e * e;
                }

                if (options.callback.isPresent()) {
                    trainLoss = Math.sqrt(trainLoss / trainSet.size());
                    FitInfo info = new FitInfo(iteration + 1, (float) trainLoss);
                    options.callback.get().accept(info);
                }
            }
        }

        recommender.userNorms = norms(recommender.userFactors);
        recommender.itemNorms = norms(recommender.itemFactors);

        return recommender;
    }

    /**
     * Returns the predicted rating for a specific user and item.
     */
    public float predict(T userId, U itemId) {
        Optional<Integer> i = this.userMap.get(userId);
        if (!i.isPresent()) {
            return this.globalMean;
        }

        Optional<Integer> j = this.itemMap.get(itemId);
        if (!j.isPresent()) {
            return this.globalMean;
        }

        return dot(this.userFactors[i.get()], this.itemFactors[j.get()]);
    }

    /**
     * Returns recommendations for a user.
     */
    public List<Rec<U>> userRecs(T userId, int count) {
        Optional<Integer> oi = this.userMap.get(userId);
        if (!oi.isPresent()) {
            return new ArrayList<>();
        }
        int i = oi.get();

        Set<Integer> rated = this.rated.get(i);
        float[] f = this.userFactors[i];
        List<Rec<Integer>> predictions = new ArrayList<>(this.itemFactors.length);
        for (int j = 0; j < this.itemFactors.length; j++) {
            predictions.add(new Rec<Integer>(j, dot(f, this.itemFactors[j])));
        }
        predictions.sort(Comparator.comparing(v -> -v.score));

        List<Rec<U>> recs = new ArrayList<>();
        for (Rec<Integer> v : predictions) {
            if (rated.contains(v.id)) {
                continue;
            }

            recs.add(new Rec<U>(this.itemMap.lookup(v.id), v.score));

            if (recs.size() == count) {
                break;
            }
        }
        return recs;
    }

    /**
     * Returns recommendations for an item.
     */
    public List<Rec<U>> itemRecs(U itemId, int count) {
        return similar(
            this.itemMap,
            this.itemFactors,
            this.itemNorms,
            itemId,
            count
        );
    }

    /**
     * Returns similar users.
     */
    public List<Rec<T>> similarUsers(T userId, int count) {
        return similar(
            this.userMap,
            this.userFactors,
            this.userNorms,
            userId,
            count
        );
    }

    /**
     * Returns user ids.
     */
    public List<T> userIds() {
        return this.userMap.ids();
    }

    /**
     * Returns item ids.
     */
    public List<U> itemIds() {
        return this.itemMap.ids();
    }

    /**
     * Returns factors for a specific user.
     */
    public Optional<float[]> userFactors(T userId) {
        return this.userMap.get(userId).map(i -> this.userFactors[i]);
    }

    /**
     * Returns factors for a specific user.
     */
    public Optional<float[]> itemFactors(U itemId) {
        return this.itemMap.get(itemId).map(i -> this.itemFactors[i]);
    }

    /**
     * Returns the global mean.
     */
    public float globalMean() {
        return this.globalMean;
    }

    private static void leastSquaresCg(List<List<SparseRow>> cui, float[][] x, float[][] y, float regularization, int factors) {
        int cgSteps = 3;

        // calculate YtY
        float[][] yty = new float[factors][factors];
        for (int i = 0; i < factors; i++) {
            for (int j = 0; j < factors; j++) {
                float sum = 0.0f;
                for (int k = 0; k < y.length; k++) {
                    sum += y[k][i] * y[k][j];
                }
                yty[i][j] = sum;
            }
        }
        for (int i = 0; i < factors; i++) {
            yty[i][i] += regularization;
        }

        for (int u = 0; u < cui.size(); u++) {
            List<SparseRow> rowVec = cui.get(u);

            // start from previous iteration
            float[] xi = x[u];

            // calculate residual r = (YtCuPu - (YtCuY.dot(Xu), without computing YtCuY
            float[] r = new float[yty.length];
            for (int i = 0; i < yty.length; i++) {
                r[i] = -dot(yty[i], xi);
            }
            for (SparseRow row : rowVec) {
                int i = row.index;
                float confidence = row.confidence;
                scaledAdd(r, confidence - (confidence - 1.0f) * dot(y[i], xi), y[i]);
            }

            float[] p = Arrays.copyOf(r, r.length);
            float rsold = dot(r, r);

            for (int j = 0; j < cgSteps; j++) {
                // calculate Ap = YtCuYp - without actually calculating YtCuY
                float[] ap = new float[yty.length];
                for (int i = 0; i < yty.length; i++) {
                    ap[i] = dot(yty[i], p);
                }
                for (SparseRow row : rowVec) {
                    int i = row.index;
                    float confidence = row.confidence;
                    scaledAdd(ap, (confidence - 1.0f) * dot(y[i], p), y[i]);
                }

                // standard CG update
                float alpha = rsold / dot(p, ap);
                scaledAdd(xi, alpha, p);
                scaledAdd(r, -alpha, ap);
                float rsnew = dot(r, r);

                if (rsnew < 1e-20) {
                    break;
                }

                float rs = rsnew / rsold;
                for (int i = 0; i < p.length; i++) {
                    p[i] = r[i] + rs * p[i];
                }
                rsold = rsnew;
            }
        }
    }

    private static float[][] createFactors(int rows, int cols, Random prng, float endRange) {
        float[][] m = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m[i][j] = prng.nextFloat() * endRange;
            }
        }
        return m;
    }

    private <V> List<Rec<V>> similar(IdMap<V> map, float[][] factors, float[] norms, V id, int count) {
        Optional<Integer> oi = map.get(id);
        if (!oi.isPresent()) {
            return new ArrayList<>();
        }
        int i = oi.get();

        float[] f = factors[i];
        float norm = norms[i];
        float eps = Math.ulp(0.0f);
        List<Rec<Integer>> predictions = new ArrayList<>(factors.length);
        for (int j = 0; j < factors.length; j++) {
            predictions.add(new Rec<Integer>(j, dot(f, factors[j]) / Math.max(norm * norms[j], eps)));
        }
        predictions.sort(Comparator.comparing(v -> -v.score));

        List<Rec<V>> recs = new ArrayList<>();
        for (Rec<Integer> v : predictions) {
            if (v.id == i) {
                continue;
            }

            recs.add(new Rec<V>(map.lookup(v.id), v.score));

            if (recs.size() == count) {
                break;
            }
        }
        return recs;
    }

    private static float[] norms(float[][] factors) {
        float[] norms = new float[factors.length];
        for (int i = 0; i < factors.length; i++) {
            float[] row = factors[i];
            float norm = 0.0f;
            for (float v : row) {
                norm += v * v;
            }
            norms[i] = (float) Math.sqrt(norm);
        }
        return norms;
    }

    private static float dot(float[] a, float[] b) {
        float sum = 0.0f;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static void scaledAdd(float[] x, float a, float[] v) {
        for (int i = 0; i < x.length; i++) {
            x[i] += a * v[i];
        }
    }

    private static List<Integer> sample(Random prng, int n) {
        List<Integer> v = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            v.add(i);
        }
        Collections.shuffle(v, prng);
        return v;
    }
}
