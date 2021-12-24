"""
Extract and cluster a domain's event patterns (event nouns + verb-noun pairs) to approximate the template topics.
"""

import helpers
import pandas as pd
from sklearn.preprocessing import MultiLabelBinarizer

ROOT = 'C:\\Users\\timjo\\PycharmProjects\\newscript'


def load_muc(amount=0):
    """Returns _n_ articles from the MUC dataset, with stanza annotation and event patterns"""

    # load only article texts
    df = pd.read_csv(ROOT + '\\processed_data\\muc.csv', index_col=0)
    articles = df['text'].tolist()

    # sample (or not) based on the value of amount
    articles = helpers.conditional_sample(articles=articles, amount=amount)

    # turn into a list of objects with annotation and event patterns
    articles = [helpers.Article(text) for text in articles]

    return articles


def get_event_pattern_matrix(articles):
    """Creates one-hot encoding matrix from event patterns of all articles"""

    # put all event pattern lists in a series object
    ep_list = pd.Series([art.get_event_patterns() for art in articles])

    mlb = MultiLabelBinarizer()
    encoding = pd.DataFrame(mlb.fit_transform(ep_list), columns=mlb.classes_, index=ep_list.index)

    return encoding


def distance_clustering():
    """Agglomerative clustering on event patterns"""

    # TODO: implement

    return True


def main():
    # load the muc / gnm data
    arts = [100, 300, 500, 700, 900, 1100]
    for a in arts:
        article_list = load_muc(amount=a)
        event_pattern_matrix = get_event_pattern_matrix(article_list)
        print(f'Amount of articles: {a}, shape: {event_pattern_matrix.shape}')

    # TODO: perform agglomerative clustering on event pattern distance
    distance_clustering()


if __name__ == "__main__":
    main()
