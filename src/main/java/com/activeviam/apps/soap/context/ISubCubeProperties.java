/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap.context;

/**
 * The main component of the SubCube functionality.
 *
 * <p>The SubCube functionality aims to expose virtual sub-cubes.
 * When subcube properties are attached to an ActivePivot's instance
 * all queries return sub-views of the real cube.
 *
 * <p>SubCube properties store granted rights on an ActivePivot's instance.
 *
 * @see IContextValue
 * @author ActiveViam
 */
public interface ISubCubeProperties extends IContextValue {
    /**
     * PLUGIN_KEY_PREFIX
     */
    String PLUGIN_KEY_PREFIX = "SubCube-";

    /**
     * Returns true if this user can access to the pivot, false otherwise.
     *
     * @return true if this user can access to the pivot, false otherwise.
     */
    boolean isAccessGranted();
}
