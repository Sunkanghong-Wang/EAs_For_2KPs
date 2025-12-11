package com.wskh.run;

import com.wskh.classes.IntValue_Item;
import com.wskh.classes.PlaceItem;
import com.wskh.solvers.TKPC.TKPC_Safe_Solver;
import com.wskh.utils.CommonUtil;
import com.wskh.utils.TimeUtil;
import com.wskh.utils.WriteUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class RunForSolving_2KPC {

    static void test(FileOutputStream csv, String dirPath, String instanceType) throws Exception {

        String localResultDir = resultDir + "/" + instanceType + "/";
        String imgDir = localResultDir + "img/";
        String solutionDir = localResultDir + "solution/";
        if (csv != null) {
            new File(localResultDir).mkdirs();
            new File(imgDir).mkdirs();
            new File(solutionDir).mkdirs();
        }

        File dir = new File(dirPath);
        File[] files = Objects.requireNonNull(dir.listFiles());
        Arrays.sort(files, Comparator.comparingInt((File o) -> o.getName().length()).thenComparing(File::getName));

        for (File file : files) {
            if (file.getName().endsWith(".txt")) {

//                if (!file.getName().equals("uw10_2.txt")) continue;
//                if (!file.getName().equals("gcut8_2.txt")) continue;
//                if (!file.getName().equals("uw11_2.txt")) continue;
//                if (!file.getName().equals("m1_1.txt")) continue;
//                if (!file.getName().equals("mw3_2.txt")) continue;
//                if (!file.getName().equals("m4_1.txt")) continue;
//                if (file.getName().equals("gcut13_1.txt")) continue;

                System.gc();

                if (csv != null) {
                    System.out.println(file.getName() + " => " + new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss").format(new Date()));
                }

                int W = 0, H = 0;
                double density = 0;
                List<IntValue_Item> itemList = new ArrayList<>();
                List<int[]> conflictList = new ArrayList<>();

                BufferedReader bufferedReader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String input;
                int row = 0;
                int n = 0;
                int conflictNum = 0;
                while ((input = bufferedReader.readLine()) != null) {
                    if (row == 0) {
                        n = Integer.parseInt(input);
                        itemList = new ArrayList<>(n);
                    } else if (row == 1) {
                        conflictNum = Integer.parseInt(input);
                        conflictList = new ArrayList<>(conflictNum);
                    } else if (row == 2) {
                        String[] split = input.trim().split(" ");
                        W = Integer.parseInt(split[0]);
                        H = Integer.parseInt(split[1]);
                    } else if (row > 3 && row <= 3 + n) {
                        // item
                        String[] split = input.trim().split(" ");
                        int w = Integer.parseInt(split[0]);
                        int h = Integer.parseInt(split[1]);
                        int v = Integer.parseInt(split[2]);
                        int value = v == 0 ? w * h : v;
                        itemList.add(new IntValue_Item(itemList.size(), 0, w, h, w * h, value, value / (double) (w * h)));
                    } else if (row > 3 + n + 1 && row <= 3 + n + conflictNum + 1) {
                        String[] split = input.trim().split(" ");
                        int i = Integer.parseInt(split[0]) - 1;
                        int j = Integer.parseInt(split[1]) - 1;
                        if (i < j) {
                            conflictList.add(new int[]{i, j});
                        } else {
                            conflictList.add(new int[]{j, i});
                        }
                    } else {
                        if (input.contains("density")) {
                            density = Double.parseDouble(input.replace("density:", ""));
                        }
                    }
                    row++;
                }
                bufferedReader.close();
                for (int i = 0; i < itemList.size(); i++) itemList.get(i).index = i;

                Random random = new Random(929L);

                TKPC_Safe_Solver solver = new TKPC_Safe_Solver(random);
//                TKPC_UnSafe_Solver solver = new TKPC_UnSafe_Solver(random);
//                TKPC_UnSafe_Solver_NoLBD solver = new TKPC_UnSafe_Solver_NoLBD(random);

                TimeUtil.startTime = System.currentTimeMillis();
                solver.solve(n, W, H, IntValue_Item.copy(itemList.toArray(new IntValue_Item[0])), conflictList.stream().map(int[]::clone).toList());
                long time = TimeUtil.getCurTime();

                // 还原尺寸
                for (PlaceItem placeItem : solver.bestPlaceItemList) {
                    for (IntValue_Item item : itemList) {
                        if (item.id == placeItem.id) {
                            placeItem.w = item.w;
                            placeItem.h = item.h;
                            placeItem.s = item.s;
                            placeItem.index = item.index;
                            break;
                        }
                    }
                }

                // 检查冲突
                boolean[] used = new boolean[n];
                for (PlaceItem placeItem : solver.bestPlaceItemList) {
                    used[placeItem.index] = true;
                }
                for (int[] conflict : conflictList) {
                    if (used[conflict[0]] && used[conflict[1]]) {
                        System.out.println(Arrays.toString(conflict) + " " + itemList.get(conflict[0]).id + " " + itemList.get(conflict[1]).id);
                        throw new RuntimeException();
                    }
                }

                if (csv != null) {
                    Object[] objects = new Object[]{
                            instanceType, file.getName().replace(".txt", ""), W, H, itemList.size(), density, conflictList.size(),
                            solver.I_in, solver.I_out,
                            solver.UB0_KP, solver.UB0_LS, solver.UB0, solver.LB0, solver.UB, solver.LB,
                            (solver.UB0 - solver.LB0) / (double) solver.UB0, (solver.UB - solver.LB) / (double) solver.UB,
                            (solver.UB0 == solver.LB0 ? 1 : 0), (solver.UB == solver.LB ? 1 : 0),
                            solver.exploredNodes, solver.generatedNodes,
                            solver.oppCnt, solver.exactOppCnt, solver.oppTime / 1000d, solver.exactOppTime / 1000d,
                            solver.ub0kpTime / 1000d, solver.ub0lsTime / 1000d, solver.lb0Time / 1000d, solver.redTime / 1000d, time / 1000d,
                    };
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < objects.length; i++) {
                        stringBuilder.append(objects[i]);
                        if (i == objects.length - 1) {
                            stringBuilder.append("\n");
                        } else {
                            stringBuilder.append(",");
                        }
                    }
                    csv.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));

                    WriteUtil.writeSolution(List.of(solver.bestPlaceItemList), solutionDir + file.getName().replace(".txt", ".2kpc.sol"));
                    WriteUtil.writePatternPlotToPng(W, H, solver.bestPlaceItemList, imgDir + file.getName().replace(".txt", ".png"));
                    System.out.println("LB0: " + solver.LB0 + " , UB0: " + solver.UB0 + " , LB: " + solver.LB + " , isOpt: " + (solver.UB == solver.LB ? 1 : 0) + " , Time: " + ((time / 1000d)));
                    System.out.println("------------------------------------");
                }

            }
        }

    }

    static String resultDir;

    public static void main(String[] args) throws Exception {
        double r = 3787d / 5348d;
        TimeUtil.TimeLimit = CommonUtil.ceilToInt(3600d / r * 1000);
        resultDir = "./res/2KPC";
        new File(resultDir).mkdirs();

        // 预热
//        for (int i = 0; i < 60; i++) {
//            test(null, "../../Instances/2KPC/CGCUT", "CGCUT");
//            test(null, "../../Instances/2KPC/MW", "MW");
//        }

        FileOutputStream csv = new FileOutputStream(resultDir + "/Res-2KPC.csv");

        csv.write(("Set,Instance,W,H,n,density,|E_d|," +
                "|I_in|,|I_out|," +
                "UB0-KP,UB0-LS,UB0,LB0,UB,LB," +
                "Gap0,Gap,Opt0,Opt," +
                "Nodes',Nodes," +
                "#OPP,#EOPP,Time-OPP,Time-EOPP," +
                "Time-UB0-KP,Time-UB0-LS,Time-LB0,Time-Red,Time\n").getBytes(StandardCharsets.UTF_8));

        test(csv, "../../Instances/2KPC/CGCUT", "CGCUT");
        test(csv, "../../Instances/2KPC/GCUT", "GCUT");
        test(csv, "../../Instances/2KPC/M", "M");
        test(csv, "../../Instances/2KPC/MW", "MW");
        test(csv, "../../Instances/2KPC/NGCUT", "NGCUT");
        test(csv, "../../Instances/2KPC/OKP", "OKP");
        test(csv, "../../Instances/2KPC/UW", "UW");

        csv.close();
    }
}