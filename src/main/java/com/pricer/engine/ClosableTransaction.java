/*
 * Copyright (c) 2022. Vade Mecum Ltd. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.pricer.engine;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

@Slf4j
@Accessors(fluent = true)
public class ClosableTransaction implements AutoCloseable {
    private final EntityTransaction trans;
    @Getter
    private final EntityManager em;
    public ClosableTransaction(EntityManager em) {
        this.em = em;
        if(em.getTransaction().isActive())
            trans = null;
        else {
            trans = em.getTransaction();
            trans.begin();
        }
    }

    @Override
    public void close() {
        try {
            if(trans != null)
                trans.commit();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
