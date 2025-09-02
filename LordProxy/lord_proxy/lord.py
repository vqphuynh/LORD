import pandas as pd
from io import StringIO
import tempfile
import os
from sklearn.base import BaseEstimator, ClassifierMixin
import jpype
from jpype import JByte, JArray, JString, JInt

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
                    return self._fit_csv(path, datatypes)
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
                return self._fit_stream(input_stream, datatypes)
        else:
            raise TypeError(f"X/y should be Pandas DataFrame/Series. Unsupported type for X and/or y: {type(X)}, {type(y)}")
    

    def _fit_stream(self, input_stream, datatypes):
        metric_enum = getattr(self.METRIC_TYPES, self.metric)
        self.learner.declareAttributeTypes(datatypes)
        self.learner.fetch_information(input_stream)
        self.learner.learning(metric_enum, float(self.metric_arg))
        return self
    

    def _fit_csv(self, file_path, datatypes):
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



class InfoBase():
    def __init__(self):
        self.Supporter = jpype.JClass("rl.Supporter")
        self.learner = jpype.JClass("rl.eg.Lord")()

    def feed(self, X, y, file_chanel: bool = False):
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
                    return self._feed_csv(path, datatypes)
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
                return self._feed_stream(input_stream, datatypes)
        else:
            raise TypeError(f"X/y should be Pandas DataFrame/Series. Unsupported type for X and/or y: {type(X)}, {type(y)}")
    

    def _feed_stream(self, input_stream, datatypes):
        self.learner.declareAttributeTypes(datatypes)
        self.learner.fetch_information(input_stream)
        self.selector_nlists = self.learner.getSelectorNlist()
        return self
    

    def _feed_csv(self, file_path, datatypes):
        self.train_file = file_path
        self.learner.declareAttributeTypes(datatypes)
        self.learner.fetch_information(file_path)
        self.selector_nlists = self.learner.getSelectorNlist()
        return self
    

    def support_count(self, selector_ids: list) -> int:
        nlist = self.selector_nlists[selector_ids[0]]
        for i in range(1, len(selector_ids)):
            nlist = self.Supporter.create_nlist(nlist, self.selector_nlists[selector_ids[i]])
        return nlist.supportCount()
    

    def convert_features_to_ids(self, features: list[str]) -> list[int]:
        buffer = JArray(JInt)(len(features))
        return self.learner.convert_values_to_selectorIDs(features, buffer)