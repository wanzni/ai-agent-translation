-- =====================================================
-- 智能翻译助手 AI Agent 初始化数据脚本
-- 创建时间: 2024-01-15
-- 版本: 1.0.0
-- =====================================================

-- 设置字符集
SET NAMES utf8mb4;

-- =====================================================
-- 1. 初始化支持语言数据
-- =====================================================

INSERT INTO `supported_languages` (`language_code`, `language_name`, `native_name`, `sort_order`) VALUES
('zh', '中文', '中文', 1),
('en', '英语', 'English', 2),
('ja', '日语', '日本語', 3),
('ko', '韩语', '한국어', 4),
('fr', '法语', 'Français', 5),
('de', '德语', 'Deutsch', 6),
('es', '西班牙语', 'Español', 7),
('it', '意大利语', 'Italiano', 8),
('pt', '葡萄牙语', 'Português', 9),
('ru', '俄语', 'Русский', 10),
('ar', '阿拉伯语', 'العربية', 11),
('hi', '印地语', 'हिन्दी', 12),
('th', '泰语', 'ไทย', 13),
('vi', '越南语', 'Tiếng Việt', 14),
('id', '印尼语', 'Bahasa Indonesia', 15),
('ms', '马来语', 'Bahasa Melayu', 16),
('tr', '土耳其语', 'Türkçe', 17),
('pl', '波兰语', 'Polski', 18),
('nl', '荷兰语', 'Nederlands', 19),
('sv', '瑞典语', 'Svenska', 20),
('da', '丹麦语', 'Dansk', 21),
('no', '挪威语', 'Norsk', 22),
('fi', '芬兰语', 'Suomi', 23),
('cs', '捷克语', 'Čeština', 24),
('hu', '匈牙利语', 'Magyar', 25),
('ro', '罗马尼亚语', 'Română', 26),
('bg', '保加利亚语', 'Български', 27),
('hr', '克罗地亚语', 'Hrvatski', 28),
('sk', '斯洛伐克语', 'Slovenčina', 29),
('sl', '斯洛文尼亚语', 'Slovenščina', 30),
('et', '爱沙尼亚语', 'Eesti', 31),
('lv', '拉脱维亚语', 'Latviešu', 32),
('lt', '立陶宛语', 'Lietuvių', 33),
('el', '希腊语', 'Ελληνικά', 34),
('he', '希伯来语', 'עברית', 35),
('fa', '波斯语', 'فارسی', 36),
('ur', '乌尔都语', 'اردو', 37),
('bn', '孟加拉语', 'বাংলা', 38),
('ta', '泰米尔语', 'தமிழ்', 39),
('te', '泰卢固语', 'తెలుగు', 40),
('ml', '马拉雅拉姆语', 'മലയാളം', 41),
('kn', '卡纳达语', 'ಕನ್ನಡ', 42),
('gu', '古吉拉特语', 'ગુજરાતી', 43),
('pa', '旁遮普语', 'ਪੰਜਾਬੀ', 44),
('or', '奥里亚语', 'ଓଡ଼ିଆ', 45),
('as', '阿萨姆语', 'অসমীয়া', 46),
('ne', '尼泊尔语', 'नेपाली', 47),
('si', '僧伽罗语', 'සිංහල', 48),
('my', '缅甸语', 'မြန်မာ', 49),
('km', '高棉语', 'ខ្មែរ', 50),
('lo', '老挝语', 'ລາວ', 51);

-- =====================================================
-- 2. 初始化术语库分类数据
-- =====================================================

INSERT INTO `terminology_categories` (`category_name`, `category_code`, `description`, `sort_order`) VALUES
('技术术语', 'TECHNOLOGY', '计算机、软件、互联网等技术相关术语', 1),
('商业术语', 'BUSINESS', '商业、贸易、经济相关术语', 2),
('医学术语', 'MEDICAL', '医学、健康、生物相关术语', 3),
('法律术语', 'LEGAL', '法律、法规、合同相关术语', 4),
('金融术语', 'FINANCE', '金融、投资、银行相关术语', 5),
('教育术语', 'EDUCATION', '教育、学术、研究相关术语', 6),
('科学术语', 'SCIENCE', '自然科学、工程技术相关术语', 7),
('通用术语', 'GENERAL', '日常通用词汇和短语', 8);

-- =====================================================
-- 3. 初始化翻译引擎配置
-- =====================================================

INSERT INTO `translation_engines` (`engine_name`, `engine_code`, `is_active`, `priority`, `supported_languages`, `config_params`) VALUES
('阿里云机器翻译', 'ALIYUN', 1, 1, '["zh","en","ja","ko","fr","de","es","it","pt","ru","ar"]', '{"region":"cn-hangzhou","endpoint":"mt.cn-hangzhou.aliyuncs.com"}'),
('通义千问', 'DASHSCOPE', 1, 2, '["zh","en","ja","ko","fr","de","es","it","pt","ru"]', '{"model":"qwen-turbo","temperature":0.1}'),
('OpenAI GPT', 'OPENAI', 1, 3, '["zh","en","ja","ko","fr","de","es","it","pt","ru","ar"]', '{"model":"gpt-3.5-turbo","temperature":0.1}'),
('百度翻译', 'BAIDU', 1, 4, '["zh","en","ja","ko","fr","de","es","it","pt","ru","ar"]', '{"appid":"","secret":""}'),
('腾讯翻译', 'TENCENT', 1, 5, '["zh","en","ja","ko","fr","de","es","it","pt","ru"]', '{"region":"ap-beijing","secretId":"","secretKey":""}'),
('有道翻译', 'YOUDAO', 1, 6, '["zh","en","ja","ko","fr","de","es","it","pt","ru"]', '{"appKey":"","appSecret":""}');

-- =====================================================
-- 4. 初始化系统配置
-- =====================================================

INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `description`, `category`) VALUES
-- 翻译相关配置
('translation.default_engine', 'ALIYUN', 'STRING', '默认翻译引擎', 'TRANSLATION'),
('translation.max_text_length', '5000', 'NUMBER', '单次翻译最大文本长度', 'TRANSLATION'),
('translation.max_document_size', '10485760', 'NUMBER', '文档翻译最大文件大小（字节）', 'TRANSLATION'),
('translation.supported_formats', '["pdf","docx","txt","xlsx","pptx","html","xml","json"]', 'JSON', '支持的文档格式', 'TRANSLATION'),
('translation.auto_detect_language', 'true', 'BOOLEAN', '是否启用自动语言检测', 'TRANSLATION'),
('translation.use_terminology_by_default', 'false', 'BOOLEAN', '是否默认使用术语库', 'TRANSLATION'),

-- 质量评估配置
('quality.auto_assessment', 'true', 'BOOLEAN', '是否启用自动质量评估', 'QUALITY'),
('quality.assessment_threshold', '70', 'NUMBER', '质量评估阈值', 'QUALITY'),
('quality.manual_review_threshold', '60', 'NUMBER', '人工审核阈值', 'QUALITY'),

-- 术语库配置
('terminology.max_entries_per_user', '10000', 'NUMBER', '每个用户最大术语条目数', 'TERMINOLOGY'),
('terminology.auto_suggest', 'true', 'BOOLEAN', '是否启用术语自动建议', 'TERMINOLOGY'),
('terminology.import_batch_size', '1000', 'NUMBER', '术语导入批次大小', 'TERMINOLOGY'),

-- 文件存储配置
('storage.type', 'LOCAL', 'STRING', '文件存储类型', 'STORAGE'),
('storage.max_file_size', '52428800', 'NUMBER', '最大文件大小（字节）', 'STORAGE'),
('storage.cleanup_days', '30', 'NUMBER', '文件清理天数', 'STORAGE'),

-- 系统性能配置
('system.max_concurrent_translations', '100', 'NUMBER', '最大并发翻译数', 'SYSTEM'),
('system.cache_ttl', '3600', 'NUMBER', '缓存生存时间（秒）', 'SYSTEM'),
('system.rate_limit_per_minute', '60', 'NUMBER', '每分钟请求限制', 'SYSTEM'),

-- 通知配置
('notification.email_enabled', 'true', 'BOOLEAN', '是否启用邮件通知', 'NOTIFICATION'),
('notification.sms_enabled', 'false', 'BOOLEAN', '是否启用短信通知', 'NOTIFICATION'),
('notification.push_enabled', 'true', 'BOOLEAN', '是否启用推送通知', 'NOTIFICATION');

-- =====================================================
-- 5. 初始化示例术语数据
-- =====================================================

INSERT INTO `terminology_entries` (`source_term`, `target_term`, `source_language`, `target_language`, `category`, `domain`, `notes`, `created_by`) VALUES
-- 技术术语
('人工智能', 'Artificial Intelligence', 'zh', 'en', 'TECHNOLOGY', 'AI', 'AI领域的核心概念', 'system'),
('机器学习', 'Machine Learning', 'zh', 'en', 'TECHNOLOGY', 'AI', 'AI的重要分支', 'system'),
('深度学习', 'Deep Learning', 'zh', 'en', 'TECHNOLOGY', 'AI', '机器学习的一个子领域', 'system'),
('神经网络', 'Neural Network', 'zh', 'en', 'TECHNOLOGY', 'AI', '模拟人脑神经元结构的计算模型', 'system'),
('算法', 'Algorithm', 'zh', 'en', 'TECHNOLOGY', 'Computer Science', '解决问题的步骤和方法', 'system'),
('数据库', 'Database', 'zh', 'en', 'TECHNOLOGY', 'Computer Science', '存储和管理数据的系统', 'system'),
('云计算', 'Cloud Computing', 'zh', 'en', 'TECHNOLOGY', 'Cloud', '基于互联网的计算服务', 'system'),
('大数据', 'Big Data', 'zh', 'en', 'TECHNOLOGY', 'Data Science', '海量、高增长率和多样化的信息资产', 'system'),

-- 商业术语
('商业模式', 'Business Model', 'zh', 'en', 'BUSINESS', 'Strategy', '企业创造、传递和获取价值的方式', 'system'),
('市场分析', 'Market Analysis', 'zh', 'en', 'BUSINESS', 'Marketing', '对市场状况的研究和分析', 'system'),
('竞争优势', 'Competitive Advantage', 'zh', 'en', 'BUSINESS', 'Strategy', '企业在竞争中获得的优势地位', 'system'),
('客户关系管理', 'Customer Relationship Management', 'zh', 'en', 'BUSINESS', 'CRM', '管理客户关系的系统和方法', 'system'),
('供应链管理', 'Supply Chain Management', 'zh', 'en', 'BUSINESS', 'Operations', '管理产品从原材料到最终用户的流程', 'system'),

-- 医学术语
('诊断', 'Diagnosis', 'zh', 'en', 'MEDICAL', 'General', '确定疾病或病症的过程', 'system'),
('治疗', 'Treatment', 'zh', 'en', 'MEDICAL', 'General', '医疗干预措施', 'system'),
('症状', 'Symptom', 'zh', 'en', 'MEDICAL', 'General', '疾病或病症的表现', 'system'),
('处方', 'Prescription', 'zh', 'en', 'MEDICAL', 'Pharmacy', '医生开具的用药指导', 'system'),
('手术', 'Surgery', 'zh', 'en', 'MEDICAL', 'Surgery', '外科医疗操作', 'system'),

-- 法律术语
('合同', 'Contract', 'zh', 'en', 'LEGAL', 'Contract Law', '双方或多方之间的法律协议', 'system'),
('知识产权', 'Intellectual Property', 'zh', 'en', 'LEGAL', 'IP Law', '智力创造的法律保护', 'system'),
('法律责任', 'Legal Liability', 'zh', 'en', 'LEGAL', 'General', '法律规定的责任和义务', 'system'),
('诉讼', 'Litigation', 'zh', 'en', 'LEGAL', 'Civil Law', '通过法院解决争议的过程', 'system'),
('仲裁', 'Arbitration', 'zh', 'en', 'LEGAL', 'Dispute Resolution', '通过第三方解决争议的方式', 'system'),

-- 金融术语
('投资', 'Investment', 'zh', 'en', 'FINANCE', 'Investment', '将资金投入以获取收益的行为', 'system'),
('风险管理', 'Risk Management', 'zh', 'en', 'FINANCE', 'Risk', '识别、评估和控制风险的过程', 'system'),
('资产配置', 'Asset Allocation', 'zh', 'en', 'FINANCE', 'Portfolio', '投资组合中各类资产的比例分配', 'system'),
('收益率', 'Return Rate', 'zh', 'en', 'FINANCE', 'Investment', '投资获得的收益比率', 'system'),
('流动性', 'Liquidity', 'zh', 'en', 'FINANCE', 'General', '资产转换为现金的难易程度', 'system');

-- =====================================================
-- 6. 初始化示例用户数据
-- =====================================================

INSERT INTO `users` (`username`, `email`, `password_hash`, `nickname`, `language_preference`, `role`) VALUES
('admin', 'admin@ai-translator.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '系统管理员', 'zh', 'SUPER_ADMIN'),
('demo_user', 'demo@ai-translator.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '演示用户', 'zh', 'USER'),
('test_user', 'test@ai-translator.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '测试用户', 'en', 'USER');

-- =====================================================
-- 7. 初始化用户配置数据
-- =====================================================

INSERT INTO `user_settings` (`user_id`, `setting_key`, `setting_value`, `setting_type`) VALUES
(1, 'default_source_language', 'zh', 'STRING'),
(1, 'default_target_language', 'en', 'STRING'),
(1, 'auto_translate', 'true', 'BOOLEAN'),
(1, 'use_terminology', 'true', 'BOOLEAN'),
(1, 'quality_assessment', 'true', 'BOOLEAN'),
(1, 'notification_email', 'true', 'BOOLEAN'),
(1, 'theme', 'light', 'STRING'),
(1, 'font_size', '14', 'NUMBER'),

(2, 'default_source_language', 'zh', 'STRING'),
(2, 'default_target_language', 'en', 'STRING'),
(2, 'auto_translate', 'true', 'BOOLEAN'),
(2, 'use_terminology', 'false', 'BOOLEAN'),
(2, 'quality_assessment', 'true', 'BOOLEAN'),
(2, 'notification_email', 'false', 'BOOLEAN'),
(2, 'theme', 'dark', 'STRING'),
(2, 'font_size', '16', 'NUMBER'),

(3, 'default_source_language', 'en', 'STRING'),
(3, 'default_target_language', 'zh', 'STRING'),
(3, 'auto_translate', 'false', 'BOOLEAN'),
(3, 'use_terminology', 'true', 'BOOLEAN'),
(3, 'quality_assessment', 'false', 'BOOLEAN'),
(3, 'notification_email', 'true', 'BOOLEAN'),
(3, 'theme', 'light', 'STRING'),
(3, 'font_size', '12', 'NUMBER');

-- =====================================================
-- 8. 初始化系统通知数据
-- =====================================================

INSERT INTO `system_notifications` (`title`, `content`, `notification_type`, `priority`, `is_global`, `is_active`, `publish_time`) VALUES
('欢迎使用智能翻译助手', '欢迎使用基于Spring AI的智能翻译助手！我们支持文本翻译、文档翻译和实时对话翻译，让语言不再是沟通的障碍。', 'SYSTEM', 'NORMAL', 1, 1, NOW()),
('新功能上线：术语库管理', '我们新增了术语库管理功能，您可以创建和管理专业术语，提高翻译的准确性和一致性。', 'SYSTEM', 'NORMAL', 1, 1, NOW()),
('质量评估功能优化', '翻译质量评估功能已优化，现在可以更准确地评估翻译质量并提供改进建议。', 'QUALITY', 'NORMAL', 1, 1, NOW()),
('支持更多语言', '我们新增了对阿拉伯语、印地语、泰语等20多种语言的支持，让翻译更加全面。', 'SYSTEM', 'NORMAL', 1, 1, NOW());

-- =====================================================
-- 9. 初始化示例翻译记录数据
-- =====================================================

INSERT INTO `translation_records` (`user_id`, `source_language`, `target_language`, `source_text`, `translated_text`, `translation_type`, `translation_engine`, `quality_score`, `processing_time`, `character_count`, `use_terminology`) VALUES
('demo_user', 'zh', 'en', '你好，欢迎使用智能翻译助手！', 'Hello, welcome to use the intelligent translation assistant!', 'TEXT', 'ALIYUN', 95, 1200, 15, 1),
('demo_user', 'en', 'zh', 'This is a powerful AI translation system.', '这是一个强大的AI翻译系统。', 'TEXT', 'DASHSCOPE', 92, 1500, 35, 1),
('test_user', 'zh', 'ja', '人工智能技术正在快速发展。', '人工知能技術は急速に発展している。', 'TEXT', 'ALIYUN', 88, 1800, 12, 0),
('demo_user', 'zh', 'ko', '机器学习是人工智能的重要分支。', '머신러닝은 인공지능의 중요한 분야입니다.', 'TEXT', 'ALIYUN', 90, 1600, 16, 1);

-- =====================================================
-- 10. 初始化示例质量评估数据
-- =====================================================

INSERT INTO `quality_assessments` (`translation_record_id`, `assessment_mode`, `overall_score`, `accuracy_score`, `fluency_score`, `consistency_score`, `completeness_score`, `improvement_suggestions`, `attention_points`, `strengths`, `assessment_engine`) VALUES
(1, 'AUTOMATIC', 95, 98, 92, 95, 100, '["翻译准确，语言流畅","建议保持一致性"]', '["注意标点符号的使用"]', '["语法正确","用词恰当","表达自然"]', 'ALIYUN_QA'),
(2, 'AUTOMATIC', 92, 95, 90, 90, 95, '["翻译基本准确","可以更加流畅"]', '["注意专业术语的翻译"]', '["意思传达准确","结构清晰"]', 'DASHSCOPE_QA'),
(3, 'AUTOMATIC', 88, 90, 85, 90, 90, '["翻译准确度较高","语言表达可以更自然"]', '["注意日语敬语的使用"]', '["专业术语翻译准确","语法正确"]', 'ALIYUN_QA'),
(4, 'AUTOMATIC', 90, 92, 88, 90, 95, '["翻译质量良好","可以更加流畅"]', '["注意韩语语序的调整"]', '["专业术语准确","意思完整"]', 'ALIYUN_QA');

-- =====================================================
-- 11. 初始化使用统计数据
-- =====================================================

INSERT INTO `usage_statistics` (`user_id`, `stat_date`, `translation_count`, `character_count`, `document_count`, `chat_message_count`, `terminology_usage_count`) VALUES
('demo_user', CURDATE(), 15, 2500, 3, 25, 8),
('test_user', CURDATE(), 8, 1200, 1, 12, 3),
('demo_user', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 12, 1800, 2, 18, 5),
('test_user', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 6, 900, 0, 8, 2);

-- 提交事务
COMMIT;

