/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.kerberos;

import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_KERBEROS;
import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_PROP;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@ConfigurationProperties(prefix = KerberosSecurityProperties.KERBEROS_PROPERTIES_PREFIX)
@ConditionalOnProperty(name = MODE_PROP, havingValue = MODE_KERBEROS)
@Data
@Validated
public class KerberosSecurityProperties {
    public static final String KERBEROS_PROPERTIES_PREFIX = "kerberos";

    @NotBlank
    private String realm;

    @NotBlank
    private String kdc;

    @NotBlank
    private String servicePrincipal;

    @NotNull
    private Resource keytabLocation;

    //    @NotNull
    private Resource krbConfLocation;

    private boolean debug;
}
