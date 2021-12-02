"""
Most important general functions that are used in the template extraction process
"""

from json import load
import helpers
import os
import pandas as pd

def load_muc(path = 'src/srcNguyen/muc-dev-tst1-4'):
    print(os.getcwd())

    # load the muc data from the file we got from Nguyen
    df = pd.read_csv(path, sep = '\t', names = ['muc_id', 'location'])

    # split the second column
    df = helpers.split_df_column(df, col1 = 'location', col2 = 'date', sep = ', ')
    
    # split the third column
    # lines 1243 and 1246 do not have ' -- ' between the date and text, so let's insert that before the next split
    for x in range(1243, 1246):
        df.loc[x]['date'] = df.loc[x]['date'][:9] + " --" + df.loc[x]['date'][9:]
    df = helpers.split_df_column(df, col1 = 'date', col2 = 'text', sep = ' -- ')

    return df

def load_gnm(data_path: str, cols: list, n = 776569) -> list:
    """Retrieves a specified number (n) of articles from the drugs database

    Parameters
    ----------
    data_path : str
        The file location of the database_dump_drugs folder or the pickle file
    cols : list
        Which columns to get from the original dataset
    n : int
        Number of articles requested (default is all)

    Returns
    -------
    list
        A list of lists per article
    """
    
    processing = True
    json_doc = 0
    data = []    

    while processing == True:
        with open(f'{data_path}/{json_doc}.json') as f:
            raw = load(f)
            json_doc += 1
            for _ in raw:
                content = []
                for col in cols:
                    content.append(_[col])
                data.append(content)
        
        if json_doc == 1572:
            processing = False
            print(f'Reached maximum number of articles ({len(data)}), terminating loading process.')
            # there are 779569 total articles, out of which 178647 unique
        elif len(data) >= n:
            processing = False
            print(f"Loaded {n} articles.")
        else:
            if json_doc % 100 == 0:
                    print(f'Loaded {len(data)} articles from {json_doc} out of 1572 json files.')
    
    return data[:n]