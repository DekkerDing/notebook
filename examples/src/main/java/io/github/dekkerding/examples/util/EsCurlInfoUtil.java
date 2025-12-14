package io.github.dekkerding.examples.util;

import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.core.Map;

@Slf4j
public class EsCurlInfoUtil {

    private static final ObjectMapper JACKSON = new ObjectMapper();
    private static final JacksonJsonpMapper MAPPER = new JacksonJsonpMapper();

    /**
     * 生成索引创建操作的 cURL 命令
     */
    public static String generateCreateIndexCurl(CreateIndexRequest request, String endpointUrl) {
        try {
            String jsonBody = toJson(request).replaceAll("\\s+", " ").trim().replace("'", "\\'");
            String indexName = request.index();
            String settingsMappings =jsonBody;

            return String.format(
                    "curl -X PUT '%s/%s' \\\n" +
                            "  -H 'Content-Type: application/json' \\\n" +
                            "  -d '%s'",
                    endpointUrl, indexName, settingsMappings
            );
        } catch (Exception e) {
            return "Error generating create index curl: " + e.getMessage();
        }
    }

    /**
     * 生成文档索引（创建/更新）操作的 cURL 命令
     */
    public static String generateIndexDocumentCurl(IndexRequest<?> request, String endpointUrl) {
        try {
            String index = request.index();
            String id = request.id();
            String document = toJson(request.document());

            return String.format(
                    "curl -X POST '%s/%s/_doc/%s' \\\n" +
                            "  -H 'Content-Type: application/json' \\\n" +
                            "  -d '%s'",
                    endpointUrl, index, id, document
            );
        } catch (Exception e) {
            return "Error generating index document curl: " + e.getMessage();
        }
    }

    /**
     * 生成文档查询操作的 cURL 命令
     */
    public static String generateGetDocumentCurl(GetRequest request, String endpointUrl) {
        String index = request.index();
        String id = request.id();

        return String.format(
                "curl -X GET '%s/%s/_doc/%s' \\\n" +
                        "  -H 'Content-Type: application/json'",
                endpointUrl, index, id
        );
    }

    /**
     * 生成搜索操作的 cURL 命令（支持复杂查询）
     */
    public static String generateSearchCurl(SearchRequest request, String endpointUrl) {
        try {
            String index = String.join(",", request.index());
            String queryJson = toJson(request);

            return String.format(
                    "curl -X GET '%s/%s/_search' \\\n" +
                            "  -H 'Content-Type: application/json' \\\n" +
                            "  -d '%s'",
                    endpointUrl, index, queryJson
            );
        } catch (Exception e) {
            return "Error generating search curl: " + e.getMessage();
        }
    }

    /**
     * 生成文档更新操作的 cURL 命令
     */
    public static String generateUpdateDocumentCurl(UpdateRequest<?, ?> request, String endpointUrl) {
        try {
            String index = request.index();
            String id = request.id();

            // 提取 update 内容：doc / script
            Object updateSource = null;

            if (request.doc() != null) {
                updateSource = Map.of(
                        "doc", request.doc(),
                        "doc_as_upsert", request.docAsUpsert() != null && request.docAsUpsert()
                );
            } else if (request.script() != null) {
                updateSource = Map.of(
                        "script", request.script().source()
                );
            } else {
                updateSource = Map.of("doc", Map.of());
            }

            // 转换为 JSON 并压缩为单行
            String jsonBody = toJson(updateSource);

            return String.format(
                    "curl -X POST \"%s/%s/_update/%s\" -H \"Content-Type: application/json\" -d '%s'",
                    endpointUrl, index, id, jsonBody
            );

        } catch (Exception e) {
            return "Error generating update curl: " + e.getMessage();
        }
    }

    /**
     * 生成文档删除操作的 cURL 命令
     */
    public static String generateDeleteDocumentCurl(DeleteRequest request, String endpointUrl) {
        String index = request.index();
        String id = request.id();

        return String.format(
                "curl -X DELETE '%s/%s/_doc/%s'",
                endpointUrl, index, id
        );
    }

    /**
     * 生成索引删除操作的 cURL 命令
     */
    public static String generateDeleteIndexCurl(DeleteIndexRequest request, String endpointUrl) {
        String index = request.index().get(0); // 获取第一个索引名

        return String.format(
                "curl -X DELETE '%s/%s'",
                endpointUrl, index
        );
    }

    /**
     * 将 Elasticsearch Java Client 请求序列化为 json
     */
    public static String toJson(Object object) {
        try {
            // ES client request -> raw Jackson Node
            Object node = MAPPER.objectMapper().convertValue(object, Object.class);

            // 再序列化（可压缩 JSON）
            return JACKSON.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }
}