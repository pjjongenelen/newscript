### muc.csv  
contains the MUC dataset as received from Nguyen, preprocessed to:
- CSV format
- Truecase

### muc_annotation.pkl  
contains the contents of muc.csv, but with an added column with the stanza pipeline annotation of each article text. This is useful for quick loading of the annotation, as annotation can take 20+ minutes for the 1700 articles.