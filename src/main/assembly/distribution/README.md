# Yetamine OSGi Framework Launcher

Yetamine OSGi Launcher, or YOFL for short, is a vendor-agnostic launcher for the OSGi Framework and OSGi-based applications.


## Introduction

YOFL is a tool for expert users and therefore assumes some knowledge of Java applications and the OSGi Framework in particular.
This tool launches the framework if provided with an implementation on the class path according to the supplied command line.

Assuming that Java runtime environment 1.8 or newer is available, YOFL can show the command line reference with following command:

```bash
java -jar yofl.jar help
```

Most commands require an OSGi framework implementation to operate with, unlike the `help` command.
However, the launcher contains no OSGi framework implementation.
Instead, it assumes that the OSGi framework implementation (compatible with OSGi R5 or newer) to be launched is provided on the class path.
This allows using the launcher with any sound OSGi framework implementation with relative ease: the implementation must be supplied on the class path.


## Using the launcher

For all commands that have to start a framework instance an OSGi framework implementation must be available, otherwise Java complains about missing classes and terminates.
Of course, it is possible to invoke the launcher as an ordinary Java application directly:

```bash
java -cp yofl.jar:some/path/osgi-framework-implementation.jar launch
```

This becomes quickly uncomfortable, especially when additional tuning options are applied.
The supplied launch scripts provide a bit more comfortable alternative.
However, because the scripts are still a very thin layer around the launcher, they are more useful for making application distributions on the top of them.

The scripts use an interface based on environment variables and pass all command line arguments directly to the launcher.
This approach seems to be more robust and portable than attempts to process command line arguments partially.
The details of all used environment variables are available in the script headers.

Check [ServiceBox](http://github.com/yetamine/servicebox), a ready-to-use distribution based on YOFL.
ServiceBox shows how YOFL can be used for making a small OSGi framework distribution.
Moreover ServiceBox might be actual thing to be used rather than YOFL itself.


## Licensing

The project is licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). Contributions to the project are welcome and accepted if they can be incorporated without the need of changing the license or license conditions and terms. Check the [GitHub site](http://github.com/yetamine/net.yetamine.osgi.launcher).
