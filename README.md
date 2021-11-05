# LORD
A Java implementation on Eclipse IDE for LORD, a rule learning algorithm with the approach of searching for a locally optimal rule for each training example.



# Packages

run: contain executable java files, some main files such as

	1. LordRun.java: Do cross-validation benchmarks for LORD algorithm.

	2. JRIP.java: Do cross-validation benchmarks for JRIP, a WEKA implementation of RIPPER algorithm.
	WEKA library is already in 'libs' directory

	3. TrainTestSplitter.java: Employ WEKA lib to prepare k folds of train-test data sets for cross-validation benchmarks.

	4. DiscretizationRun.java: Discretize pairs of train-test data sets with FUSINTER method, 
	a discretization found from train set then apply the discretization to discretize the test set.

rl.eg: contain implementations of LORD algorithm

	1. Lord.java: a serial version of LORD

	2. MultiThreadLord.java: a multi-thread version of LORD


rl: contain implementations of PPC-tree, Nlist, R-tree structures, rule search strategies (greedy and brute-force), ...

prepr: contain classes for data preprocessing such as data discretization, data properties, selectors.
	It supports ARFF and CSV formats. Numeric attributes in an ARFF file are discretized automatically.
	All attributes in a CSV file are treated as nominal ones, so no discretization is performed.

evaluations: contain a general implementation for heuristic metrics

discretizer: contain implementations for discretization methods, MDLP and FUSINTER

arg: contain a parameters parser for LORD algorithm



# Data sets

'data/inputs' directory contains some  data sets

'data/outputs' directory contains the output data


# Execution

1. Every Java file in 'run' package can be built and executed with an IDE, e.g. Eclipse

2. Run with the complied .jar file, 'lord.jar'. Be sure that you are at the working directory containing 'lord.jar' file and 'data' directory. Some commands as follows.
	
	java -jar ./lord.jar	: for a general help
	
	parameters for LORD:

		--thread_count (-tc): number of threads to run, default value is the number of physical cores

        	--input_directory (-id): input directory of test and training files for cross-validation

        	--output_directory (-od): output directory for results, auto-generated if not specified

        	--metric_type (-mt): metric type, default value is MESTIMATE

        	--metric_argument (-ma): metric argument

		e.g. java -cp ./lord.jar run.LordRun -id .\data\inputs\german_arff -mt mestimate -ma 0

	java -cp ./lord.jar run.LordRun		: to view the parameters list of LORD		

	java -cp ./lord.jar run.JRIP <data filename> <number of folds> <seed> <optimize_run_count>	: do cross-validation benchmarks for JRIP

		e.g. java -cp ./lord.jar run.JRIP ./data/inputs/datasets/german.arff 10 1 2

	java -cp ./lord.jar run.TrainTestSplitter <data filename> <number of folds> <seed> <output_format>

		<output_format> can be 'arff' or 'csv'

		e.g. java -cp ./lord.jar run.TrainTestSplitter ./data/inputs/datasets/german.arff 10 1 arff

	java -cp ./lord.jar run.DiscretizationRun <data directory> <one_outputfile>

		<data directory> directory containing pairs of train-test data sets

		<one_outputfile> 'true' or 'false', if the value is 'true', the discretized test set will be written after the discretized train set in a single file.
		If  the value is 'false', the discretized train and test sets will be written into seperate files.

