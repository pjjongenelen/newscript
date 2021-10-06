from truecase import get_true_case


def muc_to_truecase(muc_data_path: str, output_path: str):
    # convert the muc dataset from Nguyen to true case to bypass the faulty part of their code
    f = open(muc_data_path, "r")
    docs = f.readlines()

    text_cat = [d[:3] if d[:3] == "DEV" else d[:4] for d in docs]
    text_id = [d[4:13] if d[:3] == "DEV" else d[5:14] for d in docs]
    text = [d[14:] if d[:3] == "DEV" else d[15:] for d in docs]
    text = [get_true_case(t).replace(" .", ".") for t in text]

    with open(output_path, "w") as f:
        for c, i, t in zip(text_cat, text_id, text):
            f.write(c + "-" + i + "\t" + t + "\n")
