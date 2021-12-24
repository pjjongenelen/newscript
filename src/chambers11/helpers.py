"""
Simple code snippets that make the 1_, 2_, and 3_ files more readable
"""

import en_core_web_sm
from nltk.corpus import wordnet
from random import sample
import stanza

stanza.download(lang="en", processors="tokenize,pos,lemma,depparse", logging_level="WARN")
PIPE = stanza.Pipeline(lang="en", processors="tokenize,mwt,pos,lemma,depparse", verbose=False)


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


class Article:
    """Class for articles that contains the most important information for clustering"""

    def __init__(self, text):
        self.text = text
        self.annotation = PIPE(self.text)
        self.event_patterns = list(set(get_noun_patterns(self.annotation) + get_verb_patterns(self.annotation)))

    def get_text(self):
        return self.text

    def get_event_patterns(self):
        return self.event_patterns


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
