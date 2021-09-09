from stanza.server import CoreNLPClient


def get_head_words(text, nlp, endpoint='8000'):
    """
    Extracts single-noun head words from an article.

    1) Extracts all noun phrases, tagged with 'NP' by the CoreNLPClient,
    2) Removes multiples (NPs that contain other NPS),
    3) Transforms multi-word NPs into single-word head nouns based on a simple Stanza Pipeline.
    4) Converts the list to a general sentence ID + word ID representation

    Parameters:
    text (string): Article
    endpoint (string): Port number to start the CoreNLPClient on
    nlp (stanza Pipeline): Vanilla stanza Pipeline

    Returns:
    list: Head word sentence IDs & word IDs
    """

    # extract all noun phrases from the article
    with CoreNLPClient(properties='corenlp_server-2e15724b8064491b.props', endpoint=f'http://localhost:{endpoint}', memory='8G',
                       be_quiet=True) as client:
        matches = client.tregex(text=text, pattern='NP')
    # reformat the data structure into a list of lists
    noun_phrases = [[text, sent, begin, end] for text, sent, begin, end in
                    zip([sentence[match_id]['spanString'] for sentence in matches['sentences'] for match_id in sentence],
                        [sentence[match_id]['sentIndex'] for sentence in matches['sentences'] for match_id in sentence],
                        [sentence[match_id]['characterOffsetBegin'] for sentence in matches['sentences'] for match_id in sentence],
                        [sentence[match_id]['characterOffsetEnd'] for sentence in matches['sentences'] for match_id in sentence])]

    # remove 'multiples'
    _ = [np1 for np1 in noun_phrases for np2 in noun_phrases if (np1 != np2) and (np1[1] == np2[1]) and ((np1[2] <= np2[2]) and (np1[3] >= np2[3]))]
    noun_phrases = [np for np in noun_phrases if np not in _]

    # convert multi-word noun phrases into single-word head nouns, and remove pronouns
    doc = nlp(text)
    head_words = [[np[2] + np[0].find(word.text), np[2] + np[0].find(word.text) + len(word.text)] for np in noun_phrases for sent in
                  nlp(np[0]).sentences for word in sent.words if word.deprel == "root" and word.upos != "PRON"]

    # convert the word indices into sentence ID and word ID pairs
    head_words = [[sent.id, word.id] for sent in doc.sentences for word in sent.words for [i, j] in head_words if
                  word.start_char == i and word.end_char == j]

    return head_words


def get_triggers(text, hw, nlp):
    """
    Creates head word-trigger pairings.

    1) Extract all verbs
    2) Get eventive nouns based on the Wordnet Synsets indicated by the authors
    3) Combine 1 and 2 into a list of trigger candidates
    4) Finds all head words that have a trigger as its subject/object/preposition.
    5) Looks for transitive triggers, and extracts the correct subject from the root verb.

    Parameters:
    text (string): article
    hw (list): list of head word IDs

    Returns:
    list: List of head word, trigger, and relations - e.g., [[4, 6, 'nsubj']]
    """

    from nltk.corpus import wordnet as wn
    from warnings import catch_warnings, simplefilter

    # parse the text and extract all verbs and lemmas
    doc = nlp(text)
    verbs = [[sent.id, word.id] for sent in doc.sentences for word in sent.words if word.upos == 'VERB']
    lemmas = [[word.lemma, sent.id, word.id] for sent in doc.sentences for word in sent.words]

    # generate a list of eventive nouns
    with catch_warnings():
        simplefilter("ignore")
        wn_evnouns = list(set([w for s in wn.synset('event.n.01').closure(lambda s: s.hyponyms()) for w in s.lemma_names()]))
        wn_evnouns += list(set([w for s in wn.synset('act.n.02').closure(lambda s: s.hyponyms()) for w in s.lemma_names()]))

    # generates a list of trigger candidates based on the verbs and eventive nouns in the text
    candidates = verbs + [[s, w] for [lemma, s, w] in lemmas if lemma in wn_evnouns and [s, w] not in verbs]

    # finds all head word - trigger dyads and their syntactic relation
    triggers = []
    for sent in doc.sentences:
        for word in sent.words:
            if ([sent.id, word.head] in candidates) and ([sent.id, word.id] in hw):
                if "IN" in word.xpos:
                    triggers.append([sent.id, word.id, word.head, word.xpos])
                elif any(_ in word.deprel for _ in ["subj", "obj"]):
                    triggers.append([sent.id, word.id, word.head, word.deprel])

    for [sent, word] in verbs:
        head = doc.sentences[sent].words[word - 1].head
        if head != 0:
            for [_, noun, verb, rel] in triggers:
                if verb == head and ('subj' in rel):
                    triggers.append([sent, noun, word, rel])

    return triggers


def get_attributes(text, hw, nlp):
    """
    Adds attributes to the head word-trigger pairings to create entity triplets

    1) Find modifier candidates (deprel == amod, vmod, or nmod)
    2) If a candidate is related to a head word, keep it as the adjective

    Parameters:
    text (string): article
    hw (list): list of head word IDs + trigger IDs + relations

    Returns:
    list: 'Triplet' lists of head word, trigger/relation, and attribute
    [[sent_id, hw_id, trig_id, relation, attr_id]] - e.g., [[1, 4, 6, 'nsubj', 3]]
    """

    from itertools import combinations

    # get candidate modifiers
    doc = nlp(text)
    candidates = [[sent.id, word.head, word.id] for sent in doc.sentences for word in sent.words if
                  any(_ in word.deprel for _ in ['nmod', 'amod', 'vmod'])]

    # keep only the modifier that is closest per head word
    _ = []
    for a, b, in combinations(candidates, 2):
        if (a[0], a[1]) == (b[0], b[1]) and (abs(a[1] - a[2]) > abs(b[1] - b[2])):
            _.append(a)
        elif (a[0], a[1]) == (b[0], b[1]) and (abs(a[1] - a[2]) == abs(b[1] - b[2])):
            _.append(b)
    candidates = [c for c in candidates if c not in _]

    # UNDER DEVELOPMENT
    for w in hw:
        for c in candidates:
            if c[0:2] == w[0:2]:
                w.append(c[2])

    return hw
