The following table gives an example of what the argument matrix looks like: 
- The columns represent all entities that are found as either subjects or objects in the corpus
- The rows are all event patterns, represented twice. Once as a subject and once as an object relation


A 1 in a cell indicates that entity_x served as either the __:subj or __:obj of ev_patternX.  


|                  | entity_1 | entity_2 | entity_3 | entity_4 | entity_5 |
|------------------|----------|----------|----------|----------|----------|
| ev_pattern1:subj | 1        | 0        | 0        | 1        | 0        |
| ev_pattern2:subj | 0        | 0        | 1        | 0        | 0        |
| ev_pattern3:subj | 0        | 0        | 0        | 1        | 0        |
| ev_pattern1:obj  | 0        | 1        | 0        | 0        | 0        |
| ev_pattern2:obj  | 0        | 0        | 0        | 0        | 0        |
| ev_pattern3:obj  | 0        | 0        | 0        | 0        | 1        |