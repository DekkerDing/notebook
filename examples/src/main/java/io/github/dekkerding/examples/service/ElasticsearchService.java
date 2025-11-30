package io.github.dekkerding.examples.service;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElasticsearchService extends ElasticsearchRepository<Object,Long> {
}