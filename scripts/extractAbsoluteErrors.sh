#!/bin/bash
LUDecomposition="LUDecomposition_"
output='myfile.txt'
for f in `ls ${LUDecomposition}*`;       
do
	if [ -f ${f} ];
	then
                                                 
	echo $f $output 
         tail -n 2 ${f} >> $output  
	fi 
	echo $f 
done
