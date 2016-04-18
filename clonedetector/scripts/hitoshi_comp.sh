#!/bin/sh
iorepos=(iorepo/cj_11 iorepo/cj_12 iorepo/cj_13 iorepo/cj_14)

read -s -p "Password: " pw

echo "Comparison mode"

repo_length=${#iorepos[@]}
echo "Repo length: $repo_length"

for ((i=0; i<$repo_length; i++))
do
	for ((j=i+1; j<$repo_length; j++))
	do
		comp_name=$(echo "${iorepos[$i]}-${iorepos[$j]}"|sed "s/\///g")
		java -Xmx62g -cp target/CloneDetector-0.0.1-SNAPSHOT.jar edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver -cb code_repo/bin -alg deepHash -mode comparison -eName $comp_name -db liberty.cs.columbia.edu:3306/hitoshio -user root -pw $pw -io ${iorepos[$i]} ${iorepos[$j]}
	done
done

