from collections import Counter

import json

def get_templates(label_path: str):
    # start with a 'labels' file (resulting from the java code)
    with open(label_path, "r") as f:
        label = json.loads(f.read())['data']

    topics = []

    # extract triplets per topic
    for x in range(35):
        topic_x = []
        for l in label:
            if str(x) in l.keys():
                topic_x.append([str(triplet) for triplet in l[str(x)]])
        
        # count the occurences of triplets per topic
        topic_x = dict(Counter([item for t in topic_x for item in t]))

        # keep only the 10 most frequent triplets per topic
        topics.append([(w, topic_x[w]) for w in sorted(topic_x, key=topic_x.get, reverse=True)][:10])

    return topics