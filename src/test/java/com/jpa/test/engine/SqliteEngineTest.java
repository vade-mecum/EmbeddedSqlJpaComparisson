/*
 * Copyright (c) 2022. Vade Mecum Ltd. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.jpa.test.engine;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class SqliteEngineTest extends EngineTest{
    @BeforeAll
    static void setUp() {
        engine = new Engine("sqlite-test");
    }
    @AfterAll
    static void tearDown() {
        engine.close();
    }
}
