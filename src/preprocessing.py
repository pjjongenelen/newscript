import json

from truecase import get_true_case



def muc_to_truecase(input_path: str, output_path: str):
    # convert the muc dataset from Nguyen to true case to bypass the deprecated first part of their code (commented out)
    with open(input_path, "r") as f:
        docs = f.readlines()

    text_cat = [d[:3] if d[:3] == "DEV" else d[:4] for d in docs]
    text_id = [d[4:13] if d[:3] == "DEV" else d[5:14] for d in docs]
    text = [d[14:] if d[:3] == "DEV" else d[15:] for d in docs]
    text = [get_true_case(t).replace(" .", ".") for t in text]

    with open(output_path, "w") as f:
        for c, i, t in zip(text_cat, text_id, text):
            f.write(c + "-" + i + "\t" + t + "\n")

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