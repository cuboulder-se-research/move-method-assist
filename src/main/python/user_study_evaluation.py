import json
from collections import Counter

files = [
'mboulay_ref_telemetry_data.jsonl',
'ref_telemetry_data.jsonl',
'ref_telemetry_data-2.jsonl',
'ref_telemetry_data-3.jsonl'
]
all_user_data = []
for f in files:
    with open(f'../../../data/user_study/{f}') as f1:
        all_user_data += [json.loads(s) for s in f1.read().split('\n') if s!='']

applied_candidates_count = 0
total_candidates = 0
ratings = []
for telemetry in all_user_data:
    for candidate in telemetry["candidatesTelemetryData"]["candidates"]:
        total_candidates += 1
        if candidate["applied"]:
            applied_candidates_count+=1
        if 'userRating' in candidate:
            ratings.append(candidate['userRating'])
print(Counter(ratings))
print(len(ratings))
print(applied_candidates_count)