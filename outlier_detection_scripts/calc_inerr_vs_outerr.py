#!/usr/bin/env python

from optparse import OptionParser

import numpy as np
import scipy.misc as spm

import cPickle                         # to read and write energy and perf values to binary file

def readVector(line):
    vector = []
    line_s = line.split(' ')
    for i in range(2,len(line_s)-1):
        vector.append(float(line_s[i]))
    return vector
# end def readVector(line)

def readGoldFile(gold_filename):
    gold_vector=None
    gold_file = open(gold_filename,'rb')

    line = gold_file.readline()
    gold_vector=readVector(line)

    return gold_vector
# end def readGoldFile(gold_filename)

def readErrsFile(errs_filename):
    err_vectors=[]
    errs=[]
    abs_errs=[]

    errs_file = open(errs_filename,'rb')

    line = errs_file.readline()
    while (line):
        line_s = line.split(' ')
        if line_s[1] == 'vector:':
            err_vectors.append(readVector(line))
        else:
            if line_s[0] == 'Errs':
                errs.append(float(line_s[1]))
            else:
                if line_s[0] == 'Abs':
                    abs_errs.append(float(line_s[2]))
        line = errs_file.readline()

    # Ensure that this is a well-formed file
    if len(err_vectors) != len(errs) or len(errs) != len(abs_errs):
        print "ERROR: mismatched output number!"
        exit(1)

    return (err_vectors,errs,abs_errs)
# end def readErrsFile(errs_filename)

def calcMSE(gold_vector,err_vectors):
    mses = []
    
    for v in err_vectors:
        mse_tmp = 0.0
        for o,e in zip(gold_vector,v):
            mse_tmp += (float(o)-float(e))**2
        mse_tmp /= len(v)
        mses.append(mse_tmp)

    return mses
# end def calcMSE(gold_vector,err_vectors)

def generateErrDist(in_filename,gold_in_filename,out_filename):
    # read the input files
    (err_vectors,errs,abs_errs) = readErrsFile(in_filename)
    gold_vector = readGoldFile(gold_in_filename)

    # calculate output errors -- mean squared error for vectors
    mses =  calcMSE(gold_vector,err_vectors)

    # write the output file for later plotting
    outfile = open(out_filename,'wb')
    cPickle.dump(errs, outfile)
    cPickle.dump(abs_errs, outfile)
    cPickle.dump(mses, outfile)
    outfile.close()

# end def generateErrDist(in_filename,gold_in_filename,out_filename)

def addOptions(parser):
    parser.add_option("-i", "--in_filename", dest="in_filename",
                      help="in_filename -- filename to read the erroneous output vectors and arithmetic errors.")
    parser.add_option("-o", "--out_filename", dest="out_filename",
                      help="out_filename -- filename for saving the python data for plotting.")
    parser.add_option("-g", "--gold_in_filename", dest="gold_in_filename",
                      help="gold_in_filename -- filename to read the golden output vector.")
# end def addOptions(parser)

#### main ####

# get filename from command line arguments
parser = OptionParser()
addOptions(parser)
(options, args) = parser.parse_args()

in_filename = options.in_filename
gold_in_filename = options.gold_in_filename
out_filename = options.out_filename

generateErrDist(in_filename,gold_in_filename,out_filename)
