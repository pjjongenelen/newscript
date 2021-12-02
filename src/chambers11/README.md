## Chambers & Jurafsky (2011) 
### Template-Based Information Extraction Without the Templates

----

This folder contains the code to do template extraction according to the methodology as described by Chambers & Jurafsky - henceforth referred to as CJ11.    

The authors describe a three-step process:
1. "Cluster the domains event patterns to approcimate the template topics"  
implemented in [``` 1_template_topics.py ```](1_template_topics.py)
2. "Build a new corpus _specific to each cluster_ by retrieving documents from a larger unrelated corpus"  
not yet implemented in [``` 2_build_corpus.py ```](2_build_corpus.py)
3. "Induce each template's slots using its new (larger) corpus of documents"  
implemented in [``` 3_template_slots.py ```](3_template_slots.py)

(From the first paragraph of section 4, pp. 978)

---

Full citation:  
Chambers, N., & Jurafsky, D. (2011). Template-based information extraction without the templates. _Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies_, 976–986. [https://aclanthology.org/P11-1098/](https://aclanthology.org/P11-1098/)