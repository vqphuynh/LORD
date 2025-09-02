import pandas as pd
from lord_proxy import lord


data = pd.read_csv('data/005.csv', dtype='string')       #  Java LORD needs all data as string

# Split features and target
X = data.drop(columns=['class'])
y = data['class']

lord.start()
info_base = lord.InfoBase()
info_base.feed(X, y, file_chanel=False)

instance = X.iloc[0].to_list()
id_record = info_base.convert_features_to_ids(instance)
print(f'instance: {instance}')
print(f'id_record: {id_record}')
subset = id_record[2:6]
count = info_base.support_count(subset)
print(f'support count of {subset}: {count}')
