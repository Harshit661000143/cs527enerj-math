import cPickle
from os.path import exists
import os, subprocess, sys
error=float(0.0000000)
cum_error=float(0.0000000)
LUDecomposition='LUDecomposition_'
work=str(sys.argv[1])
Magnitude='.out.Magnitude.'
Mantissa='10'
collect='collect_error'
i='i'
it='6'
last='.tmp'
error_value='error_value'
collect_error=[]
for x in xrange(0, 52):
    #print x	
    Mantissa=str(x)
    for y in xrange(1, 11):
	#print y
    	it=str(y)
	inputfile= LUDecomposition + work + Magnitude + Mantissa +i + it + last 
	print inputfile
	if exists(inputfile):
		with open(inputfile) as f:
			mutant_file_content = f.readlines()
			for line in mutant_file_content:
				line_elem= line.split(':')
				##print line_elem	
				if(len(line_elem)< 2):
					pass
				else:
					if(line_elem[0].strip()=='Error value') or(line_elem[0].strip()=='Error Value') :
						
						error=float(line_elem[1].rstrip())
						##print error	
						cum_error=float(cum_error)+error
						##print cum_error	
								
					else:	
						pass
						          			
		##print inputfile
              	collect_error.append(cum_error)
		##print collect_error
	        cum_error=float(0.0000000)
		if (x==51) and (y==10):
			out_path=os.getcwd()
			#print out_path

	else:
		pass

##i#print collect_error
##print 'I reach'
out_path=os.getcwd()
print out_path
out_path=out_path+error_value
outfile = open(out_path,'wb')
outfile.writelines(str(collect_error))
outfile.close()
collect_error=[]
cum_error=float(0.0000000)
			#os.chdir(prev)
			
			
