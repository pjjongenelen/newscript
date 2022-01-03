"""
Approximate the domain template topics.
"""

import helpers
import numpy as np
from os.path import exists
import pandas as pd


ROOT = "C:\\Users\\timjo\\PycharmProjects\\newscript"


def load_muc(amount=0) -> pd.DataFrame:
    """
    Returns articles from the preprocessed MUC dataset
    """    

    # load pickle with annotations
    df = pd.read_pickle(ROOT + "\\processed_data\\muc_annotation.pkl")

    # sample (or not) based on the value of amount
    if 0 < amount < df.shape[0]:
        df = df.sample(n=amount, replace=False, axis=0)
  
    return df


def main(): 
    # load all MUC data
    muc_data = load_muc()

    # get the cdist matrix
    if exists(ROOT + "/processed_data/muc_cdist_matrix.npy"):
        muc_cdist_matrix = np.load(ROOT + "/processed_data/muc_cdist_matrix.npy")
    else:
        events_set = helpers.get_events_set(muc_data)
        muc_cdist_matrix = helpers.fill_cdist_matrix(muc_data, np.zeros([len(events_set), len(events_set)], dtype=np.int8), events_set)
        np.save(ROOT + "/processed_data/muc_cdist_matrix.npy", muc_cdist_matrix)


if __name__ == "__main__":
    main()
