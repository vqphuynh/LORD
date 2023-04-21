# LORD
A Java implementation for LORD, a rule learning algorithm with the approach of searching for a locally optimal rule for each training example.
The rule searching is completely independent among training examples so LORD inherently can run parallel in shared-memory or distributed environments.

Details of LORD is from the article:
Huynh, V.Q.P., Fürnkranz, J. & Beck, F. Efficient learning of large sets of locally optimal classification rules. Mach Learn 112, 571–610 (2023). 
https://doi.org/10.1007/s10994-022-06290-w


# Packages

run: contain executable java files, some main files:

	1. LordRun.java: Do cross-validation benchmarks for LORD algorithm.

	2. LordStarRun.java: Do cross-validation benchmarks for LORDStar algorithm, an improved version of LORD for running time. LORDStar should apply only on very large datasets in case the running time is critical.
	Anyway, Lord is easily implemented to run parallel in distributed environments.
	
	3. LordLoopRun.java: Do cross-validation benchmarks for LORDLoop (also called OverLORD) algorithm, a variant of LORD, which iteratively performs loops of growth and pruning phases until the currently best rule can not be improved more.

	4. WLordRun.java: Do cross-validation benchmarks for WLORD algorithm, a LORD adaptation to interface with WEKA.

	5. TrainTestSplitter.java: Employ WEKA lib to prepare k folds of train-test data sets for cross-validation benchmarks. The seed set to 1 for all data sets.

	6. DiscretizationRun.java: Discretize pairs of train-test data sets with FUSINTER method, a discretization found from train set then apply the discretization to discretize the test set.


rl.eg: contain implementations of LORD algorithm, and variants

	1. Lord.java: a multi-thread version of LORD

	2. LordStar.java: LORDStar is an improved version of LORD for running time. LORDStar should apply only on very large data sets in case the running time is critical.
	Anyway, Lord is easily implemented to run parallel in distributed environments.
	
	3. LordLoopRun.java: LORDLoop (also called OverLORD) algorithm, a variant of LORD, which iteratively performs loops of growth and pruning phases until the currently best rule can not be improved more.

	4. WLord.java: a LORD adaptation to interface with WEKA


rl: contain fundamental implementations: PPC-tree, Nlist, R-tree structures, rule search strategies (greedy and brute-force), ...


prepr: contain classes for data preprocessing such as data loading, data discretization, selectors.
	It supports ARFF and CSV formats. Numeric attributes in an ARFF file are discretized automatically.
	Note: all attributes in a CSV file are treated as nominal ones, so no discretization is performed.


evaluations: contain a general and extendable implementation for heuristic metrics: PRECISION, LAPLACE, ENTROPY, MESTIMATE, LINEAR_COST, RELATIVE_COST, COSINE, ...


discretizer: contain implementations for discretization methods, MDLP and FUSINTER


arg: contain a parameters parser for LORD algorithm


utilities: contain class MemoryHistogramer.java for forcing Garbage Collector to run, support benchmark precisely memory occupied by a certain data structure.



# Data sets

'data/inputs' directory contains some data sets

'data/outputs' directory contains the output data



# Execution

1. Every Java file in 'run' package can be built and executed in a Java IDE, e.g. Eclipse


2. Run with the complied .jar file, 'lord.jar'. Be sure that you are at the working directory containing 'lord.jar' file and 'data' directory. Some commands as follows.
	
	java -jar ./lord.jar								: for a general help
	
	java -cp ./lord.jar run.LordRun						: to view the parameters list of LORD
	
	java -cp ./lord.jar run.LordStarRun					: to view the parameters list of LORDStar (the same as LORD's)
	
	java -cp ./lord.jar run.LordLoopRun					: to view the parameters list of LORDLoop (the same as LORD's)
	
	java -cp ./lord.jar run.WLordRun					: to view the parameter list of WLORD (LORD adaptation to interface with WEKA)
	
	java -cp ./lord.jar run.TrainTestSplitter			: to view the parameter list to prepare k folds of train-test data sets for cross-validation benchmarks
	
	java -cp ./lord.jar run.DiscretizationRun			: to view the parameter list to discretize pairs of train-test data sets with FUSINTER method


	parameters of LORD (also of LORDStar and LORDLoop):

		--thread_count (-tc): number of threads to run, default value is the number of physical cores

		--input_directory (-id): input directory of test and training files for cross-validation, 

			contain 'train' for a train set, 'test' for a test set, and pairing is based on an index number,

			e.g. pairs of train-test sets: (train_01.arff, test_01.arff), (train_02.arff, test_02.arff), ...
			
			use run.TrainTestSplitter to prepare test and training files for cross-validation

		--output_directory (-od): output directory for results, auto-generated if not specified

		--metric_type (-mt): metric type, non-case sensitive, support PRECISION, LAPLACE, ENTROPY, MESTIMATE, LINEAR_COST, RELATIVE_COST, COSINE

		--metric_argument (-ma): metric argument (if possible) for the metric type

		
	Examples of cross-validation benchmarks for LORD, LORDStar, LORDLoop:

		java -cp ./lord.jar run.LordRun -id .\data\inputs\german -mt mestimate -ma 0.1

		java -cp ./lord.jar run.LordStarRun -id .\data\inputs\adult -mt mestimate -ma 0.1
			
		java -cp ./lord.jar run.LordLoopRun -id .\data\inputs\adult -mt mestimate -ma 0.1

		Note: Numeric attributes in an ARFF file are discretized automatically, and all attributes in a CSV file are treated as nominal ones, so no discretization is performed.

	
	parameters of WLORD (WEKA-adapted LORD):  first 3 parameters are the same as LORD's

		--thread_count (-tc): number of threads to run, default value is the number of physical cores
			
		--metric_type (-mt): metric type, non-case sensitive, support PRECISION, LAPLACE, ENTROPY, MESTIMATE, LINEAR_COST, RELATIVE_COST, COSINE

		--metric_argument (-ma): metric argument (if possible) for the metric type

		--discretize_attribute (-da): whether numeric attributes will be discretized, boolean values: true/false, default value: true
			
			
	cross-validation benchmarks for WLORD (WEKA-adapted LORD)

		java -cp ./lord.jar run.WLordRun <data_filename> <number_of_folds> <seed_value> <metric_type> <metric_arg> <discretize_attr>

		Example: java -cp ./lord.jar run.WLordRun data\inputs\datasets\german.arff 10 1 mestimate 0.1


	prepare k-fold stratified cross-validation data

		java -cp ./lord.jar run.TrainTestSplitter <data_filename> <number_of_folds> <seed_value> <output_format>

			<output_format> can be 'arff' or 'csv'

			The output files generated in the same directory of the input file

			Examples: java -cp ./lord.jar run.TrainTestSplitter ./data/inputs/datasets/german.arff 10 1 arff


	discretize data (use this tool in case benchmark other algorithms on the same discretized data as LORD's, a discretization found from train set is used to discretize the test set.)

		java -cp ./lord.jar run.DiscretizationRun <data_directory> <one_outputfile>

			<data_directory> directory containing pairs of train-test data sets, e.g. (train_01.arff, test_01.arff), (train_02.arff, test_02.arff)

			<one_outputfile> 'true' or 'false'.
			
				If the value is 'true', the discretized test set will be written after the discretized train set in a single file.
				
				If  the value is 'false', the discretized train and discretized test sets will be written into seperate files.

			The output files generated in the same directory of the input files

			Examples: java -cp ./lord.jar run.DiscretizationRun ./data/inputs/german true

