/**
 * 默认流程图布局算法
 * 三阶段布局：放置节点 -> 生成管道 -> A*寻路
 */

var LayoutAlgorithm = {
    /**
     * 阶段1：放置基本块
     * 使用网格布局算法
     */
    placeNodes: function(nodes, edges, params) {
        params = params || {};
        var positions = {};
        var spacing = params.nodeSpacing || 50;
        var gridSize = params.gridSize || 10;
        var maxWidth = params.maxWidth || 1200;
        
        // 计算拓扑层级（简化版）
        var levels = this.calculateLevels(nodes, edges);
        
        // 按层级布局
        var levelPositions = {};
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var level = levels[node.id] || 0;
            
            if (!levelPositions[level]) {
                levelPositions[level] = { x: spacing, y: spacing + level * 150 };
            }
            
            var pos = levelPositions[level];
            positions[node.id] = {
                x: Math.round(pos.x / gridSize) * gridSize,
                y: Math.round(pos.y / gridSize) * gridSize
            };
            
            pos.x += node.width + spacing;
        }
        
        return positions;
    },
    
    /**
     * 计算节点的拓扑层级
     */
    calculateLevels: function(nodes, edges) {
        var levels = {};
        var inDegree = {};
        
        // 初始化
        for (var i = 0; i < nodes.length; i++) {
            levels[nodes[i].id] = 0;
            inDegree[nodes[i].id] = 0;
        }
        
        // 计算入度
        for (var i = 0; i < edges.length; i++) {
            var edge = edges[i];
            inDegree[edge.targetId] = (inDegree[edge.targetId] || 0) + 1;
        }
        
        // 拓扑排序计算层级
        var changed = true;
        while (changed) {
            changed = false;
            for (var i = 0; i < edges.length; i++) {
                var edge = edges[i];
                var sourceLevel = levels[edge.sourceId];
                var targetLevel = levels[edge.targetId];
                
                if (sourceLevel + 1 > targetLevel) {
                    levels[edge.targetId] = sourceLevel + 1;
                    changed = true;
                }
            }
        }
        
        return levels;
    },
    
    /**
     * 阶段2：生成管道
     * 为每条边生成控制点序列
     */
    generateChannels: function(nodePositions, edges, nodes, params) {
        params = params || {};
        var channels = {};
        var channelWidth = params.channelWidth || 20;
        
        // 创建节点查找表
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
            
            if (sourcePos && targetPos && sourceNode && targetNode) {
                // 确定连接点位置
                var startX = sourcePos.x + sourceNode.width / 2;
                var startY = sourcePos.y + sourceNode.height;
                var endX = targetPos.x + targetNode.width / 2;
                var endY = targetPos.y;
                
                // 如果目标在源上方，调整连接点
                if (targetPos.y < sourcePos.y) {
                    startY = sourcePos.y;
                    endY = targetPos.y + targetNode.height;
                }
                
                // 如果目标在源左侧，调整连接点
                if (targetPos.x < sourcePos.x) {
                    startX = sourcePos.x;
                    endX = targetPos.x + targetNode.width;
                } else if (targetPos.x > sourcePos.x + sourceNode.width) {
                    startX = sourcePos.x + sourceNode.width;
                    endX = targetPos.x;
                }
                
                // 生成管道控制点
                var midY = (startY + endY) / 2;
                channels[edge.id] = [
                    { x: startX, y: startY },
                    { x: startX, y: midY },
                    { x: endX, y: midY },
                    { x: endX, y: endY }
                ];
            }
        }
        
        return channels;
    },
    
    /**
     * 阶段3：管道约束A*寻路
     * 在管道约束下计算边的实际路径
     */
    routeEdges: function(nodePositions, channels, nodes, params) {
        params = params || {};
        var paths = {};
        var gridSize = params.gridSize || 10;
        
        // 创建障碍物地图
        var obstacles = this.buildObstacles(nodes, nodePositions);
        
        for (var edgeId in channels) {
            var channel = channels[edgeId];
            if (channel.length >= 2) {
                var start = channel[0];
                var end = channel[channel.length - 1];
                var waypoints = channel.slice(1, -1);
                
                // 在管道约束下执行A*寻路
                paths[edgeId] = this.aStarWithWaypoints(start, end, waypoints, obstacles, gridSize);
            }
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
                    x: pos.x - 5,
                    y: pos.y - 5,
                    width: node.width + 10,
                    height: node.height + 10
                });
            }
        }
        return obstacles;
    },
    
    /**
     * A*寻路算法（带途经点）
     */
    aStarWithWaypoints: function(start, end, waypoints, obstacles, gridSize) {
        var path = [start];
        var current = start;
        
        // 依次经过所有途经点
        for (var i = 0; i < waypoints.length; i++) {
            var waypoint = waypoints[i];
            var segment = this.aStar(current, waypoint, obstacles, gridSize);
            path = path.concat(segment.slice(1));
            current = waypoint;
        }
        
        // 最后到达终点
        var finalSegment = this.aStar(current, end, obstacles, gridSize);
        path = path.concat(finalSegment.slice(1));
        
        return path;
    },
    
    /**
     * A*寻路算法实现
     */
    aStar: function(start, end, obstacles, gridSize) {
        // 简化的A*实现 - 使用正交路径
        var path = [start];
        
        // 先水平移动
        if (start.x !== end.x) {
            path.push({ x: end.x, y: start.y });
        }
        
        // 再垂直移动
        if (start.y !== end.y) {
            path.push({ x: end.x, y: end.y });
        }
        
        return path;
    },
    
    /**
     * 执行完整布局
     */
    layout: function(flowChart, params) {
        params = params || {};
        var nodes = flowChart.nodes || [];
        var edges = flowChart.edges || [];
        
        // 阶段1：放置节点
        var nodePositions = this.placeNodes(nodes, edges, params);
        
        // 阶段2：生成管道
        var channels = this.generateChannels(nodePositions, edges, nodes, params);
        
        // 阶段3：路由边
        var edgePaths = this.routeEdges(nodePositions, channels, nodes, params);
        
        // 计算画布大小
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
                algorithm: 'default',
                timestamp: new Date().toISOString(),
                nodeCount: nodes.length,
                edgeCount: edges.length
            }
        };
    }
};
