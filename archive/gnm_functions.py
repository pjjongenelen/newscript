"""
Old functions that consider the GNM dataset. Stored here in case I need the code later on. 
No use in throwing it away now and having to reinvent the wheel next week.
"""

def joa_to_nguyen(input_path: str, output_path: str, number_of_files: int = 4):
    df = []

    for x in range(number_of_files):
        with open(f"{input_path}/{x}.json", "r") as f:
            df.append(json.load(f))

    # flatten the list
    text = [d['article_contentRaw'] for dump in df for d in dump]
    # remove empty articles, encoding errors, and duplicate articles
    text = [t.replace("\n", " ").encode('ascii', 'ignore').decode('ascii') for t in text if t != None]
    text = set(text)
    text_id = [f"joa{x}" for x in range(len(text))]

    with open(output_path, "w") as f:
        for i, t in zip(text_id, text):
            f.write(i + "\t" + t + "\n")


def load_gnm(data_path: str, cols: list, n = 776569) -> list:
    """Retrieves a specified number (n) of articles from the drugs database

    Parameters
    ----------
    data_path : str
        The file location of the database_dump_drugs folder or the pickle file
    cols : list
        Which columns to get from the original dataset
    n : int
        Number of articles requested (default is all)

    Returns
    -------
    list
        A list of lists per article
    """
    
    processing = True
    json_doc = 0
    data = []    

    while processing == True:
        with open(f'{data_path}/{json_doc}.json') as f:
            raw = load(f)
            json_doc += 1
            for _ in raw:
                content = []
                for col in cols:
                    content.append(_[col])
                data.append(content)
        
        if json_doc == 1572:
            processing = False
            print(f'Reached maximum number of articles ({len(data)}), terminating loading process.')
            # there are 779569 total articles, out of which 178647 unique
        elif len(data) >= n:
            processing = False
            print(f"Loaded {n} articles.")
        else:
            if json_doc % 100 == 0:
                    print(f'Loaded {len(data)} articles from {json_doc} out of 1572 json files.')
    
    return data[:n]