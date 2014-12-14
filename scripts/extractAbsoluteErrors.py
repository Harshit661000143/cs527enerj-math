import pickle
from os.path import exists
import sys
import os
collect_error=[]
filename=str(sys.argv[1])
error=float(0.0000000)
with open(filename) as f:
	mutant_file_content = f.readlines()
	for line in mutant_file_content:
		line_elem= line.strip()
		line_elem2= line.split()
		#print line_elem2," ",len(line_elem2)
		if(len(line_elem2)< 3):
			pass

		else:
			error=line_elem2[-1]
              		collect_error.append(error)
			
	out_path=os.getcwd()
	print out_path
	out_path=out_path+'AbsError_value'
	outfile = open(out_path,'wb')
	outfile.writelines(str(collect_error))
	outfile.close()
	collect_error=[]
