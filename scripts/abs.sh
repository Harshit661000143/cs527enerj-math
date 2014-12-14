#!/bin/bash

LUDecomposition='SphericalCoordinates_'
Mutation='Base'
Magnitude='.out.Magnitude.'
Mantissa='10'
collect='collect_error'
it='6'
last='.tmp'

mutant_dir="/dcsdata/home/hdokani2/Desktop/cs527/mutantsSphe"
error_value='error_value'
files="/dcsdata/home/hdokani2/Desktop/cs527/files"
for m in `ls ${mutant_dir}`;
do
Mutation=$m
	inputfile=${LUDecomposition}${Mutation}${Magnitude}'*'
	mkdir $m
        mv ${inputfile} $m
#  echo "" #### print the ddnew line ###
done
