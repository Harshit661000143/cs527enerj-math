#!/bin/sh
enerjdir=../../enerj

enerjargs=-noisy
scimarkargs=
for arg
do
    case "$arg" in
    -nonoise) enerjargs= ;;
    *) scimarkargs="$scimarkargs $arg" ;;
    esac
done

#$enerjdir/bin/enerj -Xmx2048m $enerjargs jnt.scimark2.commandline -tiny $scimarkargs

maxmag=51
iters=10

outfile="lu${1}.out"
goldfile=lu-gold.out

rm ${outfile}

for m in `seq 45 $maxmag`
do

  # modify the noise constants for enerj
  sed -i.bu "s/\"DOUBLE_ERROR_MAG\"\:[0-9][0-9]*/\"DOUBLE_ERROR_MAG\"\:${m}/g" enerjnoiseconsts.json
  for i in `seq 5 $iters`
  do
    
    echo "Magnitude: $m Iteration: $i"
    
    # run LU
    $enerjdir/bin/enerj -Xmx2048m $enerjargs jnt.scimark2.commandline -tiny $scimarkargs lu > ${outfile}.tmp 2>&1
    # $enerjdir/bin/enerjstats

    # collect output error to gold file
    sed '/vector/!d' < ${outfile}.tmp >> ${outfile}
    sed '/^Err/!d' < ${outfile}.tmp >> ${outfile}
    sed '/^Abs/!d' < ${outfile}.tmp >> ${outfile}

  done
done

# plot input error vs output error
#./calc_inerr_vs_outerr.py -i ${outfile} -g ${goldfile} -o ${2}
