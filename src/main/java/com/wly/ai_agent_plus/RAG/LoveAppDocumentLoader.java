package com.wly.ai_agent_plus.RAG;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文档加载器 - 加载 Markdown 文档并转换为 Spring AI 的 Document 对象
 * 
 * 作用：
 *   把 src/main/resources/document/ 目录下的 Markdown 文件加载到内存，
 *   切分成小片段，每个片段带上元数据（文件名、标题、层级等），
 *   最终供向量存储使用。
 * 
 * 调用关系：
 *   LoveAppVectorStoreConfig（应用启动时）
 *       → LoveAppDocumentLoader.loadmarkdowns()（当前类）
 *           → 读取所有 .md 文件
 *           → 按标题层级切分
 *           → 语义切片（TokenTextSplitter）
 *           → 返回 List<Document>
 *       → 把文档列表加入 VectorStore
 * 
 * 为什么这样写：
 *   1. 使用 Spring AI 的标准接口（DocumentReader、TextSplitter），代码更规范
 *   2. 混合切片策略：先按 Markdown 标题切（保留结构），再语义切（控制大小）
 *   3. 添加缓存（@Cacheable），避免每次启动都重新加载文档
 *   4. 并行处理多个文件，提升加载速度
 *   5. 丰富的元数据（文件名、标题路径、chunk_index），方便后续检索和调试
 * 
 * 优化特性：
 *   - 使用 Spring AI 的 DocumentReader 接口进行标准化文档读取
 *   - 使用 TextSplitter 策略模式实现灵活的切片配置
 *   - 添加缓存机制避免重复加载
 *   - 使用配置化的切片参数（可在 application.yml 调整）
 *   - 改进元数据管理和文档转换链
 */
@Component
@Slf4j
public class LoveAppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;
    
    // 以下参数都可以在 application.yml 里配置，不改代码就能调整切片行为
    
    // 每个切片的目标 token 数，越大上下文越多但检索精度可能下降
    @Value("${spring.ai.document.chunk-size:800}")
    private int chunkSize;
    
    // 切片最小字符数，低于这个长度的内容不会被单独切出来
    @Value("${spring.ai.document.min-chunk-size-chars:350}")
    private int minChunkSizeChars;
    
    // 切片最小嵌入长度，低于这个长度的切片不会被向量化
    @Value("${spring.ai.document.min-chunk-length-to-embed:5}")
    private int minChunkLengthToEmbed;
    
    // 最多切多少片，防止文档过大撑爆内存
    @Value("${spring.ai.document.max-num-chunks:10000}")
    private int maxNumChunks;
    
    // 切片重叠字符数，相邻切片共享部分内容，避免语义在边界处断裂
    @Value("${spring.ai.document.chunk-overlap:100}")
    private int chunkOverlap;
    
    // Markdown 标题匹配正则：匹配 # 到 ###### 开头的行
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    
    // 文档目录，支持通配符，默认加载 resources/document/ 下所有 .md 文件
    @Value("${spring.ai.document.location:classpath:document/*.md}")
    private String documentLocation;

    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }
    
    /**
     * 创建 TextSplitter（语义切片器）
     * 使用 TokenTextSplitter，按 token 数量切片，比按字符数更准确
     * 参数全部来自配置文件，方便调整
     */
    private TextSplitter createTextSplitter() {
        return new TokenTextSplitter(
                chunkSize,
                minChunkSizeChars,
                minChunkLengthToEmbed,
                maxNumChunks,
                true  // keepSeparator
        );
    }

    /**
     * 加载所有 Markdown 文档
     * 
     * @Cacheable：第一次调用时真正执行，结果缓存起来；
     *             后续调用直接返回缓存，不重复读文件和切片。
     *             unless = "#result.isEmpty()" 表示结果为空时不缓存（避免缓存异常情况）
     * 
     * 被调用方：LoveAppVectorStoreConfig，在应用启动时调用一次
     */
    @Cacheable(value = "markdownDocuments", unless = "#result.isEmpty()")
    public List<Document> loadmarkdowns() {
        log.info("开始加载 Markdown 文档，位置: {}", documentLocation);

        //ArrayList 是 Java 中最常用的动态数组，可以理解为一个可以自动扩容的列表。
        List<Document> documents = new ArrayList<>();
        
        try {
            // 查找所有 .md 文件

            //得到文件位置
            Resource[] resources = resourcePatternResolver.getResources(documentLocation);
            
            if (resources.length == 0) {
                log.warn("未找到任何 Markdown 文档，请在 src/main/resources/document/ 目录下添加 .md 文件");
                return documents;
            }
            
            log.info("找到 {} 个 Markdown 文件", resources.length);
            
            // 并行处理文档以提高性能
            // Arrays.stream(resources)转成流后可以像流水线一样处理每个元素   resources 是数组：[文件A, 文件B, 文件C, 文件D, 文件E]   → 流: 文件A → 文件B → 文件C → 文件D → 文件E
            // .parallel()会用多线程同时处理多个文件
            // 文件A ↘
            // 文件B ↘
            // 文件C → 同时处理  (总时间 ≈ 最慢那个文件的时间)
            // 文件D ↗
            // 文件E ↗
            // flatMap 把每个文件的切片列表合并成一个大列表



            // resources[5个文件]
            //     ↓ Arrays.stream()
            // 流: 文件A → 文件B → 文件C → 文件D → 文件E
            //     ↓ .parallel()
            // 并行流: 5个线程同时处理
            //     ↓ .flatMap(loadSingleDocument)
            // 每个文件读取内容并切片，压平成一个流:
            // [A1,A2,A3,A4,A5,A6, B1,B2...B7, C1...C8, D1...D7, E1...E8]
            //     ↓ .collect(Collectors.toList())
            // List<Document> documents (36个片段)

            documents = Arrays.stream(resources)
                    .parallel()
                    .flatMap(resource -> {
                        try {
                            // 读取单个文件，返回多个文档片段
                            // 比如 love-tips.md 被切成 6 个片段
                            return loadSingleDocument(resource).stream();
                            // → [片段1, 片段2, 片段3, 片段4, 片段5, 片段6]
                        } catch (IOException e) {
                            log.error("加载文档失败: {}", resource.getFilename(), e);
                            return java.util.stream.Stream.empty();
                        }
                    })
                    // 流处理完后还是"流"的形式，需要收集成具体的集合
                    .collect(Collectors.toList());
                    // → List<Document>，包含 36 个文档片段
            
            log.info("成功加载 {} 个文档片段", documents.size());
            
        } catch (IOException e) {
            log.error("加载 Markdown 文档失败", e);
        }
        
        return documents;
    }
    
    /**
     * 加载单个 Markdown 文件
     * 
     * 使用 Spring AI 的 TextReader（DocumentReader 接口的实现）读取文件，
     * 比手动 BufferedReader 更规范，自动处理编码。
     * 读完后交给 hybridSplit 做切片。
     */
    private List<Document> loadSingleDocument(Resource resource) throws IOException {
        String filename = resource.getFilename();
        if (filename == null) {
            filename = "unknown";
        }
        log.debug("正在加载文档: {}", filename);
        
        // 使用 Spring AI 的 TextReader 读取文档
        DocumentReader reader = new TextReader(resource);
        List<Document> rawDocuments = reader.get();
        
        if (rawDocuments.isEmpty()) {
            log.warn("文档 {} 内容为空", filename);
            return Collections.emptyList();
        }
        
        // 获取文档内容
        String content = rawDocuments.get(0).getText();
        if (content == null || content.isEmpty()) {
            log.warn("文档 {} 内容为空", filename);
            return Collections.emptyList();
        }
        
        // 混合切片：严格按标题层级切分，再语义切片
        List<Document> fileDocuments = hybridSplit(content, filename);
        
        log.debug("文档 {} 切分为 {} 个片段", filename, fileDocuments.size());
        
        return fileDocuments;
    }
    
    /**
     * 混合切片策略
     * 
     * 为什么要两步切片，而不是直接用 TextSplitter 切整个文档？
     *   直接切的话，切片可能横跨两个不相关的章节，导致检索时上下文混乱。
     *   先按标题切，保证每个切片只属于一个章节；
     *   再用 TextSplitter 控制大小，保证切片不会太大（超出 token 限制）。
     * 
     * 第一步：splitByHeadingsHierarchical → 按 Markdown 标题切，保留结构
     * 第二步：TokenTextSplitter → 对过长的章节再细切，控制 token 数量
     */
    private List<Document> hybridSplit(@NonNull String content, String filename) {
        List<Document> documents = new ArrayList<>();
        
        // 第一步：严格按 Markdown 标题层级切分
        List<Section> sections = splitByHeadingsHierarchical(content);
        
        // 第二步：对每个章节使用语义切片
        // 根据配置重新创建 TextSplitter
        TextSplitter splitter = createTextSplitter();
        
        int chunkIndex = 0;
        for (Section section : sections) {
            // 如果章节内容较短，直接作为一个文档
            if (section.content.length() < minChunkSizeChars * 2) {
                Map<String, Object> metadata = createMetadata(filename, section, chunkIndex++);
                documents.add(new Document(section.content, metadata));
            } else {
                // 章节内容较长，使用语义切片
                Map<String, Object> tempMetadata = createMetadata(filename, section, 0);
                Document tempDoc = new Document(section.content, tempMetadata);
                
                // 使用 Spring AI 的 TextSplitter 进行切片
                List<Document> chunks = splitter.apply(Collections.singletonList(tempDoc));
                
                // 为每个切片添加 chunk_index 和丰富的元数据
                for (Document chunk : chunks) {
                    enrichMetadata(chunk, filename, section, chunkIndex++);
                    documents.add(chunk);
                }
            }
        }
        
        return documents;
    }
    
    /**
     * 创建文档元数据
     * 
     * 元数据的作用：检索时可以按这些字段过滤，比如只检索某个文件、某个标题层级。
     * 同时方便调试，知道每个切片来自哪里。
     */
    @NonNull
    private Map<String, Object> createMetadata(String filename, @NonNull Section section, int chunkIndex) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", filename);
        metadata.put("heading", section.heading);
        metadata.put("level", section.level);
        metadata.put("chunk_index", chunkIndex);
        metadata.put("content_length", section.content.length());
        metadata.put("document_type", "markdown");
        
        // 添加层级路径（面包屑）
        if (section.headingPath != null && !section.headingPath.isEmpty()) {
            metadata.put("heading_path", String.join(" > ", section.headingPath));
        }
        
        // 添加时间戳
        metadata.put("loaded_at", System.currentTimeMillis());
        
        return metadata;
    }
    
    /**
     * 为 TextSplitter 切出来的切片补充元数据
     * 
     * TextSplitter 切片后，原始元数据会继承过来，但 chunk_index 需要重新编号。
     * 这里统一补充所有需要的字段，保证每个切片的元数据完整。
     */
    private void enrichMetadata(@NonNull Document document, String filename, 
                                @NonNull Section section, int chunkIndex) {
        Map<String, Object> metadata = document.getMetadata();
        metadata.put("chunk_index", chunkIndex);
        metadata.put("filename", filename);
        metadata.put("heading", section.heading);
        metadata.put("level", section.level);
        
        String text = document.getText();
        if (text != null) {
            metadata.put("content_length", text.length());
        }
        
        if (section.headingPath != null && !section.headingPath.isEmpty()) {
            metadata.put("heading_path", String.join(" > ", section.headingPath));
        }
    }
    
    /**
     * 按 Markdown 标题层级切分文档
     * 
     * 原理：用正则找出所有标题的位置，然后按位置把文档切成多个 Section。
     * 每个 Section 包含从当前标题到下一个同级/更高级标题之间的所有内容。
     * 
     * 例如：
     *   # 第一章          → Section(level=1, heading="第一章", content="# 第一章\n## 1.1...")
     *   ## 1.1 小节       → Section(level=2, heading="1.1 小节", content="## 1.1 小节\n内容...")
     *   ## 1.2 小节       → Section(level=2, heading="1.2 小节", content="## 1.2 小节\n内容...")
     *   # 第二章          → Section(level=1, heading="第二章", content="# 第二章\n内容...")
     */
    @NonNull
    private List<Section> splitByHeadingsHierarchical(@NonNull String content) {
        // 存放切分结果，每个 Section 对应一个标题下的内容块
        List<Section> sections = new ArrayList<>();

        // 用正则 HEADING_PATTERN（匹配 # 到 ###### 开头的行）在全文中查找所有标题
        Matcher matcher = HEADING_PATTERN.matcher(content);
        
        // 收集所有标题及其在文档中的位置
        List<HeadingPosition> headings = new ArrayList<>();
        while (matcher.find()) {
            int level = matcher.group(1).length();  // # 的数量就是标题级别，# = 1，## = 2，以此类推
            String title = matcher.group(2).trim(); // 标题文字，去掉首尾空格
            int position = matcher.start();         // 该标题在整个文档字符串中的起始位置
            headings.add(new HeadingPosition(level, title, position));
        }
        
        // 如果文档没有任何标题（纯文本），把整个文档作为一个章节返回
        if (headings.isEmpty()) {
            Section section = new Section(0, "文档内容", content.trim());
            section.headingPath = new ArrayList<>();
            sections.add(section);
            return sections;
        }
        
        // 遍历每个标题，确定它对应的内容范围，切出一个 Section
        for (int i = 0; i < headings.size(); i++) {
            HeadingPosition current = headings.get(i);
            
            // 默认结束位置是文档末尾
            int endPos = content.length();

            // 向后查找：找到第一个级别 <= 当前标题的标题，作为当前 Section 的结束位置
            // 例如当前是 ## 二级标题，遇到下一个 # 一级标题或 ## 二级标题就停止
            // 遇到 ### 三级标题不停止，因为它属于当前 Section 的子内容
            for (int j = i + 1; j < headings.size(); j++) {
                HeadingPosition next = headings.get(j);
                if (next.level <= current.level) {
                    endPos = next.position;
                    break;
                }
            }
            
            // 截取从当前标题到结束位置的文本，就是这个 Section 的完整内容
            // 例如：## 1.1 小节\n内容...\n### 子节\n子节内容...
            String sectionContent = content.substring(current.position, endPos).trim();
            
            // 构建面包屑路径，例如：["第一章", "1.1 小节"]，方便知道当前 Section 的层级位置
            List<String> headingPath = buildHeadingPath(headings, i);
            
            // 创建 Section 对象并加入结果列表
            Section section = new Section(current.level, current.title, sectionContent);
            section.headingPath = headingPath;
            sections.add(section);
        }
        
        return sections;
    }
    
    /**
     * 构建标题路径（面包屑）
     * 
     * 作用：记录当前标题的完整层级路径，方便检索时知道上下文。
     * 例如：一级标题 > 二级标题 > 当前标题
     * 
     * 原理：从当前标题往前找，找到比自己级别更高的标题就加入路径，
     *       一直找到最顶层为止。
     */
    @NonNull
    private List<String> buildHeadingPath(@NonNull List<HeadingPosition> headings, int currentIndex) {
        List<String> path = new ArrayList<>();
        HeadingPosition current = headings.get(currentIndex);
        
        // 向前查找所有父级标题
        for (int i = currentIndex - 1; i >= 0; i--) {
            HeadingPosition prev = headings.get(i);
            if (prev.level < current.level) {
                path.add(0, prev.title);  // 添加到开头
                current = prev;  // 继续向上查找
            }
        }
        
        // 添加当前标题
        path.add(headings.get(currentIndex).title);
        
        return path;
    }
    
    /**
     * 标题位置信息（内部数据结构）
     * 用于记录文档中每个标题的级别、文本和字符位置，
     * 方便后续按位置切分文档内容
     */
    private static class HeadingPosition {
        int level;      // 标题级别（1-6）
        String title;   // 标题文本
        int position;   // 在文档中的位置
        
        HeadingPosition(int level, String title, int position) {
            this.level = level;
            this.title = title;
            this.position = position;
        }
    }
    
    /**
     * 文档章节（内部数据结构）
     * 代表按标题切分后的一个章节，包含标题信息和该章节的完整内容。
     * 是 hybridSplit 第一步的输出，第二步 TextSplitter 的输入。
     */
    private static class Section {
        int level;              // 标题级别
        String heading;         // 标题文本
        String content;         // 章节内容
        List<String> headingPath;  // 标题路径（面包屑）
        
        Section(int level, String heading, String content) {
            this.level = level;
            this.heading = heading;
            this.content = content;
        }
    }
}

