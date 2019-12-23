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

/**
 * Declares constants describing the layout of an instance.
 */
final class InstanceLayout {

    /** Instance path where the configuration data are located. */
    public static final String CONF_PATH = "conf";
    /** Instance path where the framework data are located. */
    public static final String DATA_PATH = "data";
    /** Instance path where applied properties are stored. */
    public static final String ETC_PATH = "etc";

    /** Name of the file storing the framework properties used for start. */
    public static final String FRAMEWORK_PROPERTIES = "framework.properties";
    /** Name of the file storing the launching properties used for start. */
    public static final String LAUNCHING_PROPERTIES = "launching.properties";
    /** Name of the file storing the system properties used for start. */
    public static final String SYSTEM_PROPERTIES = "system.properties";

    /** Name of the file storing the command link parameters. */
    public static final String COMMAND_LINK_FILE = "instance.link";

    private InstanceLayout() {
        throw new AssertionError();
    }
}
