"""
Annotates all MUC articles with a Stanza pipeline and extracts event patterns:
verbs, event nouns, or verb:object pairs

Objects are NER tagged (if possible), and we only store lemmas.

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
NLP = en_core_web_sm.load()


def annotate(df: pd.DataFrame) -> pd.DataFrame:
    """Annotates each article with the Stanza pipeline."""
    # extract texts
    articles = df['text']

    # for loop so we can use tqdm
    annotations = []
    for index in tqdm(range(len(articles)), desc="Annotating articles"):
        text = articles[index]
        annotations.append(PIPE(text))

    return annotations


def get_noun_patterns(annotation) -> list:
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


def get_verb_patterns(annotation, freq_verbs: list) -> list:
    """
    Extract verbs and their syntactic object head words
    """

    matches = []

    # extract verbs and their object head words (if they have one)
    for sent in annotation.sentences:
        for word1 in sent.words:
            # find all verbs - the most frequent ones
            if word1.upos == 'VERB' and word1.lemma not in freq_verbs:
                # look for a corresponding object
                obj = None
                for word2 in sent.words:
                    if 'obj' in word2.deprel and word2.head == word1.id:
                        # check for available NER tag
                        ann = NLP(word2.text)
                        obj = ann.ents[0].label_ if len(ann.ents) > 0 else word2.lemma
                        break

                # put the event pattern in the list
                matches.append([word1.lemma + ":" + obj, sent.id] if obj else [word1.lemma, sent.id])

    # convert to lowercase
    matches = [[m1.lower(), m2] for [m1, m2] in matches]    

    return matches


def get_event_patterns(annotation, freq_verbs: list) -> list:
    """
    Extracts the two possible event pattern representations:
    1. event nouns based on the WordNet synsets
    2. verbs and the head words of their syntactic objects (with NER tags if available)
    """

    # 1. event nouns
    ev_patterns = get_noun_patterns(annotation)

    # 2. verbs (+ head words / NER tags)
    ev_patterns += get_verb_patterns(annotation, freq_verbs)

    return ev_patterns


def main():
    source = "gnm"
    pickle_loc = f"{ROOT}\\processed_data\\{source}_annotation.pkl"
    
    if exists(pickle_loc):
        print('Annotation file already created. No need to run this script again.')

    else:
        # 1) load all 1700 articles-----
        df = pd.read_csv(f'{ROOT}\\processed_data\\{source}.csv', index_col=0)

        # 2) annotate with the stanza pipeline-----
        df['annotation'] = annotate(df)    

        # 3) extract event patterns-----
        freq_verbs = helpers.get_freq_verbs(df, threshold = 0.3)
        tqdm.pandas(desc='Extracting event patterns')
        df['event_patterns'] = df.progress_apply(lambda row: get_event_patterns(row['annotation'], freq_verbs), axis=1)
        
        # 4) save to pickle (JSON will throw an overflow error)-----
        df.to_pickle(f"{ROOT}\\processed_data\\{source}_annotation.pkl")

if __name__ == "__main__":
    main()