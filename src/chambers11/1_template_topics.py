"""
Extract and cluster a domain's event patterns (event nouns + verb-noun pairs) to approximate the template topics.
"""

import helpers
import os
import pandas as pd

ROOT = 'C:\\Users\\timjo\\PycharmProjects\\newscript'


def load_gnm(amount=0):
    """Returns _n_ articles from the GNM dataset"""

    # TODO: implement

    return None


def load_muc(amount=0):
    """Returns _n_ articles from the MUC dataset, with stanza annotation and event patterns"""

    # load only article texts
    df = pd.read_csv(ROOT + '\\processed_data\\muc.csv', index_col=0)
    articles = df['text'].tolist()

    # sample (or not) based on the value of amount
    articles = helpers.conditional_sample(articles=articles, amount=amount)

    # turn into a list of objects
    articles = [helpers.Article(text) for text in articles]

    return articles


def distance_clustering():
    """Agglomerative clustering on event patterns"""

    # TODO: implement

    return True


def main():
    # load the muc / gnm data
    article_list = load_muc(amount=10)

    # TODO: perform agglomerative clustering on event pattern distance
    distance_clustering()


if __name__ == "__main__":
    main()
