import json
import os

def filter_fp(data):
    bad_refs = ['Move Class', 'Extract Class']
    filtered_commits = []
    for commit in data['commits']:
        for ref in commit['refactorings']:
            if ref['type'] in bad_refs:
                break
            filtered_commits.append(commit)

    return {"commits": filtered_commits}


mm_path = "../../../../data/refminer_data/contains_a_mm"
filtered_path = "../../../../data/refminer_data/filter_false_positives"
files = [i for i in os.listdir(mm_path) if i.endswith(".json")]
for fname in files:
    print(fname)
    with open(os.path.join(mm_path, fname)) as f:
        move_data = json.load(f)
    filtered_moves = filter_fp(move_data)
    with open(os.path.join(filtered_path, fname), "w") as f:
        json.dump(filtered_moves, f, indent=4)