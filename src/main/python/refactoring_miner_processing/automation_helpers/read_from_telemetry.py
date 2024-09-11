import json


project_name = "kafka"
project_basepath = "/Users/abhiram/Documents/TBE/evaluation_projects"
refminer_filtered_file = f"../../../../../data/refminer_data/filter_fp/{project_name}_res.json"
mm_assist_outfile = f"../../../../../data/refminer_data/mm-assist/{project_name}_res.json"

with open(refminer_filtered_file) as f:
    refdata = json.load(f)

telemetry_file = '/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/build/idea-sandbox/system/log/ref_plugin_logs/ref_telemetry_data.jsonl'
with open(telemetry_file) as f:
    telemetry = [json.loads(i) for i in f.read().split('\n') if i!=''][-len(refdata):]
for tele, ref in zip(telemetry, refdata):
    ref["telemetry"] = tele

with open(mm_assist_outfile, "w") as f:
    json.dump(refdata, f, indent=4)