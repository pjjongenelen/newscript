"""
Annotates all MUC articles with a Stanza pipeline, and saves the result to a pickle file.
"""

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


def main():
    # load all MUC articles (1700)
    muc = load_muc()

    # annotate with the stanza pipeline
    muc = annotate(muc)    
    
    # save to pickle (JSON will throw an overflow error)
    muc.to_pickle(ROOT + "\\processed_data\\muc_annotation.pkl")

if __name__ == "__main__":
    main()