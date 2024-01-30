Author: Maksymilian Strzelczak

# WARNING!
The library is not tested on a large scale, so be warned about that.
It should work for few thousand images etc.

# Usage of library for version `0.3` or later:
It's straightforward. Only what you need to do is:
1. Prepare yourself **destination directory** *(where files will be moved if they're duplicates)*, **source directory** or **list of files** *(files/directory that you want to compare files in)*,
2. Create `PictureComparer` object *(with either blank constructor or with your destination and source files/directory that you prepared in a previous step)*,
3. If you have used **blank** constructor you should now use `_setUp(<source files/directory>, <destination directory>)`.
4. Then you have to use three different methods. Firstly you would use `map()` method, this method will load all files prepare checksums etc., next you would use `compare()`, this method will note all duplicated files. And finally `move()` method that will move all duplicates to destination folder that you specified.
5. And lastly in case you would want to re-use this object, you can call `_reset()` method, that will clear a whole object. *(After resetting an object you should then call `_setUp(<source files/directory>, <destination directory>)` method)*
### Note:
* Files extensions supported: all that are supported by `ImageIO` class.
* App was tested on Windows 11.
* Jdk version: 21
### Code snippet:
```java
// Creating my source files
List<File> files = new ArrayList<>();
files.add(new File("{file1}"));
files.add(new File("{file2}"));
files.add(new File("{file3}"));
files.add(new File("{file4}"));
files.add(new File("{file5}"));
// I could also use 
// File source = new File("{sourceDir}");
// Creating my destination directory
File destination = new File("{destDir}");
// Creating a Picture Comparer object
PictureComparer pc = new PictureComparer(files, destination);
// I could also use
// PictureComparer pc = new PictureComparer();
// pc._setUp(files, destination);
// Usage
pc.map();
pc.compare();
pc.move();
// Resetting object
pc._reset();
```
