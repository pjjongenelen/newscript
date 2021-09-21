from warnings import catch_warnings, simplefilter

import stanza
from nltk.corpus import wordnet as wn

import helpers


def create_pipeline():
    # Create Stanza pipeline
    stanza.download(
        lang="en", processors="tokenize,mwt,pos,lemma,depparse", logging_level="WARN"
    )
    # TODO: Zijn dit wel de goeie processoren?
    return stanza.Pipeline(
        lang="en", processors="tokenize,mwt,pos,lemma,depparse", verbose=False
    )


def retrieve_document() -> stanza.Document:
    # Retrieve Stanza document
    text = helpers.get_text()
    pipeline = create_pipeline()
    return pipeline(text)


def get_event_nouns():
    # Define a list of eventive nouns
    with catch_warnings():
        simplefilter("ignore")
        return list(
            set(
                [
                    w
                    for s in wn.synset("event.n.01").closure(lambda s: s.hyponyms())
                    or wn.synset("act.n.02").closure(lambda s: s.hyponyms())
                    for w in s.lemma_names()
                ]
            )
        )


def get_entities():
    # Extract entities from the text
    doc = retrieve_document()
    print(
        f"Retrieved Stanza document ({doc.num_words} words, {doc.num_tokens} tokens)."
    )
    wn_evnouns = get_event_nouns()
    print(f"Identified {len(wn_evnouns)} eventive nouns.")
    entities = []
    print('Extracting entities...')
    for sent in doc.sentences:
        for word in sent.words:
            if word.upos == "NOUN" and (
                sent.words[word.head - 1].upos == "VERB"
                or sent.words[word.head - 1].text in wn_evnouns
            ):
                closest_mod = get_closest_mod(sent, word)
                entities = append_entities_list(entities, sent, word, closest_mod)
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


def append_entities_list(entities, sent, word, closest_mod):
    # Append entities list based on closest modifier
    if closest_mod[2] < 100000:
        # Attach the modifier to the list
        entities.append(
            [
                word.text,
                sent.id,
                word.id,
                sent.words[word.head - 1].text,
                sent.words[word.head - 1].id,
                word.deprel,
                closest_mod[3],
                closest_mod[1],
            ]
        )
    else:
        # Attach without a modifier
        entities.append(
            [
                word.text,
                sent.id,
                word.id,
                sent.words[word.head - 1].text,
                sent.words[word.head - 1].id,
                word.deprel,
                "",
                -1,
            ]
        )
    return entities
