package com.wskh.run;

import com.wskh.classes.IntValue_Item;
import com.wskh.classes.PlaceItem;
import com.wskh.solvers.TKPC.TKPC_Safe_Solver;
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

public class RunForSolving_2KP {

    static void test(FileOutputStream csv, String dirPath, String instanceType) throws Exception {

        String localResultDir = resultDir + "/" + instanceType + "/";
        String imgDir = localResultDir + "img/";
        String solutionDir = localResultDir + "solution/";
        if (instanceType != null) {
            new File(localResultDir).mkdirs();
            new File(imgDir).mkdirs();
            new File(solutionDir).mkdirs();
        }

        File dir = new File(dirPath);
        File[] files = Objects.requireNonNull(dir.listFiles());
        Arrays.sort(files, Comparator.comparingInt((File o) -> o.getName().length()).thenComparing(File::getName));

        for (File file : files) {
            if (file.getName().endsWith("ins2D")) {

//                if (file.getName().equals("GCUT13.ins2D")) continue;
//                if (!file.getName().equals("GCUT13.ins2D")) continue;

                if (csv != null) {
                    System.gc();
                    System.out.println(instanceType + " : " + file.getName() + " => " + new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss").format(new Date()));
                }

                int W = 0, H = 0, m = 0;
                List<IntValue_Item> itemList = new ArrayList<>();

                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String input;
                int row = 0;
                while ((input = bufferedReader.readLine()) != null) {
                    String[] split = input.split(" ");
                    if (row == 0) {
                        m = Integer.parseInt(input);
                    } else if (row == 1) {
                        W = Integer.parseInt(split[0]);
                        H = Integer.parseInt(split[1]);
                    } else if (row > 1) {
                        int w = Integer.parseInt(split[1]);
                        int h = Integer.parseInt(split[2]);
                        int num = Integer.parseInt(split[3]);
                        int v = Integer.parseInt(split[4]);
                        int s = w * h;
                        for (int i = 0; i < num; i++) {
                            itemList.add(new IntValue_Item(itemList.size(), itemList.size(), w, h, s, v, v / (double) s));
                        }
                    }
                    row++;
                }
                bufferedReader.close();

                Random random = new Random(929L);

//                TKPC_UnSafe_Solver solver = new TKPC_UnSafe_Solver(random);
//                TKPC_UnSafe_Solver_NoLBD solver = new TKPC_UnSafe_Solver_NoLBD(random);
                TKPC_Safe_Solver solver = new TKPC_Safe_Solver(random);

                TimeUtil.startTime = System.currentTimeMillis();
                solver.solve(itemList.size(), W, H, IntValue_Item.copy(itemList.toArray(new IntValue_Item[0])), new ArrayList<>());
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

                CheckUtil.checkOverlapAndOutBin(W, H, solver.bestPlaceItemList);

                if (csv != null) {
                    Object[] objects = new Object[]{
                            instanceType, file.getName().replace(".ins2D", ""), W, H, itemList.size(),
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

                    WriteUtil.writeSolution(List.of(solver.bestPlaceItemList), solutionDir + file.getName().replace(".ins2D", ".2kp.sol"));
                    WriteUtil.writePatternPlotToPng(W, H, solver.bestPlaceItemList, imgDir + file.getName().replace(".ins2D", ".png"));
                    System.out.println("LB0: " + solver.LB0 + " , UB0: " + solver.UB0 + " , LB: " + solver.LB + " , isOpt: " + (solver.UB == solver.LB ? 1 : 0) + " , Time: " + ((time / 1000d)));
                    System.out.println("------------------------------------");
                }

            }
        }

//        System.out.println(instanceType + " : " + min_w + "-" + max_w + " : " + min_h + "-" + max_h);

    }

    static String resultDir;

    public static void main(String[] args) throws Exception {
        double r = 3787d / 385d;
        TimeUtil.TimeLimit = (long) Math.ceil(36000d / r * 1000);
//        TimeUtil.TimeLimit = 24 * 3600L * 1000L;
        resultDir = "./res/2KP";
        new File(resultDir).mkdirs();

        // 预热
//        for (int i = 0; i < 20; i++) {
//            test(null, "../../Instances/2KP/NGCUT", "NGCUT");
//            test(null, "../../Instances/2KP/HCCUT", "HCCUT");
//            test(null, "../../Instances/2KP/CGCUT", "CGCUT");
//            test(null, "../../Instances/2KP/WANG", "WANG");
//        }

        FileOutputStream csv = new FileOutputStream(resultDir + "/Res-2KP.csv");

        csv.write(("Set,Instance,W,H,n," +
                "|I_in|,|I_out|," +
                "UB0-KP,UB0-LS,UB0,LB0,UB,LB," +
                "Gap0,Gap,Opt0,Opt," +
                "Nodes',Nodes," +
                "#OPP,#EOPP,Time-OPP,Time-EOPP," +
                "Time-UB0-KP,Time-UB0-LS,Time-LB0,Time-Red,Time\n").getBytes(StandardCharsets.UTF_8));

        test(csv, "../../Instances/2KP/NGCUT", "NGCUT");
        test(csv, "../../Instances/2KP/HCCUT", "HCCUT");
        test(csv, "../../Instances/2KP/CGCUT", "CGCUT");
        test(csv, "../../Instances/2KP/GCUT", "GCUT");
        test(csv, "../../Instances/2KP/OKP", "OKP");
        test(csv, "../../Instances/2KP/WANG", "WANG");
        test(csv, "../../Instances/2KP/H", "H");
        test(csv, "../../Instances/2KP/HZ", "HZ");
        test(csv, "../../Instances/2KP/M", "M");
        test(csv, "../../Instances/2KP/MW", "MW");
        test(csv, "../../Instances/2KP/UU", "UU");
        test(csv, "../../Instances/2KP/UW", "UW");
        test(csv, "../../Instances/2KP/W", "W");

        csv.close();
    }
}