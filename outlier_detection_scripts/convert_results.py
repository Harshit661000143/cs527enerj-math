#!/usr/bin/env python

from optparse import OptionParser

import numpy as np
import scipy.misc as spm
import cPickle                         # to read and write values to binary file

def readTextFile(filename):
    vector = []

    in_file = open(filename,'r')

    line = in_file.readline()
    while (line):
        vector.append(float(line))
        line = in_file.readline()
    
    return vector
# end def readTextFile(filename)

def addOptions(parser):
    parser.add_option("-i", "--in_filename", dest="in_filename",
                      help="in_filename -- filename containing text-based output errs.")
    parser.add_option("-a", "--abs_in_filename", dest="abs_in_filename",
                      help="abs_in_filename -- filename containing text-based absolution injected errs.")
    parser.add_option("-o", "--out_filename", dest="out_filename",
                      help="out_filename -- filename for saving the pickle data.")
# end def addOptions(parser)

#### main ####

# get filename from command line arguments
parser = OptionParser()
addOptions(parser)
(options, args) = parser.parse_args()

in_filename = options.in_filename
abs_in_filename = options.abs_in_filename
out_filename = options.out_filename


o_error = readTextFile(in_filename)
a_error = readTextFile(abs_in_filename)

if not len(o_error) == len(a_error):
    print "ERROR: injected (" + `len(a_error)` + ") and output (" + `len(o_error)` + ") error numbers do not match!"


# write the output file for later plotting
outfile = open(out_filename,'wb')
cPickle.dump(a_error, outfile)
cPickle.dump(a_error, outfile)
cPickle.dump(o_error, outfile)
outfile.close()

