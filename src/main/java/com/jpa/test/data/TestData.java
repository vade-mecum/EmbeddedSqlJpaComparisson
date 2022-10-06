/*
 * Copyright (c) 2022. Vade Mecum Ltd. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.jpa.test.data;

import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "test_data")
@NamedQueries({
        @NamedQuery(name = "TestData.countBySomeString", query = "select count(t) from TestData t where upper(t.someString) like upper(:someString)"),
        @NamedQuery(name = "TestData.findAll", query = "select t from TestData t")
})
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Accessors(fluent = true)
@ToString
public class TestData {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    @Getter@Setter
    private Integer id;

    @Getter
    @ToString.Include
    private String someString;
    public void someString(String newString) {
        someString = newString;
        postLoad();
    }
    @Getter@Setter
    private String moreData;

    @Getter
    private transient int len;

    @PostLoad
    void postLoad() {
        len = someString.length();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        TestData testData = (TestData) o;
        return id != null && Objects.equals(id, testData.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static long cnt(String likeSomeString, EntityManager em) {
        TypedQuery<Long> q = em.createNamedQuery("TestData.countBySomeString", Long.class);
        q.setParameter("someString", likeSomeString);
        return q.getSingleResult();
    }

    static public List<TestData> all(EntityManager em) {
        TypedQuery<TestData> query = em.createNamedQuery("TestData.findAll", TestData.class);
        return query.getResultList();
    }
}