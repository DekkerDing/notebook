package io.github.dekkerding.examples.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.github.dekkerding.examples.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SearchController {

    @Resource
    private final ElasticsearchService elasticsearchService;

    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 匹配查询
     *
     * @param index
     * @param field
     * @param text
     * @return
     */
    public List<Hit<Object>> matchQuery(String index, String field, String text) {
        SearchRequest searchRequest = SearchRequest.of(builder ->
                builder
                        .index(index)
                        .query(query -> query.match(matchQuery ->
                                        matchQuery.field(field)
                                                .query(text)
                                )
                        )
        );
        SearchResponse<Object> search;
        try {
            search = elasticsearchClient.search(searchRequest, Object.class);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return search.hits().hits();
    }

    public void save(String index, Object entity) {
        try {
            elasticsearchClient.index(IndexRequest.of(request ->
                    request
                            .index(index)
                            .document(entity)
                            .refresh(Refresh.True)));
        } catch (Exception e) {
            if (e instanceof ElasticsearchException) {
                e = (ElasticsearchException) e;
                log.error("ES Error {}", (((ElasticsearchException) e).getDetailedMessage()));
            }
        }
    }

    public void saveAll(String index, Collection<Object> entity) {
        if (entity.isEmpty()) {
            log.warn("isEmpty");
        }
        try {
            elasticsearchClient.bulk(bulk -> bulk.operations(
                    entity.stream().map(detail ->
                            BulkOperation.of(of ->
                                    of.index(i -> i.index(index).document(detail))
                            )
                    ).collect(Collectors.toList())));
            elasticsearchClient.indices().refresh(refresh -> refresh.index(index));
        } catch (Exception e) {
            if (e instanceof ElasticsearchException) {
                e = (ElasticsearchException) e;
                log.error("ES Error {}", (((ElasticsearchException) e).getDetailedMessage()));
            }
        }
    }

    public List<SearchHit<Object>> nativeMatchQuery(String index, String field, String text) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery(field, text))
                .build();
        SearchHits<Object> searchHits = elasticsearchRestTemplate.search(searchQuery, Object.class, IndexCoordinates.of(index));
        return searchHits.getSearchHits();
    }

    public List<SearchHit<Object>> nativeSimpleCriteriaQuery(String index, String field, String text, String condition) {
        // 构造条件
        Criteria criteria = new Criteria();
        if (condition.equals("AND")) {
            criteria = Criteria.where(new SimpleField(field)).contains(text);
        } else if (condition.equals("OR")) {
            criteria.or(new SimpleField(field)).contains(text);
        }
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        SearchHits<Object> searchHits = elasticsearchRestTemplate.search(criteriaQuery, Object.class, IndexCoordinates.of(index));
        return searchHits.getSearchHits();
    }

    public List<SearchHit<Object>> nativeSimpleHighlightQuery(String index, String field, String text) {
        // 设置高亮效果
        String preTag = "<font color='#dd4b39'>";//Google的色值
        String postTag = "</font>";

        NativeSearchQuery highlightQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery(field, text))
                .withHighlightFields(new HighlightBuilder.Field(field).preTags(preTag).postTags(postTag)).build();

        SearchHits<Object> searchHits = elasticsearchRestTemplate.search(highlightQuery, Object.class, IndexCoordinates.of(index));
        return searchHits.getSearchHits();
    }

    public List<SearchHit<Object>> nativeSimplePageQuery(String index, String field, String text, int pageNumber, int pageSize, String sort) {

        NativeSearchQuery pageQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery(field, text))
                .withPageable(PageRequest.of(pageNumber - 1 < 0 ? 0 : pageNumber - 1, pageSize, Sort.by(Sort.Order.asc(sort)))).build();

        SearchHits<Object> searchHits = elasticsearchRestTemplate.search(pageQuery, Object.class, IndexCoordinates.of(index));

        return searchHits.getSearchHits();
    }
}