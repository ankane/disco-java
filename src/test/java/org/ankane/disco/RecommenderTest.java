package org.ankane.disco;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecommenderTest {
    @Test
    void testExplicit() throws Exception {
        Dataset<Integer, String> data = Data.loadMovieLens();

        Recommender<Integer, String> recommender = Recommender
            .builder()
            .factors(20)
            .fitExplicit(data);

        assertEquals(943, recommender.userIds().size());
        assertEquals(1664, recommender.itemIds().size());
        assertEquals(3.52986f, recommender.globalMean(), 0.00001f);

        List<Rec<String>> recs = recommender.itemRecs("Star Wars (1977)", 5);
        assertEquals(5, recs.size());

        List<String> itemIds = getIds(recs);
        assertTrue(itemIds.contains("Empire Strikes Back, The (1980)"));
        assertTrue(itemIds.contains("Return of the Jedi (1983)"));
        assertFalse(itemIds.contains("Star Wars (1977)"));

        assertEquals(0.9972f, recs.get(0).score, 0.01f);
    }

    @Test
    void testImplicit() throws Exception {
        Dataset<Integer, String> data = Data.loadMovieLens();

        Recommender<Integer, String> recommender = Recommender
            .builder()
            .factors(20)
            .fitImplicit(data);

        assertEquals(0.0f, recommender.globalMean());

        List<Rec<String>> recs = recommender.itemRecs("Star Wars (1977)", 5);
        List<String> itemIds = getIds(recs);
        assertTrue(itemIds.contains("Return of the Jedi (1983)"));
        assertFalse(itemIds.contains("Star Wars (1977)"));
    }

    @Test
    void testRated() {
        Dataset<Integer, String> data = new Dataset<>();
        data.add(1, "A", 1.0f);
        data.add(1, "B", 1.0f);
        data.add(1, "C", 1.0f);
        data.add(1, "D", 1.0f);
        data.add(2, "C", 1.0f);
        data.add(2, "D", 1.0f);
        data.add(2, "E", 1.0f);
        data.add(2, "F", 1.0f);

        Recommender<Integer, String> recommender = Recommender.fitImplicit(data);

        List<String> itemIds = getIds(recommender.userRecs(1, 5));
        itemIds.sort(Comparator.naturalOrder());
        assertEquals(itemIds, Arrays.asList("E", "F"));

        itemIds = getIds(recommender.userRecs(2, 5));
        itemIds.sort(Comparator.naturalOrder());
        assertEquals(itemIds, Arrays.asList("A", "B"));
    }

    @Test
    void testItemRecsSameScore() {
        Dataset<Integer, String> data = new Dataset<>();
        data.add(1, "A", 1.0f);
        data.add(1, "B", 1.0f);
        data.add(2, "C", 1.0f);

        Recommender<Integer, String> recommender = Recommender.fitImplicit(data);
        List<String> itemIds = getIds(recommender.itemRecs("A", 5));
        assertEquals(itemIds, Arrays.asList("B", "C"));
    }

    @Test
    void testSimilarUsers() throws Exception {
        Dataset<Integer, String> data = Data.loadMovieLens();

        Recommender<Integer, String> recommender = Recommender.fitExplicit(data);

        assertEquals(5, recommender.similarUsers(1, 5).size());
        assertEquals(0, recommender.similarUsers(100000, 5).size());
    }

    @Test
    void testIds() {
        Dataset<Integer, String> data = new Dataset<>();
        data.add(1, "A", 1.0f);
        data.add(1, "B", 1.0f);
        data.add(2, "B", 1.0f);

        Recommender<Integer, String> recommender = Recommender.fitImplicit(data);
        assertEquals(recommender.userIds(), Arrays.asList(1, 2));
        assertEquals(recommender.itemIds(), Arrays.asList("A", "B"));
    }

    @Test
    void testFactors() {
        Dataset<Integer, String> data = new Dataset<>();
        data.add(1, "A", 1.0f);
        data.add(1, "B", 1.0f);
        data.add(2, "B", 1.0f);

        Recommender<Integer, String> recommender = Recommender
            .builder()
            .factors(20)
            .fitImplicit(data);

        assertEquals(recommender.userFactors(1).get().length, 20);
        assertEquals(recommender.itemFactors("A").get().length, 20);

        assertEquals(recommender.userFactors(3), Optional.empty());
        assertEquals(recommender.itemFactors("C"), Optional.empty());
    }

    @Test
    void testUserRecsNewUser() {
        Dataset<Integer, Integer> data = new Dataset<>();
        data.add(1, 1, 5.0f);
        data.add(2, 1, 3.0f);

        Recommender<Integer, Integer> recommender = Recommender.fitExplicit(data);

        assertEquals(0, recommender.userRecs(1000, 5).size());
    }

    @Test
    void testCallback() {
        Dataset<Integer, Integer> data = new Dataset<>();
        data.add(1, 1, 5.0f);

        final AtomicInteger iterations = new AtomicInteger(0);
        Recommender
            .builder()
            .callback((info) -> iterations.incrementAndGet())
            .fitExplicit(data);

        assertEquals(20, iterations.intValue());
    }

    @Test
    void testNoTrainingData() {
        Dataset<Integer, Integer> data = new Dataset<>();
        Recommender<Integer, Integer> recommender = Recommender.fitExplicit(data);
        assertTrue(recommender.userIds().isEmpty());
        assertTrue(recommender.itemIds().isEmpty());
        assertEquals(Float.NaN, recommender.predict(1, 1));
    }

    <T> List<T> getIds(List<Rec<T>> recs) {
        return recs.stream().map(v -> v.id).collect(Collectors.toList());
    }
}
