"""
Approximate the domain template topics by clustering events patterns based on distance.
"""

import helpers as h
from itertools import permutations
import json
import math
import numpy as np
from os.path import exists
import pandas as pd
from sklearn.cluster import AgglomerativeClustering
from tqdm import tqdm


# USER CONFIG:
ROOT = "C:\\Users\\timjo\\PycharmProjects\\newscript"
SOURCE = "gnm"
N_CLUS = 15  # number of event pattern clusters


def make_cdist_matrix(df, ev_set):
    """
    Follows the cdist formula from page 979: 
    
    cdist(wi, wi) = 1 - ( log_4 of g(wi, wj) ) 
    Summed over all event pairs (wi, wj) in all documents (d):

    Variable names correspond to formula
    """

    cdist_loc = f"{ROOT}/src/chambers11/matrices/{SOURCE}_cdist_matrix.npy"

    if exists(cdist_loc):
        # load and return
        cdist_matrix = np.load(cdist_loc)
        print("Succesfully loaded cdist matrix")
    else:
        print(f"Failed to find cdist matrix at {cdist_loc}, creating new...")        
        cdist_matrix = np.zeros([len(ev_set), len(ev_set)], dtype=np.float16)
        
        for _, d in tqdm(df.iterrows(), total=df.shape[0]):  # for all documents d
            for wi, wj in list(permutations(d['event_patterns'], r=2)):  # for all pairs of events wi, wj
                # calculate the single cdist value
                g = abs(wi[1] - wj[1]) + 1
                c_dist = max((1 - math.log(g, 4)), 0)

                # add this number to the main matrix
                wi_index = ev_set.index(wi[0])
                wj_index = ev_set.index(wj[0])
                cdist_matrix[wi_index, wj_index] += c_dist

        # save for fast access later
        np.save(cdist_loc, cdist_matrix)

    return cdist_matrix


def make_pmi_matrix(p, pdist, ev_set):
    """
    Calculates the pmi values from the formula on page 979:"

    pmi(wi, wj) = pdist(wi, wj) / (p(wi)*p(wj))

    Variable names correspond to formula
    """

    pmi_loc = f"{ROOT}/src/chambers11/matrices/{SOURCE}_pmi_matrix.npy"

    if exists(pmi_loc):
        # load and return
        pmi_matrix = np.load(pmi_loc)
        print("Succesfully loaded pmi matrix")
    else:
        print(f"Failed to find pmi matrix at {pmi_loc}, creating new...")
        # create empty matrix
        pmi_matrix = np.zeros([len(ev_set), len(ev_set)], dtype=np.float16)

        # loop over all elements and fill
        for i in tqdm(range(pdist.shape[0])):
            for j in range(pdist.shape[1]):
                pmi_matrix[i,j] = pdist[i,j] / (p[ev_set[i]] * p[ev_set[j]])

        # save for fast access later
        np.save(pmi_loc, pmi_matrix)

    return pmi_matrix


def main(): 
    # 1) load data-----
    df = pd.read_pickle(f"{ROOT}\\processed_data\\{SOURCE}_annotation.pkl")

    # 2) get the sorted set of events in the data, necessary to look up indices in the cdist and pmi matrices-----
    event_set, event_counts = h.make_events_set(df)

    # 3) get the cdist matrix-----
    cdist_matrix = make_cdist_matrix(df, event_set)

    # 4) get the pmi matrix-----
    pmi_matrix = make_pmi_matrix(h.prob(df), h.pdist(cdist_matrix, SOURCE), event_set)
    
    # 5) agglomerative clustering-----
    clustering = AgglomerativeClustering(n_clusters = N_CLUS, affinity = 'precomputed', linkage = 'average').fit(pmi_matrix)
    print(f'number of clusters = {N_CLUS}, size of largest cluster: {max(np.bincount(clustering.labels_))}')

    # 6) print to allow manual identification of clusters-----
    ep_cluster_mapping = pd.DataFrame({'cluster': clustering.labels_, 'event': event_set})
    h.save_to_dict(ep_cluster_mapping['event'], ep_cluster_mapping['cluster'], loc = f'{ROOT}/src/chambers11/matrices/{SOURCE}_event_cluster_dict.json')
    h.print_clusters(ep_cluster_mapping, event_counts)
    
    # 7) add cluster info to the dataset-----
    with open(f"{ROOT}\\src\\chambers11\\matrices\\{SOURCE}_event_cluster_dict.json") as f:
        evp_dict = json.load(f)

    clusters = []
    for _, row in df.iterrows():
        subclusters = []
        for evp in row.event_patterns:
            subclusters.append(evp_dict[evp[0]])
        most_freq_clus = max(set(subclusters), key = subclusters.count)
        clusters.append(most_freq_clus)    

    df['cluster'] = clusters

    pd.to_pickle(df, f"{ROOT}\\processed_data\\{SOURCE}_ann_clus.pkl")


if __name__ == "__main__":
    main()