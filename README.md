Author: [GitHub](https://github.com/maksik997)
# WARNING!
Largest test date size: (about 40 000 different files).
The library could not delete all duplicates, but surely it will extract all pixel-to-pixel images.
It Could be used to find similar pictures.

## Information:
- Please report any bugs :)
- If you have any idea (after all, you're reading this) please let me know. I could use some nice ideas to enhance this library.

# Functions: 
The library shares some packages: Algorithms, Comparator, IO, Structures, and Utils.
- **Algorithms**: 
Simple implementation of Fast DCT Lee algorithm (nothing to be expected here),
  also powered by ChatGPT (as short-cut for any other implementation).
Also with quantization method (using JPEG quantization table).
- **Comparator**: Two classes for File Predicate, with implementation for images. It uses magic numbers as it was in previous versions. Functional interface shares test method which return boolean and take File and could throw IOException. So it can be used in case someone would want to create something themselves.
- **IO**: FileOperator class which can load images efficiently (using new Java's virtual threads). Method takes depth (used by FileVisitor), FilePredicate to ensure that we take only valid files and collection/array of File objects.
- **Structures**: Classes for Records. Represents image with an added checksum. Shares process, groupByChecksum methods (Process for processing grouped by checksum data and groupByChecksum for grouping by checksum). And ImageRecord, which shares two public static fields, that are Function objects that allows using Record's process function to process a given image collection.
- **Utils**:Logging interface for Tinylog with tinylog.properties settings.

# Description:
This library was created to fulfill my own ambition to create my image comparing app into two parts.
You can edit anything there, create your own function that returns this monstrosity...
Or even enhance it.
I don't know why anyone would want to do this, but anyway, I had a great time creating it.
The library works in a way that satisfies me.
Please ask if you have any questions.

# Usage of a library version 0.5 or grater:
Simple code that describes usage:
```java
public static void main(String[] args) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        FileOperator fo = new FileOperator();

        File[] files = {
            /*
            * Directories to check files for 
            */
        };
        
        // Simple function that creates ImageRecords (groupByFunction)
        Function<File, ImageRecord> createImageRecord = file -> {
            try {
                return new ImageRecord(file);
            } catch (IOException ex) {
                LoggingInterface.staticLog(String.format("Skipping file: %s", file.getName()));
                LoggingInterface.staticLog(ex, String.format("Skipping file: %s", file.getName()));
            }
            return null;
        };

        List<File> f = fo.loadFiles(Integer.MAX_VALUE, new ImageFilePredicate(), files);

        System.out.println(f);
        System.out.println("Found: "+ f.size());
        
        System.out.println("Record.process output: ");
        System.out.println(Record.process(f, createImageRecord, ImageRecord.pHashFunction, ImageRecord.pixelByPixelFunction));
        // And here ImageRecord.[name] represents built-in functions to use. 
        
    }
```

### Note:
* App uses Tinylog as a logging service, and uses TwelveMonkeys as ImageIO enhancement.
* Files extensions supported: all that are supported by `ImageIO` class and all that are supported by: `TwelveMonkeys` (used packages: `bmp`, `tiff`, `jpeg`, `core`).
* The library was tested on Windows 11.
* Jdk version: **22**