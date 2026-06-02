package com.flowchart.editor;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.flowchart.editor.engine.JSEngine;
import com.flowchart.editor.model.Edge;
import com.flowchart.editor.model.FlowChart;
import com.flowchart.editor.model.LayoutResult;
import com.flowchart.editor.model.Node;
import com.flowchart.editor.view.FlowChartView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 主Activity - 使用ActionBar Tab分页
 * 纯Java实现UI，无XML布局
 */
public class MainActivity extends Activity implements ActionBar.TabListener, JSEngine.JSEngineCallback {

    // 静态实例供Fragment访问
    public static MainActivity instance;
    
    // UI组件
    private FrameLayout contentContainer;
    private LinearLayout drawerLayout;
    private ListView drawerList;
    private TextView statusText;
    
    // 视图组件
    public FlowChartView flowChartView;
    public android.webkit.WebView webView;
    public EditText codeEditor;
    
    // 引擎和数据
    public JSEngine jsEngine;
    public FlowChart currentFlowChart;
    public boolean isEngineReady = false;
    
    // 状态
    private boolean isDrawerOpen = false;
    private static final String TAB_CFG = "CFG";
    private static final String TAB_JS = "JS";
    private String currentTab = TAB_CFG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        
        // 创建根布局
        createRootLayout();
        
        // 初始化数据
        initData();
        
        // 初始化JS引擎（延迟执行）
        initJSEngine();
    }

    /**
     * 创建根布局
     */
    private void createRootLayout() {
        // 根布局：FrameLayout包含主内容和侧滑栏
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // 主内容容器
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 内容区域
        contentContainer = new FrameLayout(this);
        contentContainer.setId(1000);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1.0f));
        mainLayout.addView(contentContainer);

        // 状态栏
        createStatusBar(mainLayout);
        
        rootLayout.addView(mainLayout);

        // 创建侧滑栏
        createDrawer(rootLayout);

        setContentView(rootLayout);

        // 设置ActionBar
        setupActionBar();
    }

    /**
     * 设置ActionBar
     */
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) return;
        
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle("CFG Visualizer");
        actionBar.setSubtitle("控制流图可视化");
        
        // 添加菜单按钮
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // 添加Tab
        ActionBar.Tab cfgTab = actionBar.newTab();
        cfgTab.setText(TAB_CFG);
        cfgTab.setTabListener(this);
        actionBar.addTab(cfgTab);

        ActionBar.Tab jsTab = actionBar.newTab();
        jsTab.setText(TAB_JS);
        jsTab.setTabListener(this);
        actionBar.addTab(jsTab);
    }

    /**
     * 创建侧滑栏
     */
    private void createDrawer(FrameLayout root) {
        drawerLayout = new LinearLayout(this);
        drawerLayout.setOrientation(LinearLayout.VERTICAL);
        drawerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                320, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT));
        drawerLayout.setBackgroundColor(Color.WHITE);
        drawerLayout.setX(-320); // 初始隐藏

        // 头部
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        header.setBackgroundColor(Color.parseColor("#3F51B5"));
        header.setPadding(16, 48, 16, 16);

        TextView titleText = new TextView(this);
        titleText.setText("CFG Visualizer");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(20);
        header.addView(titleText);

        TextView subtitleText = new TextView(this);
        subtitleText.setText("控制流图可视化工具");
        subtitleText.setTextColor(Color.parseColor("#B3E5FC"));
        subtitleText.setTextSize(14);
        header.addView(subtitleText);

        drawerLayout.addView(header);

        // 示例标题
        TextView examplesTitle = new TextView(this);
        examplesTitle.setText("示例数据");
        examplesTitle.setTextColor(Color.parseColor("#757575"));
        examplesTitle.setTextSize(14);
        examplesTitle.setPadding(16, 16, 16, 8);
        drawerLayout.addView(examplesTitle);

        // 示例列表
        String[] examples = {
            "简单if-else结构",
            "while循环结构", 
            "switch-case结构",
            "嵌套循环结构",
            "复杂控制流",
            "从JSON加载..."
        };

        drawerList = new ListView(this);
        drawerList.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1.0f));
        drawerList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, examples));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onDrawerItemSelected(position);
            }
        });
        drawerLayout.addView(drawerList);

        root.addView(drawerLayout);
    }

    /**
     * 创建状态栏
     */
    private void createStatusBar(LinearLayout parent) {
        LinearLayout statusBar = new LinearLayout(this);
        statusBar.setOrientation(LinearLayout.HORIZONTAL);
        statusBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        statusBar.setBackgroundColor(Color.parseColor("#263238"));
        statusBar.setPadding(16, 8, 16, 8);

        statusText = new TextView(this);
        statusText.setText("就绪 | 节点: 0 | 边: 0");
        statusText.setTextColor(Color.parseColor("#B0BEC5"));
        statusText.setTextSize(12);
        statusBar.addView(statusText);

        parent.addView(statusBar);
    }

    /**
     * 切换侧滑栏
     */
    public void toggleDrawer() {
        if (isDrawerOpen) {
            drawerLayout.animate().x(-320).setDuration(250).start();
        } else {
            drawerLayout.animate().x(0).setDuration(250).start();
        }
        isDrawerOpen = !isDrawerOpen;
    }

    /**
     * 侧滑栏项目选择
     */
    private void onDrawerItemSelected(int position) {
        toggleDrawer();
        
        switch (position) {
            case 0: loadExampleCFG("if_else"); break;
            case 1: loadExampleCFG("while_loop"); break;
            case 2: loadExampleCFG("switch_case"); break;
            case 3: loadExampleCFG("nested_loop"); break;
            case 4: loadExampleCFG("complex"); break;
            case 5: showLoadJSONDialog(); break;
        }
    }

    /**
     * Tab选中
     */
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        String tabText = tab.getText().toString();
        currentTab = tabText;
        
        contentContainer.removeAllViews();
        
        if (TAB_CFG.equals(tabText)) {
            showCFGView();
        } else if (TAB_JS.equals(tabText)) {
            showJSEditorView();
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {}

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {}

    /**
     * 显示CFG视图
     */
    private void showCFGView() {
        if (flowChartView == null) {
            flowChartView = new FlowChartView(this);
        }
        if (flowChartView.getParent() != null) {
            ((ViewGroup) flowChartView.getParent()).removeView(flowChartView);
        }
        flowChartView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        contentContainer.addView(flowChartView);
        
        // 重新设置数据
        if (currentFlowChart != null) {
            flowChartView.setFlowChart(currentFlowChart);
            flowChartView.invalidate();
        }
    }

    /**
     * 显示JS编辑器视图
     */
    private void showJSEditorView() {
        LinearLayout jsContainer = new LinearLayout(this);
        jsContainer.setOrientation(LinearLayout.VERTICAL);
        jsContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        jsContainer.setBackgroundColor(Color.parseColor("#1E1E1E"));

        // 工具栏
        LinearLayout toolbar = createJSToolbar();
        jsContainer.addView(toolbar);

        // 代码编辑器区域
        LinearLayout editorContainer = new LinearLayout(this);
        editorContainer.setOrientation(LinearLayout.HORIZONTAL);
        editorContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1.0f));

        // 行号
        TextView lineNumbers = new TextView(this);
        lineNumbers.setTextColor(Color.parseColor("#858585"));
        lineNumbers.setTextSize(14);
        lineNumbers.setPadding(16, 16, 16, 16);
        lineNumbers.setBackgroundColor(Color.parseColor("#1E1E1E"));
        lineNumbers.setText("1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n13\n14\n15\n16\n17\n18\n19\n20\n21\n22\n23\n24\n25\n26\n27\n28\n29\n30");
        editorContainer.addView(lineNumbers);

        // 代码编辑框
        if (codeEditor == null) {
            codeEditor = new EditText(this);
            codeEditor.setBackgroundColor(Color.parseColor("#1E1E1E"));
            codeEditor.setTextColor(Color.parseColor("#D4D4D4"));
            codeEditor.setTextSize(14);
            codeEditor.setPadding(16, 16, 16, 16);
            codeEditor.setGravity(Gravity.TOP | Gravity.START);
            codeEditor.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                    android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            codeEditor.setHorizontallyScrolling(true);
            codeEditor.setMinLines(50);
            loadDefaultAlgorithm();
        }
        if (codeEditor.getParent() != null) {
            ((ViewGroup) codeEditor.getParent()).removeView(codeEditor);
        }
        codeEditor.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        editorContainer.addView(codeEditor);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1.0f));
        scrollView.addView(editorContainer);
        jsContainer.addView(scrollView);

        // API提示
        TextView apiHint = new TextView(this);
        apiHint.setText("API: LayoutAlgorithm.layout(flowChart, params)");
        apiHint.setTextColor(Color.parseColor("#888888"));
        apiHint.setTextSize(12);
        apiHint.setPadding(16, 8, 16, 8);
        apiHint.setBackgroundColor(Color.parseColor("#2D2D30"));
        jsContainer.addView(apiHint);

        contentContainer.addView(jsContainer);
    }

    /**
     * 创建JS工具栏
     */
    private LinearLayout createJSToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        toolbar.setBackgroundColor(Color.parseColor("#2D2D30"));
        toolbar.setPadding(8, 8, 8, 8);

        toolbar.addView(createToolbarButton("API手册", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAPIDocumentation();
            }
        }));

        toolbar.addView(createToolbarButton("重置算法", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDefaultAlgorithm();
                Toast.makeText(MainActivity.this, "算法已重置", Toast.LENGTH_SHORT).show();
            }
        }));

        Button executeBtn = createToolbarButton("▶ 执行", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeAlgorithm();
            }
        });
        executeBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        toolbar.addView(executeBtn);

        toolbar.addView(createToolbarButton("重置视图", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flowChartView != null) {
                    flowChartView.resetView();
                }
            }
        }));

        return toolbar;
    }

    private Button createToolbarButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(11);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.parseColor("#3F51B5"));
        button.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.setMargins(4, 0, 4, 0);
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        return button;
    }

    /**
     * 初始化数据
     */
    private void initData() {
        currentFlowChart = new FlowChart("cfg_1", "Control Flow Graph");
        // 默认加载示例
        loadExampleCFG("if_else");
    }

    /**
     * 初始化JS引擎
     */
    private void initJSEngine() {
        // 延迟初始化WebView
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (webView == null) {
                    webView = new android.webkit.WebView(MainActivity.this);
                    webView.setLayoutParams(new FrameLayout.LayoutParams(1, 1));
                    addContentView(webView, new FrameLayout.LayoutParams(1, 1));
                }
                jsEngine = new JSEngine(MainActivity.this);
                jsEngine.setCallback(MainActivity.this);
                jsEngine.init(webView);
            }
        }, 500);
    }

    private android.os.Handler mainHandler = new android.os.Handler();

    /**
     * 加载默认算法
     */
    private void loadDefaultAlgorithm() {
        if (codeEditor != null) {
            codeEditor.setText(getDefaultAlgorithmCode());
        }
    }

    private String getDefaultAlgorithmCode() {
        return "// 三阶段布局算法\n" +
                "var LayoutAlgorithm = {\n" +
                "    // 阶段1：放置基本块\n" +
                "    placeNodes: function(nodes, edges, params) {\n" +
                "        var positions = {};\n" +
                "        var x = 50, y = 50;\n" +
                "        for (var i = 0; i < nodes.length; i++) {\n" +
                "            positions[nodes[i].id] = {x: x, y: y};\n" +
                "            x += 150;\n" +
                "            if (x > 800) { x = 50; y += 100; }\n" +
                "        }\n" +
                "        return positions;\n" +
                "    },\n" +
                "\n" +
                "    // 阶段2：生成管道\n" +
                "    generateChannels: function(nodePositions, edges, nodes, params) {\n" +
                "        var channels = {};\n" +
                "        for (var i = 0; i < edges.length; i++) {\n" +
                "            var e = edges[i];\n" +
                "            var s = nodePositions[e.sourceId];\n" +
                "            var t = nodePositions[e.targetId];\n" +
                "            if (s && t) {\n" +
                "                channels[e.id] = [\n" +
                "                    {x: s.x + 50, y: s.y + 25},\n" +
                "                    {x: t.x, y: t.y + 25}\n" +
                "                ];\n" +
                "            }\n" +
                "        }\n" +
                "        return channels;\n" +
                "    },\n" +
                "\n" +
                "    // 阶段3：A*寻路\n" +
                "    routeEdges: function(nodePositions, channels, nodes, params) {\n" +
                "        return channels;\n" +
                "    },\n" +
                "\n" +
                "    // 执行布局\n" +
                "    layout: function(flowChart, params) {\n" +
                "        var nodes = flowChart.nodes || [];\n" +
                "        var edges = flowChart.edges || [];\n" +
                "        var nodePositions = this.placeNodes(nodes, edges, params);\n" +
                "        var channels = this.generateChannels(nodePositions, edges, nodes, params);\n" +
                "        var edgePaths = this.routeEdges(nodePositions, channels, nodes, params);\n" +
                "        return {\n" +
                "            nodePositions: nodePositions,\n" +
                "            edgePaths: edgePaths,\n" +
                "            canvasWidth: 1000,\n" +
                "            canvasHeight: 600\n" +
                "        };\n" +
                "    }\n" +
                "};";
    }

    /**
     * 执行算法
     */
    private void executeAlgorithm() {
        if (!isEngineReady) {
            Toast.makeText(this, "JS引擎未就绪，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (codeEditor == null) return;
        
        String algorithmCode = codeEditor.getText().toString();
        jsEngine.executeLayout(currentFlowChart, algorithmCode);
        updateStatus("正在执行算法...");
    }

    @Override
    public void onEngineReady() {
        isEngineReady = true;
        updateStatus();
    }

    @Override
    public void onEngineError(String error) {
        updateStatus("错误: " + error);
        Toast.makeText(this, "执行错误: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResult(LayoutResult result) {
        if (flowChartView != null) {
            flowChartView.setLayoutResult(result);
            flowChartView.fitToView();
        }
        updateStatus();
        
        // 跳转到CFG标签页
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.selectTab(actionBar.getTabAt(0));
        }
        Toast.makeText(this, "算法执行完成", Toast.LENGTH_SHORT).show();
    }

    /**
     * 加载示例CFG
     */
    private void loadExampleCFG(String exampleName) {
        try {
            String json = loadAssetFile("cfg/" + exampleName + ".json");
            if (json != null) {
                loadCFGFromJSON(json);
                Toast.makeText(this, "已加载: " + exampleName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从JSON加载CFG
     */
    private void loadCFGFromJSON(String json) {
        try {
            JSONObject cfgJson = new JSONObject(json);
            currentFlowChart = FlowChart.fromJSON(cfgJson);
            
            if (flowChartView != null) {
                flowChartView.setFlowChart(currentFlowChart);
                flowChartView.invalidate();
            }
            
            if (isEngineReady) {
                executeAlgorithm();
            }
            updateStatus();
        } catch (JSONException e) {
            Toast.makeText(this, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示加载JSON对话框
     */
    private void showLoadJSONDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入CFG JSON数据");

        final EditText input = new EditText(this);
        input.setHint("粘贴JSON数据...");
        input.setMinLines(10);
        input.setGravity(Gravity.TOP);
        builder.setView(input);

        builder.setPositiveButton("加载", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String json = input.getText().toString();
                if (!json.isEmpty()) {
                    loadCFGFromJSON(json);
                }
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 显示API文档
     */
    private void showAPIDocumentation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("API 参考手册");

        TextView textView = new TextView(this);
        textView.setText("LayoutAlgorithm.layout(flowChart, params)\n\n" +
                "返回: { nodePositions, edgePaths, canvasWidth, canvasHeight }\n\n" +
                "阶段方法:\n" +
                "- placeNodes(nodes, edges, params)\n" +
                "- generateChannels(nodePositions, edges, nodes, params)\n" +
                "- routeEdges(nodePositions, channels, nodes, params)");
        textView.setPadding(16, 16, 16, 16);
        textView.setTextSize(14);
        builder.setView(textView);
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    private void updateStatus() {
        if (currentFlowChart == null) return;
        int nodeCount = currentFlowChart.getNodes().size();
        int edgeCount = currentFlowChart.getEdges().size();
        statusText.setText("就绪 | 节点: " + nodeCount + " | 边: " + edgeCount);
    }

    private void updateStatus(String text) {
        statusText.setText(text);
    }

    private String loadAssetFile(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            is.close();
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            toggleDrawer();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
