import statistics
import pandas as pd


def store_result(file_path, results):
    with open(file_path, "w") as f:
        f.write('metrics,')
        for idx in range(1, len(results['runtimes'])+1):
            f.write(f'fold{idx:02d},')
        f.write('avg\n')
        
        for key, values in results.items():
            f.write(f'{key},')
            if key == "accuracies":
                digit_n = 4
            else:
                digit_n = 2
            for value in values:
                f.write(f'{round(value, digit_n)},')
            f.write(f'{round(statistics.mean(values), digit_n)}\n')


def store_prediction(y_test, y_pred, file_path):
    with open(file_path, "w") as f:
        f.write(f'y_test, y_pred, match\n')
        match = 0;
        for t, p in zip(y_test, y_pred):
            if t == p:
                match += 1
            f.write(f'{t}, {p}, {1 if t == p else 0}\n')
        f.write(f',,{match/len(y_test)}')


def store_data_split(X, y, file_path):
    if isinstance(X, pd.DataFrame) and isinstance(y, pd.Series):
        df = pd.concat([X, y], axis=1)
        df.to_csv(file_path, sep=",", index=False)
    else:
        raise TypeError(f"X/y should be Pandas DataFrame/Series. Unsupported type for X and/or y: {type(X)}, {type(y)}")