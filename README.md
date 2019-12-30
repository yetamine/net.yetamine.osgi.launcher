# net.yetamine.osgi.launcher

Yetamine OSGi Launcher, or YOFL for short, is a vendor-neutral launcher for the OSGi Framework and OSGi-based applications.


## Introduction

Launching a framework instance is not a difficult task, thanks to the specification-defined support for framework launching and embedding, but it still requires a non-trivial effort, especially when taking into account that launching the framework instance must include deploying at least the initial set of bundles.

A launcher therefore must:

* Prepare the framework deployment area and configure the environment if necessary.
* Deploy application bundles, or at least an initial bundle set that takes the bootstrapping process over and deploys the application bundles later.
* Start the framework instance.
* Take care of a graceful shutdown of the framework instance (at least the usual system signals should be handled, some launchers provide even the possibility of remote triggers).

YOFL performs all these tasks according to the instructions given by the command line arguments and configuration files, which are specified in the arguments.


## Why to use YOFL?

As noted above, a framework launcher must take care of quite a lot of tasks.
Framework vendors therefore often supply own launchers together with their framework implementations, so that the framework user does not have to deal with all those tasks.
However, the vendor-specific launchers differ, which brings a couple of points to consider:

* Vendor-specific launchers are usually specific for a particular framework implementation and therefore details like the bootstrap configuration can make changing the framework implementation more difficult.
* Some launchers may be more suitable for specific use cases than others and having more options could help to choose a better suited launcher for a particular use case.

Let's have a look which YOFL's features might be interesting.

**Vendor-neutral:**
YOFL favours no particular OSGi Framework implementation.
It uses the specification-defined portable way to make a framework instance and uses no dirty hacks or proprietary extensions.

**Light and simple:**
Large framework distributions provide many impressive features and services, but such a plethora of options might become a burden for small applications.
Instead of trying to shrink a large framework distribution for hosting such an application, it might be easier to build a small distribution from ground up.

**Flexible layout:**
YOFL takes a number of arguments to learn what to do and what to use.
Having bundles scattered among various places on the filesystem is no problem.
Building an application/framework distribution with a specific layout should be then no problem either.

**Bundle discovery:**
YOFL can discover and configure multiple bundles from a filesystem in a bulk way.
Just tell where the bundles are located and optionally supply specific options, like the start levels of particular bundles that deviate from the default settings.
YOFL deploys and configures them accordingly.

**Bundle synchronization:**
It is possible to configure one or more bundle sources, so that YOFL keeps the deployed bundle set on launch consistent with particular bundle sources.

**Basic process control:**
Like any ordinary launcher, YOFL supports graceful framework shutdown and optional remote shutdown on receiving a cryptographically protected packet.

**Smaller and faster Docker images:**
There are two points contributing to this goal.

* Firstly, as mentioned above, an application distribution can be build from ground up, so that it contains no features that the application does not need or use.
* Secondly, installing a bundle in a framework instance means usually that the framework copies the bundle binary to its storage area.
This allows using the bundle, until it is uninstalled, even if the original bundle location is not available anymore.
A naïvely built Docker image repeats the installation steps for every new container, duplicating the bundles in the container file system and prolonging the startup time.

Unlike most other launchers, YOFL distinguishes the *deploy* phase and the *start* phase and it allows executing them separately.
This feature can be used conveniently with the Docker builder pattern:

1. Take a base Docker image with YOFL and use it as the builder.
2. Add the application distribution.
3. Let YOFL deploy the application.
4. Create the final Docker image from the base image and the deployed application.

Then the final image contains just the pre-deployed application ready to run with YOFL as the entry point that starts it (but skips the deploy phase completely).


## Getting started

Download the release and check the attached documentation to get more information.
A brief help can be displayed with the following command:

```bash
java -jar yofl.jar help
```


## Licensing ##

The project is licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). Contributions to the project are welcome and accepted if they can be incorporated without the need of changing the license or license conditions and terms.


[![Yetamine logo](https://github.com/yetamine/yetamine.github.io/raw/master/brand/light/Yetamine_logo_opaque_100x28.png "Our logo")](https://github.com/yetamine/yetamine.github.io/blob/master/brand/light/Yetamine_logo_opaque.svg)
