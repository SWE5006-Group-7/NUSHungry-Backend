#!/usr/bin/env python3
"""
MySQL to MongoDB Data Migration Script for Review Service
从 MySQL 数据库迁移评价数据到 MongoDB

Usage:
    python migrate_reviews_to_mongodb.py [--dry-run] [--batch-size 100]

Requirements:
    pip install pymysql pymongo
"""

import pymysql
import pymongo
from datetime import datetime
from typing import List, Dict, Any
import json
import argparse
import sys

# ==================== 配置 ====================
MYSQL_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'nushungry_db',
    'charset': 'utf8mb4'
}

MONGODB_CONFIG = {
    'uri': 'mongodb://localhost:27017/',
    'database': 'review_service'
}

# ==================== 数据库连接 ====================
def get_mysql_connection():
    """创建 MySQL 连接"""
    try:
        conn = pymysql.connect(**MYSQL_CONFIG)
        print(f"✓ 成功连接到 MySQL: {MYSQL_CONFIG['host']}:{MYSQL_CONFIG['port']}")
        return conn
    except Exception as e:
        print(f"✗ MySQL 连接失败: {e}")
        sys.exit(1)

def get_mongodb_client():
    """创建 MongoDB 客户端"""
    try:
        client = pymongo.MongoClient(MONGODB_CONFIG['uri'])
        # 测试连接
        client.server_info()
        print(f"✓ 成功连接到 MongoDB: {MONGODB_CONFIG['uri']}")
        return client
    except Exception as e:
        print(f"✗ MongoDB 连接失败: {e}")
        sys.exit(1)

# ==================== 数据迁移函数 ====================
def fetch_reviews_from_mysql(mysql_conn) -> List[Dict[str, Any]]:
    """从 MySQL 获取所有评价数据"""
    cursor = mysql_conn.cursor(pymysql.cursors.DictCursor)
    
    # 查询评价数据（联表查询用户信息和摊位信息）
    query = """
    SELECT 
        r.id,
        r.stall_id,
        s.name as stall_name,
        r.user_id,
        u.username,
        u.avatar_url as user_avatar_url,
        r.rating,
        r.comment,
        r.image_urls,
        r.total_cost,
        r.number_of_people,
        r.likes_count,
        r.created_at,
        r.updated_at
    FROM review r
    LEFT JOIN users u ON r.user_id = u.id
    LEFT JOIN stall s ON r.stall_id = s.id
    WHERE r.moderation_status = 'APPROVED'
    ORDER BY r.created_at DESC
    """
    
    cursor.execute(query)
    reviews = cursor.fetchall()
    cursor.close()
    
    print(f"✓ 从 MySQL 查询到 {len(reviews)} 条评价记录")
    return reviews

def fetch_review_likes_from_mysql(mysql_conn) -> List[Dict[str, Any]]:
    """从 MySQL 获取所有点赞数据"""
    cursor = mysql_conn.cursor(pymysql.cursors.DictCursor)
    
    query = """
    SELECT 
        id,
        review_id,
        user_id,
        created_at
    FROM review_likes
    ORDER BY created_at DESC
    """
    
    cursor.execute(query)
    likes = cursor.fetchall()
    cursor.close()
    
    print(f"✓ 从 MySQL 查询到 {len(likes)} 条点赞记录")
    return likes

def fetch_review_reports_from_mysql(mysql_conn) -> List[Dict[str, Any]]:
    """从 MySQL 获取所有举报数据"""
    cursor = mysql_conn.cursor(pymysql.cursors.DictCursor)
    
    query = """
    SELECT 
        id,
        review_id,
        reporter_id as reporter_id,
        reason,
        description,
        status,
        moderator_id as handled_by,
        created_at,
        moderated_at as handled_at
    FROM review_reports
    ORDER BY created_at DESC
    """
    
    cursor.execute(query)
    reports = cursor.fetchall()
    cursor.close()
    
    print(f"✓ 从 MySQL 查询到 {len(reports)} 条举报记录")
    return reports

def transform_review_to_mongodb(review: Dict[str, Any]) -> Dict[str, Any]:
    """转换 Review 数据格式为 MongoDB 文档"""
    # 处理 JSON 类型的 image_urls 字段
    image_urls = []
    if review.get('image_urls'):
        if isinstance(review['image_urls'], str):
            image_urls = json.loads(review['image_urls'])
        elif isinstance(review['image_urls'], list):
            image_urls = review['image_urls']
    
    # 构建 MongoDB 文档
    doc = {
        '_id': str(review['id']),  # 使用原 MySQL ID 作为 MongoDB _id
        'stallId': review['stall_id'],
        'stallName': review.get('stall_name', ''),
        'userId': review['user_id'],
        'username': review.get('username', ''),
        'userAvatarUrl': review.get('user_avatar_url', ''),
        'rating': float(review['rating']),
        'comment': review['comment'],
        'imageUrls': image_urls,
        'totalCost': float(review['total_cost']) if review.get('total_cost') else None,
        'numberOfPeople': int(review['number_of_people']) if review.get('number_of_people') else None,
        'likesCount': int(review.get('likes_count', 0)),
        'createdAt': review['created_at'] if isinstance(review['created_at'], datetime) else datetime.fromisoformat(str(review['created_at'])),
        'updatedAt': review['updated_at'] if isinstance(review['updated_at'], datetime) else datetime.fromisoformat(str(review['updated_at']))
    }
    
    return doc

def transform_like_to_mongodb(like: Dict[str, Any]) -> Dict[str, Any]:
    """转换 ReviewLike 数据格式为 MongoDB 文档"""
    return {
        '_id': str(like['id']),
        'reviewId': str(like['review_id']),
        'userId': like['user_id'],
        'createdAt': like['created_at'] if isinstance(like['created_at'], datetime) else datetime.fromisoformat(str(like['created_at']))
    }

def transform_report_to_mongodb(report: Dict[str, Any]) -> Dict[str, Any]:
    """转换 ReviewReport 数据格式为 MongoDB 文档"""
    # 映射举报原因
    reason_map = {
        'SPAM': 'SPAM',
        'OFFENSIVE': 'OFFENSIVE',
        'FAKE': 'FAKE',
        'OTHER': 'OTHER'
    }
    
    # 映射处理状态
    status_map = {
        'PENDING': 'PENDING',
        'APPROVED': 'APPROVED',
        'REJECTED': 'REJECTED',
        'IGNORED': 'IGNORED'
    }
    
    doc = {
        '_id': str(report['id']),
        'reviewId': str(report['review_id']),
        'reporterId': report['reporter_id'],
        'reporterName': '',  # MySQL 中没有存储，需要后续补充
        'reason': reason_map.get(report.get('reason', 'OTHER'), 'OTHER'),
        'description': report.get('description', ''),
        'status': status_map.get(report.get('status', 'PENDING'), 'PENDING'),
        'handledBy': report.get('handled_by'),
        'handledAt': report.get('handled_at'),
        'handleNote': '',  # MySQL 中没有此字段
        'createdAt': report['created_at'] if isinstance(report['created_at'], datetime) else datetime.fromisoformat(str(report['created_at']))
    }
    
    return doc

def insert_to_mongodb_batch(collection, documents: List[Dict[str, Any]], batch_size: int = 100):
    """批量插入数据到 MongoDB"""
    total = len(documents)
    inserted = 0
    errors = 0
    
    for i in range(0, total, batch_size):
        batch = documents[i:i + batch_size]
        try:
            # 使用 insert_many 批量插入，ordered=False 允许部分失败继续插入
            result = collection.insert_many(batch, ordered=False)
            inserted += len(result.inserted_ids)
        except pymongo.errors.BulkWriteError as e:
            # 处理重复键错误
            inserted += e.details['nInserted']
            errors += len(e.details['writeErrors'])
            for error in e.details['writeErrors']:
                if error['code'] != 11000:  # 11000 是重复键错误
                    print(f"  ⚠ 插入错误: {error}")
        except Exception as e:
            print(f"  ✗ 批量插入失败: {e}")
            errors += len(batch)
    
    return inserted, errors

def create_mongodb_indexes(db):
    """创建 MongoDB 索引"""
    print("\n正在创建 MongoDB 索引...")
    
    # Review 集合索引
    review_collection = db['review']
    review_collection.create_index([('stallId', pymongo.ASCENDING), ('createdAt', pymongo.DESCENDING)])
    review_collection.create_index([('userId', pymongo.ASCENDING), ('createdAt', pymongo.DESCENDING)])
    review_collection.create_index([('stallId', pymongo.ASCENDING), ('likesCount', pymongo.DESCENDING)])
    review_collection.create_index([('rating', pymongo.ASCENDING)])
    print("  ✓ Review 集合索引创建完成")
    
    # ReviewLike 集合索引
    like_collection = db['reviewLike']
    like_collection.create_index([('reviewId', pymongo.ASCENDING), ('userId', pymongo.ASCENDING)], unique=True)
    like_collection.create_index([('reviewId', pymongo.ASCENDING)])
    print("  ✓ ReviewLike 集合索引创建完成")
    
    # ReviewReport 集合索引
    report_collection = db['reviewReport']
    report_collection.create_index([('reviewId', pymongo.ASCENDING)])
    report_collection.create_index([('status', pymongo.ASCENDING)])
    report_collection.create_index([('reporterId', pymongo.ASCENDING)])
    print("  ✓ ReviewReport 集合索引创建完成")

def verify_migration(mysql_conn, mongo_db):
    """验证数据迁移结果"""
    print("\n正在验证数据迁移...")
    
    cursor = mysql_conn.cursor()
    
    # 验证评价数量
    cursor.execute("SELECT COUNT(*) FROM review WHERE moderation_status = 'APPROVED'")
    mysql_review_count = cursor.fetchone()[0]
    mongo_review_count = mongo_db['review'].count_documents({})
    print(f"  评价数量: MySQL={mysql_review_count}, MongoDB={mongo_review_count}")
    
    # 验证点赞数量
    cursor.execute("SELECT COUNT(*) FROM review_likes")
    mysql_like_count = cursor.fetchone()[0]
    mongo_like_count = mongo_db['reviewLike'].count_documents({})
    print(f"  点赞数量: MySQL={mysql_like_count}, MongoDB={mongo_like_count}")
    
    # 验证举报数量
    cursor.execute("SELECT COUNT(*) FROM review_reports")
    mysql_report_count = cursor.fetchone()[0]
    mongo_report_count = mongo_db['reviewReport'].count_documents({})
    print(f"  举报数量: MySQL={mysql_report_count}, MongoDB={mongo_report_count}")
    
    cursor.close()
    
    if mysql_review_count == mongo_review_count and mysql_like_count == mongo_like_count and mysql_report_count == mongo_report_count:
        print("  ✓ 数据验证通过")
        return True
    else:
        print("  ⚠ 数据数量不匹配，请检查")
        return False

# ==================== 主函数 ====================
def main():
    parser = argparse.ArgumentParser(description='迁移评价数据从 MySQL 到 MongoDB')
    parser.add_argument('--dry-run', action='store_true', help='模拟运行，不实际插入数据')
    parser.add_argument('--batch-size', type=int, default=100, help='批量插入大小（默认100）')
    parser.add_argument('--clean', action='store_true', help='迁移前清空 MongoDB 目标集合')
    args = parser.parse_args()
    
    print("=" * 60)
    print("评价服务数据迁移工具 (MySQL → MongoDB)")
    print("=" * 60)
    
    if args.dry_run:
        print("⚠ 模拟运行模式（Dry Run）")
    
    # 连接数据库
    mysql_conn = get_mysql_connection()
    mongo_client = get_mongodb_client()
    mongo_db = mongo_client[MONGODB_CONFIG['database']]
    
    try:
        # 清空目标集合（如果指定）
        if args.clean and not args.dry_run:
            print("\n正在清空 MongoDB 目标集合...")
            mongo_db['review'].delete_many({})
            mongo_db['reviewLike'].delete_many({})
            mongo_db['reviewReport'].delete_many({})
            print("  ✓ 目标集合已清空")
        
        # ==================== 迁移 Review ====================
        print("\n[1/3] 迁移评价数据...")
        reviews = fetch_reviews_from_mysql(mysql_conn)
        review_docs = [transform_review_to_mongodb(r) for r in reviews]
        
        if not args.dry_run:
            inserted, errors = insert_to_mongodb_batch(mongo_db['review'], review_docs, args.batch_size)
            print(f"  ✓ 插入 {inserted} 条评价记录, 失败 {errors} 条")
        else:
            print(f"  [Dry Run] 将插入 {len(review_docs)} 条评价记录")
        
        # ==================== 迁移 ReviewLike ====================
        print("\n[2/3] 迁移点赞数据...")
        likes = fetch_review_likes_from_mysql(mysql_conn)
        like_docs = [transform_like_to_mongodb(l) for l in likes]
        
        if not args.dry_run:
            inserted, errors = insert_to_mongodb_batch(mongo_db['reviewLike'], like_docs, args.batch_size)
            print(f"  ✓ 插入 {inserted} 条点赞记录, 失败 {errors} 条")
        else:
            print(f"  [Dry Run] 将插入 {len(like_docs)} 条点赞记录")
        
        # ==================== 迁移 ReviewReport ====================
        print("\n[3/3] 迁移举报数据...")
        reports = fetch_review_reports_from_mysql(mysql_conn)
        report_docs = [transform_report_to_mongodb(r) for r in reports]
        
        if not args.dry_run:
            inserted, errors = insert_to_mongodb_batch(mongo_db['reviewReport'], report_docs, args.batch_size)
            print(f"  ✓ 插入 {inserted} 条举报记录, 失败 {errors} 条")
        else:
            print(f"  [Dry Run] 将插入 {len(report_docs)} 条举报记录")
        
        # ==================== 创建索引 ====================
        if not args.dry_run:
            create_mongodb_indexes(mongo_db)
        else:
            print("\n[Dry Run] 将创建 MongoDB 索引")
        
        # ==================== 验证数据 ====================
        if not args.dry_run:
            verify_migration(mysql_conn, mongo_db)
        
        print("\n" + "=" * 60)
        print("✓ 数据迁移完成")
        print("=" * 60)
        
    except Exception as e:
        print(f"\n✗ 数据迁移失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        mysql_conn.close()
        mongo_client.close()

if __name__ == '__main__':
    main()
