"""
Approximate the domain template topics.
"""

import helpers
from itertools import permutations
import math
import numpy as np
from os.path import exists
import pandas as pd
from tqdm import tqdm


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


def make_cdist_matrix(df, ev_set):
    """
    Follows the cdist formula from page 979 of the article: 
    
    cdist(wi, wi) = 1 - ( log_4 of g(wi, wj) ) 
    Summed over all event pairs (wi, wj) in all documents (d):

    Variable names correspond to formula
    """

    main_matrix = np.zeros([len(ev_set), len(ev_set)], dtype=np.float16)
    
    for _, d in tqdm(df.iterrows(), total=df.shape[0]):  # for all documents d
        for wi, wj in list(permutations(d['event_patterns'], r=2)):  # for all pairs of events wi, wj
            # calculate the single cdist value
            g = abs(wi[1] - wj[1]) + 1
            c_dist = max((1 - math.log(g, 4)), 0)

            # add this number to the main matrix
            wi_index = ev_set.index(wi[0])
            wj_index = ev_set.index(wj[0])
            main_matrix[wi_index, wj_index] += c_dist

    return main_matrix


def make_pmi_matrix(df, ev_set, cdist_matrix):
    """
    Creates a matrix where each cell stores the pmi as defined in the formula on page 979:
    pmi(wi, wj) = pdist(wi, wj) / (p(wi)*p(wj))

    Variable names correspond to formula
    """

    # get the p dictionary
    p = helpers.get_p_dict(df)

    # load or make new pdist matrix
    if exists(ROOT + "/processed_data/muc_pdist_matrix.npy"):
        pdist = np.load(ROOT + "/processed_data/muc_pdist_matrix.npy")
    else:
        pdist = helpers.make_pdist_matrix(cdist_matrix)
        np.save(ROOT + "/processed_data/muc_pdist_matrix.npy", pdist)

    # apply the formula on an empty matrix 
    pmi_matrix = np.zeros([len(ev_set), len(ev_set)], dtype=np.int8)

    return pmi_matrix


def clustering(pmi_matrix):
    return pmi_matrix


def main(): 
    # 1) load all MUC data
    muc_data = load_muc()

    # 2) get the sorted set of events in the data, necessary to look up indices in the cdist and pmi matrices
    events_set = helpers.get_events_set(muc_data)

    # 3) get the cdist matrix
    if exists(ROOT + "/processed_data/muc_cdist_matrix.npy"):
        muc_cdist_matrix = np.load(ROOT + "/processed_data/muc_cdist_matrix.npy")
    else:
        muc_cdist_matrix = make_cdist_matrix(muc_data, events_set)
        np.save(ROOT + "/processed_data/muc_cdist_matrix.npy", muc_cdist_matrix)

    # 4) get the pmi matrix
    # TODO: implement
    # muc_pmi_matrix = make_pmi_matrix(muc_data, events_set, muc_cdist_matrix)
    
    # 5) agglomerative clustering
    # TODO: implement
    # clusters = clustering(muc_pmi_matrix)
    # TODO: save to file system

    # 6) manual inspection, and selection of cluster numbers
    # TODO: implement

if __name__ == "__main__":
    main()
