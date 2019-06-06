This is Mike's old version.

HitoshiIO: Detecting functionally similar code
========


HitoshiIO is a system for identifying functional similar methods based on thier I/Os. The information about how HitoshiIO works can be found in our [ICPC 2016 paper](http://jonbell.net/icpc_16_hitoshiio.pdf).


Running
-------
HitoshiIO will modify the bytecode of your application for recording I/Os and then compute similarity between these I/Os. The steps to install and use HitoshiIO are as follows.

### Step 0
HitoshiIO needs a database to store the captured functional clones. We use MySQL. For downloading and installing MySQL, please refer to [MySQL](https://www.mysql.com/).

HitoshiIO is a maven project. For compiling HitoshiIO, please change your directory to "clonedetector" and use the following command:

mvn clean package

### Step 1
Before running the application, HitoshiIO needs to identify the inputs and outputs of each method. Please make sure that you have "methodeps.db" under the "classinfo" directory. You can then run this command:

java -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.analysis.PreAnalyzer -cb /path/to/your/bytecodebase

Th I/O identification results will be stored in "cb.db" under the "classinfo" directory.

###Step 2
Now you can execute your application. Please use this command:

java -javaagent:target/CloneDetector-0.0.1-SNAPSHOT.jar -noverify -cp "target/CloneDetector-0.0.1-SNAPSHOT.jar:/path/to/your/bytecodebase" edu.columbia.cs.psl.ioclones.driver.IODriver your.application.class args

Because HitoshiIO is a dynamic analysis tool, you need to execute (profile) every application that you want to detection functional clones in your codebase by the command above. The profiling results of each method in a single application can be found uner the "iorepo" directory.

###Step 3
For computing the functional similarity between methods, you can either assign a single I/O repository, which exhaustively compare all I/O profiles in this repository:

java -Xmx62g -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver -cb /path/to/your/bytecodebase -alg deepHash -mode exhaustive -eName "preferred_comparision_name_in_db" -db db_ip:port/dbname -user username -pw pw -io /path/to/your/io_repo1

or you can assign two I/O repositories, which compare every pair of I/O profiles (one from repo1 while the other one from repo2):

java  -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver -cb /path/to/your/bytecodebase -alg deepHash -mode comparison -eName "preferred_comparision_name_in_db" -db db_ip:port/dbname -user username -pw pw -io /path/to/your/io_repo1 /path/to/your/io_repo2

Notes: HitoshiIO filter out constructor, static constructor, toString, equals and hashCode methods. We plan to make this configurable.

###Step 4
For reviewing the detected function clones, you can simply use the following SQL command:

SELECT * FROM hitoshio_row
WHERE comp_id=codebase_id and sim>=your_threshold;

Notes: The defaul similarity threshold for HitoshiIO is 0.85.


Questions, concerns, comments
----
Please email [Fang-Hsiang (Mike) Su](mailto:mikefhsu@cs.columbia.edu) with any feedback. This project is still under heavy development, and we have several future plans. Thus we would very much welcome any feedback.

License
-------
This software is released under the MIT license.

Copyright (c) 2016, by The Trustees of Columbia University in the City of New York.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Acknowledgements
--------
The authors of this software are [Fang-Hsiang (Mike) Su](mailto:mikefhsu@cs.columbia.edu), [Jonathan Bell](mailto:jbell@cs.columbia.edu), [Gail Kaiser](mailto:kaiser@cs.columbia.edu) and [Simha Sethumadhavan](mailto:simha@cs.columbia.edu). This work is funded in part by NSF CCF-1302269, CCF-1161079 and NSF CNS-0905246..


