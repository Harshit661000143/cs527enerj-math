#!/usr/bin/env python

from optparse import OptionParser

import numpy as np
import scipy.misc as spm
import itertools
import os
import re
import cPickle                         # to read and write values to binary file

def readTextFileSingleLine(filename):
    vector = []

    in_file = open(filename,'r')

    line = in_file.readline()
    while (line):
        #print line
        vector.append(float(line))
        line = in_file.readline()
    
    return vector
# end def readTextFileSingleLine(filename)

def readTextFile(filename):
    vector = []

    in_file = open(filename,'r')

    line = in_file.readline() # reads an output vector for entire run
    if not line[0] == '[':
        in_file.close()
        return readTextFileSingleLine(filename)

    line = re.sub('\'','',line) # remove all 's from line
    line = re.sub('[\[\]]','',line) # remove surrounding brackets
    line = re.sub(' ','',line) # remove all spaces

    line_s = line.split(',') # get individual values
    for val in line_s:
        vector.append(float(val))
    
    in_file.close()
    return vector
# end def readTextFile(filename)

def writePickleFile(filename,e_error,a_error,o_error):
    # write the output file for later plotting
    outfile = open(out_filename,'wb')
    cPickle.dump(a_error, outfile)
    cPickle.dump(a_error, outfile)
    cPickle.dump(o_error, outfile)
    outfile.close()
# end def writePickleFile(filename,e_error,a_error,o_error)

def sort_filenames(in_files):
    in_files_sorted = []
    in_files_vals = []
    in_files_pattern = re.compile('\D*(\d+)\D*')
    for f in in_files:
        match = in_files_pattern.search(f)
        if match:
            #print 'matched: ' + `match.group(1)` + ' for: ' + f
            in_files_vals.append(int(match.group(1)))
    
    # sort by digits
    in_files_ind = [i[0] for i in sorted(enumerate(in_files_vals), key=lambda x:x[1])]
    for i in in_files_ind:
        in_files_sorted.append(in_files[i])
    

    return in_files_sorted
# end def sort_filenames(in_files)

def addOptions(parser):
    parser.add_option("-i", "--in_dir", dest="in_dir",
                      help="in_dir -- directory name containing text-based output errs files.")
    parser.add_option("-a", "--abs_in_dir", dest="abs_in_dir",
                      help="abs_in_dir -- directory containing text-based absolution injected errs.")
    parser.add_option("-o", "--out_dir", dest="out_dir",
                      help="out_filename -- filename for saving the pickle data.")
# end def addOptions(parser)

#### main ####

# get filename from command line arguments
parser = OptionParser()
addOptions(parser)
(options, args) = parser.parse_args()

in_dir = options.in_dir
abs_in_dir = options.abs_in_dir
out_dir = options.out_dir


in_dir = os.path.abspath(in_dir)
abs_in_dir = os.path.abspath(abs_in_dir)
out_dir = os.path.abspath(out_dir)

# do re match for digits in output err directory
in_files_sorted = sort_filenames(os.listdir(in_dir))
abs_in_files_sorted = sort_filenames(os.listdir(abs_in_dir))

print abs_in_files_sorted

for i,a in itertools.izip(in_files_sorted,abs_in_files_sorted):
    print 'i: ' + `i` + ' a: ' + `a`
    o_error = readTextFile(os.path.join(in_dir,i))
    a_error = readTextFile(os.path.join(abs_in_dir,a))
    e_error = a_error #FIXME: don't actually have the raw sum of execution error, so use the absolute execution error just to make pickle files correctly formatted.
    if not len(o_error) == len(a_error):
        print "ERROR: injected (" + `len(a_error)` + ") and output (" + `len(o_error)` + ") error numbers do not match!"
    

    # write the output file for later analysis and plotting
    outfile = open(os.path.join(out_dir,i),'wb')
    cPickle.dump(e_error, outfile)
    cPickle.dump(a_error, outfile)
    cPickle.dump(o_error, outfile)
    outfile.close()

