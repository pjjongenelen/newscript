from json import load

def load_data(n):    
    # create the raw dataset
    processing = True
    data = []
    doc = 0
    docs = 0
    while processing == True:
        with open(f"C:/Users/s161158/OneDrive - TU Eindhoven/Silva Ducis/Master_Thesis/drugs data/{doc}.json") as f:
            raw = load(f)
        doc += 1
        for _ in raw:
            content = _['article_contentRaw']
            if isinstance(content, str):
                data.append(content)
        data = list(set(data))
        if docs == len(data):
            processing = False
            print(f'Reached maximum number of articles ({len(data)}), terminating loading process.')
        elif len(data) >= n:
            processing = False
            print(f"Loaded {n} articles.")
        else:
            docs = len(data)
    
    return data[:n]