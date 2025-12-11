[![INFORMS Journal on Computing Logo](https://INFORMSJoC.github.io/logos/INFORMS_Journal_on_Computing_Header.jpg)](https://pubsonline.informs.org/journal/ijoc)

# Exact Algorithms for Two-Dimensional Knapsack Problems: A Unified Framework with New Benchmark Results

[![IJOC](https://img.shields.io/badge/INFORMS-Journal%20on%20Computing-3F7EBC?link=https%3A%2F%2Fpubsonline.informs.org%2Fjournal%2Fijoc)](https://pubsonline.informs.org/journal/ijoc) [![DOI](https://img.shields.io/badge/DOI-10.1287%2Fijoc.2025.1423.cd-3F7EBC)](https://doi.org/10.1287/ijoc.2025.1423.cd) [![LICENSE](https://img.shields.io/badge/LICENSE-MIT-3F7EBC)](https://mit-license.org/) [![Java](https://img.shields.io/badge/Java-F37439)](https://www.oracle.com/java/technologies/downloads/) [![Maven](https://img.shields.io/badge/Maven-5EBFA2)](https://maven.apache.org/) [![CPLEX](https://img.shields.io/badge/CPLEX-6F1970)](https://www.ibm.com/docs/en)

This archive is distributed in association with the [INFORMS Journal on Computing](https://pubsonline.informs.org/journal/ijoc) under the [MIT License](LICENSE.txt).

The software and data in this repository are a snapshot of the software and data that were used in the research reported on in the paper [Exact Algorithms for Two-Dimensional Knapsack Problems: A Unified Framework with New Benchmark Results](https://doi.org/10.1287/ijoc.2025.1423) by Sunkanghong Wang, Roberto Baldacci, Fabio Furini, Lijun Wei, and Qiang Liu. This snapshot corresponds to the version used in the published paper.

**Important: This repository is being developed on an on-going basis at https://github.com/WSKH0929/EAs_For_2KPs. Please go there if you would like to get a more recent version or would like support**.

## 🏷️ Cite

To cite the contents of this repository, please cite both the paper and this repo, using their respective DOIs.

https://doi.org/10.1287/ijoc.2025.1423

https://doi.org/10.1287/ijoc.2025.1423.cd

Below is the BibTex for citing this snapshot of the repository.

```
@misc{wang2025exact_repo,
  author =        {Wang, Sunkanghong and Baldacci, Roberto and Furini, Fabio and Wei, Lijun and Liu, Qiang},
  publisher =     {INFORMS Journal on Computing},
  title =         {Exact Algorithms for Two-Dimensional Knapsack Problems: {A} Unified Framework with New Benchmark Results},
  year =          {2025},
  doi =           {10.1287/ijoc.2025.1423.cd},
  url =           {https://github.com/INFORMSJoC/2025.1423},
  note =          {Available for download at https://github.com/INFORMSJoC/2025.1423},
}
```

## 🔎 Overview

This repository was established to to facilitate future research and support our work in:

> **Sunkanghong Wang, Roberto Baldacci, Fabio Furini, Lijun Wei, and Qiang Liu (2025) Exact Algorithms for Two-Dimensional Knapsack Problems: A Unified Framework with New Benchmark Results. *INFORMS Journal on Computing*.**

In this work, we addressed the following strongly $\mathcal{NP}$-hard two-dimensional packing problems:

- Two-Dimensional Orthogonal Packing Problem (2OPP) and its special version, the Perfect Packing Problem (PPP);
- Two-Dimensional Knapsack Problem (2KP);
- 2KP with item conflicts (2KPC);
- Generalized 2KP (G2KP).

We designed both non-numerically exact and numerically exact algorithms (N-NEA and NEA) for solving these problems.

This comprehensive repository provides the complete source code of our algorithms, as well as all instance data, aggregated results, and detailed solutions involved in our work. The structure of the repository is shown below:

```
├─Code
│  └─EXA-G2KP
├─Instances
│  ├─2KP
│  ├─2KPC
│  ├─2OPP
│  └─G2KP
└─Results
    ├─2KP
    ├─2KPC
    ├─2OPP
    └─G2KP
```

> If you have any questions, please feel free to reach out to **[wskh0929@gmail.com](mailto:wskh0929@gmail.com)** or **[villagerwei@gdut.edu.cn](mailto:villagerwei@gdut.edu.cn)**.

## 💻 Code

In the **`Code/EXA-G2KP`** directory, you will find the source code of our algorithms.

The tree structure of the directory and the functions of the main subdirectories or files are as follows:

```shell
EXA-G2KP (project root directory)
│   .gitignore (lists files and patterns for Git to ignore)
│   pom.xml (Maven POM file for managing dependencies and build)
│
└── src (source code root directory)
    └── main (main source directory)
        └── java (Java source code directory)
            └── com (top-level package)
                └── wskh (main project package)
                    ├── classes (core classes package)
                    │       EarlyTerminationException.java (custom exception indicating early termination of an algorithm)
                    │       IntValue_Item.java (entity class representing an integer-valued item)
                    │       Item.java (base class defining common properties and methods for items)
                    │       LongValue_Item.java (entity class representing a long-valued item)
                    │       Parameter.java (class holding algorithm parameters)
                    │       PlaceItem.java (entity class representing a packed item)
                    │       SkyLine.java (entity class representing a skyline)
                    │       SR_Cut.java (entity class representing a subset-row cut)
                    │       UnionFind.java (union–find data structure implementation)
                    │
                    ├── run (runner package)
                    │       RunForSolving_2KP.java (runner for the 2KP)
                    │       RunForSolving_2KPC.java (runner for the 2KPC)
                    │       RunForSolving_2OPP.java (runner for the 2OPP)
                    │       RunForSolving_G2KP_Set1.java (runner for G2KP sets: CLA-1 and NEW-1)
                    │       RunForSolving_G2KP_Set2.java (runner for G2KP sets: CLA-2 and NEW-2)
                    │
                    ├── solvers (solvers package)
                    │   ├── GTKP (subpackage for G2KP solvers)
                    │   │       GTKP_LS_Heu_Solver.java (lable-setting algorithm for computing initial lower bounds)
                    │   │       GTKP_LS_UB_Solver.java (lable-setting algorithm for computing initial upper bounds)
                    │   │       GTKP_Safe_Solver.java (NEA)
                    │   │       GTKP_UnSafe_Solver.java (N-NEA)
                    │   │       GTKP_UnSafe_Solver_NoLBD.java (N-NEA without LBD)
                    │   │
                    │   ├── TKPC (subpackage for 2KP(C) solvers)
                    │   │       TKPC_LS_Heu_Solver.java (lable-setting algorithm for computing initial lower bounds)
                    │   │       TKPC_LS_UB_Solver.java (lable-setting algorithm for computing initial upper bounds)
                    │   │       TKPC_Safe_Solver.java (NEA)
                    │   │       TKPC_UnSafe_Solver.java (N-NEA)
                    │   │       TKPC_UnSafe_Solver_NoLBD.java (N-NEA without LBD)
                    │   │
                    │   └── TOPP (subpackage for 2OPP solvers)
                    │           PPP_Solver.java (improved PPP solver)
                    │           Solver.java (common solver interface definition)
                    │           TOPP_Safe_Solver.java (numerically exact 2OPP solver, i.e., branch-and-bound algorithm for LBD)
                    │           TOPP_UnSafe_Solver.java (non-numerically exact 2OPP solver, i.e., logic-based Benders decomposition for LBD)
                    │
                    └── utils (utilities package providing common helper classes and methods)
                            CheckUtil.java (utility for input and result validation)
                            CommonUtil.java (general-purpose helper methods)
                            DffUtil.java (utility for computing dual feasible functions)
                            FastOppUtil.java (utility for fast 2OPP check)
                            PointSetUtil.java (utility for computing MIM points)
                            SkyLineUtil.java (utilities for performing skyline-based heuristics)
                            TimeUtil.java (time-handling utilities)
                            WriteUtil.java (file writing utilities)
                            YCheckUtil.java (utility for solving BSPs)
```

It is worth noting that we implemented two versions of N-NEA and NEA to solve the G2KP and 2KP(C), respectively. The execution logic of the two versions is almost identical, except:

- The versions used to solve G2KP use the entity class (``LongValue_Item.java``) for long-valued items, while the others use the entity class (``IntValue_Item.java``) for integer-valued items. The reason for this difference is that 32-bit integers are sufficient for all 2KP(C) benchmark instances; however, to handle G2KP instances, 64-bit integers must be used.
- The versions used to solve 2KP(C) do not contain code to handle *subset-row* (SR) cuts and *same-bin* constraints.

### 🔧 Set up

We compiled and ran the code using the following software:

- IntelliJ IDEA 2024.1.3 (Compiler)
- GraalVM JDK 23.0.1
- Apache MAVEN 3.99

Additionally, we installed the jar package of CPLEX into MAVEN so that we can use it directly by introducing the following dependency:

```xml
<dependency>
    <groupId>cplex</groupId>
    <artifactId>cplex</artifactId>
    <version>12.6.3</version>
</dependency>
```

The command used is as follows:

```shell
mvn install:install-file -Dfile=D:\WSKH\Environment\Cplex\Cplex_Library_And_Bin\Cplex1263\lib\cplex.jar -DgroupId=cplex -DartifactId=cplex -Dversion=12.6.3 -Dpackaging=jar
```

where

- Users need to replace **`D:\WSKH\Environment\Cplex\Cplex_Library_And_Bin\Cplex1263\lib\cplex.jar`** with the file path of their own jar package of CPLEX.

Moreover, we used the following Virtual Machine Options to compile and run the proposed algorithm:

```shell
-Djava.library.path=D:\WSKH\Environment\Cplex\Cplex_Library_And_Bin\Cplex1263\bin\x64_win64
```

where

-  **`-Djava.library.path=D:\WSKH\Environment\Cplex\Cplex_Library_And_Bin\Cplex1263\bin\x64_win64`** specifies the path to the native libraries for CPLEX, allowing JAVA to locate them for use.

### 🚀 Run

Users can run our algorithms through the runners in **`src/main/java/com/wskh/run`**.

## 🗄️ Instances

The **`Instances`** directory contains all the 2KP, 2KPC, 2OPP, and G2KP instances we used in our experiments.

The format of each 2KP instance file is as follows:

```
m
W H
i w_i h_i n_i p_i (for each i=1,...,m)

where:
m: number of items
W: width of the bin
H: height of the bin
i: item ID (starting from 1)
w_i: width of item i
h_i: height of item i
n_i: number of copies of item i
p_i: profit of item i
```

The format of each 2KPC instance file is as follows:

```
Number of items
Number of conflicts
Size of the rectangular bin W x H

1st item w x h and value (if 0 means equal to item's area)
2nd item. . . and so on

1st pair of items conflicting each other
2nd pair of items . . . and so on

Original 2KP instance name 
Other file information
```

The format of each 2OPP instance file is as follows:

```
m
W H
i w_i h_i d_i b_i p_i (for each i=1,...,m)

where:
m: number of items
W: width of the bin
H: height of the bin
i: item ID (starting from 1)
w_i: width of item i
h_i: height of item i
d_i: demand of item i (minimum number of copies to be packed)
b_i: maximum number of copies of item i
p_i: profit of item i
```

The format of each G2KP instance file is as follows:

```
n
W H
i w_i h_i p_i (for each i=1,...,n)
j^k (for each {j,k} in E_d)
j&k (for each {j,k} in E_s)
j-k-l-e (for each item triplet {j,k,l} with penalty of e in T)

where:
n: number of items
W: width of the bin
H: height of the bin
i: item ID (starting from 1)
w_i: width of item i
h_i: height of item i
p_i: profit of item i
E_d: edge set of the different-bin graph
E_s: edge set of the same-bin graph
T: set of item triplets
```

## 📊 Results

The **`Results`** directory contains results obtained by the proposed algorithms as well as results of other versions of our algorithms.

We provide ``.csv`` or ``.xlsx`` files for the aggregated results for each algorithm and each problem, where the main columns have the following meanings:

- **(W, H)**: bin size
- **n**: number of items
- **|I_in|**: number of items that must be packed
- **|I_out|**: number of items that cannot be packed
- **UB0-KP**: initial upper bound computed by the enhanced knapsack function
- **UB0-LS**: initial upper bound computed by the label-setting algorithm
- **UB0**: min(UB0-KP, UB0-LS)
- **LB0**: initial lower bound computed by the label-setting algorithm
- **UB**: best upper bound proven by our algorithm  (N-NEA or NEA)
- **LB**: best lower bound found by our algorithm  (N-NEA or NEA)
- **Gap0**: (UB0-LB0)/UB0
- **Gap**: (UB-LB)/UB
- **Opt0**: if UB0=LB0, this value is 1; otherwise, it is 0
- **Opt**: if UB=LB, this value is 1; otherwise, it is 0
- **Nodes'**: number of explored nodes
- **Nodes**: number of generated nodes
- **#OPP**: number of times 2OPP check (LBD) is executed
- **#EOPP**: number of times exact 2OPP check (exact BB of LBD) is executed
- **Time-OPP**: time to perform 2OPP check (all times are in seconds)
- **Time-EOPP**: time to perform exact 2OPP check
- **Time-UB0-KP**: time to compute UB0-KP
- **Time-UB0-LS**: time to compute UB0-LS
- **Time-LB0**: time to compute LB0
- **Time-Red**: time to perform reductions
- **Time**: total computation time

In particular, we provide the solution of each instance computed by our algorithms and its visualization, such as the solution visualization of GCUT13 below:

![GCUT13](https://picgo-wskh.oss-cn-guangzhou.aliyuncs.com/202511201029166.png)

where the white rectangles represent the packed items, the gray area represents the unused area in the bin, and the numbers represent the index of the packed items (starting from 1).

Details of the solution to an instance can be found in the ``.sol`` file, which has the following format:

```
b
n
i x_i y_i

where:
b: number of bin used (for any 2KP(C), 2OPP and G2KP solution, b is always equal to 1)
n: number of items
i: index of a packed item (starting from 1)
(x_i, y_i): coordinates of the bottom-left corner of the packed item i
```

### Detailed Comparisons with SOTA Methods of 2KP, 2KPC, and 2OPP

| Problem | Short name - Reference                                       | Numerically exact |
| :------ | :----------------------------------------------------------- | ----------------- |
| 2KP     | CM04 - [Caprara and Monaci (2004)](https://www.sciencedirect.com/science/article/abs/pii/S0167637703000579) | ✅                 |
| 2KP     | FSV07 - [Fekete et al. (2007)](https://pubsonline.informs.org/doi/10.1287/opre.1060.0369) | ✅                 |
| 2KP     | BB07 - [Baldacci and Boschetti (2007)](https://www.sciencedirect.com/science/article/abs/pii/S0377221706002943) | ❌                 |
| 2KP     | CLQ20 - [de Almeida Cunha et al. (2020)](https://link.springer.com/article/10.1007/s10288-019-00419-9) | ❌                 |
| 2KPC    | QHSM17 - [de Queiroz et al. (2017)](https://www.sciencedirect.com/science/article/abs/pii/S0360835217300347) | ❌                 |
| 2OPP    | KINYN09 - [Kenmochi et al. (2009)](https://doi.org/10.1016/j.ejor.2008.08.020) | ✅                 |
| 2OPP    | MSB12 - [Mesyagutov et al. (2012)](https://doi.org/10.1016/j.cor.2011.12.010) | ❌                 |
| 2OPP    | BR13 - [Belov and Rohling (2013)](https://doi.org/10.1287/opre.1120.1150) | ❌                 |
| 2OPP    | CI18-BB - [Côté and Iori (2018)](https://doi.org/10.1287/ijoc.2018.0806) | ✅                 |
| 2OPP    | CI18-BD - [Côté and Iori (2018)](https://doi.org/10.1287/ijoc.2018.0806) | ❌                 |

Our algorithms (N-NEA and NEA) were implemented in JAVA and executed on a Windows 11 Professional operating system equipped with an Intel Core i9-12900H processor (2.5 GHz) and 32 GB of RAM. The processor achieves a single-thread PassMark score of 3,787 (https://www.cpubenchmark.net, 2024).

In the tables presented below, the marks ``T.L.`` and ``?`` are used to indicate, respectively, instances in which the optimal solution could not be found within the time limit and cases where data are missing due to either insufficient testing or unreported results.

#### Comparison with the State-of-the-Art 2KP algorithms

We compare our algorithms with seven state-of-the-art 2KP algorithms: four versions ($A_0$, $A_1$, $A_2$, and $A_3$) of CM04, FSV07, BB07, and CLQ20. The experimental environments and time limits for these algorithms are as follows:

1. CM04- $A_0$, $A_1$, $A_2$, and $A_3$ were tested on an Intel Pentium III 800 MHz processor (single thread PassMark score: 200) with a time limit of 1800 seconds;
2. FSV07 was tested on an Intel Pentium IV 2.8 GHz processor (single thread PassMark score: 509) with a time limit of 1800 seconds;
3. BB07 was tested on an Intel Pentium IV 2.5 GHz processor (single thread PassMark score: 385) with a time limit of 36000 seconds;
4. CLQ20, without limiting the number of threads, was tested on an Intel Xeon X3430 2.4 GHz processor (multi-thread PassMark score: 2293) with a time limit of 3600 seconds.

Consistent with the most recent two-level based algorithm, BB07, our algorithms were tested with a time limit of 3660 seconds ($36000 \times 385 / 3787 \approx 3660$).

The results are summarized in **Table 1**, where the ``Time'`` column represents the scaled computation time, adjusted based on the ratio between the PassMark score of the CPU running each algorithm and that of CM04. Except for GCUT13, our algorithms solve all other instances optimally. Notably, GCUT13 remains particularly challenging, and, to date, no algorithm has solved it optimally. However, our N-NEA presents a better solution with a value of 8,661,108, surpassing the best known value of 8,622,498 reported by [Fekete et al. (2007)](https://pubsonline.informs.org/doi/10.1287/opre.1060.0369). Furthermore, for the large-scale instances used in 2KP by [de Almeida Cunha et al. (2020)](https://link.springer.com/article/10.1007/s10288-019-00419-9), our NEA solves 15 more instances than their method and achieves significantly faster computation times on the remaining instances.

![image-20250610092314493](https://picgo-wskh.oss-cn-guangzhou.aliyuncs.com/image-20250610092314493.png)

Since our numerically exact implementation can be regarded as an improvement over CM04- $A_3$ ([Caprara and Monaci (2004)](https://www.sciencedirect.com/science/article/abs/pii/S0167637703000579)), **Table 2** compares CM04- $A_3$ with our NEA. The columns ``#EOPP`` and ``Time_{EOPP}`` report, respectively, the number of times the 2OPP exact algorithm is called and the corresponding computation time. In almost all instances, our NEA outperforms CM04- $A_3$, achieving significantly shorter computation times and fewer 2OPP exact algorithm calls.

![image-20250610092612698](https://picgo-wskh.oss-cn-guangzhou.aliyuncs.com/image-20250610092612698.png)

#### Comparison with the State-of-the-Art 2KPC algorithms

We compare our algorithms with the F3 approach (QHSM17) in [de Queiroz et al. (2017)](https://www.sciencedirect.com/science/article/abs/pii/S0360835217300347), which imposed no limit on the number of threads and was tested on an Intel Core i7-2600 3.4 GHz processor (multi-thread PassMark score: 5348) with a time limit of 3600 seconds. Following QHSM17, our N-NEA and NEA were tested with a time limit of 5084 seconds ($3600 \times 5348 / 3787 \approx 5084$).

**Tables 3–5** present the results for instances with densities of 10%, 17%, and 25%. The ``Time'`` column reports the scaled computation time, adjusted by the ratio of the PassMark score of the CPU used for our approach to that of QHSM17. The average computation time for all algorithms decreases as density increases, suggesting that higher-density instances are easier to solve. Overall, our approach, particularly NEA, outperforms QHSM17. We obtained an optimal value of 5,452 for the UW11 instance with $|\mathcal{E_{\text{d}}}| = 73$ (highlighted in red in **Table 4**, whereas [de Queiroz et al. (2017)](https://www.sciencedirect.com/science/article/abs/pii/S0360835217300347) reported a feasible value of 5,717. After thoroughly reviewing our code, we found no issues. Because the results and code of [de Queiroz et al. (2017)](https://www.sciencedirect.com/science/article/abs/pii/S0360835217300347) are unavailable, reproducing their solution of 5,717 is impossible.

![image-20250610093332670](https://picgo-wskh.oss-cn-guangzhou.aliyuncs.com/image-20250610093332670.png)

![image-20250610093354047](https://picgo-wskh.oss-cn-guangzhou.aliyuncs.com/image-20250610093354047.png)

![image-20250610093418102](https://picgo-wskh.oss-cn-guangzhou.aliyuncs.com/image-20250610093418102.png)

#### Comparison with the State-of-the-Art 2OPP algorithms

We compare our approach with four state-of-the-art 2OPP algorithms: MSB12, BR13, CI18-BB, and CI18-BD. The experimental environments and time limits for these algorithms are as follows:

1. MSB12 was tested on an AMD Athlon 64 Dual Core 4200+ at 2.2 GHz (single-thread PassMark score: 804) with a time limit of 900 seconds;
2. BR13 was tested on an Intel Xeon E5430 at 2.66 GHz (single-thread PassMark score: 1159) with a time limit of 3600 seconds;
3. CI18-BB and CI18-BD were tested on an Intel Westmere EP X5650 at 2.667 GHz (single-thread PassMark score: 1294) with a time limit of 900 seconds.

Following the latest algorithms (CI18-BB and CI18-BD), our algorithms were tested with a time limit of 307 seconds ($900 \times 1294 / 3787 \approx 307$).

**Table 6** compares the above four leading algorithms and our algorithms on the benchmark sets C, N, T, CJCM, MSB-450, and MSB-630. The ``Time'`` column reports the scaled average computation time, obtained by multiplying the raw time by the ratio between the PassMark score of the CPU used for each algorithm and that of MSB12. Our N-NEA performs well on nearly all sets, producing results that are comparable to or better than those of previous algorithms. Our NEA also shows excellent performance, slightly outperforming N-NEA on the CJCM set. MSB12 excels on the MSB-630 set, which features large bin sizes, because it iteratively fixes the mutual positions of pairs of items. Unlike CI18-BB’s coordinate interval division and the coordinate variable assignment used by CI18-BD and our algorithms, this strategy is particularly effective when bins are large and the number of items is small.

![image-20250610093926577](https://picgo-wskh.oss-cn-guangzhou.aliyuncs.com/image-20250610093926577.png)

By incorporating an improved PPP algorithm, our 2OPP algorithms demonstrate remarkable performance on the *waste-free* instances in sets C, N, and T. To evaluate the effectiveness of our improved PPP algorithm, **Table 7** compares our PPP algorithm with the state-of-the-art exact PPP algorithm (KINYN09), which was tested on a Pentium IV 3.0 GHz processor (single-thread PassMark score: 522) with a 3600-second time limit. Our PPP algorithm demonstrates shorter computation times on most instances, particularly on the most challenging ones (N4a, N4b, N4c, N4d, and N4e). Notably, our PPP algorithm solves instance N4c (for the first time), which KINYN09 fails to solve.

![image-20250610094009625](https://picgo-wskh.oss-cn-guangzhou.aliyuncs.com/image-20250610094009625.png)

## 📚 Reference

##### Baldacci R, Boschetti MA (2007) A cutting-plane approach for the two-dimensional orthogonal non-guillotine cutting problem. *European Journal of Operational Research* 183(3):1136–1149.

##### Belov G, Rohling H (2013) LP bounds in an interval-graph algorithm for orthogonal-packing feasibility. *Operations Research* 61(2):483–497.

##### Caprara A, Monaci M (2004) On the two-dimensional knapsack problem. *Operations Research Letters* 32(1):5–14.

##### Côté JF, Iori M (2018) The meet-in-the-middle principle for cutting and packing problems. *INFORMS Journal on Computing* 30(4):646–661.

##### de Queiroz TA, Hokama PHDB, Schouery RCS, Miyazawa FK (2017) Two-dimensional disjunctively constrained knapsack problem: Heuristic and exact approaches. *Computers & Industrial Engineering* 105:313–328.

##### de Almeida Cunha JG, De Lima VL, De Queiroz TA (2020) Grids for cutting and packing problems: A study in the 2D knapsack problem. *4OR* 18:293–339.

##### Fekete SP, Schepers J, Van der Veen JC (2007) An exact algorithm for higher-dimensional orthogonal packing. *Operations Research* 55(3):569–587.

##### Kenmochi M, Imamichi T, Nonobe K, Yagiura M, Nagamochi H (2009) Exact algorithms for the two-dimensional strip packing problem with and without rotations. *European Journal of Operational Research* 198(1):73–83.

##### Mesyagutov M, Scheithauer G, Belov G (2012) LP bounds in various constraint programming approaches for orthogonal packing. *Computers & Operations Research* 39(10):2425–2438.
