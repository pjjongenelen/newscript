from warnings import catch_warnings, simplefilter

from nltk.corpus import wordnet

import helpers


def main():
    doc = helpers.retrieve_document()
    print(
        f"Retrieved Stanza document ({doc.num_words} words, {doc.num_tokens} tokens)."
    )

    event_nouns = define_event_nouns()
    print(f"Identified {len(event_nouns)} eventive nouns.")

    entities = get_entities(doc, event_nouns)
    print(f"Extracted {len(entities)} entities:")

    for entity in entities:
        print(entity)


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


def get_entities(doc, event_nouns):
    # Extract entities from the text
    entities = []
    print("Extracting entities...")
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
