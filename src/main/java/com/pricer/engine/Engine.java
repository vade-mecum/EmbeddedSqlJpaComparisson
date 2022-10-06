/*
 * Copyright (c) 2022. Vade Mecum Ltd. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.pricer.engine;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class Engine implements AutoCloseable {
    EntityManager em;
    EntityManagerFactory emf;

    public Engine(String persistenceUnit) {
        emf = Persistence.createEntityManagerFactory(persistenceUnit);
        em = emf.createEntityManager();
    }

    public void close() {
        em.close();
        em = null;
        emf.close();
        emf = null;
    }

    public ClosableTransaction openTransaction() {
        return new ClosableTransaction(em);
    }

}
