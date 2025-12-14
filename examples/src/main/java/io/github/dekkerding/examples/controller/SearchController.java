package io.github.dekkerding.examples.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import io.github.dekkerding.examples.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SearchController {

    @Resource
    private final ElasticsearchService elasticsearchService;

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
                log.error("ES Error", e);
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
                log.error("ES Error", e);
            }
        }
    }

    public void createEsIndex(String index, TypeMapping mapping, IndexSettings settings){
        log.info("createEsIndex prepare to create index {} mapping {}", index, settings);
        try {
            CreateIndexResponse response = elasticsearchClient.indices()
                    .create((c) ->
                            c.index(index)
                                    .mappings(mapping)
                                    .settings(settings)
                    );
            log.info("createEsIndex finished index {} response {}", index, response.acknowledged());
        }catch (Exception e){
            log.error("createEsIndex index {} mapping {} error",index,mapping, convertEsError(e));
        }
        
    }

    public TypeMapping generateIndexMapping(){
        Map<String, Property> properties = new HashMap();
        properties.put("keyword", Property.of(k-> k.keyword(key->key)));
        properties.put("int", Property.of(i->i.integer(integer->integer)));
        properties.put("text", Property.of(t->t.text(text->text.index(false))));
        properties.put("tk", Property.of(p->p.text(text->text.fields("keyword",f->f.keyword(k -> k)))));
        properties.put("long", Property.of(l->l.long_(longs->longs)));
        properties.put("float", Property.of(f->f.float_(floats->floats)));
        properties.put("double", Property.of(d->d.double_(doubles->doubles)));
        properties.put("date", Property.of(d->d.date(date->date.format("strict_date_optional_time||epoch_millis"))));
        properties.put("byte", Property.of(b->b.byte_(bytes->bytes.nullValue(0))));
        return TypeMapping.of((m)->m.properties(properties));
    }

    public IndexSettings generateIndexSettings(String shards,String replicas){
        return IndexSettings.of(s->s
                .numberOfShards(shards)
                .numberOfReplicas(replicas)
        );
    }

    public Exception convertEsError(Exception e) {
        if(e instanceof ElasticsearchException){
            ElasticsearchException ES_Exception = (ElasticsearchException) e;
            log.error("ES Error", ES_Exception.toString());
        }
        return e;
    }
}