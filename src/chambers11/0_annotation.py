"""
Annotates all MUC articles with a Stanza pipeline, also extracts event pattersn.
The resulting dataframe is saved to a pickle file.
"""

import en_core_web_sm
import helpers
from os.path import exists
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
    ev_nouns = helpers.get_event_nouns()

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
    nlp = en_core_web_sm.load()

    # extract verbs and their object head words (if they have one)
    for sent in annotation.sentences:
        for word1 in sent.words:
            # find all verbs
            if word1.upos == 'VERB':
                # look for a corresponding object
                obj = None
                for word2 in sent.words:
                    if 'obj' in word2.deprel and word2.head == word1.id:
                        # check for available NER tag
                        ann = nlp(word2.text)
                        obj = ann.ents[0].label_ if len(ann.ents) > 0 else word2.lemma
                        break

                # put the event pattern in the list
                matches.append([word1.lemma + ":" + obj, sent.id] if obj else [word1.lemma, sent.id])

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
    muc_pickle_loc = ROOT + "\\processed_data\\muc_annotation.pkl"
    
    if exists(muc_pickle_loc):
        print('Annotation file already created. No need to run this script again.')
    else:
        # 1) load all MUC articles (1700)-----
        muc_data = load_muc()

        # 2) annotate with the stanza pipeline-----
        muc_data = annotate(muc_data)    

        # 3) extract event patterns-----
        tqdm.pandas(desc='Extracting event patterns')
        muc_data['event_patterns'] = muc_data.progress_apply(lambda row: get_event_patterns(row['annotation']), axis=1)
        
        # 4) save to pickle (JSON will throw an overflow error)-----
        muc_data.to_pickle(ROOT + "\\processed_data\\muc_annotation.pkl")

if __name__ == "__main__":
    main()