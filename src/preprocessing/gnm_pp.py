import numpy as np
import pandas as pd
from tqdm import tqdm


COLS = ['date', 'article_contentRaw', 'article_domain', 'article_author', 'gdelt_ActionGeo_CountryCode']
ROOT = 'C:\\Users\\timjo\\PycharmProjects\\newscript'


def get_date(date: int) -> str:
    return str(date)[:4] + "-" + str(date)[4:6] + "-" + str(date)[6:8]


# load the correct columns for all data
for x in tqdm(range(1572)):
    with open(ROOT + f'\\data\\database_dump_drugs\\{x}.json') as f:
        if x == 0:
            df = pd.read_json(f, convert_dates=False)[COLS]
        else:
            art = pd.read_json(f, convert_dates=False)[COLS]
            df = df.append(other=art)
            
# rename columns
df = df.rename(columns = {'article_contentRaw': 'text', 'article_domain': 'origin', 'article_author': 'author', 'gdelt_ActionGeo_CountryCode': 'location'})

# convert int date to correct format            
df['date'] = [get_date(dat) for dat in df['date'].tolist()]
df['date'] = pd.to_datetime(df['date'])

# drop duplicate article texts
df = df.drop_duplicates(subset='text')

# remove exceedingly large (len > 15.000) and short (len < 500) texts
df['text_length'] = df.text.str.len()
df = df[df.text_length.notna()]
df = df[df.text_length > 500]
df = df[df.text_length < 15000]
df = df.drop(columns=['text_length'], axis=1)

# replace empty strings and lists with NaN
df = df.replace('', np.NaN)

auths = []
for auth in df['author']:
    if isinstance(auth, list):
        auths.append(np.NaN)
    else:
        auths.append(auth)
df['author'] = auths


df = df.reset_index(drop=True)

# draw a sample 
sample = df.sample(1700)
sample = sample.reset_index(drop=True)

sample.to_csv(f"{ROOT}\\processed_data\\gnm.csv")