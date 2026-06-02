/**
 * 分层布局算法
 * 适用于控制流图，类似IDA Pro的布局风格
 */

var LayoutAlgorithm = {
    /**
     * 阶段1：放置基本块 - 使用分层布局
     */
    placeNodes: function(nodes, edges, params) {
        params = params || {};
        var positions = {};
        var levelHeight = params.levelHeight || 120;
        var nodeSpacing = params.nodeSpacing || 40;
        var gridSize = params.gridSize || 10;
        
        // 构建图结构
        var graph = this.buildGraph(nodes, edges);
        
        // 计算拓扑层级
        var levels = this.assignLevels(graph, nodes);
        
        // 按层级分组
        var levelGroups = {};
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var level = levels[node.id];
            if (!levelGroups[level]) {
                levelGroups[level] = [];
            }
            levelGroups[level].push(node);
        }
        
        // 在每个层级内进行排序（减少交叉）
        this.orderNodesInLevels(levelGroups, levels, edges);
        
        // 计算位置
        var maxLevel = 0;
        for (var nodeId in levels) {
            maxLevel = Math.max(maxLevel, levels[nodeId]);
        }
        
        for (var level = 0; level <= maxLevel; level++) {
            var levelNodes = levelGroups[level] || [];
            var totalWidth = 0;
            
            for (var i = 0; i < levelNodes.length; i++) {
                totalWidth += levelNodes[i].width;
                if (i > 0) totalWidth += nodeSpacing;
            }
            
            var startX = 50;
            var currentX = startX;
            var y = 50 + level * levelHeight;
            
            for (var i = 0; i < levelNodes.length; i++) {
                var node = levelNodes[i];
                positions[node.id] = {
                    x: Math.round(currentX / gridSize) * gridSize,
                    y: Math.round(y / gridSize) * gridSize
                };
                currentX += node.width + nodeSpacing;
            }
        }
        
        return positions;
    },
    
    /**
     * 构建图结构
     */
    buildGraph: function(nodes, edges) {
        var graph = {
            nodes: {},
            edges: []
        };
        
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            graph.nodes[node.id] = {
                id: node.id,
                predecessors: [],
                successors: []
            };
        }
        
        for (var i = 0; i < edges.length; i++) {
            var edge = edges[i];
            graph.edges.push(edge);
            
            if (graph.nodes[edge.sourceId]) {
                graph.nodes[edge.sourceId].successors.push(edge.targetId);
            }
            if (graph.nodes[edge.targetId]) {
                graph.nodes[edge.targetId].predecessors.push(edge.sourceId);
            }
        }
        
        return graph;
    },
    
    /**
     * 分配层级
     */
    assignLevels: function(graph, nodes) {
        var levels = {};
        var visited = {};
        var visiting = {};
        
        // 初始化
        for (var i = 0; i < nodes.length; i++) {
            levels[nodes[i].id] = 0;
        }
        
        // DFS分配层级
        var assignLevel = function(nodeId, level) {
            if (visiting[nodeId]) {
                // 检测到循环，打破循环
                return;
            }
            if (visited[nodeId]) {
                levels[nodeId] = Math.max(levels[nodeId], level);
                return;
            }
            
            visiting[nodeId] = true;
            levels[nodeId] = level;
            
            var node = graph.nodes[nodeId];
            if (node) {
                for (var i = 0; i < node.successors.length; i++) {
                    assignLevel(node.successors[i], level + 1);
                }
            }
            
            visiting[nodeId] = false;
            visited[nodeId] = true;
        };
        
        // 从所有没有前驱的节点开始
        for (var nodeId in graph.nodes) {
            var node = graph.nodes[nodeId];
            if (node.predecessors.length === 0) {
                assignLevel(nodeId, 0);
            }
        }
        
        // 处理未被访问的节点（循环中的节点）
        for (var i = 0; i < nodes.length; i++) {
            if (!visited[nodes[i].id]) {
                assignLevel(nodes[i].id, 0);
            }
        }
        
        return levels;
    },
    
    /**
     * 在层级内排序节点（减少边交叉）
     */
    orderNodesInLevels: function(levelGroups, levels, edges) {
        // 简单的启发式排序：根据前驱节点的平均位置排序
        for (var level in levelGroups) {
            var levelNodes = levelGroups[level];
            
            levelNodes.sort(function(a, b) {
                var aPreds = [];
                var bPreds = [];
                
                for (var i = 0; i < edges.length; i++) {
                    var edge = edges[i];
                    if (edge.targetId === a.id) {
                        aPreds.push(edge.sourceId);
                    }
                    if (edge.targetId === b.id) {
                        bPreds.push(edge.sourceId);
                    }
                }
                
                // 根据前驱数量排序（简单的启发式）
                return aPreds.length - bPreds.length;
            });
        }
    },
    
    /**
     * 阶段2：生成管道
     */
    generateChannels: function(nodePositions, edges, nodes, params) {
        params = params || {};
        var channels = {};
        var channelOffset = params.channelOffset || 10;
        
        var nodeMap = {};
        for (var i = 0; i < nodes.length; i++) {
            nodeMap[nodes[i].id] = nodes[i];
        }
        
        for (var i = 0; i < edges.length; i++) {
            var edge = edges[i];
            var sourcePos = nodePositions[edge.sourceId];
            var targetPos = nodePositions[edge.targetId];
            var sourceNode = nodeMap[edge.sourceId];
            var targetNode = nodeMap[edge.targetId];
            
            if (!sourcePos || !targetPos || !sourceNode || !targetNode) continue;
            
            // 确定连接点
            var startX = sourcePos.x + sourceNode.width / 2;
            var startY = sourcePos.y + sourceNode.height;
            var endX = targetPos.x + targetNode.width / 2;
            var endY = targetPos.y;
            
            // 处理反向边（循环）
            if (sourcePos.y > targetPos.y) {
                startY = sourcePos.y;
                endY = targetPos.y + targetNode.height;
            }
            
            // 处理同一层级的边
            if (Math.abs(sourcePos.y - targetPos.y) < 10) {
                if (sourcePos.x < targetPos.x) {
                    startX = sourcePos.x + sourceNode.width;
                    endX = targetPos.x;
                } else {
                    startX = sourcePos.x;
                    endX = targetPos.x + targetNode.width;
                }
            }
            
            var midY = (startY + endY) / 2;
            
            // 创建正交路径
            channels[edge.id] = [
                { x: startX, y: startY },
                { x: startX, y: midY },
                { x: endX, y: midY },
                { x: endX, y: endY }
            ];
        }
        
        return channels;
    },
    
    /**
     * 阶段3：A*寻路
     */
    routeEdges: function(nodePositions, channels, nodes, params) {
        params = params || {};
        var paths = {};
        var gridSize = params.gridSize || 10;
        
        var obstacles = this.buildObstacles(nodes, nodePositions);
        
        for (var edgeId in channels) {
            var channel = channels[edgeId];
            if (channel.length < 2) continue;
            
            var start = channel[0];
            var end = channel[channel.length - 1];
            var waypoints = channel.slice(1, -1);
            
            paths[edgeId] = this.routeWithObstacles(start, end, waypoints, obstacles, gridSize);
        }
        
        return paths;
    },
    
    /**
     * 构建障碍物地图
     */
    buildObstacles: function(nodes, positions) {
        var obstacles = [];
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var pos = positions[node.id];
            if (pos) {
                obstacles.push({
                    left: pos.x - 5,
                    top: pos.y - 5,
                    right: pos.x + node.width + 5,
                    bottom: pos.y + node.height + 5
                });
            }
        }
        return obstacles;
    },
    
    /**
     * 带障碍物的路由
     */
    routeWithObstacles: function(start, end, waypoints, obstacles, gridSize) {
        var path = [start];
        var current = start;
        
        for (var i = 0; i < waypoints.length; i++) {
            var waypoint = waypoints[i];
            var segment = this.findPath(current, waypoint, obstacles);
            path = path.concat(segment.slice(1));
            current = waypoint;
        }
        
        var finalSegment = this.findPath(current, end, obstacles);
        path = path.concat(finalSegment.slice(1));
        
        return path;
    },
    
    /**
     * 查找两点之间的路径（避开障碍物）
     */
    findPath: function(start, end, obstacles) {
        // 简化的正交路径
        var path = [start];
        
        // 尝试直接路径
        if (this.isPathClear(start, { x: end.x, y: start.y }, obstacles) &&
            this.isPathClear({ x: end.x, y: start.y }, end, obstacles)) {
            path.push({ x: end.x, y: start.y });
            path.push(end);
            return path;
        }
        
        // 尝试另一种路径
        if (this.isPathClear(start, { x: start.x, y: end.y }, obstacles) &&
            this.isPathClear({ x: start.x, y: end.y }, end, obstacles)) {
            path.push({ x: start.x, y: end.y });
            path.push(end);
            return path;
        }
        
        // 默认正交路径
        path.push({ x: end.x, y: start.y });
        path.push(end);
        return path;
    },
    
    /**
     * 检查路径是否畅通
     */
    isPathClear: function(start, end, obstacles) {
        // 简化的碰撞检测
        for (var i = 0; i < obstacles.length; i++) {
            var obs = obstacles[i];
            // 检查线段是否与障碍物相交
            if (this.lineIntersectsRect(start, end, obs)) {
                return false;
            }
        }
        return true;
    },
    
    /**
     * 检查线段是否与矩形相交
     */
    lineIntersectsRect: function(start, end, rect) {
        // 简化的相交检测
        var minX = Math.min(start.x, end.x);
        var maxX = Math.max(start.x, end.x);
        var minY = Math.min(start.y, end.y);
        var maxY = Math.max(start.y, end.y);
        
        return !(maxX < rect.left || minX > rect.right ||
                 maxY < rect.top || minY > rect.bottom);
    },
    
    /**
     * 执行完整布局
     */
    layout: function(flowChart, params) {
        params = params || {};
        var nodes = flowChart.nodes || [];
        var edges = flowChart.edges || [];
        
        var nodePositions = this.placeNodes(nodes, edges, params);
        var channels = this.generateChannels(nodePositions, edges, nodes, params);
        var edgePaths = this.routeEdges(nodePositions, channels, nodes, params);
        
        var maxX = 0, maxY = 0;
        for (var nodeId in nodePositions) {
            var pos = nodePositions[nodeId];
            var node = nodes.find(function(n) { return n.id === nodeId; });
            if (node) {
                maxX = Math.max(maxX, pos.x + node.width);
                maxY = Math.max(maxY, pos.y + node.height);
            }
        }
        
        return {
            nodePositions: nodePositions,
            edgePaths: edgePaths,
            canvasWidth: maxX + 100,
            canvasHeight: maxY + 100,
            metadata: {
                algorithm: 'hierarchical',
                timestamp: new Date().toISOString(),
                nodeCount: nodes.length,
                edgeCount: edges.length
            }
        };
    }
};
