from json import load
import pickle

def load_drugs_data(data_path: str, source = 'pickle', n = 776569, unique_articles = True) -> list:
    """Retrieves a specified number (n) of articles from the drugs database

    Parameters
    ----------
    data_path : str
        The file location of the database_dump_drugs folder or the pickle file
    source : str
        Whether to load from the original json files, or a previously created pickle (default is pickle, which is three times faster)
    n : int
        Number of articles requested (default is all)
    unique_articles : boolean
        Whether or not to remove absolute duplicates from the dataset

    Returns
    -------
    list
        A list of string articles
    """

    if source == 'pickle':
        with open(data_path, "rb") as fp:   # Unpickling
            data = pickle.load(fp)

    elif source == 'json':
        processing = True
        json_doc = 0
        data = []    

        while processing == True:
            with open(f'{data_path}/{json_doc}.json') as f:
                raw = load(f)
            json_doc += 1
            for _ in raw:
                content = _['article_contentRaw']
                if isinstance(content, str):
                    data.append(content)

            if unique_articles:
                data = list(set(data))
            
            if json_doc == 1572:
                processing = False
                print(f'Reached maximum number of articles ({len(data)}), terminating loading process.')
                # there are 779569 total articles, out of which 178647 unique
            elif len(data) >= n:
                processing = False
                print(f"Loaded {n} articles.")
            else:
                if json_doc % 100 == 0:
                        print(f'Loaded {len(data)} articles from the {json_doc} out of 1572 json files.')
    else:
        print("Invalid source, please specify either 'json' or 'pickle'.")
    
    return data[:n]