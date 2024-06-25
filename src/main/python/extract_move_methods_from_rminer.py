import json

import pandas as pd

from github import Github

# Authentication is defined via github.Auth
from github import Auth
import os

# using an access token
auth = Auth.Token(os.environ['GITHUB_TOKEN'])
g = Github(auth=auth)

get_source_class = lambda x: x.split('from class')[1].split(' ')[1]
get_to_class = lambda x: x.split('from class')[2].strip()

def select_move_methods(ref_miner):
    count = 0
    for i in ref_miner:
        refs = []
        for j in i['refactorings']:
            if j['type'] == 'Move Method':
                count += 1
                refs.append(j)

        i['moveMethods'] = refs


def main():
    with open("/Users/abhiram/Documents/TBE/ref-miner.json") as f:
        ref_miner = json.load(f)
    select_move_methods(ref_miner)
    move_method_examples = [i for i in ref_miner if i['moveMethods'] != []]

    df = create_dataframe(move_method_examples)

    print(df)
    df.to_csv("~/Downloads/move_methods_all.csv", index=False)


def get_file_contents(obj, source_class):

    repo_name = ".".join(obj['repository'].split('github.com/')[1].split('.')[:-1])
    repo = g.get_repo(repo_name)
    sha = obj['sha1']
    commit = repo.get_commit(sha)

    parent_commit = commit.parents[0]

    source_class_name = get_class_name(source_class)

    matching_files = list(filter(lambda x: x.filename.endswith(f"/{source_class_name}.java"), commit.files))
    if len(matching_files)==1:
        filename = matching_files[0].filename
    elif len(matching_files)>1:
        class_path = source_class.split(source_class_name)[0].replace('.', '/')
        matching_files = list(filter(lambda x: class_path in x.filename, matching_files))
        if len(matching_files)==0:
            raise Exception("No match found")
        elif len(matching_files)>1:
            raise Exception("Too many match found")
        filename = matching_files[0].filename

    else:
        raise Exception("No match found")

    file_contents = repo.get_contents(filename, parent_commit.sha).\
        decoded_content.decode('utf-8')

    return filename, file_contents

def is_camelcase(name):
    return name[0].isupper()

def get_class_name(source_class):
    class_names = list(filter(is_camelcase, source_class.split('.')))
    if len(class_names) > 0:
        return class_names[0]
    raise Exception("No valid class name found.")


def create_dataframe(move_method_examples):
    pd_data = []
    count = 0
    for obj in move_method_examples:
        for m in obj['moveMethods']:
            if m['validation'] != 'TP':
                continue
            try:
                filepath, file_contents = get_file_contents(obj,
                                  get_source_class(m['description']))
            except:
                print("failed to fetch file contents")
                continue
            count += 1
            pd_data.append(
                {
                    "project_name": obj['url'].split('commit')[0],
                    "commit": obj['url'].split('/')[-1],
                    "description": m['description'],
                    "comments": m['comment'],
                    "validation": m['validation'],
                    "file_path": filepath,
                    "file_contents": file_contents,
                    'ID':count
                }
            )
    df = pd.DataFrame(pd_data)


    df['get_source_class'] = df['description'].apply(get_source_class)


    df['dest_class'] = df['description'].apply(get_to_class)
    
    
    return df


if __name__ == '__main__':
    main()