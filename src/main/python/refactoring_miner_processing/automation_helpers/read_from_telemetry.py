import json


project_name = "spring_framework"
project_basepath = "/Users/abhiram/Documents/TBE/evaluation_projects"
refminer_filtered_file = f"../../../../../data/refminer_data/filter_fp/{project_name}_res.json"
mm_assist_outfile = f"../../../../../data/refminer_data/mm-assist/{project_name}_res.json"
with open("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/classes_and_commits.json") as f:
    mm_assist_runs = json.load(f)

with open(refminer_filtered_file) as f:
    refdata = json.load(f)

telemetry_file = '/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/build/idea-sandbox/system/log/ref_plugin_logs/ref_telemetry_data.jsonl'
with open(telemetry_file) as f:
    telemetry = [json.loads(i) for i in f.read().split('\n') if i!=''][-len(mm_assist_runs):]
for ref in refdata:
    matches = [1 if i['new_commit_hash']==ref['sha1'] and i['file_path']==ref['move_method_refactoring']['leftSideLocations'][0]['filePath'] else 0 for i in mm_assist_runs ]
    index = matches.index(1)
    ref["telemetry"] = telemetry[index]

with open(mm_assist_outfile, "w") as f:
    json.dump(refdata, f, indent=4)