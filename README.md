# Disco Java

🔥 Recommendations for Java using collaborative filtering

- Supports user-based and item-based recommendations
- Works with explicit and implicit feedback
- Uses high-performance matrix factorization

🎉 Zero dependencies

[![Build Status](https://github.com/ankane/disco-java/actions/workflows/build.yml/badge.svg)](https://github.com/ankane/disco-java/actions)

## Installation

For Maven, add to `pom.xml` under `<dependencies>`:

```xml
<dependency>
    <groupId>org.ankane</groupId>
    <artifactId>disco</artifactId>
    <version>0.1.0</version>
</dependency>
```

For other build tools, see [this page](https://central.sonatype.com/artifact/org.ankane/disco).

## Getting Started

Prep your data in the format `user_id, item_id, value`

```java
import org.ankane.disco.Dataset;

Dataset<String, String> data = new Dataset<>();
data.add("user_a", "item_a", 5.0f);
data.add("user_a", "item_b", 3.5f);
data.add("user_b", "item_a", 4.0f);
```

IDs can be integers, strings, or any other hashable data type

```java
data.add(1, "item_a", 5.0f);
```

If users rate items directly, this is known as explicit feedback. Fit the recommender with:

```java
import org.ankane.disco.Recommender;

Recommender<String, String> recommender = Recommender.fitExplicit(data);
```

If users don’t rate items directly (for instance, they’re purchasing items or reading posts), this is known as implicit feedback. Use `1.0` or a value like number of purchases or page views for the dataset, and fit the recommender with:

```java
Recommender<String, String> recommender = Recommender.fitImplicit(data);
```

Get user-based recommendations - “users like you also liked”

```java
recommender.userRecs(userId, 5);
```

Get item-based recommendations - “users who liked this item also liked”

```java
recommender.itemRecs(itemId, 5);
```

Get predicted ratings for a specific user and item

```java
recommender.predict(userId, itemId);
```

Get similar users

```java
recommender.similarUsers(userId, 5);
```

## Examples

### MovieLens

Load the data

```java
import org.ankane.disco.Data;

Dataset<Integer, String> data = Data.loadMovieLens();
```

Create a recommender

```java
Recommender<Integer, String> recommender = Recommender
    .builder()
    .factors(20)
    .fitExplicit(data);
```

Get similar movies

```java
recommender.itemRecs("Star Wars (1977)", 5);
```

## Storing Recommendations

Save recommendations to your database.

Alternatively, you can store only the factors and use a library like [pgvector-java](https://github.com/pgvector/pgvector-java).

## Algorithms

Disco uses high-performance matrix factorization.

- For explicit feedback, it uses the [stochastic gradient method with twin learners](https://www.csie.ntu.edu.tw/~cjlin/papers/libmf/mf_adaptive_pakdd.pdf)
- For implicit feedback, it uses the [conjugate gradient method](https://www.benfrederickson.com/fast-implicit-matrix-factorization/)

Specify the number of factors and iterations

```java
Recommender<String, String> recommender = Recommender
    .builder()
    .factors(8)
    .iterations(20)
    .fitExplicit(data);
```

## Progress

Pass a callback to show progress

```java
Recommender<String, String> recommender = Recommender
    .builder()
    .callback((info) -> System.out.printf("%d: %f\n", info.iteration, info.trainLoss))
    .fitExplicit(data);
```

Note: `trainLoss` is not available for implicit feedback

## Cold Start

Collaborative filtering suffers from the [cold start problem](https://en.wikipedia.org/wiki/Cold_start_(recommender_systems)). It’s unable to make good recommendations without data on a user or item, which is problematic for new users and items.

```java
recommender.userRecs(newUserId, 5); // returns empty array
```

There are a number of ways to deal with this, but here are some common ones:

- For user-based recommendations, show new users the most popular items
- For item-based recommendations, make content-based recommendations

## Reference

Get ids

```java
recommender.userIds();
recommender.itemIds();
```

Get the global mean

```java
recommender.globalMean();
```

Get factors

```java
recommender.userFactors(userId);
recommender.itemFactors(itemId);
```

## References

- [A Learning-rate Schedule for Stochastic Gradient Methods to Matrix Factorization](https://www.csie.ntu.edu.tw/~cjlin/papers/libmf/mf_adaptive_pakdd.pdf)
- [Faster Implicit Matrix Factorization](https://www.benfrederickson.com/fast-implicit-matrix-factorization/)

## History

View the [changelog](https://github.com/ankane/disco-java/blob/master/CHANGELOG.md)

## Contributing

Everyone is encouraged to help improve this project. Here are a few ways you can help:

- [Report bugs](https://github.com/ankane/disco-java/issues)
- Fix bugs and [submit pull requests](https://github.com/ankane/disco-java/pulls)
- Write, clarify, or fix documentation
- Suggest or add new features

To get started with development:

```sh
git clone https://github.com/ankane/disco-java.git
cd disco-java
mvn test
```
