import pickle
from os.path import exists

error=float(0.0000000)
with open('myfile.txt') as f:
	mutant_file_content = f.readlines()
	for line in mutant_file_content:
		line_elem= line.strip()
		line_elem2= line.split()
		#print line_elem2," ",len(line_elem2)
		if(len(line_elem2)< 3):
			pass

		else:
			error=line_elem2[-1]
			print error
