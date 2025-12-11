package com.wskh.run;

import com.wskh.classes.*;
import com.wskh.solvers.GTKP.GTKP_Safe_Solver;
import com.wskh.solvers.GTKP.GTKP_UnSafe_Solver;
import com.wskh.utils.CheckUtil;
import com.wskh.utils.TimeUtil;
import com.wskh.utils.WriteUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RunForSolving_G2KP_Set1 {
    static void test(FileOutputStream csv, String dirPath) throws Exception {

        String localResultDir = resultDir + "/";
        String imgDir = localResultDir + "img/";
        String solutionDir = localResultDir + "solution/";
        if (csv != null) {
            new File(localResultDir).mkdirs();
            new File(imgDir).mkdirs();
            new File(solutionDir).mkdirs();
        }

        File dir = new File(dirPath);
        File[] files = Objects.requireNonNull(dir.listFiles());
        Arrays.sort(files, (o1, o2) -> {
            String fileName1 = o1.getName().replace(".ins2D", "");
            String fileName2 = o2.getName().replace(".ins2D", "");
            String[] fileNameSplit1 = fileName1.split("_");
            String[] fileNameSplit2 = fileName2.split("_");
            int c = (fileNameSplit1[0] + fileNameSplit1[1]).compareTo(fileNameSplit2[0] + fileNameSplit2[1]);
            if (c == 0) c = Integer.compare(Integer.parseInt(fileNameSplit1[3]), Integer.parseInt(fileNameSplit2[3]));
            if (c == 0) c = Integer.compare(Integer.parseInt(fileNameSplit1[6]), Integer.parseInt(fileNameSplit2[6]));
            return c;
        });

        int fileCount = 0;

        for (File file : files) {
            if (file.getName().endsWith(".ins2D")) {

                fileCount++;

                if (new File(solutionDir + file.getName().replace(".ins2D", ".g2kp.sol")).exists()) {
                    continue;
                }

                System.gc();

//                if (!file.getName().equals("Class_06_080_40.ins2D")) continue;

                String fileName = file.getName().replace(".ins2D", "");
                String[] fileNameSplit = fileName.split("_");
                String className = fileNameSplit[0] + fileNameSplit[1];
                int id = Integer.parseInt(fileNameSplit[3]);

                int W = 0, H = 0, n = 0;
                List<LongValue_Item> itemList = new ArrayList<>();
                List<int[]> conflictList = new ArrayList<>();
                List<int[]> bindList = new ArrayList<>();
                List<SR_Cut> srCutList = new ArrayList<>();

                BufferedReader bufferedReader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String input;
                int row = 0;
                while ((input = bufferedReader.readLine()) != null) {
                    if (row == 0) {
                        n = Integer.parseInt(input);
                    } else if (row == 1) {
                        String[] split = input.split(" ");
                        W = Integer.parseInt(split[0]);
                        H = Integer.parseInt(split[1]);
                    } else {
                        if (input.contains(" ")) {
                            // 物品
                            String[] split = input.split(" ");
                            int w = Integer.parseInt(split[1]);
                            int h = Integer.parseInt(split[2]);
                            int s = w * h;
                            long v = Long.parseLong(split[3]);
                            itemList.add(new LongValue_Item(itemList.size(), itemList.size(), w, h, s, v, (double) v / s));
                        } else if (input.contains("^")) {
                            // 冲突
                            String[] split = input.split("\\^");
                            conflictList.add(new int[]{Integer.parseInt(split[0]) - 1, Integer.parseInt(split[1]) - 1});
                        } else if (input.contains("&")) {
                            // same-bin
                            String[] split = input.split("&");
                            bindList.add(new int[]{Integer.parseInt(split[0]) - 1, Integer.parseInt(split[1]) - 1});
                        } else if (input.contains("-")) {
                            // SR cuts
                            String[] split = input.split("-");
                            srCutList.add(new SR_Cut(new int[]{Integer.parseInt(split[0]) - 1, Integer.parseInt(split[1]) - 1, Integer.parseInt(split[2]) - 1}, Long.parseLong(split[3])));
                        } else {
                            throw new RuntimeException();
                        }
                    }
                    row++;
                }
                bufferedReader.close();

                if (csv != null) {
                    System.out.println((fileCount) + " => " + file.getName() + " => |E_c|: " + conflictList.size() + " , |E_s|: " + bindList.size() + " , |SR|: " + srCutList.size() + " , " + new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss").format(new Date()));
                }

                UnionFind uf = new UnionFind(n);
                for (int[] bind : bindList) {
                    uf.union(bind[0], bind[1]);
                }
                int[] fat = uf.fat.clone();
                for (int i = 0; i < n; i++) fat[i] = uf.find(i);

                LongValue_Item[] items = new LongValue_Item[n];
                for (int i = 0; i < itemList.size(); i++) items[i] = itemList.get(i).copy();

                List<int[]> copy_conflictList = conflictList.stream().map(int[]::clone).collect(Collectors.toList());
                List<int[]> copy_bindList = bindList.stream().map(int[]::clone).collect(Collectors.toList());
                List<SR_Cut> copy_srCutList = srCutList.stream().map(SR_Cut::copy).collect(Collectors.toList());

                GTKP_Safe_Solver solver = new GTKP_Safe_Solver(new Random(929L));
//                GTKP_UnSafe_Solver solver = new GTKP_UnSafe_Solver(new Random(929L));
//                GTKP_UnSafe_Solver_NoLBD solver = new GTKP_UnSafe_Solver_NoLBD(new Random(929L));

                TimeUtil.startTime = System.currentTimeMillis();
                solver.solve(n, W, H, items, fat, copy_conflictList, copy_bindList, copy_srCutList);
                long time = TimeUtil.getCurTime();

                // 还原尺寸
                for (PlaceItem placeItem : solver.bestPlaceItemList) {
                    for (Item item : itemList) {
                        if (item.id == placeItem.id) {
                            placeItem.w = item.w;
                            placeItem.h = item.h;
                            placeItem.s = item.s;
                            placeItem.index = item.index;
                            break;
                        }
                    }
                }

                boolean violateDif = false;
                boolean violateSame = false;
                boolean valueError = false;

                // 检查没有出现重复的物品
                HashSet<Integer> set = new HashSet<>();
                for (PlaceItem placeItem : solver.bestPlaceItemList) {
                    if (!set.add(placeItem.id)) throw new RuntimeException();
                }

                // 检查冲突
                boolean[] used = new boolean[n];
                for (PlaceItem placeItem : solver.bestPlaceItemList) used[placeItem.index] = true;
                for (int[] conflict : conflictList) {
                    if (used[conflict[0]] && used[conflict[1]]) {
                        System.out.println(Arrays.toString(conflict) + " " + itemList.get(conflict[0]).id + " " + itemList.get(conflict[1]).id);
//                        throw new RuntimeException();
                        violateDif = true;
                        System.out.println("Violate conflict: " + Arrays.toString(conflict));
                    }
                }

                // 检查绑定
                for (int[] bind : bindList) {
                    if (used[bind[0]] != used[bind[1]]) {
                        System.out.println(Arrays.toString(bind) + " " + itemList.get(bind[0]).id + " " + itemList.get(bind[1]).id);
//                        throw new RuntimeException();
                        violateSame = true;
                        System.out.println("Violate bind: " + Arrays.toString(bind));
                    }
                }

                // 检查价值对不对
                long objValue = 0;
                for (PlaceItem placeItem : solver.bestPlaceItemList) {
                    objValue += itemList.get(placeItem.index).value;
                }
                for (SR_Cut sr : srCutList) {
                    int cnt = 0;
                    for (int index : sr.indexs) if (used[index]) cnt++;
                    if (cnt >= 2) objValue -= sr.penalty;
                }
                if (objValue != solver.LB) {
//                    throw new RuntimeException(objValue + " != " + solver.LB);
                    valueError = true;
                    System.out.println("Obj value error: " + objValue + " != " + solver.LB);
                }

                // 检查是否重叠或超出容易
                CheckUtil.checkOverlapAndOutBin(W, H, solver.bestPlaceItemList);

                if (solver.UB < solver.LB) {
                    System.out.println(solver.UB + " < " + solver.LB);
                    System.out.println(1 / 0);
                }
                if (solver.UB0 < solver.LB) {
                    System.out.println(solver.UB0 + " < " + solver.LB);
                    System.out.println(1 / 0);
                }

                if (csv != null) {

                    Object[] objects = new Object[]{
                            className, id, W, H, n, conflictList.size(), bindList.size(), srCutList.size(),
                            solver.Ed_Pie, solver.Es_Pie, solver.SR_Pie, solver.I_in, solver.I_out,
                            solver.UB0_KP, solver.UB0_LS, solver.UB0, solver.LB0, solver.UB, solver.LB,
                            (solver.UB0 - solver.LB0) / (double) solver.UB0, (solver.UB - solver.LB) / (double) solver.UB,
                            (solver.UB0 == solver.LB0 ? 1 : 0), (solver.UB == solver.LB ? 1 : 0),
                            violateDif ? 1 : 0, violateSame ? 1 : 0, valueError ? 1 : 0,
                            solver.exploredNodes, solver.generatedNodes,
                            solver.oppCnt, solver.exactOppCnt, solver.oppTime / 1000d, solver.exactOppTime / 1000d,
                            solver.ub0kpTime / 1000d, solver.ub0lsTime / 1000d, solver.lb0Time / 1000d, solver.redTime / 1000d, time / 1000d,
//                            solver.change ? 1 : 0,
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

//                    if (!solver.change) continue;

                    WriteUtil.writeSolution(List.of(solver.bestPlaceItemList), solutionDir + file.getName().replace(".ins2D", ".g2kp.sol"));
                    WriteUtil.writePatternPlotToPng(W, H, solver.bestPlaceItemList, imgDir + file.getName().replace(".ins2D", ".png"));
                    System.out.println("LB0: " + solver.LB0 + " , UB0: " + solver.UB0 + " , LB: " + solver.LB + " , isOpt: " + (solver.UB == solver.LB ? 1 : 0) + " , Time: " + ((time / 1000d)));
                    System.out.println("------------------------------------");
                } else {
                    return;
                }

            }
        }

    }

    static String resultDir;

    public static void main(String[] args) throws Exception {
        TimeUtil.TimeLimit = 1800 * 1000L;
//        TimeUtil.TimeLimit = 1000L;
        resultDir = "./res/G2KP";
        new File(resultDir).mkdirs();

        FileOutputStream csv = new FileOutputStream(resultDir + "/Res-G2KP.csv");

        csv.write(("Class,id,W,H,n,|E_d|,|E_s|,|SR|," +
                "|E_d|',|E_s|',|SR|',|I_in|,|I_out|," +
                "UB0-KP,UB0-LS,UB0,LB0,UB,LB," +
                "Gap0,Gap,Opt0,Opt,Violate-Dif,Violate-Same,Value-Error," +
                "Nodes',Nodes," +
                "#OPP,#EOPP,Time-OPP,Time-EOPP," +
                "Time-UB0-KP,Time-UB0-LS,Time-LB0,Time-Red,Time\n").getBytes(StandardCharsets.UTF_8));

        // 预热
//        for (int i = 0; i < 400; i++) {
//            test(null, "../../Instances/G2KP/Classic/Set1");
//        }

        test(csv, "../../Instances/G2KP/Classic/Set1");
//        test(csv, "../../Instances/G2KP/Newly/Set1");

        csv.close();
    }
}