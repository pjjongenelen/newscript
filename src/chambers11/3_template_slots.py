"""
Induces template slots from clusters of documents
"""

import helpers as h
import numpy as np
import os
import pandas as pd
from stanza.server import CoreNLPClient

ROOT = 'C:\\Users\\timjo\\PycharmProjects\\newscript'


def annotate_corenlp(documents: list) -> list:
    """Annotates documents with the CoreNLPClient
    
    :param documents: list of strings representing articles
    :returns: list of CoreNLPClient annotations
    """
    
    # annotate
    with CoreNLPClient(endpoint='http://localhost:8000', timeout=30000, memory='4G', be_quiet=True) as client:
        annotations = [client.annotate(doc) for doc in documents]
    
    # remove pesky .props files
    files = os.listdir(os.getcwd())
    for item in files:
        if item.endswith(".props"):
            os.remove(os.path.join(os.getcwd(), item))    

    return annotations


class event_pattern:
    """Contains all data of event patterns necessary to allow template slot induction"""

    def __init__(self, ev_type: str, lemma: str, loc: list):
        # general information
        self.ev_type = ev_type  # verb or noun
        self.lemma = lemma
        self.loc = [loc]  # [doc.id, sent.id, token.id]
        
        # for coreference vectors
        self.sub_loc = []  # [doc.id, sent.id, token.id]
        self.obj_loc = []  # [doc.id, sent.id, token.id]

        # for Selection Preferences (SP) vectors
        self.sub_lemmas = []
        self.obj_lemmas = []


def extract_event_patterns():
    """Use CoreNLP annotation to extract event patterns

    Chambers & Jurafsky (pp. 978) define event patterns as either:
    1a) a verb ("explode")
    1b) a verb and the head word of its syntactic object ("explode:bomb")
    2) a noun in WordNet under the Event synset ("explosion")
    
    Parameters: (?)
    annotations (list): CoreNLPClient annotations of documents

    Returns:
    event_patterns (list): event_pattern class instances
    """

    # 1) find verbs and their arguments


    # 2) find nouns that are in the WordNet Event synsets
    event_nouns = h.get_event_nouns()


def add_coref(arg_mat: np.ndarray, coref_chains: list) -> np.ndarray:
    """Sets columns in the argument matrix equal to their sum for all entities per coreference chain"""

    # TODO: implement

    return "Not implemented yet, so you got some work to do!"

def fill_coreference_matrix(coref_mat: np.ndarray, arg_mat: np.ndarray) -> np.ndarray:
    """The coreference matrix should be filled with (...)"""

    # TODO: implement

    return "Not implemented yet, so you got some work to do!"


class template:
    """This class should hold information on each of the templates. Most notably their slots.
    Haven't thought a lot about this yet, as it is far in the future, but it seems like templates should be a class"""

    def __init__(self):
        # TODO: implement
        pass


def cluster(coref_mat: np.ndarray):
    """Is a custom agglomerative clustering algorithm that takes into account the constraints the authors mention"""

    # TODO: implement

    return "Not implemented yet, so you got some work to do!"


def main():
    # ! BEFORE FURTHER IMPLEMENTATION:
    # ! 1) Please consider whether the current approach is strong enough to contain count information.
    # ! 2) Check if we have enough information in the event_pattern class to eventually implement the clustering with its constraints.

    # load sample documents
    dev_muc = pd.read_pickle(ROOT + '/src/chambers11/dev_dummy.pkl')  # ! this is just a dummy data file of 10 MUC documents

    # annotate documents with CoreNLPClient
    dev_muc['annotations'] = annotate_corenlp(dev_muc['text'])

    # use annotations to extract event patterns
    # TODO: implement
    event_patterns = extract_event_patterns(dev_muc['annotations'])

    # for each document, transform the coreference chain into a more usable format
    # TODO: implement
    corefering_entities = h.get_corefering_entities(dev_muc['annotations'])

    # get list of all subjects and objects
    # TODO: implement
    entities = h.get_sub_obj(event_patterns)

    # make the matrix that shows argument relations between event patterns and entities
    # TODO: implement
    argument_matrix = np.zeros([len(event_patterns)*2, len(entities)])

    # use the information on corefering entities to update the argument matrix
    # TODO: implement
    # ? I'm not sure if this improves the model, but since it makes the hyper-sparse matrix less so, let's start with using it
    argument_matrix = add_coref(argument_matrix, corefering_entities)

    # use the argument matrix to create a event pattern coreference matrix
    # TODO: implement
    coreference_matrix = np.zeros([len(event_patterns)*2, len(event_patterns)*2])
    coreference_matrix = fill_coreference_matrix(coreference_matrix, argument_matrix) 

    # cluster the coreference vectors, which are the rows of the coreference matrix 
    # TODO: implement
    # ! requires manual creation of clustering algorithm, due to the authors' two constraints
    templates = cluster(coreference_matrix)


if __name__ == "__main__":
    main()