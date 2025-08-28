import pandas as pd
from sklearn.model_selection import KFold
from sklearn.metrics import accuracy_score

import glob
import os
import time

from lord_proxy import lord, utilities


def run_cross_validate(file_path, target_column, base_dir, cv_folds=10):
    data = pd.read_csv(file_path, dtype='string')       #  Java LORD needs all data as string

    # Split features and target
    X = data.drop(columns=[target_column])
    y = data[target_column]

    # Create KFold object
    kf = KFold(n_splits=cv_folds, shuffle=True, random_state=42)

    # Iterate over folds
    accuracies = []
    runtimes = []
    rule_lengths = []
    rule_counts = []
    results = {"rule_lengths": rule_lengths, 
               "rule_counts": rule_counts, 
               "runtimes": runtimes, 
               "accuracies": accuracies}
    for fold_idx, (train_idx, test_idx) in enumerate(kf.split(X), start=1):
        X_train, X_test = X.iloc[train_idx], X.iloc[test_idx]
        y_train, y_test = y[train_idx], y[test_idx]
        print(f"Fold {fold_idx}")
        print("X_train shape:", X_train.shape, "X_test shape:", X_test.shape)
        print("y_train length:", len(y_train), "y_test length:", len(y_test))
        
        start = time.perf_counter()
        model = lord.LocalRuleClassifier()
        model.fit(X_train, y_train, file_chanel=False)
        y_pred = model.predict(X_test)
        end = time.perf_counter()
        acc = accuracy_score(y_test.tolist(), y_pred)
        accuracies.append(acc)
        runtimes.append(end-start)
        rule_counts.append(model.get_rule_count())
        rule_lengths.append(model.get_avg_rule_length())
        # utilities.store_data_split(X_train, y_train, os.path.join(base_dir, f"train_{fold_idx:02d}.csv"))
        # utilities.store_data_split(X_test, y_test, os.path.join(base_dir, f"test_{fold_idx:02d}.csv"))
        # utilities.store_prediction(y_test, y_pred, os.path.join(output_dir, f"prediction_{fold_idx:02d}.csv"))
    
    utilities.store_result(os.path.join(base_dir, "results.txt"), results)



######################
lord.start()
file_paths = glob.glob("data/**/*.csv", recursive=True)
# file_paths = ["data/005.csv", "data/016.csv", "data/german.csv"]
for file_path in file_paths:
    filename = os.path.basename(file_path)
    file_name_no_ext = os.path.splitext(filename)[0]
    base_dir = os.path.join("outputs/lord_benchmark", file_name_no_ext)
    os.makedirs(base_dir, exist_ok=True)
    run_cross_validate(file_path, target_column='class', base_dir=base_dir)
