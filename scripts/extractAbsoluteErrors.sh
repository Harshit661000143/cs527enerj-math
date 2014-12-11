#!/bin/bash
LUDecomposition="LUDecomposition_"
Magnitude=".out.Magnitude."
collect="collect_error"
last=".tmp"
error_value="error_value"
WORKSPACE=`pwd`
MUTANT_DIR=${WORKSPACE}`/MUTANT'

for f in `ls ${MUTANT_DIR}`;       ### Outer for loop ###
do
	
    		Mutation=$f
   		inputfile=${LUDecomposition}${Mutation}${Magnitude}
		output=${files}/$f''abserror'
   	 	for m in `seq 0 51`;
        		do
			echo "Magnitude" $m
				 nextiter=${inputfile}
         	  		 inputfile=${inputfile}$m'i'
                                 nexti=${inputfile}
		 	   	 echo $inputfile 
    	         	 	 for i in `seq 1 10`;
              				do
						echo "iteration" $i
              		  			inputfile=${inputfile}$i${last}
					       	
						if [ -f ${inputfile} ]
						then
                                                 
		 	   			echo $inputfile $output 
         					 tail -n 2 ${inputfile} >> $output  
						fi 
		 	   			echo $inputfile 
                                                if [ "$i" -ne 10 ]; then	
		                     		  inputfile=${nexti}
		   				else
						  inputfile=${nextiter}	
						fi
	 		 		done	   
		
		       done 
		cd -  
   
done
