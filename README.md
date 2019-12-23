# net.yetamine.osgi.launcher

Yetamine OSGi Launcher, or YOFL for short, is a vendor-agnostic launcher for the OSGi Framework and OSGi-based applications.


## Introduction

Launching a framework instance is an easy task actually and it can be done in a few steps:

1. Find the `FrameworkFactory` using the `ServiceLoader` as defined in the OSGi specification.
2. Make the `Framework` instance using the `FrameworkFactory::newFramework` method.
3. Invoke `Framework::init` to initialize the framework instance.
4. Invoke `Framework::start` to actually launch the framework instance.

However, the launched framework instance contains just the system bundle, which does nothing alone.
The framework launcher must therefore take care of populating the framework instance with the application bundles, or it must take care at least of deploying the initial bundle set that could take the bootstrapping over and populate the framework with remaining bundles.

Populating the framework instance with bundles is a tedious task.
Framework vendors therefore often supply a launcher together with their framework implementations, so that the user could just let the launcher install the bundles.
However, the vendor-specific launchers differ, which brings a couple of points to consider:

* Vendor-specific launchers are usually specific for a particular framework implementation and therefore details like the bootstrap configuration can make changing the framework implementation more difficult.
* Some launchers may be more suitable for specific use cases than others and having more options could help to choose a better suited launcher for a particular use case.


## Overview

Let's have a look which YOFL's features might be interesting.

**Vendor-agnostic:** YOFL favours no particular OSGi Framework implementation.
It uses the specification-defined portable way to make a framework instance and uses no dirty hacks or proprietary extensions.

**Light and simple:** Large framework distributions provide many impressive features and services, but such a plethora of options might become a burden for small applications.
Instead of trying to shrink a large framework distribution for hosting such an application, it might be easier to build a small distribution from ground up.
YOFL can be used conveniently as the launcher.

**Flexible layout:** YOFL takes a number of arguments to learn what to do and what to use.
It supports sharing distribution parts scattered among various places on the filesystem.
Building an application/framework distribution with specific layout should be then easy.

**Bundle discovery:** YOFL can discover and configure multiple bundles from a filesystem easily.
Just tell where the bundles are located and optionally supply specific options, like the start level.
YOFL deploys and configures them accordingly.

**Bundle synchronization:** It is possible to configure one or more bundle sources, so that YOFL keeps the deployed bundle set on launch consistent with particular bundle sources.

**Basic process control:** Like any ordinary launcher, YOFL supports graceful framework shutdown and optional remote shutdown on receiving a cryptographically protected packet.

**Smaller and faster Docker images:** There are two points contributing to this goal.

* Firstly, as mentioned above, an application distribution can be build from ground up, so that it contains no features that the application does not need or use.
* Secondly, installing a bundle in a framework instance means usually that the framework copies the bundle binary to its storage area.
This allows using the bundle, until it is uninstalled, even if the original bundle location is not available anymore.
A naïvely built Docker image repeats the installation steps for every new container, duplicating the bundles in the container file system and prolonging the startup time.

Unlike most other launchers, YOFL distinguishes the *deploy* phase and the *start* phase and allows executing them separately.
This feature can be used conveniently with the Docker builder pattern:

1. Take a base Docker image with YOFL and use it as the builder.
2. Add the application distribution.
3. Run YOFL `deploy` command to deploy the application.
4. Create the final Docker image from the base image and the deployed application.

Then the final image contains just the pre-deployed application ready to run.


## Getting started

Just run following command to get the command reference:

```bash
java -jar yofl.jar help
```

The `help` command gives some idea what YOFL can do and how it could be used.
Unlike `help`, most YOFL commands require an OSGi framework implementation to operate with.
However, the launcher contains no OSGi framework implementation.
Instead, it assumes that the OSGi framework implementation (compatible with OSGi R5 or newer) to be launched is provided on the class path.
This allows using the launcher with any sound OSGi framework implementation with relative ease: the implementation must be supplied on the class path.

The scripts use an interface based on environment variables and pass all command line arguments directly to the launcher.
This approach seems to be more robust and portable than attempts to process command line arguments partially.
The details of all used environment variables are available in the script headers.
Here is provided just a brief listing:

* `JAVA`: The command to launch JVM, by default `java`.
* `JAVA_OPTS`: User-defined options passed to the `JAVA` command.
* `YOFL_AUTOPATH`: All `*.jar` files in this directory are appended to the class path.
* `YOFL_BOOTPATH`: The user-defined part of the class path.
* `YOFL_LOGGING_FILE`: The file name for the log, which the launcher prints to standard error output by default.
* `YOFL_LOGGING_LEVEL`: The threshold for log messages with the possible values `FORCE`, `ERROR` (the default), `WARN`, `INFO` and `DEBUG`.


With the help of the launch script the command to run an application can look like this:

```bash
YOFL_BOOTPATH="${that_framework_jar}" yofl launch --bundles "${here_are_the_bundles}" "${instance_home}"
```

Although this form might already be usable for use in scripts, it is still quite mouthful.
Check rather [ServiceBox](http://github.com/yetamine/servicebox), a ready-to-use distribution based on YOFL.


## Licensing ##

The project is licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). Contributions to the project are welcome and accepted if they can be incorporated without the need of changing the license or license conditions and terms.


[![Yetamine logo](https://github.com/yetamine/yetamine.github.io/raw/master/brand/light/Yetamine_logo_opaque_100x28.png "Our logo")](https://github.com/yetamine/yetamine.github.io/blob/master/brand/light/Yetamine_logo_opaque.svg)
