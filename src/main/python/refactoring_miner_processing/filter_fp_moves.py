import json
import os
import re
import subprocess

from MethodSignature import MethodSignature
from MoveMethodValidator import MoveMethodValidator
count = 0

def is_tp_extractmovemethod(refactoring):
    original_class = refactoring['description'].split(' & moved to class')[0].split('.')[-1]
    right_code_element = \
        [i for i in refactoring['rightSideLocations'] if i['description'] == "extracted method declaration"] \
            [0]['codeElement'].split(":")[0]
    return re.search(r'\b' + re.escape(original_class) + r'\b', right_code_element) is not None





def filter_fp_moves(data, project_path):
    tp_moves = []
    for i, commit in enumerate(data['commits']):
        print(f"processing commit ({i}/{len(data['commits'])})")
        print(f"{len(tp_moves)=}")
        try:
            tp_moves += MoveMethodValidator(commit, project_path).get_valid_moves()
        except Exception as e:
            print("failed to process commit")
        if i%100==0:
            with open(os.path.join(filtered_path, fname), "w") as f:
                json.dump(tp_moves, f, indent=4)

    return tp_moves


if __name__ == '__main__':
    filtered_path = "../../../../data/refminer_data/filter_fp"
    mm_path = "../../../../data/refminer_data/contains_a_mm"
    evaluation_dir = "/Users/abhiram/Documents/TBE/evaluation_projects"
    project_basepath_map = {
        'vue_pro_res.json': '',
        'flink_res.json': '',
        'halo_res.json': '',
        'elastic_res.json': '',
        'redisson_res.json': '',
        'spring_framework_res.json': '',
        'springboot_res.json': '',
        'stirling_res.json': '',
        'selenium_res.json': '',
        'ghidra_res.json': 'ghidra',
        'dbeaver_res.json': ''}
    files = [i for i in os.listdir(mm_path) if i.endswith(".json")]
    for fname in files:
        print(fname)
        project_name = project_basepath_map.get(fname)
        if project_name=='' or project_name is None:
            print(f"skipping {fname}")
            continue

        with open(os.path.join(mm_path, fname)) as f:
            move_data = json.load(f)
        filtered_moves = filter_fp_moves(move_data, os.path.join(evaluation_dir, project_name))
        with open(os.path.join(filtered_path, fname), "w") as f:
            json.dump(filtered_moves, f, indent=4)
