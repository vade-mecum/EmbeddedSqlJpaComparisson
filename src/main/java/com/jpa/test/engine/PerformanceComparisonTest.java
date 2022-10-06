/*
 * Copyright (c) 2022. Vade Mecum Ltd. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.jpa.test.engine;

import com.jpa.test.data.EmbeddedData;
import com.jpa.test.util.ClosableTransaction;
import lombok.Builder;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;


@Slf4j
@Builder
public class PerformanceComparisonTest {
    static final int WARMUP_ITERATIONS = 1000;
    static final String[] SPINNER = {"-", "\\", "|", "/", "-", "\\", "|", "/"};
    int spinnerPos;
    String spinnerLabel;
    final int iterations;
    final int objects;
    final int querySize;
    static final String[] DBS = {"sqlite-test", "h2-test", "hsqldb-test"};
    final HashMap<String, Engine> engines = new HashMap<>();
    final TreeMap<String, Long> results = new TreeMap<>();

    private void doSpinner(String label) {
        if(spinnerLabel == null || (! spinnerLabel.equals(label) && label.length() > 0)) {
            if(spinnerLabel != null)
                System.out.print("\b ... complete\n");
            System.out.print(label + ": ");
            spinnerLabel = label;
        }
        String ln = "\b" + SPINNER[spinnerPos++];
        spinnerPos = spinnerPos % SPINNER.length;
        System.out.print(ln);
    }

    /**
     * Main to run the performance tests
     */
    public static void main(String[] args) {
        int objects = 10;
        int iterations = 1000;
        int querySize = 1000;

        Options options = new Options();
        options.addOption(Option.builder()
                .option("n")
                .longOpt("iterations")
                .hasArg()
                .optionalArg(true)
                .desc("loop iterations (1000)").build());
        options.addOption(Option.builder()
                .option("o")
                .hasArg()
                .longOpt("objects")
                .optionalArg(true)
                .desc("object count (10)").build());
        options.addOption(Option.builder()
                .option("q")
                .hasArg()
                .longOpt("query")
                .optionalArg(true)
                .desc("query size (1000)").build());
        try {
            DefaultParser parser = DefaultParser.builder().build();
            CommandLine cmd = parser.parse(options, args);
            if(cmd.hasOption("n"))
                iterations = Integer.parseInt(cmd.getOptionValue("n"));
            if(cmd.hasOption("o"))
                objects = Integer.parseInt(cmd.getOptionValue("o"));
            if(cmd.hasOption("q"))
                querySize = Integer.parseInt(cmd.getOptionValue("q"));

        } catch (Exception e) {
            HelpFormatter helper = new HelpFormatter();
            System.out.println(e.getMessage());
            helper.printHelp("Usage:", options);
            return;
        }

        PerformanceComparisonTest test = PerformanceComparisonTest.builder()
                .iterations(iterations)
                .objects(objects)
                .querySize(querySize)
                .build();
        test.warmup();
        test.insertTest();
        test.updateTest();
        test.bulkUpdateTest();
        test.queryTest();
        test.queryAllTest();
        test.cleanup();
    }

    /**
     * Warmup the DB's
     */
    void warmup() {
        for (String db : DBS) {
            engines.put(db, new Engine(db));
        }

        for (String db : DBS) {
            Engine engine = engines.get(db);
            doSpinner("Warmup");
            empty(engine);
            load(engine, WARMUP_ITERATIONS, false);
            doSpinner("Warmup");
            empty(engine);
        }
    }

    /**
     * Cleanup and print results
     */
    void cleanup() {
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

    /**
     * Insert rows, one transaction per insert
     */
    void insertTest() {
        for (String db : DBS) {
            doSpinner("Insert " + db);
            Engine engine = engines.get(db);
            empty(engine);

            long start_t = System.nanoTime();
            load(engine, iterations, false);
            long end_t = System.nanoTime();
            results.put("Insert " + db, end_t - start_t);
        }
    }

    /**
     * Query single items by key
     */
    void queryTest() {
        for (String db : DBS) {
            doSpinner("Query " + db);
            Engine engine = engines.get(db);
            empty(engine);
            load(engine,objects, true);

            long start_t = System.nanoTime();
            query(engine,objects, iterations);
            long end_t = System.nanoTime();
            results.put("Query One " + db, end_t - start_t);
        }

    }

    /**
     * Query the full contents of the table
     */
    void queryAllTest() {
        for (String db : DBS) {
            doSpinner("Query All " + db);
            Engine engine = engines.get(db);
            empty(engine);
            load(engine, querySize, true);

            long start_t = System.nanoTime();
            queryAll(engine, iterations, querySize);
            long end_t = System.nanoTime();
            results.put("Query All " + db, end_t - start_t);
        }

    }

    /**
     * Update items, one per transaction
     */
    void updateTest() {
        for (String db : DBS) {
            doSpinner("Update " + db);
            Engine engine = engines.get(db);
            empty(engine);
            load(engine,objects, true);

            long start_t = System.nanoTime();
            update(engine,objects, iterations /objects, true);
            long end_t = System.nanoTime();
            results.put("Update " + db, end_t - start_t);
        }

    }

    /**
     * Update items, chunks per transaction
     */
    void bulkUpdateTest() {
        for (String db : DBS) {
            Engine engine = engines.get(db);
            doSpinner("Bulk Update " + db);
            empty(engine);
            load(engine,objects, true);

            long start_t = System.nanoTime();
            update(engine,objects, iterations /objects, false);
            long end_t = System.nanoTime();
            results.put("Bulk Update " + db, end_t - start_t);
        }

    }

    /*
     * Empty the table contents
     */
    static void empty(Engine engine) {
        @Cleanup ClosableTransaction trans = engine.openTransaction();
        List<EmbeddedData> data = EmbeddedData.all(trans.em());
        data.forEach(d -> trans.em().remove(d));
    }

    /*
     * Loader, one or bulk
     */
    void load(Engine engine, int cnt, boolean oneTran) {
        @Cleanup ClosableTransaction outer = oneTran ? engine.openTransaction() : null;
        for (int i = 0; i < cnt; i++) {
            if( i % 100 == 0)
                doSpinner("");
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

    /*
     * Query as list
     */
    void queryAll(Engine engine, int loops, int expected) {
        for (int i = 0; i < loops; i++) {
            if(i % 100 == 0)
                doSpinner("");
            Collection<EmbeddedData> data = EmbeddedData.all(engine.em);
            assert(data.size()  == expected);
        }
    }

    /*
     * Query one
     */
    void query(Engine engine, int cnt, int loops) {
        for (int i = 0; i < loops; i++) {
            doSpinner("");
            for (int j = 0; j < cnt; j++) {
                EmbeddedData data = engine.em.find(EmbeddedData.class,
                        EmbeddedData.CustomId.builder()
                                .k1("key-" + j)
                                .k2("val2")
                                .build()
                );
                assert(data != null);
            }
        }
    }

    /*
     * Update, one or bulk
     */
    static void update(Engine engine, int cnt, int loops, boolean transactionPerUpdate) {
        for (int i = 0; i < loops; i++) {
            //noinspection unused
            @Cleanup ClosableTransaction outer = transactionPerUpdate ? null : engine.openTransaction();
            for (int j = 0; j < cnt; j++) {
                //noinspection unused
                @Cleanup ClosableTransaction trans = transactionPerUpdate ? engine.openTransaction() : null;
                EmbeddedData data = engine.em.find(EmbeddedData.class,
                        EmbeddedData.CustomId.builder()
                                .k1("key-" + j)
                                .k2("val2")
                                .build()
                );
                assert(data != null);
                data.data3("data" + Math.random());
            }
        }
    }
}