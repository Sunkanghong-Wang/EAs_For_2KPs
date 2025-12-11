package com.wskh.utils;
import com.wskh.classes.PlaceItem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

public class WriteUtil {
    public static void writeSolution(List<List<PlaceItem>> placeItemListList, String path) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            for (List<PlaceItem> placeItemList : placeItemListList)
                placeItemList.sort(Comparator.comparingInt(o -> o.id));
            fileOutputStream.write((placeItemListList.size() + "\n").getBytes(StandardCharsets.UTF_8));
            for (List<PlaceItem> placeItemList : placeItemListList) {
                fileOutputStream.write((placeItemList.size() + "\n").getBytes(StandardCharsets.UTF_8));
                for (PlaceItem placeItem : placeItemList) {
                    fileOutputStream.write(((placeItem.id + 1) + " " + placeItem.x + " " + placeItem.y + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }
            fileOutputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writePatternPlotToPng(int W, int H, List<PlaceItem> placeItemList, String exportPath) {
        int min_W_H = Math.min(W, H);
        double qingxidu = 800d;
        int r = min_W_H < qingxidu ? (int) Math.ceil(qingxidu / min_W_H) : 1; // 缩放比例
        int imgW = W * r;
        int imgH = H * r;

        int gap = Math.min(imgW, imgH) / 40;
        float borderWidth = Math.min(imgW, imgH) / 200f; // 边框粗细随缩放比例调整

        // 创建高分辨率图像
        BufferedImage bufferedImage = new BufferedImage(imgW + gap * 2, imgH + gap * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bufferedImage.createGraphics();

        // 启用抗锯齿
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 设置背景颜色
        graphics.setColor(new Color(255, 255, 255, 255));
        graphics.fillRect(0, 0, imgW + gap * 2, imgH + gap * 2);

        // 绘制大板
        graphics.setColor(new Color(158, 158, 158, 255));
        graphics.fillRect(gap, gap, imgW, imgH);

        // 设置大板边框粗细
        graphics.setStroke(new BasicStroke(borderWidth));
        graphics.setColor(Color.BLACK);
        graphics.drawRect(gap, gap, imgW, imgH);

        // 绘制物品
        for (PlaceItem placeItem : placeItemList) {
            // 计算物品的绘制区域
            int boardLen = placeItem.w * r;
            int boardWid = placeItem.h * r;
            int boardX = placeItem.x * r + gap;
            int boardY = imgH - placeItem.y * r - placeItem.h * r + gap;

            // 绘制物品背景
            graphics.setColor(new Color(250, 250, 250, 255));
            graphics.fillRect(boardX, boardY, boardLen, boardWid);

            // 设置物品边框粗细
            graphics.setStroke(new BasicStroke(borderWidth));
            graphics.setColor(Color.BLACK);
            graphics.drawRect(boardX, boardY, boardLen, boardWid);

            // 在矩形中间绘制ID
            String id = String.valueOf(placeItem.id + 1);
            Font font = new Font("Times New Roman", Font.PLAIN, 12); // 初始字体大小
            FontMetrics metrics = graphics.getFontMetrics(font);

            // 计算字体大小，使其适应矩形
            int textWidth = metrics.stringWidth(id);
            int textHeight = metrics.getHeight();
            int maxFontSize = Math.min(boardLen * 12 / textWidth, boardWid * 12 / textHeight);
            font = font.deriveFont((float) maxFontSize);
            metrics = graphics.getFontMetrics(font);

            // 重新计算文本宽度和高度
            textWidth = metrics.stringWidth(id);
            textHeight = metrics.getHeight();

            // 计算文本位置
            int textX = boardX + (boardLen - textWidth) / 2;
            int textY = boardY + (boardWid - textHeight) / 2 + metrics.getAscent();

            // 设置字体并绘制文本
            graphics.setFont(font);
            graphics.setColor(Color.BLACK);
            graphics.drawString(id, textX, textY);
        }

        // 导出图片
        try {
            ImageIO.write(bufferedImage, "PNG", Files.newOutputStream(Paths.get(exportPath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            graphics.dispose(); // 释放资源
        }
    }

}