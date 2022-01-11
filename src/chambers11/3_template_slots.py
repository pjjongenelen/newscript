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


def extract_event_patterns(annotations):
    """Use CoreNLP annotation to extract event patterns

    Chambers & Jurafsky (pp. 978) define event patterns as either:
    1a) a verb ("explode")
    1b) a verb and the head word of its syntactic object ("explode:bomb")
    2) a noun in WordNet under the Event synset ("explosion")
    
    :param annotations: list of CoreNLPClient annotations of documents

    :returns: list of event_pattern class instances
    """

    # 1) find verbs and their arguments
    event_patterns = {}
    for ann in annotations:
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


def fill_arg_matrix(arg_mat: np.ndarray, ev_patterns: dict, ents: list) -> np.ndarray:
    """Loops over all event pattern objects, and annotates """
    pass


def add_coref(arg_mat: np.ndarray, coref_chains: list) -> np.ndarray:
    """Sets columns in the argument matrix equal to their sum for all entities per coreference chain"""

    # Backlog, because coref information comes from the CoreNLPClient, 
    # whereas our annotations (with deprel) comes from the neural pipeline

    return "Not implemented yet."

def fill_coreference_matrix(coref_mat: np.ndarray, arg_mat: np.ndarray) -> np.ndarray:
    """Creates a matrix containing information relating to corefering arguments (C&J, pp. 980)

    Each row in the matrix will contain an event pattern as subject and/or object.
    Information is one-hot encoded to show with which other event patterns it corefers in this cluster of documents
    Note that we use one-hot encoding instead of count-based information. This is the way we interpreted "relation counts" on page 980.

    :param coref_mat: empty coreference matrix
    :param arg_mat: 

    :returns: filled coreference matrix
    """

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
    # load sample documents
    dev_muc = pd.read_pickle(ROOT + '/src/chambers11/matrices/dev_dummy.pkl')  # ! this is just a dummy data file of 10 MUC documents

    # annotate documents with CoreNLPClient
    dev_muc['annotations'] = annotate_corenlp(dev_muc['text'])

    # use annotations to extract event patterns
    event_patterns = extract_event_patterns(dev_muc['annotations'].tolist())

    # for each document, transform the coreference chain into a more usable format
    # TODO: implement
    # corefering_entities = h.get_corefering_entities(dev_muc['annotations'])

    # get list of all subjects and objects to serve as the column indices of the argument matrix
    entities = h.get_sub_obj(event_patterns)
    print(entities)

    # make the matrix that shows argument relations between event patterns and entities
    # TODO: implement
    argument_matrix = np.zeros([len(event_patterns)*2, len(entities)])
    argument_matrix = fill_arg_matrix(argument_matrix, event_patterns, entities)

    # use the information on corefering entities to update the argument matrix
    # TODO: implement
    # ? I'm not sure if this improves the model
    # argument_matrix = add_coref(argument_matrix)#, corefering_entities)

    # use the argument matrix to create a event pattern coreference matrix
    # TODO: implement
    # coreference_matrix = np.zeros([len(event_patterns)*2, len(event_patterns)*2])
    # coreference_matrix = fill_coreference_matrix(coreference_matrix, argument_matrix) 

    # cluster the coreference vectors, which are the rows of the coreference matrix 
    # TODO: implement
    # ! requires manual creation of clustering algorithm, due to the authors' two constraints
    # templates = cluster(coreference_matrix)


if __name__ == "__main__":
    main()