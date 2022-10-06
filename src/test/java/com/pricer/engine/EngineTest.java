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
import com.pricer.data.EmbeddedDataDto;
import com.pricer.data.TestData;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@Slf4j
abstract class EngineTest {
    static Engine engine;

    @Test
    void persistenceTest() {
        assertThat(engine.em).isNotNull();
        emptyData();
        saveData();

        assertThat(TestData.cnt("%", engine.em)).isGreaterThan(0);
        assertThat(TestData.all(engine.em)).isNotEmpty();

        TestData data = TestData.all(engine.em).get(0);
        assertThat(data).isNotNull();
        {
            //noinspection unused
            @Cleanup ClosableTransaction t = engine.openTransaction();
            data.someString("a new value");
        }
        assertThat(data.someString()).endsWith("a new value");
        assertThat(data.len()).isEqualTo("a new value".length());

    }

    private void emptyData() {
        @Cleanup ClosableTransaction trans = engine.openTransaction();
        List<TestData> data = TestData.all(trans.em());
        data.forEach(d -> trans.em().remove(d));
    }

    private void saveData() {
        @Cleanup ClosableTransaction trans = engine.openTransaction();
        TestData one = TestData.builder()
                .someString("test")
                .moreData("ing")
                .build();
        trans.em().persist(one);
        TestData two = TestData.builder()
                .someString("another")
                .moreData("thing")
                .build();
        trans.em().persist(two);
    }

    @Test
    void dtoTest() {
        // Empty the table
        {
            @Cleanup ClosableTransaction trans = engine.openTransaction();
            List<EmbeddedData> data = EmbeddedData.all(trans.em());
            data.forEach(d -> trans.em().remove(d));

        }

        // Create some data
        for(int i = 0; i < 10; i++) {
            @Cleanup ClosableTransaction trans = engine.openTransaction();
            EmbeddedData d = EmbeddedData.builder()
                    .id(EmbeddedData.CustomId.builder()
                            .k1("k1-" + i)
                            .k2("all the same")
                            .build())
                    .data1("data 1 value")
                    .data2("data 2")
                    .build();
            trans.em().persist(d);
        }
        List<EmbeddedData> all = EmbeddedData.all(engine.em);
        assertThat(all).isNotNull();
        assertThat(all).isNotEmpty();

        // Load the dto
        List<EmbeddedDataDto> allDto = new ArrayList<>();
        for(EmbeddedData d : all) {
            allDto.add(d.getDto());
        }

        // Edit the dto's
        for(EmbeddedDataDto d : allDto) {
            d.data3("changed");
        }

        // Save them
        {
            @Cleanup ClosableTransaction trans = engine.openTransaction();
            for(EmbeddedDataDto dtoData : allDto) {
                EmbeddedData dbData = trans.em().find(EmbeddedData.class, dtoData.id());
                assertThat(dbData).isNotNull();
                dbData.update(dtoData);
            }

        }

        // Validate results
        for(EmbeddedData d : EmbeddedData.all(engine.em)) {
            assertThat(d.data3()).isEqualTo("changed");
        }

        // cnt

        assertThat(EmbeddedData.cnt(engine.em)).isEqualTo(10);
    }
}