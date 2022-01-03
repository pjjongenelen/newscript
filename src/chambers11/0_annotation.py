"""
Annotates all MUC articles with a Stanza pipeline, also extracts event pattersn.
The resulting dataframe is saved to a pickle file.
"""

import helpers
import pandas as pd
import stanza
from tqdm import tqdm

stanza.download(lang="en", processors="tokenize,pos,lemma,depparse", logging_level="WARN")
PIPE = stanza.Pipeline(lang="en", processors="tokenize,mwt,pos,lemma,depparse", verbose=False)
ROOT = 'C:\\Users\\timjo\\PycharmProjects\\newscript'


def load_muc() -> pd.DataFrame:
    """Loads MUC csv file and returns as dataframe"""
    df = pd.read_csv(ROOT + '\\processed_data\\muc.csv', index_col=0)
    return df

def annotate(df) -> pd.DataFrame:
    """Annotates each article with the Stanza pipeline."""
    # extract texts
    articles = df['text']

    # for loop so we can use tqdm
    annotations = []
    for index in tqdm(range(len(articles)), desc="Annotation of MUC articles"):
        text = articles[index]
        annotations.append(PIPE(text))

    # add annotations to df, and return
    df['annotation'] = annotations
    return df

def get_noun_patterns(annotation):
    """
    Extract event nouns based on the WorNet synsets
    """

    # get a list of the WordNet event nouns
    ev_nouns = helpers.define_event_nouns()

    matches = []
    for sent in annotation.sentences:
        for word in sent.words:
            if word.text in ev_nouns:
                matches.append([word.text.lower(), sent.id])

    return matches

def get_verb_patterns(annotation):
    """
    Extract verbs and their syntactic object head words
    """

    matches = []

    # get dict to transform object words into NER tags
    ner_dict = helpers.get_ner_dict(annotation)

    # extract verbs and their object head words
    for sent in annotation.sentences:
        for word1 in sent.words:
            if word1.upos == 'VERB':
                obj = None
                for word2 in sent.words:
                    if 'obj' in word2.deprel and word2.head == word1.id:
                        obj = helpers.get_ner(word2, ner_dict)
                if obj:
                    matches.append([word1.text + ":" + obj, sent.id])
                else:
                    matches.append([word1.text, sent.id])

    # convert to lowercase
    matches = [[m1.lower(), m2] for [m1, m2] in matches]    

    return matches


def get_event_patterns(annotation):
    """
    Extracts the two possible event pattern representations:
    1. event nouns based on the WordNet synsets
    2. verbs and the head words of their syntactic objects (with NER tags if available)
    """

    # 1. event nouns
    ev_patterns = get_noun_patterns(annotation)

    # 2. verbs (+ head words / NER tags)
    ev_patterns += get_verb_patterns(annotation)

    return ev_patterns


def main():
    # load all MUC articles (1700)
    muc = load_muc()

    # annotate with the stanza pipeline
    muc = annotate(muc)    

    # extract event patterns
    tqdm.pandas(desc='Extracting event patterns')
    muc['event_patterns'] = muc.progress_apply(lambda row: get_event_patterns(row['annotation']), axis=1)
    
    # save to pickle (JSON will throw an overflow error)
    muc.to_pickle(ROOT + "\\processed_data\\muc_annotation.pkl")

if __name__ == "__main__":
    main()