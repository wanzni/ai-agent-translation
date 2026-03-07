package cn.net.susan.ai.translation.controller;

import cn.net.susan.ai.translation.config.PayProviderProperties;
import cn.net.susan.ai.translation.entity.PaymentRecord;
import cn.net.susan.ai.translation.repository.PaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 页面控制器
 * 处理页面路由和模板渲染
 * 
 * @author 苏三
 * @version 1.0
 * @since 2024-01-15
 */
@Controller
@RequiredArgsConstructor
public class PageController {
    private final PaymentRecordRepository paymentRecordRepository;
    private final PayProviderProperties.Alipay alipayProps;
    private final PayProviderProperties.WeChat wechatProps;

    /**
     * 首页
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("activePage", "home");
        model.addAttribute("pageTitle", "首页");
        
        // 添加统计数据
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTranslations", 1234);
        stats.put("totalDocuments", 567);
        stats.put("totalTerminologies", 890);
        stats.put("activeUsers", 123);
        model.addAttribute("stats", stats);
        
        // 添加最近翻译记录
        List<Map<String, Object>> recentTranslations = Arrays.asList(
            createTranslationRecord("Hello World", "你好世界", "EN", "ZH", "Google", "2分钟前"),
            createTranslationRecord("Good morning", "早上好", "EN", "ZH", "Baidu", "5分钟前"),
            createTranslationRecord("Thank you", "谢谢", "EN", "ZH", "Tencent", "10分钟前"),
            createTranslationRecord("How are you?", "你好吗？", "EN", "ZH", "Microsoft", "15分钟前"),
            createTranslationRecord("See you later", "再见", "EN", "ZH", "DeepL", "20分钟前")
        );
        model.addAttribute("recentTranslations", recentTranslations);
        
        // 添加系统公告
        List<Map<String, Object>> announcements = Arrays.asList(
            createAnnouncement("系统更新通知", "新增批量翻译功能，支持一次性翻译多个文本。", "2024-01-15"),
            createAnnouncement("新增翻译引擎", "新增DeepL翻译引擎，提供更高质量的翻译服务。", "2024-01-14"),
            createAnnouncement("术语库优化", "优化术语库匹配算法，提升专业术语翻译准确性。", "2024-01-13")
        );
        model.addAttribute("announcements", announcements);
        
        return "index";
    }

    /**
     * 文本翻译页面
     */
    @GetMapping("/translate")
    public String translate(Model model) {
        model.addAttribute("activePage", "translate");
        model.addAttribute("pageTitle", "文本翻译");
        
        // 添加面包屑导航
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "文本翻译", "url", "/translate")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "translate";
    }

    /**
     * 批量翻译页面
     */
    @GetMapping("/translate/batch")
    public String batchTranslate(Model model) {
        model.addAttribute("activePage", "batch-translate");
        model.addAttribute("pageTitle", "批量翻译");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "文本翻译", "url", "/translate"),
            Map.of("name", "批量翻译", "url", "/translate/batch")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "batch-translate";
    }

    /**
     * 文档翻译页面
     */
    @GetMapping("/document")
    public String document(Model model) {
        model.addAttribute("activePage", "document");
        model.addAttribute("pageTitle", "文档翻译");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "文档翻译", "url", "/document")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "document";
    }

    /**
     * 术语库管理页面
     */
    @GetMapping("/terminology")
    public String terminology(Model model) {
        model.addAttribute("activePage", "terminology");
        model.addAttribute("pageTitle", "术语库管理");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "术语库管理", "url", "/terminology")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "terminology";
    }

    /**
     * 聊天翻译页面
     */
    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("activePage", "chat");
        model.addAttribute("pageTitle", "聊天翻译");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "聊天翻译", "url", "/chat")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "chat";
    }

    /**
     * 翻译历史页面
     */
    @GetMapping("/history")
    public String history(Model model, 
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int size) {
        model.addAttribute("activePage", "history");
        model.addAttribute("pageTitle", "翻译历史");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "翻译历史", "url", "/history")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        // 添加分页参数
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        
        return "history";
    }

    /**
     * 质量评估页面
     */
    @GetMapping("/quality")
    public String quality(Model model) {
        model.addAttribute("activePage", "quality");
        model.addAttribute("pageTitle", "质量评估");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "质量评估", "url", "/quality")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "quality";
    }

    /**
     * 统计分析页面
     */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        model.addAttribute("activePage", "statistics");
        model.addAttribute("pageTitle", "统计分析");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "统计分析", "url", "/statistics")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "statistics";
    }

    /**
     * 系统设置页面
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("activePage", "settings");
        model.addAttribute("pageTitle", "系统设置");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "系统设置", "url", "/settings")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "settings";
    }

    /**
     * 个人资料页面
     */
    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("activePage", "profile");
        model.addAttribute("pageTitle", "个人资料");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "个人资料", "url", "/profile")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "profile";
    }

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "登录");
        model.addAttribute("showSidebar", false);
        return "login";
    }

    /**
     * API密钥管理页面
     */
    @GetMapping("/api-keys")
    public String apiKeys(Model model) {
        model.addAttribute("activePage", "api-keys");
        model.addAttribute("pageTitle", "API密钥管理");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "API密钥管理", "url", "/api-keys")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "api-keys";
    }

    /**
     * 使用帮助页面
     */
    @GetMapping("/help")
    public String help(Model model) {
        model.addAttribute("activePage", "help");
        model.addAttribute("pageTitle", "使用帮助");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "使用帮助", "url", "/help")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "help";
    }

    /**
     * API文档页面
     */
    @GetMapping("/api-docs")
    public String apiDocs(Model model) {
        model.addAttribute("activePage", "api-docs");
        model.addAttribute("pageTitle", "API文档");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "API文档", "url", "/api-docs")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "api-docs";
    }

    /**
     * 意见反馈页面
     */
    @GetMapping("/feedback")
    public String feedback(Model model) {
        model.addAttribute("activePage", "feedback");
        model.addAttribute("pageTitle", "意见反馈");
        
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "意见反馈", "url", "/feedback")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return "feedback";
    }

    /**
     * 关于我们页面
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "关于我们");
        model.addAttribute("showSidebar", false);
        
        return "about";
    }

    /**
     * 联系我们页面
     */
    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("pageTitle", "联系我们");
        model.addAttribute("showSidebar", false);
        
        return "contact";
    }

    /**
     * 朋友圈页面
     */
    @GetMapping("/moments")
    public String moments(Model model) {
        model.addAttribute("activePage", "moments");
        model.addAttribute("pageTitle", "朋友圈");

        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "朋友圈", "url", "/moments")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);

        return "moments";
    }

    /**
     * 开通会员页面
     */
    @GetMapping("/membership")
    public String membership(Model model) {
        model.addAttribute("activePage", "membership");
        model.addAttribute("pageTitle", "开通会员");

        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "开通会员", "url", "/membership")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);

        return "membership";
    }

    /**
     * 支付成功页面
     */
    @GetMapping("/membership/success")
    public String membershipSuccess(Model model) {
        model.addAttribute("activePage", "membership-success");
        model.addAttribute("pageTitle", "支付成功");
        List<Map<String, String>> breadcrumbs = Arrays.asList(
            Map.of("name", "首页", "url", "/"),
            Map.of("name", "开通会员", "url", "/membership"),
            Map.of("name", "支付成功", "url", "/membership/success")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        return "payment_success";
    }

    /**
     * 支付页面
     */
    @GetMapping("/membership/pay")
    public String membershipPay(Model model) {
        model.addAttribute("activePage", "membership-pay");
        model.addAttribute("pageTitle", "会员支付");
        List<Map<String, String>> breadcrumbs = Arrays.asList(
                Map.of("name", "首页", "url", "/"),
                Map.of("name", "开通会员", "url", "/membership"),
                Map.of("name", "会员支付", "url", "/membership/pay")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        return "payment_checkout";
    }

    /**
     * 我的订单列表页面
     */
    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("activePage", "orders");
        model.addAttribute("pageTitle", "我的订单");
        List<Map<String, String>> breadcrumbs = Arrays.asList(
                Map.of("name", "首页", "url", "/"),
                Map.of("name", "我的订单", "url", "/orders")
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        return "orders_list";
    }

    /**
     * 订单详情页面
     */
    @GetMapping("/orders/{orderNo}")
    public String orderDetail(Model model, @org.springframework.web.bind.annotation.PathVariable String orderNo) {
        model.addAttribute("activePage", "order-detail");
        model.addAttribute("pageTitle", "订单详情");
        model.addAttribute("orderNo", orderNo);
        List<Map<String, String>> breadcrumbs = Arrays.asList(
                Map.of("name", "首页", "url", "/"),
                Map.of("name", "我的订单", "url", "/orders"),
                Map.of("name", "订单详情", "url", "/orders/" + orderNo)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        return "order_detail";
    }

    /**
     * 模拟支付宝 Precreate 页面（扫码后展示）
     */
    @GetMapping("/mock/alipay/precreate")
    public String mockAlipayPrecreate(@RequestParam String paymentNo, Model model) {
        model.addAttribute("pageTitle", "支付宝 - 模拟收银台");
        model.addAttribute("provider", "ALIPAY");
        model.addAttribute("paymentNo", paymentNo);
        Optional<PaymentRecord> rec = paymentRecordRepository.findTopByPaymentNo(paymentNo);
        String amount = rec.map(r -> r.getAmount() != null ? r.getAmount().toPlainString() : null).orElse("—");
        String currency = rec.map(PaymentRecord::getCurrency).orElse("CNY");
        String appId = alipayProps != null ? alipayProps.getAppId() : null;
        String accountName = "模拟商户-支付宝";
        model.addAttribute("amount", amount);
        model.addAttribute("currency", currency);
        model.addAttribute("alipayAppId", appId);
        model.addAttribute("alipayAccountName", accountName);
        return "mock_alipay_precreate";
    }

    /**
     * 模拟微信统一下单页面（扫码后展示）
     */
    @GetMapping("/mock/wechat/unifiedorder")
    public String mockWeChatUnifiedOrder(@RequestParam String paymentNo, Model model) {
        model.addAttribute("pageTitle", "微信支付 - 模拟收银台");
        model.addAttribute("provider", "WECHAT");
        model.addAttribute("paymentNo", paymentNo);
        Optional<PaymentRecord> rec = paymentRecordRepository.findTopByPaymentNo(paymentNo);
        String amount = rec.map(r -> r.getAmount() != null ? r.getAmount().toPlainString() : null).orElse("—");
        String currency = rec.map(PaymentRecord::getCurrency).orElse("CNY");
        String mchid = wechatProps != null ? wechatProps.getMchid() : null;
        String appId = wechatProps != null ? wechatProps.getAppId() : null;
        String accountName = "模拟商户-微信";
        model.addAttribute("amount", amount);
        model.addAttribute("currency", currency);
        model.addAttribute("wechatMchid", mchid);
        model.addAttribute("wechatAppId", appId);
        model.addAttribute("wechatAccountName", accountName);
        return "mock_wechat_unifiedorder";
    }

    /**
     * 创建翻译记录
     */
    private Map<String, Object> createTranslationRecord(String sourceText, String translatedText, 
                                                       String sourceLanguage, String targetLanguage, 
                                                       String engine, String timeAgo) {
        Map<String, Object> record = new HashMap<>();
        record.put("sourceText", sourceText);
        record.put("translatedText", translatedText);
        record.put("sourceLanguage", sourceLanguage);
        record.put("targetLanguage", targetLanguage);
        record.put("engine", engine);
        record.put("timeAgo", timeAgo);
        return record;
    }

    /**
     * 创建系统公告
     */
    private Map<String, Object> createAnnouncement(String title, String content, String publishTime) {
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("content", content);
        announcement.put("publishTime", publishTime);
        return announcement;
    }
}