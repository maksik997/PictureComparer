# Image Comparison Library

Author: [GitHub](https://github.com/maksik997)

## WARNING!

**Largest test dataset size**: Approximately 40,000 different files.

### Note:
The library may not be able to delete all duplicates, but it should extract all pixel-to-pixel images.
It can be used to find similar pictures.

## Information

- Please report any bugs you encounter.
- If you have ideas for enhancements (after all, you're reading this), please let me know. I welcome suggestions to improve this library.

## Library Structure

The library is organized into several packages:

- **Algorithms**: Contains:
  > An interface for simple algorithm creation and usage.
  > A simple utility interface for image reading.
  > An implementation of DCT algorithm.
  > An implementation of Perceptual Hash algorithm.
  > An implementation of Pixel by pixel comparison algorithm
- **IO**: Contains:
  > A FileOperations interface, which gives access to various IO related methods (loading, moving, deleting of files).
  > An implementation of FileOperations interface, FileOperator class, which allows you to load and pre-validate loaded images (reading headers of files and checking if supported image type). This class uses virtual threading in the default implementation.
  > A FileValidator class, which validates given files. Implementation uses FilePredicate, which in its implementation (ImageFilePredicate) uses format magic numbers for validation.
  > A FileVisitor class, which is an extension of SimpleFileVisitor and handles loading files from filesystem trees. 
- **Predicates:** Contains:
  > A FilePredicate functional interface, which is similar to Java's Predicate interface, but throws IOException.
  > An implementation of FilePredicate interface, ImageFilePredicate, which uses file headers and format magic numbers for supported files validation.
- **Structures**: Contains:
  > An abstract Record class, which represents file record used in processing, record class has its file reference (File field) and checksum (long field).
  > An implementation of Record, ImageRecord, which represents Record for images, and uses CRC32 algorithm for checksum extraction. 
- **Utils**: Includes a logging interface for Tinylog, with configurations in `tinylog.properties`.
- **And one class without a package:**
  > RecordProcessor, which handles record processing: grouping by checksum and extraction of duplicates using given algorithms.

## Description

This library was created to fulfill my ambition to develop an image comparison application. You can modify and extend the library as needed. I had a great time creating it, and it meets my requirements. If you have any questions or suggestions, feel free to ask!

### Implementation
To customize logging, you can set up your own configuration by passing the following argument to your application:
```properties
-Dtinylog.configuration=[PATH]/tinylog.properties
```
[More info](https://tinylog.org/v2/configuration/)

### Specification: 
- App uses Tinylog as a logging service, uses TwelveMonkeys as ImageIO enhancement, and uses JTransforms for better DCT implementation.
- Files extensions supported: all that are supported by `ImageIO` class and all that are supported by: `TwelveMonkeys` (used packages: `bmp`, `tiff`, `jpeg`, `core`).
- The library was tested on Windows 11.
- JDK version: **22**

### External libraries used:
- [Tinylog](https://tinylog.org/v2/)
- [TwelveMonkeys](https://github.com/haraldk/TwelveMonkeys)
- [JTransforms](https://github.com/wendykierp/JTransforms)

### Usage example:
```java
public static void main(String[] args) throws IOException {
  FileOperator fo = new FileOperator(new ImageFilePredicate(), Integer.MAX_VALUE);

  File[] files = {
          /*
          * Files to be loaded.
          * */
  };

  long time = System.currentTimeMillis();

  System.out.println("=========== LOADING ===========");
  List<File> f = fo.load(files);

  System.out.println(f);
  System.out.println("Found: "+ f.size());
  System.out.println("Found in: " + (System.currentTimeMillis() - time) + " milliseconds");

  time = System.currentTimeMillis();

  RecordProcessor rp = new RecordProcessor();
  var map = rp.process(f, ImageRecord::create, new PerceptualHash(), new PixelByPixel());

  System.out.println("=========== PROCESSING ===========");
  for (List<?> list : map.values()) {
    System.out.println(list);
  }
  System.out.println("Found: "+ map.size());
  System.out.println("Found in: " + (System.currentTimeMillis() - time) + " milliseconds");
}
```