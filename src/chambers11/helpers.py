"""
Simple code snippets that make the 1_, 2_, and 3_ files more readable
"""


import en_core_web_sm
from itertools import combinations
import math
from nltk.corpus import wordnet
from tqdm import tqdm


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
    Create sorted list of the set of events in order to look up indices in the cdist matrix later on
    """
    # get all events
    event_patterns = df['event_patterns'].tolist()
    events = [event[0] for ep in event_patterns for event in ep]
    # remove duplicates and sort
    ev_set = sorted(set(events))

    return ev_set

def fill_cdist_matrix(df, main_matrix, ev_set):
    """
    Follows the cdist formula from page 979 of the article: 
    
    cdist(wi, wi) = 1 - ( log_4 of g(wi, wj) ) 
    Summed over all event pairs (wi, wj) in all documents (d):

    Note that the variable names might seem undescriptive, but they're chosen to be equal to those from the formula 
    """
    
    for _, d in tqdm(df.iterrows(), total=df.shape[0]):  # for all documents d
        for wi, wj in list(combinations(d['event_patterns'], 2)):  # for all pairs of events wi, wj
            # calculate the single cdist value
            g = abs(wi[1] - wj[1]) + 1
            c_dist = max((1 - math.log(g, 4)), 0)

            # add this number to the main matrix
            wi_index = ev_set.index(wi[0])
            wj_index = ev_set.index(wj[0])
            main_matrix[wi_index, wj_index] += c_dist

    return main_matrix
        