"""
Simple code snippets that make the 1_, 2_, and 3_ files more readable
"""

import en_core_web_sm
from nltk.corpus import wordnet
from random import sample
from time import perf_counter


def get_noun_patterns(annotation):
    """Extract event nouns based on the WorNet synsets"""

    # define the event nouns
    ev_nouns = define_event_nouns()

    matches = []
    for sent in annotation.sentences:
        for word in sent.words:
            if word.text in ev_nouns:
                matches.append(word.text.lower() + ":N")

    return matches


def get_verb_patterns(annotation):
    """Extract verbs and their syntactic object head words"""

    # create a dict to transform objects to NER labels if applicable
    ner_dict = get_ner_dict(annotation)

    # TODO: simplify this nested structure
    # extract verbs and their object head words
    matches = []
    for sent in annotation.sentences:
        for word in sent.words:
            if word.upos == 'VERB':
                # check if there is a corresponding object
                obj = ""
                for word2 in sent.words:
                    if 'obj' in word2.deprel and word2.head == word.id:
                        if word2.text.lower() in ner_dict.keys():
                            # get the NER label for the object
                            obj = ner_dict[word2.text.lower()]
                        else:
                            # get the regular object text
                            obj = word2.text.upper()
                if obj != "":
                    matches.append(word.text.lower() + ":" + obj.upper())
                else:
                    matches.append(word.text.lower())

    return matches


def get_ner_dict(annotation):
    nlp = en_core_web_sm.load()
    ner_list = []
    for sent in annotation.sentences:
        doc = nlp(sent.text)
        ner_list += [(X.text.lower(), X.label_) for X in doc.ents]

    return dict(ner_list)


def conditional_sample(articles, amount):
    """Sample articles, or return all available"""

    if 0 < amount < len(articles):
        return sample(articles, amount)
    else:
        return articles


def define_event_nouns():
    """Define a list of event nouns"""

    n01 = wordnet.synset("event.n.01").closure(lambda s: s.hyponyms())
    n02 = wordnet.synset("act.n.02").closure(lambda s: s.hyponyms())

    event_nouns = [w for s in n01 or n02 for w in s.lemma_names()]

    return list(set(event_nouns))

class TimerError(Exception):
    """A custom exception"""
    
class Timer:
    """Timer class to measure code running times"""
    def __init__(self) -> None:
        self._start_time = None
    
    def start(self):
        "Start a new timer"
        if self._start_time is not None:
            raise TimerError("Timer is running. Use .stop() to stop it.")
        
        self._start_time = perf_counter()

    def stop(self):
        """Stop the timer and report the elapsed time"""
        if self._start_time is None:
            raise TimerError("No timer is running. Use .start() to start one.")

        elapsed_time = perf_counter() - self._start_time
        self._start_time = None
        print(f"Elapsed time: {elapsed_time:0.4f} seconds")

    def stop_go(self):
        """Stop the timer, report the elapsed time, and immediately start a new one"""
        if self._start_time is None:
            raise TimerError("No timer is running. Use .start() to start one.")

        elapsed_time = perf_counter() - self._start_time
        print(f"Elapsed time: {elapsed_time:0.4f} seconds")
        self._start_time = perf_counter()
        