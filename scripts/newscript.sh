#!/bin/sh
    for m in `ls /dcsdata/home/hdokani2/Desktop/cs527/mutantsSphe`
    do
    echo $m
    ret=`pwd`
    collect=${ret}/collect
    cd $m
    cp ${ret}/extractAbsoluteErrors.sh ${ret}/extractAbsoluteErrors.py ${ret}/extractErrorsMutants.py .
    python extractErrorsMutants.py
    ./extractAbsoluteErrors.sh
    python extractAbsoluteErrors.py > LUDecompositionAbsError${m}
    mv LUDecompositionAbsError${m} ${collect}
echo $m    
mv  LUDecompositionerror_value  LUDecompositionerror_value${m}
    mv LUDecompositionerror_value${m} ${collect}
    cd -

    done	


