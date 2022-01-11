"""
Simple code snippets that make the 1_, 2_, and 3_ files more readable
"""

import json
import numpy as np
from nltk.corpus import wordnet
import os
import pandas as pd
from tqdm import tqdm

# set up:
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


def get_freq_verbs(df: pd.DataFrame, threshold: float) -> list:
    """
    Builds a list of verbs that occur in more of the documents than a certain threshold
    """
    doc_verbs = []
    annotations = df['annotation']
    for ann in annotations:
        dv = []
        for sent in ann.sentences:
            for word in sent.words:
                if word.upos == 'VERB':
                    dv.append(word.lemma)
        doc_verbs.append(list(set(dv)))
    
    # flatten list and get value counts
    doc_verbs = pd.Series([verb for doc in doc_verbs for verb in doc]).value_counts()
    freq_verbs = doc_verbs[doc_verbs.values > (1700 * threshold)].index
    return freq_verbs


def get_event_nouns() -> list:
    """Defines event nouns based on WordNet event.n.01 and act.n.02

    :returns: event noun set as list
    """

    n01 = wordnet.synset("event.n.01").closure(lambda s: s.hyponyms())
    n02 = wordnet.synset("act.n.02").closure(lambda s: s.hyponyms())

    event_nouns = [w for s in n01 or n02 for w in s.lemma_names()]

    return list(set(event_nouns))


def make_events_set(df) -> list:
    """
    Create sorted list of the set of events in order to look up indices in matrices later on
    """
    # get all events
    events = [event[0] for ep in df['event_patterns'] for event in ep]

    # Extract set of events, and the count of their occurence. Both lists sorted on index
    ev_set = pd.Series(events).value_counts().sort_index(ascending = True).index.tolist()
    ev_counts = pd.Series(events).value_counts().sort_index(ascending = True).values.tolist()

    return ev_set, ev_counts


def prob(df) -> dict:
    """
    Create a dict of event count probabilities following the formula on page 979
    p(wi) = c(wi) / sum( c(wj) )
    in pseudocode:
    prob(wi) = count(wi) / count(w).sum() - count(wi)
    """

    # get all events
    event_patterns = df['event_patterns']
    events = pd.Series([event[0] for ep in event_patterns for event in ep])

    # create dataframe with event counts
    event_counts = events.value_counts().rename_axis('event').reset_index(name='count')

    # now calculate for each event its count / the sum of the count of all others
    event_counts['prob'] = event_counts['count'] / (event_counts['count'].sum() - event_counts['count'])

    return dict(zip(event_counts['event'], event_counts['prob']))


def pdist(cdist_matrix) -> np.ndarray:
    """
    Calculates pdist values given the cdist matrix based on the formula on page 979
    pdist(wi, wj) = cdist(wi, wj) / ( sum_all_cdist(wk, wl) )
    
    The denominator can be calculated by:
    sum(cdist_matrix) - ( sum(row_i) + sum(col_j) - cdist(wi, wj) )
    """

    pdist_loc = ROOT + "/src/chambers11/matrices/muc_pdist_matrix.npy"

    if os.path.exists(pdist_loc):
        # load and return
        pdist_matrix = np.load(pdist_loc)
        print("Succesfully loaded pdist matrix")
    else:
        print(f"Failed to find pdist matrix at {pdist_loc}, creating new...")
        # create empty matrix
        pdist_matrix = np.zeros(cdist_matrix.shape)

        # calculate some variables to speed up processing in the nested loop
        cdist_sum = sum(cdist_matrix.sum(axis=1).tolist())
        sum_col_j = [cdist_matrix[:,j].sum() for j in range(cdist_matrix.shape[1])]
        
        # fill the pdist matrix
        for i in tqdm(range(cdist_matrix.shape[0])):
            sum_row_i = cdist_matrix[i,].sum()
            for j in range(cdist_matrix.shape[1]):
                cdist = cdist_matrix[i,j]
                pdist_matrix[i,j] = cdist / (cdist_sum - (sum_row_i + sum_col_j[j] - cdist))

        # save for fast access later
        np.save(pdist_loc, pdist_matrix)

    return pdist_matrix


def print_clusters(df: pd.DataFrame, counts: list) -> None:
    """
    Prints the 10 most frequent (on a corpus level) event patterns for each cluster
    """

    df['count'] = counts
    df.sort_values(by='count', ignore_index=True, inplace=True)

    # for each cluster
    for c in range(len(set(df['cluster']))):
        cluster_events = df['event'][df['cluster'] == c]
        print(f"Cluster {c} has size {len(cluster_events)}.")
        # if the cluster is large enough, print the 10 most frequent event patterns
        amount = min(len(cluster_events), 10)        
        print(f"Contains: {'  -  '.join([ev for ev in cluster_events][:amount])}")
        print("______________________________________________________________________________")


def save_to_dict(col1, col2, loc: str) -> None:
    cluster_events_dict = dict()
    for e, c in zip(col1, col2):
        cluster_events_dict[e] = c
    
    with open(loc, 'w+') as f:
        json.dump(cluster_events_dict, f)


def get_sub_obj(event_patterns: dict) -> list:
    """Extracts all subjects and objects included in the event patterns
    This information is necessary to know how many columns the argument matrix should have,
    and it will also serve as an index of these columns
    
    :param event_patterns: dictionary of all event pattern objects from the corpus

    :returns: sorted list of all subject and object lemmas

    """

    subjects = [sub_lem.lower() for value in event_patterns.values() for sub_lem in value.sub_lemmas]
    objects = [obj_lem.lower() for value in event_patterns.values() for obj_lem in value.obj_lemmas]

    return sorted(list(set(subjects + objects)))


def cos_sim(a, b):
    """Calculates the cosine similarity between two vectors.
    Except when one of the vectors is all zeros, then it returns None"""

    if sum(a) == 0 or sum(b) == 0:
        return None
    else:
        return np.dot(a, b)/(np.linalg.norm(a)*np.linalg.norm(b))