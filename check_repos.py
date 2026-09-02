import os
import glob

repos = ["InstantRequestRepository.kt", "OfferRepository.kt", "RequestRepository.kt", "UrgentRepositoryImpl.kt", "UrgentServiceRepository.kt"]

for r in repos:
    path = f"app/src/main/java/com/example/data/repositories/{r}"
    with open(path, "r") as f:
        print(f"==== {r} ====")
        for line in f.readlines():
            if "fun " in line and "{" in line:
                print(line.strip())
