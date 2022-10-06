/*
 * Copyright (c) 2022. Vade Mecum Ltd. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.pricer.data;

import lombok.*;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "embedded_data")
@NamedQueries({
        @NamedQuery(name = "EmbeddedData.countBy", query = "select count(e) from EmbeddedData e"),
        @NamedQuery(name = "EmbeddedData.findAll", query = "select e from EmbeddedData e")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class EmbeddedData {
    @EmbeddedId
    @Getter
    private CustomId id;

    @Builder.Default
    @Getter@Setter
    private String data1 = "";
    @Builder.Default
    @Getter@Setter
    private String data2 = "";
    @Builder.Default
    @Getter@Setter
    private String data3 = "";

    @Embeddable
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    @Accessors(fluent = true)
    public static class CustomId implements Serializable {
        @Serial
        private static final long serialVersionUID = -5006569953680128953L;
        @Getter
        private String k1;
        @Getter
        private String k2;
    }

    public static long cnt(EntityManager em) {
        TypedQuery<Long> query = em.createNamedQuery("EmbeddedData.countBy", Long.class);
        return query.getSingleResult();
    }

    public EmbeddedDataDto getDto() {
        return EmbeddedDataDto.builder()
                .id(id)
                .data1(data1)
                .data2(data2)
                .data3(data3)
                .build();
    }

    public void update(EmbeddedDataDto newVersion) {
        data1(newVersion.data1())
                .data2(newVersion.data2())
                .data3(newVersion.data3());
    }

    static public List<EmbeddedData> all(EntityManager em) {
        TypedQuery<EmbeddedData> query = em.createNamedQuery("EmbeddedData.findAll", EmbeddedData.class);
        return query.getResultList();
    }
}