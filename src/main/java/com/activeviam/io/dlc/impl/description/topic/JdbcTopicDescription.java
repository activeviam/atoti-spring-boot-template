/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.io.dlc.impl.description.topic;

import static com.activeviam.io.dlc.api.description.topic.DlcTopicType.JDBC;

import java.util.List;
import java.util.Set;

import com.activeviam.io.dlc.api.description.IOverridableDescription;
import com.activeviam.io.dlc.api.description.topic.DlcTopicType;
import com.activeviam.io.dlc.api.description.topic.ILoadingTopicDescription;
import com.activeviam.io.dlc.api.description.topic.channel.IChannelDescription;
import com.activeviam.io.dlc.impl.properties.topic.JdbcTopicProperties;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * OVERRIDE THIS CLASS: this is in order to workaround the fact that Dremio does not yet support
 * prepared statements
 * Description of a topic that will retrieve data via JDBC connection into the datastore.
 */
@Builder(toBuilder = true)
public record JdbcTopicDescription(
        @NonNull String name,
        String sql,
        List<String> parameterOrder,
        @Singular Set<IChannelDescription> channels,
        Set<String> restrictToSources,
        Set<String> restrictFromSources)
        implements ILoadingTopicDescription {
    public JdbcTopicDescription {
        if (name.isEmpty()) throw new IllegalArgumentException("Topic name cannot be empty");
        if (sql != null && sql.isEmpty()) throw new IllegalArgumentException("SQL query cannot be empty");
    }

    /**
     * Default builder for jdbc topic.
     */
    public static JdbcTopicDescriptionBuilder builder(@NonNull String name, @NonNull String sql) {
        if (sql.isEmpty()) throw new IllegalArgumentException("SQL query cannot be empty");
        return new JdbcTopicDescriptionBuilder().name(name).sql(sql);
    }

    /**
     * Default builder for jdbc topic.
     */
    public static JdbcTopicDescriptionBuilder querylessBuilder(@NonNull String name) {
        return new JdbcTopicDescriptionBuilder().name(name);
    }

    @Override
    public DlcTopicType type() {
        return JDBC;
    }

    @Override
    public JdbcTopicDescription overrideWith(IOverridableDescription overridingDescription) {
        if (!(overridingDescription instanceof JdbcTopicDescription other)) {
            throw new IllegalArgumentException(
                    "Cannot merge different types of topics. Was provided: " + overridingDescription);
        }

        return new JdbcTopicDescription(
                this.name,
                other.sql == null ? this.sql : other.sql,
                other.parameterOrder == null ? this.parameterOrder : other.parameterOrder,
                other.channels == null || other.channels.isEmpty() ? this.channels : other.channels,
                other.restrictToSources == null ? this.restrictToSources : other.restrictToSources,
                other.restrictFromSources == null ? this.restrictFromSources : other.restrictFromSources);
    }

    public interface IJdbcTopicDescriptionFactory
            extends ITopicDescriptionFactory<JdbcTopicProperties, JdbcTopicDescription> {}
}
