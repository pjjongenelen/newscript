"""
Simple code snippets that make the 1_, 2_, and 3_ files more readable
"""

import numpy as np
import en_core_web_sm
from nltk.corpus import wordnet
from tqdm import tqdm


def get_ner_dict(annotation):
    """
    Creates a dictionary with nouns and their corresponding NER tags according to Spacy's en_core_web_sm model
    """
    nlp = en_core_web_sm.load()
    ner_list = []
    for sent in annotation.sentences:
        doc = nlp(sent.text)
        ner_list += [(X.text.lower(), X.label_) for X in doc.ents]

    return dict(ner_list)


def get_ner(word, dict):
    """
    Try to replace the word with its appropriate NER tag
    """
    word = word.text.lower()

    if word in dict.keys():
        # replace word with NER tag
        return dict[word]
    else:
        # no NER tag available
        return word


def define_event_nouns():
    """
    Defines a list of event nouns based on the two WordNet synsets used by Chambers & Jurafsky
    """

    n01 = wordnet.synset("event.n.01").closure(lambda s: s.hyponyms())
    n02 = wordnet.synset("act.n.02").closure(lambda s: s.hyponyms())

    event_nouns = [w for s in n01 or n02 for w in s.lemma_names()]

    return list(set(event_nouns))


def get_events_set(df):
    """
    Create sorted list of the set of events in order to look up indices in matrices later on
    """
    # get all events
    event_patterns = df['event_patterns'].tolist()
    events = [event[0] for ep in event_patterns for event in ep]
    # remove duplicates and sort
    ev_set = sorted(set(events))

    return ev_set


def get_p_dict(df):
    """
    Create a dict of event counts
    """

    # get all events
    event_patterns = df['event_patterns'].tolist()
    events = [event[0] for ep in event_patterns for event in ep]

    # create dataframe with event counts
    event_counts = events.value_counts().rename_axis('event').reset_index(name='count')

    # now calculate for each event its count / the sum of the count of all others
    event_counts['countfrac'] = event_counts['countfrac'] / (event_counts['count'].sum() - event_counts['countfrac'])

    return dict(zip(event_counts['event'], event_counts['countfrac']))


def make_pdist_matrix(cdist_matrix):
    """
    Calculates pdist values given the cdist matrix based on the formula on page 979
    pdist(wi, wj) = cdist(wi, wj) / ( sum_all_cdist(wk, wl) )
    
    The denominator can be calculated by:
    sum(cdist_matrix) - ( sum(row_i) + sum(col_j) - cdist(wi, wj) )
    """

    # create empty pdist matrix
    pdist_matrix = np.zeros(cdist_matrix.shape)

    # calculate sum(cdist_matrix)
    cdist_sum = sum(cdist_matrix.sum(axis=1).tolist())
    
    # get the column sums for reduced running time in the nested loop
    sum_col_j = []
    for j in range(cdist_matrix.shape[1]):
        sum_col_j.append(cdist_matrix[:,j].sum())

    # fill the pdist matrix
    for i in tqdm(range(cdist_matrix.shape[0])):
        sum_row_i = cdist_matrix[i,].sum()
        for j in range(cdist_matrix.shape[1]):
            cdist = cdist_matrix[i,j]
            pdist_matrix[i,j] = cdist / (cdist_sum - (sum_row_i + sum_col_j[j] - cdist))

    return pdist_matrix