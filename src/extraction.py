from nltk.corpus import wordnet
from warnings import catch_warnings, simplefilter

import numpy as np
import stanza

def create_nested_entites(ent, type):
    # input: list of entities resulting from event_extraction
    # output: list of lists with entities per sentence
    if type == "Chambers":
        invalid_count = sum(map(lambda x : len(x) != 3, ent))
    if type == "Nguyen":
        invalid_count = sum(map(lambda x : len(x) != 8, ent))

    if invalid_count == 0:
        for d in range(len(ent)):
            ent_per_sent = []
            for x in range(len(ent[d])):
                del ent[d][x][0]
                ent_per_sent.append(ent[d][x])

        print("Entity list processing succesful.")
        return ent_per_sent
    
    else:
        print("Entity list processing unsuccesful.")
        print(f"Something is wrong with the input data shape, elements are of length {len(ent[0])} instead of 8.")   

def create_pipeline():
    # Create Stanza pipeline
    stanza.download(
        lang="en",
        processors="tokenize,mwt,pos,lemma,depparse",
        logging_level="WARN",
    )
    return stanza.Pipeline(lang="en", processors="tokenize,mwt,pos,lemma,depparse", verbose=False)

def define_event_nouns():
    # Define a list of eventive nouns
    with catch_warnings():
        simplefilter("ignore")
        return list(
            set(
                [
                    w
                    for s in wordnet.synset("event.n.01").closure(
                        lambda s: s.hyponyms()
                    )
                    or wordnet.synset("act.n.02").closure(lambda s: s.hyponyms())
                    for w in s.lemma_names()
                ]
            )
        )

def event_extraction(docs, type):

    event_nouns = define_event_nouns()
    print(f"Identified {len(event_nouns)} eventive nouns.")
    
    entities = [get_entities(doc, event_nouns) for doc in docs]
    counter = 0
    for e in entities:
        counter += len(e)
    # print(f'Extracted {counter} elements from {len(entities)} input documents.')

    if type == "Chambers":
        c_entities = [to_chambers_ent(e) for e in entities]
        return [entities, c_entities]
    else:
        return entities


def get_entities(doc, event_nouns):
    # Extract entities from the text
    entities = []
    for sent in doc.sentences:
        for word in sent.words:
            if word.upos == "NOUN" and (
                sent.words[word.head - 1].upos == "VERB"
                or sent.words[word.head - 1].text in event_nouns
            ):
                closest_mod = get_closest_mod(sent, word)
                entities.append(determine_entities(sent, word, closest_mod))
    return entities


def get_closest_mod(sent, word):
    # check if there is a modifier
    closest_mod = [sent.id, -3, 100000, ""]
    for mod in sent.words:
        if (
            "mod" in mod.deprel
            and mod.head == word.id
            and abs(word.id - mod.id) < closest_mod[2]
        ):
            closest_mod = [sent.id, mod.id, abs(word.id - mod.id), mod.text]
    return closest_mod

def get_pmi_matrix(events):
    # input: list of entities in chambers' format
    # output: pmi matrix

    # find out how many unique event triggers we have, to create the dimensions of the cdist/pmi matrix
    flat_events = [item for sublist in events for item in sublist]
    flat_events = [item[2] for item in flat_events]
    sorted_set = sorted(list(set(flat_events)))

    cdist = np.zeros((len(sorted_set), len(sorted_set)))

    # for each document
    for doc in events:
        # for all events in this document, fill the cdist matrix
        for x in range(len(doc)):
            for y in range(len(doc)):
                if x == y:
                    i, j = get_cdist_coor(sorted_set, doc[x][2], doc[y][2])
                    cdist[i][j] = 0
                else:
                    g_wi_wj = abs(doc[x][0] - doc[y][0]) + 1
                    i, j = get_cdist_coor(sorted_set, doc[x][2], doc[y][2])
                    if cdist[i][j] == np.inf:
                        cdist[i][j] = 1 / g_wi_wj
                    else:
                        cdist[i][j] += 1 / g_wi_wj

    # calculate pw values
    pw = []
    for x in range(len(sorted_set)):
        _ = flat_events.count(sorted_set[x])
        pw.append(_ / (len(flat_events) - _))

    # calculate pdist values
    sum_cdist = 0
    for k in range(len(sorted_set)):
        for l in range(len(sorted_set)):
            if k != x and k != y and l != x and l != y and k != l:
                sum_cdist += cdist[k][l]

    for x in range(len(sorted_set)):
        for y in range(len(sorted_set)):
            cdist[x][y] = cdist[x][y] / (sum_cdist - cdist[x][y])
        if x % 500 == 0:
            print(f'Done {x+1} out of {len(sorted_set)}')
    
    # note that at this point, cdist is actually equal to pdist in the paper
    # calculate pmi values
    for x in range(len(sorted_set)):
        for y in range(len(sorted_set)):
            cdist[x][y] = cdist[x][y] / (pw[x] * pw[y])

    return cdist, flat_events, sorted_set

def get_cdist_coor(flatlist, wordx, wordy):
    x = flatlist.index(wordx)
    y = flatlist.index(wordy)
    return x, y


def determine_entities(sent, word, closest_mod):
    # Determine addition to entities list based on closest modifier
    result = [
        word.text,
        sent.id,
        word.id,
        sent.words[word.head - 1].text,
        sent.words[word.head - 1].id,
        word.deprel,
    ]
    if closest_mod[2] < 100000:
        # Append with modifier
        result += [closest_mod[3], closest_mod[1]]
    else:
        # Append without modifier
        result += [
            "",
            -1,
        ]
    return result

def template_extraction(ent):
    nested_entities = create_nested_entites(ent)

    templates = get_pmi_matrix(nested_entities)

    return templates

def to_chambers_ent(ent):
    # transforms the Nguyen event representation into that of Chambers (2011)
    ent = list(set([(e[1], e[4], e[3]) for e in ent]))
    ent = [list(e) for e in ent]
    return ent