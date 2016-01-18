#!/bin/sh
iorepos=(iorepo/cj_11 iorepo/cj_12 iorepo/cj_13 iorepo/cj_14)

read -s -p "Password: " pw

echo "Exhaustive mode"
for i in "${iorepos[@]}"
do
	java -Xmx62g -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver -cb code_repo/bin -alg deepHash -mode exhaustive -eName $i -db liberty.cs.columbia.edu:3306/hitoshio -user root -pw $pw -io $i
done