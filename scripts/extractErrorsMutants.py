import cPickle
from os.path import exists
import os, subprocess, sys
error=float(0.0000000)
cum_error=float(0.0000000)
LUDecomposition='LUDecomposition_'
#Mutation='Base'
Magnitude='.out.Magnitude.'
Mantissa='10'
collect='collect_error'
i='i'
last='.tmp'
error_value='error_value'
collect_error=[]
with open('Mutation.txt') as f:
	mutant_file_content = f.readlines()
	for line in mutant_file_content:
		line_elem= line.strip()
		line_elem2= line.split()
		length=len(line_elem2)
		length=length-1
		for x in range(0, length):
			work=line_elem2[x]
			print work
			prev=os.getcwd()
			outputfile=work+error_value
			print outputfile
			for x in xrange(0, 52):
    				Mantissa=str(x)
    				for y in xrange(1, 11):
    					it=str(y)
					inputfile= LUDecomposition + work + Magnitude + Mantissa +i + it + last 
					if exists(inputfile):
						with open(inputfile) as f:
							mutant_file_content = f.readlines()
							for line in mutant_file_content:
								line_elem= line.split(':')
								if(len(line_elem)< 2):
									pass
								else:
									if(line_elem[0].strip()=='Error value') or (line_elem[0].strip()=='Error Value'):
										error=float(line_elem[1].rstrip())
										cum_error=float(cum_error)+error
										print cum_error	
									else:	
										pass
						          			
                        				collect_error.append(cum_error)
							cum_error=float(0.0000000)
					else:
						pass
			out_path=work+error_value
			outfile = open(out_path,'wb')
 			outfile.writelines(str(collect_error))
			outfile.close()
			collect_erro=[]
			cum_error=float(0.0000000)
			
			
