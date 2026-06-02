package com.flowchart.editor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.flowchart.editor.model.Edge;
import com.flowchart.editor.model.FlowChart;
import com.flowchart.editor.model.LayoutResult;
import com.flowchart.editor.model.Node;
import com.flowchart.editor.model.Point;

import java.util.List;

public class FlowChartView extends View {
    private Paint nodePaint, nodeBorderPaint, nodeTextPaint, edgePaint, edgeArrowPaint, gridPaint;
    private FlowChart flowChart;
    private LayoutResult layoutResult;
    private float scale = 1.0f, offsetX = 0, offsetY = 0;
    private float lastTouchX, lastTouchY;
    private boolean showGrid = true;
    private int gridSize = 20;

    public FlowChartView(Context context) {
        super(context);
        initPaints();
    }

    private void initPaints() {
        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setColor(Color.parseColor("#4A90D9"));
        nodePaint.setStyle(Paint.Style.FILL);

        nodeBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodeBorderPaint.setColor(Color.parseColor("#2E5C8A"));
        nodeBorderPaint.setStyle(Paint.Style.STROKE);
        nodeBorderPaint.setStrokeWidth(2);

        nodeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodeTextPaint.setColor(Color.WHITE);
        nodeTextPaint.setTextSize(24);
        nodeTextPaint.setTextAlign(Paint.Align.CENTER);

        edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setColor(Color.parseColor("#333333"));
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(2);

        edgeArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgeArrowPaint.setColor(Color.parseColor("#333333"));
        edgeArrowPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStrokeWidth(1);
    }

    public void setFlowChart(FlowChart flowChart) {
        this.flowChart = flowChart;
        invalidate();
    }

    public void setLayoutResult(LayoutResult layoutResult) {
        this.layoutResult = layoutResult;
        if (flowChart != null && layoutResult != null) {
            flowChart.applyLayoutResult(layoutResult);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        if (showGrid) drawGrid(canvas);
        if (flowChart != null) {
            drawEdges(canvas);
            drawNodes(canvas);
        }
        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        int width = getWidth(), height = getHeight();
        float startX = -offsetX / scale, startY = -offsetY / scale;
        float endX = startX + width / scale, endY = startY + height / scale;
        startX = (int) (startX / gridSize) * gridSize;
        startY = (int) (startY / gridSize) * gridSize;
        for (float x = startX; x <= endX; x += gridSize) {
            canvas.drawLine(x, startY, x, endY, gridPaint);
        }
        for (float y = startY; y <= endY; y += gridSize) {
            canvas.drawLine(startX, y, endX, y, gridPaint);
        }
    }

    private void drawNodes(Canvas canvas) {
        List<Node> nodes = flowChart.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            RectF rect = new RectF(node.getX(), node.getY(), node.getRight(), node.getBottom());
            canvas.drawRoundRect(rect, 8, 8, nodePaint);
            canvas.drawRoundRect(rect, 8, 8, nodeBorderPaint);
            String label = node.getLabel();
            if (label != null && !label.isEmpty()) {
                canvas.drawText(label, node.getCenterX(), node.getCenterY() + 8, nodeTextPaint);
            }
        }
    }

    private void drawEdges(Canvas canvas) {
        List<Edge> edges = flowChart.getEdges();
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            List<Point> path = edge.getPath();
            if (path == null || path.size() < 2) continue;
            Path pathObj = new Path();
            Point first = path.get(0);
            pathObj.moveTo(first.x, first.y);
            for (int j = 1; j < path.size(); j++) {
                Point point = path.get(j);
                pathObj.lineTo(point.x, point.y);
            }
            canvas.drawPath(pathObj, edgePaint);
            if (path.size() >= 2) {
                Point last = path.get(path.size() - 1);
                Point secondLast = path.get(path.size() - 2);
                drawArrow(canvas, secondLast.x, secondLast.y, last.x, last.y);
            }
        }
    }

    private void drawArrow(Canvas canvas, int fromX, int fromY, int toX, int toY) {
        float angle = (float) Math.atan2(toY - fromY, toX - fromX);
        float arrowLength = 15, arrowAngle = (float) Math.PI / 6;
        float x1 = toX - arrowLength * (float) Math.cos(angle - arrowAngle);
        float y1 = toY - arrowLength * (float) Math.sin(angle - arrowAngle);
        float x2 = toX - arrowLength * (float) Math.cos(angle + arrowAngle);
        float y2 = toY - arrowLength * (float) Math.sin(angle + arrowAngle);
        Path arrowPath = new Path();
        arrowPath.moveTo(toX, toY);
        arrowPath.lineTo(x1, y1);
        arrowPath.lineTo(x2, y2);
        arrowPath.close();
        canvas.drawPath(arrowPath, edgeArrowPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - offsetX) / scale;
        float y = (event.getY() - offsetY) / scale;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX;
                float dy = event.getY() - lastTouchY;
                offsetX += dx;
                offsetY += dy;
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void resetView() {
        scale = 1.0f;
        offsetX = 0;
        offsetY = 0;
        invalidate();
    }

    public void fitToView() {
        if (flowChart == null || flowChart.getNodes().isEmpty()) {
            resetView();
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        List<Node> nodes = flowChart.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            minX = Math.min(minX, node.getX());
            minY = Math.min(minY, node.getY());
            maxX = Math.max(maxX, node.getRight());
            maxY = Math.max(maxY, node.getBottom());
        }
        int contentWidth = maxX - minX + 100;
        int contentHeight = maxY - minY + 100;
        float scaleX = getWidth() / (float) contentWidth;
        float scaleY = getHeight() / (float) contentHeight;
        scale = Math.min(scaleX, scaleY);
        offsetX = (getWidth() - contentWidth * scale) / 2 - minX * scale;
        offsetY = (getHeight() - contentHeight * scale) / 2 - minY * scale;
        invalidate();
    }

    public void setShowGrid(boolean show) {
        this.showGrid = show;
        invalidate();
    }

    public boolean isShowGrid() {
        return showGrid;
    }
}
