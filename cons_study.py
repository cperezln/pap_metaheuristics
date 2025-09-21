import os

if __name__ == "__main__":
    cons_name = "graspConstructiveRefinements"
    dir = f"/home/cristian/Escritorio/TFM/pap_metaheuristics/solutionsFinal/{cons_name}"
    sota_dir = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/solutions"
    number_best = 0
    number_equals = 0
    mean_cons = 0
    mean_sota = 0
    mean_std = 0
    n_files = len(os.listdir(dir))
    for file in os.listdir(dir):
        name = file
        cons = open(f"{dir}/{file}")
        file_sota = f"{name.split(".")[0]}.sol"
        read_sota = open(f"{sota_dir}/{file_sota}")
        z_sota = int(read_sota.readlines()[0])
        z_cons = float(cons.readlines()[1])
        if z_sota > z_cons:
            number_best += 1
        if z_sota == z_cons:
            number_equals += 1
        mean_cons += z_cons
        mean_sota += z_sota
        mean_std += (z_cons - min(z_cons, z_sota)) / min(z_cons, z_sota)

    print(f"Mean STD {100*mean_std/n_files}% \t Mean SOTA {mean_sota/n_files} \t Mean CONS {mean_cons/n_files} \t N. BEST {number_best} \t N. Eq {number_equals}")