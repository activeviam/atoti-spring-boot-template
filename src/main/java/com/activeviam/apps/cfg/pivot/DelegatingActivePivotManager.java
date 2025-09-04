/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import com.activeviam.activepivot.core.impl.internal.cube.IInternalActivePivotSchema;
import com.activeviam.activepivot.core.impl.internal.impl.ActivePivotManagerRebuilder;
import com.activeviam.activepivot.core.impl.internal.pivot.IInternalActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.ICatalog;
import com.activeviam.activepivot.core.intf.api.cube.IMultiVersionActivePivot;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.internal.cube.IInternalMultiVersionActivePivot;
import com.activeviam.activepivot.core.intf.internal.structure.action.listener.IStructuralTransactionListener;
import com.activeviam.tech.core.api.agent.AgentException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DelegatingActivePivotManager implements IInternalActivePivotManager {

    private final Supplier<IInternalActivePivotManager> underlyingManager;

    private IInternalActivePivotManager getUnderlyingManager() {
        return underlyingManager.get();
    }

    @Override
    public String getName() {
        return getUnderlyingManager().getName();
    }

    @Override
    public String getActivePivotVersion() {
        return getUnderlyingManager().getActivePivotVersion();
    }

    @Override
    public Map<String, ICatalog> getCatalogs() {
        return getUnderlyingManager().getCatalogs();
    }

    @Override
    public Map<String, IMultiVersionActivePivot> getActivePivots() {
        return getUnderlyingManager().getActivePivots();
    }

    @Override
    public IActivePivotManagerDescription getDescription() {
        return getUnderlyingManager().getDescription();
    }

    @Override
    public void init(Properties props) throws AgentException {
        getUnderlyingManager().init(props);
    }

    @Override
    public Properties getProperties() {
        return getUnderlyingManager().getProperties();
    }

    @Override
    public State getStatus() {
        return getUnderlyingManager().getStatus();
    }

    @Override
    public void start() throws AgentException {
        getUnderlyingManager().start();
    }

    @Override
    public void pause() throws AgentException {
        getUnderlyingManager().pause();
    }

    @Override
    public void resume() throws AgentException {
        getUnderlyingManager().resume();
    }

    @Override
    public void stop() throws AgentException {
        getUnderlyingManager().stop();
    }

    @Override
    public String getType() {
        return getUnderlyingManager().getType();
    }

    @Override
    public IInternalMultiVersionActivePivot getActivePivot(String id) {
        return getUnderlyingManager().getActivePivot(id);
    }

    @Override
    public Map<String, ? extends IInternalActivePivotSchema> getSchemas() {
        return getUnderlyingManager().getSchemas();
    }

    @Override
    public IStructuralTransaction startStructureUpdate() {
        return getUnderlyingManager().startStructureUpdate();
    }

    @Override
    public void registerStructuralTransactionListener(IStructuralTransactionListener listener) {
        getUnderlyingManager().registerStructuralTransactionListener(listener);
    }

    @Override
    public void unregisterStructuralTransactionListener(IStructuralTransactionListener listener) {
        getUnderlyingManager().unregisterStructuralTransactionListener(listener);
    }

    @Override
    public ActivePivotManagerRebuilder createRebuilder() {
        return getUnderlyingManager().createRebuilder();
    }
}
