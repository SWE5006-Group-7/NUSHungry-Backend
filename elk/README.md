# ELK Stack - 集中式日志系统

本目录包含NUSHungry微服务架构的ELK Stack（Elasticsearch、Logstash、Kibana）配置。

## 🚀 快速开始

### Windows
```bash
start-elk.bat
```

### Linux/Mac
```bash
chmod +x start-elk.sh
./start-elk.sh
```

## 📊 访问地址

- **Kibana UI**: http://localhost:5601
- **Elasticsearch API**: http://localhost:9200
- **Logstash Metrics**: http://localhost:9600

## 📁 目录结构

```
elk/
├── docker-compose.yml          # ELK Stack Docker编排
├── logstash/
│   ├── config/
│   │   └── logstash.yml       # Logstash配置
│   └── pipeline/
│       └── logstash.conf      # 日志处理管道
├── filebeat/
│   └── filebeat.yml           # Filebeat配置
├── start-elk.sh/bat           # 启动脚本
├── stop-elk.sh/bat            # 停止脚本
└── README.md                   # 本文件
```

## 📖 详细文档

完整的使用文档请参阅: [docs/CENTRALIZED_LOGGING.md](../docs/CENTRALIZED_LOGGING.md)

文档内容包括：
- 架构说明
- 配置详解
- 使用指南
- 查询示例
- 故障排查
- 最佳实践

## ⚙️ 配置说明

### Elasticsearch
- 端口: 9200 (HTTP), 9300 (Transport)
- 堆内存: 512MB（可在docker-compose.yml中调整）
- 数据卷: `elasticsearch-data`

### Logstash
- TCP输入端口: 5000
- Metrics端口: 9600
- Pipeline: JSON日志处理和过滤

### Kibana
- 端口: 5601
- 连接Elasticsearch: http://elasticsearch:9200

### Filebeat
- 收集Docker容器日志
- 输出到Logstash: 5044端口

## 🛠️ 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 完全删除（包括数据）
docker-compose down -v
```

## 💡 使用提示

1. **首次使用**需要在Kibana中创建索引模式 `nushungry-logs-*`
2. 微服务会自动将日志发送到Logstash（需要配置logback-spring.xml）
3. 推荐至少分配4GB内存给Docker

## 🔧 故障排查

### Elasticsearch启动失败
```bash
# Linux系统需要设置虚拟内存
sudo sysctl -w vm.max_map_count=262144
```

### 内存不足
```yaml
# 修改docker-compose.yml中的ES_JAVA_OPTS
ES_JAVA_OPTS=-Xms1g -Xmx1g
```

### 端口冲突
检查端口9200、5601、5000是否被占用

## 📚 更多资源

- [Elasticsearch文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash文档](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana文档](https://www.elastic.co/guide/en/kibana/current/index.html)
