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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.yetamine.osgi.launcher.deploying.BundleState;
import net.yetamine.osgi.launcher.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Implements detailed status printing.
 */
final class StatusFormatter {

    private StatusFormatter() {
        throw new AssertionError();
    }

    /**
     * Dumps the status.
     *
     * @param logger
     *            the logger to dump the status to. It must not be {@code null}.
     * @param runtime
     *            the runtime. It must not be {@code null}.
     * @param configuration
     *            the configuration. It must not be {@code null}.
     */
    public static void dump(Logger logger, InstanceRuntime runtime, Configuration configuration) {
        if (configuration.dumpStatus()) {
            final StringPrinter result = new StringPrinter();
            formatInstance(result, runtime, configuration);
            formatBundles(result, runtime);
            formatServices(result, runtime);
            logger.log(Logger.Level.FORCE, result.toString(), null);
        } else {
            logger.debug(log -> log.debug(formatListing(new StringPrinter(), runtime).toString()));
        }

        // Print at the end, so there is a quick visual check of the status
        logger.info(log -> log.info(formatSummary(runtime, configuration)));
    }

    private static String formatSummary(InstanceRuntime runtime, Configuration configuration) {
        final Framework framework = runtime.framework();

        final String startLevel = Optional.ofNullable(framework.adapt(FrameworkStartLevel.class))
                .map(sl -> Integer.toString(sl.getStartLevel())) // Make the conversion right now
                .orElse("N/A");

        final BundleContext systemBundle = framework.getBundleContext();

        if (systemBundle == null) {
            return "Framework at start level " + startLevel + ", not active.";
        }

        // There should be at least one bundle: the system bundle
        final List<Bundle> bundles = lookupAllBundles(runtime);

        // @formatter:off
        return bundles.stream()
                .collect(Collectors.groupingBy(BundleState::of, TreeMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .map(StatusFormatter::formatBundleState)
                .collect(Collectors.joining(
                    ", ",
                    "Framework at start level " + startLevel + " with total " + bundles.size() + " bundle(s): ",
                    "."));
        // @formatter:on
    }

    private static String formatBundleState(Map.Entry<BundleState, Long> entry) {
        return Long.toString(entry.getValue()) + ' ' + entry.getKey().toString().toLowerCase(Locale.ROOT);
    }

    private static StringPrinter formatInstance(StringPrinter result, InstanceRuntime runtime, Configuration configuration) {
        result.println("Configuration for instance: " + configuration.instance());
        result.println();

        formatProperties(result, "System properties", configuration.systemProperties());
        formatProperties(result, "Launching properties", configuration.launchingProperties());
        formatProperties(result, "Framework properties", runtime.properties()); // Use the effective ones

        return result;
    }

    private static StringPrinter formatBundles(StringPrinter result, InstanceRuntime runtime) {
        final List<Bundle> bundles = lookupAllBundles(runtime);

        result.println("Bundle listing:");

        if (bundles.isEmpty()) {
            return result.println("(no bundles available)").println();
        }

        result.println();
        for (Bundle bundle : bundles) {
            result.println(formatBundleSummary(bundle, 0));
            result.println("Registered services:");
            formatServices(result, list(bundle.getRegisteredServices()));
            result.println("Services in use:");
            formatServices(result, list(bundle.getServicesInUse()));
            result.println("---");
        }

        return result;
    }

    private static StringPrinter formatServices(StringPrinter result, Collection<ServiceReference<?>> services) {
        if (services.isEmpty()) {
            return result.println("(no services available)").println();
        }

        // @formatter:off
        services.stream()
            .map(ServiceSummary::new)
            .sorted()
            .map(ServiceSummary::toString)
            .forEachOrdered(result::println);
        // @formatter:on

        result.println();
        return result;
    }

    private static StringPrinter formatServices(StringPrinter result, InstanceRuntime runtime) {
        final List<ServiceReference<?>> services = lookupAllServices(runtime);
        if (services.isEmpty()) {
            return result.println("(no services available)").println();
        }

        result.println("Service listing:");
        result.println();
        services.stream().map(ServiceSummary::new).sorted().forEachOrdered(summary -> {
            result.println(summary.toString());
            result.println();

            final ServiceReference<?> service = summary.service();

            // @formatter:off
            result.println(list(service.getUsingBundles()).stream()
                    .mapToLong(Bundle::getBundleId)
                    .mapToObj(Long::toString)
                    .collect(Collectors.joining(", ", "Using bundles: [", "]"))
                );
            // @formatter:on
            result.println();
            formatProperties(result, service);
            result.println("---");
        });

        return result;
    }

    private static StringPrinter formatListing(StringPrinter result, InstanceRuntime runtime) {
        final List<Bundle> bundles = lookupAllBundles(runtime);

        result.println("Bundle status overview:");

        if (bundles.isEmpty()) {
            return result.println("(no bundles available)").println();
        }

        result.println();
        final int column = decimalOrder(bundles.size());
        bundles.stream().map(bundle -> formatBundleSummary(bundle, column)).forEachOrdered(result::print);
        return result;
    }

    private static StringPrinter formatProperties(StringPrinter result, ServiceReference<?> service) {
        result.println("Service properties:");

        final String[] keys = service.getPropertyKeys();
        if (keys.length == 0) {
            return result.println("(no properties available)").println();
        }

        list(keys).stream().sorted().forEachOrdered(key -> {
            result.format("%s=%s%n", key, toUsefulString(service.getProperty(key)));
        });

        result.println();
        return result;
    }

    private static StringPrinter formatProperties(StringPrinter result, String headline, Map<?, ?> properties) {
        result.format("%s:%n", headline);

        if (properties.isEmpty()) {
            return result.println("(no properties available)").println();
        }

        properties.forEach((name, value) -> result.format("%s=%s%n", name, value));
        result.println();
        return result;
    }

    private static List<ServiceReference<?>> lookupAllServices(InstanceRuntime runtime) {
        // @formatter:off
        return Optional.ofNullable(runtime.framework().getBundleContext())
                .map(StatusFormatter::lookupAllServices)
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList);
        // @formatter:on
    }

    private static ServiceReference<?>[] lookupAllServices(BundleContext context) {
        try {
            return context.getAllServiceReferences(null, null);
        } catch (InvalidSyntaxException e) { // Impossible with null filter
            throw new AssertionError(e);
        }
    }

    private static List<Bundle> lookupAllBundles(InstanceRuntime runtime) {
        // @formatter:off
        return Optional.ofNullable(runtime.framework().getBundleContext())
                .map(BundleContext::getBundles)
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList);
        // @formatter:on
    }

    private static String formatBundleSummary(Bundle bundle, int column) {
        final String bundleId = Long.toString(bundle.getBundleId());
        final int extraPadding = Math.max(0, column - bundleId.length());

        // @formatter:off
        return new StringPrinter()
                .with(output -> padding(' ', extraPadding, output))
                .format("#%s [%s] %s @ %s",
                    bundleId,
                    BundleState.of(bundle),
                    bundle.getSymbolicName(),
                    bundle.getVersion())
                .println()
                .with(output -> padding(' ', bundleId.length() + extraPadding, output)
                    .append("  ") // Padding for the hanging part and the separating space
                    .append(bundle.getLocation()))
                .println()
                .toString();
        // @formatter:on
    }

    private static StringBuilder padding(char character, int count, StringBuilder result) {
        for (int i = 0; i < count; i++) {
            result.append(' ');
        }

        return result;
    }

    private static String toUsefulString(Object o) {
        if (o == null) {
            return "";
        }

        final Class<?> clazz = o.getClass();

        // @formatter:off
        return clazz.isArray()
                ? IntStream.range(0, Array.getLength(o))
                        .mapToObj(i -> Array.get(o, i))
                        .map(Objects::toString)
                        .collect(Collectors.joining(", ", "[", "]"))
                        : o.toString();
        // @formatter:on
    }

    private static int decimalOrder(int number) {
        int result = 1;

        assert (0 <= number); // Good enough locally, otherwise take care of MIN_VALUE
        for (int current = number; 10 <= current; current /= 10) {
            ++result;
        }

        return result;
    }

    private static <T> List<T> list(T[] array) {
        return (array != null) ? Arrays.asList(array) : Collections.emptyList();
    }

    /**
     * Helps with formatting services, especially with their sorting by the
     * <i>service.id</i> rather than the natural order that prefers ranking.
     */
    private static final class ServiceSummary implements Comparable<ServiceSummary> {

        private final ServiceReference<?> service;
        private final Long pid;

        public ServiceSummary(ServiceReference<?> serviceReference) {
            pid = (Long) serviceReference.getProperty(Constants.SERVICE_ID); // Must exist
            service = serviceReference;
        }

        @Override
        public String toString() {
            // Rather print the service in the common form (e.g., Equinox prints it as a Map)
            final String[] objectClass = (String[]) service.getProperty(Constants.OBJECTCLASS);
            return String.format("#%s %s", pid, Arrays.toString(objectClass));
        }

        @Override
        public int compareTo(ServiceSummary o) {
            return pid.compareTo(o.pid);
        }

        public ServiceReference<?> service() {
            return service;
        }
    }
}
