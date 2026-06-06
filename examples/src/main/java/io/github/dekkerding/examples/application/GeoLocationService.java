package io.github.dekkerding.examples.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.api.RedissonClient;
import org.redisson.api.GeoPosition;
import org.redisson.api.GeoUnit;
import org.redisson.api.RGeo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 地理位置服务（简化版）
 */
@Service
public class GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationService.class);
    private static final String GEO_KEY_PREFIX = "geo:";

    @Autowired(required = false)
    private RedissonClient redisson;

    public boolean addLocation(String geoKey, String member, double longitude, double latitude) {
        if (redisson == null) return false;
        try {
            RGeo<String> geo = redisson.getGeo(GEO_KEY_PREFIX + geoKey);
            geo.add(longitude, latitude, member);
            return true;
        } catch (Exception e) {
            log.error("添加位置失败", e);
            return false;
        }
    }

    public GeoPosition getPosition(String geoKey, String member) {
        if (redisson == null) return null;
        try {
            RGeo<String> geo = redisson.getGeo(GEO_KEY_PREFIX + geoKey);
            Map<String, GeoPosition> positions = geo.pos(member);
            return positions.get(member);
        } catch (Exception e) {
            log.error("获取位置失败", e);
            return null;
        }
    }

    public long deleteGeoSet(String geoKey) {
        if (redisson == null) return 0;
        try {
            RGeo<String> geo = redisson.getGeo(GEO_KEY_PREFIX + geoKey);
            return geo.delete() ? 1 : 0;
        } catch (Exception e) {
            log.error("删除GEO集合失败", e);
            return 0;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPoint {
        private double longitude;
        private double latitude;
    }
}
