/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap;

/**
 *
 * Pair class used to preserve the original index after sorting
 *
 */
public record Pair(int index, double value) implements Comparable<Pair> {
    @Override
    public int compareTo(Pair other) {
        return Double.compare(value, other.value);
    }
}
