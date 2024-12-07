# Image Comparison Library 
### PictureComparer
#### Version: 0.7.0

## Introduction

This Java library is designed to identify and remove duplicates from collections of images.
It can help you efficiently manage your photo gallery or image stock.
The library also provides robust extensibility options, allowing you to implement custom image comparison algorithms, advanced caching solutions, and more.

In addition, it provides convenient tools to manage and organize your files, making it a versatile solution for various image management needs.

## Requirements:

The application requires:
- **Java**: 21 (requires virtual threads, introduced in JDK 21 | [Java 21 Virtual Threads](https://openjdk.java.net/jeps/444))
- **Maven** (building from source)
- **Git** (building from source)


- Supported picture types:
  - all that are supported by `ImageIO` class 
  - and all that are supported by: `TwelveMonkeys` (used packages: `bmp`, `tiff`, `jpeg`, `core`).

## Dependencies:

- **Runtime dependencies:**
  - **SLF4J**: 2.0.13
  - **logback**: 1.5.6
  - **TwelveMonkeys**: 3.11.0
  - **JTransforms**: 3.1
  - **Caffeine**: 3.1.8


- **Testing dependencies:**
  - **JUnit**: 5.8.2
  - **Mockito**: 5.11.0
  - **Byte-Buddy**: 1.14.10

## Installation:

**Easy way:**

Visit the [Releases](https://github.com/maksik997/PictureComparer/releases) section on GitHub to download the latest .jar file, 
then add it to your project dependencies.


**Building from source:**

1. Clone git repository:
```bash
git clone https://github.com/maksik997/PictureComparer.git
cd PictureComparer
```
2. Build project using Maven:
```bash
mvn clean package
```

## Configuration:

### Configuring JVM Memory for Optimal Cache Allocation with AdaptiveCache:
To ensure that `AdaptiveCache`, which leverages the **Caffeine** caching library,
can effectively manage memory, it's important to allocate enough memory to the JVM heap.

This section provides guidelines on how to adjust the JVM memory settings.

#### 1. Adjusting Maximum Heap Size for the JVM
The JVM heap size determines how much memory Java is allowed to use for objects and data structures,
including cache. By allocating sufficient heap memory, you can ensure that the
`AdaptiveCache` has enough room to function efficiently.

To control the maximum heap size, you can use the following JVM options when starting
your Java application:
- `-Xmx` - Sets the maximum heap size.
- `-Xms` - Sets the initial heap size.

For example, to allocate a maximum of 2 GB of heap space, use
```bash
java -Xmx512m -Xmx2g -jar your-application.jar
```
This command sets the initial heap size to 512 MB (`-Xms512m`) and the maximum heap size to 2 GB (`-Xmx2g`).

#### 2. Calculating Cache Size (60% of JVM Heap)
The `AdaptiveCache` utilize up to **60% of the available JVM heap** for caching, 
you should calculate the desired cache size based on the `-Xmx` value you set.

For example, if you set `-Xmx2g` (2 GB) you can allocate up to 60% of that for the cache:
60% of 2GB = 1.2 GB.

#### 3. Monitoring Cache Behaviour
Once you've configured the cache size, it's important to monitor the performance of the cache
and the JVM memory usage.
You can track memory usage with tools like:
- **JVM Monitoring Tools**: Tools such as VisualVM or JConsole allow you to monitor memory usage and garbage collection within the JVM.
- **Caffeine's Cache Metrics**: Caffeine provides internal metrics that you can use to observe cache hits, misses, and eviction rates.
Those metrics are available via `AdaptiveCache.getInstance().monitor(1)` method in the `AdaptiveCache` class.

#### 4. General tips
- For optimal performance, aim to fit all of your data into memory. If this isn't feasible, monitor cache evictions and adjust the heap size or cache policy accordingly.
- Keep the cache as big as possible.

### Setting Up Logging
The library uses SLF4J for logging, which allows you to configure different logging frameworks to suit your needs. 
To get started with logging, follow the instructions below:

#### 1. Choose your Logging Framework:
You can use different logging frameworks. Some popular options are:
- **Logback** (implemented natively, thus configuration is required).
- **Log4j2**
- **java.util.logging**

To use specific framework, include its corresponding dependency in your project.

#### 2. Configure your Logging:
Each logging framework requires a configuration file.

##### Logback Configuration (logback.xml):
Create a file name `logback.xml` in your classpath (typically in the `src/main/resources` directory)
with the following content:

```xml
<configuration>
  <appender name="Console" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss} - %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="info">
    <appender-ref ref="Console" />
  </root>
</configuration>
```
This configuration will log messages to the console, 
with a pattern that includes the timestamp, log level, logger name, and message.

#### Log4j2 Configuration (log4j2.xml):
If you prefer to use Log4j2, create `log4j2.xml` configuration file in your classpath with a
similar structure:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} - %-5level %logger{36} - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>

```

## Usage:

Usage of this library is quiet straightforward.

For starters, you should attach the `.jar` file to your project (or use e.g., [jitpack.io](https://jitpack.io)).

And then:
1. Create instance of `FileOperator` and `Processor` classes.

```java
import pl.magzik.Processor;
import pl.magzik.algorithms.PerceptualHash;
import pl.magzik.algorithms.PixelByPixel;
import pl.magzik.grouping.CRC32Grouper;
import pl.magzik.io.FileOperator;
import pl.magzik.predicates.ImageFilePredicate;

import java.util.List;

public class MyClass {
  public FileOperator fileOperator;
  public Processor processor;

  public MyClass() { // Example class
      fileOperator = new FileOperator(new ImageFilePredicate(), Integer.MAX_VALUE);
      processor = new Processor(
          new CRC32Grouper(),
          List.of(new PerceptualHash(), new PixelByPixel())
      );
  }
}
```

2. Prepare your image collection:

```java
import java.io.File;
import java.util.List;

public static void main(String[] args) {
    MyClass mc = new MyClass();
    List<File> files = List.of(new File("path/to/my/gallery"));
    files = mc.fileOperator.load(files);
}
```

3. And process you image collection using `Processor` class:

```java
public static void main(String[] args) {
    // ...
    Map<File, Set<File>> duplicates = mc.processor.process(files);
}
```

## Wiki:

Visit the GitHub [Wiki](https://github.com/maksik997/PictureComparer/wiki/1.-Introduction) for detailed documentation on installation, configuration, and usage examples.

## Information

- Please report any bugs you encounter.
- If you have ideas for enhancements, please let me know. I welcome suggestions to improve this library.

## Contribution

We welcome contributions! If you'd like to contribute, please fork the repository, create a new branch, and submit a pull request with your changes.

## License
This project is licensed under the MIT License. See the LICENSE file for details.
