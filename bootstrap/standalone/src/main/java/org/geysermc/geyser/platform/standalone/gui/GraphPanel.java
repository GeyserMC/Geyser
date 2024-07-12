/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.platform.standalone.gui;

import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This has been modified to fit Geyser more but is based on
 * <a href="https://gist.github.com/roooodcastro/6325153#gistcomment-3107524">this Github gist</a>
 */
public final class GraphPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private final static int padding = 10;
    private final static int labelPadding = 25;
    private final static int pointWidth = 4;
    private final static int numberYDivisions = 10;
    private final static Color lineColor = new Color(44, 102, 230, 255);
    private final static Color pointColor = new Color(100, 100, 100, 255);
    private final static Color gridColor = new Color(200, 200, 200, 255);
    private static final Stroke graphStroke = new BasicStroke(2f);
    private final List<Integer> values = new ArrayList<>(10);

    @Setter
    private String xLabel = "";

    public GraphPanel() {
        setPreferredSize(new Dimension(200 - (padding * 2), 150 - (padding * 2)));
    }

    public void setValues(Collection<Integer> newValues) {
        values.clear();
        addValues(newValues);
    }

    public void addValues(Collection<Integer> newValues) {
        values.addAll(newValues);
        updateUI();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (!(graphics instanceof final Graphics2D g)) {
            graphics.drawString("Graphics is not Graphics2D, unable to render", 0, 0);
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final int length = values.size();
        final int width = getWidth();
        final int height = getHeight();
        final int maxScore = getMaxScore();
        final int minScore = getMinScore();
        final int scoreRange = maxScore - minScore;

        // draw white background
        g.setColor(Color.WHITE);
        g.fillRect(
                padding + labelPadding,
                padding,
                width - (2 * padding) - labelPadding,
                height - 2 * padding - labelPadding);
        g.setColor(Color.BLACK);

        final FontMetrics fontMetrics = g.getFontMetrics();
        final int fontHeight = fontMetrics.getHeight();

        // create hatch marks and grid lines for y axis.
        for (int i = 0; i < numberYDivisions + 1; i++) {
            final int x1 = padding + labelPadding;
            final int x2 = pointWidth + padding + labelPadding;
            final int y = height - ((i * (height - padding * 2 - labelPadding)) / numberYDivisions + padding + labelPadding);
            if (length > 0) {
                g.setColor(gridColor);
                g.drawLine(padding + labelPadding + 1 + pointWidth, y, width - padding, y);

                g.setColor(Color.BLACK);
                final int tickValue = minScore + ((scoreRange * i) / numberYDivisions);
                final String yLabel = tickValue + "";
                final int labelWidth = fontMetrics.stringWidth(yLabel);
                g.drawString(yLabel, x1 - labelWidth - 5, y + (fontHeight / 2) - 3);
            }
            g.drawLine(x1, y, x2, y);
        }

        // and for x axis
        if (length > 1) {
            for (int i = 0; i < length; i++) {
                final int x = i * (width - padding * 2 - labelPadding) / (length - 1) + padding + labelPadding;
                final int y1 = height - padding - labelPadding;
                final int y2 = y1 - pointWidth;
                if ((i % ((int) ((length / 20.0)) + 1)) == 0) {
                    g.setColor(gridColor);
                    g.drawLine(x, height - padding - labelPadding - 1 - pointWidth, x, padding);

                    g.setColor(Color.BLACK);

                    /*g.setColor(Color.BLACK);
                    final String xLabel = i + "";
                    final int labelWidth = fontMetrics.stringWidth(xLabel);
                    g.drawString(xLabel, x - labelWidth / 2, y1 + fontHeight + 3);*/
                }
                g.drawLine(x, y1, x, y2);
            }
        }

        // create x and y axes
        g.drawLine(padding + labelPadding, height - padding - labelPadding, padding + labelPadding, padding);
        g.drawLine(padding + labelPadding, height - padding - labelPadding, width - padding, height - padding - labelPadding);

        g.setColor(Color.BLACK);
        final int labelWidth = fontMetrics.stringWidth(xLabel);
        final int labelX = ((padding + labelPadding) + (width - padding)) / 2;
        final int labelY = height - padding - labelPadding;
        g.drawString(xLabel, labelX - labelWidth / 2, labelY + fontHeight + 3);

        final Stroke oldStroke = g.getStroke();
        g.setColor(lineColor);
        g.setStroke(graphStroke);

        final double xScale = ((double) width - (2 * padding) - labelPadding) / (length - 1);
        final double yScale = ((double) height - 2 * padding - labelPadding) / scoreRange;

        final List<Point> graphPoints = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            final int x1 = (int) (i * xScale + padding + labelPadding);
            final int y1 = (int) ((maxScore - values.get(i)) * yScale + padding);
            graphPoints.add(new Point(x1, y1));
        }

        for (int i = 0; i < graphPoints.size() - 1; i++) {
            final int x1 = graphPoints.get(i).x;
            final int y1 = graphPoints.get(i).y;
            final int x2 = graphPoints.get(i + 1).x;
            final int y2 = graphPoints.get(i + 1).y;
            g.drawLine(x1, y1, x2, y2);
        }

        boolean drawDots = width > (length * pointWidth);
        if (drawDots) {
            g.setStroke(oldStroke);
            g.setColor(pointColor);
            for (Point graphPoint : graphPoints) {
                final int x = graphPoint.x - pointWidth / 2;
                final int y = graphPoint.y - pointWidth / 2;
                //noinspection SuspiciousNameCombination
                g.fillOval(x, y, pointWidth, pointWidth);
            }
        }
    }

    private int getMinScore() {
        return 0;
    }

    private int getMaxScore() {
        return 100;
    }
}
