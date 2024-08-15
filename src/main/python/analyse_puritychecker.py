import pandas as pd
import os
import json

from has_multiple_refactorings import filter_refactorings
from collections import defaultdict, Counter

def group_by_file(combined_data):
    file_refactoring_map = defaultdict(list)
    for commit in combined_data['commits']:
        for refactoring in commit['refactorings']:
            fileChanged = refactoring['leftSideLocations'][0]['filePath']
            refactoring['commitUrl'] = commit['commitUrl']
            file_refactoring_map[fileChanged].append(refactoring)
    return file_refactoring_map

def isPure(refactoring):
    if 'isPure' not in refactoring:
        return True
    return refactoring['isPure']
def filter_only_pure(file_refactoring_map):
    new_file_refactoring_map = {
        filename:refactorings for filename, refactorings in file_refactoring_map.items()
        if all([isPure(r) for r in refactorings])
    }
    return new_file_refactoring_map

def find_purity_pct(file_refactoring_map_filtered):
    refactoring_count = 0
    purity_count = 0
    purity_key_count = 0
    purity_key_true_count = 0
    for filename in file_refactoring_map_filtered:
        refactorings = file_refactoring_map_filtered[filename]
        for r in refactorings:
            if 'isPure' not in r:
                purity_count += 1
            else:
                purity_key_count += 1
                if r['isPure']:
                    purity_count += 1
                    purity_key_true_count += 1
            refactoring_count +=1
    print(f"{purity_count/refactoring_count}=")
    print(f"{purity_key_true_count/purity_key_count}=")

def print_project_stats(file_refactoring_map):
    projects = []
    for file in file_refactoring_map:
        refactoring0 = file_refactoring_map[file][0]
        commit_url = refactoring0['commitUrl']
        projects.append("/".join(commit_url.split('/')[3:5]))
    return Counter(projects)

def refactoring_type_stats(file_refactoring_map):
    refactorings = []
    for file in file_refactoring_map:
        for refactoring in file_refactoring_map[file]:
            refactorings.append(refactoring['type'])
    return Counter(refactorings)


purityOutDir = "/Users/abhiram/Documents/TBE/PurityChecker/purityOut"
purity_out_files = os.listdir(purityOutDir)

commits_data = []
combined_data = {"commits": commits_data}

for file in purity_out_files:
    with open(purityOutDir+"/"+file) as f:
        try:
            commits_data.append(json.load(f))
        except:
            continue

filter_refactorings(combined_data)
file_refactoring_map = group_by_file(combined_data)
file_refactoring_map_filtered = {k:v for k,v in file_refactoring_map.items() if len(v)>=5}
find_purity_pct(file_refactoring_map_filtered)
file_refactoring_map_pure = filter_only_pure(file_refactoring_map_filtered)
project_stats = print_project_stats(file_refactoring_map_pure)
refactoring_stats = refactoring_type_stats(file_refactoring_map_pure)
print(file_refactoring_map_pure)

