## Chambers & Jurafsky (2011) 
### Template-Based Information Extraction Without the Templates*

----

This folder contains the code to do template extraction according to the methodology as described by Chambers & Jurafsky - henceforth referred to as CJ11.    

The authors describe a three-step process:
1. "Cluster the domains event patterns to approcimate the template topics"  
2. "Build a new corpus _specific to each cluster_ by retrieving documents from a larger unrelated corpus"  
3. "Induce each template's slots using its new (larger) corpus of documents"

(From the first paragraph of section 4, pp. 978)  
The implementation of these steps is in [``` 1_template_topics.py ```](1_template_topics.py), [``` 2_build_corpus.py ```](2_build_corpus.py), [``` 3_template_slots.py ```](3_template_slots.py) respectively.

----

### 1 - Template topics

There is a discrepancy between my implementation and the paper, namely that they state they have a maximum cluster
size (m) of 40 for 77 clusters (pp. 979). This indicates that the number of event patterns the authors extract is O(40*77) = O(3080).
This is ridiculously little, if we extract all verbs, event nouns, and verb:objs from 1700 documents. 
There's definetely more than 2 event patterns in a news article!

Precisely following their method, my maximum cluster size did not go lower than ~320. So I came up with a different strategy.
Before considering a verb an event pattern, I checked it against a list of the most common verbs in the corpus.
If a verb occurs in more than _10%_ of all documents, it is not considered as an event pattern.

----

*Full citation:  
Chambers, N., & Jurafsky, D. (2011). Template-based information extraction without the templates. _Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies_, 976–986. [https://aclanthology.org/P11-1098/](https://aclanthology.org/P11-1098/)