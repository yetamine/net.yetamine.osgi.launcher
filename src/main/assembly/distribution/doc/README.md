# Yetamine OSGi Framework Launcher

Yetamine OSGi Launcher, or YOFL for short, is a vendor-neutral launcher for the [OSGi](http://www.osgi.org/) Framework and OSGi-based applications.


## Introduction

Launching a framework instance is not a difficult task, thanks to the specification-defined support for framework launching and embedding, but it still requires a non-trivial effort, especially when taking into account that launching the framework instance must include deploying at least the initial set of bundles.

A launcher therefore must:

* Prepare the framework deployment area and configure the environment if necessary.
* Deploy application bundles, or at least an initial bundle set that takes the bootstrapping process over and deploys the application bundles later.
* Start the framework instance.
* Take care of a graceful shutdown of the framework instance (at least the usual system signals should be handled, some launchers provide even the possibility of remote triggers).

YOFL performs all these tasks according to the instructions given by the command line arguments and configuration files, which are specified in the arguments.


## Getting started

Run following command to see the help (assuming that Java is installed):

```bash
java -jar yofl.jar help
```

Besides `help` YOFL offers several commands to perform:

* deploying a framework instance,
* starting a deployed instance,
* stopping a running instance remotely.

The deploy and start phases can be invoked even separately, which can be very useful for creating smaller Docker images with pre-deployed instances and for speeding up an instance start.

However, most of the commands require a framework implementation available on the class path, so that YOFL could find it and use it.
On one hand, providing the framework implementation on the class path allows to use any OSGi-compliant framework implementation.
On the other hand, providing the class path and possibly other Java command line options can be come tedious.
Bundled launch scripts for Windows and for Unix-based systems make running the launcher easier.


## Launch scripts

The scripts use an interface based on environment variables and pass all command line arguments directly to the launcher.
This approach seems to be more robust and portable than attempts to process command line arguments partially.

On the other hand, using environment variables impose a risk that must be understood and taken into account: some of the variables determine the code to execute in the future, therefore never run the scripts without controlling the variables.

### Environment variables

**`JAVA`:**
The command to launch JVM.
Default assumes `java` command available from the `PATH` environment variable.

**`JAVA_OPTS`:**
Additional Java runtime options passed to the `JAVA` command.
This variables must contain only arguments that need no shell escaping as it expands to multiple arguments and shells typically handle such a situation poorly.
Tuning options, for instance heap limits, are typically passed via this variable.
Java 9 introduces the `JDK_JAVA_OPTIONS` environment variable and argument files, which allows work around the `JAVA_OPTS` limitations.

**`YOFL_AUTOPATH`:**
The path to the directory which shall be scanned for the `.jar` files to be added to the boot class path.
Note that only the files contained directly in the specified directory are found and their order on the resulting class path depends on the operating system.
Despite of the limitations, this variable makes class path composition much easier for typical cases.

**`YOFL_BOOTPATH`:**
The class path to be appended to the boot class path.
Unlike `YOFL_AUTOPATH`, this variable is an actual class path fragment, and therefore does not suffer from the same limitations.
It is appended to the final class path before `YOFL_AUTOPATH`.

**`YOFL_LOGGING_FILE`:**
The desired output stream for the launcher logger.
The value may be `stdout` or `stderr` (the default) for standard process file handles, or a file name.
In the case of the file name, the target path must be accessible for write and the file is overwritten.
Note that while `stdout` prints to the standard output, `./stdout` prints to the file with the name of `stdout` in the current working directory.

**`YOFL_LOGGING_LEVEL`:**
The logging level for the launcher logger.
The value may be `FORCE`, `ERROR` (the default), `WARN`, `INFO` or `DEBUG`.
The launcher reports only severe errors by default and forced output (see `--dump-status` option) to the standard error handle, so that YOFL can be used for applications that produce their actual output to standard output without affecting it.


### Using launch scripts

Assuming that `./boot` directory contains the framework implementation in a `.jar` file and `./bundles` directory contains the bundles of an OSGi application, this command makes YOFL deploy the application into `./instance` directory and start it:

```bash
YOFL_AUTOPATH=./boot yofl launch --bundles ./bundles ./instance
```

However, most framework implementations need a bit tuning and additional options to work smoothly.

We recommend [ServiceBox](http://github.com/yetamine/servicebox), a ready-to-use minimalistic distribution based on YOFL, which takes care of these details for a particular framework implementation and which can be used as a template for more distributions.


## Configuration

A careful reader spotted that the `help` command output mentions:

* framework properties,
* launching properties,
* system properties,
* deployment properties (specifically, `deployment.properties` file).

What are all these properties for?

**Framework properties** are passed to the framework implementation almost directly.
Besides standard properties, defined by the OSGi specification, the properties may configure implementation-specific features.
However, some properties are controlled by the launcher arguments and can't be overridden.

**Launching properties** configure the launcher and indirectly the framework instance, e.g., by setting the default start level for newly deployed bundles.

**System properties** set default values for Java system properties.
The system properties can be set with Java launcher options as well.
Note that the existing values are never overridden, so the externally supplied values prevail.

**Deployment properties** provide settings specific for particular bundle sources and may override applicable default launching properties.

For the details on all the properties check the the sample properties files in the `configuration` directory.
The sample files contain explanation for all recognized properties and they can serve as a good starting point for creating own configuration.


## System requirements

YOFL needs Java Runtime Environment 1.8 or newer.

[JPMS](https://jcp.org/en/jsr/detail?id=376) (Java Platform Module System) causes no problems to YOFL itself as long as class path is used instead of module path.
However, the used framework implementation must be JPMS-aware and it may require additional JPMS-specific options for Java launcher in order to work properly.


## Licensing

The project is licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). Contributions to the project are welcome and accepted if they can be incorporated without the need of changing the license or license conditions and terms. Check the [GitHub site](http://github.com/yetamine/net.yetamine.osgi.launcher).
