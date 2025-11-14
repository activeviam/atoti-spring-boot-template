/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlRootElement
@XmlType(name = "cellSet")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@EqualsAndHashCode
@ToString
public class CellSetDTO implements Externalizable {
    @Serial
    private static final long serialVersionUID = 201510271649L;

    private List<String> cellProperties;

    private static void writeNullableStrings(ObjectOutput out, List<String> list) throws IOException {
        if (list == null) {
            out.writeInt(0);
        } else {
            out.writeInt(list.size());

            for (String elt : list) {
                out.writeUTF(elt);
            }
        }
    }

    private static List<String> readNullableStrings(ObjectInput in) throws IOException {
        var size = in.readInt();
        if (size == 0) {
            return null;
        } else {
            List<String> result = new ArrayList<>(size);
            for (var i = 0; i < size; ++i) {
                result.add(in.readUTF());
            }
            return result;
        }
    }

    @XmlElementWrapper(name = "cellProperties")
    @XmlElement(name = "p")
    public List<String> getCellProperties() {
        return cellProperties;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        writeNullableStrings(out, cellProperties);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        cellProperties = readNullableStrings(in);
    }
}
