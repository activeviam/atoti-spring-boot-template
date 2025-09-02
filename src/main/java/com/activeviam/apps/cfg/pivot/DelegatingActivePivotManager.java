/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import java.util.Map;
import java.util.Properties;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotSchema;
import com.activeviam.activepivot.core.intf.api.cube.ICatalog;
import com.activeviam.activepivot.core.intf.api.cube.IMultiVersionActivePivot;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.tech.core.api.agent.AgentException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DelegatingActivePivotManager implements IActivePivotManager {
    private final DelegatingApplication application;

    private IActivePivotManager getInternalManager() {
        return application.getManager();
    }

    @Override
    public String getName() {
        return getInternalManager().getName();
    }

    @Override
    public String getActivePivotVersion() {
        return getInternalManager().getActivePivotVersion();
    }

    @Override
    public Map<String, ? extends IActivePivotSchema> getSchemas() {
        return getInternalManager().getSchemas();
    }

    @Override
    public Map<String, ICatalog> getCatalogs() {
        return getInternalManager().getCatalogs();
    }

    @Override
    public Map<String, IMultiVersionActivePivot> getActivePivots() {
        return getInternalManager().getActivePivots();
    }

    @Override
    public IActivePivotManagerDescription getDescription() {
        return getInternalManager().getDescription();
    }

    @Override
    public void init(Properties props) throws AgentException {
        getInternalManager().init(props);
    }

    @Override
    public Properties getProperties() {
        return getInternalManager().getProperties();
    }

    @Override
    public State getStatus() {
        return getInternalManager().getStatus();
    }

    @Override
    public void start() throws AgentException {
        getInternalManager().start();
    }

    @Override
    public void pause() throws AgentException {
        getInternalManager().pause();
    }

    @Override
    public void resume() throws AgentException {
        getInternalManager().resume();
    }

    @Override
    public void stop() throws AgentException {
        getInternalManager().stop();
    }

    @Override
    public String getType() {
        return getInternalManager().getType();
    }
}
