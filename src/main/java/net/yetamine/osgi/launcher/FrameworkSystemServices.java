/*
 * Copyright 2019 Yetamine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.yetamine.osgi.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

/**
 * Provides common framework services registered to the system bundle.
 */
final class FrameworkSystemServices {

    /**
     * Name of the property that defines the PID of the service providing the
     * remaining launching arguments.
     */
    public static final String PROPERTY_ARGUMENT_SERVICE_PID = "arguments.service.pid";

    /**
     * Default value of {@link #PROPERTY_ARGUMENT_SERVICE_PID}.
     */
    public static final String DEFAULT_ARGUMENT_SERVICE_PID = "net.yetamine.osgi.launcher.arguments";

    private FrameworkSystemServices() {
        throw new AssertionError();
    }

    /**
     * Registers system services.
     *
     * @param framework
     *            the framework instance to configure. It must not be
     *            {@code null}.
     * @param configuration
     *            the configuration for the services. It must not be
     *            {@code null}.
     *
     * @throws BundleException
     *             if the operation failed
     */
    public static void register(Framework framework, Configuration configuration) throws BundleException {
        final String servicePid = argumentServicePid(configuration);
        if (servicePid.isEmpty()) {
            return;
        }

        final BundleContext systemBundle = framework.getBundleContext();

        if (systemBundle == null) {
            throw new BundleException("Could not register system services.");
        }

        final List<String> service = Collections.unmodifiableList(new ArrayList<>(configuration.parameters()));
        final Dictionary<String, String> serviceProperties = new Hashtable<>();
        serviceProperties.put(Constants.SERVICE_DESCRIPTION, "Application arguments provided to the launcher.");
        serviceProperties.put(Constants.SERVICE_PID, servicePid);
        systemBundle.registerService(List.class, service, serviceProperties);
    }

    private static String argumentServicePid(Configuration configuration) {
        final Map<String, String> launchingProperties = configuration.launchingProperties();
        return launchingProperties.getOrDefault(PROPERTY_ARGUMENT_SERVICE_PID, DEFAULT_ARGUMENT_SERVICE_PID);
    }
}
