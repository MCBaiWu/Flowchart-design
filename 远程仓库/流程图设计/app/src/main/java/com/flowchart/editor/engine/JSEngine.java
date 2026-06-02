package com.flowchart.editor.engine;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.flowchart.editor.model.FlowChart;
import com.flowchart.editor.model.LayoutResult;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * JavaScript引擎
 */
public class JSEngine {

    private static final String TAG = "JSEngine";

    private WebView webView;
    private Context context;
    private Handler mainHandler;
    private boolean isReady;
    private JSEngineCallback callback;

    public interface JSEngineCallback {
        void onEngineReady();
        void onEngineError(String error);
        void onResult(LayoutResult result);
    }

    public JSEngine(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isReady = false;
    }

    public void init(WebView webView) {
        this.webView = webView;
        setupWebView();
        loadEngine();
    }

    public void setCallback(JSEngineCallback callback) {
        this.callback = callback;
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d(TAG, "JS: " + consoleMessage.message());
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                isReady = true;
                if (callback != null) {
                    callback.onEngineReady();
                }
            }
        });

        webView.addJavascriptInterface(new JSInterface(), "JSEngine");
    }

    private void loadEngine() {
        String html = buildEngineHTML();
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private String buildEngineHTML() {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>CFG Engine</title>" +
                "<script>" +
                "var FlowChartLayout = {" +
                "    placeNodes: function(nodes, edges, params) {" +
                "        params = params || {};" +
                "        var positions = {};" +
                "        var spacing = params.nodeSpacing || 50;" +
                "        var x = spacing, y = spacing;" +
                "        var rowHeight = 0;" +
                "        var maxWidth = params.maxWidth || 1200;" +
                "        for (var i = 0; i < nodes.length; i++) {" +
                "            var node = nodes[i];" +
                "            if (x + node.width + spacing > maxWidth) {" +
                "                x = spacing;" +
                "                y += rowHeight + spacing;" +
                "                rowHeight = 0;" +
                "            }" +
                "            positions[node.id] = {x: x, y: y};" +
                "            x += node.width + spacing;" +
                "            rowHeight = Math.max(rowHeight, node.height);" +
                "        }" +
                "        return positions;" +
                "    }," +
                "    generateChannels: function(nodePositions, edges, nodes, params) {" +
                "        var channels = {};" +
                "        var nodeMap = {};" +
                "        for (var i = 0; i < nodes.length; i++) nodeMap[nodes[i].id] = nodes[i];" +
                "        for (var i = 0; i < edges.length; i++) {" +
                "            var edge = edges[i];" +
                "            var s = nodePositions[edge.sourceId];" +
                "            var t = nodePositions[edge.targetId];" +
                "            var sn = nodeMap[edge.sourceId];" +
                "            var tn = nodeMap[edge.targetId];" +
                "            if (s && t && sn && tn) {" +
                "                var startX = s.x + sn.width;" +
                "                var startY = s.y + sn.height / 2;" +
                "                var endX = t.x;" +
                "                var endY = t.y + tn.height / 2;" +
                "                var midX = (startX + endX) / 2;" +
                "                channels[edge.id] = [{x: startX, y: startY}, {x: midX, y: startY}, {x: midX, y: endY}, {x: endX, y: endY}];" +
                "            }" +
                "        }" +
                "        return channels;" +
                "    }," +
                "    routeEdges: function(nodePositions, channels, nodes, params) {" +
                "        return channels;" +
                "    }," +
                "    layout: function(flowChart, params) {" +
                "        params = params || {};" +
                "        var nodes = flowChart.nodes || [];" +
                "        var edges = flowChart.edges || [];" +
                "        var nodePositions = this.placeNodes(nodes, edges, params);" +
                "        var channels = this.generateChannels(nodePositions, edges, nodes, params);" +
                "        var edgePaths = this.routeEdges(nodePositions, channels, nodes, params);" +
                "        var maxX = 0, maxY = 0;" +
                "        for (var id in nodePositions) {" +
                "            var pos = nodePositions[id];" +
                "            var node = nodes.find(function(n) { return n.id === id; });" +
                "            if (node) {" +
                "                maxX = Math.max(maxX, pos.x + node.width);" +
                "                maxY = Math.max(maxY, pos.y + node.height);" +
                "            }" +
                "        }" +
                "        return {nodePositions: nodePositions, edgePaths: edgePaths, canvasWidth: maxX + 100, canvasHeight: maxY + 100};" +
                "    }," +
                "    executeCustom: function(algorithmCode, flowChart, params) {" +
                "        try {" +
                "            var customAlgorithm = new Function('flowChart', 'params', algorithmCode + '; return LayoutAlgorithm.layout(flowChart, params);');" +
                "            return customAlgorithm(flowChart, params);" +
                "        } catch (e) {" +
                "            throw e;" +
                "        }" +
                "    }" +
                "};" +
                "</script></head><body></body></html>";
    }

    public void executeLayout(final FlowChart flowChart, final String customAlgorithmCode) {
        if (!isReady) {
            if (callback != null) callback.onEngineError("Engine not ready");
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String flowChartJson = flowChart.toJSON().toString();
                    String paramsJson = "{\"nodeSpacing\": 50, \"gridSize\": 10}";

                    String script;
                    if (customAlgorithmCode != null && !customAlgorithmCode.isEmpty()) {
                        script = "(function(){try{var flowChart=" + flowChartJson + ";" +
                                "var params=" + paramsJson + ";" +
                                "var customCode=" + JSONObject.quote(customAlgorithmCode) + ";" +
                                "var result=FlowChartLayout.executeCustom(customCode,flowChart,params);" +
                                "JSEngine.onResult(JSON.stringify(result));}catch(e){JSEngine.onError(e.toString());}})();";
                    } else {
                        script = "(function(){try{var flowChart=" + flowChartJson + ";" +
                                "var params=" + paramsJson + ";" +
                                "var result=FlowChartLayout.layout(flowChart,params);" +
                                "JSEngine.onResult(JSON.stringify(result));}catch(e){JSEngine.onError(e.toString());}})();";
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        webView.evaluateJavascript(script, null);
                    } else {
                        webView.loadUrl("javascript:" + script);
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onEngineError(e.getMessage());
                }
            }
        });
    }

    public boolean isReady() {
        return isReady;
    }

    public class JSInterface {
        @JavascriptInterface
        public void onResult(String resultJson) {
            try {
                JSONObject json = new JSONObject(resultJson);
                final LayoutResult result = LayoutResult.fromJSON(json);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.onResult(result);
                    }
                });
            } catch (JSONException e) {
                onError(e.getMessage());
            }
        }

        @JavascriptInterface
        public void onError(final String error) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) callback.onEngineError(error);
                }
            });
        }
    }
}
