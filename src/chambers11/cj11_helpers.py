import pandas as pd

def split_df_column (df: pd.DataFrame, col1: str, col2: str, sep: str):
    # split col1 of a dataframe into col1 and col2 based on separator sep
    _ = [d.split(sep, 1) for d in df[col1]]    
    df[col1] = [d[0] for d in _]
    df[col2] = [d[1] for d in _]
    return df