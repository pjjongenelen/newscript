"""
Preprocessing steps taken on the MUC data file as received from Nguyen
"""

import os
import pandas as pd
from truecase import get_true_case

IN_PATH = os.getcwd() + "\data\muc-dev-tst1-4"
OUT_PATH = os.getcwd() + "\processed_data\muc.csv"

def load_data(path: str):
    """
    Load the MUC data, and split the location + text column
    """
    
    # location column contains location, date, and text
    return pd.read_csv(path, sep = '\t', names = ['muc_id', 'location'])  

def fix_seperators(df: pd.DataFrame) -> pd.DataFrame:
    """
    Fix mmiss a seperator between the date and article text
    """

    # find the rows without seperator hyphens
    seperator_errors = [index for index, row in df.iterrows() if " -- " not in row['location']]
    
    # add the hyphens
    for x in seperator_errors:
        df.loc[x]['location'] = df.loc[x]['location'][:17] + " --" + df.loc[x]['location'][17:]

    return df
    
    
def split_column(df, col1, col2, sep):
    """
    Splits textual content of a df column based on a separator
    """

    df[col1], df[col2] = zip(*[(d[0], d[1]) for d in [c.split(sep, 1) for c in df[col1]]])
    return df

def main():
    """
    Preprocess the MUC data that we got from Nguyen
    """
    # load data
    muc_data = load_data(IN_PATH)
    # fix the missing separators in lines ~1240
    muc_data = fix_seperators(muc_data)
    # split the location and date information
    muc_data = split_column(df = muc_data, col1='location', col2='date', sep=', ')
    # split the date and text information
    muc_data = split_column(df = muc_data, col1='date', col2='text', sep = ' -- ')
    # convert the article text to truecase
    muc_data['text'] = [get_true_case(text) for text in muc_data['text']]
    # save as csv to processed data folder
    muc_data.to_csv(OUT_PATH)

if __name__ == "__main__":
    main()