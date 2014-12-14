#!/bin/bash

initial=`pwd`
LUDecomposition='LUDecomposition_'
Mutation='Base'
Magnitude='.out.Magnitude.'
file='myfile.txt'
inputfile=${LUDecomposition}${Mutation}${Magnitude}'*'
mkdir ${Mutation}
mv ${inputfile} ${Mutation} cd ${Mutation}
cd ${Mutation}
cp ${WORKSPACE}/scripts/extractAbsoluteErrors.sh ${WORKSPACE}/scripts/extractAbsoluteErrors.py ${WORKSPACE}/scripts/extractErrorsMutants.py .
python extractErrorsMutants.py ${Mutation}
./extractAbsoluteErrors.sh
python extractAbsoluteErrors.py ${file} 
cd ${initial}
mutant_dir=${WORKSPACE}/mutants1
for m in `ls ${mutant_dir}`;
do
	Mutation=$m
	inputfile=${LUDecomposition}${Mutation}${Magnitude}'*'
	mkdir ${Mutation}
        mv ${inputfile} ${Mutation}
        cd ${Mutation}
        cp ${WORKSPACE}/scripts/extractAbsoluteErrors.sh ${WORKSPACE}/scripts/extractAbsoluteErrors.py ${WORKSPACE}/scripts/extractErrorsMutants.py .
       python extractErrorsMutants.py ${Mutation}
       ./extractAbsoluteErrors.sh
       python extractAbsoluteErrors.py ${file} 
        cd ${initial}
	 
done
