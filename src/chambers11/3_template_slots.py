"""
Induces template slots from clusters of documents

Backlog:
1) ---
Extract event nouns for event patterns

2) ---
Take coreference chains into account. Introduces a transitive property in the subject/object relation between event patterns and entities.
This means updating the argument matrix such that all columns for corefering entities are equal to the sum of their parts.
Requires switching to the (less interpreatble) location representation of subjects and objects instead of their lemmas.
"""

import helpers as h
from itertools import permutations
import numpy as np
import os
import pandas as pd
from sklearn.cluster import AgglomerativeClustering
from stanza.server import CoreNLPClient
from tqdm import tqdm


N_SLOTS = 5
ROOT = 'C:\\Users\\timjo\\PycharmProjects\\newscript'
SOURCE = 'gnm'


def annotate_corenlp(documents: list) -> list:
    """Annotates documents with the CoreNLPClient
    
    :param documents: list of strings representing articles
    :returns: list of CoreNLPClient annotations
    """
    
    # annotate
    with CoreNLPClient(endpoint='http://localhost:8000', timeout=100000000, memory='4G', be_quiet=True) as client:
        annotations = []
        print('Annotating documents')
        for x in tqdm(range(len(documents))):
            doc = documents[x]
            annotations.append(client.annotate(doc))
    
    # remove pesky .props files
    files = os.listdir(os.getcwd())
    for item in files:
        if item.endswith(".props"):
            os.remove(os.path.join(os.getcwd(), item))    

    return annotations


class event_pattern:
    """Contains all data of event patterns necessary to allow template slot induction"""

    def __init__(self, lemma: str):
        self.lemma = lemma
        
        # for coreference resoluation
        self.sub_locs = []  # [doc.id, sent.id, token.id]
        self.obj_locs = []  # [doc.id, sent.id, token.id]

        # for selection preferences
        self.sub_lemmas = []
        self.obj_lemmas = []

    def add_sub(self, loc, lemma):
        self.sub_locs.append(loc)
        self.sub_lemmas.append(lemma)

    def add_obj(self, loc, lemma):
        self.obj_locs.append(loc)
        self.obj_lemmas.append(lemma)


def extract_event_patterns(annotations: list) -> dict:
    """Use CoreNLP annotation to extract event patterns

    Chambers & Jurafsky (pp. 978) define event patterns as either:
    1a) a verb ("explode")
    1b) a verb and the head word of its syntactic object ("explode:bomb")
    2) a noun in WordNet under the Event synset ("explosion")
    
    :param annotations: list of CoreNLPClient annotations of documents

    :returns: dictionary of event_pattern class instances
    """

    # 1) find verbs and their arguments
    event_patterns = {}
    print('Extracting event patterns')
    for ann_index in tqdm(range(len(annotations))):
        ann = annotations[ann_index]
        doc_id = annotations.index(ann)

        for sent in ann.sentence:
            sent_id = sent.basicDependencies.node[0].sentenceIndex

            for token in sent.token:  # for each word
                if 'VB' in token.pos:
                    if token.lemma not in event_patterns:
                        # create new event pattern
                        event_patterns[token.lemma] = event_pattern(lemma=token.lemma)

                    # search for subjects and objects
                    for e in sent.basicDependencies.edge:
                        if ('sub' in e.dep or 'obj' in e.dep) and e.source == token.tokenEndIndex:

                            # get lemma/NER tag
                            sub_obj_token = sent.token[e.target - 1]
                            if sub_obj_token.coarseNER == "O":
                                lemma = sub_obj_token.lemma
                            else:
                                lemma = sub_obj_token.coarseNER
                            lemma = lemma.lower()

                            # get location info
                            loc = [doc_id, sent_id, e.target - 1]  # e.target index starts at 1

                            # add to subject or object to event pattern
                            if 'sub' in e.dep:
                                event_patterns[token.lemma].add_sub(loc, lemma)
                            else:
                                event_patterns[token.lemma].add_obj(loc, lemma)



    # 2) find nouns that are in the WordNet Event synsets
    # TODO: implement
    # event_nouns = h.get_event_nouns()

    return event_patterns


def create_arg_matrix(ev_patterns: dict, ents: list, evps: list) -> np.ndarray:
    """Creates the argument matrix based on the information in ev_patterns
    
    :param ev_patterns: dictionary of event pattern objects
    :param ents: list of entities that serves as index for the columns
    :param evps: list of event patterns that serves as index for the rows

    :returns: filled argument matrix
    """

    # create empty argument matrix
    arg_matrix = np.zeros([len(ev_patterns)*2, len(ents)])

    # extract all subjects and objects of all event patterns, and annotate the argument matrix accordingly
    print('Extracting subjects and objects for event patterns')
    for ep_sub_index in tqdm(range(len(evps))):
        pattern = ev_patterns[evps[ep_sub_index]]
        ep_obj_index = ep_sub_index + len(evps)

        subjects = pattern.sub_lemmas
        objects = pattern.obj_lemmas
        
        if len(subjects) > 0:
            for sub in subjects:
                col_index = ents.index(sub)
                arg_matrix[ep_sub_index, col_index] = 1

        if len(objects) > 0:
            for obj in objects:
                col_index = ents.index(obj)
                arg_matrix[ep_obj_index, col_index] = 1

    return arg_matrix


def create_coreference_matrix(arg_mat: np.ndarray, evps: list) -> np.ndarray:
    """Creates a matrix containing information relating to corefering arguments (C&J, pp. 980)

    Each row in the matrix will contain an event pattern as subject and/or object.
    Information is one-hot encoded to show with which other event patterns it corefers in this cluster of documents
    Note that we use one-hot encoding instead of count-based information. This is the way we interpreted "relation counts" on page 980.

    :param arg_mat: filled argument matrix
    :param evps: event pattern index list 

    :returns: filled coreference matrix
    """

    # create empty coreference matrix
    coref_mat = np.zeros([len(evps), len(evps)])

    # iterate over all entities
    print('Creating coreference matrix from selectional preference matrix, column-by-column')
    for col in tqdm(range(arg_mat.shape[1])):
        # find all ev_patterns that corefer with this entity
        col_evps = list(np.argwhere(arg_mat[:,col] > 0))
        
        # if there are corefering event patterns, update the coref_matrix
        if len(col_evps) > 1:
            for i1, i2 in permutations(col_evps, r=2):
                coref_mat[i1, i2] = 1

    return coref_mat


def create_similarity_matrix(coref_mat: np.ndarray, selpref_mat: np.ndarray, evp_i: list):
    """Calculates the cosine similarity scores for each pair of event patterns, based on the rule on page 980:
    
    similarity = {  max(cos_sim(coref), cos_sim(selpref))  --  if max(...) >= 0.7    }
                 {  avg(cos_sim(coref), cos_sim(selpref))  --  if max(...) <  0.7    }
                 {  cos_sim(coref) OR cos_sim(selpref)     --  if one doesn't exist  }*  
    
    *because we removed empty lines from the matrices for convenience, there might be differences in the coref and selpref lists of event patterns

    :param coref_mat: one-hot encoded coreference matrix (which evps have corefering arguments with which other evps) 
    :param selpref_mat: one-hot encoded selectional preferences matrix (which evps refer to which entities)
    :param evp_i: event pattern index list for indexing the matrices
    
    :returns: single similarity matrix that contains the measure of similarity between event patterns as defined above

    Good to know: 
    - coref_mat.shape[0] == coref_mat.shape[1]  == selpref_mat.shape[0]
    - selpref_mat.shape[1] == number of entities (ergo: len(set) of all subjects and objects of extracted event patterns)
    """

    # remove rows from both matrices if we have no information
    to_remove = [x for x in range(len(evp_i)) if sum(coref_mat[x,:]) == 0 and sum(selpref_mat[x,:]) == 0]
    coref_mat = np.delete(coref_mat, to_remove, 0)
    coref_mat = np.delete(coref_mat, to_remove, 1)
    selpref_mat = np.delete(selpref_mat, to_remove, 0)
    to_remove.sort(reverse = True)
    for tr in to_remove:
        # start from the back so that the removed elements do not mess up the indices
        del evp_i[tr]


    # create empty similarity matrix
    sim_matrix = np.zeros(coref_mat.shape)

    print('Creating similarity matrix')
    for i1 in tqdm(range(coref_mat.shape[0])):
        for i2 in range(coref_mat.shape[0]):
            if evp_i[i1][0:len(evp_i[i1]) - 2] == evp_i[i2][0:len(evp_i[i2]) - 2]:
                # clustering constraint 1: prevent sub and obj of same ep from being in the same cluster
                sim_matrix[i1, i2] = -10
            elif i1 != i2:
                # cos_sim calculates cosine similarity if both rows have data, else it returns None
                coref_sim = h.cos_sim(coref_mat[i1,:], coref_mat[i2,:])
                selpref_sim = h.cos_sim(selpref_mat[i1,:], selpref_mat[i2,:])
                
                # determine case based on whether or not we found empty rows
                if coref_sim and selpref_sim:
                    # no empty rows, normal case
                    max_sim = max(coref_sim, selpref_sim)
                    if max_sim < 0.7:
                        # if we're confident, take the maximum of the two
                        sim_matrix[i1, i2] = max_sim
                    else:
                        # else, take the averagee
                        sim_matrix[i1, i2] = (coref_sim + selpref_sim) / 2
                elif coref_sim:
                    # if only coref information
                    sim_matrix[i1, i2] = coref_sim
                elif selpref_sim:
                    # if only selpref information
                    sim_matrix[i1, i2] = selpref_sim

    return sim_matrix


def main():
    # load documents
    df = pd.read_pickle(f'{ROOT}/processed_data/{SOURCE}_selected_cluster.pkl')

    # annotate documents with CoreNLPClient
    df['annotations'] = annotate_corenlp(df['text'])

    # use annotations to extract event patterns
    event_patterns = extract_event_patterns(df['annotations'].tolist())

    # get list of all event patterns, subjects, and objects to serve as the indices of the matrices
    evp_index = sorted(list(event_patterns.keys()))
    ent_index = h.get_sub_obj(event_patterns)

    # make the matrix that shows argument relations between event patterns and entities
    # ? for a visual example of the argument matrix see res/argument_matrix.md
    selpref_matrix = create_arg_matrix(event_patterns, ent_index, evp_index)
    
    # update event pattern index list (each event pattern is considered as both subject and object)
    evp_index = [ep + ':s' for ep in evp_index] + [ep + ':o' for ep in evp_index]

    # use the argument matrix to create an event pattern coreference matrix
    coreference_matrix = create_coreference_matrix(selpref_matrix, evp_index) 

    # similarity matrix
    similarity_matrix = create_similarity_matrix(coreference_matrix, selpref_matrix, evp_index)
    np.save(f"{ROOT}/src/chambers11/matrices/{SOURCE}_similarity.npy", similarity_matrix)

    # cluster the similarity matrix
    clustering = AgglomerativeClustering(n_clusters = N_SLOTS, affinity = 'precomputed', linkage = 'average').fit(similarity_matrix)
    slot_mapping = pd.DataFrame({'slot': clustering.labels_, 'event': evp_index})
    pd.to_pickle(slot_mapping, f'{ROOT}/src/chambers11/matrices/{SOURCE}_slot_mapping.pkl')


if __name__ == "__main__":
    main()