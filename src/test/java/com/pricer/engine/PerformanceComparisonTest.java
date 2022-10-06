/*
 * Copyright (c) 2022. Vade Mecum Ltd. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.pricer.engine;

import com.pricer.data.EmbeddedData;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import static com.google.common.truth.Truth.assertThat;

@SuppressWarnings("SameParameterValue")
@Slf4j
public class PerformanceComparisonTest {
    static final int ITERATIONS = 1000;
    static final String[] DBS = {"sqlite-test", "h2-test", "hsqldb-test"};
    static HashMap<String, Engine> engines = new HashMap<>();
    static TreeMap<String, Long> results = new TreeMap<>();

    @BeforeAll
    static void warmup() {
        for (String db : DBS) {
            engines.put(db, new Engine(db));
        }

        for (String db : DBS) {
            Engine engine = engines.get(db);
            load(engine, ITERATIONS, false);
            empty(engine);
        }
        log.info("Warmup complete");

    }

    @AfterAll
    static void cleanup() {
        for (String db : DBS) {
            engines.get(db).close();
        }

        log.info("==================");
        for (String k : results.keySet()) {
            log.info(k + ": " + results.get(k) / 1000000.0);
        }
        log.info(" ---- totals ----");
        for (String db : DBS) {
            long total = 0;
            for (String k : results.keySet()) {
                if (k.endsWith(db))
                    total += results.get(k);
            }
            log.info(db + ": " + total / 1000000.0);
        }
        log.info("==================");
    }

    @Test
    void insertTest() {
        for (String db : DBS) {
            Engine engine = engines.get(db);
            empty(engine);

            long start_t = System.nanoTime();
            load(engine, ITERATIONS, false);
            long end_t = System.nanoTime();
            results.put("Insert " + db, end_t - start_t);
        }
    }

    @Test
    void queryTest() {
        for (String db : DBS) {
            Engine engine = engines.get(db);
            empty(engine);
            load(engine, 10, true);

            long start_t = System.nanoTime();
            query(engine, 10, ITERATIONS);
            long end_t = System.nanoTime();
            results.put("Query One " + db, end_t - start_t);
        }

    }

    @Test
    void queryAllTest() {
        for (String db : DBS) {
            Engine engine = engines.get(db);
            empty(engine);
            load(engine, 1000, true);

            long start_t = System.nanoTime();
            queryAll(engine, 1000, ITERATIONS);
            long end_t = System.nanoTime();
            results.put("Query All " + db, end_t - start_t);
        }

    }

    @Test
    void updateTest() {
        for (String db : DBS) {
            Engine engine = engines.get(db);
            empty(engine);
            load(engine, 10, true);

            long start_t = System.nanoTime();
            update(engine, 10, ITERATIONS / 10);
            long end_t = System.nanoTime();
            results.put("Update " + db, end_t - start_t);
        }

    }

    static void empty(Engine engine) {
        @Cleanup ClosableTransaction trans = engine.openTransaction();
        List<EmbeddedData> data = EmbeddedData.all(trans.em());
        data.forEach(d -> trans.em().remove(d));
    }

    static void load(Engine engine, int cnt, boolean oneTran) {
        @Cleanup ClosableTransaction outer = oneTran ? engine.openTransaction() : null;
        for (int i = 0; i < cnt; i++) {
            @Cleanup ClosableTransaction inner = oneTran ? null : engine.openTransaction();
            EmbeddedData d = EmbeddedData.builder()
                    .id(EmbeddedData.CustomId.builder()
                            .k1("key-" + i)
                            .k2("val2")
                            .build())
                    .data1("data 1")
                    .data2("data 2")
                    .build();
            if (outer != null)
                outer.em().persist(d);
            else if (inner != null)
                inner.em().persist(d);
        }
    }

    static void queryAll(Engine engine, int loops, int expected) {
        for (int i = 0; i < loops; i++) {
            Collection<EmbeddedData> data = EmbeddedData.all(engine.em);
            assertThat(data).hasSize(expected);
        }
    }

    static void query(Engine engine, int cnt, int loops) {
        for (int i = 0; i < loops; i++) {
            for (int j = 0; j < cnt; j++) {
                EmbeddedData data = engine.em.find(EmbeddedData.class,
                        EmbeddedData.CustomId.builder()
                                .k1("key-" + j)
                                .k2("val2")
                                .build()
                );
                assertThat(data).isNotNull();
            }
        }
    }

    static void update(Engine engine, int cnt, int loops) {
        for (int i = 0; i < loops; i++) {
            for (int j = 0; j < cnt; j++) {
                //noinspection unused
                @Cleanup ClosableTransaction trans = engine.openTransaction();
                EmbeddedData data = engine.em.find(EmbeddedData.class,
                        EmbeddedData.CustomId.builder()
                                .k1("key-" + j)
                                .k2("val2")
                                .build()
                );
                assertThat(data).isNotNull();
                data.data3("data" + Math.random());
            }
        }
    }
}