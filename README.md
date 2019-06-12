# HitoshiIO: Detecting functionally similar code

HitoshiIO is a system for identifying functional similar methods based on their I/Os. Information on how HitoshiIO works can be found in our [ICPC 2016 paper](http://jonbell.net/icpc_16_hitoshiio.pdf).


Running
-------

### Pre-step 1 (Optional)
Change base variable in `hitoshiIO2/clonedetector/src/main/java/edu/columbia/cs/psl/ioclones/sim/AbstractSim.java` to change the correlation base.

### Pre-step 2 (Optional)
Change indexType variable in `hitoshiIO2/clonedetector/src/main/java/edu/columbia/cs/psl/ioclones/sim/FastAnalyzer.java` to choose a coefficient.

HitoshiIO will modify the bytecode of your application to record I/Os, and then compute similarity between these I/Os. The steps to install and use HitoshiIO are as follows. 

### Step 0

HitoshiIO is a maven project. For compiling HitoshiIO, please change your directory to `clonedetector` and run the following command:

```mvn clean package```

### Step 1 (PreAnalyzer: Capture and store identified I/O's)

Before running the application, HitoshiIO needs to identify the inputs and outputs of each method. Please make sure that you have `clonedetector/classinfo/methodeps.db` - it is provided in the current release of HitoshiIO. You can then run this command:

```java -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.analysis.PreAnalyzer -cb {/path/to/your/bytecodebase}```

`{/path/to/your/bytecodebase}` can be a relative or full path to a folder of Java .class files. A smaller bytecodebase may serve you better. Make sure that all classes that you want analyzed have been compiled using JDK 8 - compiling with newer versions of Java may not be supported by ASM, which is heavily used throughout the project.

The I/O identification results will be stored in "cb.db" under the "classinfo" directory.

### Step 2 (IODriver: Execute your application)

Please execute your application using this command:

```java -javaagent:target/CloneDetector-0.0.1-SNAPSHOT.jar -noverify -cp "target/CloneDetector-0.0.1-SNAPSHOT.jar:{/path/to/your/bytecodebase}" edu.columbia.cs.psl.ioclones.driver.IODriver {your.application.class} {... args}```

Note: replace `{/path/to/your/bytecodebase}` with the file path to your bytecodebase containing all your java class files,  `{your.application.class}` with the fully qualified class name (minus the .class file extension) of the java class you would like to capture I/Os for, and `{... args}` with any command-line arguments expected. Note that `{args}` can be a filepath, and will start at the `ioclones/clonedetector` directory, so all relative paths must adhere to that.

Because HitoshiIO is a dynamic analysis tool, you need to execute (profile) every application that you want to detection functional clones in your codebase by the command above. **However, only classes with a `main` method can be executed, as functionality to individually execute methods has not yet been implemented.**

The I/O profiles of each method executed in your application can be found under the "iorepo" directory.

### Step 3 (SimAnalysisDriver: Store methods and corresponding similarities to database)

HitoshiIO needs a database to store the captured functional clones. We use MySQL Workbench and MySQL Community Server. Note that Workbench is not necessary, just convenient. For downloading and installing MySQL, please refer to [MySQL](https://www.mysql.com/). For MySQL Workbench please refer to [MySQL Workbench download link](https://dev.mysql.com/downloads/workbench/). For MySQL Community Server please refer to [MySQL Community Server download link](https://dev.mysql.com/downloads/mysql/).

In order to run the command successfully, you must first create two tables in your database:

**[db_gen.txt](https://github.com/Programming-Systems-Lab/hitoshiIO2/blob/master/clonedetector/classinfo/db_gen.txt) exists to help with the creation of these tables, and contains the table schemas for `hitoshio_summary` and `hitoshio_row`.**

For computing the functional similarity between methods, you can select from the following options:

1. For a single I/O repository with exhaustive I/O profile comparisons, run the following command: 

```java -Xmx62g -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver -cb {/path/to/your/bytecodebase} -alg deepHash -mode exhaustive -eName “{preferred_comparision_name_in_db}” -db {db_IP}:{port}/{db_name} -user {root or your_username} -pw {your_password} -io {path/to/iorepo}```

2. For a single I/O repository, to explore similarities of a specific method, run the following command: 

```java -Xmx62g -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver -cb {/path/to/your/bytecodebase} -alg deepHash -mode individual -target {method_name} -eName “{preferred_comparision_name_in_db}” -db {db_IP}:{port}/{db_name} -user {root or your_username} -pw {your_password} -io {path/to/iorepo}```
**NOTE: all methods with method names (does not include class name!) equal to the string replacing `{method_name}` will be compared against all other methods.**

3. For comparison of I/O profiles between two specified I/O repositories, run the following command: 

```java  -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver -cb {/path/to/your/bytecodebase} -alg deepHash -mode comparison -eName "{preferred_comparision_name_in_db}" -db {db_ip}:{port}/{dbname} -user {username} -pw {pw} -io {/path/to/your/io_repo1} {/path/to/your/io_repo2}```

In all of these commands, replace `{/path/to/your/bytecodebase}` with the relative or full path to your bytecodebase, `{preferred_comparision_name_in_db}` with a name for your codebase to be stored in the database for any comparison reults from this execution, `{path/to/iorepo}` (or `{path/to/io_repo1}` and `{path/to/io_repo2}`) with the path to the `iorepo` directory that you wrote to in Step 2, and `{db_IP}`, `{port}`, `{db_name}`, `{your_username}`, and `{your_password}` with the appropriate identification fields for connecting to your mysql database. If the `-mode` flag is omitted, the mode will default to exhaustive comparison in a single I/O repo.

Only method I/O pairs with similarity scores higher than a predefined threshold will be written to the database.

Note: HitoshiIO will filter out constructor, static constructor, toString, equals and hashCode methods. We plan to make this configurable.

### Step 4 (Filter in database)

For reviewing the detected functional clones, you can simply use the following SQL command:

```sql
SELECT * FROM hitoshio_row
WHERE comp_id=codebase_id and sim>=your_threshold;
```

Notes: The default similarity threshold for HitoshiIO is 0.85.

Additional Notes
------
A new release has been pushed on 6/11/2019, fixing a few issues with the selection of inputs and outputs. The previous version of HitoshiIO, referenced in the paper, can be found in `oldclonedetector`.


Questions, concerns, comments
----
We would very much welcome any feedback, please add to "issues". 

License
-------
This software is released under the MIT license.

Copyright (c) 2016-2019, by The Trustees of Columbia University in the City of New York.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Acknowledgements
--------
The primary researchers on this project have been [Fang-Hsiang (Mike) Su](mailto:mikefhsu@cs.columbia.edu), [Jonathan Bell](mailto:jbell@cs.columbia.edu), [Gail Kaiser](mailto:kaiser@cs.columbia.edu) and [Simha Sethumadhavan](mailto:simha@cs.columbia.edu). Contact [Gail Kaiser](mailto:kaiser@cs.columbia.edu) with any questions or concerns. The [Programming Systems Lab](http://psl.cs.columbia.edu/) has been funded in part by NSF CNS-1842456, CCF-1815494, CNS-1563555, CCF-1302269, CCF-1161079 and CNS-0905246.
