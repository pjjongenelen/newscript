"""
File that contains functions to make other code more readable.
"""

# TODO: clean up imports
import gensim
from gensim import models
from nltk.stem.porter import *
import os
import stanza


def retrieve_document() -> stanza.Document:
    # Retrieve Stanza document
    text = get_sample_text()
    pipeline = create_pipeline()
    return pipeline(text)


def get_sample_text():
    return 'Detectives have seized a large sum of cash along with drugs and a designer watch after targeting a female associate of drugs lord Brian Rattigan.\nArmed gardai raided a south Dublin property on Tuesday evening as part of an ongoing investigation into Rattigan\'s mob.\nThe convicted killer, who is serving a jail term in Portlaoise Prison, is suspected of continuing to run a drugs operation behind bars, and his mob is believed to be widening its criminal network ahead of his release next year.\nOn Tuesday, detectives attached to Sundrive Road Garda Station raided a property in the Walkinstown area on foot of a search warrant.\nNetwork\nThe house is linked to a close female associate of Rattigan, and during a search of the premises around €10,000 in cash was seized.\nDetectives also recovered a watch valued at around €4,000 and a small quantity of drugs.\nThe cash and watch were seized on suspicion of being the proceeds of crime, but no arrests have yet been made.\nAnother associate of Rattigan was also in the property at the time of the raid, but neither of the two associates is believed to be directly involved in the drugs trade.\n"Brian Rattigan may be locked up, but he still has a network on the outside that continues to be involved in the sale and supply of drugs in south Dublin," a source told the Herald.\n"The latest operation was co-ordinated by the local crime management, but national units are also continuing to investigate the gang.\n"The concern is that Rattigan is building up his drugs gang again ahead of his release and gardai are determined to prevent that from happening."\nDetectives from Crumlin and Sundrive Road Garda Stations, who have been investigating the gang over the past two decades, have made a number of significant seizures against the mob in recent months.\nLast December, more than €100,000 worth of drugs was seized and a key lieutenant arrested after gardai swooped on a number of cars in a south Dublin fast-food restaurant car park.\nLocal detectives and drugs unit gardai also searched two houses in the Crumlin and Drimnagh areas and arrested two people aged in their 20s.\nOne of them is a close associate of Rattigan, who gardai suspect was heavily involved in running the day-to-day operations while the mobster was behind bars.\nRattigan, who is due for release in November, is being held on C-Wing at Portlaoise Prison, the same wing where Freddie Thompson is locked up.\nThe two men were centrally involved in the Crumlin/Drimnagh feud, but in the High Court last year both claimed they have since reconciled with jailhouse meetings.\nIn January of last year, Ratt- igan was jailed for nine years for stabbing Declan Gavin to death in 2001.\nRattigan, of Cooley Road, Drimnagh, pleaded guilty to the manslaughter of Mr Gavin (20), who he stabbed in the heart with a knife outside an Abrakebabra in Crumlin on August 25 that year.\nVerdict\nMr Justice Michael White made the sentence concurrent to a jail term he was already serving and backdated it to October 1, 2018.\nRattigan had been tried for murder twice in 2009. The first jury could not reach a verdict, but a second jury convicted him.\nHowever, that conviction was then successfully appealed in 2017.\nHe was due for another retrial last year, but on October 22 he entered a guilty plea to the lesser charge, which was accepted by the State.\nRattigan has been in custody since 2003, and in 2013 was given a 17-year sentence for the sale or supply of drugs from within prison, with a release date with remission in November this year.'


def create_pipeline():
    # Create Stanza pipeline
    stanza.download(
        lang="en",
        processors="tokenize,pos,lemma,depparse",
        logging_level="WARN",
    )
    return stanza.Pipeline(
        lang="en", processors="tokenize,mwt,pos,lemma,depparse", verbose=False
    )

def split_df_column (df, col1, col2, sep):
    # split col1 of a dataframe into col1 and col2 based on separator sep
    _ = [d.split(sep, 1) for d in df[col1]]    
    df[col1] = [d[0] for d in _]
    df[col2] = [d[1] for d in _]
    return df

def preprocess(text):
    result = []
    stemmer = PorterStemmer()
    for token in gensim.utils.simple_preprocess(text):
        if token not in gensim.parsing.preprocessing.STOPWORDS and len(token) > 3:
            result.append(stemmer.stem(WordNetLemmatizer().lemmatize(text, pos='v')))
    return result

def topic_modeling(docs, ntopics: int, extreme_low = 15, extreme_high = 0.5, iterations = 50, tfidf = True):
    # generic topic modeling algorithm from:
    # https://towardsdatascience.com/topic-modeling-and-latent-dirichlet-allocation-in-python-9bf156893c24
    
    processed_docs = []
    for x in range(len(docs)):
        doc = docs[x]
        stemmer = PorterStemmer()
        _ = []

        for token in gensim.utils.simple_preprocess(doc):
            if token not in gensim.parsing.preprocessing.STOPWORDS and len(token) > 3:
                _.append(stemmer.stem(WordNetLemmatizer().lemmatize(token, pos = 'v')))
        
        processed_docs.append(_)

    dictionary = gensim.corpora.Dictionary(processed_docs)
    dictionary.filter_extremes(no_below=extreme_low, no_above=extreme_high)
    bow_corpus = [dictionary.doc2bow(doc) for doc in processed_docs]

    tfidf = models.TfidfModel(bow_corpus)
    corpus_tfidf = tfidf[bow_corpus]

    if tfidf == False:
        lda_model = gensim.models.LdaMulticore(bow_corpus, num_topics=ntopics, id2word=dictionary, passes=2, workers=4, iterations=iterations)
        for idx, topic in lda_model.print_topics(-1):
            print('Topic: {} \nWords: {}'.format(idx+1, topic))
        return lda_model
    else:
        lda_model_tfidf = gensim.models.LdaMulticore(corpus_tfidf, num_topics=ntopics, id2word=dictionary, passes=2, workers=4, iterations=iterations)
        for idx, topic in lda_model_tfidf.print_topics(-1):
            print('Topic: {} Word: {}'.format(idx+1, topic))
        return lda_model_tfidf
    