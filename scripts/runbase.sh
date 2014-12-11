#!/bin/sh
#cd ${WORKSPACE}
WORKSPACE=`pwd`


commons_math_src=${WORKSPACE}/commons-math3-3.3-src
commons_math_src_tar=${WORKSPACE}/commons-math3-3.3-src.tar.gz
commons_math_bin_tar=${WORKSPACE}/commons-math3-3.3-bin.tar.gz
commons_classes=${commons_math_src}/target/classes
tests=${commons_math_src}/target/test-classes/
run_path=${commons_math_src}/src/test/java
results_dir=${WORKSPACE}/OUTLIERS10/


#run_noisy LUDecomposition /commons-math3-3.3-src/src/main/java/org/apache/commons/math3/linear/ /org/apache/commons/math3/linear/  /commons-math3-3.3-src/target/classes/org/apache/commons/math3/linear/ $m or "Base" /results/LUDecomposition
run_noisy () {
    # $1 Class Under Test (CUT) name
    # $2 Path to CUT source
    # $3 Java name path to CUT
    # $4 Path to CUT targets
    # $5 Mutation number
    # $6 Output directory

    cd ${2}
    echo "cd" ${2}	
    rm -f *.class
   echo "rm -f *.class"

    # compile just the selected class under test with enerjc including instrumentation for error injection (-Alint=mbstatic,simulation)
    enerjc -Alint=mbstatic,simulation -cp ${commons_classes}  ${1}.java
 #  echo "enerjc -Alint=mbstatic,simulation -cp" ${commons_classes}  ${1}".java"

    # move resulting class files into the target classes
    mv ${2}/${1}*.class ${4}/
#echo "mv" ${2}"/"${1}"*.class" ${4}"/"

    # run the tests for class under test with enerj
    cd ${run_path}
   # echo "cd" ${run_path}
	
    # move enerj error injection (i.e., noise) parameters file into directory
    cp ${WORKSPACE}/enerjnoiseconsts.json .

    maxmag=51
    iters=10

    outfile="${6}/${1}_${5}.out"
    rm ${outfile}
  #  echo "rm" ${outfile}	
    for m in `seq 0 $maxmag`
    do
    
        # modify the noise constants for enerj
        sed -i.bu "s/\"DOUBLE_ERROR_MAG\"\:[0-9][0-9]*/\"DOUBLE_ERROR_MAG\"\:${m}/g" enerjnoiseconsts.json
        for i in `seq 1 $iters`
        do
            
            echo "Magnitude: $m Iteration: $i"
            
            # run CUT
            enerj $3.$1Test > ${outfile}.Magnitude.${m}i${i}.tmp 2>&1
         #   echo "enerj" $3"."$1"Test >" ${outfile}".Magnitude."${m}"i"${i}".tmp 2>&1"
        
        done
    done

}

# Class Under Test (CUT) macros
cut=LUDecomposition
cut_jpath=org.apache.commons.math3.linear
cut_jpath_src=/org/apache/commons/math3/linear/
cut_src=/src/main/java/${cut_jpath_src}
cut_targets=/target/classes/${cut_jpath_src}

# Save baseline source
cut_bu=cut_bu
cut_src_save_dir=${WORKSPACE}/${cut_bu}/
rm -rf ${cut_bu}
mkdir ${cut_src_save_dir}
cp ${cut_src}/${cut}.java ${cut_src_save_dir}/

# Create results directory
mkdir ${results_dir}
cd ${results_dir}
mkdir ${cut}
cd -

# Compile and run baseline -- no mutants
run_noisy $cut "${commons_math_src}/${cut_src}" ${cut_jpath} "${commons_math_src}/${cut_targets}" "Base" "${results_dir}/${cut}"
