import json
import pandas as pd

from extract_move_methods_from_rminer import get_class_name

def get_recalled_classes(responses__file="../../../data/moveMethodResponses(2).json",
                         api_responses_file="../../../data/apiResponses_selected.json",
                         move_method_data_file="../resources/data/move_methods_selected.csv",
                         iterations=5):
    with open(responses__file) as f:
        llm_responses = json.load(f)

    with open(api_responses_file) as f:
        api_responses = json.load(f)

    df = pd.read_csv(move_method_data_file)
    found_dest_class_count = 0
    found_method_count = 0
    found_method_ids = []
    for i, row in df.iterrows():
        id = row['ID']
        class_name = get_class_name(row['dest_class'])
        method_name = row['methodName']

        keys = [k for k in api_responses.keys() if k.startswith(f"ID_{id}-")][:iterations]
        found_dest_class_count += any([class_name in api_responses[k] for k in keys])
        found_method_count += any([method_name in api_responses[k] for k in keys])
        found_method_ids+=[id] if any([method_name in api_responses[k] for k in keys]) else []

        # print(f"dest_class={class_name}")
        # print(f"method_name={method_name}")
        # print([api_responses[k] for k in keys])
        # print("------")
    print(f"{found_dest_class_count=}")
    print(f"{found_method_count=}")
    print(f"{found_method_ids=}")

def main():
    # get_recalled_methods(iterations=5)
    # get_recalled_methods(iterations=1)
    #
    # get_recalled_classes(iterations=5)
    # get_recalled_classes(iterations=1)

    get_recalled_methods(
        responses__json="../../../data/moveMethodResponses(5).json",
        move_method_data="../resources/data/move_methods_all.csv", iterations=5)
    get_recalled_methods(
        responses__json="../../../data/moveMethodResponses(5).json",
        move_method_data="../resources/data/move_methods_all.csv", iterations=1)

    get_recalled_classes(
        responses__file="../../../data/moveMethodResponses(5).json",
        move_method_data_file="../resources/data/move_methods_all.csv",
        api_responses_file="../../../data/apiResponses(2).json",
        iterations=5)
    get_recalled_classes(
        responses__file="../../../data/moveMethodResponses(5).json",
        move_method_data_file="../resources/data/move_methods_all.csv",
        api_responses_file="../../../data/apiResponses(2).json",
        iterations=1)


def get_recalled_methods(responses__json="../../../data/moveMethodResponses(2).json",
                         move_method_data="../resources/data/move_methods_selected.csv",
                         iterations=5):
    with open(responses__json) as f:
        llm_responses = json.load(f)
    df = pd.read_csv(move_method_data)
    found_arr = []
    found_ids = []
    for ind, row in df.iterrows():
        method_name = row['methodName']
        found = False
        if ind in {0, 1, 4, 6, 7, 14}:
            print()

        if f'ID_{ind}' not in llm_responses:
            continue
        for resp in list(llm_responses[f'ID_{ind}'].keys())[:iterations]:
            found = found or method_name in llm_responses[f'ID_{ind}'][resp]
        # print(ind, found)
        found_arr.append(found)
        found_ids += [row['ID']] if found else []
    assert (len(found_arr) == len(llm_responses))
    print(f"Recalled methods names = {sum(found_arr)}")
    print(f"Recalled methods %= {sum(found_arr) / len(found_arr)}")
    print(f"{found_ids=}")


if __name__=='__main__':
    main()