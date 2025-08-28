import pandas as pd
from io import StringIO
import tempfile
import os
from sklearn.base import BaseEstimator, ClassifierMixin
import jpype
from jpype import JByte, JArray, JString

def start():
    print(os.getcwd())
    jar_path = "lord_proxy/lord.jar"
    if not jpype.isJVMStarted():
        jpype.startJVM(jpype.getDefaultJVMPath(), "-ea", classpath=[jar_path])


def get_datatype(df: pd.DataFrame) -> list[str]:
    """
    Return a list of data types of features in the data as 'numeric' or 'string'.

    Parameters
    ----------
    df : pd.DataFrame.
    """

    datatypes = []
    for col in df.columns:
        try:
            pd.to_numeric(df[col], errors="raise")  # test conversion
            datatypes.append("NUMERIC")
        except Exception:
            datatypes.append("NOMINAL")
    return datatypes



class InfoBase:
    def __init__(self, learner):
        self.learner = learner
        self.selector_nlists = learner.getSelectorNlists()
        self.constructing_selectors = list(learner.getConstructingSelectors())
        self.selector_id_records = learner.getSelectorIDRecords()
        self.class_ids = list(learner.getClassIDs())
        self.RuleSearcher = jpype.JClass("rl.RuleSearcher")
        self.INlist = jpype.JClass("rl.INlist")

    def support_count(self, selector_ids):
        nlist_array = jpype.JArray(self.INlist)([self.selector_nlists[i] for i in selector_ids])
        return self.RuleSearcher.calculate_nlist_direct(nlist_array).supportCount()
    

class LocalRuleClassifier(BaseEstimator, ClassifierMixin):
    def __init__(self, metric="MESTIMATE", metric_arg=0.1):
        self.metric = metric
        self.metric_arg = metric_arg
        self.METRIC_TYPES = jpype.JClass("evaluations.HeuristicMetricFactory$METRIC_TYPES")
        self.IntHolder = jpype.JClass("rl.IntHolder")
        self.RuleLearnerClass = jpype.JClass("rl.eg.Lord")
        self.learner = self.RuleLearnerClass()

    def fit(self, X, y, file_chanel: bool = False):
        if isinstance(X, pd.DataFrame) and isinstance(y, pd.Series):
            datatypes = get_datatype(X)
            datatypes.append("NOMINAL")             # data type of the class attribute
            datatypes = JArray(JString)(datatypes)  # convert to java string array
            df = pd.concat([X, y], axis=1)
            if file_chanel:
                # Send data to file
                fdesc, path = tempfile.mkstemp(suffix=".csv", text=True)
                try:
                    df.to_csv(path, sep=",", index=False)
                    os.close(fdesc)
                    return self.fit_csv(path, datatypes)
                finally:
                    os.remove(path)                
            else:  
                # Send data in memory to Lord via a stream
                buffer = StringIO()
                df.to_csv(buffer, index=False)
                csv_bytes = buffer.getvalue().encode('utf-8')
                java_bytes = JArray(JByte)(csv_bytes)
                ByteArrayInputStream = jpype.JClass("java.io.ByteArrayInputStream")
                input_stream = ByteArrayInputStream(java_bytes)
                return self.fit_stream(input_stream, datatypes)
        else:
            raise TypeError(f"X/y should be Pandas DataFrame/Series. Unsupported type for X and/or y: {type(X)}, {type(y)}")
    

    def fit_stream(self, input_stream, datatypes):
        metric_enum = getattr(self.METRIC_TYPES, self.metric)
        self.learner.declareAttributeTypes(datatypes)
        self.learner.fetch_information(input_stream)
        self.learner.learning(metric_enum, float(self.metric_arg))
        return self
    

    def fit_csv(self, file_path, datatypes):
        self.train_file = file_path
        metric_enum = getattr(self.METRIC_TYPES, self.metric)
        self.learner.declareAttributeTypes(datatypes)
        self.learner.fetch_information(file_path)
        self.learner.learning(metric_enum, float(self.metric_arg))
        return self


    def predict(self, X):
        results = []

        if isinstance(X, pd.DataFrame):
            for _, row in X.iterrows():
                p_strings = row.astype(str).tolist()
                j_strings = JArray(JString)(p_strings)
                holder = self.IntHolder(-1)
                # X without the target class, use method 'predict_noclass'
                # id of the target class returned in "holder"
                self.learner.predict_noclass(j_strings, holder) 
                results.append(str(self.learner.getValue(holder.value)))    # need to convert Java String to Python String
            return results
        else:
            raise TypeError(f"X must be a Pandas DataFrame, unsupported type for X: {type(X)}")
        

    def get_rule_count(self):
        return self.learner.rm.ruleList.size()
    

    def get_avg_rule_length(self):
        rule_length = 0
        for rule in  self.learner.rm.ruleList:
            rule_length += rule.body.length
        return rule_length/self.get_rule_count()


    def get_info_base(self):
        return InfoBase(self.learner)


    def set_params(self, **params):
        for key, value in params.items():
            setattr(self, key, value)
        return self


    def get_params(self, deep=True):
        return {
            "metric": self.metric,
            "metric_arg": self.metric_arg
        }
    

class LocalRuleClassifier_OneHotExporter(LocalRuleClassifier):
    def __init__(self, metric="MESTIMATE", metric_arg=0.1):
        self.metric = metric
        self.metric_arg = metric_arg
        self.METRIC_TYPES = jpype.JClass("evaluations.HeuristicMetricFactory$METRIC_TYPES")
        self.IntHolder = jpype.JClass("rl.IntHolder")
        self.RuleLearnerClass = jpype.JClass("export.onehot.LordOneHotExport")
        self.learner = self.RuleLearnerClass()


    def export_onehot(self, dir_path, y_test):
        if y_test:
            j_strings = JArray(JString)(y_test)
        else:
            j_strings = jpype.JObject(None, JArray)
        self.learner.export_onehot(dir_path, j_strings)