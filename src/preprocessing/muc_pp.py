"""
Preprocessing steps taken on the MUC data file as received from Nguyen
"""

import numpy as np
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

def muc_date(df):
    dates = []

    for date in df['date']:
        date_split = date.split(" ")

        mon = date_split[1]    

        if len(date_split) != 3:
            formatted = np.NaN
        elif mon == "DATE":
            formatted = np.NaN
        elif mon == "JAN":
            formatted = '19' + date_split[2] + '-01-' + date_split[0]
        elif mon == "FEB":
            formatted = '19' + date_split[2] + '-02-' + date_split[0]
        elif mon == "MAR":
            formatted = '19' + date_split[2] + '-03-' + date_split[0]
        elif mon == "APR" or mon == "APRIL":
            formatted = '19' + date_split[2] + '-04-' + date_split[0]
        elif mon == "MAY":
            formatted = '19' + date_split[2] + '-05-' + date_split[0]
        elif mon == "JUN":
            formatted = '19' + date_split[2] + '-06-' + date_split[0]
        elif mon == "JUL":
            formatted = '19' + date_split[2] + '-07-' + date_split[0]
        elif mon == "AUG" or mon == "AUGUST":
            formatted = '19' + date_split[2] + '-08-' + date_split[0]
        elif mon == "SEP" or mon == "SEPT":
            formatted = '19' + date_split[2] + '-09-' + date_split[0]
        elif mon == "OCT":
            formatted = '19' + date_split[2] + '-10-' + date_split[0]
        elif mon == "NOV":
            formatted = '19' + date_split[2] + '-11-' + date_split[0]
        elif mon == "DEC":
            formatted = '19' + date_split[2] + '-12-' + date_split[0]

        dates.append(formatted)

    return dates 

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
    # convert the dates to a pandas representation
    muc_data['date'] = muc_date(muc_data)
    # DEV-MUC3-0246 has a wrongly encoded date
    muc_data.at[muc_data.index[muc_data['muc_id'] == "DEV-MUC3-0246"], 'date'] = '1989-06-11'
    muc_data['date'] = pd.to_datetime(muc_data['date'])
    # save as csv to processed data folder
    muc_data.to_csv(OUT_PATH)

if __name__ == "__main__":
    main()