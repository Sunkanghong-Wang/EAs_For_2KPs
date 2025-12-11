package com.wskh.run;

import com.wskh.classes.Item;
import com.wskh.classes.PlaceItem;
import com.wskh.solvers.TOPP.TOPP_Safe_Solver;
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

public class RunForSolving_2OPP {

    static void test(FileOutputStream csv, String dirPath, String instanceType) throws Exception {
        String localResultDir = resultDir + "\\" + instanceType + "\\";
        String imgDir = localResultDir + "img\\";
        String solutionDir = localResultDir + "solution\\";
        if (instanceType != null) {
            new File(localResultDir).mkdirs();
            new File(imgDir).mkdirs();
            new File(solutionDir).mkdirs();
        }

        File dir = new File(dirPath);
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getName().endsWith("ins2D")) {

                System.gc();

//                if (!file.getName().equals("E00X23.ins2D")) continue;
//                if (!file.getName().equals("E03N10.ins2D")) continue;
//                if (!file.getName().equals("OPP_2D_10_2s_5I_40W.ins2D")) continue;
//                if (!file.getName().equals("OPP_2D_20_2s_7I_0W.ins2D")) continue;
//                if (!file.getName().equals("OPP_2D_25_2s_4I_0W.ins2D")) continue;
//                if (!file.getName().equals("OPP_2D_10_2s_5I_6W.ins2D")) continue;

//                if (file.getName().equals("OPP_2D_20_2s_7I_0W.ins2D") || file.getName().equals("OPP_2D_25_2s_6I_0W.ins2D")) continue;

                if (csv != null){
                    System.out.println(instanceType + " : " + file.getName() + " => " + new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss").format(new Date()));
                }

                int m = 0, W = 0, H = 0;
                List<Item> itemList = new ArrayList<>();

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
                        int dMax = Integer.parseInt(split[4]);
                        int w = Integer.parseInt(split[1]);
                        int h = Integer.parseInt(split[2]);
                        int s = w * h;
                        for (int i = 0; i < dMax; i++) {
                            itemList.add(new Item(itemList.size(), itemList.size(), w, h, s));
                        }
                    }
                    row++;
                }
                bufferedReader.close();

                Item[] items = new Item[itemList.size()];
                for (int i = 0; i < itemList.size(); i++) items[i] = itemList.get(i).copy();
                Arrays.sort(items, Item.itemComparatorByDecreaseS);
                for (int i = 0; i < items.length; i++) items[i].index = i;

                Random random = new Random(929L);

                // 求解
//                TOPP_UnSafe_Solver solver = new TOPP_UnSafe_Solver(random);
                TOPP_Safe_Solver solver = new TOPP_Safe_Solver(random);

                TimeUtil.startTime = System.currentTimeMillis();
                List<PlaceItem> placeItemList = solver.solve(W, H, items.length, items);
                long time = TimeUtil.getCurTime();
                boolean opt = placeItemList != null || time < TimeUtil.TimeLimit - 10;

                if (placeItemList != null) {
                    for (PlaceItem placeItem : placeItemList) {
                        for (Item item : itemList) {
                            if (item.id == placeItem.id) {
                                placeItem.index = item.index;
                                placeItem.w = item.w;
                                placeItem.h = item.h;
                                placeItem.s = item.s;
                                break;
                            }
                        }
                    }
                    CheckUtil.checkOverlapAndOutBin(W, H, placeItemList);
                }

                // 导出结果
                if (csv != null) {
                    csv.write((instanceType + "," + file.getName().replace(".ins2D", "") + "," + W + "," + H + "," + m + "," + itemList.size()
                            + "," + solver.exploredNodes + "," + solver.generatedNodes + "," + (opt ? 1 : 0) + "," + (placeItemList == null ? 0 : 1) + "," + time / 1000d + "\n").getBytes(StandardCharsets.UTF_8));

//                    csv.write((instanceType + "," + file.getName().replace(".ins2D", "") + "," + W + "," + H + "," + m + "," + itemList.size()
//                            + "," + solver.exploredNodes + "," + solver.generatedNodes + "," + +solver.liftCnt + "," + solver.addCutCnt + "," +solver.infeasibleSetSize+","+ (opt ? 1 : 0) + "," + (placeItemList == null ? 0 : 1) + "," + time / 1000d + "\n").getBytes(StandardCharsets.UTF_8));

                    if (placeItemList != null) {
                        WriteUtil.writePatternPlotToPng(W, H, placeItemList, imgDir + file.getName().replace(".ins2D", ".png"));
                    }
                    if (opt) {
                        WriteUtil.writeSolution(List.of((placeItemList == null ? new ArrayList<>() : placeItemList)), solutionDir + file.getName().replace(".ins2D", ".2opp.sol"));
                    }
                    System.out.println("opt: " + opt + " , feasible: " + (placeItemList != null) + " , time: " + (time / 1000d) + " s");
                    System.out.println("------------------------------------");
                }

            }
        }

    }

    static String resultDir;

    public static void main(String[] args) throws Exception {

        double r = 3787d / 1294d;
        TimeUtil.TimeLimit = (long) Math.ceil(900d / r * 1000);
        resultDir = "./res/2OPP";
        new File(resultDir).mkdirs();

        // 预热
//        for (int i = 0; i < 20; i++) {
//            test(null, "../../Instances/2OPP/CJCM", null);
//        }

        FileOutputStream csv = new FileOutputStream(resultDir + "/Res-2OPP.csv");

        csv.write(("type,instance,W,H,m,n,exploredNodes,generatedNodes,opt,feasible,time\n").getBytes(StandardCharsets.UTF_8));

        test(csv, "../../Instances/2OPP/CJCM", "CJCM");
        test(csv, "../../Instances/2OPP/MSB/MSB-450", "MSB-450");
        test(csv, "../../Instances/2OPP/N", "N");
        test(csv, "../../Instances/2OPP/T", "T");
        test(csv, "../../Instances/2OPP/C", "C");
        test(csv, "../../Instances/2OPP/MSB/MSB-630", "MSB-630");

        csv.close();
    }
}