"""
To induce the template slots from the clustered documents, we need to do the following:

1. extract the correct coreference information using CoreNLPClient annotations.
2. calculate similarity between event patterns using cosine distance.
3. agglomerative clustering to get cluster's event patterns that indicate template slots.

**In pseudocode:**  

*Annotate articles*  
_ FOR each article:  
____ annotate the CoreNLPClient  
_ return annotations  

*---Current progress in scratchpad.ipynb---*  

*Fill the coreference matrix*  
_ FOR each article annotation:  
____ find all corefering sets of entities (set is of size 1 if no coreference is found)  
____ FOR each corefering set:    
_______ FOR each set member:  
__________ extract subject/object verbs as *verb:o or verb:s*    
_______ FOR each subject/object i:  
__________ FOR each other subject/object j:  
_____________ coref_matrix[i,j] += 1

*Calculate cosing similarity*
- See helpers.cos_sim(a, b)

*Agglomerative clustering*
- See code in 1_template_topics.main()
"""

