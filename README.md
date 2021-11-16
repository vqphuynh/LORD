# LORD
A Java implementation on Eclipse IDE for LORD, a rule learning algorithm with the approach of searching for a locally optimal rule for each training example.



# Packages

run: contain executable java files, some main files such as

	1. LordRun.java: Do cross-validation benchmarks for LORD algorithm.

	2. WLordRun.java: Do cross-validation benchmarks for WLORD algorithm, a LORD adaptation to interface with WEKA.

	3. JRIP.java: Do cross-validation benchmarks for JRIP, a WEKA implementation of RIPPER algorithm.
	WEKA library is already in 'libs' directory

	4. TrainTestSplitter.java: Employ WEKA lib to prepare k folds of train-test data sets for cross-validation benchmarks.

	5. DiscretizationRun.java: Discretize pairs of train-test data sets with FUSINTER method, 
	a discretization found from train set then apply the discretization to discretize the test set.

rl.eg: contain implementations of LORD algorithm

	1. Lord.java: a multi-thread version of LORD

	2. WLord.java: a LORD adaptation to interface with WEKA


rl: contain implementations of PPC-tree, Nlist, R-tree structures, rule search strategies (greedy and brute-force), ...

prepr: contain classes for data preprocessing such as data discretization, data properties, selectors.
	It supports ARFF and CSV formats. Numeric attributes in an ARFF file are discretized automatically.
	All attributes in a CSV file are treated as nominal ones, so no discretization is performed.

evaluations: contain a general implementation for heuristic metrics: PRECISION, LAPLACE, ENTROPY, MESTIMATE, LINEAR_COST, RELATIVE_COST, COSINE

discretizer: contain implementations for discretization methods, MDLP and FUSINTER

arg: contain a parameters parser for LORD algorithm



# Data sets

'data/inputs' directory contains some  data sets

'data/outputs' directory contains the output data


# Execution

1. Every Java file in 'run' package can be built and executed with an IDE, e.g. Eclipse

2. Run with the complied .jar file, 'lord.jar'. Be sure that you are at the working directory containing 'lord.jar' file and 'data' directory. Some commands as follows.
	
	java -jar ./lord.jar	: for a general help

	
	java -cp ./lord.jar run.LordRun		: to view the parameters list of LORD

	parameters for LORD:

		--thread_count (-tc): number of threads to run, default value is the number of physical cores

		--input_directory (-id): input directory of test and training files for cross-validation, 

			contain 'train' for a train set, 'test' for a test set, and pairing based on an index number,

			e.g. pairs of train-test sets: (train_01.arff, test_01.arff), (train_02.arff, test_02.arff), ...

		--output_directory (-od): output directory for results, auto-generated if not specified

		--metric_type (-mt): metric type, support PRECISION, LAPLACE, ENTROPY, MESTIMATE, LINEAR_COST, RELATIVE_COST, COSINE

		--metric_argument (-ma): metric argument (if possible) for the metric type

			e.g. java -cp ./lord.jar run.LordRun -id .\data\inputs\german_arff -mt mestimate -ma 0


	java -cp ./lord.jar run.WLordRun	: to view the parameter list of WLORD (LORD adaptation to interface with WEKA)

	java -cp ./lord.jar run.WLordRun <data_filename> <number_of_folds> <seed_value> <metric_type> <metric_arg> <discretize_attr>	: do cross-validation benchmarks for WLORD

		e.g. java -cp ./lord.jar run.WLordRun data\inputs\datasets\german.arff 10 1 mestimate 0.1

	
	java -cp ./lord.jar run.JRIP <data_filename> <number_of_folds> <seed_value> <optimize_run_count>	: do cross-validation benchmarks for JRIP

		e.g. java -cp ./lord.jar run.JRIP ./data/inputs/datasets/german.arff 10 1 2


	java -cp ./lord.jar run.TrainTestSplitter <data_filename> <number_of_folds> <seed_value> <output_format>

		<output_format> can be 'arff' or 'csv'

		e.g. java -cp ./lord.jar run.TrainTestSplitter ./data/inputs/datasets/german.arff 10 1 arff


	java -cp ./lord.jar run.DiscretizationRun <data_directory> <one_outputfile>

		<data_directory> directory containing pairs of train-test data sets

		<one_outputfile> 'true' or 'false', if the value is 'true', the discretized test set will be written after the discretized train set in a single file.
		If  the value is 'false', the discretized train and test sets will be written into seperate files.

