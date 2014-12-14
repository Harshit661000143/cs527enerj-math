#!/usr/bin/env python

from optparse import OptionParser

import os
import re
import numpy as np
import scipy.misc as spm
from scipy import stats
import matplotlib.pyplot as plt
import statsmodels.api as sm
from statsmodels.sandbox.regression.predstd import wls_prediction_std
from statsmodels.stats.outliers_influence import summary_table
import statsmodels.stats.outliers_influence as oi
import cPickle                         # to read and write values to binary file

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

def returnOutliers(results, x, y, alpha=0.05):
    o_x = []
    o_y = []

    #print results.cov_params().shape[0]
    exog = results.model.exog
    #print exog.shape
    #print x.shape[0]
    pred_y, iv_l, iv_u = wls_prediction_std(results, exog=x, weights=None, alpha=alpha)

    i = 0
    for val in y:
        if (val > iv_u[i] or val < iv_l[i]):
            o_x.append(x[i][1])
            o_y.append(val)
        i += 1

    return o_x, o_y
# end def returnOutliers(results, x, y, alpha=0.05)


def calcErrDistBug(in_filename,gold_in_filename,out_filename,title):
    errs = []
    abs_errs = []
    mses = []

    # read data
    in_file = open(in_filename,'rb')
    errs = cPickle.load(in_file)
    abs_errs = cPickle.load(in_file)
    mses = cPickle.load(in_file)
    in_file.close()
    
    g_in_file = open(gold_in_filename,'rb')
    g_errs = cPickle.load(g_in_file)
    g_abs_errs = cPickle.load(g_in_file)
    g_mses = cPickle.load(g_in_file)
    g_in_file.close()

    # ensure that we don't actually take the log of 0
    # FIXME: we may need to make this more dynamic so that it doesn't skew results.
    g_mses = np.array(g_mses)
    g_abs_errs = np.array(g_abs_errs)
    zeros = g_mses>0.0
    g_mses = g_mses[zeros]
    g_abs_errs = g_abs_errs[zeros]
    #zeros = g_abs_errs>0.0
    zeros = g_abs_errs>1.0E-14
    g_mses = g_mses[zeros]
    g_abs_errs = g_abs_errs[zeros]
    #print g_mses
    #print g_abs_errs

    #print g_mses 
    
    mses = np.array(mses)
    abs_errs = np.array(abs_errs)
    zeros = mses>0.0
    mses = mses[zeros]
    abs_errs = abs_errs[zeros]
    #zeros = abs_errs>0.0
    zeros = abs_errs>1.0E-14
    mses = mses[zeros]
    abs_errs = abs_errs[zeros]
    skip = 5
    mses = mses[::skip]
    abs_errs = abs_errs[::skip]

    #print g_mses
    #print np.log2(g_mses)

    g_dist = np.divide(g_mses,g_abs_errs)
    dist = np.divide(mses,abs_errs)

    # determine Ordinary Least Squares
    X = np.log2(g_abs_errs)
    X = sm.add_constant(X)
    #print X
    #print len(g_abs_errs)
    #model = sm.OLS(g_mses,X)
    model = sm.OLS(np.log2(g_mses),X)
    #model = sm.RLM(np.log2(g_mses),X)
    results = model.fit()
    #print results.params
    #print results.summary()
    #print dir(results)
    #print results.outlier_test()
    prstd, iv_l, iv_u = wls_prediction_std(results)

    st, data, ss2 = summary_table(results, alpha=0.05)
    
    #print oi.OLSInfluence(results).influence

    fittedvalues = data[:,2]
    predict_mean_se = data[:,3]
    predict_mean_ci_low, predict_mean_ci_upp = data[:,4:6].T
    predict_ci_low, predict_ci_upp = data[:,6:8].T

    # check we got the right things
    #print np.max(np.abs(results.fittedvalues - fittedvalues))
    #print np.max(np.abs(iv_l - predict_ci_low))
    #print np.max(np.abs(iv_u - predict_ci_upp))

    #legend = []
    #legend.append('> to < Mutation')
    #plt.loglog(abs_errs, mses, basex=2, linestyle='', marker='+', color='b')
    #plt.plot(np.log2(abs_errs), np.log2(mses), linestyle='', marker='+', color='b')
    #legend.append('> to < Mutation -- outliers')
    o_x,o_y = returnOutliers(results,sm.add_constant(np.log2(abs_errs)),np.log2(mses),alpha=0.05)
    #plt.plot(o_x, o_y, linestyle='', marker='*', color='b')
    #plt.plot(abs_errs, dist, linestyle='', marker='o', color='b')
    #print 'Mutation outliers: ' + `len(o_x)`
    
    #legend.append('Bug-free')
    #plt.loglog(g_abs_errs, g_mses, basex=2, linestyle='', marker='+', color='r')
    #plt.plot(np.log2(g_abs_errs), np.log2(g_mses), linestyle='', marker='+', color='r')
    #g_o_x,g_o_y = returnOutliers(results,sm.add_constant(np.log2(g_abs_errs)),np.log2(g_mses),alpha=0.05)
    #plt.plot(g_o_x, g_o_y, linestyle='', marker='*', color='r')
    #legend.append('Bug-free -- MSE/ABS')
    #plt.plot(g_abs_errs, g_dist, linestyle='', marker='o', color='r')
    #print 'Gold outliers: ' + `len(g_o_x)`

    #legend.append('95% CI -')
    #plt.loglog(g_abs_errs, iv_l, basex=2, linestyle='-', color='c')
    #plt.plot(np.log2(g_abs_errs), iv_l, linestyle='-', color='c')
    #legend.append('95% CI - manual')
    #plt.plot(np.log2(g_abs_errs), predict_ci_low, linestyle='-', color='m')
    #plt.loglog(g_abs_errs, results.fittedvalues, basex=2, linestyle='-', color='k')
    #plt.plot(np.log2(g_abs_errs), results.fittedvalues, linestyle='-', color='k')
    #legend.append('95% CI +')
    #plt.loglog(g_abs_errs, iv_u, basex=2, linestyle='-', color='g')
    #plt.plot(np.log2(g_abs_errs), iv_u, linestyle='-', color='g')
    #legend.append('95% CI + manual')
    #plt.plot(np.log2(g_abs_errs), predict_ci_upp, linestyle='-', color='y')
  
    #leg = plt.legend(legend, 'lower right',ncol=1)
   
    # fix up plotting to look nice
    #plt.suptitle(title, fontsize=35)
    #plt.xlabel('Sum of Absolute Execution Errors', fontsize=23)
    #plt.ylabel('Mean Squared Error of Output', fontsize=23)

    #plt.show()
    return len(o_x),len(mses)
# end def calcErrDistBug(in_filename,gold_in_filename,out_filename,'LU Output Errors Vs Input Errors')


def addOptions(parser):
    parser.add_option("-i", "--in_dir", dest="in_dir",
                      help="in_dir -- directory name containing text-based output errs files.")
    parser.add_option("-g", "--gold_in_filename", dest="gold_in_filename",
                      help="gold_in_filename -- filename to read golden plotting data.")
    parser.add_option("-o", "--out_filename", dest="out_filename",
                      help="out_filename -- filename for saving the plot.")
# end def addOptions(parser)

#### main ####

# get filename from command line arguments
parser = OptionParser()
addOptions(parser)
(options, args) = parser.parse_args()

in_dir = options.in_dir
gold_in_filename = options.gold_in_filename
out_filename = options.out_filename

in_files_sorted = sort_filenames(os.listdir(in_dir))

total_caught = 0
not_caught = []
for f in in_files_sorted:
    print 'f: ' + `f`
    outliers,num_points = calcErrDistBug(os.path.join(in_dir,f),gold_in_filename,out_filename,'LU Output Errors Vs Input Errors')
    print 'Outliers: ' + `outliers` + ' of ' + `num_points` + ' points'
    if outliers > num_points*0.1:
        total_caught += 1
        print 'Caught!'
    else:
        not_caught.append(f)

print 'Approx bugs caught: ' + `total_caught` + ' of ' + `len(in_files_sorted)` + ' bugs'
print not_caught
